# LunarUnlocker 模块使用说明

## 概述

LunarUnlocker 是一个移植到 Vape 4.21 的 Lunar Client 化妆品解锁模块，仅支持 **Minecraft 1.8.9**。

## 功能

该模块通过反射技术绕过 Lunar Client 的化妆品验证系统，实现客户端解锁：

- ✅ Cosmetics (v1/v2) - 所有化妆品
- ✅ Emotes - 表情动作
- ✅ Badges - 徽章
- ✅ Sprays - 喷漆

## 使用方法

### 前置条件
1. 必须在 **Lunar Client 1.8.9** 上运行
2. 必须已经加入一个世界（单人或多人）
3. Vape 必须已成功注入

### 操作步骤

1. **打开 Vape GUI** (默认按键 `RSHIFT`)
2. 导航到 **Utility** 类别
3. 找到 **LunarUnlocker** 模块
4. **点击模块或按下绑定键**来激活

### 预期行为

- 模块会立即禁用（这是一个一次性操作模块）
- 显示通知消息：
  - ✅ 成功: "Unlocked: cosmetics, emotes, badges, sprays"
  - ❌ 失败: 显示具体错误原因

### 常见错误消息

| 错误消息 | 原因 | 解决方案 |
|---------|------|---------|
| "Join a world first!" | 未在游戏世界中 | 进入单人或多人世界后再试 |
| "Lunar Client was not detected." | 不是 Lunar Client 环境 | 确保在 Lunar Client 上运行 |
| "Could not find Lunar instance." | Lunar 单例未初始化 | 等待几秒后重试 |
| "Could not apply unlock (...)" | 部分系统解锁失败 | 部分功能可能已解锁 |

## 技术细节

### 工作原理

1. **运行时检测**: 检查是否存在 `com.moonsworth.lunar.LunarClient` 类
2. **单例查找**: 通过反射找到 Lunar Client 的单例实例
3. **伪造登录响应**: 为每个化妆品系统构建假的 LoginResponse 对象
4. **注入处理器**: 调用 Lunar 的内部处理方法来注册化妆品
5. **设置标志位**: 使用反射设置 "hasAll" 标志为 true

### 核心类

- **LunarUnlocker.java**: 模块入口，处理用户交互和通知
- **LunarUnlockUtil.java**: 核心解锁逻辑，包含所有反射操作

### 解锁的类和方法

```java
// Lunar Client 化妆品系统类名
com.lunarclient.websocket.cosmetic.v2.LoginResponse
com.lunarclient.websocket.cosmetic.v1.LoginResponse  
com.lunarclient.websocket.emote.v1.LoginResponse
com.lunarclient.websocket.badge.v1.LoginResponse
com.lunarclient.websocket.spray.v1.LoginResponse

// 设置的标志
setHasAllCosmeticsFlag(true)
setHasAllEmotesFlag(true)
setHasAllBadgesFlag(true)
setHasAllSpraysFlag(true)
```

## 限制和注意事项

⚠️ **重要警告**:

1. **仅客户端**: 解锁仅在你的客户端有效，其他玩家看不到你的化妆品
2. **临时性**: 每次重启 Lunar Client 后需要重新运行
3. **版本限制**: 仅在 **1.8.9** 测试通过，其他版本可能不工作
4. **检测风险**: 虽然是客户端操作，但可能违反 Lunar Client ToS
5. **不保证**: Lunar Client 更新后可能失效

## 故障排查

### 解锁后看不到化妆品？

1. 打开 Lunar Client 的化妆品菜单（通常在主菜单或 ESC 菜单）
2. 检查化妆品选项是否已解锁
3. 尝试选择和应用化妆品
4. 如果还是不行，尝试重新加入世界后再运行

### 模块无法启用？

- 检查是否在 Lunar Client 上
- 确保已经在世界中（不是主菜单）
- 查看 Vape 日志是否有错误信息

### 部分化妆品解锁失败？

这是正常的，不同版本的 Lunar Client 可能有不同的化妆品系统。模块会尝试所有已知系统，至少应该有一部分成功。

## 开发和调试

### 启用详细日志

修改 `LunarUnlockUtil.java` 添加调试输出：

```java
// 在关键位置添加
System.out.println("[LunarUnlocker] Found Lunar instance: " + lunarClient);
System.out.println("[LunarUnlocker] Attempting unlock: " + loginResponseClassName);
```

### 测试新的 Lunar Client 版本

如果 Lunar 更新导致模块失效：

1. 使用 Java 反编译工具检查新的类结构
2. 更新 `LunarUnlockUtil` 中的类名常量
3. 检查方法签名是否改变
4. 调整反射调用逻辑

## 源代码来源

本模块基于 Meowtils 框架的 LunarUnlocker 扩展进行移植和重写：

- 原始扩展: `LunarUnlocker-1.0.meowtils`
- Meowtils 框架: `Meowtils-2.0.1.jar`
- 反编译和分析: 使用 `javap` 工具

## 法律声明

⚠️ **免责声明**:

本模块仅用于：
- 教育目的
- 软件逆向工程研究
- 在自有环境中进行兼容性测试

使用本模块可能违反：
- Lunar Client 服务条款
- 某些服务器的规则
- 当地法律法规

**使用者需自行承担所有风险和法律责任。**

作者不对以下情况负责：
- 账号封禁
- 法律问题
- 数据损失
- 其他任何损害

## 版本历史

### v1.0 (2026-08-26)
- ✅ 初始版本
- ✅ 支持 Minecraft 1.8.9
- ✅ 支持 Cosmetics v1/v2
- ✅ 支持 Emotes, Badges, Sprays
- ✅ 集成到 Vape Utility 类别
- ✅ 完整的错误处理和通知

## 贡献

如果你发现 bug 或有改进建议：

1. 测试并记录详细的复现步骤
2. 包含 Lunar Client 版本信息
3. 提供日志输出（如果有）
4. 说明预期行为和实际行为

## 相关文档

- [UNLOCK_ANALYSIS.md](../UNLOCK_ANALYSIS.md) - 详细的技术分析
- [Vape 4.21 README](../README.md) - 项目主文档

---

**最后更新**: 2026-08-26  
**兼容版本**: Vape 4.21 + Lunar Client 1.8.9  
**状态**: 实验性功能
