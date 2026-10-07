package br.com.concursosimulator.platform;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    private final JdbcTemplate jdbc;

    public StatusController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/status")
    public ResponseEntity<Status> status() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(new Status("UP"));
        } catch (DataAccessException exception) {
            return ResponseEntity.status(503).body(new Status("DOWN"));
        }
    }

    public record Status(String status) {}
}
