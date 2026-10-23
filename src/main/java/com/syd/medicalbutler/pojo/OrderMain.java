package com.syd.medicalbutler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName order_main
 */
@TableName(value ="order_main")
@Data
public class OrderMain {
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
    @TableField(value = "info")
    private String info;

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
    @TableField(value = "check_user_id")
    private Integer checkUserId;

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
}