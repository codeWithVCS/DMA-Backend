package org.chandra.dmabackend.service;

import org.chandra.dmabackend.dto.response.EmiBreakdownResult;

import java.math.BigDecimal;

public interface EmiBreakdownService {

    EmiBreakdownResult calculate(BigDecimal openingBalance,
                                 BigDecimal monthlyInterestRate,
                                 BigDecimal emiAmount);

}
