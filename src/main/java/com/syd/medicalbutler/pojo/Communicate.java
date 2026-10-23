package com.syd.medicalbutler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName communicate
 */
@TableName(value ="communicate")
@Data
public class Communicate {
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
    @TableField(value = "main_id")
    private Integer mainId;

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
    @TableField(value = "comments")
    private String comments;
}