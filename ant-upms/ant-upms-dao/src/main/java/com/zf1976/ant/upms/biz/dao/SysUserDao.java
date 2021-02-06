package com.zf1976.ant.upms.biz.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zf1976.ant.upms.biz.pojo.po.SysUser;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * 系统用户(SysUser)表数据库访问层
 *
 * @author makejava
 * @since 2020-08-31 11:44:06
 */
@Repository
public interface SysUserDao extends BaseMapper<SysUser> {

    /**
     * 查询用户信息
     *
     * @param username 用户名
     * @return result
     */
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "departmentId", column = "department_id"),
            @Result(property = "department", column = "department_id",
                    one = @One(select = "com.zf1976.upms.biz.dao.SysDepartmentDao.selectById")),
            @Result(property = "roleList", column = "id",
                    many = @Many(select = "com.zf1976.upms.biz.dao.SysRoleDao.selectListByUserId")),
            @Result(property = "positionList", column = "id",
                    many = @Many(select = "com.zf1976.upms.biz.dao.SysPositionDao.selectListByUserId"))
    })
    @Select(value = "select * from sys_user where username = #{username}")
    SysUser findByUsername(@Param("username") String username);

    /**
     * 添加岗位依赖关系
    　　　　 *
     * @param id id
     * @param jobIds job collection id
     */
    void savePositionRelationById(@Param("id") long id, @Param("jobIds") Collection<Long> jobIds);

    /**
     * 添加角色依赖关系
     * @param id id
     * @param roleIds role collection id
     */
    void savaRoleRelationById(@Param("id") long id, @Param("roleIds") Collection<Long> roleIds);

    /**
     * 删除角色依赖关系
     *
     * @param ids id collection
     */
    void deleteRoleRelationByIds(@Param("ids") Collection<Long> ids);

    /**
     * 删除岗位依赖关系
     * @param ids ids
     */
    void deletePositionRelationByIds(@Param("ids") Collection<Long> ids);

    /**
     * 根据角色id集合查询依赖用户id集合
     *
     * @param roleIds ids
     * @return user ids
     */
    List<Long> selectByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    /**
     * 根据部门集合id查询关联用户id
     *
     * @param departmentIds ids
     * @return user ids
     */
    List<Long> selectByDepartmentIds(@Param("departmentIds") Collection<Long> departmentIds);
}
