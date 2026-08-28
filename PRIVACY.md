# Privacy

Effective date: 27 August 2026

Redstone Utils does not include telemetry, analytics, advertising, an update tracker, or automatic crash reporting. The mod does not send data to the project maintainer or to an external web service.

## Local data

The mod stores the following JSON files in the Minecraft `config` directory:

- `redstone_utils.json`: client settings and profile keys;
- `redstone_utils_macros.json`: user-created macros and key bindings;
- `redstone_utils_macros_export.json`: an export created only when the user requests one;
- `redstone_utils-server.json`: server-side permissions and limits.

Client profiles use the multiplayer server address or the singleplayer save-directory name as a local profile key. Macro data may contain commands or other text entered by the user. These files remain on the machine where the mod runs. Valid `.bak` files and timestamped `.corrupt-*.bak` recovery copies may also exist.

## Multiplayer traffic

When connected to a server with Redstone Utils installed, the mod exchanges only gameplay payloads with that connected Minecraft server. These payloads cover backend detection, AutoWire mode and feedback, teleport requests, and client UI actions. The mod does not forward them to the project maintainer.

Server owners and third-party hosting providers may independently log connections, commands, chat, or gameplay according to their own policies. Redstone Utils does not control those systems.

## Removal and reports

Exit Minecraft before deleting the files listed above and their `.bak` or `.corrupt-*.bak` copies. They will be recreated with defaults when needed. For a private privacy or security report, use [GitHub private reporting](https://github.com/Redstone787/RedstoneUtils/security/advisories/new) rather than a public issue.
