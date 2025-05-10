package com.mango.test.vo;

import lombok.Data;
import java.util.Date;

@Data
public class UserVO {
    private Long id;
    private String username;
    private Boolean status;
    private Date createTime;
} 