package com.smartadmin.base.module.support.message.constant;


import lombok.AllArgsConstructor;
import lombok.Getter;
import com.smartadmin.base.common.enumeration.BaseEnum;


/**
 * 消息类型
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum implements BaseEnum {

    MAIL(1, "站内信"),

    ORDER(2, "订单"),
    ;

    private final Integer value;

    private final String desc;
}
