package com.zf1976.ant.auth.controller.security;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zf1976.ant.auth.pojo.dto.PermissionDTO;
import com.zf1976.ant.auth.pojo.po.SysPermission;
import com.zf1976.ant.auth.pojo.vo.PermissionVO;
import com.zf1976.ant.auth.service.impl.PermissionService;
import com.zf1976.ant.common.core.foundation.DataResult;
import com.zf1976.ant.common.core.validate.ValidationInsertGroup;
import com.zf1976.ant.common.core.validate.ValidationUpdateGroup;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;
import java.util.Set;

/**
 * @author mac
 * @date 2021/5/12
 */
@RestController
@RequestMapping(
        value = "/oauth/security/permission"
)
public class PermissionController {

    private final PermissionService service;

    public PermissionController(PermissionService service) {
        this.service = service;
    }

    @PostMapping("/page")
    @PreAuthorize("hasAuthority('admin')")
    public DataResult<IPage<PermissionVO>> selectPermissionPage(@RequestBody Page<SysPermission> page) {
        return DataResult.success(this.service.selectPermissionByPage(page));
    }

    @DeleteMapping("/patch")
    @PreAuthorize("hasAuthority('admin')")
    public DataResult<Void> deletePermission(@RequestBody Set<Long> ids) {
        return DataResult.success(this.service.deletePermissionByIds(ids));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('admin')")
    public DataResult<Void> deletePermission(@RequestParam Long id) {
        return DataResult.success(this.service.deletePermissionById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    DataResult<Void> savaPermission(@RequestBody @Validated(ValidationInsertGroup.class) PermissionDTO permissionDTO) {
        return DataResult.success(this.service.savePermission(permissionDTO));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('admin')")
    DataResult<Void> updatePermission(@RequestBody @Validated(ValidationUpdateGroup.class) PermissionDTO permissionDTO) {
        return DataResult.success(this.service.updatePermission(permissionDTO));
    }
}