package com.se.kltn.spamanagement.model;

import com.se.kltn.spamanagement.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoiceDetails")
public class InvoiceDetail {
    @EmbeddedId
    private InvoiceDetailId invoiceDetailId;

    @MapsId("invoiceId")
    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @MapsId("productId")
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer totalQuantity;

    private Double totalPrice;

    private Status status;
}