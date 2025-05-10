package com.mango.test.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mango.test.system.dto.SysOrganizationDTO;
import com.mango.test.system.entity.SysOrganization;
import com.mango.test.system.service.SysOrganizationService;
import com.mango.test.vo.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 机构管理控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/organizations")
public class SysOrganizationController {

    private final SysOrganizationService organizationService;

    /**
     * 获取机构树
     */
    @GetMapping("/tree")
    public R<List<SysOrganizationDTO>> getOrganizationTree() {
        try {
            List<SysOrganizationDTO> tree = organizationService.getOrganizationTree();
            return R.ok(tree);
        } catch (Exception e) {
            log.error("获取机构树失败", e);
            return R.fail("获取机构树失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID列表获取机构
     */
    @PostMapping("/getByIds")
    public R<List<SysOrganizationDTO>> getOrganizationsByIds(@RequestBody List<String> ids) {
        log.info("根据ID列表获取机构: {}", ids);
        try {
            List<SysOrganizationDTO> orgs = organizationService.getOrganizationsByIds(ids);
            return R.ok(orgs);
        } catch (Exception e) {
            log.error("获取机构信息失败", e);
            return R.fail("获取机构信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取机构分页列表
     */
    @GetMapping("/page")
    public R<IPage<SysOrganization>> getOrganizationPage(SysOrganizationDTO dto) {
        try {
            IPage<SysOrganization> page = organizationService.getOrganizationPage(dto);
            return R.ok(page);
        } catch (Exception e) {
            log.error("获取机构分页列表失败", e);
            return R.fail("获取机构分页列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取机构
     */
    @GetMapping("/{id}")
    public R<SysOrganizationDTO> getOrganizationById(@PathVariable String id) {
        try {
            SysOrganizationDTO org = organizationService.getOrganizationById(id);
            if (org != null) {
                return R.ok(org);
            } else {
                return R.fail("机构不存在");
            }
        } catch (Exception e) {
            log.error("获取机构详情失败", e);
            return R.fail("获取机构详情失败：" + e.getMessage());
        }
    }

    /**
     * 新增机构
     */
    @PostMapping
    public R<Boolean> addOrganization(@RequestBody SysOrganizationDTO dto) {
        try {
            boolean result = organizationService.addOrganization(dto);
            if (result) {
                return R.ok("新增机构成功", true);
            } else {
                return R.fail("新增机构失败");
            }
        } catch (Exception e) {
            log.error("新增机构失败", e);
            return R.fail("新增机构失败：" + e.getMessage());
        }
    }

    /**
     * 修改机构
     */
    @PutMapping
    public R<Boolean> updateOrganization(@RequestBody SysOrganizationDTO dto) {
        try {
            boolean result = organizationService.updateOrganization(dto);
            if (result) {
                return R.ok("修改机构成功", true);
            } else {
                return R.fail("修改机构失败");
            }
        } catch (Exception e) {
            log.error("修改机构失败", e);
            return R.fail("修改机构失败：" + e.getMessage());
        }
    }

    /**
     * 删除机构
     */
    @DeleteMapping("/{id}")
    public R<Boolean> deleteOrganization(@PathVariable String id) {
        try {
            boolean result = organizationService.deleteOrganization(id);
            if (result) {
                return R.ok("删除机构成功", true);
            } else {
                return R.fail("删除失败，该机构可能有子机构");
            }
        } catch (Exception e) {
            log.error("删除机构失败", e);
            return R.fail("删除机构失败：" + e.getMessage());
        }
    }

    /**
     * 修改机构状态
     */
    @PutMapping("/status/{id}")
    public R<Boolean> updateStatus(@PathVariable String id, @RequestParam String status) {
        try {
            boolean result = organizationService.updateStatus(id, status);
            if (result) {
                return R.ok("修改状态成功", true);
            } else {
                return R.fail("修改状态失败");
            }
        } catch (Exception e) {
            log.error("修改机构状态失败", e);
            return R.fail("修改机构状态失败：" + e.getMessage());
        }
    }

    /**
     * 机构上移
     */
    @PutMapping("/move-up/{id}")
    public R<Boolean> moveUp(@PathVariable String id) {
        try {
            boolean result = organizationService.moveUp(id);
            if (result) {
                return R.ok("上移成功", true);
            } else {
                return R.fail("上移失败，该机构可能已经在最顶部");
            }
        } catch (Exception e) {
            log.error("机构上移失败", e);
            return R.fail("机构上移失败：" + e.getMessage());
        }
    }

    /**
     * 机构下移
     */
    @PutMapping("/move-down/{id}")
    public R<Boolean> moveDown(@PathVariable String id) {
        try {
            boolean result = organizationService.moveDown(id);
            if (result) {
                return R.ok("下移成功", true);
            } else {
                return R.fail("下移失败，该机构可能已经在最底部");
            }
        } catch (Exception e) {
            log.error("机构下移失败", e);
            return R.fail("机构下移失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有机构（非树形）
     */
    @GetMapping
    public R<List<SysOrganizationDTO>> getAllOrganizations() {
        try {
            // 查询所有机构（可以从树形结构中提取）
            List<SysOrganizationDTO> tree = organizationService.getOrganizationTree();
            
            // 将树形结构展平为列表
            List<SysOrganizationDTO> allOrgs = flattenTree(tree);
            
            return R.ok(allOrgs);
        } catch (Exception e) {
            log.error("获取所有机构失败", e);
            return R.fail("获取所有机构失败：" + e.getMessage());
        }
    }

    /**
     * 将树形结构展平为列表
     */
    private List<SysOrganizationDTO> flattenTree(List<SysOrganizationDTO> tree) {
        List<SysOrganizationDTO> result = new ArrayList<>();
        if (tree == null || tree.isEmpty()) {
            return result;
        }
        
        for (SysOrganizationDTO node : tree) {
            result.add(node);
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                result.addAll(flattenTree(node.getChildren()));
            }
        }
        
        return result;
    }
} 