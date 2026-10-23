package br.com.concursosimulator.platform.infrastructure;

import br.com.concursosimulator.platform.application.port.ReadinessProbe;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcReadinessProbe implements ReadinessProbe {
    private final JdbcTemplate jdbc;

    public JdbcReadinessProbe(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean isReady() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (DataAccessException exception) { return false; }
    }
}
