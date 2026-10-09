# 1.3.1 ATM10 8.1 兼容修复验收

日期：2026-10-08。用户报告本机 `ATM10 8.1`（NeoForge 21.1.249）安装 1.3.0 后功能未生效。原 PLAN.md、plan-2.md 保留不变。

## 原因

指定实例已正确安装 1.3.0，配置为 enabled=true。实际 JEI 为 19.50.0.414，而此前 mixin 和客户端事件两处版本检查只接受 19.57.0.446。正式实例日志明确记录：

```text
JEI 19.57.0.446 not present; routing mixin disabled
JEI 19.57.0.446 unavailable; client routing disabled
```

提取记录见 [p5-original-disable.log](validation/p5-original-disable.log)。此次原因是 JEI 版本差异。此前 1.3.0 的 NeoForge 21.1.249 验证使用了 8.2 的 JEI，不能覆盖 8.1 的 JEI。

本机字节码核对还确认旧版没有 RecipeTransferService、PacketRecipeTransferWithResult 和 RecipesGui.getParentContainerScreen，且按钮控制器与布局工厂的参数不同。因此只放开版本限制不足以完成兼容。

## 修复

- 两处检查使用同一版本白名单：19.50.0.414 与 19.57.0.446。其他版本继续关闭功能。
- 按实际版本选择旧版或新版控制器/布局 mixin，注入前继续验证所需字段和方法签名。
- JeiTransferService 仅调用相应 JEI 的原处理器：旧版 RecipeTransferUtil 接收菜单，新版 RecipeTransferService 接收屏幕。没有新增物品搬运算法。
- 握手检查分别使用旧 PacketRecipeTransfer 或新 PacketRecipeTransferWithResult 通道，保留服务端能力要求。
- 样板按钮使用两版都有的 getParentScreen API；编码菜单上隐藏新增按钮、保留原生按钮。
- 隔离客户端脚本新增 InstanceDir 与 PrepareOnly，使用对应实例的 JSON/JAR/natives；重用目录时清理脚本管理的旧依赖。版本保护测试仅准备模板，不再为了取得参数启动后立即终止模板进程。
- 连续场景夹具每次按真实流程关闭 JEI，清除父界面和历史，避免前一编码菜单影响下一背包场景。

## 交付

- 成品：`build/jei_portable_craft_router-1.3.1.jar`，86,671 字节，43 个 Java 源文件。
- SHA-256：`BA753E6782DC12A68490CDC1EA090E367C0AC2D360708787DBBD3450DD710ECF`。
- 最终构建使用 ATM10 8.1 的本机依赖、NeoForge 21.1.249，javac --release 21 零错误、4 条原有弃用警告。编译记录：[p5-build-81.log](validation/p5-build-81.log)。
- 最终 JAR 同时用于 8.1 与 8.2 的隔离运行，两个测试副本的 SHA-256 与成品一致。
- 已安装到 `E:\BaiduNetdiskDownload\PCL 正式版 2.13.0.1\.minecraft\versions\ATM10 8.1\mods`，目录中仅保留一个本模组 JAR；安装后哈希一致。
- 旧 1.3.0 已备份到 `build/install-backups/20261008-212635-197`。安装记录：[p5-install-81.log](validation/p5-install-81.log)。正式游戏/服务端未启动、停止或重启，服务端未安装本轮更新。

## 测试结果

每个实例均提取其实际的 13 个核心依赖模组，在独立世界中使用离线身份测试。运行使用本机 Java 26，编译目标为 Java 21。

| 测试 | 结果 | 日志 |
|---|---|---|
| ATM10 8.1 / JEI 19.50.0.414 / NeoForge 21.1.249 | 场景 0–52 共 53 项全部通过 | [p5-atm81-final.log](validation/p5-atm81-final.log) |
| ATM10 8.2 / JEI 19.57.0.446 / NeoForge 21.1.249 | 同一 JAR，场景 0–52 共 53 项全部通过 | [p5-atm82.log](validation/p5-atm82.log) |
| 旧版 JEI 字段/签名破坏与网络编解码 | 通过，错误适配器和不兼容签名被拒绝 | [p5-checks-81.log](validation/p5-checks-81.log) |
| 新版 JEI 字段/签名破坏与网络编解码 | 通过 | [p5-checks-82.log](validation/p5-checks-82.log) |
| 未支持版本保护 | 隔离 JEI 元数据改为 19.58.0.0 后，正常进世界，按钮禁用 | [p5-version-guard.log](validation/p5-version-guard.log) |

完整场景覆盖工作台、无线合成与通用终端、Integrated Terminals、Curios、真实扣电与成品取出、网络库存、缺料阻止、优先级与失败降级。场景 34 验证蓝色指定文案和原生蓝色 renderer；50 验证保存 Ctrl 意图后进入客户端/服务端原生合成确认菜单。42–46、49、51、52 验证样板模式、虚拟槽位、真实鼠标点击、空白样板图标和原生按钮保留。两个日志均以 ALL SCENARIOS COMPLETE 结束。

8.1 实际界面截图：

![蓝色自动合成提示](validation/p5-atm81-blue-tooltip.png)

![空白样板按钮](validation/p5-atm81-pattern-button.png)

## 复现

```powershell
$instance = 'E:\BaiduNetdiskDownload\PCL 正式版 2.13.0.1\.minecraft\versions\ATM10 8.1'
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1 -InstanceDir $instance -NeoForgeVersion 21.1.249
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-probe.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -InstanceDir $instance -WithProbe -StartScenario 0 -EndScenario 52 -Session atm81-recheck -Language zh_cn -NeoForgeVersion 21.1.249
```

## 验证范围

本轮没有完整启动正式 8.1 实例的全部 491 个 JAR，也没有重跑专用服务端联机矩阵；完整整合包冲突仍需正式实例重启后复核。自动合成验证到原生任务确认与服务端处理，实际任务完成仍取决于玩家网络的 CPU、样板与供料。版本保护测试使用隔离元数据和 ASM 签名破坏，没有承诺任意未来 JEI 二进制兼容。
