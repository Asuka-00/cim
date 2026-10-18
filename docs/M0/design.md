# M0 地基与骨架

> 状态:**设计草案,待核对**(未实现)· 依赖:无(依赖现有 app-common / app-system) · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_0__mes_m0.sql`)

## 1. 目标

MES 各模块能启动并被 app-boot 聚合;编号规则、并发控制、统一事务层、内部事件、站内预警、调度底座等公共能力就位。后续所有里程碑的写操作都经由本里程碑的事务层。

## 2. 范围

- 建 app-mes-core / app-mes-base / app-mes-warehouse / app-mes-exec,接入父 POM 与 app-boot
- 架构边界测试:domain 不依赖 api/infra;模块间只经 share.integration.mes.*
- app-common 通用能力补齐:乐观锁拦截器、AppendOnly 实体基类与仓储支撑
- 调度底座:ShedLock + 按租户遍历的任务基类
- MES 业务错误码段与统一异常
- 菜单/权限/字典/参数种子,以及新租户的 MES 初始化服务
- 统一事务层(幂等、加锁、历史、领域事件与按订阅者投递)
- 编号规则、MES 运行参数、MES 字典、站内预警 mes_alert(自 M9 前移)
- 事务撤销登记 hist_mes_txn_reversal;租户创建钩子 `TenantCreatedEvent`;修正会话租户优先级使 `TenantContextHolder.runAs` 在请求内生效

## 3. 设计规则

- **包结构**:`com.scaffold.mes.{core|base|warehouse|exec}`,内部沿用 `api.v1.<feature>` / `domain.{entity,dto,vo,query,converter,repository,service}.<feature>` / `infra.{mapper.db,repository.db}.<feature>`。框架 `@MapperScan("com.scaffold.**.mapper", annotationClass = Mapper.class)` 要求:Mapper 包名含 `.mapper` **且接口必须标注 `@Mapper`**。
- **模块依赖**:`app-mes-core` 依赖 app-common;`base/warehouse/exec` 都依赖 `app-mes-core`,彼此**不直接依赖**。跨模块访问只经 `com.scaffold.share.integration.mes.{base|warehouse|exec|quality|maint}.{provider|response|command|event}`:契约放 app-common,实现放提供方模块。首批契约:`base.EquipmentStatusProvider`(设备状态变更,M1)、`exec.WorkOrderProvider`(工单查询,M3)、`exec.LotCommandProvider`(Hold/Release/Scrap,M4 追加 Rework/Split/Terminate,M3/M4)、`warehouse.InventoryProvider`(扣减/回补/物料批状态重算,M7);其余契约在提供方里程碑定义。**契约包只放接口、请求/响应 DTO、事件载荷与枚举**(与框架 `share.integration.system` 同口径),不放实体、仓储、Service 实现与业务规则,架构测试校验。通用的导入框架、UDA、班次日历、打印模板、通知规则、预警都在 core(M12/M13 里程碑逐步补入),避免 base 反向依赖 exec。
- **错误码段**:通用 400000–409999;base 410000–419999;warehouse 420000–429999;exec 430000–439999;quality 440000–449999;maint 450000–459999。写入各模块自己的常量类,不污染 `ErrorCodeConstants`。
- **数据库范围(D20)**:MES 首期**只支持 MySQL 8**。迁移只写 `db/migration/mysql`;其他数据库目录不出 MES 脚本。为避免在非 MySQL 库上启动后才报"表不存在",MES 模块以 `app.mes.enabled` 开关 + 启动检查(`DatabaseKind != MYSQL` 时拒绝启用)。项目中并没有现成的"多库转换脚本",如日后要支持其他库需单独立项(涉及 JSON 列、过滤唯一索引、NOWAIT 方言与错误码)。
- **排序规则**:现有基线中 `sys_dict_*` 等表是 `utf8mb4_0900_ai_ci`,MES 表按现有多数用 `utf8mb4_unicode_ci`。**MES 表不与 `sys_dict_*` 做 SQL 关联**(字典值在应用层取),避免 `Illegal mix of collations`(1267)。
- **乐观锁(修正现状)**:现有 `MybatisPlusConfig` 没有注册 `OptimisticLockerInnerInterceptor`,代码中也无 `@Version`,`version` 列此前不生效;且 `DbRepositorySupport.update()` 返回 `void`,不暴露影响行数。M0 在 app-common 补注册该拦截器,并新增 `updateWithVersion()`(返回影响行数,为 0 抛 `MesConcurrentModificationException`,错误码 400101);MES 实体的 `version` 字段标 `@Version`。该拦截器只对带 `@Version` 的实体生效,不影响现有 `sys_*`。
- **并发控制(事务内行锁为主)**:写事务对涉及的聚合根按固定顺序加行锁:**工单(仅开批/Bonus/工单拆分合并时)→ 设备(含 Port)→ Lot(按 id 升序)→ Wafer → 其他**。"其他"内部顺序固定为:**抽检计数器 → 工具行(按 id)→ 库存与绑定行(按 id)**,避免出站事务互相死锁。"用户直接争用"的聚合根(设备、Lot、Wafer、Batch、Carrier)使用 `SELECT … FOR UPDATE NOWAIT`,冲突立即转成"对象被占用"(错误码 400102);**共享热点行**(抽检计数器)用原子 `UPDATE … SET c = c + 1`,尽量靠后执行(原子 UPDATE 仍持有行锁至提交,越晚执行持锁越短;抽检计数需在下一步解析时读回新值,见 M10);PM 计数不在业务事务内,由异步订阅者原子更新(M13);编号计数在事务外取号(见下);库存行/工具行按 id 升序加锁并允许短等待(会话 `innodb_lock_wait_timeout=3`,超时转 400103),避免不同 Lot 并发出站时被误报占用。关键校验读必须在持有行锁后读取。MySQL 的 NOWAIT 冲突错误码 3572、等待超时 1205 在事务层统一翻译。
- **逻辑锁 `mes_object_lock`**:仅用于跨请求的长占用(如 Batch 组批编辑、盘点期间锁库位),由独立请求以自身的短事务获取与释放(不嵌套在业务事务内),`expire_at` 到期自动失效。不用于单次请求内的互斥。
- **统一事务层**:所有 Lot/Wafer/物料变更走 `MesTransactionService.execute(cmd)`:① 取号等需要独立事务的动作**在业务事务开始之前完成** → ② 幂等检查(仅命中 SUCCESS,命中时返回原事务的响应摘要) → ③ 行锁 → ④ 业务校验 → ⑤ 变更主表 → ⑥ 写 `hist_mes_*` 与 `hist_mes_genealogy_link` → ⑦ 写 `hist_mes_transaction`(SUCCESS) → ⑧ 写 `mes_domain_event`,同一数据库事务。**失败路径**:业务事务回滚**之后**,再用新事务写一条 `result=FAILED` 的 `hist_mes_transaction` 与 Interlock BLOCK 日志(此时外层行锁已释放,不会自阻塞)。**独立事务规则(唯一口径)**:业务事务**内部禁止**开 `REQUIRES_NEW`;需要独立提交的动作只有三类,且都在业务事务**之外**串行执行:① 事务前:取号(`mes_sequence_counter`);② 事务后:失败流水与 Interlock BLOCK 日志;③ 跨请求占用:`mes_object_lock` 的加锁/解锁(本身就是独立请求)。"失败不阻塞主事务"的需求一律用 `NESTED`(savepoint)或预校验实现。
- **连接池约束**:由上条规则保证一次写请求最多同时持有 1 个数据库连接;连接池大小按"并发写请求数 + 后台任务数 + 余量"配置(默认不小于 30)。
- **幂等**:`hist_mes_transaction.idem_key` = `txn_type:request_id`(最长 32+1+64=97,列宽 `VARCHAR(100)`),仅在 SUCCESS 时写入并有唯一键;FAILED 记录该列为 NULL,同一 `request_id` 可重试。命中已成功的幂等键时,**返回原事务的响应摘要**(`payload_json.response`),不重复执行。Batch 展开的子事务通过 `parent_txn_id` 挂在父事务下,只有父事务带 `request_id/idem_key`。
- **撤销关系不回写原行**:纠错事务写 `ref_txn_id` 指向原事务,并在只增表 `hist_mes_txn_reversal` 登记(`orig_txn_id` 唯一,数据库层保证一笔事务只能被撤销一次)。统计口径(返工次数、报表、良率、追溯)统一通过该表排除已撤销事务,不对 `hist_mes_transaction` 做反连接。登记撤销的同一事务内发布内部事件 `TXN_REVERSED`(载荷含 `orig_txn_id/reversal_txn_id/txn_type/object`),供 PM 计数回退。工单进度与物料需求不订阅它:撤销事务内改完状态后重发 `WORK_ORDER_PROGRESS`(有工单时)和 `MATERIAL_CONSUMED`(本事务写了消耗行时),由这两个事件的订阅者重算,避免和 `TXN_REVERSED` 并发写同一行。消耗账已用负数行表达冲销,按 `SUM` 计算,不再按撤销登记排除。
- **编号**:`txn_no`、`event_no` 直接使用雪花 ID 字符串,不占用编号规则;业务单号(工单/Lot/Batch/物料批…)通过 `mes_sequence_rule` 取号,取号在**业务事务开始之前**以独立短事务完成,接受回滚造成的跳号,避免长事务持有计数行锁形成全厂热点。**事务内才产生的子对象不取号**:Split(含 Future Action SPLIT、Wafer 级 Track 隐式 Split、按 Bin 拆批)的子 Lot 号固定为 `父 Lot 号 + "." + split_seq(两位)`,`split_seq` 在持有父 Lot 行锁后取 `MAX+1`,无需在事务前预取。每级拆批增加 3 个字符,LOT 号 ≤ 48 时最多 5 级;超过 64 时拒绝 Split。
- **编号规则**:`前缀 + 日期模板 + 流水号 + 后缀`,流水按周期(NEVER/DAY/MONTH/YEAR)重置;LOT 规则生成的号码总长不超过 48(`lot_no` 为 VARCHAR(64),为多级拆批后缀 `.NN` 预留);取号对 `mes_sequence_counter` 行 `FOR UPDATE` 后步进。示例:工单 `WO{yyyyMMdd}{0000}`、Lot `L{yyMM}{00000}`。
- **领域事件与投递**:仅内部使用(站内预警、统计、工单进度、物料需求、PM 计数),目录见 3.1。业务事务内写 `mes_domain_event`;调度任务轮询 `NEW` 事件,按订阅者注册表为每个订阅者生成一条 `mes_domain_event_delivery`(唯一键 `(event_id, subscriber)`),再把事件置 `DISPATCHED`。**每个订阅者独立处理与重试**:订阅者的业务写入与投递行置 `DONE` 同事务提交,一个订阅者失败不会让其他订阅者重做;同一聚合按 `id` 顺序投递。聚合键 `aggregate_type/aggregate_id` 取主对象:Lot 事件(含 `LOT_TRACK_OUT` 与该 Lot 的 `TXN_REVERSED`)为 `LOT/lot_id`,工单进度为 `WORK_ORDER/wo_id`,物料需求为 `WORK_ORDER/wo_id`。订阅者编码:`WO_PROGRESS`、`MATERIAL_REQ`(同时处理消耗与领退料)、`ALERT_RULE`、`PM_COUNTER`。可选模块(quality/maint)未启用时其订阅者不注册、不生成投递行。无外部推送(D12)。
- **调度底座(修正现状)**:`share.scheduling` 包目前为空、ShedLock 仅在父 POM 声明了 `shedlock-spring`。M0 补引入 `shedlock-provider-jdbc-template`(`usingDbTime()`)与 `shedlock` 表,并提供 `TenantAwareScheduledTask` 基类:任务触发时通过现有 `TenantProvider.findAllTenants()` 遍历租户并设置租户上下文,再执行子任务。否则无会话的后台任务会按 `DEFAULT_TENANT` 查询,对真实租户全部失效。
- **缓存**:`app.cache.exclude-tables` 追加 `mes_*`(全部 MES 业务表不走透明查询缓存;`hist_*` 已默认排除)。主数据量小、命中收益有限,且 `FOR UPDATE` 读本来就绕过缓存;后续如需放开某些主数据表再单独评估。
- **MES 运行参数**(复用 `sys_system_config`,键名前缀 `mes.`,`config_code` ≤ 60):`mes.scan.required`、`mes.step.skip.allowed`、`mes.wafer.track.enabled`、`mes.dummy.max.reuse`、`mes.eqp.state.auto`、`mes.batch.count.as.one`、`mes.backflush.shortage`、`mes.convert.deviation.block` 等,默认值见各里程碑。
- **MES 字典类型**(复用 `sys_dict_type/sys_dict_item`):`MES_SCRAP_REASON`、`MES_REWORK_REASON`、`MES_DEFECT_CODE`、`MES_EQP_STOP_REASON`、`MES_EQP_STATUS_CODE`、`MES_MATERIAL_STATUS`。
- **租户种子(修正现状)**:`sys_permission` 是**全局表**(租户拦截器不过滤),菜单与权限**只写一次**,放在 `R__system_data_mes.sql`(名称需排在 `R__system_data` 之后,Flyway 可重复迁移按描述字母序执行)。按租户初始化的只有字典、参数、角色授权、MES 内置数据(编号规则、内置 Interlock 规则、默认派工规则):由 `MesTenantInitializer` 在**新建租户时**调用(需在 `share.integration.system` 新增 `TenantCreatedEvent` 契约,由 app-system 发布)并可对已有租户补跑(幂等)。跨租户执行沿用现有 `TenantContextHolder.runAs(tenantId, …)`,但需**修正 `SecurityCurrentSessionProvider.getCurrentTenantId()` 的优先级**:`runAs` 设置的显式租户优先于登录主体租户(现状相反,请求内的 `runAs` 不生效,现有 `UserApplicationService.deleteUserInfo` 跨租户删角色也受此影响)。为避免普通请求被误改租户,显式租户只由 `runAs` 设置并在 finally 恢复,`TraceIdInterceptor` 等不得写入。不新增 `TenantScope`。
- **只增表约定**:`BaseEntity` 接口强制要求 `update*` 访问器,`DbRepositorySupport<T extends BaseEntity>` 与批量填充都绑定在它上面,因此不含 `update_*` 的只增表需要新增 `AppendOnlyEntity` 与独立的 `AppendOnlyRepositorySupport`(仅 insert/查询,含批量填充);少数"时间段表"(设备状态段、载具绑定)保留 `update_*`,且只允许回填结束字段。

### 3.1 内部事件目录

载荷为 JSON,载荷类放 `share.integration.mes.<模块>.event`。新增事件在发布方里程碑登记到本表。

| 事件 | 发布方与时机 | 关键载荷 | 订阅者 |
|---|---|---|---|
| `LOT_TRACK_OUT` | exec,每个 Lot 的 Track-Out 事务内(Batch 内每个 Lot 一条);聚合 `LOT/lot_id` | `lot_id, txn_id, parent_txn_id, batch_id, equipment_id, port_id, step_id, route_step_id(返工为 0), wafer_count, qty, unit_id, track_in_at, track_out_at` | `ALERT_RULE`、`PM_COUNTER` |
| `TXN_REVERSED` | core,登记 `hist_mes_txn_reversal` 的同一事务;聚合 `LOT/lot_id`(对象是 Lot 时) | `orig_txn_id, reversal_txn_id, txn_type, object_type, object_id` | `PM_COUNTER` |
| `WORK_ORDER_PROGRESS` | exec,开批/Bonus/出站/报废/拆合批/终止/出货/投入封装,以及这些事务的撤销 | `wo_id`;聚合 `WORK_ORDER/wo_id` | `WO_PROGRESS`:按 Lot 重算,天然幂等 |
| `MATERIAL_CONSUMED` | exec,每次写消耗行(含负数行、欠账清偿行、撤销冲销行) | `wo_id, bom_item_id, txn_id`;聚合 `WORK_ORDER/wo_id` | `MATERIAL_REQ`:`consumed_qty = SUM(qty)`,不排除撤销 |
| `MATERIAL_ISSUED` | warehouse,领/退料单确认事务内(退料为负) | `wo_id, material_id, qty, issue_order_id`;聚合 `WORK_ORDER/wo_id` | `MATERIAL_REQ`:`issued_qty` 累加,投递表保证一次 |
| `LOT_HOLD`、`MATERIAL_LOW`、`MATERIAL_EXPIRED`、`BINDING_DEPLETED`、`TOOL_LIFE`、`QUEUE_TIME` 等 | 各发布点 | 对象与原因 | `ALERT_RULE` |

## 4. 表设计

### 4.1 新增表(9 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_sequence_rule` | 编号规则 | `app-mes-core` |
| `mes_sequence_counter` | 编号流水计数 | `app-mes-core` |
| `mes_object_lock` | 对象逻辑锁(跨请求长占用) | `app-mes-core` |
| `hist_mes_transaction` | 事务流水 | `app-mes-core` |
| `mes_domain_event` | 内部领域事件 | `app-mes-core` |
| `mes_domain_event_delivery` | 领域事件投递 | `app-mes-core` |
| `hist_mes_txn_reversal` | 事务撤销登记 | `app-mes-core` |
| `mes_alert` | 站内预警 | `app-mes-core` |
| `shedlock` | ShedLock 分布式任务锁(ShedLock JDBC 标准结构) | `app-mes-core` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_sequence_rule` 编号规则
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `rule_code` | VARCHAR(64) | 是 |  | 规则编码,如 WORK_ORDER/LOT/WAFER/BATCH/MATERIAL_LOT/STRIP/REEL |
| `rule_name` | VARCHAR(128) | 是 |  | 规则名称 |
| `biz_type` | VARCHAR(32) | 是 |  | 业务对象类型 |
| `prefix` | VARCHAR(32) |  |  | 前缀 |
| `date_pattern` | VARCHAR(16) |  |  | 日期模板:yyyyMMdd/yyMM/yyMMdd/yyyy,空表示无 |
| `seq_length` | TINYINT | 是 | 4 | 流水号长度,不足左补 0 |
| `seq_start` | BIGINT | 是 | 1 | 流水起始值 |
| `seq_step` | INT | 是 | 1 | 流水步长 |
| `reset_cycle` | VARCHAR(16) | 是 | NEVER | 重置周期 NEVER/DAY/MONTH/YEAR |
| `suffix` | VARCHAR(32) |  |  | 后缀 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_rule_code`(tenant_id, rule_code)

