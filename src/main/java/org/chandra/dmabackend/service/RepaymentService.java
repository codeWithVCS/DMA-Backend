package org.chandra.dmabackend.service;

import org.chandra.dmabackend.dto.response.PartPaymentResponse;
import org.chandra.dmabackend.dto.response.PayEmiResponse;

import java.math.BigDecimal;

public interface RepaymentService {

    PayEmiResponse payEmi(Long emiId, Long userId, BigDecimal amountPaid);

    PartPaymentResponse partPayment(Long loanId, Long userId, BigDecimal amountPaid);

}
