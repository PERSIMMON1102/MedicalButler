package com.syd.medicalbutler.dto;

import lombok.Data;

@Data
public class OrderDetailResponse {
    private Integer id;
    private Integer mainId;
    private String date;
    private String info;
    private Integer itemId;
    private Integer status;
    private Integer userId;
    private Integer isdel;
    private String comment;
}
