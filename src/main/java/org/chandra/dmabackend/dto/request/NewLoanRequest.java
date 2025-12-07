package org.chandra.dmabackend.dto.request;

import jakarta.validation.constraints.*;
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
public class NewLoanRequest {

    @NotBlank
    private String loanName;

    @NotBlank
    private String category;

    @NotBlank
    private String lender;

    @NotNull
    @DecimalMin("1")
    private BigDecimal principal;

    @NotNull
    @DecimalMin("0")
    private BigDecimal annualInterestRate;

    @NotNull
    @Min(1)
    private Integer tenureMonths;

    private LocalDate startDate;

    private LocalDate emiStartDate;

    @NotNull
    @Min(1)
    @Max(28)
    private Integer emiDayOfMonth;

    @NotNull
    private Boolean foreclosureAllowed;

    private BigDecimal foreclosurePenaltyPercent;

    @NotNull
    private Boolean partPaymentAllowed;


}