#### `mes_sequence_counter` 编号流水计数
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `rule_id` | BIGINT | 是 |  | 规则ID |
| `period_key` | VARCHAR(16) | 是 |  | 周期键:NEVER 时为 -,DAY 时为 yyyyMMdd,MONTH 为 yyyyMM,YEAR 为 yyyy |
| `current_value` | BIGINT | 是 | 0 | 当前已发放的最大流水值 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_rule_period`(tenant_id, rule_id, period_key)

说明:取号在独立短事务内对该行 `FOR UPDATE`,不存在则插入;不含乐观锁。

#### `mes_object_lock` 对象逻辑锁(跨请求长占用)
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `object_type` | VARCHAR(32) | 是 |  | 对象类型 BATCH/LOCATION/WAREHOUSE/STOCKTAKE/ROUTE_VERSION 等 |
| `object_id` | BIGINT | 是 |  | 对象ID |
| `lock_owner` | VARCHAR(64) | 是 |  | 持锁人/会话标识 |
| `purpose` | VARCHAR(64) |  |  | 用途说明 |
| `locked_at` | DATETIME | 是 |  | 加锁时间 |
| `expire_at` | DATETIME | 是 |  | 过期时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_object`(tenant_id, object_type, object_id);索引 `idx_expire`(tenant_id, expire_at)

