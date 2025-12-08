package org.chandra.dmabackend.repository;

import org.chandra.dmabackend.model.Loan;
import org.chandra.dmabackend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByLoanOrderByPaymentDateAsc(Loan loan);

    List<Payment> findByLoan(Loan loan);

}
