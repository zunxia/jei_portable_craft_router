# 1.2.0 自动合成提示与样板编写验收

> 本文保留 1.2.0 的历史验收结果。当前 1.3.0 的 NeoForge 兼容性和样板按钮去重见 [VALIDATION-P4.md](VALIDATION-P4.md)。

日期：2026-10-08。需求来源为用户本轮追加的两项功能；原 PLAN.md、plan-2.md 保留不变。

## 交付

- 发布文件：`build/jei_portable_craft_router-1.2.0.jar`，78,669 字节，38 个 Java 文件。
- SHA-256：`606e5086a98652c3e6680fa21c04a63453624227a0d1cbb455d5c77b802f079d`。
- `javac --release 21` 零错误、4 条原有弃用警告；本机离线依赖与构建方式不变。
- 客户端安装由 `scripts/install.ps1` 备份旧版本并校验哈希，重启后生效。正式服务端未安装或重启；联机两端需更新到 1.2.0。

## 最终行为

AE 的 `findMissingIngredients` 结果保留原库存语义，同时区分 ENOUGH、CRAFTABLE、NOT_ENOUGH、UNKNOWN。可合成材料需要明确 Ctrl 点击；普通路由打开终端后在该菜单上重新显示原 JEI 配方，原 AE2 JEI ErrorRenderer 继续渲染蓝色按钮与可合成槽位。悬停文案为蓝色的「CTRL+左键让ME网络自动合成缺失物品」。真正无法取得或合成的槽位仍使用原生红色提示。

路由保存输入事件的 Ctrl 修饰键。确认后先检查实际注册的 AE2 JEI 处理器与其 dry-run，再调用该处理器原本使用的公开 `CraftingHelper.performTransfer`，显式传入已保存的意图。测试在 Ctrl 实际未按住的延迟阶段验证服务端收到原生 craft-missing 操作，进入 `CraftConfirmMenu`。这沿用 AE 的确认与 CPU 调度流程，不绕过材料、权限或任务确认。

样板按钮复用 JEI IconButton 底框、点击和 tooltip。JEI 默认从下向上排按钮，因此新增布局注入为转移和样板按钮调整两行位置，避免样板按钮跑到「+」上方。图标使用 `ae2:blank_pattern` 的原物品渲染，缩放到按钮边框内。没有编码终端/模块或关闭 `wirelessPatternEncoding` 时隐藏按钮。

独立无线样板编码终端和带 PATTERN_ENCODING 模块的通用终端，扫描背包与 Curios，服务端重读原槽位后用已注册快照定位器安全打开。通用终端在构造定位器前切到已安装的编码组件。原开启请求/结果字段与协议版本保留，新增可选 `pattern_support` 握手，旧服务端上禁用新按钮并提示升级。

原 AE2 JEI EncodePatternTransferHandler 填入虚拟配方：合成模式与处理模式由原集成选择，不要求持有配方材料。按钮负责打开和填入；玩家仍在终端中用空白样板完成编码，不自动消耗空白样板或领取编码产物。已经在编码菜单时直接复用，不重新打开；其他容器来源受当前菜单编号校验。

## 运行证据

测试环境为从本机 ATM10 提取的 13 个核心依赖模组，使用隔离世界、测试身份与进程。

| 范围 | 结果 | 日志 |
|---|---|---|
| 原功能 0–15 | 全部通过，包括 Curios 成品取出、真实扣电、优先级、失败降级、最大套数 | `validation/p3-regression-base.log` |
| 原矩阵 16–41 | 全部通过，12 种终端状态、网络取料/不足、UNKNOWN；34 改为要求 Ctrl 确认 | `validation/p3-regression-materials.log` |
| 独立编码终端 × 背包/Curios | 实际服务端编码菜单、8 个木板虚拟输入，无原料也可编写 | 42、43，`validation/p3-pattern-matrix.log` |
| 通用编码终端 × 背包/Curios | 从合成组件切换到编码组件，安全打开和填入 | 44、45，同上 |
| 熔炼配方 | 原集成选处理模式，原铁输入、铁锭输出正确 | 46，同上 |
| 无编码模块 / 配置关闭 | 按钮隐藏且不可点击 | 47、48，同上 |
| 已打开合成菜单切换编码 | 原合成格已真实填入后，同一 Curios 通用终端切换到编码，额外原料不消耗 | 49，同上 |
| Ctrl 点击路由 | 保存 Ctrl 意图，服务端和客户端都进入原生 CraftConfirmMenu | 50，`validation/p3-ctrl.log` 与矩阵日志 |
| 实际 JEI 按钮布局与点击 | 按钮在转移正下方，无重叠；准确使用空白样板图标；真实按下/释放后服务端填入 | 51，`validation/p3-pattern-ui.log` |
| 失败降级 | 未链接的独立编码终端失败后，转用 Curios 通用编码终端 | 52，`validation/p3-pattern-matrix.log` |
| 蓝色提示 | 原生 COSMETIC 蓝色 renderer、自定义蓝色中文提示；普通点击未改合成格，原配方重新显示 | 34，`validation/p3-blue.log` |
| 专用服务端 + 两个独立客户端 | 两个不同 UUID 从 Curios 通用终端转入编码菜单，服务端验证虚拟槽，客户端收到同步 | `validation/p3-pattern-server.log`、`p3-pattern-remote-one.log`、`p3-pattern-remote-two.log` |
| JEI 版本保护 | 隔离 JEI 元数据改为 19.58.0.0 后正常进世界，新旧两处 mixin 均禁用 | `validation/p3-version-guard.log` |
| ASM / 网络 / 双语资源 | 实际控制器与布局签名通过；删字段或改入口签名会禁用注入；原 payload 往返与新翻译键通过 | `validation/p3-checks.log` |

编译记录为 `validation/p3-build.log`。新项目 API 均在本机字节码中核对，不依赖文档中的接口假设。测试夹具等待真实网络建立后注册可合成样板，避免在网络尚未创建时误判失败。

## 中文界面

原生蓝色材料槽与指定文案：

![蓝色自动合成提示](validation/p3-blue-tooltip.png)

原按钮底框中的空白样板图标，位置在「+」下方：

![样板按钮](validation/p3-pattern-button.png)

截图来自运行中的真实 JEI 界面，测试渲染时将悬停坐标放在被测按钮上。布局位置、图标对应物品和原生蓝色 renderer 另有运行断言。

## 复现

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-probe.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -WithProbe -StartScenario 42 -EndScenario 52 -Session pattern-check -Language zh_cn
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-server.ps1 -Pattern
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -WithProbe -Pattern -Session pattern-one -PlayerName RouterOne -Remote 127.0.0.1:25576
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -WithProbe -Pattern -Session pattern-two -PlayerName RouterTwo -Remote 127.0.0.1:25576
```

上述命令不要同时运行另一个占用 25576 的测试服务端。日志和世界写在项目 build 子目录，测试 probe 不包含在发布 JAR 中。

## 范围限制

未完成完整 ATM10 494 模组同时加载的冲突验证。自动合成测试验证了原生确认界面和服务端操作，没有验证用户实际网络中的 CPU、供料、样板加工与最终任务完成；最终能力与原 AE 相同。未来真实 JEI 二进制兼容性未承诺，版本保护测试仍使用隔离元数据和 ASM 签名破坏检查。1.1/1.2 混合版本未另做联机矩阵。