说明:由独立请求的短事务加锁/解锁(不嵌套在业务事务内),唯一键冲突即视为被占用;加锁前先清理已过期记录。**不用于单次请求内的互斥**(单次请求用事务内 FOR UPDATE NOWAIT)。

#### `hist_mes_transaction` 事务流水
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `txn_no` | VARCHAR(64) | 是 |  | 事务号(雪花ID字符串) |
| `txn_type` | VARCHAR(32) | 是 |  | 事务类型 CREATE_LOT/TRACK_IN/TRACK_OUT/HOLD/RELEASE/SCRAP/… |
| `object_type` | VARCHAR(32) | 是 |  | 主对象类型 |
| `object_id` | BIGINT |  |  | 主对象ID |
| `object_code` | VARCHAR(64) |  |  | 主对象编码(冗余,便于检索) |
| `parent_txn_id` | BIGINT |  |  | 父事务ID,Batch 等展开的子事务指向父事务 |
| `request_id` | VARCHAR(64) |  |  | 客户端请求ID(诊断用,不要求唯一) |
| `idem_key` | VARCHAR(100) |  |  | 幂等键 = txn_type:request_id,仅 result=SUCCESS 时有值,否则为 NULL |
| `ref_txn_id` | BIGINT |  |  | 关联原事务ID,纠错/撤销事务指向被纠正的事务 |
| `source` | VARCHAR(16) | 是 | ADMIN | 来源 TERMINAL/ADMIN/IMPORT/SYSTEM |
| `reason_code` | VARCHAR(64) |  |  | 原因码 |
| `operator` | VARCHAR(64) | 是 |  | 操作人(用户名) |
| `result` | VARCHAR(16) | 是 | SUCCESS | 结果 SUCCESS/FAILED |
| `error_message` | VARCHAR(512) |  |  | 失败原因 |
| `payload_json` | JSON |  |  | 请求与变更摘要 |
| `remark` | VARCHAR(512) |  |  | 备注 |
| `txn_time` | DATETIME | 是 |  | 事务发生时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:唯一 `uk_txn_no`(tenant_id, txn_no);唯一 `uk_idem_key`(tenant_id, idem_key);索引 `idx_object`(tenant_id, object_type, object_id, txn_time);索引 `idx_type_time`(tenant_id, txn_type, txn_time);索引 `idx_operator_time`(tenant_id, operator, txn_time);索引 `idx_parent`(tenant_id, parent_txn_id);索引 `idx_ref`(tenant_id, ref_txn_id)

