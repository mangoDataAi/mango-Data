package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 任务中的表配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "data_task_table", autoResultMap = true)
public class TaskTable extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的任务ID
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 关联的版本ID
     */
    @TableField("version_id")
    private String versionId;

    /**
     * 表ID
     */
    @TableField("table_id")
    private String tableId;

    /**
     * 表名称
     */
    @TableField("table_name")
    private String tableName;

    /**
     * 表注释
     */
    @TableField("table_comment")
    private String tableComment;

    /**
     * 所属模型ID
     */
    @TableField("model_id")
    private String modelId;

    /**
     * 所属模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 是否是主表
     */
    @TableField("is_main")
    private Boolean isMain;

    /**
     * 物化类型：full, incremental
     */
    @TableField("materialize_type")
    private String materializeType;

    /**
     * 增量字段
     */
    @TableField("incremental_field")
    private String incrementalField;

    /**
     * 写入模式：append, overwrite
     */
    @TableField("write_mode")
    private String writeMode;

    /**
     * 依赖表IDs
     */
    @TableField(value = "dependencies", typeHandler = JacksonTypeHandler.class)
    private String dependencies;

    /**
     * 状态：waiting, running, completed, failed
     */
    @TableField("status")
    private String status;

    public String getDependencies() {
        return dependencies;
    }

    public void setDependencies(String dependencies) {
        this.dependencies = dependencies;
    }

    public void setDependenciesList(List<String> dependenciesList) {
        if (dependenciesList != null && !dependenciesList.isEmpty()) {
            this.dependencies = String.join(",", dependenciesList);
        } else {
            this.dependencies = null;
        }
    }

    public List<String> getDependenciesList() {
        if (StringUtils.isBlank(this.dependencies)) {
            return Collections.emptyList();
        }
        return Arrays.asList(this.dependencies.split(","));
    }
} 