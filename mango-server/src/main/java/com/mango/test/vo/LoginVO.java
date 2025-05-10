package com.mango.test.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.List;

@Data
@Accessors(chain = true)
public class LoginVO {
    private String token;
    private UserVO user;
    private List<MenuVO> menus;
    private List<String> permissions;
} 