说明:**只增不改**:撤销关系靠 `ref_txn_id` 反查,不回写原行。失败事务(`result=FAILED`)在独立事务中写入,`idem_key` 为 NULL 因而不占用幂等键;MySQL 唯一键允许多个 NULL。

#### `mes_domain_event` 内部领域事件
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `event_no` | VARCHAR(64) | 是 |  | 事件号(雪花ID字符串) |
| `event_type` | VARCHAR(64) | 是 |  | 事件类型,如 LOT_HOLD/LOT_TRACK_OUT/MATERIAL_LOW |
| `aggregate_type` | VARCHAR(32) | 是 |  | 聚合类型 |
| `aggregate_id` | BIGINT |  |  | 聚合ID |
| `txn_id` | BIGINT |  |  | 来源事务ID |
| `payload_json` | JSON |  |  | 事件内容 |
| `status` | VARCHAR(16) | 是 | NEW | 分发状态 NEW/DISPATCHED/FAILED(订阅者处理状态见投递表) |
| `retry_count` | INT | 是 | 0 | 已重试次数 |
| `next_retry_at` | DATETIME |  |  | 下次重试时间 |
| `processed_at` | DATETIME |  |  | 处理完成时间 |
| `error_message` | VARCHAR(512) |  |  | 最近失败原因 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_event_no`(tenant_id, event_no);索引 `idx_status_retry`(tenant_id, status, next_retry_at);索引 `idx_aggregate`(tenant_id, aggregate_type, aggregate_id)

说明:不对外推送;本表只记录分发,`retry_count/next_retry_at` 用于分发失败重试。全部投递 `DONE/SKIPPED` 后按保留期清理。

#### `mes_domain_event_delivery` 领域事件投递
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `event_id` | BIGINT | 是 |  | 领域事件ID |
| `subscriber` | VARCHAR(64) | 是 |  | 订阅者编码,如 WO_PROGRESS/ALERT_RULE/PM_COUNTER/MATERIAL_REQ |
| `aggregate_type` | VARCHAR(32) | 是 |  | 聚合类型(冗余,用于同聚合顺序投递) |
| `aggregate_id` | BIGINT |  |  | 聚合ID(冗余) |
| `status` | VARCHAR(16) | 是 | PENDING | PENDING/DONE/FAILED/SKIPPED |
| `retry_count` | INT | 是 | 0 | 已重试次数 |
| `next_retry_at` | DATETIME |  |  | 下次重试时间 |
| `processed_at` | DATETIME |  |  | 处理完成时间 |
| `error_message` | VARCHAR(512) |  |  | 最近失败原因 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_event_subscriber`(tenant_id, event_id, subscriber);索引 `idx_status_retry`(tenant_id, status, next_retry_at);索引 `idx_aggregate`(tenant_id, subscriber, aggregate_type, aggregate_id, status)

