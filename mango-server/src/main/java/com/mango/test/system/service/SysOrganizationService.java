package com.mango.test.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.system.dto.SysOrganizationDTO;
import com.mango.test.system.entity.SysOrganization;

import java.util.List;

/**
 * 机构管理服务接口
 */
public interface SysOrganizationService extends IService<SysOrganization> {

    /**
     * 获取机构树结构
     *
     * @return 机构树列表
     */
    List<SysOrganizationDTO> getOrganizationTree();

    /**
     * 根据条件获取机构分页列表
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    IPage<SysOrganization> getOrganizationPage(SysOrganizationDTO dto);

    /**
     * 根据ID获取机构信息
     *
     * @param id 机构ID
     * @return 机构信息
     */
    SysOrganizationDTO getOrganizationById(String id);

    /**
     * 根据ID列表获取机构信息
     *
     * @param ids 机构ID列表
     * @return 机构列表
     */
    List<SysOrganizationDTO> getOrganizationsByIds(List<String> ids);

    /**
     * 新增机构
     *
     * @param dto 机构信息
     * @return 是否成功
     */
    boolean addOrganization(SysOrganizationDTO dto);

    /**
     * 修改机构
     *
     * @param dto 机构信息
     * @return 是否成功
     */
    boolean updateOrganization(SysOrganizationDTO dto);

    /**
     * 删除机构
     *
     * @param id 机构ID
     * @return 是否成功
     */
    boolean deleteOrganization(String id);

    /**
     * 更新机构状态
     *
     * @param id     机构ID
     * @param status 状态值（1-正常，0-禁用）
     * @return 是否成功
     */
    boolean updateStatus(String id, String status);

    /**
     * 机构上移
     *
     * @param id 机构ID
     * @return 是否成功
     */
    boolean moveUp(String id);

    /**
     * 机构下移
     *
     * @param id 机构ID
     * @return 是否成功
     */
    boolean moveDown(String id);
} 