package com.smartadmin.base.module.support.job.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.smartadmin.base.common.enumeration.BaseEnum;

/**
 * job 任务触发类型 枚举类
 **/
@AllArgsConstructor
@Getter
public enum SmartJobTriggerTypeEnum implements BaseEnum {

    /**
     * 1 cron表达式
     */
    CRON("cron", "cron表达式"),

    FIXED_DELAY("fixed_delay", "固定间隔"),

    ;

    private final String value;

    private final String desc;
}

