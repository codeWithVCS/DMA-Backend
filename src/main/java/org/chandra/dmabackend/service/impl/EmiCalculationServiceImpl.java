package org.chandra.dmabackend.service.impl;

import org.chandra.dmabackend.service.EmiCalculationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class EmiCalculationServiceImpl implements EmiCalculationService {

    // Use high precision for internal EMI calculations
    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);

    @Override
    public BigDecimal calculateEmi(BigDecimal principal,
                                   BigDecimal annualInterestRate,
                                   Integer tenureMonths) {

        // VALIDATION
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be greater than zero");
        }

        if (tenureMonths == null || tenureMonths <= 0) {
            throw new IllegalArgumentException("Tenure (months) must be greater than zero");
        }

        if (annualInterestRate == null) {
            throw new IllegalArgumentException("Interest rate must be provided");
        }

        // HANDLE 0% INTEREST CASE
        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal
                    .divide(new BigDecimal(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        // EMI FORMULA IMPLEMENTATION
        //
        // monthlyRate = R / (12 * 100)
        // EMI = P * r * (1+r)^n / ((1+r)^n - 1)

        BigDecimal hundred = new BigDecimal("100");
        BigDecimal twelve = new BigDecimal("12");

        // Convert annual interest rate → monthly decimal interest rate
        BigDecimal monthlyRate = annualInterestRate
                .divide(hundred, MC)
                .divide(twelve, MC);

        // (1 + r)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, MC);

        // (1 + r)^n
        BigDecimal pow = onePlusR.pow(tenureMonths, MC);

        // numerator = P * r * (1+r)^n
        BigDecimal numerator = principal
                .multiply(monthlyRate, MC)
                .multiply(pow, MC);

        // denominator = (1+r)^n - 1
        BigDecimal denominator = pow.subtract(BigDecimal.ONE, MC);

        // EMI final = numerator / denominator
        BigDecimal emi = numerator.divide(denominator, MC);

        // Round off to 2 decimals for storage
        return emi.setScale(2, RoundingMode.HALF_UP);
    }
}
