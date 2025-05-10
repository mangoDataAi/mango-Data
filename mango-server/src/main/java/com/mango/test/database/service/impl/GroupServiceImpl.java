package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.database.entity.Group;
import com.mango.test.database.entity.Table;
import com.mango.test.database.mapper.TableMapper;
import com.mango.test.exception.BusinessException;
import com.mango.test.database.mapper.GroupMapper;
import com.mango.test.database.service.GroupService;
import com.mango.test.vo.GroupVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    @Autowired
    private TableMapper tableMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Group createGroup(Group group) {
        // 检查名称是否重复
        if (isNameDuplicated(group.getName(), null)) {
            throw new BusinessException("分组名称已存在");
        }

        // 设置初始值
        group.setTableCount(0);
        group.setCreateTime(new Date());
        group.setUpdateTime(new Date());

        // 保存分组
        saveOrUpdate(group);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Group group) {
        // 检查分组是否存在
        Group existingGroup = getById(group.getId());
        if (existingGroup == null) {
            throw new BusinessException("分组不存在");
        }

        // 检查名称是否重复
        if (isNameDuplicated(group.getName(), Long.valueOf(group.getId()))) {
            throw new BusinessException("分组名称已存在");
        }

        // 更新基本信息
        existingGroup.setName(group.getName());
        existingGroup.setDescription(group.getDescription());
        existingGroup.setUpdateTime(new Date());

        saveOrUpdate(existingGroup);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        // 检查分组是否存在
        Group group = getById(id);
        if (group == null) {
            throw new BusinessException("分组不存在");
        }

        // 删除分组下的所有表
        tableMapper.delete(new LambdaQueryWrapper<Table>()
            .eq(Table::getGroupId, id));

        // 删除分组
        removeById(id);
    }

    @Override
    public Group getGroupDetail(Long id) {
        Group group = getById(id);
        if (group == null) {
            throw new BusinessException("分组不存在");
        }
        return group;
    }

    @Override
    public void updateTableCount(Long groupId) {
        // 统计分组下的表数量
        int count = Math.toIntExact(tableMapper.selectCount(new LambdaQueryWrapper<Table>()
                .eq(Table::getGroupId, groupId)));

        // 更新分组的表数量
        Group group = getById(groupId);
        if (group != null) {
            group.setTableCount(count);
            group.setUpdateTime(new Date());
            updateById(group);
        }
    }

    @Override
    public IPage<GroupVO> pageGroups(Page<GroupVO> page, LambdaQueryWrapper<Group> wrapper) {
        // 查询分页数据
        Page<Group> groupPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<Group> groupIPage = baseMapper.selectPage(groupPage, wrapper);
        
        // 转换为 VO
        List<GroupVO> records = groupIPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 构建返回结果
        Page<GroupVO> result = new Page<>(page.getCurrent(), page.getSize(), groupIPage.getTotal());
        result.setRecords(records);
        
        return result;
    }

    /**
     * 将实体转换为VO
     */
    private GroupVO convertToVO(Group group) {
        GroupVO vo = new GroupVO();
        BeanUtils.copyProperties(group, vo);
        // 可以在这里添加其他需要的转换逻辑
        return vo;
    }

    /**
     * 检查分组名称是否重复
     */
    private boolean isNameDuplicated(String name, Long excludeId) {
        LambdaQueryWrapper<Group> wrapper = new LambdaQueryWrapper<Group>()
            .eq(Group::getName, name);

        if (excludeId != null) {
            wrapper.ne(Group::getId, excludeId);
        }

        return count(wrapper) > 0;
    }
}
