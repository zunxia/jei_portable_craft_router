# 交付与验收记录

> 本文记录第一轮 1.0.0。1.1.0 的 Curios 修复、材料预检见 [VALIDATION-P2.md](VALIDATION-P2.md)；1.2.0 的蓝色自动合成提示与样板按钮见 [VALIDATION-P3.md](VALIDATION-P3.md)；1.3.0 的 NeoForge 兼容性与按钮去重见 [VALIDATION-P4.md](VALIDATION-P4.md)；当前 1.3.1 的 ATM10 8.1 修复见 [VALIDATION-P5.md](VALIDATION-P5.md)。

日期：2026-10-08。成品：`build/jei_portable_craft_router-1.0.0.jar`，46,238 字节，24 个 Java 源文件，Java class 目标版本 21。需求来源为原始 `PLAN.md`；本文记录最终实现和证据，不修改规划书。

## 验证环境与范围

使用本机 ATM10 8.2 的 Minecraft 1.21.1、NeoForge 21.1.251、JEI 19.57.0.446 和 13 个相关核心依赖模组，在项目 `build` 下建立隔离客户端、专用服务端、存档和离线测试身份。专用服务端使用本机 `E:\DfyWork\ATM10-Server` 已有运行库，绑定 `127.0.0.1:25576`。测试身份没有读取正式玩家令牌；正式游戏和服务端没有被停止或重启。

测试 probe 实际调用 JEI 控制器及本模组网络流程，最终在服务端读取真实菜单槽位，断言 8 个橡木木板槽位和箱子输出；联机客户端还验证同步后的输出槽。probe 不在发布 JAR 中。运行时日志包含第三方模组的更新提示和缺少可选集成的警告，不能把这些视为本模组验证失败。

完整 494 模组共同运行的兼容性尚未验收；图标和提示的人工视觉复核也应在正式整合包重启后进行。核心逻辑测试不能替代这些检查。

## 验收清单

| PLAN 编号 | 结果与测试内容 | 证据 |
|---|---|---|
| A1 | 通过：已链接、充电的 AE2 无线合成终端与有合成模块的 AE2WTLib 通用终端，一次调用真实点击入口后填格并产生箱子 | `validation/singleplayer.log`，场景 6、7 |
| A2 | 通过：背包中的便携式工作台打开、填格；Curios 中的工作台同样成功 | 同上，场景 2、4 |
| A3 | 通过：已链接的 Integrated Terminals 便携式存储终端自动切换合成标签页并填格，背包和 Curios 都成功 | 同上，场景 11、12；`validation/integrated.log` |
| A3b | 通过：未链接的 Integrated Terminals 返回原因，降级到工作台 | `validation/singleplayer.log`，场景 3 |
| A4 优先级 | 通过：三类物品同时携带，候选列表以 AE2 开头；未链接的 AE2、IT 按顺序失败后工作台完成填格 | 同上，场景 3 |
| A4 无终端 | 通过：无终端时保留禁用按钮；普通 2×2 配方仍可转移；关闭总开关保留原行为 | 同上，场景 0、1、5 |
| A5 | 通过：AE2 未链接返回明确原因并降级 | 同上，场景 3 |
| A6 | 通过：已链接但没电的 AE2 返回电源不足并降级 | 同上，场景 8 |
| A7 | 通过：超出无线范围返回原因并降级 | 同上，场景 9 |
| A8 | 通过：服务端保留本模组但移除 JEI 及其 AE2 集成，客户端保持按钮禁用，输出缺少服务端支持的提示，`serverSupported()` 为 false | `validation/server-no-jei-client.log`、`validation/server-no-jei-server.log` |
| A9 | 通过：两个独立客户端 RouterOne 和 RouterTwo 同时连接专用服务端，两名不同 UUID 的玩家都完成工作台填格并收到箱子输出同步 | `validation/dedicated-server.log`、`validation/remote-one.log`、`validation/remote-two.log` |
| A10 | 保护路径通过：隔离副本的 JEI 元数据改为 19.58.0.0 后，游戏正常进入世界、路由禁用并记录警告；ASM 测试删除各 shadow 字段、改变各注入方法时均拒绝应用 | `validation/version-guard.log`、`validation/checks.log` |

A4 无终端场景验证保留原控制器禁用路径；原始 tooltip 没有替换，未另行做逐字截图比较。A9 联机覆盖工作台路由；AE2 和 IT 的成功路径由单人集成服务端实际校验。A10 没有使用真实未来版本的 JEI 二进制，不保证任意未来版本都可启动。

附加验证：场景 10 验证 Curios 中 AE2 无线终端；场景 13 验证保存的最大转移参数能够填入 8 套完整配方；场景 14 清空玩家木板，仅向 AE2 存储元件放入 64 块木板，填格和输出仍成功，证明已有 AE2 处理器确实从网络取料。后两项见 `validation/extra-single.log`。

