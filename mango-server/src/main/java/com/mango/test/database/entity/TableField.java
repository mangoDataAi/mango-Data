package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 表字段实体
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("data_md_table_field")
public class TableField extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属表ID
     */
    private String tableId;

    /**
     * 字段名
     */
    private String name;

    /**
     * 字段显示名
     */
    private String displayName;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 字段长度
     */
    private Integer length;

    /**
     * 精度
     */
    private Integer precision;

    /**
     * 小数位数
     */
    private Integer scale;

    /**
     * 是否为主键
     */
    private Integer isPrimary;

    /**
     * 是否可为空
     */
    private Integer notNull;

    /**
     * 是否自动递增
     */
    private Integer mgautoIncrement;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 字段注释
     */
    private String mgcomment;

    /**
     * 字段排序
     */
    private Integer fieldOrder;

    public Integer getPk() {
        return this.getIsPrimary();
    }

    public String getComment() {
        return this.getMgcomment();
    }
    
    /**
     * 获取字段名称
     * @return 字段名称
     */
    public String getFieldName() {
        return this.name;
    }
    
    /**
     * 获取字段类型
     * @return 字段类型
     */
    public String getFieldType() {
        return this.type;
    }
    
    /**
     * 判断字段是否可为空
     * @return 如果notNull=1则返回false，否则返回true
     */
    public boolean isNullable() {
        // notNull=1表示非空，所以isNullable应该返回相反的值
        return this.notNull == null || this.notNull != 1;
    }

    /**
     * 判断字段是否为主键
     * @return 如果isPrimary=1则返回true，否则返回false
     */
    public boolean isPrimaryKey() {
        return this.isPrimary != null && this.isPrimary == 1;
    }
}
