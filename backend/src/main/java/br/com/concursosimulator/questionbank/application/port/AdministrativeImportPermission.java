package br.com.concursosimulator.questionbank.application.port;

import java.util.UUID;

/** Current permission and credential epoch, independent of the identity implementation. */
public interface AdministrativeImportPermission { boolean permits(UUID actor,long authenticationVersion); }
