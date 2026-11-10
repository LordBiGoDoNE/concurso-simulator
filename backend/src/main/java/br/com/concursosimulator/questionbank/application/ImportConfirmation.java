package br.com.concursosimulator.questionbank.application;

import java.time.Instant;
import java.util.UUID;

/** Vínculo de operação verificado criptograficamente na infraestrutura; não autentica o ator. */
public record ImportConfirmation(UUID nonce, UUID actor, String destination, String packageDigest,
        String selectionDigest, Instant issuedAt, Instant expiresAt) {}
