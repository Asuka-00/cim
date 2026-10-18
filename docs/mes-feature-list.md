# 半导体通用 MES 功能清单 v8(按实现顺序,待核对)

> 状态:草案,**未开始实现**。
> 核对方式:`[ ]` 改为 `[x]` 保留 / `[-]` 砍掉,意见写在行尾。
> 排序依据:**依赖关系优先**,后一里程碑只依赖前面已完成的内容;每个里程碑结束都有可演示的验收点。
> 行尾括号 `(P0/P1/P2/P3)` 为原优先级,仅供参考,以里程碑顺序为准。
> **【新增】** = v4 新增;**【调整】** = v4 移动位置或改写;**【找回】** = v1 有、v2 改写时遗漏;**【v5】** = 第一轮设计审核后新增或改写;**【v6】** = 第二轮复审后新增或改写;**【v7】** = 第三轮一致性修订;**【v8】** = 第四轮审查修订(均见 `README.md` 第 7 节对照表)。

---

## 0. 范围与已确认决策

| # | 决策 |
|---|---|
| D1 | 场景:**通用**,覆盖前道与封测,**包含封测后段计数单位转换**(Wafer → Die → Strip → Unit 等)【调整】 |
| D2 | **单系统独立运行**,不依赖、不对接 EAP / SPC / RTD / ERP / WMS / AMHS,不做外部集成适配 |
| D3 | 作业以**人工操作为主**(操作员终端扫码 + 手工录入),设备状态、采集值、位置均在 MES 内手工维护 |
| D4 | Track-In 前 Interlock 由 MES 决策 |
| D5 | 物料:**轻量库房**(收货、库位、领退、盘点)+ 线边 + 设备端绑定 |
| D6 | 设备端物料绑定;Track-Out **自动反冲**,支持人工修正留痕 |
| D7 | 采集原始结果 MES 存储;质量判定只做 **Spec 上下限**,不做控制图/判异 |
| D8 | **Batch 组批**与 **Wafer 级 Track** 均为 P1 |
| D9 | Route 重入简化:站点可重复出现 + 记录"第 N 次经过",不做 Stage/Module/Layer |
| D10 | 沿用 `backend-db`(Java 21 / Spring Boot / MyBatis-Plus / Flyway,纯 DB)+ `soybean-admin`(Vue 3 / Naive UI);**首期只支持 MySQL 8**(见 D20) |
| D11 | 不做分区与归档;迁移脚本只写 MySQL(不生成其他数据库脚本,其他库不在范围) |
| D12 | 一个租户 = 一个厂区;仅保留内部领域事件,无对外推送 |
| D13 | 模块拆分:`core`(编号/锁/事务/事件,M0 新增)/ `base` / `warehouse` / `exec`(+ P2 的 `quality` / `maint`);业务模块互不依赖,均依赖 `core`;跨模块契约(仅接口/DTO/事件/枚举)放 `app-common` 的 `share.integration.mes.*`【v7】 |
| D14 | 审批流用"状态机 + 审批人字段"简化版;MES 菜单/权限种子放单独 `R__system_data_mes.sql`(须排在 `R__system_data` 之后)【v7】 |
| D15 | 【新增】Lot 数量**始终带计数单位**;Die 默认不单独建 ID(按数量与 Bin 管理),Strip/载具建 ID,Unit 序列号为可选 |
| D16 | 【v5】写事务在事务内按序行锁(工单→设备→Lot→Wafer,聚合根 `FOR UPDATE NOWAIT`,锁分级见 D22),冲突立即报"对象被占用";失败事务与拦截日志用独立事务留痕;幂等键只对成功事务生效 |
| D17 | 【v5】MES 表不用软删除;"空表示通用"的可选限定一律用 0 哨兵,不用 NULL;只增表统一 `hist_` 前缀 |
| D18 | 【v5】Route/BOM/Recipe/产品任一时刻只有一个 ACTIVE 版本,在制 Lot 通过自身记录的版本 ID 继续使用旧版本 |
| D19 | 【v5】Lot 在各站点的经过次数显式计数(`mes_lot_step_pass`);Route 步骤级覆盖(Recipe/设备组/规格/抽检/工具)优先于站点级绑定 |
| D20 | 【v6】**首期只支持 MySQL 8**:`FOR UPDATE NOWAIT`、JSON 列、唯一键多 NULL 均按 MySQL 设计;非 MySQL 库启动时拒绝启用 MES |
| D21 | 【v7】业务事务内禁止 `REQUIRES_NEW`:取号在事务前、失败流水在回滚后、逻辑锁由独立请求处理;一次写请求最多持有 1 个连接;"失败不阻塞"用 NESTED 或预校验;事务内拆出的子 Lot 号 = 父 Lot 号 + 拆批序号 |
| D22 | 【v6】锁分级:聚合根(设备/Lot/Wafer/Batch/Carrier)NOWAIT;共享计数器原子更新;库存/工具行短等待;锁序 工单→设备→Lot→Wafer→其他 |
| D23 | 【v6】产品版本绑定 `route_id/bom_id`,下工单时固化具体版本;步骤级覆盖独立成表,覆盖语义为整体替换 |

