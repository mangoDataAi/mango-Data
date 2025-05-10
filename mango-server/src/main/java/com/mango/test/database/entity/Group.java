package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("data_md_group")
public class Group extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private Integer tableCount;

}
