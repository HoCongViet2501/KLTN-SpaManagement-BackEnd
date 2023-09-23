package com.se.kltn.spamanagement.service.impl;

import com.se.kltn.spamanagement.dto.request.EmployeeRequest;
import com.se.kltn.spamanagement.dto.response.EmployeeResponse;
import com.se.kltn.spamanagement.exception.ResourceNotFoundException;
import com.se.kltn.spamanagement.model.Employee;
import com.se.kltn.spamanagement.repository.EmployeeRepository;
import com.se.kltn.spamanagement.service.EmployeeService;
import com.se.kltn.spamanagement.utils.MappingData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = this.employeeRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee not found"));
        return MappingData.mapObject(employee, EmployeeResponse.class);
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest employeeRequest) {
        Employee employee = MappingData.mapObject(employeeRequest, Employee.class);
        employee.setCreatedDate(new Date());
        employee.setUpdatedDate(new Date());
        Employee employeeCreated = this.employeeRepository.save(employee);
        return MappingData.mapObject(employeeCreated, EmployeeResponse.class);
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest employeeRequest) {
        Employee employee = this.employeeRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee not found"));
        employee.setFirstName(employeeRequest.getFirstName());
        employee.setLastName(employeeRequest.getLastName());
        employee.setAddress(employeeRequest.getAddress());
        employee.setPhoneNumber(employeeRequest.getPhoneNumber());
        employee.setEmail(employeeRequest.getEmail());
        employee.setSalaryGross(employeeRequest.getSalaryGross());
        employee.setBirthDay(employeeRequest.getBirthDay());
        employee.setUpdatedDate(new Date());
        return MappingData.mapObject(this.employeeRepository.save(employee), EmployeeResponse.class);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = this.employeeRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee not found"));
        this.employeeRepository.delete(employee);
    }

    @Override
    public List<EmployeeResponse> getAllEmployeePaging(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Employee> employeePage = this.employeeRepository.findAll(pageable).getContent();
        return MappingData.mapListObject(employeePage, EmployeeResponse.class);
    }
}
