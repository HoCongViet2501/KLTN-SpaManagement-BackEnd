package com.se.kltn.spamanagement.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.se.kltn.spamanagement.dto.request.AppointmentRequest;
import com.se.kltn.spamanagement.dto.request.SendMailRequest;
import com.se.kltn.spamanagement.dto.response.AppointmentResponse;
import com.se.kltn.spamanagement.service.AppointmentService;
import com.se.kltn.spamanagement.service.EmailService;
import com.se.kltn.spamanagement.service.EmployeeService;
import com.se.kltn.spamanagement.utils.JsonConverter;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@RestController
@RequestMapping("/api/appointment")
@Log4j2
public class AppointmentController {

    private final AppointmentService appointmentService;

    private final EmployeeService employeeService;

    private final EmailService emailService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService, EmployeeService employeeService, EmailService emailService) {
        this.appointmentService = appointmentService;
        this.employeeService = employeeService;
        this.emailService = emailService;
    }

    @GetMapping
    @Operation(summary = "get list appointment")
    public ResponseEntity<Object> getListAppointment(@RequestParam(defaultValue = "0", value = "page", required = false) int page,
                                                     @RequestParam(defaultValue = "10", value = "size", required = false) int size) {
        return ResponseEntity.ok().body(this.appointmentService.getAllAppointment(page, size));
    }

    @PostMapping
    @Operation(summary = "create appointment")
    public ResponseEntity<Object> createAppointment(@Valid @RequestBody AppointmentRequest appointmentRequest) throws ParseException {
        return ResponseEntity.ok().body(this.appointmentService.createAppointment(appointmentRequest));
    }

    @PutMapping("/{id}")
    @Operation(summary = "update appointment")
    public ResponseEntity<Object> updateAppointment(@PathVariable Long id, @RequestBody AppointmentRequest appointmentRequest) {
        return ResponseEntity.ok().body(this.appointmentService.updateAppointment(id, appointmentRequest));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete appointment")
    public ResponseEntity<String> deleteAppointment(@PathVariable Long id) {
        this.appointmentService.deleteAppointment(id);
        return ResponseEntity.ok().body("Appointment deleted");
    }

    @GetMapping("/customer/{idCustomer}")
    @Operation(summary = "get list appointment by customerId")
    public ResponseEntity<Object> getListAppointmentByCustomer(@PathVariable Long idCustomer,
                                                               @RequestParam(defaultValue = "0", value = "page", required = false) int page,
                                                               @RequestParam(defaultValue = "10", value = "size", required = false) int size) {
        return ResponseEntity.ok().body(this.appointmentService.getAllAppointmentByCustomer(idCustomer, page, size));
    }

    @GetMapping("/search/employee/text")
    @Operation(summary = "search employee is therapist by text")
    public ResponseEntity<Object> getEmployeeIsTherapistByText(@RequestParam("username") String text,
                                                               @RequestParam(defaultValue = "0", value = "page", required = false) int page,
                                                               @RequestParam(defaultValue = "10", value = "size", required = false) int size) {
        return ResponseEntity.ok().body(this.employeeService.searchEmployeeIsTherapistByText(text));
    }

    @KafkaListener(topics = "appointment", groupId = "spa-management-group-id")
    public void listen(AppointmentResponse appointmentResponse) {
        log.info("Received message in group foo: " + appointmentResponse);
        LocalDateTime localDateTime = LocalDateTime.now();
        boolean isAlertDate = appointmentResponse.getTime().minusHours(1).isBefore(localDateTime);
        if (isAlertDate) {
            log.info("Send message to employee");
            String customerName = appointmentResponse.getReference().get("customerName");
            Long customerId = Long.parseLong(appointmentResponse.getReference().get("customerId"));
            SendMailRequest sendMailRequest = SendMailRequest.builder()
                    .msgBody("Dear " + customerName + "\n" + "You have an appointment at " + appointmentResponse.getTime())
                    .subject("Appointment Alert")
                    .build();
            emailService.sendSimpleEmail(customerId, sendMailRequest);
        }
    }
}
