package br.com.concursosimulator.questionbank.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;

@Embeddable
public class ReferenceValue {
    @Column(nullable=false, length=1000) String name;
    @Column(nullable=false, length=4000) String url;
    @Column(length=4000) String provision;
    @Column(name="checked_on") LocalDate checkedOn;
    protected ReferenceValue() {}
    ReferenceValue(String name, String url, String provision, LocalDate checkedOn) {
        this.name=name; this.url=url; this.provision=provision; this.checkedOn=checkedOn;
    }
}