说明:订阅者的业务写入与本行置 `DONE` 在同一事务提交,因此每个订阅者恰好生效一次;同一 `(subscriber, aggregate)` 存在更早的未完成投递时,后续投递等待(保证出站先于撤销处理)。`FAILED` 后同聚合后续投递暂停,产生 `SYSTEM` 预警,人工重投或标记 `SKIPPED` 后继续。

#### `hist_mes_txn_reversal` 事务撤销登记
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `orig_txn_id` | BIGINT | 是 |  | 被撤销的原事务 |
| `reversal_txn_id` | BIGINT | 是 |  | 执行撤销的纠错事务 |
| `object_type` | VARCHAR(32) |  |  | 主对象类型 |
| `object_id` | BIGINT |  |  | 主对象ID |
| `operator` | VARCHAR(64) | 是 |  | 操作人 |
| `reason_code` | VARCHAR(64) |  |  | 原因码 |
| `reversed_at` | DATETIME | 是 |  | 撤销时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:唯一 `uk_orig_txn`(tenant_id, orig_txn_id);索引 `idx_reversal_txn`(tenant_id, reversal_txn_id);索引 `idx_object`(tenant_id, object_type, object_id)

说明:只增表。统计与追溯统一通过本表排除已撤销事务;`orig_txn_id` 唯一保证同一笔事务只能被撤销一次。

