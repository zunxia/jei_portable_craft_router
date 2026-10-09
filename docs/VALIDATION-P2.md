# 1.1.0 追加开发与验收

日期：2026-10-08。依据 `plan-2.md` 的功能需求修复 Curios AE2 终端断连并新增材料预检。原规划文档保留不变。第一轮记录见 `VALIDATION.md`。

## 交付

- 发布文件：`build/jei_portable_craft_router-1.1.0.jar`，64,376 字节。
- SHA-256：`997c0f27fa027d6bb7f59c143e2c4fe505f41acc0de0f1e3717217129387498c`。
- 31 个 Java 文件，`javac --release 21` 零错误、4 条原有弃用警告。仍使用本地依赖、`build.ps1` 和手工 ZIP 打包，无 Gradle 或依赖下载。
- 已安装到本机 ATM10 8.2 客户端 `mods`，安装脚本校验哈希，旧版本保存在 `build/install-backups`。正式服务端没有安装或重启；联机时两端应更新到 1.1.0。
- 所有测试均在项目下隔离目录/存档中运行，没有重启正式游戏或服务端。发布包不包含测试 probe，条目路径为正斜杠、class major 为 65。

## 复现与修复依据

使用 1.0.0 基线，把带合成模块的通用终端放入 Curios，令 `CURRENT_TERMINAL` 指向样板编码终端，从背包点击箱子配方的「+」，成功复现客户端断连：

```
java.lang.IllegalStateException: Couldn't find menu host at curios slot 0
for ae2wtlib:wireless_crafting_terminal on client. Closing menu.
at appeng.menu.implementations.MenuTypeBuilder.fromNetwork
```

证据：`validation/p2-baseline.log`。进一步为终端同时安装样板编码模块，形成有效的双模块终端，基线仍复现同一异常，见 `validation/p2-baseline-valid.log`。修复后的相同双模块场景能打开、填格、通过屏幕点击拿走箱子，并验证实际 Curios 物品扣电，见 `validation/p2-fixed-valid.log`。

规划书的 `forStack` 网络序列化描述与本机 AE2 19.2.17 不符。javap 确认 `MenuLocators` 只注册 block/part/inventory/curio 四种 codec，`StackItemLocator` 没有注册，不能直接发入菜单打开包。此外仅在副本上使用 `forStack` 会让服务端 host 的扣电、升级与合成格修改落在副本中。

最终实现为已注册的 `SnapshotItemLocator`：客户端读取打开包中的完整物品快照，服务端通过原 `ItemReference` 读取真实槽位；保留背包槽位锁定信息。通用终端先复制、切到已经安装的合成组件、写回原槽位，再构造快照。这样同时解决 Curios 能力读取与菜单类型组件同步竞态，且不改变真实物品状态保存语义。

F1–F5 加固包括服务端重读非空物品与适配器匹配、过滤不安全候选及无合成模块的通用终端、打开异常转为失败原因、菜单编号/类型/屏幕三重确认，以及收到成功但菜单 3 秒未建立时的降级。关闭界面、切换世界或超时不会重复套用。三处 JEI 注入结构及 remap=false 保留。

原请求和结果 payload 字段、原协议版本不变；新增空的可选 `snapshot_support` 通道，仅以网络握手判断双方是否支持快照定位器。新客户端在旧服务端禁用路由，新服务端拒绝向旧客户端发送新的 AE2 定位器。混合版本的这些保护路径由通道条件保证，本轮没有单独进行 1.0/1.1 联机矩阵。

## 材料判定

AE2 使用 JEI 注册阶段提供的 `IRecipeTransferHandlerHelper#getGuiSlotIndexToIngredientMap` 和原 `CraftingTermMenu#findMissingIngredients`，WCTMenu 按父类处理。遵循规划默认口径：背包与网络均为来源，可自动合成槽位不计为缺料。

