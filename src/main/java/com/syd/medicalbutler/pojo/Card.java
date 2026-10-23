package com.syd.medicalbutler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName card
 */
@TableName(value ="card")
@Data
public class Card {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    @TableField(value = "code")
    private String code;

    /**
     * 
     */
    @TableField(value = "count")
    private Double count;

    /**
     * 
     */
    @TableField(value = "tocount")
    private Double tocount;

    /**
     * 
     */
    @TableField(value = "createdate")
    private String createdate;

    /**
     * 
     */
    @TableField(value = "client_id")
    private Integer clientId;

    /**
     * 
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 
     */
    @TableField(value = "price")
    private Double price;

    /**
     * 
     */
    @TableField(value = "user_id")
    private Integer userId;

    /**
     * 
     */
    @TableField(value = "isdel")
    private Integer isdel;

    /**
     * 
     */
    @TableField(value = "payinfo_id")
    private Integer payinfoId;

    /**
     * 
     */
    @TableField(value = "complte")
    private Integer complte;
}