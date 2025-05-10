package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.Dimension;
import com.mango.test.database.entity.DimensionAttribute;
import com.mango.test.database.entity.DimensionStandard;
import java.util.List;

public interface DimensionService extends IService<Dimension> {
    
    List<Dimension> getDimensionsByDomain(String domainId);
    
    List<Dimension> getDimensionsByType(String type);
    
    List<DimensionAttribute> getDimensionAttributes(String dimensionId);
    
    List<DimensionStandard> getDimensionStandards(String dimensionId);
    
    void addDimensionAttribute(DimensionAttribute attribute);
    
    void updateDimensionAttribute(DimensionAttribute attribute);
    
    void deleteDimensionAttribute(String attributeId);
    
    void linkStandard(DimensionStandard dimensionStandard);
    
    void unlinkStandard(String dimensionId, String standardId);
    
    void updateAttributeCount(String dimensionId);
} 