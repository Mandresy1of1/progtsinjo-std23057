package com.prog.tsinjo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    public enum Status {
        VERIFYING, SUCCEEDED, FAILED
    }

    // L'énumération Method est ajoutée ici
    public enum Method {
        ORANGE_MONEY("Orange Money"),
        AIRTEL_MONEY("Airtel Money"),
        MVOLA("Mvola"),
        CARD("Carte bancaire");

        @Getter
        private final String displayName;

        Method(String displayName) {
            this.displayName = displayName;
        }
    }

    @Id
    private String id; // Généré par Vola

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Le champ `method` est passé de String à l'énumération Method
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Method method; // Utilise la nouvelle énumération

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime date;

    @PrePersist
    protected void onCreate() {
        if (date == null) {
            date = LocalDateTime.now();
        }
    }
}