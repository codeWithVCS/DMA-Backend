package org.chandra.dmabackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "emi_schedule")
@NoArgsConstructor
@Getter
@Setter
public class EmiSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer monthIndex;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal openingBalance;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal emiAmount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal interestComponent;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal principalComponent;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal closingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmiScheduleStatus status;

}
