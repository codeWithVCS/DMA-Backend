package org.chandra.dmabackend.service;

import org.chandra.dmabackend.dto.request.ExistingLoanRequest;
import org.chandra.dmabackend.dto.request.NewLoanRequest;
import org.chandra.dmabackend.dto.response.EmiScheduleResponse;
import org.chandra.dmabackend.dto.response.LoanResponse;
import org.chandra.dmabackend.model.EmiSchedule;
import org.chandra.dmabackend.model.Loan;

import java.util.List;

public interface LoanService {

    LoanResponse createNewLoan(NewLoanRequest request, Long userId);

    LoanResponse createExistingLoan(ExistingLoanRequest request, Long userId);

    List<EmiScheduleResponse> getSchedule(Long loanId, Long userId);

}
