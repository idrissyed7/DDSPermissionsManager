package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.Nullable;

import io.micronaut.http.HttpStatus;
import io.unityfoundation.dds.permissions.manager.ResponseStatusCodes;
import io.unityfoundation.dds.permissions.manager.exception.DPMException;
import jakarta.inject.Singleton;

@Singleton
@InterceptorBean(UserIsAdmin.class)
public class UserIsAdminInterceptor implements MethodInterceptor<Object, Object> {

    private final SecurityUtil securityUtil;

    public UserIsAdminInterceptor(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    @Nullable
    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        if (!securityUtil.isCurrentUserAdmin()) {
            throw new DPMException(ResponseStatusCodes.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }
        return context.proceed();
    }
}