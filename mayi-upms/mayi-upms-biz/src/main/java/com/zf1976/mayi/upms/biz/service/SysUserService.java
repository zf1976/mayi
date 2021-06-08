package com.zf1976.mayi.upms.biz.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.power.common.util.ValidateUtil;
import com.zf1976.mayi.common.component.cache.annotation.CacheConfig;
import com.zf1976.mayi.common.component.cache.annotation.CacheEvict;
import com.zf1976.mayi.common.component.cache.annotation.CachePut;
import com.zf1976.mayi.common.component.mail.ValidateEmailService;
import com.zf1976.mayi.common.core.constants.Namespace;
import com.zf1976.mayi.common.core.foundation.exception.BusinessException;
import com.zf1976.mayi.common.core.foundation.exception.BusinessMsgState;
import com.zf1976.mayi.common.core.util.UUIDUtil;
import com.zf1976.mayi.common.core.validate.Validator;
import com.zf1976.mayi.common.encrypt.MD5Encoder;
import com.zf1976.mayi.common.encrypt.RsaUtil;
import com.zf1976.mayi.common.encrypt.property.RsaProperties;
import com.zf1976.mayi.common.security.property.SecurityProperties;
import com.zf1976.mayi.common.security.support.session.Session;
import com.zf1976.mayi.common.security.support.session.manager.SessionManagement;
import com.zf1976.mayi.upms.biz.convert.SysUserConvert;
import com.zf1976.mayi.upms.biz.dao.SysDepartmentDao;
import com.zf1976.mayi.upms.biz.dao.SysPositionDao;
import com.zf1976.mayi.upms.biz.dao.SysRoleDao;
import com.zf1976.mayi.upms.biz.dao.SysUserDao;
import com.zf1976.mayi.upms.biz.feign.SecurityClient;
import com.zf1976.mayi.upms.biz.pojo.dto.user.UpdateEmailDTO;
import com.zf1976.mayi.upms.biz.pojo.dto.user.UpdateInfoDTO;
import com.zf1976.mayi.upms.biz.pojo.dto.user.UpdatePasswordDTO;
import com.zf1976.mayi.upms.biz.pojo.dto.user.UserDTO;
import com.zf1976.mayi.upms.biz.pojo.po.SysDepartment;
import com.zf1976.mayi.upms.biz.pojo.po.SysPosition;
import com.zf1976.mayi.upms.biz.pojo.po.SysRole;
import com.zf1976.mayi.upms.biz.pojo.po.SysUser;
import com.zf1976.mayi.upms.biz.pojo.query.Query;
import com.zf1976.mayi.upms.biz.pojo.query.UserQueryParam;
import com.zf1976.mayi.upms.biz.pojo.vo.user.UserVO;
import com.zf1976.mayi.upms.biz.property.FileProperties;
import com.zf1976.mayi.upms.biz.service.base.AbstractService;
import com.zf1976.mayi.upms.biz.service.exception.UserException;
import com.zf1976.mayi.upms.biz.service.exception.enums.UserState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统用户(SysUser)表Service接口
 *
 * @author makejava
 * @since 2020-08-31 11:46:01
 */
@Service
@CacheConfig(namespace = Namespace.USER, dependsOn = {Namespace.DEPARTMENT, Namespace.POSITION, Namespace.ROLE})
public class SysUserService extends AbstractService<SysUserDao, SysUser> {

    private final Logger log = LoggerFactory.getLogger("[UserService]");
    private final byte[] DEFAULT_PASSWORD_BYTE = "123456".getBytes(StandardCharsets.UTF_8);
    private final SysPositionDao sysPositionDao;
    private final SysDepartmentDao sysDepartmentDao;
    private final SysRoleDao sysRoleDao;
    private final SysUserConvert convert;
    private final SecurityClient securityClient;
    private final SecurityProperties securityProperties;
    private final MD5Encoder md5Encoder = new MD5Encoder();

    public SysUserService(SysPositionDao sysPositionDao,
                          SysDepartmentDao sysDepartmentDao,
                          SysRoleDao sysRoleDao,
                          SecurityClient securityClient,
                          SecurityProperties securityProperties) {
        this.sysPositionDao = sysPositionDao;
        this.securityClient = securityClient;
        this.sysDepartmentDao = sysDepartmentDao;
        this.sysRoleDao = sysRoleDao;
        this.securityProperties = securityProperties;
        this.convert = SysUserConvert.INSTANCE;
    }

