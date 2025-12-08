package org.chandra.dmabackend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class PayEmiResponse {

    private Long emiId;
    private Integer monthIndex;
    private BigDecimal openingBalance;
    private BigDecimal interestComponent;
    private BigDecimal principalComponent;
    private BigDecimal closingBalance;

    private BigDecimal updatedLoanOutstanding;
    private String loanStatus;

}
