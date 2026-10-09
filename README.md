# JEI 便携合成终端跳转（ATM10 8.1 / 8.2）

实现已完成，模组版本 `1.3.2`。支持 ATM10 8.1 的 JEI 19.50.0.414 和 8.2 的 JEI 19.57.0.446，兼容 NeoForge 21.1.249。修复启动早期读取配置导致的崩溃。打开 AE2 样板编码界面后，只保留 AE2 原生编写样板按钮。保留 AE 可合成材料的蓝色提示、Ctrl 点击确认、Curios 安全打开、材料预检和便携合成跳转。

## 安装与使用

成品：`build/jei_portable_craft_router-1.3.2.jar`。

1. 将成品放入客户端和专用服务端的 `mods` 目录，两端均更新到 1.3.2；单人游戏只需安装到客户端。
2. 保留 JEI、所用终端模组及其正常依赖。AE2 自动填格还需要客户端的 AE2 JEI Integration。
3. 重启游戏/服务端。携带已链接、已充电的终端，打开背包，在 JEI 中查看 3×3 合成配方，点击「+」。

默认顺序：AE2 无线合成终端 → AE2WTLib 无线通用终端（必须安装合成模块）→ Integrated Terminals 便携式存储终端 → Crafting On A Stick 便携式工作台。扫描背包、快捷栏、副手和已装备的 Curios 槽位。未链接、没电、超范围等打开失败会显示原因并继续尝试下一个候选。没有候选或关闭总开关时保留 JEI 原行为。

Integrated Terminals 需要有效的 Integrated Dynamics 网络和可用的合成标签页；当前版本由 Integrated Tunnels 集成提供。模组会自动选中该标签页。如果标签页集成缺失，会提示原因并尝试下一个终端。

安装到本机已识别的 ATM10 实例：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install.ps1
```

安装到指定服务端：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install.ps1 -TargetMods 'E:\路径\服务器\mods'
```

安装脚本只替换本模组，旧版本备份到 `build/install-backups`，并校验 SHA-256。安装后需要重启；脚本不会重启进程。不要把测试用的 `router_probe.jar` 安装到正式实例。

## 适配版本

| 组件 | 已验证版本 |
|---|---|
| Minecraft / NeoForge | 1.21.1 / 21.1.249；1.3.0 另验证过 21.1.247 |
| JEI | 8.1：19.50.0.414；8.2：19.57.0.446，仅支持这两个已验证版本 |
| AE2 / AE2WTLib / AE2 JEI Integration | 19.2.17 / 19.5.1 / 1.2.1 |
| Integrated Terminals | 1.21.1-1.8.0-840 |
| Integrated Dynamics / Integrated Tunnels / Integrated Crafting | 8.1：1.35.0-1981 / 1.10.0-729 / 1.5.0-646；8.2：1.37.1-2160 / 1.12.0-792 / 1.7.2 |
| Crafting On A Stick / Curios | 1.21.0.6 / 9.5.1+1.21.1 |

JEI 未安装、版本不同或控制器签名不匹配时，关闭路由功能并记录警告，避免阻止游戏启动。依赖声明允许其他 JEI 版本加载，以满足软失败要求；这不代表支持其他版本的功能。

本模组 NeoForge 依赖范围为 `[21.1.169,21.2)`。这两个 JEI 版本自身均要求 NeoForge 21.1.238 或更新版本，因此搭配这套依赖使用时实际最低版本为 21.1.238；不承诺任意更早版本都能运行。

## VS Code 与离线构建

