package com.smartadmin.admin.util;

import com.smartadmin.admin.module.system.login.domain.RequestEmployee;
import com.smartadmin.base.common.domain.RequestUser;
import com.smartadmin.base.common.util.SmartRequestUtil;

/**
 * admin 端的请求工具类
 */
public final class AdminRequestUtil {


    public static RequestEmployee getRequestUser() {
        return (RequestEmployee) SmartRequestUtil.getRequestUser();
    }

    public static Long getRequestUserId() {
        RequestUser requestUser = getRequestUser();
        return null == requestUser ? null : requestUser.getUserId();
    }


}
