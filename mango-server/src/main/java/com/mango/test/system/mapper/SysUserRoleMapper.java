package com.mango.test.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联表数据访问接口
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
} 