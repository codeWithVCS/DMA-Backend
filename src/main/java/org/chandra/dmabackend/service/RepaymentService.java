package org.chandra.dmabackend.service;

import org.chandra.dmabackend.dto.response.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RepaymentService {

    PayEmiResponse payEmi(Long emiId, Long userId, BigDecimal amountPaid);

    PartPaymentResponse partPayment(Long loanId, Long userId, BigDecimal amountPaid);

    ForeclosureResponse forecloseLoan(Long loanId, Long userId, BigDecimal amountPaid);

    List<RepaymentHistoryResponse> getRepaymentHistory(Long loanId, Long userId);

    MarkPaidResponse markEmiPaid(Long emiId, Long userId, LocalDate actualPaymentDate);

    MarkMissedResponse markEmiMissed(Long emiId, Long userId);

}
