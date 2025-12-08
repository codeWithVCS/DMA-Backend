package org.chandra.dmabackend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class ForeclosureResponse {

    private BigDecimal principalOutstanding;
    private BigDecimal penaltyApplied;
    private BigDecimal totalAmountRequired;
    private BigDecimal amountPaid;
    private String status;
    private Integer pendingEmiCountClosed;

}
