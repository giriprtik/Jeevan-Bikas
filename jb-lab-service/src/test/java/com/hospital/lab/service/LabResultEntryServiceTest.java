package com.hospital.lab.service;

import com.hospital.lab.dto.LabResultSubmissionRequest;
import com.hospital.lab.dto.OrderResultsRequest;
import com.hospital.lab.model.LabOrder;
import com.hospital.lab.model.LabOrderStatus;
import com.hospital.lab.model.LabSampleCollection;
import com.hospital.lab.model.LabTest;
import com.hospital.lab.model.LabTestParameter;
import com.hospital.lab.repository.LabOrderRepository;
import com.hospital.lab.repository.LabSampleCollectionRepository;
import com.hospital.lab.repository.LabTestParameterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.hospital.lab.service.LabOrderServiceTest.assertStatus;
import static com.hospital.lab.service.LabOrderServiceTest.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LabResultEntryServiceTest {

    private final LabOrderRepository orders = mock(LabOrderRepository.class);
    private final LabTestParameterRepository parameters = mock(LabTestParameterRepository.class);
    private final LabResultService results = mock(LabResultService.class);
    private final LabSampleCollectionRepository samples = mock(LabSampleCollectionRepository.class);
    private LabResultEntryService service;

    @BeforeEach
    void setUp() {
        service = new LabResultEntryService(orders, parameters, results, samples);
        // Build the mocks first: stubbing one mock inside another's thenReturn(...) is an unfinished stubbing.
        List<LabTestParameter> catalogue = List.of(
                parameter("CBC", "Hemoglobin", "g/dL", "12.0", "17.5"),
                parameter("CBC", "Platelet Count", "x10^3/uL", "150", "450"),
                parameter("FBS", "Glucose", "mg/dL", null, "100"),
                parameter("URINE", "Protein", null, null, null));
        when(parameters.findByTestCodes(anyCollection())).thenReturn(catalogue);
    }

    @Test
    void resultsAreRefusedUntilEverySampleIsCollected() {
        LabOrder order = order(LabOrderStatus.PLACED, "CBC", "LFT");
        when(orders.findById(10L)).thenReturn(Optional.of(order));
        collected("CBC");

        assertThatThrownBy(() -> service.submitOrderResults(10L, request(entry("CBC", "Hemoglobin", "13.1")), "labtech"))
                .satisfies(e -> assertStatus(e, 409))
                .hasMessageContaining("Collect the sample for LFT");
        verifyNoInteractions(results);
        assertThat(order.getOrderStatus()).isEqualTo(LabOrderStatus.PLACED);
    }

    @Test
    void submittingResultsCompletesTheOrderAndFlagsOutOfRangeValues() {
        LabOrder order = order(LabOrderStatus.SAMPLE_COLLECTED, "CBC");
        when(orders.findById(10L)).thenReturn(Optional.of(order));
        collected("CBC");

        service.submitOrderResults(10L, request(
                entry("cbc", "hemoglobin", " 10.4 "),          // low, and case/whitespace-insensitive lookup
                entry("CBC", "Platelet Count", "260")), "labtech");

        List<LabResultSubmissionRequest> sent = submitted(2);
        assertThat(sent.get(0).getParameterName()).isEqualTo("Hemoglobin");
        assertThat(sent.get(0).getObservedValue()).isEqualTo("10.4");
        assertThat(sent.get(0).getReferenceRange()).isEqualTo("12 - 17.5 g/dL");
        assertThat(sent.get(0).isAbnormal()).isTrue();
        assertThat(sent.get(1).isAbnormal()).isFalse();
        assertThat(order.getOrderStatus()).isEqualTo(LabOrderStatus.COMPLETED);
    }

    @ParameterizedTest(name = "{0} {1} = {2} -> abnormal={3}, range={4}")
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "FBS   | Glucose    | 99    | false | <= 100 mg/dL",
            "FBS   | Glucose    | 100.1 | true  | <= 100 mg/dL",
            "CBC   | Hemoglobin | 17.5  | false | 12 - 17.5 g/dL",   // bounds are inclusive
            "CBC   | Hemoglobin | <0.1  | false | 12 - 17.5 g/dL",   // non-numeric: only the manual flag counts
            "URINE | Protein    | ++    | false | null",
    })
    void abnormalFlagAndReferenceRange(String test, String parameter, String value, boolean abnormal, String range) {
        when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.SAMPLE_COLLECTED, test)));
        collected(test);

        service.submitOrderResults(10L, request(entry(test, parameter, value)), "labtech");

        LabResultSubmissionRequest sent = submitted(1).get(0);
        assertThat(sent.isAbnormal()).isEqualTo(abnormal);
        assertThat(sent.getReferenceRange()).isEqualTo(range);
    }

    @Test
    void theTechnicianCanFlagQualitativeValuesManually() {
        when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.SAMPLE_COLLECTED, "URINE")));
        collected("URINE");

        service.submitOrderResults(10L, request(
                new OrderResultsRequest.Entry("URINE", "Protein", "++", true, "frothy sample")), "labtech");

        assertThat(submitted(1).get(0).isAbnormal()).isTrue();
    }

    @Test
    void aParameterThatIsNotInTheCatalogueIsABadRequest() {
        when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.SAMPLE_COLLECTED, "CBC")));
        collected("CBC");

        assertThatThrownBy(() -> service.submitOrderResults(10L, request(entry("CBC", "Unicorn Count", "3")), "labtech"))
                .satisfies(e -> assertStatus(e, 400))
                .hasMessageContaining("Unknown parameter 'Unicorn Count' for CBC");
    }

    @Test
    void resultsForATestThatWasNotOrderedAreABadRequest() {
        when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.SAMPLE_COLLECTED, "CBC")));
        collected("CBC");

        assertThatThrownBy(() -> service.submitOrderResults(10L, request(entry("FBS", "Glucose", "90")), "labtech"))
                .satisfies(e -> assertStatus(e, 400))
                .hasMessageContaining("FBS was not ordered");
    }

    @Test
    void aCompletedOrderCannotBeResultedAgain() {
        when(orders.findById(10L)).thenReturn(Optional.of(order(LabOrderStatus.COMPLETED, "CBC")));

        assertThatThrownBy(() -> service.submitOrderResults(10L, request(entry("CBC", "Hemoglobin", "13")), "labtech"))
                .satisfies(e -> assertStatus(e, 409));
        verifyNoInteractions(results);
    }

    // ---------------------------------------------------------------- helpers

    private void collected(String... tests) {
        when(samples.findByOrderId(10L)).thenReturn(Arrays.stream(tests)
                .map(t -> new LabSampleCollection(10L, t, "labtech"))
                .toList());
    }

    private List<LabResultSubmissionRequest> submitted(int expected) {
        ArgumentCaptor<LabResultSubmissionRequest> captor = ArgumentCaptor.forClass(LabResultSubmissionRequest.class);
        verify(results, times(expected)).submitResult(captor.capture(), eq("labtech"));
        return captor.getAllValues();
    }

    private static OrderResultsRequest request(OrderResultsRequest.Entry... entries) {
        return new OrderResultsRequest(List.of(entries));
    }

    private static OrderResultsRequest.Entry entry(String test, String parameter, String value) {
        return new OrderResultsRequest.Entry(test, parameter, value, null, null);
    }

    private static LabTestParameter parameter(String test, String name, String unit, String low, String high) {
        LabTest labTest = new LabTest();
        labTest.setTestCode(test);
        LabTestParameter p = mock(LabTestParameter.class);
        when(p.getTest()).thenReturn(labTest);
        when(p.getParameterName()).thenReturn(name);
        when(p.getUnit()).thenReturn(unit);
        when(p.getRefLow()).thenReturn(low == null ? null : new BigDecimal(low));
        when(p.getRefHigh()).thenReturn(high == null ? null : new BigDecimal(high));
        return p;
    }
}
