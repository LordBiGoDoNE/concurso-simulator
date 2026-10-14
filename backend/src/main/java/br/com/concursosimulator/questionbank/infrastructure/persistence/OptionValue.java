package br.com.concursosimulator.questionbank.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Embeddable
public class OptionValue {
    @Column(name="option_id", nullable=false) UUID optionId;
    protected OptionValue() {}
    OptionValue(UUID id) { this.optionId=id; }
}
