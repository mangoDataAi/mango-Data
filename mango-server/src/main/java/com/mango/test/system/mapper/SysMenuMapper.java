package com.mango.test.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单Mapper接口
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    
    /**
     * 根据角色ID列表查询菜单ID
     */
    @Select("<script>" +
            "SELECT DISTINCT rm.menu_id FROM sys_role_menu rm WHERE rm.role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach>" +
            "</script>")
    List<String> selectMenuIdsByRoleIds(@Param("roleIds") List<String> roleIds);
} 