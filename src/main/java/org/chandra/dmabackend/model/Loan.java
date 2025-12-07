package org.chandra.dmabackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
@NoArgsConstructor
@Getter
@Setter
public class Loan extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "loan_name", nullable = false, length = 150)
    private String loanName;

    @Column(name = "category",  nullable = false, length = 80)
    private String category;

    @Column(name = "lender", nullable = false, length = 120)
    private String lender;

    @Column(name = "principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal principal;

    @Column(name = "interest_rate",nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "emi_amount",  nullable = false, precision = 12, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "emi_start_date")
    private LocalDate emiStartDate;

    @Column(name = "foreclosure_allowed", nullable = false)
    private Boolean foreclosureAllowed = false;

    @Column(name = "foreclosure_penalty_percent", precision = 5, scale = 2)
    private BigDecimal foreclosurePenaltyPercent;

    @Column(name = "part_payment_allowed", nullable = false)
    private Boolean partPaymentAllowed = false;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

}
