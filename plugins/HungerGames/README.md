# Hungergames plugin

This is a paper minecraft plugin for an hungergames gamemode. At start, all players will spawn in a new world, and will need to fight each other until only one remain.

Once players die, they are moved to spectator, and may not interact with the game.

The configuration may also specify that players will compete as teams (either random or created with the /party command, permission gated). In this case, the last team standing wins.

## Requirements

A database is used to store games logs and players own configuration.

Players table:
- id
- uuid (mc's uuid)
- credits
- last_kit_used

Game table:
- id
- server_id (from config)
- waiting_at
- started_at (nullable)
- ended_at (nullable)

GameParty:
- id
- game_id
- name

GameLog table:
- id
- game_id
- player (foreign key)
- party_id (foreign key, not nullable, as parties are always created, even if composed of only one player)
- died_at (nullable)
- death_reason (PLAYER, ENVIRONNEMENT, DISCONNECTED, WINNER)
- killer (foreign key, nullable)
- death_message (string)


## Game plan
The server should be started with no world directory, and the world generation is handled by Paper/Bukkit.
This allows owners to customize the generation or to supply a pre-built world

### After world generation

A starting platform will be generated at spawn :

- A circle of %spawn_radius blocks of wooden blocks
- A nice "fountain/pillar" of sorts, containing chests on two levels, filled with random items (%spawn_items)
- The world border is set to %world_border_initial_size blocks

### Waiting for players...

The game will not start for some time to let the players join the server (at most %max_wait_time seconds or until the server is full) and select a kit and a party, if configured.

The players are allowed to fly around and explore the map and the spawn

### Starting...

All players are teleported in the wooden blocks spawn circle, and frozen in place (and made invincible) for %spawn_teleport_delay seconds while a countdown appear on their screen.

Once the countdown ends :

- They get a compass set to track either the spawn, the feast (when spawned), the closest party member or the closes non-party member (they toggle the modes by clicking)
- They are given their kit items and abilities 
- They are able to move.

PvP is enabled after %pvp_delay seconds. Kills are recorded, and the killer gets their opponent head that can be consumed to activate a regen IV potion on them for 3 seconds.

If enabled, chat messages will be sent every 2 minutes to remind players of the time left before the feast spawns

### The feast

If %feast_enabled is true, 10 minutes after PvP is enabled, a feast will spawn. Similarly to the spawn platform, it's a circle of %feast_radius blocks of stone blocks, spawned on the topmost block at a random location inside the world border (and at least %feast_border_distance blocks away from the world border)

The content of this platform chests are set using the %feast_items configuration variable.

Once spawned, the feast coordinates are broadcast to every online player. The compass can also be used to track the feats location.

### Border closing

The border will then start to shrink to make players move back towards the map center/spawn. The border doesn't go faster than a regular player walking speed, and will stop at 30 blocks

### The final fight

The final fight should happen naturally as the border shrinks. However, if the game is stuck for a while (%max_game_time minutes), increasing amounts (I, II, III, IV, V, X) of permanent poison effects are applied to players.

If the players are still alive after X poison effect is applied for more than 10 seconds, kill them and end the game

### Game end 
Credits and data is saved to the database if not already and the server is stopped.

## Credits 

Some actions will award players with credits in their permanent account:

- Game started (+3 credits)
- Survived for one minute (+1 credit)
- Killed someone (+50 credits)
- Someone in the players party killed an hostile player (+25 credits)
- Game won (+500 credits, only if the game had 4 teams or more, otherwise +100)
- AFK kicked (-30 credits)
- Damaged by the final fight server poison effects (-50 credits, once per game)

## Spectators

When killed, players can no longer interact with the world. However, they can, using their compass and a menu

- Spectate and view the game as normal
- View and teleport to any alive player : getting to see their kit and their inventory
- Open the kit menu and select a kit that will be used by default for them when they join the game next time
- Use any of their kit "after death" effect
- Use the chat to talk between themselves only. Alive players don't see dead players messages.

## Kits

Kits are the most important aspect of this game and can be selected in a before the game starts. Once the game has started, a player is no longer able to change their kit. 

The kit selected by the player is displayed before their name in the chat and above their head in game.

Kits are separated in two distinct categories : regular kits (avaiable to everyone), and premium kits, that must be unlocked using credits.

### Default kits

#### The swordsman

Starting items: 
- Leather armor
- Iron sword

Starting effects:
- 3:00 Speed I potion

On death effects:
None

After death effects:
They may activate the sword fight effect (1 minute cooldown) to display particles next to them 


#### The tank
Starting items: 
- Iron armor
- Stone sword
- 2x Cooked beef

Starting effects:
- 5:00 Resistance I potion
- 3:00 Slowness I potion

On death effects:
None

After death effects:
They may activate the iron defense effect (2 minute cooldown) to spawn an uncollectable iron ingot at their feet for 15 seconds

#### The archer

Starting items: 
- Leather armor
- Iron helmet
- Bow
- 32x Arrows
- Wooden sword

Starting effects:
- 1:00 Jump II potion

On death effects:
They release a torrent of uncollectable arrows in all directions when given the final blow 

After death effects:
They may activate the eagle eye effect (90 seconds cooldown) to highlight nearby players with glowing effect for 10 seconds

#### The assassin

Starting items: 
- Leather armor (black dyed)
- Iron sword (sharpness II)
- 2x Poison splash potion

Starting effects:
- 2:00 Invisibility I potion
- 5:00 Speed II potion
- Their maximum health is set to 4 hearts

On death effects:
None

After death effects:
None

#### The medic

Starting items: 
- Leather armor (dyed pink)
- 3x Health potion II
- 2x Throwable Regeneration potion
- 6 Porkchop

Starting effects:
- 10:00 Regeneration I potion

On death effects:
None

After death effects:
They may activate the healing aura effect (3 minute cooldown, 3 seconds duration) to display heart particles around them, giving 00:02 regeneration I to anyone (includes players, mobs, enemies, ...) stepping in a 15 blocks radius around them.

### Premium kits

#### The berserker

Cost: 150 credits

Starting items: 
- Chain armor
- Iron axe (sharpness I)
- 3x Cooked porkchop

Starting effects:
- 2:00 Strength I potion
- 4:00 Speed I potion

On death effects:
Explodes dealing damage to nearby players

After death effects:
They may activate the rage mode effect (4 minute cooldown) to play aggressive sounds

#### The wizard

Cost: 200 credits

Starting items: 
- Leather armor (purple dyed)
- 2x Fire charge
- 1x Ender pearl
- 3x Splash potion of harming

Starting effects:
- 3:00 Fire resistance potion

On death effects:
Strikes lightning at death location

After death effects:
None

#### The builder

Cost: 50 credits

Starting items:
- 64 dirt blocks
- 128 cobblestone blocks
- A water bucket
- A lava bucket
- 96 ladders
- 1 sapling

Starting effects:
- 2:00 jump boost I
- 2:00 regenration II

On death effects:
Their inventory is transfered into a chest

After death effects:
They get 3 glass blocks they can place on the map

#### The spawner

Cost: 500 credits

Starting items:

- 3 creeper eggs
- 3 spider eggs
- A zombie spawner
- 64 Infested cobblestone 
- Chainmail armor

Starting effects:
None

On death effects:
A charged creeper is spawned on their corpse

After death effects:
Every minute, they can spawn a random passive mob at their position.

#### Archer Pro

Cost: 1000 credits

Starting items:
- Bow
- 32 arrows
- Flint and steel

Starting effects:
- Arrows explode on hit, dealing damage to players and mobs within 4 blocks
- Explosions cause knockback and destroy blocks like TNT
- Arrows collide with spectators

## FAQ

- For team games, what happens if a party member disconnects? Do they auto-die, or can they rejoin?
If a player disconencts, they die immediatly and drops their inventory to the ground. They can rejoin as a spectator, and use their spectator effects.


- What's the maximum party size?
%maximum_party_size, 4 is recommanded.

- The compass tracking system sounds complex - how do you want to handle multiple valid targets (e.g., multiple party members)?
The compass always track the closest valid target. eg. closest team member, closest enemy,...
The mode (what KIND of thing the compass tracks) is toggled by the player, using a right click. It'll circle between the spawn, the feast (when spawned), the closest party member or the closest non-party member.

- The feast spawns 10 minutes after PvP starts - is this configurable? 
%feast_appears_after

- How do you want to handle multiple games?
Only one game is ran per server, and the server is dedicated to the game, and will be stopped at the end.

- World Generation: You clarified that Paper/Bukkit handles world generation, which is good. But what about the spawn platform generation? Are you planning to use WorldEdit API or build it manually with blocks?
The spawn platform is generated by the plugin, but the worldedit API can be used to spawn a schematic instead (%spawn_schematic_file, %feast_schematic_file)

- Database Schema: The database design looks solid, but I notice GameLog.party_id is not nullable - what happens if a player joins without a party? Do you auto-create a solo party?
Exactly : every player gets a randomly assigned named to their party as well, of the format The [block name] [mob name] (eg. the stone chickens, the diamond endermans, the lapis zombies...)

- Server Management: Since it's one game per server, how do you want to handle the server restart process? Will there be a lobby server where players wait?
The server is shutdown by the plugin, and a custom start script will delete the world and restart the server.
The players are kept online in the lobby using velocity proxy protocol.

- Anti-Cheat: Any plans for basic anti-cheat (flying detection, speed hacks, etc.)?
Not in this plugin

- Statistics: Consider adding a leaderboard system for credits, wins, kills, etc.
In the future, a website will be available for players to see their statistics.

- Configuration: You mention several config variables (like %spawn_radius, %feast_items) - are you planning a comprehensive config.yml?
Yes! The plugin should create a default config.yml in its directory to enable configuration to be adjusted.

- Sound Effects: Some kits mention sounds (berserker rage mode) - are you planning custom sound effects or using vanilla Minecraft sounds?
Vanilla sounds only

- Administrators: Is there a command to give credits ?
Yes! The /credits give player amount, is avaible with the permission hungergames.credits.give.
A user can see the credits available in the item menus and wung the /credits command (hungergames.credits), or the /credits player (hungergames.credits.other perm)

- Any other commands ?
Yes, there are some for players that aren't able to use the item based GUI :
- /kit : opens the kit menu (hungergames.kit)
- /kit kit_name : selects a given kit (hungergames.kit.select)
- /compass enemies,party,spawn,feast : selects a given compass mode (hungergames.compass.select)
- /spectate player : teleports you to another player if you are dead  (hungergames.spectate)