package com.scaffold.system.domain.service.system;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.share.context.TenantContextHolder;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.integration.system.provider.MesTenantSeedProvider;
import com.scaffold.system.domain.entity.rbac.Role;
import com.scaffold.system.domain.entity.rbac.RolePermission;
import com.scaffold.system.domain.entity.system.DictItem;
import com.scaffold.system.domain.entity.system.DictType;
import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RolePermissionRepository;
import com.scaffold.system.domain.repository.rbac.RoleRepository;
import com.scaffold.system.domain.repository.system.DictItemRepository;
import com.scaffold.system.domain.repository.system.DictTypeRepository;
import com.scaffold.system.domain.repository.system.SystemConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * MesTenantSeedService MES 系统种子
 * <p>
 * 幂等写入字典、参数,并把已有 MES 权限授给 default 角色;没有该角色时创建 MES_ADMIN。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class MesTenantSeedService implements MesTenantSeedProvider {

    private static final String MES_ADMIN = "MES_ADMIN";

    private static final List<String> MES_PERMISSIONS = List.of(
            "APP_MES_PARENT",
            "APP_MES_SEQUENCE_RULE",
            "mes:sequence-rule:add",
            "mes:sequence-rule:edit",
            "mes:sequence-rule:delete",
            "APP_MES_COUNT_UNIT",
            "mes:count-unit:add",
            "mes:count-unit:edit",
            "mes:count-unit:delete",
            "APP_MES_UOM",
            "mes:uom:add",
            "mes:uom:edit",
            "mes:uom:delete",
            "APP_MES_UOM_CONVERSION",
            "mes:uom-conversion:add",
            "mes:uom-conversion:edit",
            "mes:uom-conversion:delete",
            "APP_MES_PRODUCT_FAMILY",
            "mes:product-family:add",
            "mes:product-family:edit",
            "mes:product-family:delete",
            "APP_MES_CUSTOMER",
            "mes:customer:add",
            "mes:customer:edit",
            "mes:customer:delete",
            "APP_MES_PRODUCT",
            "mes:product:add",
            "mes:product:edit",
            "mes:product:delete",
            "APP_MES_AREA",
            "mes:area:add",
            "mes:area:edit",
            "mes:area:delete",
            "APP_MES_STEP",
            "mes:step:add",
            "mes:step:edit",
            "mes:step:delete",
            "APP_MES_STEP_GROUP",
            "mes:step-group:add",
            "mes:step-group:edit",
            "mes:step-group:delete",
            "APP_MES_ROUTE",
            "mes:route:add",
            "mes:route:edit",
            "mes:route:delete",
            "APP_MES_BOM",
            "mes:bom:add",
            "mes:bom:edit",
            "mes:bom:delete",
            "APP_MES_APPROVAL",
            "APP_MES_EQUIPMENT_GROUP",
            "mes:equipment-group:add",
            "mes:equipment-group:edit",
            "mes:equipment-group:delete",
            "APP_MES_EQUIPMENT",
            "mes:equipment:add",
            "mes:equipment:edit",
            "mes:equipment:delete",
            "APP_MES_MATERIAL",
            "mes:material:add",
            "mes:material:edit",
            "mes:material:delete",
            "APP_MES_RECIPE",
            "mes:recipe:add",
            "mes:recipe:edit",
            "mes:recipe:delete");

    private final DictTypeRepository dictTypeRepository;
    private final DictItemRepository dictItemRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    /**
     * 在目标租户上下文中写入种子。已存在的编码不覆盖。
     *
     * @param tenantId 租户标识
     */
    @Override
    @Transactional
    public void seed(String tenantId) {
        TenantContextHolder.runAs(tenantId, () -> {
            seedDicts();
            seedConfigs();
            grantMesPermissions();
        });
    }

    private void seedDicts() {
        dict("MES_SCRAP_REASON", "报废原因", item("OTHER", "其他", 1));
        dict("MES_REWORK_REASON", "返工原因", item("OTHER", "其他", 1));
        dict("MES_DEFECT_CODE", "缺陷代码", item("UNKNOWN", "未分类", 1));
        dict("MES_EQP_STOP_REASON", "设备停机原因", item("PM", "保养", 1), item("FAULT", "故障", 2));
        dict("MES_EQP_STATUS_CODE", "设备状态细分", item("RUN", "运行", 1), item("IDLE", "待机", 2), item("DOWN", "停机", 3));
        dict("MES_MATERIAL_STATUS", "物料状态", item("AVAILABLE", "可用", 1), item("FROZEN", "冻结", 2),
                item("QUARANTINE", "隔离", 3));
    }

    private void dict(String type, String name, DictSeed... items) {
        if (dictTypeRepository.findByDictType(type).isEmpty()) {
            DictType dictType = new DictType();
            dictType.setDictType(type);
            dictType.setDictName(name);
            dictType.setEnabled(StateEnum.ENABLED);
            dictTypeRepository.save(dictType);
        }
        for (DictSeed seed : items) {
            if (dictItemRepository.findByDictTypeAndItemCode(type, seed.code()).isPresent()) {
                continue;
            }
            DictItem item = new DictItem();
            item.setDictType(type);
            item.setItemCode(seed.code());
            item.setItemLabel(seed.label());
            item.setSortOrder(seed.sort());
            item.setEnabled(StateEnum.ENABLED);
            dictItemRepository.save(item);
        }
    }

    private void seedConfigs() {
        config("mes.scan.required", "false", "boolean", "终端是否必须扫码");
        config("mes.step.skip.allowed", "false", "boolean", "是否允许跳站");
        config("mes.wafer.track.enabled", "false", "boolean", "是否启用 Wafer 级进出站");
        config("mes.dummy.max.reuse", "0", "int", "Dummy 全局最大使用次数,0 表示用 Wafer 自身上限");
        config("mes.eqp.state.auto", "true", "boolean", "Track 是否联动设备状态");
        config("mes.batch.count.as.one", "true", "boolean", "Batch 是否只占 1 个设备名额");
        config("mes.backflush.shortage", "BLOCK", "string", "关键料反冲不足时 BLOCK,非关键料仍按 BOM 行放行");
        config("mes.convert.deviation.block", "false", "boolean", "单位转换偏差是否阻断出站");
    }

    private void config(String code, String value, String valueType, String remark) {
        if (systemConfigRepository.findByConfigCode(code).isPresent()) {
            return;
        }
        SystemConfig config = new SystemConfig();
        config.setConfigCode(code);
        config.setConfigValue(value);
        config.setValueType(valueType);
        config.setGroupCode("MES");
        config.setRemark(remark);
        config.setState(StateEnum.ENABLED);
        systemConfigRepository.save(config);
    }

    private void grantMesPermissions() {
        Role role = roleRepository.findByRoleCode("default").orElseGet(() -> roleRepository.findByRoleCode(MES_ADMIN)
                .orElseGet(this::createMesAdmin));
        for (String code : MES_PERMISSIONS) {
            permissionRepository.findByPermissionCode(code).ifPresent(permission -> {
                if (rolePermissionRepository.findByRoleIdAndPermissionId(role.getId(), permission.getId()).isEmpty()) {
                    rolePermissionRepository.save(RolePermission.of(role.getId(), permission.getId()));
                }
            });
        }
    }

    private Role createMesAdmin() {
        Role role = new Role();
        role.setRoleCode(MES_ADMIN);
        role.setRoleName("MES 管理员");
        role.setEnabled(StateEnum.ENABLED);
        roleRepository.save(role);
        return role;
    }

    private static DictSeed item(String code, String label, int sort) {
        return new DictSeed(code, label, sort);
    }

    private record DictSeed(String code, String label, int sort) {
    }
}
