package br.com.concursosimulator.parameterization.application;

import br.com.concursosimulator.parameterization.domain.TemplateVersion;
import java.util.List;

/** Contrato privado: leitura participa do snapshot transacional do consumidor. */
public interface EligibleTemplates { List<TemplateVersion> capture(); }
