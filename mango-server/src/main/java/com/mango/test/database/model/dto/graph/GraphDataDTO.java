package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.List;

/**
 * 图数据DTO
 */
@Data
public class GraphDataDTO {
    
    /**
     * 节点列表
     */
    private List<NodeDTO> nodes;
    
    /**
     * 关系列表
     */
    private List<RelationshipDTO> relationships;
} 