---

## 路线总览

| 里程碑 | 主题 | 依赖 | 完成后能做什么 |
|---|---|---|---|
| **M0** | 地基与骨架 | — | 模块能启动,菜单可见,有统一事务层、编号规则、并发控制、调度底座、租户种子 |
| **M1** | 主数据 I:独立实体 | M0 | 能维护产品/站点/设备/物料/Recipe/计数单位 |
| **M2** | 主数据 II:制程与版本 | M1 | 能定义完整 Route(含单位转换站点)、BOM,版本可生效 |
| **M3** | 执行核心 | M2 | **首个可跑通**:工单→开批→Track-In/Out→Hold→Scrap |
| **M4** | 执行扩展 | M3 | Split/Merge/Rework/纠错/Future Action/Wafer 级 Track/WIP 看板 |
| **M5** | Batch 组批 | M4 | 批处理设备组批进出站 |
| **M6** | 封测后段:单位转换与载具 | M4 | Wafer → Die → Strip → Unit 转换、Strip/Tray/Reel 载具 |
| **M7** | 轻量库房 | M1 | 物料入库、领退、库存查询 |
| **M8** | 管控完整化 | M4, M2 | Interlock 全规则、资格矩阵、工具、数据权限 |
| **M9** | 物料执行与反冲 | M7, M8 | 设备上料、自动反冲、开批扣晶圆、消耗追溯 |
| **M10** | 采集与质量判定 | M3 | 录入采集值、Spec 判定、抽检 |
| **M11** | 追溯与报表 | M9, M10 | 正反向追溯、报表、生产日报 |
| **M12** | 上线准备与平台增强 | M3+ | 导入、打印、通知、交接班、WIP 初始化 |
| **M13** | 质量/维保/高级分析 | M11 | NCR/MRB、PM、OEE、盘点等 |

> **最小上线线**:
> - 前道产线:M0–M4 + M7 + M8 + M9 + M10 + M12(M9 的物料 Interlock 依赖 M8)【v5】
> - 封测产线:在前道基础上 + M6
> - M5(Batch)视首批产线是否有批处理设备穿插;M8 已在两条最小上线线内(M9 依赖它)【v7】。

---

## M0 地基与骨架

**目标:** 四个 MES 模块(core/base/warehouse/exec)能启动并被 `app-boot` 聚合,约束与公共能力就位。

- [ ] 建 `app-mes-base` / `app-mes-warehouse` / `app-mes-exec` 模块,接入父 POM 与 `app-boot`
- [ ] 各模块架构边界测试(domain 不依赖 api/infra;模块间只经 `share.integration.mes.*`)
- [ ] MES 业务错误码段(400000+)与统一异常(P0)
- [ ] MES 菜单/权限种子 `R__system_data_mes.sql`(P0)【v7】
- [ ] 统一事务层:所有 Lot/Wafer/物料变更走同一入口,写历史并发布**内部**领域事件(P0)
- [ ] 【新增】并发控制:Lot/Wafer/Batch/物料批级锁或版本号,防止同一对象被同时操作(P0)
- [ ] 【新增】编号规则:工单号、Lot ID、Wafer ID、Batch ID、物料批号、Strip ID 的可配置生成规则(前缀 + 日期 + 流水)(P0)
- [ ] 【新增】MES 运行参数:是否强制扫码、是否允许跳站、是否启用 Wafer 级 Track 等全局开关,复用现有系统配置(P0)
- [ ] MES 字典:Hold/Scrap/Rework 原因码、缺陷码、停机原因码、物料状态(P0)
- [ ] 【v5】app-common 补注册乐观锁拦截器(现状未注册),`@Version` 冲突抛错(P0)
- [ ] 【v5】AppendOnly 实体基类与仓储支撑(只增表不含 `update_*`)(P0)
- [ ] 【v5】调度底座:ShedLock + `shedlock` 表 + 按租户遍历的任务基类(现状为空,后台任务否则对真实租户失效)(P0)
- [ ] 【v5】`MesTenantInitializer`:新建/已有租户的 MES 菜单、字典、参数、内置规则初始化(幂等)(P0)
- [ ] 【v5】`mes_alert` 站内预警前移到 M0(M4/M7/M8 都要产生预警)(P0)
- [ ] 【v5】热表加入透明查询缓存排除清单(P0)
- [ ] 【v6】事务撤销登记 `hist_mes_txn_reversal`(一笔事务只能撤销一次;统计/追溯据此排除)(P0)
- [ ] 【v6】新增 `updateWithVersion()`(影响行数为 0 抛错)、修正会话租户优先级使 `TenantContextHolder.runAs` 生效【v7】、`TenantCreatedEvent` 契约(P0)
- [ ] 【v6】MES 启动检查:非 MySQL 拒绝启用;菜单/权限种子 `R__system_data_mes.sql` 排在系统种子之后(P0)
- [ ] 前端 `views/mes/*`、`service/api/mes/*` 目录与路由骨架(P0)
- [ ] 复用确认:用户/角色/权限/租户/字典/系统配置/操作日志/多租户/审计(P0)

