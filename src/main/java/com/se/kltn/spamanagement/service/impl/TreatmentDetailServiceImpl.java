package com.se.kltn.spamanagement.service.impl;

import com.se.kltn.spamanagement.dto.request.TreatmentDetailRequest;
import com.se.kltn.spamanagement.dto.response.CustomerResponse;
import com.se.kltn.spamanagement.dto.response.ProductResponse;
import com.se.kltn.spamanagement.dto.response.TreatmentDetailResponse;
import com.se.kltn.spamanagement.exception.ResourceNotFoundException;
import com.se.kltn.spamanagement.model.Customer;
import com.se.kltn.spamanagement.model.Product;
import com.se.kltn.spamanagement.model.TreatmentDetail;
import com.se.kltn.spamanagement.model.TreatmentDetailId;
import com.se.kltn.spamanagement.constants.enums.Status;
import com.se.kltn.spamanagement.repository.CustomerRepository;
import com.se.kltn.spamanagement.repository.EmployeeRepository;
import com.se.kltn.spamanagement.repository.ProductRepository;
import com.se.kltn.spamanagement.repository.TreatmentDetailRepository;
import com.se.kltn.spamanagement.service.TreatmentDetailService;
import com.se.kltn.spamanagement.utils.MappingData;
import com.se.kltn.spamanagement.utils.NullUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static com.se.kltn.spamanagement.constants.ErrorMessage.*;

@Service
@Log4j2
public class TreatmentDetailServiceImpl implements TreatmentDetailService {

    private final TreatmentDetailRepository treatmentDetailRepository;

    private final CustomerRepository customerRepository;

    private final ProductRepository productRepository;

    private final EmployeeRepository employeeRepository;

    @Autowired
    public TreatmentDetailServiceImpl(TreatmentDetailRepository treatmentDetailRepository, CustomerRepository customerRepository, ProductRepository productRepository, EmployeeRepository employeeRepository) {
        this.treatmentDetailRepository = treatmentDetailRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public TreatmentDetailResponse addTreatmentDetail(TreatmentDetailId treatmentDetailId, TreatmentDetailRequest treatmentDetailRequest) {
        log.debug("add treatment detail");
        TreatmentDetail treatmentDetail = MappingData.mapObject(treatmentDetailRequest, TreatmentDetail.class);
        treatmentDetail.setProduct(getTreatmentById(treatmentDetailId.getProductId()));
        treatmentDetail.setCustomer(getCustomerById(treatmentDetailId.getCustomerId()));
        treatmentDetail.setTreatmentDetailId(treatmentDetailId);
        treatmentDetail.setStatus(Status.NEW);
        treatmentDetail.setCreatedDate(new Date());
        treatmentDetail.setEmployee(this.employeeRepository.findById(treatmentDetailRequest.getIdEmployee()).orElseThrow(
                () -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND)));
        return mapToTreatmentDetailResponse(this.treatmentDetailRepository.save(treatmentDetail));
    }


    @Override
    public TreatmentDetailResponse updateTreatmentDetail(TreatmentDetailId treatmentDetailId, TreatmentDetailRequest treatmentDetailRequest) {
        log.debug("update treatment detail");
        TreatmentDetail treatmentDetail = this.treatmentDetailRepository.findById(treatmentDetailId).orElseThrow(
                () -> new ResourceNotFoundException(TREATMENT_DETAIL_NOT_FOUND));
        NullUtils.updateIfPresent(treatmentDetail::setNote, treatmentDetailRequest.getNote());
        NullUtils.updateIfPresent(treatmentDetail::setImageBefore, treatmentDetailRequest.getImageBefore());
        NullUtils.updateIfPresent(treatmentDetail::setStatus, treatmentDetailRequest.getStatus());
        NullUtils.updateIfPresent(treatmentDetail::setImageCurrent, treatmentDetailRequest.getImageCurrent());
        NullUtils.updateIfPresent(treatmentDetail::setImageResult, treatmentDetailRequest.getImageAfter());
        treatmentDetail.setUpdatedDate(new Date());
        return mapToTreatmentDetailResponse(this.treatmentDetailRepository.save(treatmentDetail));
    }

    @Override
    public List<TreatmentDetailResponse> getTreatmentDetailByCustomer(Long customerId) {
        log.debug("get treatment detail by customer have id: " + customerId);
        List<TreatmentDetail> treatmentDetails = this.treatmentDetailRepository.getTreatmentDetailsByCustomer_Id(customerId);
        return mappingTreatmentDetails(treatmentDetails);
    }


    @Override
    public List<TreatmentDetailResponse> getListTreatmentDetail(int page, int size) {
        log.debug("get list treatment detail");
        Pageable pageable = PageRequest.of(page, size);
        List<TreatmentDetail> treatmentDetails = this.treatmentDetailRepository.findAll(pageable).getContent();
        return mappingTreatmentDetails(treatmentDetails);
    }

    @Override
    public void deleteTreatmentDetail(TreatmentDetailId treatmentDetailId) {
        log.debug("delete treatment detail");
        TreatmentDetail treatmentDetail = this.treatmentDetailRepository.findById(treatmentDetailId).orElseThrow(
                () -> new ResourceNotFoundException(TREATMENT_DETAIL_NOT_FOUND));
        this.treatmentDetailRepository.delete(treatmentDetail);
    }

    private Customer getCustomerById(Long customerId) {
        return this.customerRepository.findById(customerId).orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND));
    }

    private Product getTreatmentById(Long productId) {
        return this.productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND));
    }

    private List<TreatmentDetailResponse> mappingTreatmentDetails(List<TreatmentDetail> treatmentDetails) {
        List<TreatmentDetailResponse> treatmentDetailResponses = MappingData.mapListObject(treatmentDetails, TreatmentDetailResponse.class);
        treatmentDetailResponses.forEach(
                treatmentDetailResponse -> {
                    treatmentDetailResponse.setCustomerResponse(MappingData.mapObject(treatmentDetails
                            .get(treatmentDetailResponses.indexOf(treatmentDetailResponse)).getCustomer(), CustomerResponse.class));
                    treatmentDetailResponse.setProductResponse(MappingData.mapObject(treatmentDetails
                            .get(treatmentDetailResponses.indexOf(treatmentDetailResponse)).getProduct(), ProductResponse.class));
                }
        );
        return treatmentDetailResponses;
    }

    private TreatmentDetailResponse mapToTreatmentDetailResponse(TreatmentDetail treatmentDetail) {
        TreatmentDetailResponse treatmentDetailResponse = MappingData.mapObject(treatmentDetail, TreatmentDetailResponse.class);
        treatmentDetailResponse.setCustomerResponse(MappingData.mapObject(treatmentDetail.getCustomer(), CustomerResponse.class));
        treatmentDetailResponse.setProductResponse(MappingData.mapObject(treatmentDetail.getProduct(), ProductResponse.class));
        return treatmentDetailResponse;
    }
}
