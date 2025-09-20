# NDJC Run 2025-09-20T03-33-00-993Z

- mode: **B**
- allowCompanions: **true**
- template: **core**
- appName: **NDJC core**
- packageId: **com.ndjc.demo.core**
- repo: `/var/task`
- templates: **remote** @ `/tmp/ndjc-templates/2025-09-20T03-33-00-993Z/templates`

## Artifacts
- 00_input.json
- 00_templates_source.json
- 00_checks.json
- 01_orchestrator_mode.txt
- 01_orchestrator.json
- 01a_llm_request.json / 01b_llm_response.json / 01c_llm_raw.txt / 01a_llm_trace.json
- 02_plan.json
- 03_apply_result.json
- 03a_companions_emitted.json / .txt
- 03b_cleanup.txt
- 04_materialize.txt

## Anchor Changes
- `NDJC:APP_LABEL` @ `/tmp/ndjc/app/src/main/res/values/strings.xml` → replaced=1, found=true
- `NDJC:HOME_TITLE` @ `/tmp/ndjc/app/src/main/res/values/strings.xml` → replaced=1, found=true
- `NDJC:MAIN_BUTTON` @ `/tmp/ndjc/app/src/main/res/values/strings.xml` → replaced=1, found=true
- `NDJC:APP_LABEL` @ `/tmp/ndjc/app/src/main/AndroidManifest.xml` → replaced=0, found=false
- `NDJC:BLOCK:LOCALE_CONFIG` @ `/tmp/ndjc/app/src/main/AndroidManifest.xml` → replaced=1, found=true
- `NDJC:BLOCK:PERMISSIONS` @ `/tmp/ndjc/app/src/main/AndroidManifest.xml` → replaced=1, found=true
- `NDJC:BLOCK:INTENT_FILTERS` @ `/tmp/ndjc/app/src/main/AndroidManifest.xml` → replaced=1, found=true
- `NDJC:PACKAGE_NAME` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:COMPILE_SDK` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:MIN_SDK` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:TARGET_SDK` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:VERSION_CODE` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:VERSION_NAME` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:PLUGINS_EXTRA` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:DEPENDENCIES_EXTRA` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:SIGNING_CONFIG` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:RES_CONFIGS` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:PROGUARD_FILES_EXTRA` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:PACKAGING_RULES` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:BLOCK:THEME_OVERRIDES` @ `/tmp/ndjc/app/src/main/res/values/themes.xml` → replaced=0, found=false
- `NDJC:HOME_TITLE` @ `/tmp/ndjc/app/src/main/java/com/ndjc/app/MainActivity.kt` → replaced=0, found=false
- `NDJC:MAIN_BUTTON` @ `/tmp/ndjc/app/src/main/java/com/ndjc/app/MainActivity.kt` → replaced=0, found=false