**验收:** 启动无报错;MES 空菜单可见;字典与编号规则可维护;并发操作同一 Lot 时只有一个成功;架构测试通过。

---
- [ ] 【v8】领域事件按订阅者投递 `mes_domain_event_delivery`,订阅者独立重试、同聚合顺序投递;内部事件目录(P0)
- [ ] 【v8】首批跨模块契约:`EquipmentStatusProvider`/`WorkOrderProvider`/`LotCommandProvider`/`InventoryProvider`(P0)

## M1 主数据 I:独立实体(无相互依赖)

**目标:** 不依赖其他 MES 实体的主数据先落地。

- [ ] 【新增】计数单位:Wafer / Die / Strip / Unit / Frame / Tray / Reel / Tube 等,可扩展(P0)
- [ ] 【新增】物料单位换算:库存单位与 BOM 用量单位换算(如 mL ↔ 桶、片 ↔ 盒)(P1)
- [ ] 产品/料号 CRUD:编码、名称、产品族、Wafer 尺寸、Die 数/Wafer、启用(P0)
- [ ] 【新增】产品换算系数:Gross Die/Wafer、Die/Strip、Unit/Strip、Unit/Tray、Unit/Reel 等(P1)
- [ ] 产品族管理(P1)
- [ ] 【新增】客户主数据与客户料号,产品关联客户(P2)
- [ ] 站点 CRUD:编码、名称、类型(加工/量测/检验/Hold/Ship/转换/包装/仓储)(P0)
- [ ] 【v5】站点是否需要设备/Recipe 标志(目检、Hold、Ship 类站点不选设备)(P0)
- [ ] 设备组管理(P1)【调整:排在设备台账之前】
- [ ] 设备台账 CRUD:编码、名称、类型、设备组、区域(P0)
- [ ] 设备状态手工变更(SEMI E10)+ 状态历史 + 停机原因码(P0)
- [ ] 物料主数据:类型(晶圆/原材料/辅料/化学品/气体/封装料)、单位、关键料标识(P0)
- [ ] Recipe 主数据与版本(名称、参数说明)(P0)

**验收:** 能创建一套产品(含换算系数)、站点、设备、物料、Recipe;设备状态可切换并留历史。

---
- [ ] 【v8】版本生效锁主表行,并发生效也只有一个 ACTIVE;Recipe `activate` 改由审批触发(P0)

## M2 主数据 II:制程与版本

**目标:** 把 M1 的实体编排成可执行的制程;建立版本生效机制。

- [ ] 版本状态机与审批人字段(草稿→待审→生效→失效),供 Route/Recipe/BOM/产品版本复用(P1)
- [ ] 产品版本(生效后不可改)(P0)
- [ ] Route CRUD、版本管理(P0)
- [ ] Route 步骤编排:顺序号、站点、必选/可选(P0)
- [ ] Route 重入:站点可重复,步骤实例唯一序号(P0)
- [ ] 【新增】步骤计数单位:每个步骤声明当前计数单位;标记"单位转换站点"(如切割 Wafer→Die、装片 Die→Strip、切筋成型 Strip→Unit、包装 Unit→Tray/Reel)(P0)
- [ ] Route 版本复制(P1)
- [ ] 产品版本绑定 Route(下工单固化版本,见 v6 条目)(P0)
- [ ] 站点绑定设备组、允许 Recipe(P0)
- [ ] 【v5】Route 步骤级覆盖:步骤可指定 Recipe/设备组,规格、抽检、工具绑定可限定到 Route 步骤(P0)
- [ ] 【v5】设备能力与资格合并为一张表(含有效期、Inhibit 暂停状态)(P1)
- [ ] 【v6】步骤级覆盖独立成表,随 Route 版本复制、生效只读,整体替换站点级集合;M2 建 `mes_route_step_recipe`,`tool` 在 M8、`collect_item/sampling` 在 M10 建表并补入复制逻辑【v7】(P0)
- [ ] 【v7】下工单固化版本时校验 BOM 行 `step_id/route_seq_no` 与 Route 版本一致(P0)
- [ ] 【v6】产品版本绑定 `route_id/bom_id`,下工单时固化 Route/BOM 版本(P0)
- [ ] Recipe ↔ 设备/站点/产品 绑定,Recipe 版本审批生效(P1)
- [ ] 站点组/区域(Bay/Area)(P1)
- [ ] 设备能力(可加工产品/站点/Recipe)(P1)
- [ ] Port/Chamber 管理(P1)
- [ ] 【新增】设备同时加工上限:按设备/Port/Chamber 配置可同时在制的 Lot 数(P1)
- [ ] 站点 Queue Time / Time Link 约束、标准工时(P1)
- [ ] BOM 头/版本/行:用量、单位、损耗率、消耗方式(P0)
- [ ] 【调整】BOM 消耗方式扩展:PER_WAFER / PER_DIE / PER_STRIP / PER_UNIT / PER_LOT / PER_RUN / PER_TIME(P0)
- [ ] BOM 行绑定站点,标记是否反冲(P0)
- [ ] 【调整:由库房移入】BOM 替代料组与优先级(P1)
- [ ] BOM 版本审批与生效(P1)
- [ ] 产品版本绑定 BOM(下工单固化版本)(P1)

