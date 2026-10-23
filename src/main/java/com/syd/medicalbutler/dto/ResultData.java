package com.syd.medicalbutler.dto;

import lombok.Data;

@Data
public class ResultData {

    private  int status;
    private  String msg;
    private  Object data;

    public ResultData(int status){
        this.status=status;
    }
    public ResultData(int status, String msg){
        this.status=status;
        this.msg=msg;
    }
}
