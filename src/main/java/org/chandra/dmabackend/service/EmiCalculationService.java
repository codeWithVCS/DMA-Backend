package org.chandra.dmabackend.service;

import java.math.BigDecimal;

public interface EmiCalculationService {

    BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualInterestRate, Integer tenureMonths);

}