#### `mes_alert` 站内预警
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `alert_type` | VARCHAR(32) | 是 |  | MATERIAL_LOW/MATERIAL_EXPIRED/BINDING_DEPLETED/QUEUE_TIME/TIME_LINK/HOLD_TIMEOUT/TOOL_LIFE/EQP_FAULT/PM_DUE/SYSTEM |
| `level` | VARCHAR(8) | 是 | WARN | INFO/WARN/ERROR |
| `subject_type` | VARCHAR(32) |  |  | 对象类型 |
| `subject_id` | BIGINT |  |  | 对象ID |
| `title` | VARCHAR(128) | 是 |  | 标题 |
| `content` | VARCHAR(512) |  |  | 内容 |
| `status` | VARCHAR(8) | 是 | NEW | NEW/READ/CLOSED |
| `receiver_scope` | VARCHAR(64) |  |  | 接收范围 ALL/ROLE:xxx/USER:xxx/AREA:xxx |
| `read_by` | VARCHAR(64) |  |  | 已读人 |
| `read_at` | DATETIME |  |  | 已读时间 |
| `event_id` | BIGINT |  |  | 来源领域事件 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_status_time`(tenant_id, status, create_at);索引 `idx_subject`(tenant_id, subject_type, subject_id);索引 `idx_type_status`(tenant_id, alert_type, status)

说明:自 M9 前移到 M0:M4(Hold 超时)、M7(过期)、M8(工具寿命/Queue Time)都要产生预警。M12 增加规则来源字段。

#### `shedlock` ShedLock 分布式任务锁(ShedLock JDBC 标准结构)

```sql
-- shedlock ShedLock 分布式任务锁(标准结构,无通用审计列)
CREATE TABLE IF NOT EXISTS `shedlock` (
    `name` VARCHAR(64) NOT NULL COMMENT '锁名称',
    `lock_until` TIMESTAMP(3) NOT NULL COMMENT '锁定截止时间',
    `locked_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加锁时间',
    `locked_by` VARCHAR(255) NOT NULL COMMENT '持锁实例',
    PRIMARY KEY (`name`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'ShedLock 分布式任务锁';
```

说明:结构由 ShedLock 库规定(`name/lock_until/locked_at/locked_by`),不含租户与审计列;任务锁是全局的,租户遍历在任务内部完成。

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- GET/POST/PUT/DELETE `/api/v1/mes/sequence-rules` 编号规则 CRUD
- GET `/api/v1/mes/sequence-rules/{ruleCode}/preview` 预览下一个号码(不消耗流水)
- GET `/api/v1/mes/transactions` 事务流水分页查询(按对象/类型/时间/操作人/结果)
- GET `/api/v1/mes/transactions/{id}` 事务详情(含 payload 与子事务)
- GET `/api/v1/mes/events` 领域事件分页查询(运维排障);GET `/api/v1/mes/event-deliveries`(按订阅者/状态);POST `/api/v1/mes/event-deliveries/{id}/retry` 重投、`/skip` 跳过
- GET `/api/v1/mes/alerts`;POST `/api/v1/mes/alerts/{id}/read`、`/close`
- POST `/api/v1/mes/tenants/{tenantId}/init` 为指定租户初始化 MES 种子(幂等)

## 6. 验收标准

1. 应用启动无报错,`mes_*`、`hist_mes_*`、`shedlock` 由 Flyway 创建成功
2. MES 菜单(空页面)可见,权限可分配到角色;新建租户后自动具备 MES 菜单与字典
3. 编号规则可配置,并发 100 次取号无重复
4. 同一对象两个并发事务:一个成功,另一个**立即**返回"对象被占用",不排队等待
5. 同一 `request_id` 成功后重复提交只生效一次;首次失败后用同一 `request_id` 重试可成功
6. 失败事务与 Interlock BLOCK 都留有流水/明细,不随业务回滚丢失
7. 后台任务对每个真实租户都被执行,且同一时刻只有一个实例执行同一任务
8. `@Version` 实体在版本冲突时抛出明确错误,而不是静默覆盖
9. 压测下写请求不出现连接池耗尽/死锁(业务事务内不嵌套 REQUIRES_NEW)
10. 抽检计数器并发递增无丢失、无"对象被占用"误报;PM 计数由事件订阅者原子更新,同一事件重复投递不重复计数
11. 同一事务只能被撤销一次(`hist_mes_txn_reversal` 唯一约束)
12. 架构边界测试通过
13. 同一事件的某个订阅者失败重试时,其他订阅者不重复执行;同一 Lot 的撤销事件不会先于其出站事件被处理

## 7. 待确认

1. 领域事件轮询间隔(默认 2 秒)与失败重试次数(默认 5 次)是否接受?
2. 事务 payload 是否全量存 JSON?(便于排障,代价是存储;建议全量并设置 90 天清理开关)
3. "对象被占用"是否需要等待重试(如终端自动重试 1 次)?当前设计为立即失败,由终端提示重试。
