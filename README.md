# NDS Web View

### Work in progress.

A Minecraft Fabric mod that adds fully functional, interactive web browsers into the game world.
Using [mcef-modern](https://github.com/DimasKama/mcef-modern) library written by [DimasKama](https://github.com/DimasKama).

## Current Features
* **In-World Browsing:** Spawn multiple browser screens anywhere in the world.
* **Multi-Screen Support:** Create, manage, and render multiple independent screens of varying sizes simultaneously.
* **Proximity Audio:** Dynamic, distance-based volume control that naturally fades out as you walk away from a screen.
* **Interactive:** Fully interactable using mouse clicks, scrolling, and keyboard typing (with focus states).

## Commands
* `/nwv create <name> <width> <height> <url>` - Spawns a new browser screen facing you.
* `/nwv seturl <name> <url>` - Changes the URL of a specific existing screen.
* `/nwv toggle <name>` - Turns a specific screen on or off.

## Dependencies
To use this mod, the following must be installed:
* [Fabric API](https://modrinth.com/mod/fabric-api)
* [MCEF Modern](https://modrinth.com/mod/mcef-modern)

## License
This project is distributed under the MIT License.

# To-do
* **Vanilla-Client Compatibility:** Guard payload sending so players without the mod can join the server without connection errors - screens simply won't render for them.
* **Multiplayer Interaction Sync:** Synchronize clicks, scrolling, and typing across all players in the server so everyone sees the exact same interaction.
* **Persistent State:** Ensure all screens and their playing states are perfectly saved and reloaded across server restarts.
* **Advanced Audio Falloff:** Implement exponential volume drop-off for a more realistic acoustic environment.
* **In-Game GUI:** A management screen to easily adjust browser properties, positions, and sizes without using chat commands.
* **Permission Gating:** Restrict commands to operators (or a configurable permission level) - currently any player can spam-create screens or hijack existing ones.
* **URL Safety Allow-list:** Reject non-http(s) schemes (`file://`, `javascript:`, local/private IP ranges and other malicious links) in commands to prevent players forcing other clients embedded browsers to load malicious or unintended local resources.
* **Rate-Limiting:** Add a short cooldown on commands per player to prevent griefing via rapid reload, which would hit every connected client's CPU (each runs its own CEF instance).
* **Popup/New-Window Blocking:** Investigate CEF's popup/new-window handling so a malicious or ad-heavy site can't spam additional windows or redirect unexpectedly.
* **Sodium/Iris/Vulkan Compatibility Testing:** Verify the custom render pipeline still works correctly alongside these rendering mods.
* **Known Issues:** ENTER/TAB/ESCAPE keys don't currently work inside the embedded browser (only character input does).