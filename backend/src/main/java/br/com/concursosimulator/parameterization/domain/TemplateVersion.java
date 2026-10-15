package br.com.concursosimulator.parameterization.domain;

import java.util.Objects;

public record TemplateVersion(TemplateDefinition definition,VariantManifest manifest) {
    public TemplateVersion { Objects.requireNonNull(definition);Objects.requireNonNull(manifest);manifest.verify(definition); }
}
