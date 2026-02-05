# FabricBukkitLoader

一个Paper 1.21+插件，使服务器能够加载Fabric 1.21+的mods。

## 功能特性

- 🔄 **Fabric Mod加载** - 在Paper服务器上加载和运行Fabric mods
- ⚙️ **配置管理** - 灵活的配置系统，支持热重载
- 🎮 **事件适配** - 自动将Bukkit事件转换为Fabric事件格式
- ⌨️ **命令适配** - 支持Fabric mod命令在Paper服务器上运行
- 🔒 **安全控制** - 白名单/黑名单系统，插件签名验证
- 📊 **管理界面** - 便捷的管理命令和监控功能
- 📈 **性能监控** - 详细的mod性能统计和分析
- 🔄 **依赖管理** - 完整的mod依赖解析、版本检查和冲突检测
- ⚡ **优化加载** - 智能加载顺序，提升服务器性能
- 🔄 **更新检查** - 自动检查mod更新并通知用户
- 🔐 **安全扫描** - 扫描mod文件中的潜在安全问题
- 🛡️ **物品保护** - 防止玩家因mod物品被踢出服务器
- ⛏️ **创造模式保护** - 专门处理创造模式物品栏数据包问题
- 🛡️ **增强物品保护** - 全面监控和处理背包操作事件
- 🧠 **智能物品保护** - 区分安全与危险操作，允许mod方块放置
- ⌨️ **指令保护** - 允许安全的mod指令执行，阻止危险操作
- 🔧 **增强依赖检查** - 更智能的依赖解析和兼容性处理

## 安装说明

### 前提条件

- Paper 1.21+ 服务器
- Java 21
- Maven 3.6+ (用于编译)

### 安装步骤

1. **下载插件**
   - 从 releases 页面下载 `FabricBukkitLoader-1.0.4.jar`
   - 或者使用本项目中已编译好的 jar 文件

2. **安装插件**
   - 将 `FabricBukkitLoader-1.0.3.jar` 放入服务器的 `plugins` 文件夹
   - 启动服务器，插件会自动生成配置文件

3. **配置插件**
   - 编辑 `plugins/FabricBukkitLoader/config.yml` 配置文件
   - 根据需要进行设置（见配置说明）

4. **添加Fabric Mods**
   - 将Fabric mod的 `.jar` 文件放入服务器根目录的 `mods/` 文件夹
   - 重启服务器或使用 `/fabricloader reload` 命令加载mods

## 配置说明

### 主配置文件 (`config.yml`)

```yaml
# 启用/禁用插件加载
enable-plugin-loading: true

# 启用/禁用调试日志
debug-logging: false

# Mods目录路径（相对于服务器根目录）
mods-directory: "mods"

# 自动重载插件
auto-reload-plugins: true

# 最大插件加载数量
max-plugins: 100

# 插件兼容性模式
allow-incompatible-plugins: false

# 事件适配器设置
event-adapter:
  enabled: true
  priority-handling: "NORMAL"
  event-filtering: true

# 命令适配器设置
command-adapter:
  enabled: true
  enable-aliases: true
  enable-permissions: true

# 日志设置
logging:
  log-plugin-loading: true
  log-plugin-errors: true
  log-command-execution: false
  log-event-handling: false

# 安全设置
security:
  verify-plugin-signatures: false
  allow-unsigned-plugins: true
  whitelist-plugins: []
  blacklist-plugins: []

# 依赖检查设置
dependency-check:
  enabled: true
  conflict-detection: true
  enforce-version-compatibility: true
  # 依赖检查严格模式：true=严格检查，false=宽松检查（允许部分依赖缺失）
  strict-mode: false
  # 允许的替代依赖（例如某些mod有多种实现）
  allowed-alternatives:
    - ["fabric-api", "fabric-api-base"]

# 优先级加载设置
priority-loading:
  enabled: true
  priority-mods:
    - "fabric-api-base"
    - "fabric-resource-loader-v0"
    - "fabric-lifecycle-events-v1"

# 性能监控设置
performance-monitoring:
  enabled: true
  record-event-times: true

# 物品保护设置
item-protection:
  # 检测到问题物品时的处理方式
  # 选项: "protect", "replace", "remove", "warn"
  action: "protect"
  
  # 启用物品替换（仅在 action 为 "replace" 时使用）
  enable-replacement: false
  
  # 启用物品移除（仅在 action 为 "remove" 时使用）
  enable-removal: false

# 增强依赖检查设置
enhanced-dependency-check:
  # 启用增强依赖检查
  enabled: true
  # 是否尝试解析替代依赖
  resolve-alternatives: true
  # 是否允许版本范围匹配
  allow-version-range: true
  # 是否记录依赖解析详情
  log-resolution-details: false
```

## 使用命令

### 主要命令

- `/fabricloader help` - 显示帮助信息
- `/fabricloader info` - 显示插件信息
- `/fabricloader mods` - 列出已加载的Fabric mods
- `/fabricloader reload` - 重载插件配置

### 管理命令 (需要 OP 权限)

- `/fabricloader load <mod-file>` - 加载指定的mod文件
- `/fabricloader unload <mod-id>` - 卸载指定的mod

### 快捷命令

- `/fblmods` - 快速查看已加载的mods列表
- `/fbl` - `/fabricloader` 的别名

## 权限节点

