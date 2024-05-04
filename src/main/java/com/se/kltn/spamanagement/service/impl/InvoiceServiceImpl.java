package com.se.kltn.spamanagement.service.impl;

import com.se.kltn.spamanagement.dto.request.InvoiceCreateRequest;
import com.se.kltn.spamanagement.dto.request.InvoiceDetailRequest;
import com.se.kltn.spamanagement.dto.request.InvoiceUpdateRequest;
import com.se.kltn.spamanagement.dto.response.CustomerResponse;
import com.se.kltn.spamanagement.dto.response.InvoiceDetailResponse;
import com.se.kltn.spamanagement.dto.response.InvoiceResponse;
import com.se.kltn.spamanagement.dto.response.ProductResponse;
import com.se.kltn.spamanagement.exception.ResourceNotFoundException;
import com.se.kltn.spamanagement.model.Invoice;
import com.se.kltn.spamanagement.model.InvoiceDetail;
import com.se.kltn.spamanagement.constants.enums.ProductType;
import com.se.kltn.spamanagement.constants.enums.Status;
import com.se.kltn.spamanagement.repository.CustomerRepository;
import com.se.kltn.spamanagement.repository.EmployeeRepository;
import com.se.kltn.spamanagement.repository.InvoiceRepository;
import com.se.kltn.spamanagement.repository.ProductRepository;
import com.se.kltn.spamanagement.service.InvoiceService;
import com.se.kltn.spamanagement.utils.MappingData;
import com.se.kltn.spamanagement.utils.NullUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.se.kltn.spamanagement.constants.ErrorMessage.*;

