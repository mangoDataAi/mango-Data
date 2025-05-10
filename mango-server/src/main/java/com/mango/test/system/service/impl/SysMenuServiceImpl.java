package com.mango.test.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.common.api.PageResult;
import com.mango.test.system.dto.MenuQueryDTO;
import com.mango.test.system.entity.SysMenu;
import com.mango.test.system.mapper.SysMenuMapper;
import com.mango.test.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> getMenuTree(MenuQueryDTO queryDTO) {
        // 构建查询条件
        LambdaQueryWrapper<SysMenu> queryWrapper = buildQueryWrapper(queryDTO);
        // 根据排序升序
        queryWrapper.orderByAsc(SysMenu::getSort);
        
        // 获取所有符合条件的菜单
        List<SysMenu> allMenus = list(queryWrapper);
        
        // 如果不需要树形结构，直接返回列表
        if (queryDTO != null && Boolean.FALSE.equals(queryDTO.getIsTree())) {
            return allMenus;
        }
        
        // 构建树形结构
        return buildTree(allMenus);
    }

    @Override
    public PageResult<SysMenu> pageMenus(MenuQueryDTO queryDTO) {
        // 如果需要树形结构，则分页方式有所不同
        if (queryDTO != null && Boolean.TRUE.equals(queryDTO.getIsTree())) {
            // 获取树形结构数据
            List<SysMenu> tree = getMenuTree(queryDTO);
            
            // 手动进行分页
            long total = tree.size();
            long size = queryDTO.getSize();
            long current = queryDTO.getCurrent();
            long fromIndex = (current - 1) * size;
            long toIndex = Math.min(current * size, total);
            
            List<SysMenu> records = fromIndex < total ? 
                    tree.subList((int)fromIndex, (int)toIndex) : 
                    new ArrayList<>();
            
            return new PageResult<>(total, size, current, records);
        } else {
            // 正常分页查询
            LambdaQueryWrapper<SysMenu> queryWrapper = buildQueryWrapper(queryDTO);
            queryWrapper.orderByAsc(SysMenu::getSort);
            
            Page<SysMenu> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
            IPage<SysMenu> pageResult = page(page, queryWrapper);
            
            return new PageResult<>(
                    pageResult.getTotal(),
                    pageResult.getSize(),
                    pageResult.getCurrent(),
                    pageResult.getRecords()
            );
        }
    }

    @Override
    public SysMenu getMenuDetail(String id) {
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createMenu(SysMenu menu) {
        // 设置创建时间
        Date now = new Date();
        menu.setCreateTime(now);
        menu.setUpdateTime(now);
        
        // 保存菜单
        save(menu);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(SysMenu menu) {
        // 检查菜单是否存在
        SysMenu existingMenu = getById(menu.getId());
        if (existingMenu == null) {
            log.warn("菜单不存在，ID: {}", menu.getId());
            return false;
        }
        
        // 设置更新时间
        menu.setUpdateTime(new Date());
        
        // 更新菜单
        return updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(String id) {
        // 检查是否有子菜单
        LambdaQueryWrapper<SysMenu> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(SysMenu::getParentId, id);
        long count = count(queryWrapper);
        if (count > 0) {
            log.warn("菜单存在子菜单，无法删除，ID: {}", id);
            return false;
        }
        
        // 删除菜单
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String id, Integer status) {
        // 检查菜单是否存在
        SysMenu existingMenu = getById(id);
        if (existingMenu == null) {
            log.warn("菜单不存在，ID: {}", id);
            return false;
        }
        
        // 更新状态
        LambdaUpdateWrapper<SysMenu> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(SysMenu::getId, id)
                    .set(SysMenu::getStatus, status)
                    .set(SysMenu::getUpdateTime, new Date());
        
        return update(updateWrapper);
    }

    @Override
    public Map<String, Object> getMenuStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            // 获取所有菜单
            List<SysMenu> allMenus = list();
            
            // 菜单总数
            statistics.put("totalCount", allMenus.size());
            
            // 目录数量
            long dirCount = allMenus.stream()
                    .filter(menu -> "DIR".equals(menu.getType()))
                    .count();
            statistics.put("dirCount", dirCount);
            
            // 菜单数量
            long menuCount = allMenus.stream()
                    .filter(menu -> "MENU".equals(menu.getType()))
                    .count();
            statistics.put("menuCount", menuCount);
            
            // 启用状态的菜单占比
            long enabledCount = allMenus.stream()
                    .filter(menu -> menu.getStatus() != null && menu.getStatus() == 1)
                    .count();
            double enabledRate = allMenus.size() > 0 ? 
                    ((double) enabledCount / allMenus.size()) * 100 : 0;
            statistics.put("enabledRate", Math.round(enabledRate));
            
        } catch (Exception e) {
            log.error("获取菜单统计信息失败", e);
            
            // 返回默认值
            statistics.put("totalCount", 0);
            statistics.put("dirCount", 0);
            statistics.put("menuCount", 0);
            statistics.put("enabledRate", 0);
        }
        
        return statistics;
    }
    
    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SysMenu> buildQueryWrapper(MenuQueryDTO queryDTO) {
        LambdaQueryWrapper<SysMenu> queryWrapper = Wrappers.lambdaQuery();
        
        if (queryDTO != null) {
            // 菜单名称模糊查询
            if (StringUtils.isNotBlank(queryDTO.getName())) {
                queryWrapper.like(SysMenu::getName, queryDTO.getName());
            }
            
            // 菜单类型精确查询
            if (StringUtils.isNotBlank(queryDTO.getType())) {
                queryWrapper.eq(SysMenu::getType, queryDTO.getType());
            }
            
            // 菜单状态精确查询
            if (StringUtils.isNotBlank(queryDTO.getStatus())) {
                queryWrapper.eq(SysMenu::getStatus, queryDTO.getStatus());
            }
        }
        
        return queryWrapper;
    }
    
    /**
     * 构建树形结构
     */
    private List<SysMenu> buildTree(List<SysMenu> menus) {
        List<SysMenu> returnList = new ArrayList<>();
        
        // 获取所有根节点
        List<SysMenu> rootNodes = menus.stream()
                .filter(menu -> menu.getParentId() == null || "0".equals(menu.getParentId()))
                .collect(Collectors.toList());
        
        for (SysMenu rootNode : rootNodes) {
            // 递归构建子树
            buildChildrenTree(rootNode, menus);
            returnList.add(rootNode);
        }
        
        return returnList;
    }
    
    /**
     * 递归构建子树
     */
    private void buildChildrenTree(SysMenu node, List<SysMenu> allMenus) {
        // 获取所有子节点
        List<SysMenu> childrenNodes = allMenus.stream()
                .filter(menu -> node.getId().equals(menu.getParentId()))
                .collect(Collectors.toList());
        
        if (!childrenNodes.isEmpty()) {
            // 设置子节点
            node.setChildren(childrenNodes);
            
            // 递归构建子树
            childrenNodes.forEach(child -> buildChildrenTree(child, allMenus));
        }
    }
} 