    /**
     * 按条件分页查询用户
     *
     * @param query request page
     * @return /
     */
    @CachePut(key = "#query", dynamics = true)
    @Transactional(readOnly = true)
    public IPage<UserVO> selectUserPage(Query<UserQueryParam> query) {
        IPage<SysUser> sourcePage;
        // 非super admin 过滤数据权限
        if (!SessionManagement.isOwner()) {
            // 用户可观察数据范围
            Set<Long> dataPermission = securityClient.getUserDetails().getData().getDataPermission();
            List<Long> userIds = super.baseMapper.selectIdsByDepartmentIds(dataPermission);
            sourcePage = super.queryWrapper()
                              .chainQuery(query, () -> {
                                  // 自定义条件
                                  return ChainWrappers.queryChain(super.baseMapper)
                                                      .in(getColumn(SysUser::getId), userIds);
                              }).selectPage();
        } else {
            sourcePage = super.queryWrapper()
                              .chainQuery(query)
                              .selectPage();
        }
        // 根据部门分页
        IPage<SysUser> finalSourcePage = sourcePage;
        Optional.ofNullable(query.getQuery())
                .ifPresent(queryParam -> {
                    if (queryParam.getDepartmentId() != null) {
                        // 当前查询部门
                        Long departmentId = queryParam.getDepartmentId();
                        Set<Long> collectIds = new HashSet<>();
                        this.sysDepartmentDao.selectChildrenById(departmentId)
                                             .stream()
                                             .map(SysDepartment::getId)
                                             .forEach(id -> this.selectDepartmentTreeIds(id, collectIds));
                        collectIds.add(departmentId);
                        List<SysUser> collectUser = finalSourcePage.getRecords()
                                                                   .stream()
                                                                   .filter(sysUser -> collectIds.contains(sysUser.getDepartmentId()))
                                                                   .collect(Collectors.toList());
                        finalSourcePage.setRecords(collectUser);
                    }
                });
        return super.mapPageToTarget(sourcePage, sysUser -> {
            ChainWrappers.lambdaQueryChain(this.sysDepartmentDao)
                         .select(SysDepartment::getId, SysDepartment::getName)
                         .eq(SysDepartment::getId, sysUser.getDepartmentId())
                         .oneOpt()
                         .ifPresent(sysUser::setDepartment);
            return this.convert.toVo(sysUser);
        });
    }

    /**
     * 获取当前部门树下所有id
     *
     * @param departmentId id
     * @param supplier supplier
     */
    @Transactional(readOnly = true)
    public void selectDepartmentTreeIds(Long departmentId, Collection<Long> supplier){
        Assert.notNull(departmentId, "department id can not been null");
        supplier.add(departmentId);
        this.sysDepartmentDao.selectChildrenById(departmentId)
                             .stream()
                             .map(SysDepartment::getId)
                             .forEach(id -> {
                                 // 继续下一级子节点
                                 this.selectDepartmentTreeIds(id, supplier);
                             });
    }

    /**
     * 获取用户所有角色id
     *
     * @param id id
     * @return role id
     */
    public Set<Long> selectUserRoleIds(Long id) {
        return this.sysRoleDao.selectBatchByUserId(id)
                              .stream()
                              .map(SysRole::getId)
                              .collect(Collectors.toSet());
    }

    /**
     * 获取用户职位id
     *
     * @param id id
     * @return position id
     */
    public Set<Long> selectUserPositionIds(Long id) {
        return this.sysPositionDao.selectBatchByUserId(id)
                                  .stream()
                                  .map(SysPosition::getId)
                                  .collect(Collectors.toSet());
    }

    /**
     * 更新用户状态
     *
     * @param id      id
     * @param enabled enabled
     * @return /
     */
    @CacheEvict
    @Transactional(rollbackFor = Exception.class)
    public Void updateUserStatus(Long id, Boolean enabled) {
        SysUser sysUser = super.lambdaQuery()
                               .eq(SysUser::getId, id)
                               .oneOpt()
                               .orElseThrow(() -> new UserException(UserState.USER_NOT_FOUND));

        Long sessionId = SessionManagement.getSessionId();
        // 禁止操作oneself,当前session ID与操作ID相等，说明操作到是当前用户
        Validator.of(sessionId)
                 .withValidated(sId -> !sId.equals(id),
                         () -> new UserException(UserState.USER_OPT_DISABLE_ONESELF_ERROR));
        // 禁止禁用管理员
        Validator.of(sysUser)
                 .withValidated(user -> !user.getUsername()
                                             .equals(this.securityProperties.getOwner()),
                         () -> new UserException(UserState.USER_OPT_DISABLE_ONESELF_ERROR));
        // 设置状态
        sysUser.setEnabled(enabled);
        // 更新
        super.savaOrUpdate(sysUser);
        return null;
    }

