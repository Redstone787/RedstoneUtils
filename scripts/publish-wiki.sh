#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

set -euo pipefail

repository_root=$(git rev-parse --show-toplevel)
wiki_remote=${1:-https://github.com/Redstone787/RedstoneUtils.wiki.git}
wiki_checkout=$(mktemp -d)
trap 'rm -rf "$wiki_checkout"' EXIT

git clone "$wiki_remote" "$wiki_checkout/repository"

# A Wiki checkout is a separate repository and would otherwise inherit an
# unrelated global Git identity. Reuse the explicit identity and signing
# configuration from the source repository instead.
wiki_user_name=$(git -C "$repository_root" config user.name)
wiki_user_email=$(git -C "$repository_root" config user.email)
git -C "$wiki_checkout/repository" config user.name "$wiki_user_name"
git -C "$wiki_checkout/repository" config user.email "$wiki_user_email"

commit_options=(--signoff)
wiki_signing_format=$(git -C "$repository_root" config --get gpg.format || true)
wiki_signing_key=$(git -C "$repository_root" config --get user.signingkey || true)
if [[ -n "$wiki_signing_format" && -n "$wiki_signing_key" ]]; then
    git -C "$wiki_checkout/repository" config gpg.format "$wiki_signing_format"
    git -C "$wiki_checkout/repository" config user.signingkey "$wiki_signing_key"
    commit_options=(-S "${commit_options[@]}")
fi

find "$wiki_checkout/repository" -maxdepth 1 -type f -name '*.md' -delete
cp "$repository_root"/docs/wiki/*.md "$wiki_checkout/repository/"

git -C "$wiki_checkout/repository" add --all
if git -C "$wiki_checkout/repository" diff --cached --quiet; then
    echo "Wiki is already up to date."
    exit 0
fi

git -C "$wiki_checkout/repository" commit "${commit_options[@]}" -m "docs: synchronize project wiki"
git -C "$wiki_checkout/repository" push origin HEAD:master
