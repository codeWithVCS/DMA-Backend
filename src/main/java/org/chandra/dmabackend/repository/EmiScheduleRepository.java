package org.chandra.dmabackend.repository;

import org.chandra.dmabackend.model.EmiSchedule;
import org.chandra.dmabackend.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmiScheduleRepository extends JpaRepository<EmiSchedule,Long> {

    List<EmiSchedule> findByLoanOrderByMonthIndexAsc(Loan loan);

}