```
fabricbukkit.loader.* - 所有权限
fabricbukkit.loader.admin - 管理权限
fabricbukkit.loader.mods - 查看mods列表
fabricbukkit.loader.reload - 重载插件
fabricbukkit.loader.info - 查看插件信息
```

## 开发说明

### 项目结构

```
fabric-bukkit-loader/
├── src/main/java/com/fabricbukkit/loader/
│   ├── FabricBukkitLoader.java    # 主类
│   ├── ModConfigManager.java      # 配置管理
│   ├── ModLoader.java             # Mod加载器
│   ├── EventAdapter.java          # 事件适配器
│   ├── CommandAdapter.java        # 命令适配器
│   ├── LoadedMod.java             # Mod实体类
│   ├── ModDependency.java         # 依赖管理
│   ├── PerformanceMonitor.java    # 性能监控
│   ├── UpdateChecker.java         # 更新检查
│   ├── SecurityScanner.java       # 安全扫描
│   ├── ItemProtectionSystem.java  # 物品保护系统
│   ├── CreativeModeSlotProtection.java  # 创造模式保护
│   ├── EnhancedItemProtectionSystem.java  # 增强物品保护
│   ├── SmartItemProtectionSystem.java  # 智能物品保护
│   ├── CommandProtectionSystem.java  # 指令保护系统
│   └── EnhancedDependencyChecker.java  # 增强依赖检查
├── src/main/resources/
│   └── plugin.yml                 # 插件配置
├── pom.xml                        # Maven配置
└── README.md                      # 说明文档
```

### 编译项目

```bash

# 编译
mvn clean package

# 生成的jar文件在 target/ 目录
```

### API 使用

```java
// 获取插件实例
FabricBukkitLoader plugin = FabricBukkitLoader.getInstance();

// 获取已加载的mods
Map<String, LoadedMod> mods = plugin.getLoadedMods();

// 加载新的mod
ModLoader modLoader = plugin.getModLoader();
LoadedMod mod = modLoader.loadMod(modFile);

// 事件适配
EventAdapter eventAdapter = plugin.getEventAdapter();
eventAdapter.registerModEvents(mod);

// 命令适配
CommandAdapter commandAdapter = plugin.getCommandAdapter();
commandAdapter.registerModCommands(mod);
```

## 支持的Fabric Mods

本插件支持大多数不需要客户端修改的Fabric mods，包括：

- 🔧 工具类mods
- 🏗️ 建筑类mods  
- 📦 存储类mods
- 🎛️ 机械类mods
- 🔬 科技类mods
- ⚔️ 战斗类mods

**注意**: 需要客户端支持的mods（如新方块、新物品渲染）可能无法完全兼容。

## 常见问题

### Q: 插件无法加载某些Fabric mods？

A: 检查以下几点：
- 确认mod是为Fabric 1.21+版本编译的
- 查看服务器日志中的错误信息
- 检查mod的依赖是否满足
- 尝试启用 `allow-incompatible-plugins` 选项

### Q: 如何调试mod加载问题？

A: 在 `config.yml` 中启用调试日志：
```yaml
debug-logging: true
logging:
  log-plugin-loading: true
  log-plugin-errors: true
```

### Q: 是否支持所有Fabric mods？

A: 不支持需要客户端修改的mods。服务器端功能通常可以正常工作。

### Q: 性能影响如何？

A: 插件本身开销很小。mod的性能取决于具体的mod实现。

## 更新日志

### v1.0.4
- 🔧 修复了原版物品被误报为问题物品的错误
- ✨ 优化了物品检测算法，提高准确性
- ✨ 改进了保护系统，减少对原版物品的误判

### v1.0.3
- ✨ 添加了指令保护系统，允许安全的mod指令执行
- ✨ 修复了mod指令无法执行的问题
- ✨ 添加了智能物品保护系统，区分安全与危险操作
- ✨ 优化了物品保护策略，允许mod方块正常放置
- ✨ 改进了创造模式操作处理，修复指令放置问题
- ✨ 保留了对数据包解码错误的防护

### v1.0.2
- ✨ 添加了增强物品保护系统，全面监控背包操作事件
- ✨ 改进了创造模式物品栏数据包处理机制
- ✨ 增加了更多事件监听器防止数据包解码错误
- ✨ 优化了物品检查算法，提高性能
- ✨ 增强了对Waystones等mod的支持

### v1.0.1
- ✨ 添加了性能监控系统
- ✨ 添加了依赖管理和版本检查
- ✨ 添加了优先级加载功能
- ✨ 添加了自动更新检查
- ✨ 添加了安全扫描功能
- ✨ 添加了物品保护系统，防止玩家因mod物品被踢出
- ✨ 添加了创造模式保护系统，解决创造模式物品栏数据包问题
- ✨ 添加了增强依赖检查系统，智能处理mod依赖问题
- ✨ 优化了事件处理性能
- ✨ 改进了错误报告机制
- ✨ 添加了可配置的依赖检查严格模式

### v1.0.0
- ✨ 初始版本
- ✨ 支持Fabric mod加载
- ✨ 事件适配系统
- ✨ 命令适配系统
- ✨ 配置管理系统
- ✨ 权限控制系统


### 开发环境搭建

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 致谢

- PaperMC 团队 - 提供优秀的服务器API
- FabricMC 团队 - 提供mod加载框架
- 所有贡献者和测试人员

---

**Made with ❤️ for the Minecraft Modding Community**