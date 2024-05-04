package com.se.kltn.spamanagement.repository;

import com.se.kltn.spamanagement.model.Appointment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAppointmentsByCustomer_Id(Long idCustomer, Pageable pageable);

    @Query(value = "select * from appointments a where a.time between :startDate and :endDate ", nativeQuery = true)
    List<Appointment> findAppointmentsByTimeUpComing2Hours(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);
}
