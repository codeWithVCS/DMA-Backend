package org.chandra.dmabackend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class PartPaymentResponse {

    private BigDecimal oldPrincipal;
    private BigDecimal newPrincipal;
    private BigDecimal amountPaid;
    private Integer emiRowsRecalculated;
    private String loanStatus;

}
