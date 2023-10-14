package com.se.kltn.spamanagement.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.se.kltn.spamanagement.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentRequest {

    @NotNull(message = "time is required")
    @JsonFormat(pattern="yyyy-MM-dd")
    private Date time;

    private Status status;

    private String note;

    @NotNull(message = "idEmployee is required")
    private Long idEmployee;

    @NotNull(message = "idProduct is required")
    private Long idProduct;

    @NotNull(message = "idCustomer is required")
    private Long idCustomer;
}
