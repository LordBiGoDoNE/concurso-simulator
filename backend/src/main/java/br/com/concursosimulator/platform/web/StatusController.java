package br.com.concursosimulator.platform.web;

import br.com.concursosimulator.platform.application.port.ReadinessProbe;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    private final ReadinessProbe readiness;

    public StatusController(ReadinessProbe readiness) {
        this.readiness = readiness;
    }

    @GetMapping("/api/v1/status")
    public ResponseEntity<Status> status() {
        return readiness.isReady() ? ResponseEntity.ok(new Status("UP"))
                : ResponseEntity.status(503).body(new Status("DOWN"));
    }

    public record Status(String status) {}
}