**验收:** 能给一个产品配出完整 Route(含重复站点与单位转换站点)、BOM,并让版本生效。

---
- [ ] 【v8】设备能力匹配:最具体优先、任一层 SUSPENDED/REVOKED/EXPIRED 即拒绝、校验有效期(P0)

## M3 执行核心(首个可跑通版本)

**目标:** 在人工操作下完成一个 Lot 从开批到流转、Hold、报废的最小闭环。

**状态模型**
- [ ] 【新增】Lot 状态机:等待/加工中/Hold/完成/已出货/报废/终止,及允许的转换(P0)
- [ ] 【新增】Wafer 状态机:正常/加工中/报废/已拆出/已转换(P0)
- [ ] 【新增】Lot 数量带计数单位,单位随所在步骤确定(数据模型为 M6 预留)(P0)

**工单与批次**
- [ ] 工单创建/下达/关闭/取消,绑定产品版本、数量(含单位)、优先级、交期(P0)
- [ ] Lot 类型:Production / Engineering / Monitor / Dummy / Control / Test(P0)
- [ ] 开批:按工单创建 Lot 与 Wafer,WaferID 手工录入或按编号规则生成(P0)
- [ ] Wafer 管理:WaferID、Slot、状态、所属 Lot(P0)
- [ ] Lot 优先级/Hot Lot 设置(P1)

**作业事务**
- [ ] Track-In:选择设备/Recipe/Carrier,手工录入(P0)
- [ ] Track-Out:推进下一站点、录入良废数、"第 N 次经过"计数(P0)
- [ ] Track-In 基础 Interlock:Lot 状态/Hold、站点正确性、设备状态、设备同时加工上限(P0)
- [ ] 【新增】设备状态随 Track 联动:Track-In 切"生产",最后一个 Lot Track-Out 后回"待机"(P0)
- [ ] Hold / Release,**多重 Hold**(多来源/多原因,各自 Release)(P0)
- [ ] 【新增】Hold 码与 Release 权限:按原因码限定可 Release 的角色(P1)
- [ ] Scrap:Lot 级 / Wafer 级(P0)
- [ ] Lot 事务历史、Wafer 历史(P0)
- [ ] Lot 位置:Track 自动更新(设备/区域)(P1)
- [ ] 【v5】Lot 站点经过次数显式计数(Split 复制、Merge 校验、WIP 导入必填、Undo 回减)(P0)
- [ ] 【v5】下一步解析链(默认下一步,后续插入返工/条件跳转/抽检跳站)与 Interlock 引擎骨架(P0)
- [ ] 【v5】系统 Hold 幂等(同 Lot/步骤/来源/原因最多一个 ACTIVE)(P0)
- [ ] 【v5】工单进度异步汇总,按起始单位核算(不在 Lot 事务内更新工单)(P1)
- [ ] 【v5】Lot 状态补充 MERGED、CONSUMED(P0)
- [ ] 【v6】终态集合与 Lot 状态迁移矩阵(P0)
- [ ] 【v7】终态分为完成态(COMPLETED/SHIPPED/CONSUMED)与关闭态(SCRAPPED/TERMINATED/MERGED);工单增加 `terminated_qty`,终止不计完成(P0)
- [ ] 【v7】追溯关系去重后的撤销口径:撤销后再次发生的同一关系改指向新事务,仍可查到(P0)
- [ ] 【v6】到站钩子 ArrivalHook(所有改变当前步骤的事务统一经过)(P0)
- [ ] 【v6】起始单位守恒:Split/Merge/Bonus 转移 `initial_qty/scrap_qty`;开批锁工单行实时校验余量(P1)
- [ ] 【v6】Track-In 设备状态联动不覆盖停机/人工态(PM 后 Qual 的 Monitor Lot 可进站)(P0)

