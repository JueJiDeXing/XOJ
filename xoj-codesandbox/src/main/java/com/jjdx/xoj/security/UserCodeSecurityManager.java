
package com.jjdx.xoj.security;

import java.io.FilePermission;
import java.security.Permission;

public class UserCodeSecurityManager extends SecurityManager {
    private final String allowedPath;

    public UserCodeSecurityManager(String allowedPath) {
        this.allowedPath = allowedPath;
    }

    @Override
    public void checkPermission(Permission perm) {
        // 允许读取class文件
        if (perm instanceof FilePermission &&
                perm.getActions().equals("read") &&
                perm.getName().startsWith(allowedPath)) {
            return;
        }
        // 禁止其他所有权限
        throw new SecurityException("禁止操作: " + perm);
    }

    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("禁止执行命令: " + cmd);
    }
}