@Service
@Transactional
@Log4j2
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    private final CustomerRepository customerRepository;

    private final EmployeeRepository employeeRepository;

    private final InvoiceDetailServiceImpl invoiceDetailService;

    private final ProductRepository productRepository;

    @Autowired
    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, CustomerRepository customerRepository, EmployeeRepository employeeRepository, InvoiceDetailServiceImpl invoiceDetailService, ProductRepository productRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.invoiceDetailService = invoiceDetailService;
        this.productRepository = productRepository;
    }

    @Override
    public InvoiceResponse createInvoice(InvoiceCreateRequest invoiceRequest) {
        log.info("Create invoice");
        Invoice invoice = MappingData.mapObject(invoiceRequest, Invoice.class);
        invoice.setCustomer(this.customerRepository.findById(invoiceRequest.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND)
        ));
        invoice.setEmployee(this.employeeRepository.findById(invoiceRequest.getEmployeeId()).orElseThrow(
                () -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND)
        ));
        invoice.setStatus(Status.UNPAID);
        invoice.setCreatedDate(new Date());
        invoice.setUpdatedDate(new Date());
        Invoice invoiceSaved = this.invoiceRepository.saveAndFlush(invoice);
        return saveInvoiceDetail(invoiceSaved, invoiceRequest);
    }

    @Override
    public InvoiceResponse updateInvoice(Long id, InvoiceUpdateRequest invoiceRequest) {
        log.info("Update invoice");
        Invoice invoice = this.invoiceRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(INVOICE_NOT_FOUND));
        NullUtils.updateIfPresent(invoice::setNote, invoiceRequest.getNote());
        NullUtils.updateIfPresent(invoice::setDueDate, invoiceRequest.getDueDate());
        NullUtils.updateIfPresent(invoice::setPaymentMethod, invoiceRequest.getPaymentMethod());
        NullUtils.updateIfPresent(invoice::setStatus, invoiceRequest.getStatus());
        invoice.setUpdatedDate(new Date());
        //calculate total amount
        Double totalAmount = 0.0;
        for (InvoiceDetail invoiceDetail : invoice.getInvoiceDetails()) {
            totalAmount += invoiceDetail.getTotalPrice();
        }
        invoice.setTotalAmount(totalAmount);
        Invoice invoiceSaved = this.invoiceRepository.saveAndFlush(invoice);
        checkToReduceProduct(invoiceSaved.getStatus(), invoice);
        return mapToResponse(invoiceSaved);
    }


    @Override
    public List<InvoiceResponse> getInvoicesByCustomer(Long idCustomer) {
        log.info("Get invoices by customer");
        List<Invoice> invoices = this.invoiceRepository.findInvoicesByCustomer_Id(idCustomer);
        return mapToListResponse(invoices);
    }


    @Override
    public void deleteInvoice(Long id) {
        log.info("Delete invoice");
        Invoice invoice = this.invoiceRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(INVOICE_NOT_FOUND));
        for (InvoiceDetail invoiceDetail : invoice.getInvoiceDetails()) {
            this.invoiceDetailService.deleteInvoiceDetail(invoiceDetail.getId());
        }
        this.invoiceRepository.delete(invoice);
    }

    @Override
    public List<InvoiceResponse> getAllInvoice(int page, int size) {
        log.info("Get all invoice paginated");
        Pageable pageable = PageRequest.of(page, size);
        return mapToListResponse(this.invoiceRepository.findAll(pageable).getContent());
    }

    private InvoiceResponse saveInvoiceDetail(Invoice invoiceSaved, InvoiceCreateRequest invoiceRequest) {
        List<InvoiceDetailRequest> invoiceDetailRequests = invoiceRequest.getInvoiceDetailRequests();
        Double totalAmount = 0.0;
        List<InvoiceDetail> invoiceDetails = new ArrayList<>();
        for (InvoiceDetailRequest invoiceDetailRequest : invoiceDetailRequests) {
            InvoiceDetail invoiceDetail = MappingData.mapObject(invoiceDetailRequest, InvoiceDetail.class);
            invoiceDetail.setInvoice(invoiceSaved);
            InvoiceDetailResponse invoiceDetailResponse = invoiceDetailService.createInvoiceDetail(invoiceDetail, invoiceDetailRequest.getIdProduct());
            totalAmount += invoiceDetailResponse.getTotalPrice();
            invoiceDetails.add(invoiceDetail);
        }
        invoiceSaved.setTotalAmount(totalAmount);
        invoiceSaved.setInvoiceDetails(invoiceDetails);
        return mapToResponse(this.invoiceRepository.saveAndFlush(invoiceSaved));
    }

    private List<InvoiceResponse> mapToListResponse(List<Invoice> invoices) {
        List<InvoiceResponse> invoiceResponses = MappingData.mapListObject(invoices, InvoiceResponse.class);
        invoiceResponses.forEach(invoiceResponse -> invoices.forEach(invoice -> {
            if (invoiceResponse.getId().equals(invoice.getId())) {
                List<InvoiceDetailResponse> invoiceDetailResponses = MappingData.mapListObject(invoice.getInvoiceDetails(), InvoiceDetailResponse.class);
                invoiceDetailResponses.forEach(invoiceDetailResponse -> invoice.getInvoiceDetails().forEach(
                        invoiceDetail -> invoiceDetailResponse.setProductResponse(MappingData.mapObject(invoiceDetail.getProduct(), ProductResponse.class))
                ));
                invoiceResponse.setCustomerResponse(MappingData.mapObject(invoice.getCustomer(), CustomerResponse.class));
                invoiceResponse.setInvoiceDetailResponses(invoiceDetailResponses);
            }
        }));
        return invoiceResponses;
    }

    private void checkToReduceProduct(Status status, Invoice invoice) {
        if (status.equals(Status.PAID)) {
            for (InvoiceDetail invoiceDetail : invoice.getInvoiceDetails()) {
                if (invoiceDetail.getProduct().getType().equals(ProductType.PRODUCT)) {
                    invoiceDetail.getProduct().setQuantity(invoiceDetail.getProduct().getQuantity() - invoiceDetail.getTotalQuantity());
                    this.productRepository.save(invoiceDetail.getProduct());
                }
            }
        }
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        InvoiceResponse invoiceResponse = MappingData.mapObject(invoice, InvoiceResponse.class);
        List<InvoiceDetailResponse> invoiceDetailResponses = MappingData.mapListObject(invoice.getInvoiceDetails(), InvoiceDetailResponse.class);
        invoiceDetailResponses.forEach(invoiceDetailResponse -> invoice.getInvoiceDetails().forEach(
                invoiceDetail -> invoiceDetailResponse.setProductResponse(MappingData.mapObject(invoiceDetail.getProduct(), ProductResponse.class))
        ));
        invoiceResponse.setCustomerResponse(MappingData.mapObject(invoice.getCustomer(), CustomerResponse.class));
        invoiceResponse.setInvoiceDetailResponses(invoiceDetailResponses);
        return invoiceResponse;
    }
}
