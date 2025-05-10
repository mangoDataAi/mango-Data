package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_file_field")
@ApiModel(description = "文件字段信息")
public class FileField extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("数据源ID")
    private String sourceId;

    @ApiModelProperty("字段名")
    private String fieldName;

    @ApiModelProperty("字段类型")
    private String fieldType;

    @ApiModelProperty("字段长度")
    private Integer fieldLength;

    @ApiModelProperty("精度")
    private Integer precision;

    @ApiModelProperty("小数位数")
    private Integer scale;

    @ApiModelProperty("是否为空")
    private Boolean nullable;

    @ApiModelProperty("字段描述")
    private String description;

    @ApiModelProperty("排序号")
    private Integer orderNum;
}
