package com.mango.test.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 机构DTO，用于前后端数据传输
 */
@Data
public class SysOrganizationDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 父级ID
     */
    private String parentId;

    /**
     * 机构名称
     */
    private String name;

    /**
     * 机构编码
     */
    private String code;

    /**
     * 机构类型（1-公司，2-部门，3-小组，4-其他）
     */
    private String type;

    /**
     * 负责人
     */
    private String leader;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态（1-正常，0-禁用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 子机构列表
     */
    private List<SysOrganizationDTO> children;
    
    /**
     * 高级查询参数：开始创建时间
     */
    private Date beginCreateTime;
    
    /**
     * 高级查询参数：结束创建时间
     */
    private Date endCreateTime;
    
    /**
     * 分页参数：页码
     */
    private Integer pageNum;
    
    /**
     * 分页参数：每页条数
     */
    private Integer pageSize;
} 