**现场入口**
- [ ] 作业清单:按设备/站点/区域的待加工 Lot 队列(P0)
- [ ] Lot / Wafer 列表与状态查询(P0)
- [ ] **操作员终端 P0**:扫码 Lot/设备 → Track-In/Out、Hold、Scrap(大按钮、适配平板/PDA)(P0)
- [ ] 管理端:工单、Lot、Wafer 管理页(P0)

**验收:** 一个 Lot 能按 Route 跑完多个站点(含重复站点),中途多重 Hold/Release、报废部分 Wafer;设备状态随进出站切换;历史完整可查。

---
- [ ] 【v8】Track-Out 同事务发布 `LOT_TRACK_OUT`;NCR 等单据来源的 Hold 带 `source_ref_id`(P0)

## M4 执行扩展

**目标:** 补齐日常生产必需的 Lot/Wafer 操作、纠错与预约动作。

**Lot 操作**
- [ ] Split / Merge,含父子关系(P1)
- [ ] Rework:返工分支定义(入口/回归站点)、次数上限、进入返工(P1)
- [ ] Skip / 跳站(受控,需权限与原因)(P1)
- [ ] Bonus / 补投(P1)
- [ ] Ship / 完工出货(P1)
- [ ] Lot 终止(Terminate)与 Wafer 回收(P1)
- [ ] 【找回】在制 Lot 改 Route / 改产品版本(受控,指定切换站点,留痕)(P1)
- [ ] Route 条件跳转/分支(P1)

**纠错与预约**
- [ ] **纠错事务**(审计留痕):Cancel Track-In、Undo Track-Out、Move Back、Force Move(P1)
- [ ] 【新增】报废撤销 Unscrap(受控、留痕)(P1)
- [ ] 【新增】预约动作 Future Action:预约在指定站点自动 Hold / 自动 Split / 更换 Recipe,到站触发(P1)
- [ ] 【v5】纠错事务通则:只撤销最后一笔有效事务、逐类补偿处理器、变更前快照;转换/出货/拆合批不可撤销(P1)
- [ ] 【v5】Split 继承规则(Hold/计时/返工状态/预约动作)与 Merge 约束(同工单/同计数/无 Hold/非 Batch)(P1)
- [ ] 【v5】Wafer 回收再入厂(新 WaferID,保留客户编号与来源关联)(P1)
- [ ] 【v6】返工次数按 `(root_lot_id, rework_id)` 统计;Move Back 不回减经过次数;Future Action 失败用 NESTED 处理,CHANGE_RECIPE 写 Lot 覆盖字段(P1)
- [ ] 【v6】Wafer 级 Track 的自动 Merge 由到站钩子触发,隐式 Split 可随 Cancel Track-In 补偿(P1)

**Wafer 与 Carrier**
- [ ] Wafer 级 Track:部分 Wafer 进出站、Wafer 级 Split(P1)
- [ ] Slot Map 维护与变更记录(P1)
- [ ] Wafer 用途区分:产品片 / Monitor / Dummy / Control,Dummy 复用次数管控(P1)
- [ ] Carrier 台账、状态、与 Lot 绑定/解绑(P1)

**工单与现场**
- [ ] 工单拆分/合并;工单进度(已开批/在制/完成/报废)与完工关闭(P1)
- [ ] 作业清单排序规则(优先级/Hot Lot/FIFO/Queue Time 紧迫度)+ 可用设备列表(P1)
- [ ] Lot 位置手工更新(暂存架/区域)(P1)
- [ ] 【调整:由报表移入】WIP 看板:按站点/产品/工单/状态(P1)
- [ ] 【调整:由报表移入】站点 Queue 视图(P1)
- [ ] Hold 清单与超时提醒、Lot 批量操作(批量 Hold/Release/改优先级)、Lot 备注/标签(P2)
- [ ] Carrier 清洗周期与使用次数管控(P2)

**验收:** 能处理拆批合批、返工、改 Route、误操作回退与报废撤销、预约 Hold 到站触发、部分 Wafer 进出站,全部有审计记录。

---
- [ ] 【v8】Undo Track-Out 恢复为 RUNNING(含 COMPLETED→RUNNING);Merge 后源 Lot 数量清零;预约动作绑定 Route 版本,改 Route/合批时取消(P0)

## M5 Batch 组批

**目标:** 支持炉管、清洗槽等批处理设备。

- [ ] 站点标记为 Batch 站点,配置批量上下限(P1)
- [ ] Batch 创建:从同站点、同 Recipe、同设备组的待加工 Lot 组批(P1)
- [ ] Batch 约束校验:批量范围、产品/Recipe 兼容(P1)
- [ ] Batch Track-In / Track-Out:一次操作展开为各 Lot 事务;默认整批原子,部分成功模式逐 Lot 独立事务【v6】(P1)
- [ ] Batch 拆批/取消、批内 Lot 异常剔除(P1)
- [ ] Batch 历史与 Lot 关联(P1)
- [ ] 操作员终端 Batch 作业界面(P1)