Integrated Terminals 的原 dry-run 使用跨菜单配方错误缓存。实际回归中出现上一菜单材料充足、下一菜单空库存却仍返回充足的问题，故改用其公开 `RecipeTransferHelpers#getMissingItemsUncached`，配合原 `RecipeInputSlotJei` 和 `JEIIntegratedTerminalsConfig` 匹配条件。这仍是复用原检查，不访问私有方法、不实现物品搬运。

工作台使用 `RecipeTransferUtil#getRecipeTransferOperations` 的缺料结果。`NOT_ENOUGH` 和 `UNKNOWN` 都停止自动套用并沿用原 `router.apply_failed` 中文；处理器缺失或 JEI 拒绝另用 `router.jei_cannot_apply`。网络终端打开前没有库存快照，预览只说明打开后检查；工作台可显示充足/缺少槽位数，缺料图标改为屏障，按钮仍可打开工作台。额外三条双语资源仅用于材料预览状态，不增加另一套缺料失败文案。

## 验收结果

在隔离核心依赖环境分组运行 0–41 场景，并完成双客户端联机、无 JEI 服务端和版本保护测试。以下表内「通过」均有真实运行证据；完整整合包兼容性和人工视觉检查范围见文末。

| plan-2 项目 | 结果 | 场景/证据 |
|---|---|---|
| A1 Curios 通用终端 | 通过：已选中其他终端类型也能安全打开合成菜单 | 15，`p2-singleplayer.log`、`p2-fixed-valid.log` |
| A2 Curios 无线合成终端 | 通过 | 10、19，`p2-singleplayer.log`、`p2-matrix-materials.log` |
| A3 背包两种终端 | 通过 | 6、7、16、22，同上 |
| A4 未链接 | 通过：两种终端、两种位置均提示并降级 | 17、20、23、26，`p2-matrix-materials.log` |
| A5 无电 | 通过：两种终端、两种位置均提示并降级 | 18、21、24、27，同上 |
| A6 通用终端切换合成类型 | 通过：组件在打开包快照中一致 | 15，`p2-fixed-valid.log` |
| A7 屏幕与成品取出 | 通过：屏幕建立，真实鼠标点击产物槽后服务端 carried 为箱子；实际装备物品扣电 | 15，同上 |
| A8 背包和 Curios 同时携带 | 通过：背包无线合成优先于 Curios 通用终端；无合成模块的通用终端被过滤 | 36、37，`p2-extra.log` |
| B1 AE 网络充足 | 通过：背包木板清空，ME 元件中 64 木板，真实填格和箱子输出 | 14，`p2-singleplayer.log` |
| B2 AE 网络不足 | 通过：网络 0 或 7 木板，门槛拦截，服务端合成格与输出不变 | 28、29，`p2-matrix-materials.log` |
| B3 背包材料充足 | 通过：空网络、背包 64 木板可用 | 6、7，`p2-singleplayer.log` |
| B4 零库存可合成 | 通过：向真实 AE crafting service 注册处理样板，客户端缺料判定为 craftable、非 missing，预检 ENOUGH | 34，`p2-matrix-materials.log` |
| B5 Integrated Terminals | 通过：标签切换、背包取料与不足；仅网络真实箱子 64 木板可填格，7/0 木板拦截 | 11、12、32、33；39–41，`p2-integrated-network.log` |
| B6 工作台不足 | 通过：0/7 木板，缺 8/1 槽位，实际套用未执行 | 30、31，`p2-matrix-materials.log` |
| B7 JEI 无支持 | 通过：使用未注册菜单类型，返回单独的集成支持提示键 | 35，同上 |
| B8 总开关关闭 | 通过：原禁用行为保留 | 5，`p2-singleplayer.log` |
| C1 无终端 | 通过：原禁用路径保留 | 0，同上 |
| C2 服务端无 JEI | 通过：保持禁用，显示服务端支持提示，不发送路由请求 | `p2-server-no-jei-client.log`、`p2-server-no-jei-server.log` |
| C3 普通 2×2 | 通过：仍可转移 | 1，`p2-singleplayer.log` |
| C4 检查和 probe | 通过：原 ASM 签名变更与请求 codec 往返检查、双语资源校验、各运行分组通过 | `p2-checks.log` 与上述运行日志 |
| UNKNOWN 保护 | 通过：非合成配方得到 UNKNOWN，RecipeApplier 拒绝实际套用 | 38，`p2-extra.log` |
| 专用服务端双客户端 | 通过：RouterOne、RouterTwo 的 Curios 通用终端均打开和填格，两个不同 UUID 收到箱子同步；缺料组两名玩家均保留空合成格 | `p2-dedicated-server.log`、`p2-remote-one.log`、`p2-remote-two.log`；`p2-dedicated-empty.log`、`p2-empty-one.log`、`p2-empty-two.log` |
| JEI 版本保护 | 通过：隔离副本标记 19.58.0.0 后仍进世界，路由禁用、日志警告 | `p2-version-guard.log` |

