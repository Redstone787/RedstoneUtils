# Contributing to Redstone Utils

Thank you for considering a contribution. By participating, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Before opening a pull request

1. Search existing issues and pull requests.
2. For large or compatibility-breaking work, open an issue before implementation.
3. Keep changes focused and include tests for behavior that can be tested without a running client.
4. Do not submit Minecraft assets, decompiled game code, secrets, personal data, or third-party material without a compatible license and recorded provenance.
5. Run `./gradlew build` with JDK 25.

## Licensing and sign-off

Contributions to covered source files are accepted under [MPL-2.0](LICENSE). Every commit must certify the unmodified [Developer Certificate of Origin 1.1](DCO) with a sign-off:

```shell
git commit --signoff
```

This adds `Signed-off-by: Name <email>` to the commit. The sign-off name and email become part of the permanent Git history and may become public. Use a verified GitHub `noreply` address if you do not want to expose a personal email.

The DCO is not a copyright assignment. It certifies that you have the right to submit the contribution under the project license.

## Development

```shell
./gradlew build
./gradlew runClient
./gradlew runServer
```

Use the existing formatting and naming conventions. Update the Wiki source under `docs/wiki/`, user-facing translations, privacy information, notices, and asset provenance whenever a change affects them.
