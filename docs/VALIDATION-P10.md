# 1.3.5 通用终端模式切换修复

日期：2026-10-17。

## 故障与原因

用户日志 `D:\download\latest (4).log` 第 24571 行：通过 JEI 编写样板后，在通用终端左侧切换到合成终端，AE2 在客户端创建菜单时抛出 `Couldn't find menu host at SnapshotItemLocator ... for ae2wtlib:wireless_crafting_terminal`。原记录涉及 Curios 第 25 槽，AE2 19.2.17、AE2WTLib 19.5.1、JEI 19.57.0.446、NeoForge 21.1.251 和路由 1.3.4。日志显示菜单创建失败后断开连接、单人服务器保存并退出。

路由打开样板菜单时创建含样板模式组件的物品快照。AE2WTLib 的 `SelectTerminalPacket` 复用该菜单定位器，修改服务端真实物品的 `CURRENT_TERMINAL` 并通过 `WUTHandler.open` 打开新菜单。旧定位器的序列化始终发送首次打开的快照。因此服务端打开合成菜单，客户端根据旧快照创建样板宿主，宿主类型不匹配导致打开失败。原生的物品更新包会解码另一份定位器，它对自己快照的更新也不能更新随后打开菜单包里的旧快照。

## 修复

`SnapshotItemLocator` 保持已注册的类名、物品引用和网络字段顺序。服务端首次定位物品时记录所属玩家；后续每次编码定位器，重新从原槽位读取并校验物品，再发送当前完整组件。客户端仍使用该次数据包内的快照，避免依赖背包或 Curios 的异步同步顺序。

服务端仍在原槽位真实物品上扣电、保存状态；物品类型替换后返回空物品。客户端快照不能写回服务器。没有新增网络字段，也未取消原有物品校验。JEI 接口选择行为保持 1.3.4 的按接口匹配，不加入版本白名单。

## 回归方式

新增隔离场景 53、54：从 JEI 编写样板打开通用终端，分别放在背包与 Curios，通过侧栏使用的原生 `SelectTerminalPacket` 连续完成三轮“样板→合成→样板”。每次校验客户端和服务端菜单类型、客户端快照当前模式、服务端真实物品引用、编码/解码后的完整组件一致性。另验证同槽位替换成更新的同类物品后，不经事先定位也能编码最新组件，以及不同物品被拒绝。

旧版 1.3.4 使用同一探针在场景 53 第一次切换时复现相同 `Couldn't find menu host` 与断连。证明该缺陷也影响背包槽位。原始日志：`docs/validation/p10-before-latest.log`。

隔离测试使用 Java 21、独立目录与世界、离线身份；不操作用户正式实例和存档。

| 环境 | 测试 | 结果 |
|---|---|---|
| 旧版 1.3.4 / JEI 19.57.0.446 / NeoForge 21.1.251 | 场景 53 | 第一次切换复现原报错及断连 |
| 1.3.5 / JEI 19.50.0.414 / NeoForge 21.1.249 | 场景 53–54 | 两个场景通过，106 条 PROBE PASS |
| 1.3.5 / JEI 19.57.0.446 / NeoForge 21.1.251 | 场景 0–54 | 全部 55 个场景通过，480 条 PROBE PASS，无原故障报错 |

构建：42 个 Java 文件，零错误、4 条原有弃用警告。常规检查通过；83 个已发布 JEI 构件接口检查通过，32 套依赖组合类型/成员检查通过。静态依赖检查不等同于完整整合包运行验证；本轮不声称完成所有整合包或专用服务端双客户端回归。

## 复现命令

```powershell
$env:Path = 'E:\JAVA\bin;' + $env:Path
./build.ps1 -NeoForgeVersion 21.1.249
./scripts/check.ps1
./scripts/build-probe.ps1
./scripts/smoke-client.ps1 -WithProbe -StartScenario 53 -EndScenario 54 -Session wut-switch -NeoForgeVersion 21.1.251 -PrepareOnly
# 使用 Java 21 在 build/wut-switch 工作目录运行 @launch.args。
```

构建脚本可使用支持 `--release 21` 的 JDK 编译。这里游戏启动固定使用本机 `E:\java21\bin\java.exe`，防止旧模组加载器受到 Java 26 的类文件版本影响。

## 发布前补充：局域网反馈

截至 2026-10-26，新增反馈日志记录客户端加载 1.3.5，在局域网联机中使用 Curios 通用终端时，打开 `wireless_crafting_terminal` 和 `wireless_pattern_access_terminal` 均出现宿主不匹配并断连。日志只能确认加入者客户端版本，未取得房主对应的日志或 JAR 校验值，不能据此认定两端构件一致，也不能排除当前实现的其他同步缺陷。

上述单人隔离测试覆盖样板编码与合成模式之间的切换，未覆盖所有样板访问模式或两进程局域网组合；55 个场景通过不能推导为该反馈已修复。本次 GitHub 发布保留该已知问题，不修改待分析的联机行为。
