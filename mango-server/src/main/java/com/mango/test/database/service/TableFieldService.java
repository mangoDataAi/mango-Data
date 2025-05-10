package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.TableField;

import java.util.List;
import java.util.Map;

/**
 * 表字段服务接口
 */
public interface TableFieldService extends IService<TableField> {

    
    /**
     * 根据表ID获取字段列表
     * @param tableId 表ID
     * @return 字段列表
     */
    List<TableField> getFieldsByTableId(String tableId);

    /**
     * 根据表ID获取主键字段列表
     * @param tableId 表ID
     * @return 主键字段列表
     */
    List<TableField> getPrimaryKeysByTableId(String tableId);


} 