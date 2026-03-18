Extension to **WorldGuard** to create temporary building zones. Blocks placed in designated regions automatically decay after a configurable time period.

## Supported Platforms

<a href="https://papermc.io/downloads/paper" target="_blank"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v2/assets/compact/supported/paper_46h.png" height="35"></a>
<br>
<a href="https://purpurmc.org/download/purpur" target="_blank"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v2/assets/compact/supported/purpur_46h.png" height="35"></a>

## Features

- **WorldGuard Integration**: Adds custom `temp-build` region flag
- **Automatic Block Decay**: Blocks placed in temp-build zones disappear after configured time
- **Visual Feedback**: Shows block damage progress as decay timer counts down
- **Configurable Drop Behavior**: Choose whether decaying blocks drop items or vanish
- **UltimateBlockRegen Compatibility**: Prevents temp blocks from being regenerated as bedrock and allows breaking UBR bedrock replacements

## Dependencies
- [WorldGuard](https://dev.bukkit.org/projects/worldguard) - Region protection and custom flags

## Build

```bash
mvn clean package
```

## Configuration

### config.yml

```yaml
# Time in seconds before placed blocks decay and disappear
blockDecayTime: 30

# Whether decaying blocks should drop items when they disappear
dropBlocks: true
```

**blockDecayTime**: Duration in seconds that blocks remain before decaying. Set to `0` or negative to disable decay entirely.

**dropBlocks**: When `true`, blocks drop their items when the timer expires. When `false`, blocks simply vanish.

## WorldGuard Flag Usage

TempBuild adds a custom WorldGuard flag called `temp-build`.

### Setting Up a Temporary Build Region

```bash
# Allow temporary building in a region
/region flag <region-name> temp-build allow

# Disable temporary building (default behavior)
/region flag <region-name> temp-build deny
```

### ⚠️ IMPORTANT

For the `temp-build` flag to work properly, you must ensure that no other WorldGuard flags are blocking block placement in the region.
If players still cannot place blocks despite `temp-build` being set to ALLOW, check for conflicting flags like `passthrough`, `build`, or other protection flags that may be preventing block placement.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/tempbuild reload` | Reload plugin configuration | `tempbuild.command.reload` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `tempbuild.command.reload` | Allows reloading the plugin configuration | `op` |
