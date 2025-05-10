package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("data_md_table")
public class Table extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String displayName;
    private Integer fieldCount;
    private String groupId;
    
    /**
     * 表所属数据源ID
     */
    private String dataSourceId;
    
    /**
     * 表所属数据源，非数据库字段，由业务层填充
     */
    @TableField(exist = false)
    private DataSource dataSource;
    
    /**
     * 表所属的schema，非数据库字段，由业务层填充
     */
    @TableField(exist = false)
    private String schema;

    /**
     * 表的字段列表，非数据库字段，由业务层填充
     */
    @TableField(exist = false)
    private List<com.mango.test.database.entity.TableField> fields;

    public String getMgcomment() {
        return this.getComment();
    }

    public String getComment() {
        return this.getDisplayName();
    }
    
    /**
     * 获取关联的数据源
     * @return 数据源
     */
    public DataSource getDatasource() {
        return this.dataSource;
    }
    
    /**
     * 设置关联的数据源
     * @param dataSource 数据源
     */
    public void setDatasource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 获取表所属的schema
     * @return schema名称
     */
    public String getSchema() {
        return this.schema;
    }
    
    /**
     * 设置表所属的schema
     * @param schema schema名称
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }
    
    /**
     * 获取表的字段列表
     * @return 字段列表
     */
    public List<com.mango.test.database.entity.TableField> getFields() {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        return fields;
    }
    
    /**
     * 设置表的字段列表
     * @param fields 字段列表
     */
    public void setFields(List<com.mango.test.database.entity.TableField> fields) {
        this.fields = fields;
    }
}
