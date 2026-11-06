package com.hospital.lab.service;

import com.hospital.lab.dto.LabOrderRequest;
import com.hospital.lab.dto.LabOrderResponse;
import com.hospital.lab.model.LabOrder;
import com.hospital.lab.model.LabOrderStatus;
import com.hospital.lab.model.LabSampleCollection;
import com.hospital.lab.repository.LabOrderRepository;
import com.hospital.lab.repository.LabSampleCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LabOrderServiceTest {

    private final LabOrderRepository orders = mock(LabOrderRepository.class);
    private final LabSampleCollectionRepository samples = mock(LabSampleCollectionRepository.class);
    private LabOrderService service;

    /** In-memory stand-in for the lab_sample_collections table. */
    private final List<LabSampleCollection> collected = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new LabOrderService(orders, samples);
        when(orders.save(any(LabOrder.class))).thenAnswer(inv -> {
            LabOrder o = inv.getArgument(0);
            o.setId(10L);
            return o;
        });
        when(samples.saveAndFlush(any(LabSampleCollection.class))).thenAnswer(inv -> {
            collected.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(samples.findByOrderId(10L)).thenAnswer(inv -> List.copyOf(collected));
        when(samples.existsByOrderIdAndTestCode(any(), any())).thenAnswer(inv ->
                collected.stream().anyMatch(s -> s.getTestCode().equals(inv.getArgument(1))));
    }

    @Test
    void newOrdersNormaliseAndDeduplicateTestCodes() {
        LabOrderRequest request = new LabOrderRequest();
        request.setVisitId(5L);
        request.setPatientMrn(" JB-000007 ");
        request.setPatientName("Ram Bahadur");
        request.setTestCodes(List.of(" cbc", "CBC", "lft ", "  "));

        LabOrderResponse response = service.createOrder(request, "doctor");

        assertThat(response.testCodes()).containsExactly("CBC", "LFT");
        assertThat(response.patientMrn()).isEqualTo("JB-000007");
        assertThat(response.orderedByDoctor()).isEqualTo("doctor");
        assertThat(response.orderStatus()).isEqualTo(LabOrderStatus.PLACED);
        assertThat(response.samples()).extracting(LabOrderResponse.Sample::collected).containsExactly(false, false);
    }

    @Nested
    @DisplayName("collectSample (one test at a time)")
    class CollectSample {

        @Test
        void theOrderStaysPlacedWhileAnyTestIsStillPending() {
            when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.PLACED, "CBC", "LFT")));

            LabOrderResponse response = service.collectSample(10L, "cbc", "labtech");

            assertThat(response.orderStatus()).isEqualTo(LabOrderStatus.PLACED);
            assertThat(response.samples()).extracting(LabOrderResponse.Sample::testCode, LabOrderResponse.Sample::collected)
                    .containsExactly(tuple("CBC", true), tuple("LFT", false));
            assertThat(response.samples().get(0).collectedBy()).isEqualTo("labtech");
        }

        @Test
        void collectingTheLastSampleMovesTheOrderToSampleCollected() {
            when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.PLACED, "CBC", "LFT")));

            service.collectSample(10L, "CBC", "labtech");
            LabOrderResponse response = service.collectSample(10L, "LFT", "labtech");

            assertThat(response.orderStatus()).isEqualTo(LabOrderStatus.SAMPLE_COLLECTED);
            assertThat(response.samples()).allMatch(LabOrderResponse.Sample::collected);
        }

        @Test
        void aTestThatIsNotOnTheOrderIsABadRequest() {
            when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.PLACED, "CBC")));

            assertThatThrownBy(() -> service.collectSample(10L, "LIPID", "labtech"))
                    .satisfies(e -> assertStatus(e, 400))
                    .hasMessageContaining("LIPID is not on order #10");
            verify(samples, never()).saveAndFlush(any());
        }

        @Test
        void theSameSampleCannotBeCollectedTwice() {
            when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.PLACED, "CBC", "LFT")));
            service.collectSample(10L, "CBC", "labtech");

            assertThatThrownBy(() -> service.collectSample(10L, "CBC", "someone-else"))
                    .satisfies(e -> assertStatus(e, 409))
                    .hasMessageContaining("already collected");
        }

        @ParameterizedTest
        @EnumSource(value = LabOrderStatus.class, names = {"SAMPLE_COLLECTED", "COMPLETED", "CANCELLED"})
        void onlyPlacedOrdersHavePendingSamples(LabOrderStatus status) {
            when(orders.findById(10L)).thenReturn(Optional.of(order(status, "CBC")));

            assertThatThrownBy(() -> service.collectSample(10L, "CBC", "labtech"))
                    .satisfies(e -> assertStatus(e, 409));
        }

        @Test
        void unknownOrderIsNotFound() {
            when(orders.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.collectSample(10L, "CBC", "labtech"))
                    .satisfies(e -> assertStatus(e, 404));
        }
    }

    @Nested
    @DisplayName("updateStatus (manual changes)")
    class UpdateStatus {

        @Test
        void aPlacedOrderCanBeCancelled() {
            when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.PLACED, "CBC")));

            assertThat(service.updateStatus(10L, LabOrderStatus.CANCELLED).orderStatus()).isEqualTo(LabOrderStatus.CANCELLED);
        }

        @Test
        void sampleCollectionMustGoThroughThePerTestEndpoint() {
            when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.PLACED, "CBC")));

            assertThatThrownBy(() -> service.updateStatus(10L, LabOrderStatus.SAMPLE_COLLECTED))
                    .satisfies(e -> assertStatus(e, 400))
                    .hasMessageContaining("/tests/{testCode}/collect");
        }

        @Test
        void completionOnlyHappensByEnteringResults() {
            when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.SAMPLE_COLLECTED, "CBC")));

            assertThatThrownBy(() -> service.updateStatus(10L, LabOrderStatus.COMPLETED))
                    .satisfies(e -> assertStatus(e, 409));
        }

        @Test
        void aCompletedOrderCannotBeCancelled() {
            when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.COMPLETED, "CBC")));

            assertThatThrownBy(() -> service.updateStatus(10L, LabOrderStatus.CANCELLED))
                    .satisfies(e -> assertStatus(e, 409));
        }
    }

    // ---------------------------------------------------------------- helpers

    static LabOrder order(LabOrderStatus status, String... tests) {
        LabOrder o = new LabOrder();
        o.setId(10L);
        o.setVisitId(5L);
        o.setPatientMrn("JB-000007");
        o.setOrderedByDoctor("doctor");
        o.setOrderStatus(status);
        o.setTestCodes(new LinkedHashSet<>(List.of(tests)));
        return o;
    }

    static void assertStatus(Throwable e, int expected) {
        assertThat(e).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(expected);
    }
}
