package com.smartadmin.admin.module.business.oa.enterprise.manager;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartadmin.admin.module.business.oa.enterprise.dao.EnterpriseEmployeeDao;
import com.smartadmin.admin.module.business.oa.enterprise.domain.entity.EnterpriseEmployeeEntity;
import org.springframework.stereotype.Service;

/**
 * 企业员工关系 manager
 */
@Service
public class EnterpriseEmployeeManager extends ServiceImpl<EnterpriseEmployeeDao, EnterpriseEmployeeEntity> {
}