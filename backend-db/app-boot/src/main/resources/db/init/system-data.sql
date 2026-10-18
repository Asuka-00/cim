SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;
-- sys_tenant DML
INSERT IGNORE INTO `sys_tenant` (`id`,`tenant_id`,`tenant_name`,`enabled`,`create_by`,`create_at`,`update_by`,`update_at`) VALUES (316239484031489,'default','DEFAULT',1,NULL,'2026-06-11 09:57:55','1','2026-06-12 14:22:16');
-- sys_permission DML（系统管理采用三级菜单：组织与权限 / 数据字典 / 平台配置）
INSERT IGNORE INTO `sys_permission` (`id`,`tenant_id`,`permission_code`,`permission_name`,`permission_description`,`type`,`path`,`path_name`,`meta`,`component`,`permission_order`,`visible`,`parent_id`,`enabled`,`create_by`,`create_at`,`update_by`,`update_at`,`deleted_at`) VALUES
(316239484170757,'316239484031489','APP_SYSTEM_MANAGEMENT_PARENT','menus.pure_system_management','APP System Management Parent',1,'/app/system-management-parent','SystemManagementParent','{"icon": "material-symbols:admin-panel-settings-outline", "title": "menus.pure_system_management"}',NULL,6,1,NULL,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170933,'316239484031489','APP_ORG_ACCESS_PARENT','menus.pure_org_access','APP Org Access Parent',1,'org-access','OrgAccessParent','{"title": "menus.pure_org_access", "showLink": true}',NULL,1,1,316239484170757,1,'system','2026-07-20 00:00:00','system','2026-07-20 00:00:00',NULL),
(316239484170934,'316239484031489','APP_DICT_PARENT','menus.pure_dict_management','APP Dict Parent',1,'dict-management','DictManagementParent','{"title": "menus.pure_dict_management", "showLink": true}',NULL,2,1,316239484170757,1,'system','2026-07-20 00:00:00','system','2026-07-20 00:00:00',NULL),
(316239484170935,'316239484031489','APP_PLATFORM_CONFIG_PARENT','menus.pure_platform_config','APP Platform Config Parent',1,'platform-config','PlatformConfigParent','{"title": "menus.pure_platform_config", "showLink": true}',NULL,3,1,316239484170757,1,'system','2026-07-20 00:00:00','system','2026-07-20 00:00:00',NULL),
(316239484170762,'316239484031489','APP_USER_MANAGEMENT','menus.pure_user_management','APP User Management',1,'/app/system-management/user','UserManagement','{"title": "menus.pure_user_management", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/system-management/user/index.vue',1,1,316239484170933,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170763,'316239484031489','APP_ROLE_MANAGEMENT','menus.pure_role_management','APP Role Management',1,'/app/system-management/role','RoleManagement','{"title": "menus.pure_role_management", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/system-management/role/index.vue',2,1,316239484170933,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170764,'316239484031489','APP_PERMISSION_MANAGEMENT','menus.pure_permission_management','APP Permission Management',1,'/app/system-management/permission','PermissionManagement','{"title": "menus.pure_permission_management", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/system-management/permission/index.vue',3,1,316239484170933,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170779,'316239484031489','APP_TENANT_MANAGEMENT','menus.pure_tenant_management','APP Tenant Management',1,'/app/system-management/tenant','TenantManagement','{"title": "menus.pure_tenant_management", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/system-management/tenant/index.vue',4,1,316239484170933,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170765,'316239484031489','app:user:add','app.system_management.user.add','APP 用户管理添加按钮',2,NULL,NULL,NULL,NULL,1,1,316239484170762,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170766,'316239484031489','app:user:edit','app.system_management.user.edit','APP 用户管理编辑按钮',2,NULL,NULL,NULL,NULL,2,1,316239484170762,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170767,'316239484031489','app:user:delete','app.system_management.user.delete','APP 用户管理删除按钮',2,NULL,NULL,NULL,NULL,3,1,316239484170762,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170768,'316239484031489','app:user:reset-password','app.system_management.user.reset_password','APP 用户管理重置密码按钮',2,NULL,NULL,NULL,NULL,4,1,316239484170762,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170769,'316239484031489','app:role:add','app.system_management.role.add','APP 角色管理添加按钮',2,NULL,NULL,NULL,NULL,1,1,316239484170763,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170770,'316239484031489','app:role:edit','app.system_management.role.edit','APP 角色管理编辑按钮',2,NULL,NULL,NULL,NULL,2,1,316239484170763,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170771,'316239484031489','app:role:delete','app.system_management.role.delete','APP 角色管理删除按钮',2,NULL,NULL,NULL,NULL,3,1,316239484170763,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170772,'316239484031489','app:permission:add','app.system_management.permission.add','APP 权限管理添加按钮',2,NULL,NULL,NULL,NULL,1,1,316239484170764,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170773,'316239484031489','app:permission:edit','app.system_management.permission.edit','APP 权限管理编辑按钮',2,NULL,NULL,NULL,NULL,2,1,316239484170764,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170774,'316239484031489','app:permission:delete','app.system_management.permission.delete','APP 权限管理删除按钮',2,NULL,NULL,NULL,NULL,3,1,316239484170764,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170780,'316239484031489','app:tenant:add','app.system_management.tenant.add','APP 租户管理添加按钮',2,NULL,NULL,NULL,NULL,1,1,316239484170779,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170781,'316239484031489','app:tenant:edit','app.system_management.tenant.edit','APP 租户管理编辑按钮',2,NULL,NULL,NULL,NULL,2,1,316239484170779,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170782,'316239484031489','app:tenant:delete','app.system_management.tenant.delete','APP 租户管理删除按钮',2,NULL,NULL,NULL,NULL,3,1,316239484170779,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170890,'316239484031489','APP_DICT_TYPE_MANAGEMENT','menus.pure_dict_type_management','APP Dict Type Management',1,'/app/system-management/dict-type','DictTypeManagement','{"title": "menus.pure_dict_type_management", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/system-management/dict-type/index.vue',1,1,316239484170934,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170891,'316239484031489','app:dict-type:add','app.system_management.dict_type.add','APP 字典类型添加按钮',2,NULL,NULL,NULL,NULL,1,1,316239484170890,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170892,'316239484031489','app:dict-type:edit','app.system_management.dict_type.edit','APP 字典类型编辑按钮',2,NULL,NULL,NULL,NULL,2,1,316239484170890,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170893,'316239484031489','app:dict-type:delete','app.system_management.dict_type.delete','APP 字典类型删除按钮',2,NULL,NULL,NULL,NULL,3,1,316239484170890,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170894,'316239484031489','APP_DICT_ITEM_MANAGEMENT','menus.pure_dict_item_management','APP Dict Item Management',1,'/app/system-management/dict-item','DictItemManagement','{"title": "menus.pure_dict_item_management", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/system-management/dict-item/index.vue',2,1,316239484170934,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170895,'316239484031489','app:dict-item:add','app.system_management.dict_item.add','APP 字典项添加按钮',2,NULL,NULL,NULL,NULL,1,1,316239484170894,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170896,'316239484031489','app:dict-item:edit','app.system_management.dict_item.edit','APP 字典项编辑按钮',2,NULL,NULL,NULL,NULL,2,1,316239484170894,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170897,'316239484031489','app:dict-item:delete','app.system_management.dict_item.delete','APP 字典项删除按钮',2,NULL,NULL,NULL,NULL,3,1,316239484170894,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170775,'316239484031489','APP_SYSTEM_CONFIG_MANAGEMENT','menus.pure_system_config_management','APP System Config Management',1,'/app/system-management/system-config','SystemConfigManagement','{"title": "menus.pure_system_config_management", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/system-management/system-config/index.vue',1,1,316239484170935,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170776,'316239484031489','app:system-config:add','app.system_management.system_config.add','APP 系统配置添加按钮',2,NULL,NULL,NULL,NULL,1,1,316239484170775,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170777,'316239484031489','app:system-config:edit','app.system_management.system_config.edit','APP 系统配置编辑按钮',2,NULL,NULL,NULL,NULL,2,1,316239484170775,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170778,'316239484031489','app:system-config:delete','app.system_management.system_config.delete','APP 系统配置删除按钮',2,NULL,NULL,NULL,NULL,3,1,316239484170775,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170940,'316239484031489','APP_RELEASE_MANAGEMENT','menus.pure_release_management','APP Release Management',1,'/app/system-management/release','ReleaseManagement','{"title": "menus.pure_release_management", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/system-management/release/index.vue',2,1,316239484170935,1,'system','2026-09-10 00:00:00','system','2026-09-10 00:00:00',NULL),
(316239484170941,'316239484031489','app:release:upload','app.system_management.release.upload','APP 版本升级上传按钮',2,NULL,NULL,NULL,NULL,1,1,316239484170940,1,'system','2026-09-10 00:00:00','system','2026-09-10 00:00:00',NULL),
(316239484170942,'316239484031489','app:release:activate','app.system_management.release.activate','APP 版本升级激活按钮',2,NULL,NULL,NULL,NULL,2,1,316239484170940,1,'system','2026-09-10 00:00:00','system','2026-09-10 00:00:00',NULL),
(316239484170943,'316239484031489','app:release:rollback','app.system_management.release.rollback','APP 版本升级回滚按钮',2,NULL,NULL,NULL,NULL,3,1,316239484170940,1,'system','2026-09-10 00:00:00','system','2026-09-10 00:00:00',NULL),
(316239484170849,'316239484031489','APP_LOG_MANAGEMENT_PARENT','menus.pure_log_management','APP Log Management Parent',1,'/app/log-management-parent','LogManagementParent','{"icon": "ri:file-list-3-line", "title": "menus.pure_log_management"}',NULL,7,1,NULL,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170850,'316239484031489','APP_OPERATION_LOG','menus.pure_operation_log','APP Operation Log',1,'/app/log-management/operation-log','OperationLog','{"title": "menus.pure_operation_log", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/log-management/operation-log/index.vue',1,1,316239484170849,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170851,'316239484031489','APP_LOG_FILE','menus.pure_log_file','APP Log File',1,'/app/log-management/log-file','LogFile','{"title": "menus.pure_log_file", "showLink": true, "keepAlive": true, "showParent": true}','/src/views/modules/log-management/log-file/index.vue',2,1,316239484170849,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170852,'316239484031489','app:log-file:preview','app.log_management.log_file.preview','APP 日志文件预览按钮',2,NULL,NULL,NULL,NULL,1,1,316239484170851,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL),
(316239484170853,'316239484031489','app:log-file:download','app.log_management.log_file.download','APP 日志文件下载按钮',2,NULL,NULL,NULL,NULL,2,1,316239484170851,1,'system','2026-06-11 00:00:00','system','2026-06-12 14:25:08',NULL);

