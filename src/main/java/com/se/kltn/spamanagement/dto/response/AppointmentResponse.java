package com.se.kltn.spamanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.se.kltn.spamanagement.constants.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentResponse {

    private Long id;

    private Date time;

    private Status status;

    private String note;

    private Date createdDate;

    private Date updatedDate;

    private Map<String, String> reference;

    public void setTime(Date time) {
        this.time = new Date(time.getTime() + (1000 * 60 * 60 * 7));
    }
}
