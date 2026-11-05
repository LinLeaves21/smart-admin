package com.smartadmin.admin.module.system.datascope.domain;

import lombok.Data;
import com.smartadmin.admin.module.system.datascope.constant.DataScopeTypeEnum;
import com.smartadmin.admin.module.system.datascope.constant.DataScopeWhereInTypeEnum;

/**
 * 数据范围
 */
@Data
public class DataScopeSqlConfig {

    /**
     * 数据范围类型
     * {@link DataScopeTypeEnum}
     */
    private DataScopeTypeEnum dataScopeType;

    /**
     * join sql 具体实现类
     */
    private Class<?> joinSqlImplClazz;

    private String joinSql;

    private Integer whereIndex;

    private String paramName;

    /**
     * whereIn类型
     * {@link DataScopeWhereInTypeEnum}
     */
    private DataScopeWhereInTypeEnum dataScopeWhereInType;
}
