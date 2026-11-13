package br.com.concursosimulator.questionbank.application;

import java.time.Instant;
import java.util.UUID;

/** Vínculo da prévia confirmada (#177), verificado criptograficamente na infraestrutura; não autentica o ator. */
public record BatchConfirmation(UUID nonce, UUID actor, String destination, String digest, Instant issuedAt, Instant expiresAt) {}
