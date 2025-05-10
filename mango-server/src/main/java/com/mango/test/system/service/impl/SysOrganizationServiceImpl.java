package com.mango.test.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.system.dto.SysOrganizationDTO;
import com.mango.test.system.entity.SysOrganization;
import com.mango.test.system.mapper.SysOrganizationMapper;
import com.mango.test.system.service.SysOrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 机构管理服务实现
 */
@Slf4j
@Service
public class SysOrganizationServiceImpl extends ServiceImpl<SysOrganizationMapper, SysOrganization> implements SysOrganizationService {

    @Override
    public List<SysOrganizationDTO> getOrganizationTree() {
        // 查询所有机构
        List<SysOrganization> allOrgs = this.list(new LambdaQueryWrapper<SysOrganization>()
                .orderByAsc(SysOrganization::getSort));
        
        // 转换为树形结构
        return buildTree(allOrgs);
    }

    @Override
    public IPage<SysOrganization> getOrganizationPage(SysOrganizationDTO dto) {
        // 构建查询条件
        LambdaQueryWrapper<SysOrganization> wrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (dto != null) {
            if (StringUtils.hasText(dto.getName())) {
                wrapper.like(SysOrganization::getName, dto.getName());
            }
            if (StringUtils.hasText(dto.getCode())) {
                wrapper.like(SysOrganization::getCode, dto.getCode());
            }
            if (StringUtils.hasText(dto.getType())) {
                wrapper.eq(SysOrganization::getType, dto.getType());
            }
            if (StringUtils.hasText(dto.getStatus())) {
                wrapper.eq(SysOrganization::getStatus, dto.getStatus());
            }
            if (StringUtils.hasText(dto.getParentId())) {
                wrapper.eq(SysOrganization::getParentId, dto.getParentId());
            }
            if (dto.getBeginCreateTime() != null) {
                wrapper.ge(SysOrganization::getCreateTime, dto.getBeginCreateTime());
            }
            if (dto.getEndCreateTime() != null) {
                wrapper.le(SysOrganization::getCreateTime, dto.getEndCreateTime());
            }
        }
        
        // 默认排序
        wrapper.orderByAsc(SysOrganization::getSort);
        
        // 分页查询
        Page<SysOrganization> page = new Page<>(
                dto.getPageNum() == null ? 1 : dto.getPageNum(),
                dto.getPageSize() == null ? 10 : dto.getPageSize()
        );
        
        return this.page(page, wrapper);
    }

