package com.smartadmin.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.smartadmin.base.common.controller.SupportBaseController;
import com.smartadmin.base.common.domain.PageResult;
import com.smartadmin.base.common.domain.ResponseDTO;
import com.smartadmin.base.constant.SwaggerTagConst;
import com.smartadmin.base.module.support.config.ConfigService;
import com.smartadmin.base.module.support.config.domain.ConfigAddForm;
import com.smartadmin.base.module.support.config.domain.ConfigQueryForm;
import com.smartadmin.base.module.support.config.domain.ConfigUpdateForm;
import com.smartadmin.base.module.support.config.domain.ConfigVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置
 */
@Tag(name = SwaggerTagConst.Support.CONFIG)
@RestController
public class AdminConfigController extends SupportBaseController {

    @Resource
    private ConfigService configService;

    @Operation(summary = "分页查询系统配置")
    @PostMapping("/config/query")
    @SaCheckPermission("support:config:query")
    public ResponseDTO<PageResult<ConfigVO>> queryConfigPage(@RequestBody @Valid ConfigQueryForm queryForm) {
        return configService.queryConfigPage(queryForm);
    }

    @Operation(summary = "添加配置参数")
    @PostMapping("/config/add")
    @SaCheckPermission("support:config:add")
    public ResponseDTO<String> addConfig(@RequestBody @Valid ConfigAddForm configAddForm) {
        return configService.add(configAddForm);
    }

    @Operation(summary = "修改配置参数")
    @PostMapping("/config/update")
    @SaCheckPermission("support:config:update")
    public ResponseDTO<String> updateConfig(@RequestBody @Valid ConfigUpdateForm updateForm) {
        return configService.updateConfig(updateForm);
    }

}
