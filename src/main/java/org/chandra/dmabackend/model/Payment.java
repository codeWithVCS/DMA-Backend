package org.chandra.dmabackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@NoArgsConstructor
@Getter
@Setter
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    private BigDecimal amountPaid;

    @Column(nullable = false)
    private BigDecimal allocatedToInterest;

    @Column(nullable = false)
    private BigDecimal allocatedToPrincipal;

    @Column(nullable = false)
    private BigDecimal outstandingAfterPayment;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    private String remarks;

}
