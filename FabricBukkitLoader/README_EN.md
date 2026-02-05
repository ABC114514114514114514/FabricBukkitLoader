# FabricBukkitLoader

A Paper 1.21+ plugin that enables servers to load Fabric 1.21+ mods.

## Features

- 🔄 **Fabric Mod Loading** - Load and run Fabric mods on Paper servers
- ⚙️ **Configuration Management** - Flexible configuration system with hot reload support
- 🎮 **Event Adaptation** - Automatically converts Bukkit events to Fabric event format
- ⌨️ **Command Adaptation** - Supports Fabric mod commands running on Paper servers
- 🔒 **Security Control** - Whitelist/blacklist system, plugin signature verification
- 📊 **Management Interface** - Convenient management commands and monitoring features

## Installation

### Prerequisites

- Paper 1.21+ server
- Java 21
- Maven 3.6+ (for compilation)

### Installation Steps

1. **Download Plugin**
   - Download `FabricBukkitLoader-1.0.0.jar` from the releases page
   - Or use the pre-compiled jar file in this project

2. **Install Plugin**
   - Place `FabricBukkitLoader-1.0.0.jar` in your server's `plugins` folder
   - Start the server, the plugin will automatically generate configuration files

3. **Configure Plugin**
   - Edit the `plugins/FabricBukkitLoader/config.yml` configuration file
   - Make necessary settings (see configuration instructions)

4. **Add Fabric Mods**
   - Place Fabric mod `.jar` files in the `mods/` folder in the server root directory
   - Restart the server or use the `/fabricloader reload` command to load mods

## Configuration

### Main Configuration File (`config.yml`)

```yaml
# Enable/disable plugin loading
enable-plugin-loading: true

# Enable/disable debug logging
debug-logging: false

# Mods directory path (relative to server root)
mods-directory: "mods"

# Auto reload plugins
auto-reload-plugins: true

# Maximum number of plugins to load
max-plugins: 100

# Plugin compatibility mode
allow-incompatible-plugins: false

# Event adapter settings
event-adapter:
  enabled: true
  priority-handling: "NORMAL"
  event-filtering: true

# Command adapter settings
command-adapter:
  enabled: true
  enable-aliases: true
  enable-permissions: true

# Logging settings
logging:
  log-plugin-loading: true
  log-plugin-errors: true
  log-command-execution: false
  log-event-handling: false

# Security settings
security:
  verify-plugin-signatures: false
  allow-unsigned-plugins: true
  whitelist-plugins: []
  blacklist-plugins: []
```

## Commands

### Main Commands

- `/fabricloader help` - Display help information
- `/fabricloader info` - Display plugin information
- `/fabricloader mods` - List loaded Fabric mods
- `/fabricloader reload` - Reload plugin configuration

### Admin Commands (OP permission required)

- `/fabricloader load <mod-file>` - Load specified mod file
- `/fabricloader unload <mod-id>` - Unload specified mod

### Shortcut Commands

- `/fblmods` - Quick view of loaded mods list
- `/fbl` - Alias for `/fabricloader`

## Permissions

```
fabricbukkit.loader.* - All permissions
fabricbukkit.loader.admin - Admin permissions
fabricbukkit.loader.mods - View mods list
fabricbukkit.loader.reload - Reload plugin
fabricbukkit.loader.info - View plugin information
```

## Development

### Project Structure

```
fabric-bukkit-loader/
├── src/main/java/com/fabricbukkit/loader/
│   ├── FabricBukkitLoader.java    # Main class
│   ├── ModConfigManager.java      # Configuration manager
│   ├── ModLoader.java             # Mod loader
│   ├── EventAdapter.java          # Event adapter
│   ├── CommandAdapter.java        # Command adapter
│   └── LoadedMod.java             # Mod entity class
├── src/main/resources/
│   └── plugin.yml                 # Plugin configuration
├── pom.xml                        # Maven configuration
└── README.md                      # Documentation
```

### Build Project

```bash
# Build
mvn clean package

# Generated jar file is in target/ directory
```

### API Usage

```java
// Get plugin instance
FabricBukkitLoader plugin = FabricBukkitLoader.getInstance();

// Get loaded mods
Map<String, LoadedMod> mods = plugin.getLoadedMods();

// Load new mod
ModLoader modLoader = plugin.getModLoader();
LoadedMod mod = modLoader.loadMod(modFile);

// Event adaptation
EventAdapter eventAdapter = plugin.getEventAdapter();
eventAdapter.registerModEvents(mod);

// Command adaptation
CommandAdapter commandAdapter = plugin.getCommandAdapter();
commandAdapter.registerModCommands(mod);
```

## Supported Fabric Mods

This plugin supports most Fabric mods that don't require client-side modifications, including:

- 🔧 Utility mods
- 🏗️ Building mods
- 📦 Storage mods
- 🎛️ Mechanical mods
- 🔬 Technology mods
- ⚔️ Combat mods

**Note**: Mods requiring client support (such as new blocks, new item rendering) may not be fully compatible.

## FAQ

### Q: Plugin cannot load certain Fabric mods?

A: Check the following:
- Confirm the mod is compiled for Fabric 1.21+
- Check error messages in server logs
- Verify mod dependencies are satisfied
- Try enabling the `allow-incompatible-plugins` option

### Q: How to debug mod loading issues?

A: Enable debug logging in `config.yml`:
```yaml
debug-logging: true
logging:
  log-plugin-loading: true
  log-plugin-errors: true
```

### Q: Does it support all Fabric mods?

A: No, mods requiring client-side modifications are not supported. Server-side functionality usually works normally.

### Q: What about performance impact?

A: The plugin itself has minimal overhead. Mod performance depends on the specific mod implementation.

## Changelog

### v1.0.0
- ✨ Initial release
- ✨ Fabric mod loading support
- ✨ Event adaptation system
- ✨ Command adaptation system
- ✨ Configuration management system
- ✨ Permission control system

## Contributing

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

- PaperMC Team - For providing excellent server API
- FabricMC Team - For providing mod loading framework
- All contributors and testers

---

**Made with ❤️ for the Minecraft Modding Community**