package com.mango.test.dto;

import lombok.Data;
import java.util.List;

@Data
public class MaterializeRequest {
    private String sourceId;
    private List<String> tables;
} 