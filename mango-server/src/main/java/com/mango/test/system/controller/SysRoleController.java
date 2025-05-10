package com.mango.test.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mango.test.system.dto.SysRoleDTO;
import com.mango.test.system.dto.SysRoleQueryDTO;
import com.mango.test.system.entity.SysRole;
import com.mango.test.system.service.SysRoleService;
import com.mango.test.vo.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/roles")
public class SysRoleController {

    private final SysRoleService roleService;

    /**
     * 获取角色列表
     */
    @GetMapping
    public R<List<Map<String, Object>>> list() {
        log.info("获取角色列表");
        try {
            // 调用服务获取所有角色
            List<SysRole> roles = roleService.selectRoleOptions();
            // 转换为前端需要的格式
            List<Map<String, Object>> roleList = roles.stream().map(role -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", role.getId());
                map.put("name", role.getName());
                map.put("code", role.getCode());
                map.put("status", role.getStatus() != null && role.getStatus() == 1 ? "1" : "0");
                map.put("sort", role.getSort());
                map.put("remark", role.getRemark());
                map.put("dataScope", role.getDataScope());
                map.put("createTime", role.getCreateTime());
                return map;
            }).collect(Collectors.toList());
            
            return R.ok(roleList);
        } catch (Exception e) {
            log.error("获取角色列表失败", e);
            return R.fail("获取角色列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询角色列表
     */
    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
            
        log.info("分页查询角色列表: name={}, status={}, current={}, size={}", name, status, current, size);
        try {
            // 构建查询条件
            SysRoleQueryDTO queryDTO = new SysRoleQueryDTO();
            queryDTO.setName(name);
            // 安全处理status参数，避免空字符串转换异常
            if (status != null && !status.trim().isEmpty()) {
                queryDTO.setStatus(Integer.parseInt(status));
            } else {
                queryDTO.setStatus(null);
            }
            queryDTO.setCurrent(current);
            queryDTO.setSize(size);
            
            // 调用服务获取分页数据
            IPage<SysRole> page = roleService.pageRoles(queryDTO);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> records = page.getRecords().stream().map(role -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", role.getId());
                map.put("name", role.getName());
                map.put("code", role.getCode());
                map.put("status", role.getStatus() != null && role.getStatus() == 1 ? "1" : "0");
                map.put("sort", role.getSort());
                map.put("remark", role.getRemark());
                map.put("dataScope", role.getDataScope());
                map.put("createTime", role.getCreateTime());
                return map;
            }).collect(Collectors.toList());
            
            // 准备返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", page.getTotal());
            result.put("size", page.getSize());
            result.put("current", page.getCurrent());
            result.put("pages", page.getPages());
            
            return R.ok(result);
        } catch (Exception e) {
            log.error("分页查询角色列表失败", e);
            return R.fail("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    public R<Map<String, Object>> getById(@PathVariable String id) {
        log.info("获取角色详情: {}", id);
        try {
            // 调用服务获取角色详情
            SysRoleDTO role = roleService.getRoleById(id);
            
            if (role == null) {
                return R.fail("角色不存在");
            }
            
            // 转换为前端需要的格式
            Map<String, Object> result = new HashMap<>();
            result.put("id", role.getId());
            result.put("name", role.getName());
            result.put("code", role.getCode());
            result.put("status", role.getStatus() != null && role.getStatus() == 1 ? "1" : "0");
            result.put("sort", role.getSort());
            result.put("remark", role.getRemark());
            result.put("dataScope", role.getDataScope());
            result.put("createTime", role.getCreateTime());
            result.put("menuIds", role.getMenuIds());
            result.put("deptIds", role.getDeptIds());
            
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取角色详情失败", e);
            return R.fail("获取失败：" + e.getMessage());
        }
    }

    /**
     * 获取角色统计概览
     */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        log.info("获取角色统计概览");
        try {
            // 调用服务获取角色统计数据
            Map<String, Object> statistics = new HashMap<>();
            
            // 获取所有角色
            List<SysRole> allRoles = roleService.selectRoleOptions();
            
            // 计算统计数据
            int total = allRoles.size();
            int systemRoles = 0;
            int customRoles = 0;
            int disabledRoles = 0;
            
            for (SysRole role : allRoles) {
                // 按照角色代码前缀判断是否为系统角色
                if (role.getCode() != null && role.getCode().startsWith("sys_")) {
                    systemRoles++;
                } else {
                    customRoles++;
                }
                
                // 判断状态：0表示禁用
                if (role.getStatus() != null && role.getStatus() == 0) {
                    disabledRoles++;
                }
            }
            
            statistics.put("total", total);
            statistics.put("systemRoles", systemRoles);
            statistics.put("customRoles", customRoles);
            statistics.put("disabledRoles", disabledRoles);
            
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取角色统计概览失败", e);
            return R.fail("获取统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取角色详情(旧版API，已被getById替代)
     * @deprecated 使用 {@link #getById(String)} 替代
     */
    @Deprecated
    @GetMapping("/info/{id}")
    public R<SysRoleDTO> getInfo(@PathVariable String id) {
        try {
            SysRoleDTO role = roleService.getRoleById(id);
            if (role != null) {
                return R.ok(role);
            } else {
                // 如果找不到角色，返回模拟数据而不是null
                SysRoleDTO mockRole = new SysRoleDTO();
                mockRole.setId(id);
                mockRole.setName("未找到角色");
                mockRole.setCode("NOT_FOUND");
                mockRole.setColor("#CCCCCC");
                mockRole.setSort(999);
                mockRole.setStatus(0);
                mockRole.setDataScope("ALL");
                mockRole.setRemark("此角色在数据库中不存在，可能已被删除");
                mockRole.setMenuIds(new ArrayList<>());
                mockRole.setDeptIds(new ArrayList<>());
                return R.fail(-1, "角色不存在", mockRole);
            }
        } catch (Exception e) {
            // 捕获所有异常并记录
            e.printStackTrace();
            SysRoleDTO errorRole = new SysRoleDTO();
            errorRole.setId(id);
            errorRole.setName("获取失败");
            errorRole.setCode("ERROR");
            errorRole.setColor("#FF0000");
            errorRole.setSort(999);
            errorRole.setStatus(0);
            errorRole.setRemark("获取角色信息时发生错误: " + e.getMessage());
            return R.fail(-1, "获取角色信息失败: " + e.getMessage(), errorRole);
        }
    }

    /**
     * 新增角色
     */
    @PostMapping
    public R<String> add(@RequestBody SysRoleDTO role) {
        String roleId = roleService.insertRole(role);
        return R.ok("新增角色成功", roleId);
    }

    /**
     * 修改角色
     */
    @PutMapping
    public R<Boolean> update(@RequestBody SysRoleDTO role) {
        try {
            System.out.println("接收到更新角色请求: " + role);
            
            if (role == null || role.getId() == null) {
                return R.fail("角色ID不能为空");
            }
            
            boolean result = roleService.updateRole(role);
            
            if (result) {
                return R.ok("修改角色成功", true);
            } else {
                return R.fail("修改角色失败，角色可能不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("修改角色异常: " + e.getMessage());
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        boolean result = roleService.deleteRoleByIds(idList);
        return R.ok("删除角色成功", result);
    }

    /**
     * 更新角色状态
     */
    @PutMapping("/status/{id}/{status}")
    public R<Boolean> updateStatus(@PathVariable String id, @PathVariable Integer status) {
        boolean result = roleService.updateRoleStatus(id, status);
        return R.ok("更新角色状态成功", result);
    }

    /**
     * 角色权限分配
     */
    @PutMapping("/auth/{roleId}")
    public R<Boolean> assignAuth(@PathVariable String roleId, @RequestBody List<String> menuIds) {
        boolean result = roleService.assignRoleMenu(roleId, menuIds);
        return R.ok("权限分配成功", result);
    }

    /**
     * 角色数据权限分配
     */
    @PutMapping("/dataScope/{roleId}")
    public R<Boolean> dataScope(@PathVariable String roleId, @RequestBody SysRoleDTO roleDTO) {
        boolean result = roleService.assignRoleDataScope(roleId, roleDTO.getDataScope(), roleDTO.getDeptIds());
        return R.ok("数据权限配置成功", result);
    }

    /**
     * 获取角色选择框列表
     */
    @GetMapping("/options")
    public R<List<Map<String, Object>>> options() {
        List<SysRole> roles = roleService.selectRoleOptions();
        List<Map<String, Object>> options = roles.stream().map(role -> {
            Map<String, Object> option = new HashMap<>();
            option.put("id", role.getId());
            option.put("name", role.getName());
            option.put("code", role.getCode());
            return option;
        }).collect(Collectors.toList());
        return R.ok(options);
    }

    /**
     * 查询用户所有角色
     */
    @GetMapping("/user/{userId}")
    public R<List<String>> getUserRoles(@PathVariable String userId) {
        return R.ok(roleService.selectRoleIdsByUserId(userId).stream().collect(Collectors.toList()));
    }

    /**
     * 分配用户角色
     */
    @PutMapping("/user/{userId}")
    public R<Boolean> assignUserRoles(@PathVariable String userId, @RequestBody List<String> roleIds) {
        boolean result = roleService.assignUserRoles(userId, roleIds);
        return R.ok("用户角色分配成功", result);
    }

    /**
     * 导入角色
     */
    @PostMapping("/import")
    public R<Boolean> importRoles(@RequestBody List<SysRoleDTO> roles) {
        // 实际项目中应该有批量导入逻辑
        for (SysRoleDTO role : roles) {
            roleService.insertRole(role);
        }
        return R.ok("导入角色成功", true);
    }

    /**
     * 导出角色
     */
    @GetMapping("/export")
    public R<String> exportRoles(SysRoleQueryDTO queryDTO) {
        // 实际项目中应该有导出Excel的逻辑
        return R.ok("导出成功");
    }
} 