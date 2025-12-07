package org.chandra.dmabackend.service;

import org.chandra.dmabackend.dto.request.NewLoanRequest;
import org.chandra.dmabackend.dto.response.LoanResponse;

public interface LoanService {

    LoanResponse createNewLoan(NewLoanRequest request, Long userId);

}
