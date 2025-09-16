#!/bin/bash

# Release script for Salt-Minion Universal Installer
# Usage: ./scripts/release.sh <version>

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION=$1
TAG="v$VERSION"

echo "🚀 Preparing release $TAG"

# Validate version format
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "❌ Invalid version format. Use semantic versioning (e.g., 1.0.0)"
    exit 1
fi

# Check if working directory is clean
if [ -n "$(git status --porcelain)" ]; then
    echo "❌ Working directory is not clean. Please commit or stash changes."
    exit 1
fi

# Update version in build.gradle.kts
echo "📝 Updating version in build.gradle.kts"
sed -i "s/version = \".*\"/version = \"$VERSION\"/" build.gradle.kts

# Update CHANGELOG.md
echo "📝 Updating CHANGELOG.md"
DATE=$(date +%Y-%m-%d)
sed -i "s/## \[Unreleased\]/## [Unreleased]\n\n## [$VERSION] - $DATE/" CHANGELOG.md

# Commit version bump
echo "📦 Committing version bump"
git add build.gradle.kts CHANGELOG.md
git commit -m "chore: bump version to $VERSION"

# Create and push tag
echo "🏷️  Creating tag $TAG"
git tag -a "$TAG" -m "Release $TAG"

echo "⬆️  Pushing changes and tag"
git push origin main
git push origin "$TAG"

echo "✅ Release $TAG created successfully!"
echo ""
echo "🎉 The GitHub Actions workflow will now:"
echo "   1. Build executables for Linux and Windows"
echo "   2. Run tests"
echo "   3. Create a GitHub release with downloadable artifacts"
echo "   4. Generate release notes automatically"
echo ""
echo "📋 You can monitor the build progress at:"
echo "   https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:/]\([^.]*\).*/\1/')/actions"