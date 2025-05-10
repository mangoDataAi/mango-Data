package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_file_data_source")
@ApiModel(description = "文件数据源")
public class FileDataSource extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件类型")
    private String fileType;

    @ApiModelProperty("文件大小(字节)")
    private Long fileSize;

    @ApiModelProperty("文件路径")
    private String filePath;

    @ApiModelProperty("表名")
    private String tableName;

    @ApiModelProperty("字段数量")
    private Integer fieldCount;

    @ApiModelProperty("数据量")
    private Integer dataCount;

    @ApiModelProperty("状态(0:未解析,1:已解析,2:解析失败)")
    private Integer status;

    @ApiModelProperty("错误信息")
    private String errorMsg;

    /**
     * 表注释
     */
    private String tableComment;
}
