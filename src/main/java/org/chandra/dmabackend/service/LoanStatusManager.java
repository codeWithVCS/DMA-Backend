package org.chandra.dmabackend.service;

import org.chandra.dmabackend.dto.response.LoanHealthResponse;
import org.chandra.dmabackend.dto.response.LoanSummaryResponse;
import org.chandra.dmabackend.model.Loan;

import java.util.List;

public interface LoanStatusManager {

    void updateLoanStatus(Loan loan);

    LoanHealthResponse evaluateLoanHealth(Long loanId, Long userId);

    String evaluateLoanStatus(Long loanId);

    List<LoanSummaryResponse> getUserLoanSummaries(Long userId);
}
