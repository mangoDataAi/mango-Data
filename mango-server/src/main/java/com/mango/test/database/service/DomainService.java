package com.mango.test.database.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.Domain;
import com.mango.test.database.model.DomainQuery;

import java.util.List;
import java.util.Map;

/**
 * 主题域服务接口
 */
public interface DomainService extends IService<Domain> {
    
    /**
     * 分页查询主题域
     */
    IPage<Domain> pageDomain(DomainQuery query);
    
    /**
     * 新增主题域
     */
    void addDomain(Domain domain);
    
    /**
     * 修改主题域
     */
    void updateDomain(Domain domain);
    
    /**
     * 删除主题域
     */
    void deleteDomain(String id);
    
    /**
     * 修改主题域状态
     */
    void updateStatus(String id, String status);
    
    /**
     * 检查是否有子域
     */
    boolean hasChildren(String id);
    
    /**
     * 获取子域列表
     */
    List<Domain> getChildren(String parentId);
    
    /**
     * 获取父级域
     */
    Domain getParent(String id);

    /**
     * 获取主题域树形结构
     * @return 主题域树形结构列表
     */
    List<Domain> getDomainTree();

    /**
     * 获取主题域列表
     * @param parentId 父级ID
     * @return 主题域列表
     */
    List<Domain> getDomainList(String parentId);

} 