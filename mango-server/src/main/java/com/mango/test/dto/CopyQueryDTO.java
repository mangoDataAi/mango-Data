package com.mango.test.dto;

import com.mango.test.database.entity.ColumnInfo;
import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

@Data
public class CopyQueryDTO {
    /**
     * 源数据源ID
     */
    @NotNull(message = "源数据源ID不能为空")
    private String sourceId;

    /**
     * 目标数据源ID列表
     */
    @NotEmpty(message = "请选择目标数据源")
    private List<String> targetIds;

    /**
     * 目标表名
     */
    @NotBlank(message = "目标表名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "表名必须以字母开头，只能包含字母、数字和下划线")
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 表已存在时的处理策略：1-不覆盖，2-覆盖，3-自动增加后缀
     */
    @NotNull(message = "请选择表已存在时的处理策略")
    @Min(value = 1, message = "无效的处理策略")
    @Max(value = 3, message = "无效的处理策略")
    private Integer existsStrategy;

    /**
     * SQL查询语句
     */
    @NotBlank(message = "SQL语句不能为空")
    private String sql;

    private List<ColumnInfo> columns;
    private List<Map<String, Object>> data;
} 