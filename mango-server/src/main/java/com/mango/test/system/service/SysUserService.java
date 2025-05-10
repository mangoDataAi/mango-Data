package com.mango.test.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.system.entity.SysUser;

import java.util.List;
import java.util.Map;

/**
 * 用户管理服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 分页查询用户列表
     *
     * @param params 查询参数
     * @return 分页数据
     */
    IPage<SysUser> getUserPage(Map<String, Object> params);
    
    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    Map<String, Object> getUserById(String id);
    
    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 新增的用户ID
     */
    String addUser(Map<String, Object> user);
    
    /**
     * 修改用户
     *
     * @param user 用户信息
     * @return 是否成功
     */
    boolean updateUser(Map<String, Object> user);
    
    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    boolean deleteUser(String id);
    
    /**
     * 批量删除用户
     *
     * @param ids 用户ID集合
     * @return 是否成功
     */
    boolean batchDeleteUsers(List<String> ids);
    
    /**
     * 重置密码
     *
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean resetPassword(String userId, String newPassword);
    
    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param status 状态值
     * @return 是否成功
     */
    boolean updateStatus(String id, String status);
    
    /**
     * 批量更新用户状态
     *
     * @param ids 用户ID集合
     * @param status 状态值
     * @return 是否成功
     */
    boolean batchUpdateStatus(List<String> ids, String status);
} 