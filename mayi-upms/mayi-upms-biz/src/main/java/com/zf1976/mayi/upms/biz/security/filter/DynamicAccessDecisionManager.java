/*
 * Copyright (c) 2021 zf1976
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING COMMUNICATION_AUTHORIZATION,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.zf1976.mayi.upms.biz.security.filter;

import com.zf1976.mayi.upms.biz.security.Context;
import com.zf1976.mayi.upms.biz.security.service.DynamicDataSourceService;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.FilterInvocation;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态访问决策管理器
 *
 * @author mac
 * @date 2020/12/25
 **/
public class DynamicAccessDecisionManager implements AccessDecisionManager {

    private final DynamicDataSourceService dynamicDataSourceService;
    private final AntPathMatcher pathMatcher;
    private static final String SUPER_ADMIN = "ROLE_root";

    public DynamicAccessDecisionManager(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    public void decide(Authentication authentication, Object target, Collection<ConfigAttribute> collection) throws AccessDeniedException, InsufficientAuthenticationException {
        if (Context.isOwner()) {
            return;
        }
        // 过滤调用
        FilterInvocation filterInvocation = (FilterInvocation) target;
        // 请求
        HttpServletRequest request = filterInvocation.getRequest();
        // 请求URI
        String uri = request.getRequestURI();
        // 请求方法
        String method = request.getMethod();
        // URI-Method
        Map<String, String> methodMap = this.dynamicDataSourceService.loadResourceMethodMap();
        // 条件
        boolean condition = false;
        Set<Map.Entry<String, String>> entrySet = methodMap.entrySet();

        // 匹配资源方法
        for (Map.Entry<String, String> entry : entrySet) {
            // eq匹配请求方法
            if (ObjectUtils.nullSafeEquals(entry.getKey(), uri)) {
                if (!ObjectUtils.nullSafeEquals(entry.getValue(), method)) {
                    throw new AccessDeniedException("Resource does not support request the method：" + method);
                }
                condition = true;
                break;
                // 模式匹配
            } else if (pathMatcher.match(entry.getKey(), uri)) {
                if (!ObjectUtils.nullSafeEquals(entry.getValue(), method)) {
                    throw new AccessDeniedException("Resource does not support request the method：" + method);
                }
                condition = true;
                break;
            }
        }

        if (!condition) {
            throw new AccessDeniedException("You do not have permission to access, please contact the administrator");
        }

        // 资源所需要所需要权限
        Set<String> needPermission = collection.stream()
                                               .map(ConfigAttribute::getAttribute)
                                               .collect(Collectors.toSet());
        // 所需权限为空，当即放行
        if (CollectionUtils.isEmpty(needPermission)) {
            return;
        }
        // 用户所有权限
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        condition = authorities.stream()
                               .map(GrantedAuthority::getAuthority)
                               .collect(Collectors.toSet())
                               .containsAll(needPermission);
        if (!condition) {
            throw new AccessDeniedException("You do not have permission to access, please contact the administrator");
        }
    }

    @Override
    public boolean supports(ConfigAttribute configAttribute) {
        return false;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return DynamicAccessDecisionManager.class.isAssignableFrom(aClass);
    }

}
