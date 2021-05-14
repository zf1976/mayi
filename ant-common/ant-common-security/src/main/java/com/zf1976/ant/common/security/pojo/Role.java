package com.zf1976.ant.common.security.pojo;

import com.zf1976.ant.upms.biz.pojo.enums.DataPermissionEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author ant
 * Create by Ant on 2020/9/8 9:02 下午
 */
@Data
@Accessors
public class Role implements Serializable {

    /**
     * 角色id
     */
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色级别
     */
    private Integer level;

    /**
     * 数据范围
     */
    private DataPermissionEnum dataScope;

}