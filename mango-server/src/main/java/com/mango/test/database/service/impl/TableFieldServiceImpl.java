package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.database.entity.TableField;
import com.mango.test.database.mapper.TableFieldMapper;
import com.mango.test.database.service.TableFieldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 表字段服务实现类
 */
@Service
public class TableFieldServiceImpl extends ServiceImpl<TableFieldMapper, TableField> implements TableFieldService {

    private static final Logger log = LoggerFactory.getLogger(TableFieldServiceImpl.class);

    @Override
    public List<TableField> getFieldsByTableId(String tableId) {
        if (tableId == null || tableId.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            LambdaQueryWrapper<TableField> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TableField::getTableId, tableId);
            queryWrapper.orderByAsc(TableField::getFieldOrder);
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取表 [{}] 的字段列表失败: {}", tableId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<TableField> getPrimaryKeysByTableId(String tableId) {
        if (tableId == null || tableId.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<TableField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TableField::getTableId, tableId);
        queryWrapper.eq(TableField::getIsPrimary, 1);
        queryWrapper.orderByAsc(TableField::getFieldOrder);
        return list(queryWrapper);
    }
}