package com.mango.test.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.system.dto.SysRoleDTO;
import com.mango.test.system.dto.SysRoleQueryDTO;
import com.mango.test.system.entity.SysRole;

import java.util.List;
import java.util.Set;

/**
 * 角色服务接口
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 分页查询角色列表
     * 
     * @param queryDTO 查询条件
     * @return 角色信息分页结果
     */
    IPage<SysRole> pageRoles(SysRoleQueryDTO queryDTO);

    /**
     * 根据ID获取角色信息
     * 
     * @param id 角色ID
     * @return 角色信息
     */
    SysRoleDTO getRoleById(String id);

    /**
     * 新增角色
     * 
     * @param roleDTO 角色信息
     * @return 结果
     */
    String insertRole(SysRoleDTO roleDTO);

    /**
     * 修改角色
     * 
     * @param roleDTO 角色信息
     * @return 结果
     */
    boolean updateRole(SysRoleDTO roleDTO);

    /**
     * 批量删除角色
     * 
     * @param ids 角色ID数组
     * @return 结果
     */
    boolean deleteRoleByIds(List<String> ids);

    /**
     * 更新角色状态
     * 
     * @param id 角色ID
     * @param status 状态
     * @return 结果
     */
    boolean updateRoleStatus(String id, Integer status);

    /**
     * 角色权限分配
     * 
     * @param roleId 角色ID
     * @param menuIds 菜单ID集合
     * @return 结果
     */
    boolean assignRoleMenu(String roleId, List<String> menuIds);

    /**
     * 角色数据权限分配
     * 
     * @param roleId 角色ID
     * @param dataScope 数据范围
     * @param deptIds 部门ID集合
     * @return 结果
     */
    boolean assignRoleDataScope(String roleId, String dataScope, List<String> deptIds);
    
    /**
     * 获取角色选择框列表
     * 
     * @return 角色选择列表
     */
    List<SysRole> selectRoleOptions();
    
    /**
     * 查询用户拥有的角色ID集合
     * 
     * @param userId 用户ID
     * @return 角色ID集合
     */
    Set<String> selectRoleIdsByUserId(String userId);
    
    /**
     * 分配用户角色
     * 
     * @param userId 用户ID
     * @param roleIds 角色ID集合
     * @return 结果
     */
    boolean assignUserRoles(String userId, List<String> roleIds);
} 