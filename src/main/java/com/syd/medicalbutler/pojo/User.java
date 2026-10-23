package com.syd.medicalbutler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    @TableField(value = "name")
    private String name;

    /**
     * 
     */
    @TableField(value = "pass")
    private String pass;

    /**
     * 
     */
    @TableField(value = "power")
    private Integer power;

    /**
     * 
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 
     */
    @TableField(value = "department_id")
    private Integer departmentId;

    /**
     * 
     */
    @TableField(value = "createdate")
    private String createdate;

    /**
     * 
     */
    @TableField(value = "isdel")
    private Integer isdel;
}