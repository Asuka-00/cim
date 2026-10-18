package com.scaffold.mes.exec.domain.service.interlock;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.InterlockLog;
import com.scaffold.mes.exec.domain.repository.InterlockLogRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * InterlockEngine 进站规则注册表
 * <p>
 * M3 的六条规则在这里固定注册，不能关掉。任何一条 BLOCK 都拒绝进站。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class InterlockEngine {

    private final LotRepository lotRepository;
    private final InterlockLogRepository logs;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 跑完全部规则。有阻断时抛出。
     *
     * @param context 进站材料
     * @throws BusinessException 有规则阻断
     */
    public void requirePass(TrackInContext context) {
        List<InterlockRule> rules = List.of(
                new LotStateRule(),
                new StepMatchRule(),
                new EqpAllowedRule(),
                new EqpStateRule(),
                new EqpCapacityRule(lotRepository),
                new RecipeAllowedRule());
        for (InterlockRule rule : rules) {
            InterlockHit hit = rule.evaluate(context);
            if ("BLOCK".equals(hit.severity())) {
                String detail = hit.rule() + ": " + hit.message();
                InterlockLog log = new InterlockLog();
                log.setLotId(context.lot().getId());
                log.setLotNo(context.lot().getLotNo());
                log.setStepId(context.stepId());
                log.setEquipmentId(context.equipmentId());
                log.setAction("TRACK_IN");
                log.setResult("BLOCK");
                log.setDetail(detail);
                log.setOperator(session.getCurrentUserNameOrDefault());
                log.setCheckedAt(LocalDateTime.now(clock));
                logs.save(log);
                throw new BusinessException(MesExecErrorCodes.INTERLOCK_BLOCK, detail);
            }
        }
    }

    private static final class LotStateRule implements InterlockRule {
        public String code() {
            return "LOT_STATE";
        }

        public InterlockHit evaluate(TrackInContext context) {
            int holds = context.lot().getHoldCount() == null ? 0 : context.lot().getHoldCount();
            if (!"WAITING".equals(context.lot().getStatus()) || holds > 0) {
                return InterlockHit.block(code(), "Lot 必须是未 Hold 的 WAITING");
            }
            return InterlockHit.pass(code());
        }
    }

    private static final class StepMatchRule implements InterlockRule {
        public String code() {
            return "STEP_MATCH";
        }

        public InterlockHit evaluate(TrackInContext context) {
            if (!context.stepId().equals(context.lot().getStepId())) {
                return InterlockHit.block(code(), "操作站点与 Lot 当前步骤不一致");
            }
            return InterlockHit.pass(code());
        }
    }

    private static final class EqpAllowedRule implements InterlockRule {
        public String code() {
            return "EQP_ALLOWED";
        }

        public InterlockHit evaluate(TrackInContext context) {
            if (!context.facts().needEquipment()) {
                return InterlockHit.skip(code());
            }
            if (context.equipmentId() == null || !context.facts().equipmentAllowed()) {
                return InterlockHit.block(code(), "设备不能在该站点加工");
            }
            return InterlockHit.pass(code());
        }
    }

    private static final class EqpStateRule implements InterlockRule {
        private static final Set<String> DOWN = Set.of("SCHEDULED_DOWN", "UNSCHEDULED_DOWN", "NON_SCHEDULED");
        private static final Set<String> QUAL = Set.of("MONITOR", "ENGINEERING", "TEST");

        public String code() {
            return "EQP_STATE";
        }

        public InterlockHit evaluate(TrackInContext context) {
            if (!context.facts().needEquipment()) {
                return InterlockHit.skip(code());
            }
            String state = context.facts().equipmentState();
            if (!DOWN.contains(state)) {
                return InterlockHit.pass(code());
            }
            boolean qual = ("ENGINEERING".equals(state) || "SCHEDULED_DOWN".equals(state))
                    && QUAL.contains(context.lot().getLotType());
            if (qual) {
                return InterlockHit.pass(code());
            }
            return InterlockHit.block(code(), "设备处于停机状态");
        }
    }

    private static final class EqpCapacityRule implements InterlockRule {
        private final LotRepository lots;

        private EqpCapacityRule(LotRepository lots) {
            this.lots = lots;
        }

        public String code() {
            return "EQP_CAPACITY";
        }

        public InterlockHit evaluate(TrackInContext context) {
            if (!context.facts().needEquipment()) {
                return InterlockHit.skip(code());
            }
            if (lots.countOccupancy(context.equipmentId(), context.lot().getBatchId()) >= context.facts().equipmentCapacity()) {
                return InterlockHit.block(code(), "设备同时加工数已满");
            }
            Integer portCap = context.facts().portCapacity();
            if (context.portId() != null && portCap != null && lots.countRunningOnPort(context.portId()) >= portCap) {
                return InterlockHit.block(code(), "Port 同时加工数已满");
            }
            return InterlockHit.pass(code());
        }
    }

    private static final class RecipeAllowedRule implements InterlockRule {
        public String code() {
            return "RECIPE_ALLOWED";
        }

        public InterlockHit evaluate(TrackInContext context) {
            if (!context.facts().needRecipe()) {
                return InterlockHit.skip(code());
            }
            if (context.recipeVersionId() == null || !context.facts().recipeAllowed()) {
                return InterlockHit.block(code(), "Recipe 版本不可用");
            }
            return InterlockHit.pass(code());
        }
    }
}
