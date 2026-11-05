package com.smartadmin.admin.module.system.position.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.smartadmin.admin.constant.AdminSwaggerTagConst;
import com.smartadmin.admin.module.system.position.domain.form.PositionAddForm;
import com.smartadmin.admin.module.system.position.domain.form.PositionQueryForm;
import com.smartadmin.admin.module.system.position.domain.form.PositionUpdateForm;
import com.smartadmin.admin.module.system.position.domain.vo.PositionVO;
import com.smartadmin.admin.module.system.position.service.PositionService;
import com.smartadmin.base.common.domain.PageResult;
import com.smartadmin.base.common.domain.ResponseDTO;
import com.smartadmin.base.common.domain.ValidateList;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 职务表 Controller
 */

@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_POSITION)
public class PositionController {

    @Resource
    private PositionService positionService;

    @Operation(summary = "分页查询")
    @PostMapping("/position/queryPage")
    public ResponseDTO<PageResult<PositionVO>> queryPage(@RequestBody @Valid PositionQueryForm queryForm) {
        return ResponseDTO.ok(positionService.queryPage(queryForm));
    }

    @Operation(summary = "添加")
    @PostMapping("/position/add")
    public ResponseDTO<String> add(@RequestBody @Valid PositionAddForm addForm) {
        return positionService.add(addForm);
    }

    @Operation(summary = "更新")
    @PostMapping("/position/update")
    public ResponseDTO<String> update(@RequestBody @Valid PositionUpdateForm updateForm) {
        return positionService.update(updateForm);
    }

    @Operation(summary = "批量删除")
    @PostMapping("/position/batchDelete")
    public ResponseDTO<String> batchDelete(@RequestBody ValidateList<Long> idList) {
        return positionService.batchDelete(idList);
    }

    @Operation(summary = "单个删除")
    @GetMapping("/position/delete/{positionId}")
    public ResponseDTO<String> batchDelete(@PathVariable Long positionId) {
        return positionService.delete(positionId);
    }


    @Operation(summary = "不分页查询")
    @GetMapping("/position/queryList")
    public ResponseDTO<List<PositionVO>> queryList() {
        return ResponseDTO.ok(positionService.queryList());
    }
}
