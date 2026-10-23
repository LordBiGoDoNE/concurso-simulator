package br.com.concursosimulator.mockexam.infrastructure;

import br.com.concursosimulator.mockexam.application.AttemptStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Clock;

/** Limpeza técnica; a regra de acesso expira no domínio mesmo se o processo estiver parado. */
public final class ExpiredAttemptCleanup {
    private final AttemptStore store;
    private final Clock clock;
    public ExpiredAttemptCleanup(AttemptStore store,Clock clock) { this.store=store; this.clock=clock; }
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { run(); }
    @Scheduled(fixedRateString="PT5M",initialDelayString="PT5M")
    public void run() { store.transaction(s -> s.deleteExpired(clock.instant())); }
}
