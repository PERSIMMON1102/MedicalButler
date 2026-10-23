package com.syd.medicalbutler.dto;


import lombok.Data;

import java.util.List;

@Data
public class OrderGetResponse {
    private Integer id;
    private Integer main_id;
    private Integer user_id;
    private String item_name;
    private String date;
    private Double total;
    private String status;
    private String comment;
    private String pic;
    private List<ProjectItemDto> items;
}
