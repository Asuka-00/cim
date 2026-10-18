package com.scaffold.system.domain.service.system;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.converter.system.SystemConfigConverter;
import com.scaffold.system.domain.dto.system.SystemConfigCreateRequestDTO;
import com.scaffold.system.domain.dto.system.SystemConfigUpdateRequestDTO;
import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.query.system.SystemConfigQuery;
import com.scaffold.system.domain.repository.rbac.OAuth2RegistrationRepository;
import com.scaffold.system.domain.repository.system.SystemConfigRepository;
import com.scaffold.system.domain.service.rbac.OAuth2SettingsService;
import com.scaffold.system.domain.vo.system.SystemConfigVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SystemConfigApplicationService服务类
 * <p>
 * 负责处理SystemConfigApplicationService相关业务编排与领域逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigApplicationService {

        private final SystemConfigRepository systemConfigRepository;
        private final SystemConfigConverter systemConfigConverter;
        private final OAuth2RegistrationRepository dynamicClientRegistrationRepository;

        /**
         * 新增系统配置
         */
        @Transactional
        public void insertSystemConfig(SystemConfigCreateRequestDTO createRequest) {
                systemConfigRepository.findByConfigCode(createRequest.getConfigCode())
                                .ifPresent(config -> {
                                        throw new BusinessException(ErrorCodeConstants.SYSTEM_CONFIG_CODE_EXISTS,
                                                        "配置编码已存在: " + createRequest.getConfigCode());
                                });

                validateConfigValue(createRequest.getValueType(), createRequest.getConfigValue());
                if (createRequest.getConfigValue() == null) {
                        createRequest.setConfigValue("");
                }

                // 创建系统配置实体
                SystemConfig systemConfig = systemConfigConverter.toEntity(createRequest);

                // 保存系统配置
                systemConfigRepository.save(systemConfig);
                log.info("System config created: configCode={}, configId={}", systemConfig.getConfigCode(),
                                systemConfig.getId());
                reloadOAuth2IfNeeded(systemConfig.getGroupCode(), systemConfig.getConfigCode());
        }

        /**
         * 更新系统配置
         */
        @Transactional
        public void updateSystemConfig(Long id, SystemConfigUpdateRequestDTO updateRequest) {
                SystemConfig systemConfig = systemConfigRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.SYSTEM_CONFIG_NOT_FOUND,
                                                "系统配置不存在: " + id));

                String valueType = updateRequest.getValueType() != null
                                ? updateRequest.getValueType()
                                : systemConfig.getValueType();
                validateConfigValue(valueType, updateRequest.getConfigValue());
                if (updateRequest.getConfigValue() == null) {
                        updateRequest.setConfigValue("");
                }

                // 更新系统配置信息
                systemConfigConverter.updateEntity(updateRequest, systemConfig);

                systemConfigRepository.update(systemConfig);
                log.info("System config updated: configCode={}, configId={}", systemConfig.getConfigCode(),
                                systemConfig.getId());
                reloadOAuth2IfNeeded(systemConfig.getGroupCode(), systemConfig.getConfigCode());
        }

        private void validateConfigValue(String valueType, String configValue) {
                if (SvgConfigValidator.isSvgType(valueType)) {
                        SvgConfigValidator.validateOrThrow(configValue);
                }
        }

        /**
         * 删除系统配置
         */
        @Transactional
        public void deleteSystemConfig(Long id) {
                SystemConfig systemConfig = systemConfigRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.SYSTEM_ERROR,
                                                "系统配置不存在: " + id));

                String groupCode = systemConfig.getGroupCode();
                String configCode = systemConfig.getConfigCode();
                systemConfigRepository.deleteById(id);
                log.info("System config deleted: configCode={}, configId={}", configCode, id);
                reloadOAuth2IfNeeded(groupCode, configCode);
        }

        private void reloadOAuth2IfNeeded(String groupCode, String configCode) {
                if (OAuth2SettingsService.isOAuth2Config(groupCode, configCode)) {
                        try {
                                dynamicClientRegistrationRepository.reload();
                        } catch (Exception ex) {
                                log.warn("Reload OAuth2 registrations after system-config change failed: {}",
                                                ex.getMessage());
                        }
                }
        }

        /**
         * 根据ID查询系统配置
         */
        public SystemConfigVO getSystemConfigById(Long id) {
                SystemConfig systemConfig = systemConfigRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.SYSTEM_CONFIG_NOT_FOUND,
                                                "系统配置不存在: " + id));

                return systemConfigConverter.toVO(systemConfig);
        }

        /**
         * 根据配置编码查询系统配置
         */
        public SystemConfigVO getSystemConfigByCode(String configCode) {
                SystemConfig systemConfig = systemConfigRepository.findByConfigCode(configCode)
                                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.SYSTEM_CONFIG_NOT_FOUND,
                                                "系统配置不存在: " + configCode));

                return systemConfigConverter.toVO(systemConfig);
        }

        /**
         * 查询系统配置列表（全量）
         */
        public List<SystemConfigVO> querySystemConfigList(SystemConfigQuery query) {
                List<SystemConfig> configs = systemConfigRepository.querySystemConfigList(query);
                return systemConfigConverter.toVOList(configs);
        }
}
