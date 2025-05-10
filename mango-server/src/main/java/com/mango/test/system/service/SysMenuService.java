package com.mango.test.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.common.api.PageResult;
import com.mango.test.system.dto.MenuQueryDTO;
import com.mango.test.system.entity.SysMenu;

import java.util.List;
import java.util.Map;

/**
 * 菜单服务接口
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 获取菜单树形结构
     *
     * @param queryDTO 查询条件
     * @return 树形菜单数据
     */
    List<SysMenu> getMenuTree(MenuQueryDTO queryDTO);

    /**
     * 分页查询菜单
     *
     * @param queryDTO 查询条件
     * @return 分页数据
     */
    PageResult<SysMenu> pageMenus(MenuQueryDTO queryDTO);

    /**
     * 获取菜单详情
     *
     * @param id 菜单ID
     * @return 菜单详情
     */
    SysMenu getMenuDetail(String id);

    /**
     * 创建菜单
     *
     * @param menu 菜单信息
     * @return 创建的菜单ID
     */
    String createMenu(SysMenu menu);

    /**
     * 更新菜单
     *
     * @param menu 菜单信息
     * @return 是否更新成功
     */
    boolean updateMenu(SysMenu menu);

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     * @return 是否删除成功
     */
    boolean deleteMenu(String id);

    /**
     * 更新菜单状态
     *
     * @param id     菜单ID
     * @param status 状态（1启用，0禁用）
     * @return 是否更新成功
     */
    boolean updateStatus(String id, Integer status);

    /**
     * 获取菜单统计信息
     *
     * @return 统计信息
     */
    Map<String, Object> getMenuStatistics();
} 