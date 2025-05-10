package com.mango.test.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.system.entity.SysOrganization;
import org.apache.ibatis.annotations.Mapper;

/**
 * 机构管理Mapper接口
 */
@Mapper
public interface SysOrganizationMapper extends BaseMapper<SysOrganization> {
} 