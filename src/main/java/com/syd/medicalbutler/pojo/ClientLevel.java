package com.syd.medicalbutler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName client_level
 */
@TableName(value ="client_level")
@Data
public class ClientLevel {
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
    @TableField(value = "info")
    private String info;

    /**
     * 
     */
    @TableField(value = "dest")
    private Integer dest;

    /**
     * 
     */
    @TableField(value = "pic")
    private String pic;

    /**
     * 
     */
    @TableField(value = "isdel")
    private Integer isdel;
}