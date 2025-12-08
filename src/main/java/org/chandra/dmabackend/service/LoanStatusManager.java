package org.chandra.dmabackend.service;

import org.chandra.dmabackend.model.Loan;

public interface LoanStatusManager {

    void updateLoanStatus(Loan loan);

}