已配置 `.vscode/tasks.json` 和 Java 设置。在本项目的 VS Code 窗口按 `Ctrl+Shift+B` 构建，或执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1 -NeoForgeVersion 21.1.249
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check.ps1
```

可用 `build.ps1 -InstanceDir '实例目录'` 指定本地 ATM10 8.1 或 8.2 实例，或用 `-NeoForgeVersion` 选择已有的 NeoForge 库，默认 21.1.249。构建读取实例 version JSON、已存在的 Minecraft/NeoForge 库及模组 jar，暂存为 ASCII 路径后以 `javac --release 21` 编译、打包。无需 Gradle，也不下载依赖。当前构建为 43 个 Java 文件、零错误、4 条弃用警告；缺少的 27 个其他平台库变体可以跳过。

## 1.3.1 ATM10 8.1 修复

8.1 的 JEI 19.50.0.414 被此前的单版本检查禁用，导致模组虽加载却没有按钮和路由功能。现在两个版本分别选择匹配的按钮控制器和布局注入；旧版调用原 `RecipeTransferUtil` 与旧转移通道，新版调用原 `RecipeTransferService` 与带结果的转移通道。样板按钮读取两个版本都有的父界面 API。1.3.2 还修复了 NeoForge 配置尚未加载时屏幕事件提前读取配置、导致整合包启动崩溃的问题。验收记录见 [docs/VALIDATION-P5.md](docs/VALIDATION-P5.md)。

## 1.3.0 兼容性与样板按钮

已打开无线样板编码终端，或无线通用终端的样板编码模式时，JEI 的父菜单属于 AE2 `PatternEncodingTermMenu`。此时自定义样板按钮隐藏且不可点击，仅保留 AE2 JEI 的原生按钮。背包和其他界面中，携带有效编码终端时仍显示新增按钮。验收记录见 [docs/VALIDATION-P4.md](docs/VALIDATION-P4.md)。

## 1.2.0 自动合成与样板编写

终端材料预检区分现货与可自动合成材料。需要 ME 网络合成材料时，普通跳转点击不执行转移，而是在真实终端上重新打开当前 JEI 配方。悬停「+」保留原 AE2 JEI 的蓝色按钮与蓝色材料槽位，并显示蓝色文案：**CTRL+左键让ME网络自动合成缺失物品**。Ctrl 点击沿用 AE 原生合成确认界面；按 AE 的确认流程启动任务，仍需可用 CPU、样板和输入材料。在背包点击时按住 Ctrl 的意图会保存到菜单打开后，松开 Ctrl 也不会丢失。真实缺料仍按原预检阻止自动套用。

携带无线样板编码终端，或安装了样板编码模块的无线通用终端时，在 JEI 配方的转移按钮下方显示「编写样板」。使用原 JEI 按钮底框，覆盖缩放后的 AE2 空白样板图标，支持背包、副手和 Curios。点击后打开编码终端，交给 AE2 JEI 原处理器填入虚拟配方；合成配方使用合成模式，熔炼等其他配方使用处理模式，不要求持有配方原料。随后在终端中使用空白样板完成编码。从 1.3.0 起，已打开编码菜单时隐藏新增按钮并使用原生按钮；其他容器菜单可以安全切换。没有编码模块或关闭配置时隐藏按钮，未链接等打开失败继续下一候选。

新增 `wirelessPatternEncoding = true` 配置。新增可选 `pattern_support` 握手标记；旧服务端上按钮不可点击并提示升级，原打开请求/结果字段与协议版本保持不变。完整验收和中文界面截图见 [docs/VALIDATION-P3.md](docs/VALIDATION-P3.md)。原始规划文件保留不变。


## 1.1.0 修复与材料预检

- AE2 终端使用已注册的快照定位器：客户端收到带有菜单类型组件的物品快照，服务端持续定位原槽位真实物品，保留真实扣电、合成格和模块状态。通用终端先确认合成模块，再切到合成类型并生成快照。不会依赖客户端 Curios capability 的同一索引或组件同步时序。
- AE2 复用 `findMissingIngredients` 与 JEI 注册时提供的槽位映射 helper。背包和绑定网络库存都可用；可自动合成槽位在 1.2.0 返回独立的 CRAFTABLE 状态，需要 Ctrl 点击确认，最终行为由 AE2 和服务端决定。
- Integrated Terminals 复用其公开的无缓存检查，包含合成格、背包和同步网络材料，使用原 JEI 匹配条件。这样避免上一菜单的缓存结果把缺料误判为充足。
- 工作台使用 JEI 原有匹配算法。缺料和无法判断均不调用实际套用，保留终端并沿用 `router.apply_failed` 提示；缺少处理器或 JEI 拒绝则显示单独的集成支持提示。
- 悬停时工作台可预览「充足」或「缺少 N 个配方槽位」，缺料图标为屏障，按钮仍可打开工作台。网络终端尚未打开时没有绑定库存快照，因此显示「打开后检查」，不会用背包库存冒充网络结论。
- 菜单打开成功后等待最多 3 秒确认菜单编号、类型及屏幕；未建立且仍在背包界面时关闭旧请求菜单并继续下一个候选。关闭界面或切换世界取消请求。

原打开请求/结果的字段与协议版本保持不变，1.1 新增可选的 `snapshot_support` 握手通道确认新定位器能力。新客户端连接没有该能力的旧服务端会禁用跳转；新服务端拒绝给旧客户端发送新 AE2 定位器，使旧客户端可以降级。当前新增的样板编写能力还要求两端更新到 1.2.0。

本轮复现和验收表见 [docs/VALIDATION-P2.md](docs/VALIDATION-P2.md)。可使用 `-StartScenario` / `-EndScenario` 单独复现：0–14 为基础回归，15 为原断连及成品取出，16–27 为 12 种 AE2 状态组合，28–35 为材料与处理器检查，36–38 为优先级、安全过滤及 UNKNOWN，39–41 为 IT 仅网络取料与不足。

联机新功能测试在服务端、两个客户端的启动命令都追加 `-Ae2Curios`；缺料组还需都追加 `-EmptyMaterials`。版本保护仍使用隔离元数据变更测试。

## 配置

首次运行生成 `config/jei_portable_craft_router-common.toml`：

```toml
enabled = true
ae2WirelessCrafting = true
ae2Universal = true
integratedPortable = true
craftingOnAStick = true
wirelessPatternEncoding = true
priority = ["ae2_wireless", "ae2_universal", "integrated", "stick"]
debugLogging = false
```

`priority` 从高到低排序，省略某个适配器即禁用它。客户端配置控制候选显示，服务端配置与物品校验决定是否允许打开。两端配置建议一致。

## 实现与验证

只接管玩家背包中合成配方的原始「配方太大」错误。服务端重新读取物品、验证当前菜单、配置、链接、电量、范围，并调用终端原有 opener。客户端确认请求编号、菜单编号、菜单类型与屏幕同步后，通过兼容层调用相应版本 JEI 的原转移处理器；AE2 和 Integrated Terminals 使用各自填格通道，工作台使用 JEI 自己的转移包。没有独立的物品搬运算法。等待超时、关闭界面、切换世界会取消请求。

AE2WTLib 会替换 AE2 无线合成终端的实际物品实现，因此按 `ItemWT` 的当前菜单类型打开。通用终端检查合成模块并切换到合成类型；不会凭空添加模块。Integrated Terminals 同时在服务端和客户端确保合成标签页。

当前验收记录见 [docs/VALIDATION-P5.md](docs/VALIDATION-P5.md)，历史记录见 [docs/VALIDATION.md](docs/VALIDATION.md)、[docs/VALIDATION-P2.md](docs/VALIDATION-P2.md)、[docs/VALIDATION-P3.md](docs/VALIDATION-P3.md) 与 [docs/VALIDATION-P4.md](docs/VALIDATION-P4.md)，原始日志在 `docs/validation`。测试用独立目录、世界和离线身份进行。

复现测试：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-probe.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -WithProbe -Session local-check
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-server.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -WithProbe -Session remote-one -PlayerName RouterOne -Remote 127.0.0.1:25576
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -WithProbe -Session remote-two -PlayerName RouterTwo -Remote 127.0.0.1:25576
```

