package com.mango.test.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String username;

    private String password;
    
    private String realName;
    
    private String gender;
    
    private String mobile;
    
    private String email;
    
    private String organizationId;
    
    @TableField(exist = false)
    private String organizationName;
    
    @TableField(exist = false)
    private List<String> roleIds;
    
    @TableField(exist = false)
    private String roleNames;
    
    private String status;
    
    private String remark;

    private Date createTime;

    private Date updateTime;
    
    private String creator;
    
    private String updater;
}
