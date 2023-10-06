package com.se.kltn.spamanagement.repository;

import com.se.kltn.spamanagement.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>{
    Optional<List<Invoice>> findInvoicesByCustomer_Id(Long idCustomer);
}