表中日志均位于 `docs/validation`。12 种组合为 16–27；另外仍验证超范围降级、最大套数等第一轮路径。

B4 注册的是隔离 probe 提供的真实 processing pattern，未装 CPU 或输入原木，因此验证的是「可合成不会被预检误判为缺料」，没有声称实际自动合成任务完成。IT 网络夹具通过公开网络位置 API 连接真实箱子并绑定全向连接器组，不使用虚构库存。预检读取客户端同步快照，最后的物品变动仍由原 handler 与服务端裁决。

## 里程碑记录

| 阶段 | 改动文件 | 编译与运行证据/剩余项 |
|---|---|---|
| P2-M0 | 扩展 `tests/probe` 的通用终端复现、保留基线 JAR | 基线成功复现并留完整堆栈 |
| P2-M1 | `SnapshotItemLocator`、两个 AE2 Adapter、`ItemReference`、`TerminalAdapter`、`TerminalPriorityResolver`、`OpenTerminalHandler`、`ClientRouter`、入口与握手通道 | 编译通过，双模块终端修复/取出/扣电通过；为真实 API 修正规划中的 forStack 假设 |
| P2-M2 | probe 场景 16–27 | 12 种组合均通过 |
| P2-M3 | `RouterJeiPlugin`、`MaterialGate`、`Ae2MaterialGate`、`RecipeSnapshot`、`RecipeApplier` | 网络/背包/缺料/可合成门槛通过 |
| P2-M4 | `IntegratedMaterialGate`、`VanillaMaterialGate` | IT 无缓存检查和工作台原算法复用；网络实际库存验证通过 |
| P2-M5 | 原三处 JEI 注入的内部逻辑、中英资源、`scripts/check.ps1` | 工作台预览、单独集成提示、开关和 UNKNOWN 验证通过；网络预览不伪造库存结论 |
| P2-M6 | `ServerProbe`、客户端 probe、两个 smoke 脚本、`version-smoke.ps1` | 两个远端玩家成功路径与缺料路径均通过；测试服务器已退出 |
| P2-M7 | 版本元数据、构建/安装脚本中的成品名、README、本文与验收日志 | 1.1.0 发布并安装，旧版本备份，哈希和 JAR 格式检查通过 |

所有里程碑最终统一构建结果见 `validation/p2-build.log`。Java 文件路径以 `src/main/java/compat/jeirouter` 为根。

## 复现命令与范围

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-probe.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -WithProbe -StartScenario 15 -EndScenario 15 -Session p2-recheck
```

使用 `-StartScenario 0 -EndScenario 41` 可依次运行整个单人矩阵；联机复现见 README。基线复现可用 `-RouterJar 'E:\DfyWork\atm10-jei-routing\build\baseline-1.0.0.jar'` 配合场景 15，只在独立测试目录使用。

仍未验证完整 ATM10 494 个模组同时加载的冲突，未逐字做 tooltip 截图比对，未对任意真实未来 JEI 二进制承诺兼容。屏幕建立与产物点击已有运行断言，完整视觉布局仍可在正式实例重启后人工复核。1.1.0 对旧版本的自动握手保护不替代两端更新要求。
