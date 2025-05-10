package com.mango.test.database.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_api_datasource")
public class ApiDataSource extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @com.baomidou.mybatisplus.annotation.TableField("NAME")
    private String name;

    @com.baomidou.mybatisplus.annotation.TableField("METHOD")
    private String method;

    @com.baomidou.mybatisplus.annotation.TableField("URL")
    private String url;

    @com.baomidou.mybatisplus.annotation.TableField("HEADERS")
    @JsonRawValue
    private String headers;

    @com.baomidou.mybatisplus.annotation.TableField("REQUEST_BODY")
    private String requestBody;

    @com.baomidou.mybatisplus.annotation.TableField("UPDATE_INTERVAL")
    private String updateInterval;

    @com.baomidou.mybatisplus.annotation.TableField("SUCCESS_CONDITION")
    private String successCondition;

    @com.baomidou.mybatisplus.annotation.TableField("DESCRIPTION")
    private String description;

    @com.baomidou.mybatisplus.annotation.TableField("STATUS")
    private String status;

}
