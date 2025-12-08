package org.chandra.dmabackend.service.impl;

import org.chandra.dmabackend.dto.response.EmiBreakdownResult;
import org.chandra.dmabackend.service.EmiBreakdownService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class EmiBreakdownServiceImpl implements EmiBreakdownService {

    // High precision for internal calculations
    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);

    @Override
    public EmiBreakdownResult calculate(BigDecimal openingBalance,
                                        BigDecimal monthlyInterestRate,
                                        BigDecimal emiAmount) {

        // Input Validation
        if (openingBalance == null || openingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Opening Balance must be greater than 0");
        }

        if (monthlyInterestRate == null || monthlyInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monthly Interest Rate must be >= 0");
        }

        if (emiAmount == null || emiAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("EMI Amount must be greater than 0");
        }

        BigDecimal rawInterest = openingBalance.multiply(monthlyInterestRate, MC);
        BigDecimal interest = rawInterest.setScale(2, RoundingMode.HALF_UP);

        BigDecimal rawPrincipal = emiAmount.subtract(interest, MC);
        BigDecimal principal = rawPrincipal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal rawClosing = openingBalance.subtract(principal, MC);
        BigDecimal closing = rawClosing.setScale(2, RoundingMode.HALF_UP);
        if (closing.compareTo(BigDecimal.ZERO) < 0) {
            closing = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        EmiBreakdownResult result = new EmiBreakdownResult();
        result.setInterestComponent(interest);
        result.setPrincipalComponent(principal);
        result.setClosingBalance(closing);

        return result;
    }
}
