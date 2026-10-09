# 1.3.2 ATM10 8.2 启动崩溃修复验收

日期：2026-10-08。用户报告 ATM10 8.2 在 1.3.1 启动时崩溃。

直接堆栈为 `ClientEvents.screen` → `RouterConfig.DEBUG.get()` → `Cannot get config value before config is loaded.`。8.2 启动过程会在 COMMON 配置完成前打开初始屏幕，1.3.1 的调试日志分支提前读取配置，导致启动线程崩溃。

修复内容：屏幕事件和客户端 tick 都在 `RouterConfig.SPEC.isLoaded()` 后才读取配置。回归检查模拟未加载配置的屏幕事件，确认不再崩溃。

同一日志还记录 Integrated Scripting 替换 `graal-sdk-25.2.4.jar` 时文件被其他进程占用。这是整合包独立的文件锁问题，需完全退出 ATM10 和启动器后再启动。

1.3.2 使用 NeoForge 21.1.249 构建，43 个 Java 文件，零错误、4 条原有弃用警告。8.2 JEI 19.57.0.446 的场景 42–52 使用新 JAR 通过；完整 494 模组启动需确保依赖文件没有被占用。
