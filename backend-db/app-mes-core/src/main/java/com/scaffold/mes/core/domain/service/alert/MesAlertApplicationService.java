package com.scaffold.mes.core.domain.service.alert;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.entity.alert.MesAlert;
import com.scaffold.mes.core.domain.repository.alert.MesAlertRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * MesAlertApplicationService 预警应用服务
 * <p>
 * 查询预警并变更已读、关闭状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class MesAlertApplicationService {

    private final MesAlertRepository alertRepository;
    private final CurrentSessionProvider session;

    /**
     * 分页查询预警。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param status 状态,空表示不限
     * @return 分页结果
     */
    public PageDTO<MesAlert> page(int page, int pageSize, String status) {
        return alertRepository.page(page, pageSize, status);
    }

    /**
     * 写入一条系统预警。不抛业务异常，调用方据此决定是否继续原事务。
     *
     * @param subjectType 对象类型
     * @param subjectId 对象主键
     * @param title 标题
     * @param content 内容
     */
    @Transactional
    public void raiseSystem(String subjectType, Long subjectId, String title, String content) {
        MesAlert alert = new MesAlert();
        alert.setAlertType("SYSTEM");
        alert.setLevel("WARN");
        alert.setSubjectType(subjectType);
        alert.setSubjectId(subjectId);
        alert.setTitle(title);
        alert.setContent(content);
        alert.setStatus("NEW");
        alert.setReceiverScope("ALL");
        alertRepository.save(alert);
    }

    /**
     * 把预警标记为已读,并记录阅读人。
     *
     * @param id 预警主键
     * @throws BusinessException 预警不存在
     */
    @Transactional
    public void read(Long id) {
        MesAlert alert = require(id);
        alert.setStatus("READ");
        alert.setReadBy(session.getCurrentUserNameOrDefault());
        alert.setReadAt(LocalDateTime.now());
        alertRepository.update(alert);
    }

    /**
     * 关闭预警。
     *
     * @param id 预警主键
     * @throws BusinessException 预警不存在
     */
    @Transactional
    public void close(Long id) {
        MesAlert alert = require(id);
        alert.setStatus("CLOSED");
        alertRepository.update(alert);
    }

    private MesAlert require(Long id) {
        return alertRepository.findById(id).orElseThrow(() -> new BusinessException(MesErrorCodes.ALERT_NOT_FOUND, "预警不存在"));
    }
}
