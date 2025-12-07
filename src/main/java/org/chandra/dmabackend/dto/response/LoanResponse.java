package org.chandra.dmabackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoanResponse {

    private Long id;
    private String loanName;
    private String category;
    private String lender;
    private BigDecimal principal;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private LocalDate startDate;
    private LocalDate emiStartDate;
    private String status;

}
