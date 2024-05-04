package com.se.kltn.spamanagement.service.impl;

import com.se.kltn.spamanagement.dto.request.SendMailRequest;
import com.se.kltn.spamanagement.dto.response.AppointmentResponse;
import com.se.kltn.spamanagement.model.Appointment;
import com.se.kltn.spamanagement.repository.AppointmentRepository;
import com.se.kltn.spamanagement.service.EmailService;
import com.se.kltn.spamanagement.utils.MappingData;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class SchedulerService {

    private final AppointmentRepository appointmentRepository;

    private final KafkaTemplate<String, AppointmentResponse> kafkaTemplate;

    private final EmailService emailService;

    @Autowired
    public SchedulerService(AppointmentRepository appointmentRepository, KafkaTemplate<String, AppointmentResponse> kafkaTemplate, EmailService emailService) {
        this.appointmentRepository = appointmentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 3600000)
    public void checkAndSendToKafka(){
        log.info("Check and send to kafka");
        Timestamp start = Timestamp.valueOf(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        Timestamp end = Timestamp.valueOf(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now().plusHours(2)));
        List<Appointment> appointments = appointmentRepository.findAppointmentsByTimeUpComing2Hours(start, end);
        List<AppointmentResponse> appointmentResponses= MappingData.mapListObject(appointments, AppointmentResponse.class);
        appointmentResponses.forEach(appointmentResponse -> appointmentResponse.setReference(getReference(appointments.get(appointmentResponses.indexOf(appointmentResponse)))));
        appointmentResponses.forEach(this::sendAppointmentToBroker);
    }

    private void sendAppointmentToBroker(AppointmentResponse appointmentResponse) {
        this.kafkaTemplate.send("appointment", appointmentResponse);
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
                    .msgBody("Dear " + customerName + "\n" + "You have an appointment at VH SPA at " + appointmentResponse.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .subject("Appointment Alert")
                    .build();
            emailService.sendSimpleEmail(customerId, sendMailRequest);
        }
    }
    private Map<String, String> getReference(Appointment appointmentSaved) {
        Map<String, String> map = new HashMap<>();
        map.put("customerId", String.valueOf(appointmentSaved.getCustomer().getId()));
        map.put("employeeId", String.valueOf(appointmentSaved.getEmployee().getId()));
        map.put("productId", String.valueOf(appointmentSaved.getProduct().getId()));
        map.put("customerName", appointmentSaved.getCustomer().getFirstName() + " " + appointmentSaved.getCustomer().getLastName());
        map.put("employeeName", appointmentSaved.getEmployee().getFirstName() + " " + appointmentSaved.getEmployee().getLastName());
        map.put("productName", appointmentSaved.getProduct().getName());
        return map;
    }
}