1.2.0 的单人回归覆盖 0–52；`-StartScenario 42 -EndScenario 52` 单独运行新增矩阵，`-Language zh_cn` 验证中文界面。专用服务端样板测试在服务端和两个客户端命令都追加 `-Pattern`。缺少 JEI 的服务端测试用 `smoke-server.ps1 -WithoutJei`，配合客户端 `-UnsupportedCheck`。版本保护测试用 `scripts/version-smoke.ps1`。每次测试前应确认上一个测试服务端已退出，以免占用 25576 端口。启动脚本不下载文件，但被启动的第三方模组可能执行自身的更新检查。

隔离客户端脚本可通过 `-InstanceDir '实例目录'` 选择 8.1 或 8.2，自动使用对应的 JSON、JAR、natives 和依赖模组；`-PrepareOnly` 仅准备目录与启动参数。未指定实例时使用上次构建记录的实例。重复使用同一测试目录时会清理脚本管理的旧依赖，避免两个 JEI 版本同时存在。

## 已知限制

- 面向上述固定版本；不支持 REI/EMI、RS 终端、纯存储 AE2 终端。物品合成跳转仅支持合成配方，样板编写可使用 AE2 JEI 支持的其他配方。
- 材料不足或原处理器拒绝转移时保留已打开的终端并提示，可在终端中补料后重新点击 JEI「+」。
- 自动测试使用从本机 ATM10 提取的 13 个核心依赖模组。尚未完成完整整合包所有模组同时运行的冲突测试。
- 版本保护启动测试修改的是隔离副本的 JEI 版本元数据，并结合 ASM 方法/字段变更测试；未声称验证任意未来 JEI 二进制。

原需求文档 [PLAN.md](PLAN.md) 保留不变。许可证：MIT。
