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

find "$wiki_checkout/repository" -maxdepth 1 -type f -name '*.md' -delete
cp "$repository_root"/docs/wiki/*.md "$wiki_checkout/repository/"

git -C "$wiki_checkout/repository" add --all
if git -C "$wiki_checkout/repository" diff --cached --quiet; then
    echo "Wiki is already up to date."
    exit 0
fi

git -C "$wiki_checkout/repository" commit --signoff -m "docs: synchronize project wiki"
git -C "$wiki_checkout/repository" push origin HEAD:master
