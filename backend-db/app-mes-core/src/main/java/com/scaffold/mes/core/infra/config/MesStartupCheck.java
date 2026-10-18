package com.scaffold.mes.core.infra.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import com.scaffold.mes.core.config.MesProperties;
import com.scaffold.share.db.DatabaseKind;

/**
 * MesStartupCheck MES 启动检查
 * <p>
 * 启用 MES 时拒绝非 MySQL,避免缺表后才失败。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public class MesStartupCheck implements ApplicationRunner {

    private final MesProperties properties;
    private final DatabaseKind databaseKind;

    public MesStartupCheck(MesProperties properties, DatabaseKind databaseKind) {
        this.properties = properties;
        this.databaseKind = databaseKind;
    }

    /**
     * 启用 MES 且当前库不是 MySQL 时中止启动。
     *
     * @param args 启动参数,未使用
     */
    @Override
    public void run(ApplicationArguments args) {
        if (properties.isEnabled() && databaseKind != DatabaseKind.MYSQL) {
            throw new IllegalStateException("MES 仅支持 MySQL 8,当前数据库是 " + databaseKind);
        }
    }
}
