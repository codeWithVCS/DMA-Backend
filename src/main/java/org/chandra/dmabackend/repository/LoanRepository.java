package org.chandra.dmabackend.repository;

import org.chandra.dmabackend.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan,Long> {

    List<Loan> findAllByStatus(String status);

    List<Loan> findByUserId(Long userId);

}
