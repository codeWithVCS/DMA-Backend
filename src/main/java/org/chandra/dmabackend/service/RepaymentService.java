package org.chandra.dmabackend.service;

import org.chandra.dmabackend.dto.response.ForeclosureResponse;
import org.chandra.dmabackend.dto.response.PartPaymentResponse;
import org.chandra.dmabackend.dto.response.PayEmiResponse;
import org.chandra.dmabackend.dto.response.RepaymentHistoryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface RepaymentService {

    PayEmiResponse payEmi(Long emiId, Long userId, BigDecimal amountPaid);

    PartPaymentResponse partPayment(Long loanId, Long userId, BigDecimal amountPaid);

    ForeclosureResponse forecloseLoan(Long loanId, Long userId, BigDecimal amountPaid);

    List<RepaymentHistoryResponse> getRepaymentHistory(Long loanId, Long userId);

}
