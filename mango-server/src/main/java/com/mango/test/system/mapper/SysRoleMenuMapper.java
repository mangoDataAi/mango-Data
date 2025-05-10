package com.mango.test.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.system.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联数据访问接口
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
} 