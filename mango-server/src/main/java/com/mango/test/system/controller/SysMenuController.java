package com.mango.test.system.controller;

import com.mango.test.common.api.PageResult;
import com.mango.test.system.dto.MenuQueryDTO;
import com.mango.test.system.entity.SysMenu;
import com.mango.test.system.service.SysMenuService;
import com.mango.test.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 菜单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
@Api(tags = "菜单管理接口")
public class SysMenuController {

    private final SysMenuService sysMenuService;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    @ApiOperation("获取菜单树")
    public R<List<SysMenu>> getMenuTree(MenuQueryDTO queryDTO) {
        log.info("获取菜单树: {}", queryDTO);
        try {
            List<SysMenu> tree = sysMenuService.getMenuTree(queryDTO);
            return R.ok(tree);
        } catch (Exception e) {
            log.error("获取菜单树失败", e);
            return R.fail("获取菜单树失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询菜单
     */
    @GetMapping("/page")
    @ApiOperation("分页查询菜单")
    public R<PageResult<SysMenu>> pageMenus(MenuQueryDTO queryDTO) {
        log.info("分页查询菜单: {}", queryDTO);
        try {
            PageResult<SysMenu> pageResult = sysMenuService.pageMenus(queryDTO);
            return R.ok(pageResult);
        } catch (Exception e) {
            log.error("分页查询菜单失败", e);
            return R.fail("分页查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取菜单详情
     */
    @GetMapping("/{id}")
    @ApiOperation("获取菜单详情")
    public R<SysMenu> getMenuDetail(@PathVariable @ApiParam("菜单ID") String id) {
        log.info("获取菜单详情: {}", id);
        try {
            SysMenu menu = sysMenuService.getMenuDetail(id);
            return menu != null ? R.ok(menu) : R.fail("菜单不存在");
        } catch (Exception e) {
            log.error("获取菜单详情失败", e);
            return R.fail("获取详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建菜单
     */
    @PostMapping
    @ApiOperation("创建菜单")
    public R<String> createMenu(@RequestBody @Validated SysMenu menu) {
        log.info("创建菜单: {}", menu);
        try {
            String id = sysMenuService.createMenu(menu);
            return R.ok("创建成功", id);
        } catch (Exception e) {
            log.error("创建菜单失败", e);
            return R.fail("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新菜单
     */
    @PutMapping
    @ApiOperation("更新菜单")
    public R<Boolean> updateMenu(@RequestBody @Validated SysMenu menu) {
        log.info("更新菜单: {}", menu);
        try {
            boolean success = sysMenuService.updateMenu(menu);
            return success ? R.ok("更新成功", true) : R.fail("更新失败，菜单不存在");
        } catch (Exception e) {
            log.error("更新菜单失败", e);
            return R.fail("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除菜单")
    public R<Boolean> deleteMenu(@PathVariable @ApiParam("菜单ID") String id) {
        log.info("删除菜单: {}", id);
        try {
            boolean success = sysMenuService.deleteMenu(id);
            return success ? R.ok("删除成功", true) : R.fail("删除失败，菜单不存在或存在子菜单");
        } catch (Exception e) {
            log.error("删除菜单失败", e);
            return R.fail("删除失败：" + e.getMessage());
        }
    }

    /**
     * 更新菜单状态
     */
    @PutMapping("/status/{id}")
    @ApiOperation("更新菜单状态")
    public R<Boolean> updateStatus(
            @PathVariable @ApiParam("菜单ID") String id,
            @RequestParam @ApiParam("状态（1启用，0禁用）") Integer status) {
        log.info("更新菜单状态: id={}, status={}", id, status);
        try {
            boolean success = sysMenuService.updateStatus(id, status);
            String statusDesc = status == 1 ? "启用" : "禁用";
            return success ? R.ok(statusDesc + "成功", true) : R.fail(statusDesc + "失败，菜单不存在");
        } catch (Exception e) {
            log.error("更新菜单状态失败", e);
            return R.fail("更新状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取菜单统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation("获取菜单统计信息")
    public R<Map<String, Object>> getMenuStatistics() {
        log.info("获取菜单统计信息");
        try {
            Map<String, Object> statistics = sysMenuService.getMenuStatistics();
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取菜单统计信息失败", e);
            return R.fail("获取统计信息失败：" + e.getMessage());
        }
    }
} 