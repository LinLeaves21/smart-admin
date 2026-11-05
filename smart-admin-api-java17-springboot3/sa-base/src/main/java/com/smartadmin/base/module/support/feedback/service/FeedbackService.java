package com.smartadmin.base.module.support.feedback.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.smartadmin.base.common.domain.PageResult;
import com.smartadmin.base.common.domain.RequestUser;
import com.smartadmin.base.common.domain.ResponseDTO;
import com.smartadmin.base.common.util.SmartBeanUtil;
import com.smartadmin.base.common.util.SmartPageUtil;
import com.smartadmin.base.module.support.feedback.dao.FeedbackDao;
import com.smartadmin.base.module.support.feedback.domain.FeedbackAddForm;
import com.smartadmin.base.module.support.feedback.domain.FeedbackEntity;
import com.smartadmin.base.module.support.feedback.domain.FeedbackQueryForm;
import com.smartadmin.base.module.support.feedback.domain.FeedbackVO;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 意见反馈
 */
@Service
public class FeedbackService {

    @Resource
    private FeedbackDao feedbackDao;

    /**
     * 分页查询
     */
    public ResponseDTO<PageResult<FeedbackVO>> query(FeedbackQueryForm queryForm) {
        Page page = SmartPageUtil.convert2PageQuery(queryForm);
        List<FeedbackVO> list = feedbackDao.queryPage(page, queryForm);
        PageResult<FeedbackVO> pageResultDTO = SmartPageUtil.convert2PageResult(page, list);
        if (pageResultDTO.getEmptyFlag()) {
            return ResponseDTO.ok(pageResultDTO);
        }
        return ResponseDTO.ok(pageResultDTO);
    }

    /**
     * 新建
     */
    public ResponseDTO<String> add(FeedbackAddForm addForm, RequestUser requestUser) {
        FeedbackEntity feedback = SmartBeanUtil.copy(addForm, FeedbackEntity.class);
        feedback.setUserType(requestUser.getUserType().getValue());
        feedback.setUserId(requestUser.getUserId());
        feedback.setUserName(requestUser.getUserName());
        feedbackDao.insert(feedback);
        return ResponseDTO.ok();
    }
}
