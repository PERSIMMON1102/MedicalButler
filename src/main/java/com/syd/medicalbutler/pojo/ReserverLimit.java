package com.syd.medicalbutler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName reserver_limit
 */
@TableName(value ="reserver_limit")
@Data
public class ReserverLimit {
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
    @TableField(value = "time")
    private String time;

    /**
     * 
     */
    @TableField(value = "allcount")
    private Integer allcount;

    /**
     * 
     */
    @TableField(value = "count")
    private Integer count;

    /**
     * 
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 
     */
    @TableField(value = "isdel")
    private Integer isdel;
}