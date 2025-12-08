package org.chandra.dmabackend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class MarkPaidResponse {

    private Long emiId;
    private Integer monthIndex;
    private LocalDate actualPaymentDate;

    private BigDecimal openingBalance;
    private BigDecimal interestComponent;
    private BigDecimal principalComponent;
    private BigDecimal closingBalance;

    private BigDecimal updatedLoanOutstanding;
    private String loanStatus;

}
