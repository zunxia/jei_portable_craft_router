# JEI Portable Craft Router 1.3.5

Minecraft **1.21.1 / NeoForge** 的 JEI 便携终端扩展。携带终端查看配方时，可以直接打开合适的终端填入合成材料，或跳转到样板编码界面编写配方。

[下载 1.3.5](https://github.com/zunxia/jei_portable_craft_router/releases/tag/v1.3.5) · [问题反馈](https://github.com/zunxia/jei_portable_craft_router/issues) · [MIT 许可证](LICENSE)

## 功能

- **便携合成跳转**：在玩家背包中查看放不进 2×2 合成格的配方，点击 JEI「+」，自动打开携带的可用合成终端。
- **材料预检**：区分现货充足、缺少材料和 ME 网络可自动合成的材料，沿用终端与 JEI 的转移逻辑。
- **ME 自动合成**：材料可由网络合成时，显示蓝色提示「CTRL+左键让ME网络自动合成缺失物品」。Ctrl 点击进入 AE2 原生合成确认流程，仍需有效样板、CPU 和输入材料。
- **编写样板**：携带无线样板编码终端，或安装了样板编码模块的无线通用终端时，在转移按钮下方显示空白样板图标。支持合成配方和 AE2 JEI 能处理的加工配方；填入虚拟槽位时不消耗原料，编码成品仍需要空白样板。
- **原生按钮去重**：已打开样板编码界面时隐藏附加样板按钮，使用 AE2 原生按钮。
- **背包与饰品支持**：检查背包、快捷栏、副手和已装备的 Curios 槽位。候选终端无法打开时提示原因，并继续尝试下一项。
- **通用终端状态同步**：再次打开菜单时发送服务端原槽位物品的最新状态，避免样板与合成模式切换时复用过期快照。保留真实扣电和物品校验。

默认合成终端优先级：

1. AE2 无线合成终端。
2. AE2WTLib 无线通用终端，必须安装合成模块。
3. Integrated Terminals 便携式存储终端，必须有可用的合成标签页及其集成依赖。
4. Crafting On A Stick 便携式工作台。

## 安装与使用

1. 从 [Release](https://github.com/zunxia/jei_portable_craft_router/releases/tag/v1.3.5) 下载 `jei_portable_craft_router-1.3.5.jar`。
2. 退出游戏，移除 `mods` 中已有的本模组 JAR，再放入新文件。同一实例只保留一份；自行改过文件名的 JAR 也需要检查。
3. 安装 JEI，以及需要使用的终端模组和它们的依赖。AE2 配方填入需要 **AE2 JEI Integration**。
4. 重启游戏。携带已链接、已充电、在有效范围内的终端，在 JEI 中查看配方并点击对应按钮。

**单人游戏**只需安装到自己的客户端。**局域网联机**需要房主和加入者都安装同一份 1.3.5，并重启游戏后重新开放存档。**独立服务器**需要服务器及各客户端都安装同一版本。

测试用的 `router_probe.jar` 不用于正式游戏。安装包不会附带其他模组。

## 兼容范围

| 组件 | 说明 |
|---|---|
| Minecraft | 1.21.1 |
| Java | 游戏运行推荐 Java 21 |
| NeoForge | 本模组声明范围 `[21.1.169,21.2)`；1.3.5 游戏回归覆盖 21.1.249、21.1.251 |
| JEI | 19.27.0.335–19.57.0.451 内全部 83 个已发布的 NeoForge 构件通过接口检查 |
| AE2 / AE2WTLib / AE2 JEI Integration | 验证组合为 19.2.17 / 19.5.1 / 1.2.1 |
| Integrated Terminals | 本项目所用接口在 1.6.25–1.10.0 的已检查构件中可解析；具体依赖必须匹配 |
| Crafting On A Stick / Curios | 验证组合为 1.21.0.6 / 9.5.1+1.21.1 |

JEI 按实际接口选择适配器，不使用版本号白名单。上述 JEI 范围是接口检查通过的范围，不代表每个构件都完成了整合包运行测试。JEI 19.25.1.334 及更早版本的按钮架构未实现；REI、EMI、RS 终端和纯存储 AE2 终端不在支持范围内。

实际 NeoForge 下限还需满足 JEI 与各终端模组的依赖。例如 JEI 19.42.0.379 及之后的已检查构件要求 NeoForge 21.1.238 或更新版本，不能仅根据本模组的依赖范围选择加载器。

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

`priority` 按从高到低排列；省略某个适配器即可禁用它。客户端配置决定候选显示，房主或服务器上的配置与真实物品校验决定是否允许打开。联机时建议两端配置一致。

## 验证与已知问题

1.3.5 已通过 NeoForge 21.1.251 / JEI 19.57.0.446 的 **55 个隔离游戏场景**，覆盖终端跳转、材料预检、自动合成、样板编写，以及背包和 Curios 中连续三轮「样板→合成→样板」切换。NeoForge 21.1.249 / JEI 19.50.0.414 的连续切换场景也已通过；另完成 83 个 JEI 构件及 32 套依赖组合的接口检查。详细记录见 [验证报告](docs/VALIDATION-P10.md)。

- 局域网联机仍有通用终端在切换合成、样板访问模式时菜单宿主不匹配的反馈。现有客户端日志不足以确认房主版本或完整触发条件，该场景尚未完成验证；遇到时可先关闭界面，再重新打开需要的模式，并提供房主、加入者双方同一时间的日志。
- 跨终端操作需要服务端打开和同步菜单，当前还包含约 5 个客户端 tick 的固定缓冲，体感不会与已打开容器上的原生 JEI 转移完全相同。
- 材料不足或原处理器拒绝时会保留已打开的终端并提示，不会凭空生成材料或绕过网络权限。
- 核心依赖回归与接口审计不等同于完整整合包冲突测试，也不等同于所有联机模式已验证。

## 构建

项目提供 VS Code Java 设置和默认构建任务，可按 `Ctrl+Shift+B` 构建。也可以在安装 JDK 并将其 `bin` 加入 PATH 后执行：

```powershell
./build.ps1 -InstanceDir '你的 Minecraft 1.21.1 实例目录' -NeoForgeVersion 21.1.249
./scripts/check.ps1
```

构建脚本读取本地实例的 Minecraft、NeoForge 和模组依赖，使用 `javac --release 21` 编译，无需 Gradle。默认构建流程不下载依赖；缺少对应运行库时需先准备完整实例。成品位于 `build/jei_portable_craft_router-1.3.5.jar`。

## 许可证

[MIT](LICENSE)。
