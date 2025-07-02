#!/bin/bash
set -e

read -p "Build status (stable, beta, exp): " STATUS

cd ..
CURRENT_VERSION=$(grep "mcVersion=" gradle.properties | cut -d "=" -f2)
CURRENT_VERSION_CUT=$(echo $CURRENT_VERSION | tr -d '.')

if [ $STATUS == "stable" ]; then
    BUILD_TAGS=$(git tag -l "${CURRENT_VERSION_CUT}_b*" | grep -v "exp")
    LATEST_BUILD_NUM=$(echo "$BUILD_TAGS" | sed -E "s/^${CURRENT_VERSION_CUT}_b([0-9]+)$/\1/" | sort -n | tail -n1)
    LATEST_BUILD_NUM=${LATEST_BUILD_NUM:-0}
    NEW_BUILD_NUM=$(($LATEST_BUILD_NUM + 1))
    NEW_BUILD="${CURRENT_VERSION_CUT}_b${NEW_BUILD_NUM}"
    LATEST_BUILD="${CURRENT_VERSION_CUT}_b${LATEST_BUILD_NUM}"
elif [ $STATUS == "exp" ]; then
    BUILD_TAGS=$(git tag -l "${CURRENT_VERSION_CUT}_b*")
    LATEST_BUILD_NUM=$(echo "$BUILD_TAGS" | sed -E "s/^${CURRENT_VERSION_CUT}_b([0-9]+)$/\1/" | sort -n | tail -n1)
    LATEST_BUILD_NUM=${LATEST_BUILD_NUM:-0}
    NEW_BUILD_NUM=$(($LATEST_BUILD_NUM + 1))
    NEW_BUILD="${CURRENT_VERSION_CUT}_b${NEW_BUILD_NUM}exp"
    LATEST_BUILD="${CURRENT_VERSION_CUT}_b${LATEST_BUILD_NUM}exp"
elif [ $STATUS == "beta" ]; then
    BUILD_TAGS=$(git tag -l "${CURRENT_VERSION_CUT}_beta*")
    LATEST_BUILD_NUM=$(echo "$BUILD_TAGS" | sed -E "s/^${CURRENT_VERSION_CUT}_beta([0-9]+)$/\1/" | sort -n | tail -n1)
    LATEST_BUILD_NUM=${LATEST_BUILD_NUM:-0}
    NEW_BUILD_NUM=$(($LATEST_BUILD_NUM + 1))
    NEW_BUILD="${CURRENT_VERSION_CUT}_beta${NEW_BUILD_NUM}"
    LATEST_BUILD="${CURRENT_VERSION_CUT}_beta${LATEST_BUILD_NUM}"
fi

RELEASE_NOTES="RELEASE.md"

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "New build tag: $NEW_BUILD"


if git rev-parse "$LATEST_BUILD" >/dev/null 2>&1; then
    LAST_RELEASE_COMMIT=$(git rev-list -n 1 "$LATEST_BUILD")
else
    read -p "What was the previous version tag?: " PREV_VER

    LAST_RELEASE_COMMIT=$(git rev-list -n 1 "$PREV_VER")
    echo "No previous release tag found. Comparing between tags: $PREV_VER $LAST_RELEASE_COMMIT"
fi

COMMIT_LOG=$(git log "$LAST_RELEASE_COMMIT"..HEAD --pretty=format:"- [\`%h\`](https://github.com/${GITHUB_REPO}/commit/%H) %s (%an)")
if [ -z "$COMMIT_LOG" ]; then
  COMMIT_LOG="⚠️No new commits since $LATEST_TAG."
else
  echo "Commits log generated"
fi

echo "" >> $RELEASE_NOTES

read -p "Enter release notes: " NOTE
{
  echo ""
  echo "### 📜 Changelog:"
  echo ""
  echo "$COMMIT_LOG"
  echo ""
  if [ -n "$NOTE" ]; then
    echo "### 📝 Notes:"
    echo ""
    echo "$NOTE"
    echo ""
  fi
} >> "$RELEASE_NOTES"

file="pufferfork-server/build/libs/pufferfork-paperclip-${CURRENT_VERSION}-R0.1-SNAPSHOT-mojmap.jar"

git tag "$NEW_BUILD"
git push origin "$NEW_BUILD"

gh release create "$NEW_BUILD" "$file" --title "Pufferfork $CURRENT_VERSION Build $NEW_TAG" --notes-file "$RELEASE_NOTES"

rm -rf $RELEASE_NOTES

echo "🚀Released!"
