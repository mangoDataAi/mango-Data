package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.common.util.UuidUtil;
import com.mango.test.database.entity.Dimension;
import com.mango.test.database.entity.DimensionAttribute;
import com.mango.test.database.entity.DimensionStandard;
import com.mango.test.database.mapper.DimensionAttributeMapper;
import com.mango.test.database.mapper.DimensionMapper;
import com.mango.test.database.mapper.DimensionStandardMapper;
import com.mango.test.database.service.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class DimensionServiceImpl extends ServiceImpl<DimensionMapper, Dimension> implements DimensionService {
    
    @Autowired
    private DimensionAttributeMapper attributeMapper;
    
    @Autowired
    private DimensionStandardMapper standardMapper;
    
    @Override
    public List<Dimension> getDimensionsByDomain(String domainId) {
        return this.list(new LambdaQueryWrapper<Dimension>()
                .eq(Dimension::getDomainId, domainId));
    }
    
    @Override
    public List<Dimension> getDimensionsByType(String type) {
        return this.list(new LambdaQueryWrapper<Dimension>()
                .eq(Dimension::getType, type));
    }
    
    @Override
    public List<DimensionAttribute> getDimensionAttributes(String dimensionId) {
        return attributeMapper.selectByDimensionId(dimensionId);
    }
    
    @Override
    public List<DimensionStandard> getDimensionStandards(String dimensionId) {
        return standardMapper.selectByDimensionId(dimensionId);
    }
    
    @Override
    @Transactional
    public void addDimensionAttribute(DimensionAttribute attribute) {
        attribute.setId(UuidUtil.generateUuid());
        attribute.setCreateTime(new Date());
        attributeMapper.insert(attribute);
        updateAttributeCount(attribute.getDimensionId());
    }
    
    @Override
    @Transactional
    public void updateDimensionAttribute(DimensionAttribute attribute) {
        attribute.setUpdateTime(new Date());
        attributeMapper.updateById(attribute);
    }
    
    @Override
    @Transactional
    public void deleteDimensionAttribute(String attributeId) {
        DimensionAttribute attribute = attributeMapper.selectById(attributeId);
        if (attribute != null) {
            attributeMapper.deleteById(attributeId);
            updateAttributeCount(attribute.getDimensionId());
        }
    }
    
    @Override
    @Transactional
    public void linkStandard(DimensionStandard dimensionStandard) {
        dimensionStandard.setId(UuidUtil.generateUuid());
        dimensionStandard.setCreateTime(new Date());
        standardMapper.insert(dimensionStandard);
    }
    
    @Override
    @Transactional
    public void unlinkStandard(String dimensionId, String standardId) {
        standardMapper.delete(new LambdaQueryWrapper<DimensionStandard>()
                .eq(DimensionStandard::getDimensionId, dimensionId)
                .eq(DimensionStandard::getStandardId, standardId));
    }
    
    @Override
    public void updateAttributeCount(String dimensionId) {
        Long count = attributeMapper.selectCount(new LambdaQueryWrapper<DimensionAttribute>()
                .eq(DimensionAttribute::getDimensionId, dimensionId));
                
        this.update(new LambdaUpdateWrapper<Dimension>()
                .eq(Dimension::getId, dimensionId)
                .set(Dimension::getAttributeCount, count.intValue()));
    }
} 