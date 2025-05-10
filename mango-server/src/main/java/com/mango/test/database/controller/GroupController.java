package com.mango.test.database.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mango.test.database.entity.Group;
import com.mango.test.database.service.GroupService;
import com.mango.test.vo.GroupVO;
import com.mango.test.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api(tags = "分组管理")
@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @GetMapping("/list")
    @ApiOperation("获取分组列表")
    public R<IPage<GroupVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) Integer minTableCount,
            @RequestParam(required = false) Integer maxTableCount) {

        try {
            Page<GroupVO> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Group> wrapper = new LambdaQueryWrapper<>();

            // 添加查询条件
            if (StringUtils.isNotBlank(name)) {
                wrapper.like(Group::getName, name);
            }

            // 创建时间范围
            if (StringUtils.isNotBlank(createTimeStart)) {
                wrapper.ge(Group::getCreateTime, createTimeStart);
            }
            if (StringUtils.isNotBlank(createTimeEnd)) {
                wrapper.le(Group::getCreateTime, createTimeEnd);
            }

            // 表数量范围
            if (minTableCount != null) {
                wrapper.ge(Group::getTableCount, minTableCount);
            }
            if (maxTableCount != null) {
                wrapper.le(Group::getTableCount, maxTableCount);
            }

            // 排序
            wrapper.orderByDesc(Group::getUpdateTime);

            IPage<GroupVO> result = groupService.pageGroups(page, wrapper);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("获取分组列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("获取分组详情")
    public R<Group> detail(@PathVariable Long id) {
        return R.ok(groupService.getGroupDetail(id));
    }

    @PostMapping
    @ApiOperation("创建分组")
    public R<Group> create(@RequestBody Group group) {
        return R.ok(groupService.createGroup(group));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新分组")
    public R<Void> update(
        @PathVariable String id,
        @RequestBody Group group
    ) {
        group.setId(id);
        groupService.updateGroup(group);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除分组")
    public R<Void> delete(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return R.ok();
    }
}
