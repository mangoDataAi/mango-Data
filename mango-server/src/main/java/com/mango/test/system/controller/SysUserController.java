package com.mango.test.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mango.test.system.entity.SysUser;
import com.mango.test.system.service.SysUserService;
import com.mango.test.vo.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/users")
public class SysUserController {

    private final SysUserService userService;

    /**
     * 分页查询用户列表
     */
    @PostMapping("/page")
    public R<Map<String, Object>> page(@RequestBody Map<String, Object> params) {
        log.info("分页查询用户列表: {}", params);
        try {
            // 调用Service获取分页数据
            IPage<SysUser> userPage = userService.getUserPage(params);
            
            // 准备返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("records", userPage.getRecords());
            result.put("total", userPage.getTotal());
            result.put("size", userPage.getSize());
            result.put("current", userPage.getCurrent());
            result.put("pages", userPage.getPages());
            
            return R.ok(result);
        } catch (Exception e) {
            log.error("分页查询用户列表失败", e);
            return R.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public R<Map<String, Object>> getById(@PathVariable String id) {
        log.info("获取用户详情: {}", id);
        try {
            // 调用Service获取用户详情
            Map<String, Object> user = userService.getUserById(id);
            
            if (user != null) {
                return R.ok(user);
            } else {
                return R.fail("用户不存在");
            }
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return R.fail("获取失败：" + e.getMessage());
        }
    }

    /**
     * 新增用户
     */
    @PostMapping
    public R<String> add(@RequestBody Map<String, Object> user) {
        log.info("新增用户: {}", user);
        try {
            // 调用Service添加用户
            String id = userService.addUser(user);
            return R.ok("新增用户成功", id);
        } catch (Exception e) {
            log.error("新增用户失败", e);
            return R.fail("新增失败：" + e.getMessage());
        }
    }

    /**
     * 修改用户
     */
    @PutMapping
    public R<Boolean> update(@RequestBody Map<String, Object> user) {
        log.info("修改用户: {}", user);
        try {
            // 调用Service更新用户
            boolean result = userService.updateUser(user);
            return result ? R.ok("修改用户成功", true) : R.fail("修改失败：用户不存在或数据无效");
        } catch (Exception e) {
            log.error("修改用户失败", e);
            return R.fail("修改失败：" + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public R<Boolean> remove(@PathVariable String id) {
        log.info("删除用户: {}", id);
        try {
            // 调用Service删除用户
            boolean result = userService.deleteUser(id);
            return result ? R.ok("删除用户成功", true) : R.fail("删除失败：用户不存在");
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return R.fail("删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除用户
     */
    @PostMapping("/batch-delete")
    public R<Boolean> batchDelete(@RequestBody List<String> ids) {
        log.info("批量删除用户: {}", ids);
        try {
            // 调用Service批量删除用户
            boolean result = userService.batchDeleteUsers(ids);
            return result ? R.ok("批量删除成功", true) : R.fail("批量删除失败：参数无效");
        } catch (Exception e) {
            log.error("批量删除用户失败", e);
            return R.fail("批量删除失败：" + e.getMessage());
        }
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public R<Boolean> resetPassword(@RequestBody Map<String, Object> params) {
        log.info("重置密码: {}", params);
        try {
            // 获取用户ID和新密码
            String userId = (String) params.get("userId");
            String newPassword = (String) params.get("newPassword");
            
            // 调用Service重置密码
            boolean result = userService.resetPassword(userId, newPassword);
            return result ? R.ok("重置密码成功", true) : R.fail("重置密码失败：用户不存在或参数无效");
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return R.fail("重置密码失败：" + e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/status/{id}")
    public R<Boolean> updateStatus(@PathVariable String id, @RequestParam String status) {
        log.info("更新用户状态: id={}, status={}", id, status);
        try {
            // 调用Service更新用户状态
            boolean result = userService.updateStatus(id, status);
            return result ? R.ok(status.equals("1") ? "启用成功" : "禁用成功", true) : R.fail("更新状态失败：用户不存在");
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return R.fail("更新状态失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新用户状态
     */
    @PostMapping("/batch-status")
    public R<Boolean> batchUpdateStatus(@RequestParam String status, @RequestBody List<String> ids) {
        log.info("批量更新用户状态: status={}, ids={}", status, ids);
        try {
            // 调用Service批量更新用户状态
            boolean result = userService.batchUpdateStatus(ids, status);
            return result ? R.ok("批量" + (status.equals("1") ? "启用" : "禁用") + "成功", true) : R.fail("批量更新状态失败：参数无效");
        } catch (Exception e) {
            log.error("批量更新用户状态失败", e);
            return R.fail("批量更新状态失败：" + e.getMessage());
        }
    }
} 