**验收:** 多个 Lot 组成一个 Batch 进出站,拆批后各 Lot 历史正确。

---
- [ ] 【v8】Batch 整体占 1 个设备名额、批级物料只扣一次;剔除为单一事务;`batch_id` 生命周期与单独作业拦截;部分成功模式的父事务口径(P1)

## M6 封测后段:单位转换与载具 【整节新增】

**目标:** 支持封测从 Wafer 到 Die、Strip、Unit 的计数与载具转换,数量与追溯不断链。

**单位转换**
- [ ] 单位转换事务:在转换站点 Track-Out 时录入转换后数量(如切割后良品 Die 数),系统按换算系数给出参考值并校验偏差(P1)
- [ ] 转换历史:转换前后单位、数量、损耗,可追溯到原 Wafer(P1)
- [ ] 转换后 Lot 数量、报废、良率按新单位统计,同时保留原 Wafer 数(P1)
- [ ] Die Bin 数量记录:按 Bin 记录良品/不良 Die 数(手工录入或导入 Wafer Map 文件)(P1)
- [ ] 按 Bin 拆批:不同 Bin 的 Die 拆成不同 Lot 继续流转(P2)

**Lot 形态转换**
- [ ] Wafer Lot → 封装 Lot:一个或多个 Wafer Lot 投入生成封装 Lot,建立父子追溯(P1)
- [ ] 多 Wafer Lot 合并投料(同产品、同 Bin 规则)(P1)
- [ ] 【v5】投入扣减:源 Wafer Lot 数量扣减,归零置 CONSUMED;封装 Lot 绑定封装产品工单;Die Bank 站点(P1)
- [ ] 【v5】Wafer Map 多版本(重测)、Frame 逐片对应 Wafer、Lot 副数量(Strip 数 + Unit 数)(P1)
- [ ] 【v6】Die 数量对账不变式与 `hist_mes_die_bin_log`(逐笔流水);汇总行与逐片行不并存(P1)

**载具**
- [ ] 载具类型扩展:Frame、Strip、Magazine、Tray、Reel、Tube(复用 Carrier 模型)(P1)
- [ ] Strip ID 生成与 Strip ↔ Lot 关系,Strip 级报废(P1)
- [ ] 装盘/装卷:Unit 装入 Tray/Reel,记录每个载具数量(P1)
- [ ] Unit 序列号(可选,按产品开关启用)(P2)

**验收:** 一个 Wafer Lot 经切割转换为 Die,装片成 Strip,切筋成型成 Unit 并装 Reel 出货;任一 Reel 可反查到原 Wafer。

---
- [ ] 【v8】发生过封装投入的源 Lot 余量报废归零时置 CONSUMED;Die Bin 流水补 SPLIT/MIGRATE/UNSCRAP;BOX 只属包装容器(P1)

## M7 轻量库房

**目标:** 物料有入口、有库位、有库存;为反冲打底。

- [ ] 库房/库位定义:库房、区域、库位、类型(原料库/线边/隔离)(P0)
- [ ] 物料批建立:批号、供应商、效期、数量、状态(待检/合格/隔离/过期)(P0)
- [ ] 来料收货:手工录入、生成物料批并入库位(P1)
- [ ] 来料检验结果记录(合格/不合格/特采),不合格转隔离(P1)
- [ ] 库存查询:按物料/批/库位/状态(P1)
- [ ] 库存冻结/解冻、过期处理(P1)
- [ ] 【v5】来料 Wafer 清单:收货时录入客户 WaferID,开批时自动带出(P1)
- [ ] 【v5】线边库位关联设备/区域;物料批号租户内唯一;批状态由库存与绑定推导(P1)
- [ ] 领料:按工单/站点领到线边,扫码出库(P1)
- [ ] 退料/退库:线边 → 库位(P1)
- [ ] 线边库存管理(P1)
- [ ] 库存流水:出入库全记录(P2)

**验收:** 来料入库、领到线边、退回、冻结过期,库存与流水一致。

---
- [ ] 【v8】单据确认走统一事务层并锁单头;库存流水 `txn_id` 必填;发布 `MATERIAL_ISSUED`(P0)

## M8 管控完整化

**目标:** Track-In 前所有校验规则就位,工具、资格、数据权限可用。