    /**
     * 修改头像
     *
     * @param multipartFile 上传头像
     * @return /
     */
    @CacheEvict
    @Transactional(rollbackFor = Exception.class)
    public Void updateAvatar(MultipartFile multipartFile) {
        final SysUser sysUser = super.lambdaQuery()
                                     .select(SysUser::getId, SysUser::getAvatarName)
                                     .eq(SysUser::getId, SessionManagement.getSessionId())
                                     .one();
        String filename = null;
        try {
            // 原文件名
            final String avatarName = sysUser.getAvatarName();
            // 上传文件名
            String originalFilename = multipartFile.getOriginalFilename();
            Assert.notNull(originalFilename, "filename cannot been null!");
            // 新文件名
            filename = UUIDUtil.getUpperCaseUuid() + originalFilename.substring(originalFilename.lastIndexOf("."));
            // 写入新头像文件
            multipartFile.transferTo(Paths.get(FileProperties.getAvatarRealPath(), filename));
            // 实体保存新文件名
            sysUser.setAvatarName(filename);
            // 进行更新
            super.savaOrUpdate(sysUser);
            // 删除旧头像文件
            Files.deleteIfExists(Paths.get(FileProperties.getAvatarRealPath(), avatarName));
        } catch (Exception e) {
            this.log.error(e.getMessage(), e.getCause());
            // 防止文件已经创建，但写入不完全，进行删除处理
            if (filename != null) {
                try {
                    Files.deleteIfExists(Paths.get(FileProperties.getAvatarRealPath(), filename));
                } catch (IOException ioException) {
                    log.error(ioException.getMessage(), ioException.getCause());
                }
            }
            throw new BusinessException(BusinessMsgState.UPLOAD_ERROR);
        }
        return null;
    }

    /**
     * 修改密码
     *
     * @param dto dto
     * @return /
     */
    @CacheEvict
    @Transactional(rollbackFor = Exception.class)
    public Void updatePassword(UpdatePasswordDTO dto) {
        final SysUser sysUser = super.lambdaQuery()
                                     .select(SysUser::getId, SysUser::getPassword)
                                     .eq(SysUser::getId, SessionManagement.getSessionId())
                                     .oneOpt()
                                     .orElseThrow(() -> new BusinessException(BusinessMsgState.CODE_NOT_FOUNT));
        String rawPassword;
        String freshPassword;
        try {
            rawPassword = RsaUtil.decryptByPrivateKey(RsaProperties.PRIVATE_KEY, dto.getOldPass());
            freshPassword = RsaUtil.decryptByPrivateKey(RsaProperties.PRIVATE_KEY, dto.getNewPass());
        } catch (Exception e) {
            throw new BusinessException(BusinessMsgState.OPT_ERROR);
        }

        // 校验原密码，新密码是否为空
        if (StringUtils.isEmpty(rawPassword) || StringUtils.isEmpty(freshPassword)) {
            throw new BusinessException(BusinessMsgState.NULL_PASSWORD);
        }

        Validator.of(freshPassword)
                 // 校验密码是否重复
                 .withValidated(value -> ObjectUtils.nullSafeEquals(value, rawPassword),
                         () -> new BusinessException(BusinessMsgState.PASSWORD_REPEAT))
                 //校验密码合格性
                 .withValidated(ValidateUtil::isPassword,
                         () -> new BusinessException(BusinessMsgState.PASSWORD_LOW));

        // 密码匹配校验
        Validator.of(rawPassword)
                 .withValidated(value -> md5Encoder.matches(value, sysUser.getPassword()),
                         () -> new BusinessException(BusinessMsgState.PASSWORD_REPEAT));

        // 设置新密码
        sysUser.setPassword(md5Encoder.encode(freshPassword));
        // 更新实体
        super.savaOrUpdate(sysUser);
        // 强制用户重新登陆
        SessionManagement.removeSession();
        return null;
    }