`scripts/check.ps1` 使用实际 JEI 控制器字节码验证兼容条件，并对请求包的背包/Curios 标志及槽位 -1、0、40、4096 做编解码往返和完整消费检查。边界值由服务端再次校验，不意味着所有槽位合法。

## 里程碑与文件

所有源文件统一构建通过：`javac --release 21` 零错误，4 条弃用警告（事件订阅总线与 JEI 旧 tooltip API）。构建准备 123 个本地 jar，跳过 27 个缺失的平台变体。构建摘要见 `validation/build.log`。

| 阶段 | 最终文件清单（目录包含该阶段相关实现） | 运行验证与剩余项 |
|---|---|---|
| M0 侦察 | `scripts/inspect.ps1`、`PLAN.md`；根据实际 API 完成终端适配 | javap、JAR 元数据和游戏运行核实；发现 AE2WT 实际替换物品、通用终端模块要求、IT 合成标签集成要求 |
| M1 骨架 | `build.ps1`、`scripts/package.ps1`、`JeiPortableRouter.java`、`META-INF/neoforge.mods.toml`、manifest、pack 元数据、mixin 配置、`.vscode` | 已实际加载；JAR 使用正斜杠条目，构建无需 Gradle/下载 |
| M2 探测 | `terminal/TerminalAdapter.java`、`TerminalPriorityResolver.java`、`ItemReference.java`、`CuriosAccess.java`、4 个终端 Adapter、`Ae2WtMenuCompat.java` | 背包与 Curios 候选/顺序断言通过 |
| M3 跳转 | `network/RouterNetwork.java`、`OpenTerminalRequestPayload.java`、`OpenTerminalResultPayload.java`、`OpenTerminalHandler.java` | 服务端重新验证物品与状态，真实菜单打开通过 |
| M4 套用 | `apply/RecipeSnapshot.java`、`RecipeApplier.java`、`IntegratedRecipeApplier.java`、`client/ClientRouter.java` | 确认菜单同步后调用已有 JEI handler；实际填格、输出、网络取料和最大套数通过 |
| M5 打磨 | `RouterConfig.java`、`client/ClientSetup.java`、`ClientEvents.java`、`ErrorTooltipCollector.java`、2 个 mixin Java 文件、中英语言资源 | 原路径/开关、降级原因、无 JEI 与版本保护通过；正式包视觉复核仍需重启游戏 |
| M6 多人 | `tests/probe`、`scripts/build-probe.ps1`、`smoke-client.ps1`、`smoke-server.ps1`、`tests/java/RouterChecks.java`、`scripts/check.ps1`、`version-smoke.ps1` | 两名不同玩家成功；本机测试服务端/客户端隔离运行 |
| M7 交付 | 成品 JAR、`README.md`、`LICENSE`、`scripts/install.ps1`、本文与 `validation` 日志 | 已提供安装、配置、版本限制和复现命令；完整整合包冲突测试与真实未来 JEI 二进制验证不在已完成范围 |

Java 路径均相对于 `src/main/java/compat/jeirouter`；资源路径相对于 `src/main/resources`。

## 关键实现差异

原规划书的一些接口推断需要按运行结果修正：AE2WTLib 的通用终端不是始终拥有合成能力，必须存在合成组件；AE2 无线合成终端的实际物品也可能被 AE2WTLib 替换，需要按 `ItemWT` 返回的菜单打开。IT 合成标签的 NAME 在缺少集成时可能为空，必须明确拒绝并降级。

自动套用统一复用 `RecipeTransferService`，由 AE2/IT 自己的 handler 选择其原生通道，工作台由 JEI 处理 counted/non-counted 与完整套数。没有照抄规划书中槽位算法，不自行实现搬运。JEI 依赖范围保持可加载，实际功能通过精确版本与 ASM 签名保护关闭，以满足升级时软失败要求。

## 手动复核方法

重启正式 ATM10 实例后，生存模式携带终端与橡木木板，在背包界面查看箱子配方，悬停「+」检查目标终端图标与中文提示，然后点击一次检查菜单、3×3 和输出。分别用背包和 Curios 测试三类终端，移除链接或电量后带工作台检查原因和降级；清空终端或关闭配置检查原始提示。专用服务端同样安装 JEI、本模组及使用的终端模组与依赖。

## 本机交付状态

已用安装脚本将发布 JAR 复制到 `E:\BaiduNetdiskDownload\PCL 正式版 2.13.0.1\.minecraft\versions\All the Mods 10 - 8.2\mods` 并核对 SHA-256：`e9505eadd764aac032367f262605c8a769099227cc9e37f87fd31a7c4005161d`。正式客户端需要重启才能加载新模组；正式服务端未安装或重启。测试用缺少 JEI 的隔离服务端已经停止，原有游戏和服务端进程保留。项目和 README 已在本机 Visual Studio Code 打开。所有 9 个 PowerShell 脚本已通过 Windows PowerShell 语法检查，中文脚本使用 UTF-8 BOM。
