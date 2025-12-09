
# NDJC Base Skeleton (Contract-UI v1) — Jetpack Compose (Standard Edition)

This repository is a **Base Skeleton** that implements the stable contract for NDJC:
- 7 core slots: `header, hero, primary, secondary, detail, sheet, tabBar`
- 3 navigation abilities: `navigate(routeId, params)`, `openSheet(sheetId, args)`, `back()`
- 5 runtime hooks: `onAppStart, onEnterRoute, onLeaveRoute, onForeground, onBackground`
- Design Tokens entry (no concrete values)
- Assembly Manifest + **machine checks** (standard edition)
- Demo UI Pack + Demo Feature module to run first screen

> Notes
> - Gradle Wrapper JAR is not included to keep the ZIP small. After unzipping, run:
>   - **PowerShell (Windows):**
>     ```powershell
>     .\gradlew.bat wrapper
>     .\gradlew.bat :app:assembleDebug
>     ```
>   - Or if you have gradle installed: `gradle wrapper`
> - Android Studio: open the folder and let it sync. Compile SDK 34+ is expected.

## Structure
```
app/                    # Host app (depends on core-skeleton + chosen UI pack + chosen features)
core-skeleton/          # Contract-UI v1 implementation (slots, nav, hooks, tokens bridge)
ui-pack-demo/           # Demo UI pack (12 components skin) for quick run
feature-demo/           # Demo feature module (routes + supportedSlots)
assembly/assembly.json  # Example assembly manifest
tools/validator/        # Lightweight machine checks (PowerShell)
```

## Quick Start
1. Unzip
2. PowerShell:
   ```powershell
   .\gradlew.bat wrapper
   .\gradlew.bat :app:installDebug
   ```

## License
MIT (for demo assets).
