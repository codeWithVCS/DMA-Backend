package org.chandra.dmabackend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class RepaymentHistoryResponse {

    private Long paymentId;
    private LocalDate paymentDate;
    private BigDecimal amountPaid;
    private BigDecimal allocatedToInterest;
    private BigDecimal allocatedToPrincipal;
    private BigDecimal outstandingAfterPayment;
    private String paymentType;
    private String remarks;

}