- [ ] 工具类型与台账(Mask/Reticle、Probe Card、夹具等)(P1)
- [ ] 工具状态、使用次数/寿命累计、绑定设备/站点(P1)
- [ ] 工具领用/归还/清洗/保养记录(P1)
- [ ] 【v6】Track-In 工具绑定事件(`TRACK_IN_BIND`)与使用/撤销对应(P1)
- [ ] 资格矩阵:设备×Recipe×产品、人员×设备/站点(含有效期)(P1)
- [ ] Interlock:Recipe 校验(与设备/站点/产品绑定匹配)(P1)
- [ ] Interlock:工具校验(存在、状态正常、未超寿命)(P1)
- [ ] Interlock:人员校验(操作权限 + 资格矩阵)(P1)
- [ ] Interlock:Queue Time / Time Link 校验;超时可配置自动 Hold(P1)
- [ ] Interlock 校验日志:通过/失败原因明细(P1)
- [ ] 【新增】数据权限:用户按区域/站点/设备组限定可见与可操作的 Lot、设备(P1)
- [ ] 设备异常登记:描述、影响 Lot、处理结果(P1)
- [ ] 关键操作二次确认/电子签名(Hold Release、Scrap、版本生效等)(P1)
- [ ] Interlock 规则可配置:按站点/产品/设备组启用,阻断/警告(P2)
- [ ] 工具寿命到期预警(P2)

**验收:** Track-In 在 Recipe 不匹配、工具超寿命、无资质、Queue Time 超时时被拦截,且有明细日志;用户只能操作授权区域。

---
- [ ] 【v8】Time Link 按 Lot 独立计时表 `mes_lot_time_link_timer`;工具状态迁移与 `UNBIND/USE_REVERSAL`;内置规则补 `STEP_MATCH/EQP_ALLOWED/EQP_CAPACITY` 且不可停用(P0)

## M9 物料执行与自动反冲

**目标:** 设备端物料绑定、按 BOM 自动扣减、可追到 Lot/Wafer。

- [ ] 设备物料位(Slot)定义(P1)
- [ ] 设备上料/下料/换料:扫码绑定,校验 BOM 允许与槽位类型(P1)
- [ ] 设备端剩余量/寿命递减,耗尽自动置状态(P1)
- [ ] 工单物料需求展开(工单数量 × BOM)与齐套检查(P1)
- [ ] 【新增】开批扣减来料晶圆:开批时从库房扣减晶圆物料批,WaferID 与来料批建立追溯(P1)
- [ ] Interlock:物料校验(关键料已绑定、剩余量>0、批次合格/未过期)(P1)
- [ ] Track-Out 自动反冲(按 BOM、消耗方式与当前计数单位)(P1)
- [ ] 【v5】反冲不足策略(关键料阻断/非关键料欠账)、防重复反冲唯一约束(P1)
- [ ] 【v5】下料剩余量处置(退回线边/报废),保证账物一致(P1)
- [ ] 【v6】反冲消耗记录每笔(含修正/Undo)有独立事务ID;唯一键含绑定与库位(P1)
- [ ] 物料消耗记录:绑定 Lot/Wafer/Step/设备/物料批(P1)
- [ ] 低位/过期预警(站内通知)(P1)
- [ ] 操作员终端:上料/换料、领料确认(P1)
- [ ] 【新增】物料开封/回温时效:开封后寿命、回温时间,超时禁止使用(P2)
- [ ] 手工修正消耗:申请 → 审批 → 留痕(P2)
- [ ] 替代料使用规则:按 BOM 行 `substitute_policy` ALLOW/WARN/BLOCK,不设事后审批【v7】(P2)

**验收:** 开批扣减晶圆库存;上料后 Track-Out 自动扣减;耗尽后下一次 Track-In 被拦截;消耗可追到物料批。

---
- [ ] 【v8】修正只追加差额行;欠账清偿 `SETTLE`;Undo 按净消耗回补;`MATERIAL_CONSUMED` 汇总;开批选用来料 Wafer 清单(P0)

## M10 采集与质量判定

**目标:** 手工录入采集值,Spec 判定,抽检决定量测站点。

- [ ] 采集项定义:参数名、单位、Spec 上下限、数据类型(P1)
- [ ] 站点绑定采集项(是否必采)(P1)
- [ ] 采集结果手工录入与存储,关联 Lot/Wafer/Step/设备(P1)
- [ ] Spec 判定:超规格标记 OOS,可配置自动 Hold(P1)
- [ ] 【v5】采集结果更正/作废(追加记录,不改原值),有效值取链上最后一条(P1)
- [ ] 【v6】抽检计数器原子更新、键含 Route 步骤;采集结果冗余产品用于趋势(P1)
- [ ] 抽检规则:每 N 个 Lot 抽 1 个 / 每 Lot 取 M 片 / 指定 Slot(P1)
- [ ] 站点绑定抽检规则,Track-Out 时判定下一量测站点是否必经(P1)
- [ ] 操作员终端:采集结果录入(P1)
- [ ] 抽检计数器与重置规则(P2)
- [ ] 同参数最近 N 次结果列表/折线(仅展示,不做判异)(P2)
- [ ] Wafer 级多点位(Site)数据存储(P2)
- [ ] 采集结果查询/导出(P2)

**验收:** 量测站点录入数据,超规格自动 Hold;抽检规则决定 Lot 是否走量测站点。

---
- [ ] 【v8】采集录入为独立 `COLLECT` 事务;有效值取链上最后一条;抽检决定挂到下一步解析与到站钩子,撤销不回退计数(P0)

