#!/usr/bin/env bash

REV=$(git rev-parse HEAD)
URL=$(git config --get remote.origin.url)
STATUS=$(git status --porcelain)
if [ -n "$STATUS" ]; then
    REV="$REV (locally modified)"
fi

echo "Generating scm-source.json..."
echo '{"url": "git:'${URL}'", "revision": "'${REV}'", "author": "'${USER}'", "status": "'${STATUS}'"}' > scm-source.json
