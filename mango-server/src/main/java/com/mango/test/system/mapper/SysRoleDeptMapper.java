package com.mango.test.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.system.entity.SysRoleDept;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色部门关联数据访问接口
 */
@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {
} 