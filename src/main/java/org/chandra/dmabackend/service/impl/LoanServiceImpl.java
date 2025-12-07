package org.chandra.dmabackend.service.impl;

import org.chandra.dmabackend.dto.DerivedDates;
import org.chandra.dmabackend.dto.request.NewLoanRequest;
import org.chandra.dmabackend.dto.response.LoanResponse;
import org.chandra.dmabackend.model.Loan;
import org.chandra.dmabackend.model.User;
import org.chandra.dmabackend.repository.LoanRepository;
import org.chandra.dmabackend.repository.UserRepository;
import org.chandra.dmabackend.service.EmiCalculationService;
import org.chandra.dmabackend.service.LoanDateService;
import org.chandra.dmabackend.service.LoanService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class LoanServiceImpl implements LoanService {

    private final UserRepository userRepository;
    private final LoanDateService loanDateService;
    private final EmiCalculationService emiCalculationService;
    private final LoanRepository loanRepository;

    public LoanServiceImpl(UserRepository userRepository,
                           LoanDateService loanDateService,
                           EmiCalculationService emiCalculationService,
                           LoanRepository loanRepository) {
        this.userRepository = userRepository;
        this.loanDateService = loanDateService;
        this.emiCalculationService = emiCalculationService;
        this.loanRepository = loanRepository;
    }

    @Override
    public LoanResponse createNewLoan(NewLoanRequest request, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DerivedDates dates = loanDateService.deriveDates(
                request.getStartDate(),
                request.getEmiStartDate(),
                request.getEmiDayOfMonth()
        );

        BigDecimal emi = emiCalculationService.calculateEmi(
                request.getPrincipal(),
                request.getAnnualInterestRate(),
                request.getTenureMonths()
        );

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setLoanName(request.getLoanName());
        loan.setCategory(request.getCategory());
        loan.setLender(request.getLender());
        loan.setPrincipal(request.getPrincipal());
        loan.setInterestRate(request.getAnnualInterestRate());
        loan.setTenureMonths(request.getTenureMonths());
        loan.setEmiAmount(emi);
        loan.setStartDate(dates.getStartDate());
        loan.setEmiStartDate(dates.getEmiStartDate());
        loan.setForeclosureAllowed(request.getForeclosureAllowed());
        loan.setForeclosurePenaltyPercent(request.getForeclosurePenaltyPercent());
        loan.setPartPaymentAllowed(request.getPartPaymentAllowed());
        loan.setStatus("ACTIVE");

        Loan savedLoan = loanRepository.save(loan);

        LoanResponse response = new LoanResponse();
        response.setId(savedLoan.getId());
        response.setLoanName(savedLoan.getLoanName());
        response.setCategory(savedLoan.getCategory());
        response.setLender(savedLoan.getLender());
        response.setPrincipal(savedLoan.getPrincipal());
        response.setAnnualInterestRate(savedLoan.getInterestRate());
        response.setTenureMonths(savedLoan.getTenureMonths());
        response.setEmiAmount(savedLoan.getEmiAmount());
        response.setStartDate(savedLoan.getStartDate());
        response.setEmiStartDate(savedLoan.getEmiStartDate());
        response.setStatus(savedLoan.getStatus());

        return response;
    }
}