    /**
     * 修改邮箱
     *
     * @param code 验证码
     * @param dto dto
     * @return /
     */
    @CacheEvict
    @Transactional(rollbackFor = Exception.class)
    public Void updateEmail(String code, UpdateEmailDTO dto) {

        // 校验验证码是否为空
        if (StringUtils.isEmpty(code) || ObjectUtils.isEmpty(dto)) {
            throw new BusinessException(BusinessMsgState.PARAM_ILLEGAL);
        }

        ValidateEmailService validateService = ValidateEmailService.validateEmailService();
        // 查询用户
        var sysUser = super.lambdaQuery()
                           .select(SysUser::getId, SysUser::getPassword, SysUser::getEmail)
                           .eq(SysUser::getId, SessionManagement.getSessionId())
                           .oneOpt()
                           .orElseThrow(() -> new UserException(UserState.USER_NOT_FOUND));
        if (validateService.validateVerifyCode(dto.getEmail(), code)) {
            try {
                String rawPassword = RsaUtil.decryptByPrivateKey(RsaProperties.PRIVATE_KEY, dto.getPassword());
                if (md5Encoder.matches(rawPassword, sysUser.getPassword())) {
                    // 验证邮箱是否重复
                    if (ObjectUtils.nullSafeEquals(sysUser.getEmail(), dto.getEmail())) {
                        throw new BusinessException(BusinessMsgState.EMAIL_EXISTING);
                    }
                    // 设置新邮箱
                    sysUser.setEmail(dto.getEmail());
                    // 更新实体
                    super.savaOrUpdate(sysUser);
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(BusinessMsgState.OPT_ERROR);
            }finally {
                validateService.clearVerifyCode(dto.getEmail());
            }
        }
        return null;
    }

    /**
     * 个人中心信息修改
     *
     * @param dto dto
     * @return /
     */
    @CacheEvict
    @Transactional(rollbackFor = Exception.class)
    public Void updateInformation(UpdateInfoDTO dto) {
        // 查询当前用户是否存在
        SysUser sysUser = super.lambdaQuery()
                               .eq(SysUser::getId, dto.getId())
                               .oneOpt()
                               .orElseThrow(() -> new BusinessException(BusinessMsgState.DATA_NOT_FOUNT));
        // 不允许非手机号
        if (!ValidateUtil.isPhone(dto.getPhone())) {
            throw new BusinessException(BusinessMsgState.NOT_PHONE);
        }

        // 校验手机号是否变化
        if (dto.getPhone() != null && !ObjectUtils.nullSafeEquals(dto.getPhone(), sysUser.getPhone())) {
            // 校验手机号是否已存在
            super.lambdaQuery()
                 .eq(SysUser::getPhone, dto.getPhone())
                 .oneOpt()
                 .ifPresent(var -> {
                     throw new BusinessException(BusinessMsgState.PHONE_EXISTING);
                 });
            sysUser.setPhone(dto.getPhone());
        }
        sysUser.setGender(dto.getGender());
        sysUser.setNickName(dto.getNickName());
        super.savaOrUpdate(sysUser);
        return null;
    }

    /**
     * 新增用户
     *
     * @param dto dto
     * @return /
     */
    @CacheEvict
    @Transactional(rollbackFor = Exception.class)
    public Void saveUser(UserDTO dto) {
        this.validateUsername(dto.getUsername());
        this.validatePhone(dto.getPhone());
        super.lambdaQuery()
             .select(SysUser::getUsername,SysUser::getEmail, SysUser::getPhone)
             .and(sysUserLambdaQueryWrapper -> {
                 // 校验用户名是否唯一
                 // 校验邮箱是否唯一
                 // 校验手机号是否唯一
                 sysUserLambdaQueryWrapper.eq(SysUser::getUsername, dto.getUsername())
                                          .or()
                                          .eq(SysUser::getEmail, dto.getEmail())
                                          .or()
                                          .eq(SysUser::getPhone, dto.getPhone());
             })
             .list()
             .forEach(sysUser -> super.validateFields(sysUser, dto, collection -> {
                 if (!CollectionUtils.isEmpty(collection)) {
                     throw new UserException(UserState.USER_INFO_EXISTING, collection.toString());
                 }
             }));
        // 转实体
        SysUser sysUser = this.convert.toEntity(dto);
        // 设置加密密码
        sysUser.setPassword(DigestUtils.md5DigestAsHex(DEFAULT_PASSWORD_BYTE));
        // 保存用户
        super.savaOrUpdate(sysUser);
        // 保存用户职位关联
        super.baseMapper.savePositionRelationById(sysUser.getId(), dto.getPositionIds());
        // 保存用户角色关联
        super.baseMapper.savaRoleRelationById(sysUser.getId(), dto.getRoleIds());
        return null;
    }

    /**
     * 更新用户
     *
     * @param dto dto
     * @return /
     */
    @CacheEvict
    @Transactional(rollbackFor = Exception.class)
    public Void updateUser(UserDTO dto) {
        this.validatePhone(dto.getPhone());
        // 查询用户是否存在
        SysUser sysUser = super.lambdaQuery()
                                .eq(SysUser::getId, dto.getId())
                                .oneOpt().orElseThrow(() -> new UserException(UserState.USER_NOT_FOUND));
        // 禁用
        if (!dto.getEnabled()) {
            Long sessionId = SessionManagement.getSessionId();
            // 禁止禁用oneself,禁止操作当前登录的用户
            Validator.of(sessionId)
                     .withValidated(id -> !id.equals(sysUser.getId()),
                             () -> new UserException(UserState.USER_OPT_DISABLE_ONESELF_ERROR));
            // 禁止禁用管理员
            Validator.of(dto.getUsername())
                     .withValidated(username -> !username.equals(this.securityProperties.getOwner()),
                             () -> new UserException(UserState.USER_OPT_ERROR));
            // 踢出当前被禁用用户
            SessionManagement.removeSession(sysUser.getId());
        }
        // 验证用户名，邮箱，手机是否已存在
        super.lambdaQuery()
             .select(SysUser::getUsername, SysUser::getEmail, SysUser::getPhone)
             .ne(SysUser::getId, dto.getId())
             .and(queryWrapper -> queryWrapper.eq(SysUser::getUsername, dto.getUsername())
                                              .or()
                                              .eq(SysUser::getEmail, dto.getEmail())
                                              .or()
                                              .eq(SysUser::getPhone, dto.getPhone()))
             .list()
             .forEach(entity -> super.validateFields(entity, dto, collection -> {
                 if (!CollectionUtils.isEmpty(collection)) {
                     throw new UserException(UserState.USER_INFO_EXISTING, collection.toString());
                 }
             }));
        // 复制属性
        this.convert.copyProperties(dto, sysUser);
        // 更新
        super.savaOrUpdate(sysUser);
        // 更新依赖
        this.updateDependents(dto);
        return null;
    }

    /**
     * 更新用户依赖关系
     *
     * @param dto dto
     */
    private void updateDependents(UserDTO dto) {
        // 用户id
        final Long userId = dto.getId();
        // 用户岗位id集合
        final Set<Long> positionIds = dto.getPositionIds();
        if (!CollectionUtils.isEmpty(positionIds)) {
            super.baseMapper.deletePositionRelationById(userId);
            super.baseMapper.savePositionRelationById(userId, positionIds);
        }
        // 用户角色id集合
        final Set<Long> roleIds = dto.getRoleIds();
        if (!CollectionUtils.isEmpty(roleIds)) {
            super.baseMapper.deleteRoleRelationById(userId);
            super.baseMapper.savaRoleRelationById(dto.getId(), roleIds);
        }
    }

    /**
     * 验证手机号
     *
     * @param phone phone
     */
    private void validatePhone(String phone) {
        if (ValidateUtil.isNotPhone(phone)) {
            throw new BusinessException(BusinessMsgState.NOT_PHONE);
        }
    }

    /**
     * 验证用户名
     *
     * @date 2021-05-16 12:06:34
     * @param username 用户名
     */
    private void validateUsername(String username) {
        if (!ValidateUtil.isUserName(username)) {
            throw new BusinessException(BusinessMsgState.USERNAME_LOW);
        }
    }

    /**
     * 删除用户
     *
     * @param ids ids
     * @return /
     */
    @CacheEvict
    @Transactional(rollbackFor = Exception.class)
    public Void deleteUser(Set<Long> ids) {
        // 超级管理员
        if (SessionManagement.isOwner()) {
            SysUser sysUser = super.lambdaQuery()
                                   .eq(SysUser::getUsername, SessionManagement.getCurrentUsername())
                                   .oneOpt().orElseThrow(() -> new UserException(UserState.USER_NOT_FOUND));
            // 禁止删除超级管理员账号
            if (ids.contains(sysUser.getId())) {
                throw new UserException(UserState.USER_OPT_ERROR);
            }
        }
        // 删除用户关系依赖
        for (Long id : ids) {
            super.baseMapper.deleteRoleRelationById(id);
            super.baseMapper.deletePositionRelationById(id);
            try {
                // 登出用户
                final Session session = SessionManagement.getSession(id);
                this.securityClient.logout(session.getToken());
            } catch (Exception e) {
                log.info(e.getMessage(), e.getCause());
            }
        }
        // 删除用户
        super.deleteByIds(ids);
        return null;
    }
}