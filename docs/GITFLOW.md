# Gitflow Branching & Debug APK CI Guide

ChargePulse uses a classic **Gitflow** branching strategy wired directly to GitHub Actions (`.github/workflows/build-debug-apk.yml`) so every feature, integration, release, and hotfix branch automatically runs unit tests and produces a downloadable `app-debug.apk` artifact.

## Branch Structure

| Branch | Purpose | CI Trigger |
| :--- | :--- | :--- |
| `main` | Production-ready releases | Runs unit tests + builds Debug APK artifact |
| `develop` | Integration branch for next release | Runs unit tests + builds Debug APK artifact |
| `feature/<name>` | New features branched off `develop` | Push builds Debug APK; PR into `develop` validates |
| `release/<version>` | Pre-release QA branched off `develop` | Push & PR into `main` / `develop` build Debug APK |
| `hotfix/<name>` | Urgent fixes branched off `main` | Push builds Debug APK; merge back to `main` & `develop` |

## Quick Gitflow Setup Commands

Once Git and a GitHub remote are configured locally:

```bash
# 1. Initialize main and develop branches
git init -b main
git add .
git commit -m "chore: initialize ChargePulse Android project with Gitflow CI"
git branch develop

# 2. Push main and develop to GitHub
git remote add origin https://github.com/<your-username>/ChargePulse.git
git push -u origin main
git push -u origin develop

# 3. Start a new feature branch (triggers Debug APK build on push)
git checkout -b feature/watt-gauge develop
git push -u origin feature/watt-gauge
```

## Downloading the Debug APK from GitHub

1. Open your repository on GitHub and click the **Actions** tab.
2. Select the latest **Gitflow Debug APK Build** run.
3. Scroll to **Artifacts** and download **`ChargePulse-debug-apk-<run_number>`**.
4. Extract `app-debug.apk` and install it on your Android device (`adb install app-debug.apk` or tap the APK on your phone).
