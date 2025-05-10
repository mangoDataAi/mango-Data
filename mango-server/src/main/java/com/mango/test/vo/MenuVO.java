package com.mango.test.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.List;

@Data
@Accessors(chain = true)
public class MenuVO {
    private Long id;
    private String name;
    private String path;
    private String component;
    private String redirect;
    private String icon;
    private Integer sort;
    private Long parentId;
    private Boolean hidden;
    private String permission;
    private List<MenuVO> children;
} 