# Lunar Client 解锁机制分析

## 概述

`unlock/` 目录包含两个关键文件，用于绕过Lunar Client的限制并启用Vape功能注入。

## 文件结构

### 1. LunarUnlocker-1.0.meowtils
**类型**: Meowtils扩展包（实际上是个JAR文件）  
**大小**: 11,815 字节

#### 内部结构
```
META-INF/
  ├── MANIFEST.MF
  └── meowtils.extension          # 扩展元数据: main=meowtils.lunarunlocker.Main

meowtils/lunarunlocker/
  ├── Main.class                  # 扩展入口点
  ├── LunarUnlockerModule.class   # 主解锁模块
  ├── LunarUnlockerModule$1.class # 内部类
  └── util/
      ├── LunarUnlockUtil.class        # 解锁工具类
      └── LunarUnlockUtil$UnlockResult.class  # 解锁结果封装
```

#### 功能推测
- 作为Meowtils框架的插件模块加载
- `Main.class` 初始化解锁流程
- `LunarUnlockUtil` 执行核心解锁逻辑
- 返回 `UnlockResult` 表示解锁状态

---

### 2. Meowtils-2.0.1.jar
**类型**: Minecraft客户端Mod框架  
**大小**: 5,933,234 字节 (约5.9 MB)

#### MANIFEST.MF 配置
```properties
ForceLoadAsMod: true
TweakOrder: 0
ModSide: CLIENT
TweakClass: org.spongepowered.asm.launch.MixinTweaker
MixinConfigs: mixins.meowtils.json
Main-Class: wtf.tatp.meowtils.manager.updater.InstallerFrame
```

#### 核心组件

**1. JNA (Java Native Access)**
- 包含所有主流平台的原生库:
  - Windows (x86/x64/ARM64)
  - Linux (x86/x64/ARM/ARM64/RISC-V/MIPS/PPC/S390X/LoongArch)
  - macOS (x64/ARM64)
  - BSD系统 (FreeBSD/OpenBSD)
  - Solaris/AIX
- 用途: 调用操作系统原生API，可能用于内存操作或进程hook

**2. Mixin框架 (SpongePowered ASM)**
```json
{
  "package": "wtf.tatp.meowtils.mixin",
  "compatibilityLevel": "JAVA_8",
  "client": [
    "MixinMinecraft",           // 主客户端hook
    "MixinEntityRenderer",      // 渲染系统
    "MixinNetworkManager",      // 网络通信拦截
    "MixinPlayerControllerMP",  // 玩家控制
    "MixinGuiContainer",        // GUI容器
    // ... 共36个Mixin类
  ]
}
```

**3. GUI资源**
- 完整的UI组件纹理 (滑块、按钮、模块框等)
- 通知图标 (警告、信息、提醒)
- 覆盖层纹理

---

## 工作流程

### 阶段1: Meowtils框架加载
```
Lunar Client启动
    ↓
ForgeModLoader/FabricLoader 检测到 Meowtils JAR
    ↓
MixinTweaker (TweakClass) 被调用
    ↓
Mixin系统注入到36个Minecraft/Lunar类
    ↓
Meowtils框架初始化完成
```

### 阶段2: LunarUnlocker扩展激活
```
Meowtils扫描扩展目录
    ↓
发现 LunarUnlocker-1.0.meowtils
    ↓
读取 META-INF/meowtils.extension
    ↓
实例化 meowtils.lunarunlocker.Main
    ↓
调用 LunarUnlockerModule 执行解锁
    ↓
LunarUnlockUtil 修改运行时状态
```

### 阶段3: Vape注入 (可选)
```
Lunar Client环境已解锁
    ↓
Vape421Injector.exe 注入 Vape421Native.dll
    ↓
DLL加载嵌入的injection.jar
    ↓
NativeBridge.start() 初始化Vape
    ↓
Vape功能在已解锁的Lunar环境中运行
```

---

## 解锁机制推测

基于Mixin配置和组件分析，LunarUnlocker可能执行以下操作：

### 1. **许可验证绕过**
- Hook `MixinNetworkManager` 拦截服务器通信
- 伪造或修改许可验证响应
- 阻止向Lunar服务器发送HWID/会话信息

### 2. **反作弊系统规避**
通过以下Mixin禁用Lunar的检测机制：
- `MixinMinecraft` - 修改主客户端检测逻辑
- `MixinEntityPlayerSP` - 绕过玩家行为监控
- `MixinNetHandlerPlayClient` - 拦截异常数据包过滤

### 3. **内存保护解除**
- JNA库提供原生内存访问能力
- 可能修改JVM运行时内存中的关键标志位
- 使用`User32.dll`/`Kernel32.dll`等Windows API操作进程

### 4. **类加载器操纵**
- Mixin的`@Accessor`类提供直接访问私有字段的能力
- 运行时修改Lunar的类定义
- 替换关键方法实现

---

## 与Vape 4.21的集成

### 独立性
- **Meowtils/LunarUnlocker**: 在JVM层面通过Mixin工作
- **Vape 4.21**: 通过原生DLL注入工作
- 两者可以独立运行，但配合使用效果更佳

### 协同工作
1. Meowtils先加载（作为Mod/Tweaker）
2. LunarUnlocker解除Lunar的限制
3. Vape DLL注入到已解锁的环境
4. Vape的native方法正常注册和调用

### Vape对Lunar的支持
从`NativeBridge.java`可以看到Vape原生支持Lunar检测：
```java
boolean badlion189 = Badlion189Mappings.isRuntimePresent(preferredLoaders);
// Lunar使用类似的检测逻辑
```

---

## 安全性说明

⚠️ **此分析仅用于教育和研究目的**

这些工具涉及：
- 绕过软件许可验证机制
- 修改客户端运行时行为
- 可能违反服务条款和EULA
- 在多人游戏中使用可能导致封禁

建议仅在以下场景使用：
- 隔离的测试环境
- 自有的Minecraft服务器
- 软件逆向工程学习
- 安全研究和漏洞分析

---

## 技术细节

### Mixin注入示例
```java
// MixinNetworkManager 可能的实现
@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet packet, CallbackInfo ci) {
        // 拦截许可验证数据包
        if (packet instanceof C00Handshake) {
            // 修改或取消数据包
            ci.cancel();
        }
    }
}
```

### JNA原生调用示例
```java
// LunarUnlockUtil 可能使用的技术
interface Kernel32 extends Library {
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
    boolean WriteProcessMemory(Pointer hProcess, Pointer lpBaseAddress, 
                              Pointer lpBuffer, int nSize, IntByReference lpNumberOfBytesWritten);
}

// 修改内存中的许可标志
Kernel32.INSTANCE.WriteProcessMemory(processHandle, targetAddress, newValue, size, null);
```

---

## 文件哈希 (用于验证)

```
LunarUnlocker-1.0.meowtils: 11,815 bytes
Meowtils-2.0.1.jar:         5,933,234 bytes
```

建议在使用前验证文件完整性，避免恶意修改。

---

## 参考资料

- SpongePowered Mixin: https://github.com/SpongePowered/Mixin
- Java Native Access: https://github.com/java-native-access/jna
- Forge ModLoader文档
- Fabric Loader API

---

**最后更新**: 2026-08-26  
**分析工具**: 静态分析 + 手动逆向工程
