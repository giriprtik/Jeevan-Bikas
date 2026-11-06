package com.hospital.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Runs the real gateway in front of a tiny stub backend that echoes what it received,
 * to check routing by path prefix and that outside callers cannot smuggle in the internal secret.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    /** One stub per service; each replies "<service> path=<path> secret=<X-Internal-Secret or none>". */
    private static final HttpServer AUTH = startBackend("auth");
    private static final HttpServer OPD = startBackend("opd");
    private static final HttpServer LAB = startBackend("lab");

    @Autowired WebTestClient client;

    @DynamicPropertySource
    static void routeToStub(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> "http://localhost:" + AUTH.getAddress().getPort());
        registry.add("OPD_SERVICE_URL", () -> "http://localhost:" + OPD.getAddress().getPort());
        registry.add("LAB_SERVICE_URL", () -> "http://localhost:" + LAB.getAddress().getPort());
    }

    @AfterAll
    static void stopBackends() {
        AUTH.stop(0);
        OPD.stop(0);
        LAB.stop(0);
    }

    @Test
    void routesEachPrefixToItsService() {
        expectBody("/api/v1/auth/login", "auth path=/api/v1/auth/login secret=none");
        expectBody("/api/v1/opd/visits/today", "opd path=/api/v1/opd/visits/today secret=none");
        expectBody("/api/v1/lab/orders/queue", "lab path=/api/v1/lab/orders/queue secret=none");
    }

    @Test
    void stripsTheInternalSecretFromOutsideTrafficToAuth() {
        client.get().uri("/api/v1/auth/users/resolve?username=doctor")
                .header("X-Internal-Secret", "guessed-or-stolen")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("auth path=/api/v1/auth/users/resolve secret=none");
    }

    @Test
    void unknownPathsAreNotForwarded() {
        client.get().uri("/api/v1/billing/invoices").exchange().expectStatus().isNotFound();
    }

    private void expectBody(String path, String body) {
        client.get().uri(path).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(body);
    }

    private static HttpServer startBackend(String service) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String secret = exchange.getRequestHeaders().getFirst("X-Internal-Secret");
                byte[] reply = (service + " path=" + exchange.getRequestURI().getPath() + " secret=" + (secret == null ? "none" : secret))
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, reply.length);
                exchange.getResponseBody().write(reply);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
