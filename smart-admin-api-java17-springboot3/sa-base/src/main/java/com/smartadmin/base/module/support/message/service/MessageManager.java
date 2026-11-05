package com.smartadmin.base.module.support.message.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartadmin.base.module.support.message.dao.MessageDao;
import com.smartadmin.base.module.support.message.domain.MessageEntity;
import org.springframework.stereotype.Service;

/**
 * 消息manager
 */
@Service
public class MessageManager extends ServiceImpl<MessageDao, MessageEntity> {


}
