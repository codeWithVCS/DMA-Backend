package org.chandra.dmabackend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class LoanHealthResponse {

    private Long loanId;
    private String loanStatus;

    private Integer totalEmis;
    private Integer paidEmis;
    private Integer pendingEmis;
    private Integer missedEmis;

    private Long nextEmiId;
    private Integer nextMonthIndex;
    private LocalDate nextDueDate;
    private BigDecimal nextEmiAmount;

    private BigDecimal principalOutstanding;

    private boolean canPayNextEmi;
    private boolean canForeclose;
    private boolean hasMissedEmis;
}