## M11 追溯与报表

**目标:** 全链路可追溯,管理有基础报表。

- [ ] Genealogy:物料批 → Lot/Wafer → 设备 → 人员,正反向查询;含封测单位转换与 Lot 形态转换链路(P1)
- [ ] 追溯查询页(P1)
- [ ] WIP 报表(站点/产品/工单)(P1)
- [ ] 产出/Move/Out 报表(按计数单位)(P1)
- [ ] Cycle Time 报表(Lot、站点、产品)(P1)
- [ ] Hold/Scrap/Rework 分析(P1)
- [ ] 【新增】生产日报:按班次/日的产出、报废、Hold 汇总(P1)

**验收:** 输入一个物料批可查到受影响 Lot/Wafer/Reel;输入 Reel 或 Wafer 可反查物料、设备、操作人。

---

## M12 上线准备与平台增强

**目标:** 具备真实上线所需的外围能力。

- [ ] 主数据 Excel 导入导出:产品/Route/站点/设备/物料/BOM/Recipe(P1)
- [ ] 上线 WIP 初始化导入:在制 Lot/Wafer 当前站点、状态与数量单位(P1)
- [ ] 【v6】WIP 初始化必填项补全:各站点经过次数、返工位置、计时起点、未释放 Hold、Recipe 覆盖(P1)
- [ ] 标签与流程卡打印:Lot Traveler、Carrier/Wafer/Strip/Reel 标签、条码二维码(P1)
- [ ] 站内通知规则:Hold/Queue Time 将超/物料低位/设备异常,复用现有 `realtime`(P1)
- [ ] 班别/班次/日历(P1)
- [ ] 交接班记录:在制、Hold、设备异常、待办(P2)
- [ ] 【新增】站点作业指导:操作员终端显示站点作业说明/SOP 附件(P2)
- [ ] 变更管理 ECN:申请、评审、生效记录(P2)
- [ ] 用户自定义属性 UDA(P2)
- [ ] 国际化文案(P2)

**验收:** 能用 Excel 一次性导入主数据与在制 WIP;流程卡与各类标签可打印。

---
- [ ] 【v8】`mes_file` 文件存储(框架无通用文件服务);WIP 导入含 Time Link 计时起点(P1)

## M13 质量 / 维保 / 高级分析(P2 以后)

**质量(`app-mes-quality`)**
- [ ] 缺陷记录:缺陷码、位置、数量,关联 Lot/Wafer/Step(P2)
- [ ] NCR 不合格品流程(P2)
- [ ] MRB 评审与处置:返工/报废/特采/退回(P2)
- [ ] 良率与 Bin 结果查询(P2)
- [ ] Wafer 标识(Scribe/Laser Mark)记录与扫码核对(P2)
- [ ] Wafer Sort/分级结果记录(P2)

**维保(`app-mes-maint`)**
- [ ] PM 计划:按时间/Lot 数/Wafer 数/使用量触发(P2)
- [ ] 【v5】设备组级 PM 计划按设备分别计数;Qual 允许在停机/工程态运行 Monitor Lot(P2)
- [ ] PM 工单与执行记录(P2)
- [ ] PM 后 Qual 验证,通过后恢复设备可用(P2)
- [ ] 设备维修记录、PM 到期预警(P2)

**分析与增强**
- [ ] E10 统计:利用率、OEE、MTBF/MTTR(基于 Track 联动 + 手工状态)(P2)
- [ ] 工单进度/交期达成报表(P2)
- [ ] 物料消耗与库存报表、良率报表(P2)
- [ ] 库存盘点:计划、录入、差异调整留痕(P2)
- [ ] Route 变更影响分析(P2)
- [ ] 【调整】追溯报告导出(P2)
- [ ] 自定义看板/报表导出(P3)

---
- [ ] 【v8】新建 quality/maint 模块与启用开关;PM 计数流水 `hist_mes_pm_count_log`;NCR Hold 按单据释放;良率记录带 `txn_id`;盘点全程锁库位/仓库(P2)

## 附:已移除(不做)

EAP/SPC/RTD/ERP/WMS/AMHS 的所有集成口与适配、Outbox 投递、对外查询/入站 API、Recipe Body 管理、设备自动状态采集、SPC 控制图与判异、优化调度。

---

## 待你核对

1. 逐条保留/砍掉。
2. **D15**:Die 不建 ID、Strip 建 ID、Unit 序列号可选,是否符合预期?
3. **M6 封测后段**:Bin 拆批放 P2、Wafer Map 文件导入放 P1,是否合适?Wafer Map 文件格式是否有既定标准(如 SEMI E142 / 厂内自定义)?
4. 最小上线线(前道 / 封测两条)是否符合预期。
5. 审批流简化版、种子脚本位置(D14)是否按默认。
