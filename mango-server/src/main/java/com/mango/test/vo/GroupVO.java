package com.mango.test.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(description = "分组视图对象")
public class GroupVO {

    @ApiModelProperty("分组ID")
    private String id;

    @ApiModelProperty("分组名称")
    private String name;

    @ApiModelProperty("分组描述")
    private String description;

    @ApiModelProperty("表数量")
    private Integer tableCount;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;
} 