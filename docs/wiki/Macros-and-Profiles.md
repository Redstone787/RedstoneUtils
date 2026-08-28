# Macros and Profiles

## Profiles

Client settings and macros use a global default profile plus automatically selected profiles for each multiplayer server and singleplayer world. Server profiles use the server address as a local key. Singleplayer profiles use the save-directory name rather than the editable world display name.

When a server/world profile is first created, settings and macros are copied from the global defaults. Later changes remain profile-specific. The active profile key is visible in the config, macro, and toolbox screens.

## Macro Manager

Open it with `/macro`, `/redstone_utils macro`, or the toolbox. Macros support:

- command aliases;
- keyboard or mouse bindings;
- `Ctrl`, `Shift`, `Alt`, and `Super` modifiers;
- pressed, released, or repeated-while-held triggers;
- search, categories, sorting, duplication, enable/disable, import/export, and delete confirmation.

The default export path is:

```text
config/redstone_utils_macros_export.json
```

Invalid, reserved, self-referencing, conflicting, or duplicate aliases/bindings are skipped or sanitized. Imported macros receive new IDs.

Macro files may contain commands or personal workflow text. Review them before sharing; see [Privacy and Data](Privacy-and-Data.md).
