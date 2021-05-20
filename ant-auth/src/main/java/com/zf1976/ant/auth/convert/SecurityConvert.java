package com.zf1976.ant.auth.convert;

import com.zf1976.ant.auth.pojo.ClientDetails;
import com.zf1976.ant.auth.pojo.dto.ClientDetailsDTO;
import com.zf1976.ant.auth.pojo.dto.PermissionDTO;
import com.zf1976.ant.auth.pojo.po.SysPermission;
import com.zf1976.ant.auth.pojo.vo.ClientDetailsVO;
import com.zf1976.ant.auth.pojo.vo.PermissionVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * @author mac
 * @date 2021/5/11
 */
@SuppressWarnings("all")
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SecurityConvert {

    SecurityConvert INSTANCE = Mappers.getMapper(SecurityConvert.class);

    /**
     * 转权限vo
     *
     * @param sysPermission 系统权限实体对象
     * @return {@link PermissionVO}
     * @date 2021-05-11 23:56:07
     */
    PermissionVO toPermissionVo(SysPermission sysPermission);

    /**
     * 实体转vo
     *
     * @param clientDetails 实体
     * @return {@link ClientDetailsVO}
     */
    ClientDetailsVO toClientDetailsVo(ClientDetails clientDetails);


    /**
     * 客户端dto转实体类
     *
     * @param clientDetailsDTO DTO
     * @return {@link ClientDetails}
     * @date 2021-05-11 23:56:50
     */
    ClientDetails toClientDetailsEntity(ClientDetailsDTO clientDetailsDTO);

    /**
     * permission DTO 转实体
     * @param permissionDTO DTO
     * @return {@link SysPermission}
     */
    SysPermission toPermissionEntity(PermissionDTO permissionDTO);

    /**
     * 复制属性
     *
     * @date 2021-05-12 09:13:09
     * @param permissionDTO 权限DTO
     * @param sysPermission 权限实体
     * @throws
     */
    void copyProperties(PermissionDTO permissionDTO, @MappingTarget SysPermission sysPermission);

}
