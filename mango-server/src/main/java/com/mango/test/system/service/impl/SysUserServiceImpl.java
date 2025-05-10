package com.mango.test.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.system.entity.SysUser;
import com.mango.test.system.entity.SysUserRole;
import com.mango.test.database.mapper.SysUserMapper;
import com.mango.test.system.mapper.SysUserRoleMapper;
import com.mango.test.system.service.SysOrganizationService;
import com.mango.test.system.service.SysRoleService;
import com.mango.test.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleService roleService;
    private final SysOrganizationService organizationService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public IPage<SysUser> getUserPage(Map<String, Object> params) {
        // 解析分页参数
        Integer current = params.get("current") instanceof Integer ? (Integer) params.get("current") : 1;
        Integer size = params.get("size") instanceof Integer ? (Integer) params.get("size") : 10;
        Page<SysUser> page = new Page<>(current, size);
        
        // 构建查询条件
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        // 用户名模糊查询
        String username = (String) params.get("username");
        if (StringUtils.isNotBlank(username)) {
            wrapper.like(SysUser::getUsername, username);
        }
        
        // 姓名模糊查询
        String realName = (String) params.get("realName");
        if (StringUtils.isNotBlank(realName)) {
            wrapper.like(SysUser::getRealName, realName);
        }
        
        // 手机号模糊查询
        String mobile = (String) params.get("mobile");
        if (StringUtils.isNotBlank(mobile)) {
            wrapper.like(SysUser::getMobile, mobile);
        }
        
        // 邮箱模糊查询
        String email = (String) params.get("email");
        if (StringUtils.isNotBlank(email)) {
            wrapper.like(SysUser::getEmail, email);
        }
        
        // 机构ID精确查询
        String organizationId = (String) params.get("organizationId");
        if (StringUtils.isNotBlank(organizationId)) {
            wrapper.eq(SysUser::getOrganizationId, organizationId);
        }
        
        // 状态精确查询
        String status = (String) params.get("status");
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(SysUser::getStatus, status);
        }
        
        // 按创建时间降序排序
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        // 查询分页数据
        IPage<SysUser> userPage = this.page(page, wrapper);
        
        // 处理用户额外信息（组织名称、角色名称等）
        if (userPage.getRecords() != null && !userPage.getRecords().isEmpty()) {
            for (SysUser user : userPage.getRecords()) {
                // 查询用户的角色ID列表
                LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
                userRoleWrapper.eq(SysUserRole::getUserId, user.getId());
                List<SysUserRole> userRoles = userRoleMapper.selectList(userRoleWrapper);
                
                if (!userRoles.isEmpty()) {
                    // 设置角色ID列表
                    List<String> roleIds = userRoles.stream()
                            .map(SysUserRole::getRoleId)
                            .collect(Collectors.toList());
                    user.setRoleIds(roleIds);
                    
                    // 设置角色名称（简单处理，实际应查询角色表）
                    user.setRoleNames(roleIds.stream()
                            .map(roleId -> {
                                // 这里应该是从角色服务获取角色名称，简化处理
                                if ("1".equals(roleId)) return "管理员";
                                if ("2".equals(roleId)) return "普通用户";
                                if ("3".equals(roleId)) return "访客";
                                return "角色" + roleId;
                            })
                            .collect(Collectors.joining(",")));
                }
                
                // 设置机构名称
                if (StringUtils.isNotBlank(user.getOrganizationId())) {
                    // 应该从机构服务获取机构名称，简化处理
                    if ("1".equals(user.getOrganizationId())) {
                        user.setOrganizationName("总公司");
                    } else if ("2".equals(user.getOrganizationId())) {
                        user.setOrganizationName("研发部");
                    } else {
                        user.setOrganizationName("机构" + user.getOrganizationId());
                    }
                }
            }
        }
        
        return userPage;
    }

    @Override
    public Map<String, Object> getUserById(String id) {
        // 查询用户基本信息
        SysUser user = this.getById(id);
        if (user == null) {
            return null;
        }
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("gender", user.getGender());
        result.put("mobile", user.getMobile());
        result.put("email", user.getEmail());
        result.put("status", user.getStatus());
        result.put("organizationId", user.getOrganizationId());
        result.put("remark", user.getRemark());
        result.put("createTime", user.getCreateTime());
        
        // 查询用户角色ID
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getUserId, id);
        List<SysUserRole> userRoles = userRoleMapper.selectList(userRoleWrapper);
        
        if (!userRoles.isEmpty()) {
            List<String> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            result.put("roleIds", roleIds);
            
            // 角色名称（简单处理，实际应查询角色表）
            String roleNames = roleIds.stream()
                    .map(roleId -> {
                        // 这里应该是从角色服务获取角色名称，简化处理
                        if ("1".equals(roleId)) return "管理员";
                        if ("2".equals(roleId)) return "普通用户";
                        if ("3".equals(roleId)) return "访客";
                        return "角色" + roleId;
                    })
                    .collect(Collectors.joining(","));
            result.put("roleNames", roleNames);
        } else {
            result.put("roleIds", new ArrayList<>());
            result.put("roleNames", "");
        }
        
        // 机构名称
        if (StringUtils.isNotBlank(user.getOrganizationId())) {
            // 应该从机构服务获取机构名称，简化处理
            String orgName;
            if ("1".equals(user.getOrganizationId())) {
                orgName = "总公司";
            } else if ("2".equals(user.getOrganizationId())) {
                orgName = "研发部";
            } else {
                orgName = "机构" + user.getOrganizationId();
            }
            result.put("organizationName", orgName);
        } else {
            result.put("organizationName", "");
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addUser(Map<String, Object> userMap) {
        SysUser user = new SysUser();
        
        // 设置用户基本信息
        user.setUsername((String) userMap.get("username"));
        user.setRealName((String) userMap.get("realName"));
        user.setGender((String) userMap.get("gender"));
        user.setMobile((String) userMap.get("mobile"));
        user.setEmail((String) userMap.get("email"));
        
        // 处理organizationId，可能是字符串或数组
        Object orgIdObj = userMap.get("organizationId");
        if (orgIdObj != null) {
            if (orgIdObj instanceof String) {
                user.setOrganizationId((String) orgIdObj);
            } else if (orgIdObj instanceof List) {
                // 如果是数组，取最后一个元素作为选中的机构ID
                @SuppressWarnings("unchecked")
                List<String> orgIdList = (List<String>) orgIdObj;
                if (!orgIdList.isEmpty()) {
                    user.setOrganizationId(orgIdList.get(orgIdList.size() - 1));
                }
            } else if (orgIdObj instanceof Object[]) {
                // 处理对象数组
                Object[] orgIdArray = (Object[]) orgIdObj;
                if (orgIdArray.length > 0) {
                    user.setOrganizationId(String.valueOf(orgIdArray[orgIdArray.length - 1]));
                }
            }
        }
        
        user.setStatus((String) userMap.get("status"));
        user.setRemark((String) userMap.get("remark"));
        
        // 设置密码（加密）
        String password = (String) userMap.get("password");
        if (StringUtils.isNotBlank(password)) {
            // 前端已进行MD5加密，这里进行BCrypt二次加密
            user.setPassword(passwordEncoder.encode(password));
        } else {
            // 默认密码，直接使用BCrypt加密
            user.setPassword(passwordEncoder.encode("123456"));
        }
        
        // 设置创建时间
        user.setCreateTime(new Date());
        
        // 插入用户表
        this.save(user);
        
        // 处理用户角色关联
        @SuppressWarnings("unchecked")
        List<String> roleIds = (List<String>) userMap.get("roleIds");
        if (roleIds != null && !roleIds.isEmpty()) {
            for (String roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
        
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(Map<String, Object> userMap) {
        String userId = (String) userMap.get("id");
        if (StringUtils.isBlank(userId)) {
            return false;
        }
        
        // 查询原用户
        SysUser user = this.getById(userId);
        if (user == null) {
            return false;
        }
        
        // 更新用户基本信息
        user.setRealName((String) userMap.get("realName"));
        user.setGender((String) userMap.get("gender"));
        user.setMobile((String) userMap.get("mobile"));
        user.setEmail((String) userMap.get("email"));
        
        // 处理organizationId，可能是字符串或数组
        Object orgIdObj = userMap.get("organizationId");
        if (orgIdObj != null) {
            if (orgIdObj instanceof String) {
                user.setOrganizationId((String) orgIdObj);
            } else if (orgIdObj instanceof List) {
                // 如果是数组，取最后一个元素作为选中的机构ID
                @SuppressWarnings("unchecked")
                List<String> orgIdList = (List<String>) orgIdObj;
                if (!orgIdList.isEmpty()) {
                    user.setOrganizationId(orgIdList.get(orgIdList.size() - 1));
                }
            } else if (orgIdObj instanceof Object[]) {
                // 处理对象数组
                Object[] orgIdArray = (Object[]) orgIdObj;
                if (orgIdArray.length > 0) {
                    user.setOrganizationId(String.valueOf(orgIdArray[orgIdArray.length - 1]));
                }
            }
        }
        
        user.setStatus((String) userMap.get("status"));
        user.setRemark((String) userMap.get("remark"));
        user.setUpdateTime(new Date());
        
        // 更新用户表
        boolean updated = this.updateById(user);
        
        if (updated) {
            // 更新用户角色关联
            @SuppressWarnings("unchecked")
            List<String> roleIds = (List<String>) userMap.get("roleIds");
            if (roleIds != null) {
                // 删除原有角色关联
                LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SysUserRole::getUserId, userId);
                userRoleMapper.delete(wrapper);
                
                // 插入新的角色关联
                for (String roleId : roleIds) {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    userRoleMapper.insert(userRole);
                }
            }
        }
        
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(String id) {
        if (StringUtils.isBlank(id)) {
            return false;
        }
        
        // 删除用户角色关联
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, id);
        userRoleMapper.delete(wrapper);
        
        // 删除用户
        return this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteUsers(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        // 批量删除用户角色关联
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUserRole::getUserId, ids);
        userRoleMapper.delete(wrapper);
        
        // 批量删除用户
        return this.removeByIds(ids);
    }

    @Override
    public boolean resetPassword(String userId, String newPassword) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(newPassword)) {
            return false;
        }
        
        SysUser user = this.getById(userId);
        if (user == null) {
            return false;
        }
        
        // 更新密码（加密）
        // 此时newPassword已经是前端MD5加密过的，直接进行BCrypt加密存储
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(new Date());
        
        return this.updateById(user);
    }

    @Override
    public boolean updateStatus(String id, String status) {
        if (StringUtils.isBlank(id) || StringUtils.isBlank(status)) {
            return false;
        }
        
        SysUser user = this.getById(id);
        if (user == null) {
            return false;
        }
        
        user.setStatus(status);
        user.setUpdateTime(new Date());
        
        return this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<String> ids, String status) {
        if (ids == null || ids.isEmpty() || StringUtils.isBlank(status)) {
            return false;
        }
        
        for (String id : ids) {
            SysUser user = this.getById(id);
            if (user != null) {
                user.setStatus(status);
                user.setUpdateTime(new Date());
                this.updateById(user);
            }
        }
        
        return true;
    }

} 