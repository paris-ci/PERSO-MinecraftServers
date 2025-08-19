# Hunger Games Admin Commands

This document describes the admin commands available for managing the Hunger Games plugin.

## Permission Requirements

All admin commands require the `hungergames.admin` permission. Individual commands may require specific sub-permissions.

## Command Overview

**Base Command:** `/hgadmin` or `/admin` (aliases: `hga`)

## Available Commands

### Game Control Commands

#### `/hgadmin start`
- **Permission:** `hungergames.admin.start`
- **Description:** Force start the game
- **Usage:** `/hgadmin start`
- **Effect:** Immediately starts a new Hunger Games match

#### `/hgadmin next`
- **Permission:** `hungergames.admin.next`
- **Description:** Proceed to the next logical game phase
- **Usage:** `/hgadmin next`
- **Effect:** Advances the game to the next state in the sequence

#### `/hgadmin state <state_name>`
- **Permission:** `hungergames.admin.state`
- **Description:** Set the game to a specific state
- **Usage:** `/hgadmin state <state_name>`
- **Available States:**
  - `WAITING` - Waiting for players
  - `STARTING` - Game starting
  - `ACTIVE` - Game active
  - `FEAST` - Feast active
  - `BORDER_SHRINKING` - Border shrinking
  - `FINAL_FIGHT` - Final fight
  - `ENDING` - Game ending
  - `FINISHED` - Game finished

#### `/hgadmin cancel`
- **Permission:** `hungergames.admin.cancel`
- **Description:** Cancel the current game
- **Usage:** `/hgadmin cancel`
- **Effect:** Immediately ends the current game and returns to waiting state

#### `/hgadmin end`
- **Permission:** `hungergames.admin.end`
- **Description:** Force end the game
- **Usage:** `/hgadmin end`
- **Effect:** Forces the game to end state

### Phase-Specific Commands

#### `/hgadmin forcepvp`
- **Permission:** `hungergames.admin.forcepvp`
- **Description:** Force enable PvP
- **Usage:** `/hgadmin forcepvp`
- **Effect:** Immediately enables PvP regardless of timing

#### `/hgadmin forcefeast`
- **Permission:** `hungergames.admin.forcefeast`
- **Description:** Force spawn the feast
- **Usage:** `/hgadmin forcefeast`
- **Effect:** Immediately spawns the feast at the configured location

#### `/hgadmin forceborder`
- **Permission:** `hungergames.admin.forceborder`
- **Description:** Force start border shrinking
- **Usage:** `/hgadmin forceborder`
- **Effect:** Immediately starts the world border shrinking phase

#### `/hgadmin forcefinal`
- **Permission:** `hungergames.admin.forcefinal`
- **Description:** Force start the final fight
- **Usage:** `/hgadmin forcefinal`
- **Effect:** Immediately starts the final fight phase with poison effects

### Utility Commands

#### `/hgadmin status`
- **Permission:** `hungergames.admin.status`
- **Description:** Show detailed game status
- **Usage:** `/hgadmin status`
- **Information Displayed:**
  - Plugin status
  - Game running status
  - Current game state
  - Player counts (alive/dead)
  - PvP status
  - Feast status
  - Online players
  - World information

#### `/hgadmin reload`
- **Permission:** `hungergames.admin.reload`
- **Description:** Reload plugin configuration
- **Usage:** `/hgadmin reload`
- **Effect:** Reloads config.yml without restarting the server

#### `/hgadmin help`
- **Permission:** `hungergames.admin`
- **Description:** Show help information
- **Usage:** `/hgadmin` or `/hgadmin help`
- **Effect:** Displays all available commands and their descriptions

## Game State Flow

The normal game progression follows this sequence:

1. **WAITING** → Players join and select kits
2. **STARTING** → Countdown and player preparation
3. **ACTIVE** → Game begins, PvP disabled initially
4. **FEAST** → Feast spawns, enhanced loot available
5. **BORDER_SHRINKING** → World border begins shrinking
6. **FINAL_FIGHT** → Poison effects, final confrontation
7. **ENDING** → Game cleanup and winner announcement
8. **FINISHED** → Game completely finished

## Examples

### Start a new game immediately:
```
/hgadmin start
```

### Skip waiting phase and go straight to active game:
```
/hgadmin state ACTIVE
```

### Force the feast to spawn:
```
/hgadmin forcefeast
```

### Check current game status:
```
/hgadmin status
```

### Cancel the current game:
```
/hgadmin cancel
```

## Safety Features

- All admin commands are logged for audit purposes
- Commands broadcast their effects to all players
- State transitions are validated to prevent invalid states
- Commands check game status before executing

## Troubleshooting

- **"No game is currently running"** - Use `/hgadmin start` to begin a game
- **"Cannot proceed from current state"** - Use `/hgadmin state <state>` to set a specific state
- **Permission denied** - Ensure you have the required permissions or are an operator

## Permission Hierarchy

```
hungergames.admin.*
├── hungergames.admin.start
├── hungergames.admin.next
├── hungergames.admin.state
├── hungergames.admin.cancel
├── hungergames.admin.reload
├── hungergames.admin.status
├── hungergames.admin.forcepvp
├── hungergames.admin.forcefeast
├── hungergames.admin.forceborder
├── hungergames.admin.forcefinal
└── hungergames.admin.end
```

All admin permissions default to `op` (operator) level for security.
