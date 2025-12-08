package org.chandra.dmabackend.service.impl;

import jakarta.transaction.Transactional;
import org.chandra.dmabackend.dto.DerivedDates;
import org.chandra.dmabackend.dto.request.ExistingLoanRequest;
import org.chandra.dmabackend.dto.request.NewLoanRequest;
import org.chandra.dmabackend.dto.response.EmiScheduleResponse;
import org.chandra.dmabackend.dto.response.LoanResponse;
import org.chandra.dmabackend.model.EmiSchedule;
import org.chandra.dmabackend.model.Loan;
import org.chandra.dmabackend.model.User;
import org.chandra.dmabackend.repository.EmiScheduleRepository;
import org.chandra.dmabackend.repository.LoanRepository;
import org.chandra.dmabackend.repository.UserRepository;
import org.chandra.dmabackend.service.EmiCalculationService;
import org.chandra.dmabackend.service.EmiScheduleGeneratorService;
import org.chandra.dmabackend.service.LoanDateService;
import org.chandra.dmabackend.service.LoanService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanServiceImpl implements LoanService {

    private final UserRepository userRepository;
    private final LoanDateService loanDateService;
    private final EmiCalculationService emiCalculationService;
    private final LoanRepository loanRepository;
    private final EmiScheduleGeneratorService emiScheduleGeneratorService;
    private final EmiScheduleRepository emiScheduleRepository;

    public LoanServiceImpl(UserRepository userRepository,
                           LoanDateService loanDateService,
                           EmiCalculationService emiCalculationService,
                           LoanRepository loanRepository, EmiScheduleGeneratorService emiScheduleGeneratorService, EmiScheduleRepository emiScheduleRepository) {
        this.userRepository = userRepository;
        this.loanDateService = loanDateService;
        this.emiCalculationService = emiCalculationService;
        this.loanRepository = loanRepository;
        this.emiScheduleGeneratorService = emiScheduleGeneratorService;
        this.emiScheduleRepository = emiScheduleRepository;
    }

    @Override
    @Transactional
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

        List<EmiSchedule> schedule = emiScheduleGeneratorService.generateSchedule(savedLoan);

        emiScheduleRepository.saveAll(schedule);

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

    @Override
    @Transactional
    public LoanResponse createExistingLoan(ExistingLoanRequest request, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DerivedDates dates = loanDateService.deriveDates(
                request.getStartDate(),
                request.getEmiStartDate(),
                request.getEmiDayOfMonth()
        );

        BigDecimal emiToUse;

        if (request.getEmiAmount() != null) {
            emiToUse = request.getEmiAmount();
        } else {
            emiToUse = emiCalculationService.calculateEmi(
                    request.getPrincipal(),
                    request.getAnnualInterestRate(),
                    request.getTenureMonths()
            );
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setLoanName(request.getLoanName());
        loan.setCategory(request.getCategory());
        loan.setLender(request.getLender());
        loan.setPrincipal(request.getPrincipal());
        loan.setInterestRate(request.getAnnualInterestRate());
        loan.setTenureMonths(request.getTenureMonths());
        loan.setEmiAmount(emiToUse);
        loan.setStartDate(dates.getStartDate());
        loan.setEmiStartDate(dates.getEmiStartDate());
        loan.setForeclosureAllowed(request.getForeclosureAllowed());
        loan.setForeclosurePenaltyPercent(request.getForeclosurePenaltyPercent());
        loan.setPartPaymentAllowed(request.getPartPaymentAllowed());
        loan.setStatus("ACTIVE");

        Loan savedLoan = loanRepository.save(loan);

        List<EmiSchedule> schedule = emiScheduleGeneratorService.generateSchedule(savedLoan);

        emiScheduleRepository.saveAll(schedule);

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

    @Override
    public List<EmiScheduleResponse> getSchedule(Long loanId, Long userId) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (!loan.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("Unauthorized access");
        }

        List<EmiSchedule> schedule = emiScheduleRepository.findByLoanOrderByMonthIndexAsc(loan);

        List<EmiScheduleResponse> scheduleResponse = new ArrayList<>();

        for(EmiSchedule emiSchedule : schedule){

            EmiScheduleResponse response = new EmiScheduleResponse();
            response.setMonthIndex(emiSchedule.getMonthIndex());
            response.setDueDate(emiSchedule.getDueDate());
            response.setOpeningBalance(emiSchedule.getOpeningBalance());
            response.setEmiAmount(emiSchedule.getEmiAmount());
            response.setInterestComponent(emiSchedule.getInterestComponent());
            response.setPrincipalComponent(emiSchedule.getPrincipalComponent());
            response.setClosingBalance(emiSchedule.getClosingBalance());
            response.setStatus(emiSchedule.getStatus().toString());

            scheduleResponse.add(response);
        }

        return scheduleResponse;
    }
}
