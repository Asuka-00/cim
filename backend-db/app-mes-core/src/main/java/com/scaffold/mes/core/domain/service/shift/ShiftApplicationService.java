package com.scaffold.mes.core.domain.service.shift;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.shift.ShiftCreateRequest;
import com.scaffold.mes.core.domain.entity.shift.Shift;
import com.scaffold.mes.core.domain.repository.shift.ShiftRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * ShiftApplicationService 班次
 * <p>
 * 编码不能重复。结束时间不晚于开始时间时记为跨日。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ShiftApplicationService {

    private final ShiftRepository repository;

    /**
     * 列出全部班次。
     *
     * @return 按编码排列
     */
    public List<Shift> list() {
        return repository.list();
    }

    /**
     * 新建班次。
     *
     * @param request 编码、名称和起止时间
     * @throws BusinessException 编码已存在，或名称、时间为空
     */
    @Transactional
    public void create(ShiftCreateRequest request) {
        if (request.getShiftName() == null || request.getShiftName().isBlank()
                || request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException(MesErrorCodes.SHIFT_INVALID, "班次名称和时间不能为空");
        }
        if (repository.findByCode(request.getShiftCode()).isPresent()) {
            throw new BusinessException(MesErrorCodes.SHIFT_INVALID, "班次编码已存在");
        }
        Shift shift = new Shift();
        shift.setShiftCode(request.getShiftCode());
        shift.setShiftName(request.getShiftName());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        shift.setCrossDay(request.getEndTime().isAfter(request.getStartTime()) ? 0 : 1);
        repository.save(shift);
    }
}
