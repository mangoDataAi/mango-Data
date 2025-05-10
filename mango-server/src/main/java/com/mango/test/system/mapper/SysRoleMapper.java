package com.mango.test.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.system.entity.SysRole;
import com.mango.test.system.entity.SysRoleDept;
import com.mango.test.system.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色数据访问接口
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询角色菜单关联
     */
    List<SysRoleMenu> selectRoleMenus(@Param("ew") LambdaQueryWrapper<SysRoleMenu> queryWrapper);
    
    /**
     * 批量插入角色菜单关联
     */
    int insertRoleMenus(@Param("list") List<SysRoleMenu> roleMenus);
    
    /**
     * 删除角色菜单关联
     */
    int deleteRoleMenus(@Param("ew") LambdaQueryWrapper<SysRoleMenu> queryWrapper);
    
    /**
     * 查询角色部门关联
     */
    List<SysRoleDept> selectRoleDepts(@Param("ew") LambdaQueryWrapper<SysRoleDept> queryWrapper);
    
    /**
     * 批量插入角色部门关联
     */
    int insertRoleDepts(@Param("list") List<SysRoleDept> roleDepts);
    
    /**
     * 删除角色部门关联
     */
    int deleteRoleDepts(@Param("ew") LambdaQueryWrapper<SysRoleDept> queryWrapper);
} 