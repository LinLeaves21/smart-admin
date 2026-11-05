package com.smartadmin.base.module.support.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.smartadmin.base.common.controller.SupportBaseController;
import com.smartadmin.base.common.domain.PageResult;
import com.smartadmin.base.common.domain.RequestUser;
import com.smartadmin.base.common.domain.ResponseDTO;
import com.smartadmin.base.common.util.SmartRequestUtil;
import com.smartadmin.base.constant.SwaggerTagConst;
import com.smartadmin.base.module.support.message.domain.MessageQueryForm;
import com.smartadmin.base.module.support.message.domain.MessageVO;
import com.smartadmin.base.module.support.message.service.MessageService;
import org.springframework.web.bind.annotation.*;

/**
 * 消息
 */
@RestController
@Tag(name = SwaggerTagConst.Support.MESSAGE)
public class MessageController extends SupportBaseController {

    @Resource
    private MessageService messageService;

    @Operation(summary = "分页查询我的消息 @luoyi")
    @PostMapping("/message/queryMyMessage")
    public ResponseDTO<PageResult<MessageVO>> query(@RequestBody @Valid MessageQueryForm queryForm) {
        RequestUser user = SmartRequestUtil.getRequestUser();
        if(user == null){
            return ResponseDTO.userErrorParam("用户未登录");
        }

        queryForm.setSearchCount(false);
        queryForm.setReceiverUserId(user.getUserId());
        queryForm.setReceiverUserType(user.getUserType().getValue());
        return ResponseDTO.ok(messageService.query(queryForm));
    }

    @Operation(summary = "查询未读消息数量 @luoyi")
    @GetMapping("/message/getUnreadCount")
    public ResponseDTO<Long> getUnreadCount() {
        RequestUser user = SmartRequestUtil.getRequestUser();
        if(user == null){
            return ResponseDTO.userErrorParam("用户未登录");
        }
        return ResponseDTO.ok(messageService.getUnreadCount(user.getUserType(), user.getUserId()));
    }

    @Operation(summary = "更新已读 @luoyi")
    @GetMapping("/message/read/{messageId}")
    public ResponseDTO<String> updateReadFlag(@PathVariable Long messageId) {
        RequestUser user = SmartRequestUtil.getRequestUser();
        if(user == null){
            return ResponseDTO.userErrorParam("用户未登录");
        }

        messageService.updateReadFlag(messageId, user.getUserType(), user.getUserId());
        return ResponseDTO.ok();
    }

}
