# 1.3.0 NeoForge 兼容性与样板按钮去重验收

> 本文保留 1.3.0 的历史验收结果。ATM10 8.1 的旧版 JEI 支持和当前 1.3.1 验收见 [VALIDATION-P5.md](VALIDATION-P5.md)。

日期：2026-10-08。需求来源为用户追加的 NeoForge 21.1.249 或更低版本兼容，以及编码界面只保留原生按钮。原 PLAN.md、plan-2.md 未修改。

## 交付

- 成品：`build/jei_portable_craft_router-1.3.0.jar`，78,836 字节，38 个 Java 源文件。
- SHA-256：`A14431F5A723EF21CF006FBEF47810CCE95EB34E604074A80743E8DBE200505C`。
- NeoForge 21.1.249 和 21.1.247 均以 `javac --release 21` 编译成功，零错误、4 条原有弃用警告。最终成品使用 21.1.249 编译。
- 成品中的 mods.toml、MANIFEST.MF 和运行日志版本统一为 1.3.0。测试 probe 不包含在发布 JAR 中。
- 已安装到本机 ATM10 8.2 客户端；mods 中只保留一个本模组 JAR，安装后与成品 SHA-256 一致。1.2.0 备份到 `build/install-backups/20261008-203213-606`。正式游戏和服务端未启动、停止或重启；正式服务端未安装本轮更新。

## 兼容范围

本模组 NeoForge 依赖范围由 `[21.1.251,21.2)` 调整为 `[21.1.169,21.2)`，不再把 21.1.249 和更低的兼容版本挡在加载阶段。

已直接检查本机依赖 JAR 的元数据：AE2 19.2.17 要求 NeoForge `[21.1.169,)`，JEI 19.57.0.446 要求 `[21.1.238,)`。因此使用当前固定 JEI 时不能低于 21.1.238；降低本模组的限制不会改变其他模组的要求。本轮实际验证版本为 21.1.249 与 21.1.247，未逐个测试 21.1.238–21.1.246。

`build.ps1` 新增 `-NeoForgeVersion`，默认 21.1.249，从本机已有库中选择对应 client/universal JAR。`smoke-client.ps1` 同样支持版本参数，并明确替换启动参数的 NeoForge 版本，避免编译使用旧版而运行仍使用实例默认的 21.1.251。专用服务端测试脚本沿用本机已有的 21.1.251 服务端运行库。

## 按钮行为

`PatternButtonController` 检查 JEI 父容器菜单是否为 AE2 `PatternEncodingTermMenu`。独立无线样板编码终端和通用终端的样板编码模式均使用此菜单。匹配时新增按钮不可见、不可用，模拟点击也被拒绝；AE2 JEI 原生转移/编写样板按钮继续显示。

在背包等其他界面中，携带有效编码终端时新增按钮仍可打开终端并交给原处理器填入虚拟配方。测试同时确认服务端真实编码菜单、8 个木板虚拟槽位，以及未消耗或制造配方物品。

## 验证结果

隔离客户端采用本机的 13 个核心依赖模组、独立世界和离线测试身份。实际运行使用本机 Java 26，编译目标为 Java 21。

| 项目 | 结果 | 证据 |
|---|---|---|
| NeoForge 21.1.249 编译 | 通过 | [p4-build.log](validation/p4-build.log) |
| NeoForge 21.1.247 编译 | 通过 | [p4-build-247.log](validation/p4-build-247.log) |
| 21.1.249 场景 42–45 | 全部通过：独立/通用编码终端 × 背包/Curios；原生按钮保留、新增按钮隐藏 | [p4-nf249.log](validation/p4-nf249.log) |
| 21.1.247 场景 42–45 | 同一最终 JAR 全部通过 | [p4-nf247.log](validation/p4-nf247.log) |
| JEI 签名保护、网络编解码、双语资源 | 通过 | [p4-checks.log](validation/p4-checks.log) |
| probe 编译 | 通过 | [p4-probe-build.log](validation/p4-probe-build.log) |
| 客户端安装、备份与哈希 | 通过 | [p4-install.log](validation/p4-install.log) |

两个最终运行日志均包含 `PROBE PASS: ALL SCENARIOS COMPLETE`，以及四次 `native JEI encoding transfer button remains visible` 和新增按钮隐藏断言。连续场景的测试夹具改为每次从背包实际打开 JEI 配方，以刷新父界面。

## 复现

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1 -NeoForgeVersion 21.1.249
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-probe.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -WithProbe -StartScenario 42 -EndScenario 45 -Session p4-nf249-recheck -Language zh_cn -NeoForgeVersion 21.1.249
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-client.ps1 -WithProbe -StartScenario 42 -EndScenario 45 -Session p4-nf247-recheck -Language zh_cn -NeoForgeVersion 21.1.247
```

未完成完整 ATM10 494 模组同时加载的冲突测试。本轮未重跑历史全部 0–52 场景，也未新增专用服务端联机矩阵；历史功能记录见 [VALIDATION-P3.md](VALIDATION-P3.md)。
