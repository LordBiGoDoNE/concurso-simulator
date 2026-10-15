package br.com.concursosimulator.mockexam.domain;

import java.util.Objects;
import java.util.UUID;

/** Identidade de posse fornecida pelo adaptador verificado, nunca pelo JSON do cliente. */
public record AttemptOwner(Kind kind, UUID id) {
    public enum Kind { ACCOUNT, VISITOR }
    public AttemptOwner { Objects.requireNonNull(kind); Objects.requireNonNull(id); }
}
