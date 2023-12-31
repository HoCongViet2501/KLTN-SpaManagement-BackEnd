package com.se.kltn.spamanagement.dto.response;

import com.se.kltn.spamanagement.constants.enums.ProductType;
import com.se.kltn.spamanagement.constants.enums.Status;
import lombok.Data;

import java.util.Date;

@Data
public class ProductResponse {

    private Long id;

    private String name;

    private Double price;

    private Integer quantity;

    private Status status;

    private String imageUrl;

    private ProductType productType;

    private String supplier;

    private String category;

    private String unit;

    private String description;

    private Date createdDate;

    private Date updatedDate;

    public String getProductType() {
        return productType.name();
    }

    public String getStatus() {
        return status.name();
    }


}
