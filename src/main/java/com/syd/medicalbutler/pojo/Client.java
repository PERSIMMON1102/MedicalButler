package com.syd.medicalbutler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName client
 */
@TableName(value ="client")
@Data
public class Client {
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


    @TableField(value= "power")
    private Integer power;
    /**
     * 
     */
    @TableField(value = "nike")
    private String nike;

    /**
     * 
     */
    @TableField(value = "pass")
    private String pass;

    /**
     * 
     */
    @TableField(value = "weixin")
    private String weixin;

    /**
     * 
     */
    @TableField(value = "sex")
    private Integer sex;

    /**
     * 
     */
    @TableField(value = "qq")
    private String qq;

    /**
     * 
     */
    @TableField(value = "tel")
    private String tel;

    /**
     * 
     */
    @TableField(value = "tel2")
    private String tel2;

    /**
     * 
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 
     */
    @TableField(value = "level_id")
    private Integer levelId;

    /**
     * 
     */
    @TableField(value = "point")
    private Integer point;

    /**
     * 
     */
    @TableField(value = "srcinfo")
    private String srcinfo;

    /**
     * 
     */
    @TableField(value = "createdate")
    private String createdate;

    /**
     * 
     */
    @TableField(value = "card_id")
    private Integer cardId;

    /**
     * 
     */
    @TableField(value = "isdel")
    private Integer isdel;

    @TableField(value = "avatar")
    private String avatar;

    /**
     * 
     */
    @TableField(value = "comment")
    private String comment;

    static String[]STATUS={"空闲","预约","签到","咨询"};
    public String getStatusname(){return STATUS[status];}

}