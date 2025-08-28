#!/bin/bash

# Appero Android SDK Publishing Script
# This script publishes the SDK to GitHub Packages

set -e

echo "🚀 Publishing Appero Android SDK to GitHub Packages..."

# Check if GitHub token is set
if [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ Error: GITHUB_TOKEN environment variable is not set"
    echo "Please set your GitHub Personal Access Token:"
    echo "export GITHUB_TOKEN=your_token_here"
    exit 1
fi

# Check if GitHub username is set
if [ -z "$GITHUB_USERNAME" ]; then
    echo "❌ Error: GITHUB_USERNAME environment variable is not set"
    echo "Please set your GitHub username:"
    echo "export GITHUB_USERNAME=your_username_here"
    exit 1
fi

echo "✅ Using GitHub username: $GITHUB_USERNAME"
echo "✅ GitHub token is configured"

# Clean and build
echo "🔨 Cleaning and building..."
./gradlew clean

# Publish to GitHub Packages
echo "📦 Publishing to GitHub Packages..."
./gradlew publishReleasePublicationToGitHubPackagesRepository

echo "✅ Successfully published to GitHub Packages!"
echo ""
echo "📋 Next steps:"
echo "1. Update your sample app's build.gradle.kts to use the published version"
echo "2. Test the integration"
echo "3. For Flutter, add the repository to pubspec.yaml"
echo ""
echo "🔗 Repository URL: https://maven.pkg.github.com/pocketworks/appero-sdk-android" 