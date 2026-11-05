package com.smartadmin.admin.module.system.message;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.smartadmin.admin.constant.AdminSwaggerTagConst;
import com.smartadmin.base.common.domain.PageResult;
import com.smartadmin.base.common.domain.ResponseDTO;
import com.smartadmin.base.common.domain.ValidateList;
import com.smartadmin.base.module.support.message.domain.MessageQueryForm;
import com.smartadmin.base.module.support.message.domain.MessageSendForm;
import com.smartadmin.base.module.support.message.domain.MessageVO;
import com.smartadmin.base.module.support.message.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 后管 消息路由
 */
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_MESSAGE)
@RestController
public class AdminMessageController {

    @Autowired
    private MessageService messageService;

    @Operation(summary = "通知消息-新建")
    @PostMapping("/message/sendMessages")
    @SaCheckPermission("system:message:send")
    public ResponseDTO<String> sendMessages(@RequestBody @Valid ValidateList<MessageSendForm> messageList) {
        messageService.sendMessage(messageList);
        return ResponseDTO.ok();
    }

    @Operation(summary = "通知消息-分页查询")
    @PostMapping("/message/query")
    @SaCheckPermission("system:message:query")
    public ResponseDTO<PageResult<MessageVO>> query(@RequestBody @Valid MessageQueryForm queryForm) {
        return ResponseDTO.ok(messageService.query(queryForm));
    }

    @Operation(summary = "通知消息-删除")
    @GetMapping("/message/delete/{messageId}")
    @SaCheckPermission("system:message:delete")
    public ResponseDTO<String> delete(@PathVariable Long messageId) {
        return messageService.delete(messageId);
    }

}