-- sys_role DML
INSERT IGNORE INTO `sys_role` (`id`,`tenant_id`,`role_code`,`role_name`,`enabled`,`create_by`,`create_at`,`update_by`,`update_at`,`deleted_at`) VALUES (316239484080641,'316239484031489','default','default',1,'test-user','2026-04-07 09:41:36','admin','2026-06-18 13:58:37',NULL);
-- sys_role_permission DML
INSERT IGNORE INTO `sys_role_permission` (`id`,`tenant_id`,`role_id`,`permission_id`,`create_by`,`create_at`,`update_by`,`update_at`) VALUES
(90000000000001,'316239484031489',316239484080641,316239484170757,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000036,'316239484031489',316239484080641,316239484170933,'system','2026-07-20 00:00:00','system','2026-07-20 00:00:00'),
(90000000000037,'316239484031489',316239484080641,316239484170934,'system','2026-07-20 00:00:00','system','2026-07-20 00:00:00'),
(90000000000038,'316239484031489',316239484080641,316239484170935,'system','2026-07-20 00:00:00','system','2026-07-20 00:00:00'),
(90000000000039,'316239484031489',316239484080641,316239484170940,'system','2026-09-10 00:00:00','system','2026-09-10 00:00:00'),
(90000000000040,'316239484031489',316239484080641,316239484170941,'system','2026-09-10 00:00:00','system','2026-09-10 00:00:00'),
(90000000000041,'316239484031489',316239484080641,316239484170942,'system','2026-09-10 00:00:00','system','2026-09-10 00:00:00'),
(90000000000042,'316239484031489',316239484080641,316239484170943,'system','2026-09-10 00:00:00','system','2026-09-10 00:00:00'),
(90000000000002,'316239484031489',316239484080641,316239484170762,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000003,'316239484031489',316239484080641,316239484170763,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000004,'316239484031489',316239484080641,316239484170764,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000005,'316239484031489',316239484080641,316239484170765,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000006,'316239484031489',316239484080641,316239484170766,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000007,'316239484031489',316239484080641,316239484170767,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000008,'316239484031489',316239484080641,316239484170768,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000009,'316239484031489',316239484080641,316239484170769,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000010,'316239484031489',316239484080641,316239484170770,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000011,'316239484031489',316239484080641,316239484170771,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000012,'316239484031489',316239484080641,316239484170772,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000013,'316239484031489',316239484080641,316239484170773,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000014,'316239484031489',316239484080641,316239484170774,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000015,'316239484031489',316239484080641,316239484170775,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000016,'316239484031489',316239484080641,316239484170776,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000017,'316239484031489',316239484080641,316239484170777,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000018,'316239484031489',316239484080641,316239484170778,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000019,'316239484031489',316239484080641,316239484170779,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000020,'316239484031489',316239484080641,316239484170780,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000021,'316239484031489',316239484080641,316239484170781,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000022,'316239484031489',316239484080641,316239484170782,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000023,'316239484031489',316239484080641,316239484170849,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000024,'316239484031489',316239484080641,316239484170850,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000025,'316239484031489',316239484080641,316239484170851,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000026,'316239484031489',316239484080641,316239484170852,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000027,'316239484031489',316239484080641,316239484170853,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000028,'316239484031489',316239484080641,316239484170890,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000029,'316239484031489',316239484080641,316239484170891,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000030,'316239484031489',316239484080641,316239484170892,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000031,'316239484031489',316239484080641,316239484170893,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000032,'316239484031489',316239484080641,316239484170894,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000033,'316239484031489',316239484080641,316239484170895,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000034,'316239484031489',316239484080641,316239484170896,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08'),
(90000000000035,'316239484031489',316239484080641,316239484170897,'system','2026-06-11 11:36:45','system','2026-06-12 14:25:08');

-- sys_user DML
INSERT IGNORE INTO `sys_user` (`id`,`tenant_id`,`user_name`,`display_name`,`password_hash`,`salt`,`enabled`,`last_login_at`,`create_by`,`create_at`,`update_by`,`update_at`,`deleted_at`) VALUES (316239484125698,'316239484031489','admin','admin','$2a$10$ryCHwo.Yz59yApHs7JolIeyZOtWNAdmGQlb0X0ou.KfuvI4sH2H0m',NULL,1,NULL,'system','2026-04-09 10:03:22','admin','2026-06-12 14:25:08',NULL);
-- sys_user_role DML
INSERT IGNORE INTO `sys_user_role` (`id`,`tenant_id`,`user_id`,`role_id`,`create_by`,`create_at`,`update_by`,`update_at`) VALUES (316239484277249,'316239484031489',316239484125698,316239484080641,NULL,'2026-06-09 17:30:02',NULL,'2026-06-12 14:25:08');

-- OAuth2 系统配置（IdP 身份认证 + 本地签发 Token）
INSERT IGNORE INTO `sys_system_config` (`id`,`config_code`,`config_value`,`value_type`,`group_code`,`remark`,`state`,`create_by`,`create_at`,`update_by`,`update_at`,`tenant_id`,`deleted_at`) VALUES
(316239485000001,'oauth2.enabled','false','boolean','OAUTH2','是否启用 OAuth2 登录（IdP 仅身份认证，业务 Token 由本系统签发）',1,'system','2026-07-17 00:00:00','system','2026-07-17 00:00:00','316239484031489',NULL),
(316239485000002,'oauth2.user-name-claim','preferred_username','string','OAUTH2','全局默认从 IdP 用户信息解析本地 userName 的 claim',1,'system','2026-07-17 00:00:00','system','2026-07-17 00:00:00','316239484031489',NULL),
(316239485000003,'oauth2.frontend-success-url','http://localhost:2900/#/login/oauth2/callback','string','OAUTH2','OAuth2 登录成功回跳前端地址（后端追加 ticket）',1,'system','2026-07-17 00:00:00','system','2026-07-17 00:00:00','316239484031489',NULL),
(316239485000004,'oauth2.frontend-failure-url','http://localhost:2900/#/login','string','OAUTH2','OAuth2 登录失败回跳前端地址（后端追加 error）',1,'system','2026-07-17 00:00:00','system','2026-07-17 00:00:00','316239484031489',NULL),
(316239485000005,'oauth2.ticket-ttl-seconds','60','int','OAUTH2','OAuth2 一次性 ticket 有效期（秒）',1,'system','2026-07-17 00:00:00','system','2026-07-17 00:00:00','316239484031489',NULL),
(316239485000010,'oauth2.provider.oidc','{"registrationId":"oidc","clientName":"OIDC","enabled":false,"clientId":"app","clientSecret":"change-me","scope":"openid,profile","clientAuthenticationMethod":"client_secret_basic","authorizationGrantType":"authorization_code","redirectUri":"{baseUrl}/login/oauth2/code/{registrationId}","issuerUri":"https://idp.example.com/realms/app","userNameAttribute":"preferred_username"}','json','OAUTH2','示例 OIDC 提供方（enabled=false；配置真实 IdP 后改为 true）',1,'system','2026-07-17 00:00:00','system','2026-07-17 00:00:00','316239484031489',NULL);

-- 品牌配置（展示层；SVG 文本存 config_value，空则前端回退 platform-config / public/brand）
INSERT IGNORE INTO `sys_system_config` (`id`,`config_code`,`config_value`,`value_type`,`group_code`,`remark`,`state`,`create_by`,`create_at`,`update_by`,`update_at`,`tenant_id`,`deleted_at`) VALUES
(316239485100001,'brand.title','Application','string','BRAND','前端展示项目名称',1,'system','2026-08-05 00:00:00','system','2026-08-05 00:00:00','316239484031489',NULL),
(316239485100002,'brand.powered-by','Your Company','string','BRAND','页脚版权主体',1,'system','2026-08-05 00:00:00','system','2026-08-05 00:00:00','316239484031489',NULL),
(316239485100003,'brand.logo-svg','','svg','BRAND','侧栏/顶栏 Logo（SVG 源码；空则用静态默认）',1,'system','2026-08-05 00:00:00','system','2026-08-05 00:00:00','316239484031489',NULL),
(316239485100004,'brand.favicon-svg','','svg','BRAND','浏览器 Favicon（SVG 源码；空则用静态默认）',1,'system','2026-08-05 00:00:00','system','2026-08-05 00:00:00','316239484031489',NULL),
(316239485100005,'brand.login-title-svg','','svg','BRAND','登录页标题图 SVG（配合 show-login-title-image）',1,'system','2026-08-05 00:00:00','system','2026-08-05 00:00:00','316239484031489',NULL),
(316239485100006,'brand.login-bg-svg','','svg','BRAND','登录页背景 SVG（矢量图案；照片请改静态资源）',1,'system','2026-08-05 00:00:00','system','2026-08-05 00:00:00','316239484031489',NULL),
(316239485100007,'brand.show-login-title-image','false','boolean','BRAND','登录页是否优先使用标题 SVG',1,'system','2026-08-05 00:00:00','system','2026-08-05 00:00:00','316239484031489',NULL);

SET FOREIGN_KEY_CHECKS = 1;