    @Override
    public SysOrganizationDTO getOrganizationById(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        
        SysOrganization org = this.getById(id);
        if (org == null) {
            return null;
        }
        
        SysOrganizationDTO dto = new SysOrganizationDTO();
        BeanUtils.copyProperties(org, dto);
        
        // 查询子机构
        List<SysOrganization> children = this.list(new LambdaQueryWrapper<SysOrganization>()
                .eq(SysOrganization::getParentId, id)
                .orderByAsc(SysOrganization::getSort));
        
        if (!children.isEmpty()) {
            List<SysOrganizationDTO> childrenDTOs = children.stream()
                    .map(child -> {
                        SysOrganizationDTO childDTO = new SysOrganizationDTO();
                        BeanUtils.copyProperties(child, childDTO);
                        return childDTO;
                    })
                    .collect(Collectors.toList());
            dto.setChildren(childrenDTOs);
        }
        
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addOrganization(SysOrganizationDTO dto) {
        if (dto == null) {
            return false;
        }
        
        // 生成UUID作为ID
        String id = UUID.randomUUID().toString().replace("-", "");
        
        SysOrganization org = new SysOrganization();
        BeanUtils.copyProperties(dto, org);
        
        org.setId(id);
        if (org.getParentId() == null) {
            org.setParentId("0"); // 默认为顶级机构
        }
        
        // 设置默认值
        if (org.getSort() == null) {
            org.setSort(0);
        }
        if (org.getStatus() == null) {
            org.setStatus("1"); // 默认启用
        }
        
        // 设置创建时间等信息
        Date now = new Date();
        org.setCreateTime(now);
        org.setUpdateTime(now);
        
        // 设置创建人和更新人，实际项目中可从当前登录用户获取
        org.setCreator("admin");
        org.setUpdater("admin");
        
        return this.save(org);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrganization(SysOrganizationDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getId())) {
            return false;
        }
        
        // 查询原机构信息
        SysOrganization existingOrg = this.getById(dto.getId());
        if (existingOrg == null) {
            return false;
        }
        
        // 复制新的属性值
        SysOrganization org = new SysOrganization();
        BeanUtils.copyProperties(dto, org);
        
        // 保留原有的创建时间和创建人
        org.setCreateTime(existingOrg.getCreateTime());
        org.setCreator(existingOrg.getCreator());
        
        // 更新updateTime和updater
        org.setUpdateTime(new Date());
        org.setUpdater("admin"); // 实际项目中可从当前登录用户获取
        
        return this.updateById(org);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOrganization(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        
        // 检查是否有子机构
        int childCount = (int) this.count(new LambdaQueryWrapper<SysOrganization>()
                .eq(SysOrganization::getParentId, id));
        
        if (childCount > 0) {
            log.warn("机构[{}]下有子机构，无法删除", id);
            return false;
        }
        
        return this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String id, String status) {
        if (!StringUtils.hasText(id) || !StringUtils.hasText(status)) {
            return false;
        }
        
        LambdaUpdateWrapper<SysOrganization> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysOrganization::getId, id)
                .set(SysOrganization::getStatus, status)
                .set(SysOrganization::getUpdateTime, new Date())
                .set(SysOrganization::getUpdater, "admin"); // 实际项目中可从当前登录用户获取
        
        return this.update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveUp(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        
        // 获取当前机构
        SysOrganization current = this.getById(id);
        if (current == null || !StringUtils.hasText(current.getParentId())) {
            return false;
        }
        
        // 获取同级前一个机构
        List<SysOrganization> siblings = this.list(new LambdaQueryWrapper<SysOrganization>()
                .eq(SysOrganization::getParentId, current.getParentId())
                .orderByAsc(SysOrganization::getSort));
        
        int currentIndex = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex <= 0) {
            // 已经是第一个，无法上移
            return false;
        }
        
        // 交换排序号
        SysOrganization previous = siblings.get(currentIndex - 1);
        int tempSort = current.getSort();
        current.setSort(previous.getSort());
        previous.setSort(tempSort);
        
        // 更新两个机构
        this.updateById(current);
        this.updateById(previous);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveDown(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        
        // 获取当前机构
        SysOrganization current = this.getById(id);
        if (current == null || !StringUtils.hasText(current.getParentId())) {
            return false;
        }
        
        // 获取同级前一个机构
        List<SysOrganization> siblings = this.list(new LambdaQueryWrapper<SysOrganization>()
                .eq(SysOrganization::getParentId, current.getParentId())
                .orderByAsc(SysOrganization::getSort));
        
        int currentIndex = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex < 0 || currentIndex >= siblings.size() - 1) {
            // 已经是最后一个，无法下移
            return false;
        }
        
        // 交换排序号
        SysOrganization next = siblings.get(currentIndex + 1);
        int tempSort = current.getSort();
        current.setSort(next.getSort());
        next.setSort(tempSort);
        
        // 更新两个机构
        this.updateById(current);
        this.updateById(next);
        
        return true;
    }

    @Override
    public List<SysOrganizationDTO> getOrganizationsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询指定ID的机构
        List<SysOrganization> organizations = this.listByIds(ids);
        
        // 转换为DTO
        return organizations.stream().map(org -> {
            SysOrganizationDTO dto = new SysOrganizationDTO();
            BeanUtils.copyProperties(org, dto);
            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * 构建机构树
     *
     * @param allOrgs 所有机构列表
     * @return 树形结构
     */
    private List<SysOrganizationDTO> buildTree(List<SysOrganization> allOrgs) {
        if (allOrgs == null || allOrgs.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 按父ID分组
        Map<String, List<SysOrganization>> parentIdMap = allOrgs.stream()
                .collect(Collectors.groupingBy(SysOrganization::getParentId));
        
        // 递归构建树
        List<SysOrganization> rootOrgs = parentIdMap.getOrDefault("0", Collections.emptyList());
        return rootOrgs.stream()
                .map(org -> buildTreeNode(org, parentIdMap))
                .collect(Collectors.toList());
    }
    
    /**
     * 递归构建树节点
     *
     * @param org 当前机构
     * @param parentIdMap 按父ID分组的机构Map
     * @return 树节点
     */
    private SysOrganizationDTO buildTreeNode(SysOrganization org, Map<String, List<SysOrganization>> parentIdMap) {
        SysOrganizationDTO dto = new SysOrganizationDTO();
        BeanUtils.copyProperties(org, dto);
        
        List<SysOrganization> children = parentIdMap.get(org.getId());
        if (children != null && !children.isEmpty()) {
            List<SysOrganizationDTO> childrenDTOs = children.stream()
                    .map(child -> buildTreeNode(child, parentIdMap))
                    .collect(Collectors.toList());
            dto.setChildren(childrenDTOs);
        }
        
        return dto;
    }
} 