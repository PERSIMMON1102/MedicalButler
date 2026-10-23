package com.syd.medicalbutler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName reserver
 */
@TableName(value ="reserver")
@Data
public class Reserver {
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
    @TableField(value = "todate")
    private String todate;

    /**
     * 
     */
    @TableField(value = "totime")
    private String totime;

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
    @TableField(value = "status")
    private Integer status;

    /**
     * 
     */
    @TableField(value = "isdel")
    private Integer isdel;
}