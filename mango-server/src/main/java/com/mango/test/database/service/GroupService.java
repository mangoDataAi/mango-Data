package com.mango.test.database.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.Group;
import com.mango.test.vo.GroupVO;

public interface GroupService extends IService<Group> {
    /**
     * 创建分组
     */
    Group createGroup(Group group);

    /**
     * 更新分组
     */
    void updateGroup(Group group);

    /**
     * 删除分组
     */
    void deleteGroup(Long id);

    /**
     * 获取分组详情
     */
    Group getGroupDetail(Long id);

    /**
     * 更新分组的表数量
     */
    void updateTableCount(Long groupId);

    IPage<GroupVO> pageGroups(Page<GroupVO> page, LambdaQueryWrapper<Group> wrapper);
}
