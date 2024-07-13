# Egg_MC Plugin

## Overview

Egg_MC is a custom Minecraft plugin designed for Spigot servers. This plugin adds various features including warp management, point tracking, and random teleportation. Players can easily navigate between different game modes and locations using warp selectors.

## Features

- **Warp Management**: Players can teleport to predefined locations such as `lobby`, `pvp`, `loja`, and `survival` using commands or a warp selector item.
- **Point Tracking**: Players earn points for killing entities. Points are displayed on the scoreboard.
- **Random Teleportation**: Players can use the `/tpr` command to teleport to a random location in the survival world after a cooldown period.
- **Welcome Messages**: Display a custom welcome message and title when players join the server.
- **Inventory Management**: The plugin saves and restores player inventories when they switch between different worlds.

## Installation

1. **Download**: Download the latest version of the plugin JAR file.
2. **Place in Plugins Folder**: Place the JAR file in the `plugins` folder of your Spigot server.
3. **Start Server**: Start or restart your server to load the plugin.
4. **Configuration**: Edit the `config.yml` file to set up the warp locations and other settings as needed.

## Commands

- `/lobby` - Teleport to the lobby.
- `/pvp` - Teleport to the PvP area.
- `/loja` - Teleport to the shop.
- `/survival` - Teleport to the survival world and restore inventory.
- `/tpr` - Teleport to a random location in the survival world.

## Permissions

- **eggmc.warp** - Allows access to the warp commands (`/lobby`, `/pvp`, `/loja`, `/survival`).
- **eggmc.tpr** - Allows access to the `/tpr` command.

## Configuration

The `config.yml` file contains settings for warp locations and other plugin configurations. Here is an example:

```yaml
warps:
  lobby:
    world: "lobby_world"
    x: 0
    y: 64
    z: 0
    yaw: 0
    pitch: 0
  pvp:
    world: "pvp_world"
    x: 0
    y: 64
    z: 0
    yaw: 0
    pitch: 0
  loja:
    world: "loja_world"
    x: 0
    y: 64
    z: 0
    yaw: 0
    pitch: 0
  survival:
    world: "world"
    x: 0
    y: 64
    z: 0
    yaw: 0
    pitch: 0
```

## Usage

### Warp Selector

1. **Receive Warp Selector**: When a player joins the server, they receive a Nether Star named "Seletor de Warp".
2. **Open Warp Menu**: Right-clicking with the Nether Star opens the warp menu.
3. **Select Warp**: Click on the desired warp location in the menu to teleport.

### Points System

1. **Earn Points**: Players earn points for killing entities.
2. **View Points**: Points are displayed on the scoreboard sidebar.

### Random Teleportation

1. **Use /tpr Command**: Players can use the `/tpr` command to teleport to a random location in the survival world.
2. **Cooldown**: The command has a cooldown of 5 minutes between uses.
