package com.mango.test.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.system.dto.SysRoleDTO;
import com.mango.test.system.dto.SysRoleQueryDTO;
import com.mango.test.system.entity.SysRole;
import com.mango.test.system.entity.SysRoleDept;
import com.mango.test.system.entity.SysRoleMenu;
import com.mango.test.system.entity.SysUserRole;
import com.mango.test.system.mapper.SysRoleMapper;
import com.mango.test.system.service.SysRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.mango.test.system.mapper.SysRoleMenuMapper;
import com.mango.test.system.mapper.SysRoleDeptMapper;
import com.mango.test.system.mapper.SysUserRoleMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Slf4j
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysRoleDeptMapper roleDeptMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Override
    public IPage<SysRole> pageRoles(SysRoleQueryDTO queryDTO) {
        // 创建分页对象
        Page<SysRole> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        
        // 构建查询条件
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        
        // 角色名称模糊查询
        if (StringUtils.isNotBlank(queryDTO.getName())) {
            queryWrapper.like(SysRole::getName, queryDTO.getName());
        }
        
        // 角色编码模糊查询
        if (StringUtils.isNotBlank(queryDTO.getCode())) {
            queryWrapper.like(SysRole::getCode, queryDTO.getCode());
        }
        
        // 状态精确查询
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(SysRole::getStatus, queryDTO.getStatus());
        }
        
        // 创建时间范围查询
        if (StringUtils.isNotBlank(queryDTO.getBeginTime()) && StringUtils.isNotBlank(queryDTO.getEndTime())) {
            queryWrapper.between(SysRole::getCreateTime, queryDTO.getBeginTime(), queryDTO.getEndTime() + " 23:59:59");
        }
        
        // 按排序字段正序排列
        queryWrapper.orderByAsc(SysRole::getSort);
        
        // 执行查询
        return page(page, queryWrapper);
    }

    @Override
    public SysRoleDTO getRoleById(String id) {
        if (StringUtils.isBlank(id)) {
            log.warn("获取角色信息时ID为空");
            return null;
        }
        
        try {
            // 查询角色基本信息
            log.info("开始查询角色, ID: {}", id);
            SysRole role = getById(id);
            if (role == null) {
                log.warn("未找到角色, ID: {}", id);
                return null;
            }
            
            SysRoleDTO roleDTO = new SysRoleDTO();
            BeanUtils.copyProperties(role, roleDTO);
            
            // 查询角色关联的菜单ID
            try {
                List<String> menuIds = getMenuIdsByRoleId(id);
                roleDTO.setMenuIds(menuIds);
                log.info("获取角色菜单成功, 角色ID: {}, 菜单数量: {}", id, menuIds.size());
            } catch (Exception e) {
                log.error("获取角色菜单异常, 角色ID: {}", id, e);
                roleDTO.setMenuIds(new ArrayList<>());
            }
            
            // 如果是自定义数据权限，查询关联的部门ID
            if ("CUSTOM".equals(role.getDataScope())) {
                try {
                    List<String> deptIds = getDeptIdsByRoleId(id);
                    roleDTO.setDeptIds(deptIds);
                    log.info("获取角色部门成功, 角色ID: {}, 部门数量: {}", id, deptIds.size());
                } catch (Exception e) {
                    log.error("获取角色部门异常, 角色ID: {}", id, e);
                    roleDTO.setDeptIds(new ArrayList<>());
                }
            }
            
            return roleDTO;
        } catch (Exception e) {
            log.error("获取角色信息异常, ID: {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String insertRole(SysRoleDTO roleDTO) {
        try {
            log.info("开始新增角色: {}", roleDTO);
            SysRole role = new SysRole();
            BeanUtils.copyProperties(roleDTO, role);
            
            // 设置默认值
            role.setCreateTime(new Date());
            role.setUpdateTime(new Date());
            
            // 如果dataScope为空，设置默认值
            if (StringUtils.isBlank(role.getDataScope())) {
                role.setDataScope("ALL");
            }
            
            log.info("准备保存角色基本信息: {}", role);
            // 插入角色基本信息
            save(role);
            log.info("角色基本信息保存成功，ID: {}", role.getId());
            
            // 保存角色菜单关联
            if (roleDTO.getMenuIds() != null && !roleDTO.getMenuIds().isEmpty()) {
                log.info("准备保存角色菜单关联, 角色ID: {}, 菜单IDs: {}", role.getId(), roleDTO.getMenuIds());
                insertRoleMenus(role.getId(), roleDTO.getMenuIds());
            }
            
            // 如果是自定义数据权限，保存角色部门关联
            if ("CUSTOM".equals(roleDTO.getDataScope()) && roleDTO.getDeptIds() != null && !roleDTO.getDeptIds().isEmpty()) {
                log.info("准备保存角色部门关联, 角色ID: {}, 部门IDs: {}", role.getId(), roleDTO.getDeptIds());
                insertRoleDepts(role.getId(), roleDTO.getDeptIds());
            }
            
            return role.getId();
        } catch (Exception e) {
            log.error("新增角色异常: {}", roleDTO, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRoleDTO roleDTO) {
        try {
            log.info("开始更新角色, ID: {}, 角色信息: {}", roleDTO.getId(), roleDTO);
            
            // 检查入参
            if (roleDTO == null || StringUtils.isBlank(roleDTO.getId())) {
                log.error("更新角色失败: 角色ID为空");
                return false;
            }
            
            // 查询原角色信息
            SysRole existingRole = getById(roleDTO.getId());
            if (existingRole == null) {
                log.error("更新角色失败: 角色不存在, ID: {}", roleDTO.getId());
                return false;
            }
            
            SysRole role = new SysRole();
            BeanUtils.copyProperties(roleDTO, role);
            role.setUpdateTime(new Date());
            
            // 保留原有的创建信息
            role.setCreateTime(existingRole.getCreateTime());
            role.setCreator(existingRole.getCreator());
            
            log.info("更新角色基本信息, ID: {}", role.getId());
            // 更新角色基本信息
            boolean updateResult = updateById(role);
            if (!updateResult) {
                log.error("更新角色基本信息失败, ID: {}", role.getId());
                return false;
            }
            
            try {
                // 删除原有角色菜单关联
                log.info("删除原有角色菜单关联, 角色ID: {}", role.getId());
                deleteRoleMenusByRoleId(role.getId());
                
                // 保存新的角色菜单关联
                if (roleDTO.getMenuIds() != null && !roleDTO.getMenuIds().isEmpty()) {
                    log.info("保存新的角色菜单关联, 角色ID: {}, 菜单数量: {}", role.getId(), roleDTO.getMenuIds().size());
                    insertRoleMenus(role.getId(), roleDTO.getMenuIds());
                }
            } catch (Exception e) {
                log.error("处理角色菜单关联时发生异常, 角色ID: {}", role.getId(), e);
                throw e;
            }
            
            try {
                // 删除原有角色部门关联
                log.info("删除原有角色部门关联, 角色ID: {}", role.getId());
                deleteRoleDeptsByRoleId(role.getId());
                
                // 如果是自定义数据权限，保存新的角色部门关联
                if ("CUSTOM".equals(roleDTO.getDataScope()) && roleDTO.getDeptIds() != null && !roleDTO.getDeptIds().isEmpty()) {
                    log.info("保存新的角色部门关联, 角色ID: {}, 部门数量: {}", role.getId(), roleDTO.getDeptIds().size());
                    insertRoleDepts(role.getId(), roleDTO.getDeptIds());
                }
            } catch (Exception e) {
                log.error("处理角色部门关联时发生异常, 角色ID: {}", role.getId(), e);
                throw e;
            }
            
            log.info("更新角色成功, ID: {}", role.getId());
            return true;
        } catch (Exception e) {
            log.error("更新角色发生异常, 角色信息: {}", roleDTO, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        // 批量删除角色菜单关联
        for (String roleId : ids) {
            deleteRoleMenusByRoleId(roleId);
            deleteRoleDeptsByRoleId(roleId);
            deleteUserRolesByRoleId(roleId);
        }
        
        // 批量删除角色
        return removeByIds(ids);
    }

    @Override
    public boolean updateRoleStatus(String id, Integer status) {
        if (StringUtils.isBlank(id) || status == null) {
            return false;
        }
        
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(status);
        role.setUpdateTime(new Date());
        
        return updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoleMenu(String roleId, List<String> menuIds) {
        if (StringUtils.isBlank(roleId)) {
            return false;
        }
        
        // 删除原有角色菜单关联
        deleteRoleMenusByRoleId(roleId);
        
        if (menuIds != null && !menuIds.isEmpty()) {
            // 保存新的角色菜单关联
            insertRoleMenus(roleId, menuIds);
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoleDataScope(String roleId, String dataScope, List<String> deptIds) {
        if (StringUtils.isBlank(roleId) || StringUtils.isBlank(dataScope)) {
            return false;
        }
        
        // 更新角色数据权限范围
        SysRole role = new SysRole();
        role.setId(roleId);
        role.setDataScope(dataScope);
        role.setUpdateTime(new Date());
        updateById(role);
        
        // 删除原有角色部门关联
        deleteRoleDeptsByRoleId(roleId);
        
        // 如果是自定义数据权限，保存新的角色部门关联
        if ("CUSTOM".equals(dataScope) && deptIds != null && !deptIds.isEmpty()) {
            insertRoleDepts(roleId, deptIds);
        }
        
        return true;
    }

    @Override
    public List<SysRole> selectRoleOptions() {
        // 查询正常状态的角色列表
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRole::getStatus, 1);
        queryWrapper.orderByAsc(SysRole::getSort);
        return list(queryWrapper);
    }

    @Override
    public Set<String> selectRoleIdsByUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            return new HashSet<>();
        }
        
        // 查询用户角色关联
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserRole::getUserId, userId);
        
        // 直接使用userRoleMapper查询用户角色关联
        List<SysUserRole> userRoles = userRoleMapper.selectList(queryWrapper);
        
        // 提取角色ID集合
        return userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignUserRoles(String userId, List<String> roleIds) {
        if (StringUtils.isBlank(userId)) {
            return false;
        }
        
        // 删除用户原有角色关联
        deleteUserRolesByUserId(userId);
        
        if (roleIds != null && !roleIds.isEmpty()) {
            // 批量插入用户角色关联
            List<SysUserRole> userRoles = new ArrayList<>();
            for (String roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoles.add(userRole);
            }
            // 使用 userRoleMapper 逐个插入
            for (SysUserRole userRole : userRoles) {
                userRoleMapper.insert(userRole);
            }
        }
        
        return true;
    }

    /**
     * 根据角色ID查询菜单ID列表
     */
    private List<String> getMenuIdsByRoleId(String roleId) {
        LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleMenu::getRoleId, roleId);
        
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(queryWrapper);
        
        return roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询部门ID列表
     */
    private List<String> getDeptIdsByRoleId(String roleId) {
        LambdaQueryWrapper<SysRoleDept> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleDept::getRoleId, roleId);
        
        List<SysRoleDept> roleDepts = roleDeptMapper.selectList(queryWrapper);
        
        return roleDepts.stream()
                .map(SysRoleDept::getDeptId)
                .collect(Collectors.toList());
    }

    /**
     * 批量插入角色菜单关联
     */
    private void insertRoleMenus(String roleId, List<String> menuIds) {
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> roleMenus = new ArrayList<>();
            for (String menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenus.add(roleMenu);
            }
            // 逐个插入，在事务中执行
            for (SysRoleMenu roleMenu : roleMenus) {
                roleMenuMapper.insert(roleMenu);
            }
        }
    }

    /**
     * 批量插入角色部门关联
     */
    private void insertRoleDepts(String roleId, List<String> deptIds) {
        if (deptIds != null && !deptIds.isEmpty()) {
            List<SysRoleDept> roleDepts = new ArrayList<>();
            for (String deptId : deptIds) {
                SysRoleDept roleDept = new SysRoleDept();
                roleDept.setRoleId(roleId);
                roleDept.setDeptId(deptId);
                roleDepts.add(roleDept);
            }
            // 逐个插入，在事务中执行
            for (SysRoleDept roleDept : roleDepts) {
                roleDeptMapper.insert(roleDept);
            }
        }
    }

    /**
     * 删除角色菜单关联
     */
    private void deleteRoleMenusByRoleId(String roleId) {
        // 直接使用 MyBatis Plus 的方式删除数据
        LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleMenu::getRoleId, roleId);
        
        // 导入并注入 SysRoleMenuMapper
        roleMenuMapper.delete(queryWrapper);
    }

    /**
     * 删除角色部门关联
     */
    private void deleteRoleDeptsByRoleId(String roleId) {
        // 直接使用 MyBatis Plus 的方式删除数据
        LambdaQueryWrapper<SysRoleDept> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleDept::getRoleId, roleId);
        
        // 导入并注入 SysRoleDeptMapper
        roleDeptMapper.delete(queryWrapper);
    }

    /**
     * 删除用户角色关联（按角色ID）
     */
    private void deleteUserRolesByRoleId(String roleId) {
        // 直接使用 MyBatis Plus 的方式删除数据
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserRole::getRoleId, roleId);
        
        // 导入并注入 SysUserRoleMapper
        userRoleMapper.delete(queryWrapper);
    }

    /**
     * 删除用户角色关联（按用户ID）
     */
    private void deleteUserRolesByUserId(String userId) {
        // 直接使用 MyBatis Plus 的方式删除数据
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserRole::getUserId, userId);
        
        // 导入并注入 SysUserRoleMapper
        userRoleMapper.delete(queryWrapper);
    }
} 