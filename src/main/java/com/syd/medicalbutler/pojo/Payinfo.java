package com.syd.medicalbutler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName payinfo
 */
@TableName(value ="payinfo")
@Data
public class Payinfo {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    @TableField(value = "date")
    private String date;

    /**
     * 
     */
    @TableField(value = "client_id")
    private Integer clientId;

    /**
     * 
     */
    @TableField(value = "project_id")
    private Integer projectId;

    /**
     * 
     */
    @TableField(value = "price")
    private Double price;

    /**
     * 
     */
    @TableField(value = "paystatus")
    private Integer paystatus;

    /**
     * 
     */
    @TableField(value = "isdel")
    private Integer isdel;
}