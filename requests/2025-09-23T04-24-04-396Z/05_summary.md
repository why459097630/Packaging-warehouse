# NDJC Run 2025-09-23T04-24-04-396Z

- mode: **A**
- allowCompanions: **false**
- template: **circle-basic**
- appName: **NDJC core**
- packageId: **com.ndjc.demo.core**
- repo: `/var/task`
- templates: **remote** @ `/tmp/ndjc-templates/2025-09-23T04-24-04-396Z/templates`

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
- `NDJC:APP_LABEL@app` @ `src/main/AndroidManifest.xml` → replaced=1, found=true
- `NDJC:APP_LABEL` @ `src/main/AndroidManifest.xml` → replaced=1, found=true
- `NDJC:PACKAGE_NAME` @ `src/main/AndroidManifest.xml` → replaced=1, found=true
- `NDJC:APP_LABEL` @ `src/main/res/values/strings.xml` → replaced=1, found=true
- `NDJC:HOME_TITLE` @ `src/main/res/values/strings.xml` → replaced=1, found=true
- `NDJC:PRIMARY_BUTTON_TEXT` @ `src/main/res/values/strings.xml` → replaced=1, found=true
- `NDJC:PACKAGE_NAME` @ `build.gradle` → replaced=1, found=true
- `NDJC:PACKAGE_NAME` @ `src/main/java/com/ndjc/app/App.kt` → replaced=1, found=true
- `NDJC:PACKAGE_NAME` @ `src/main/java/com/ndjc/app/MainActivity.kt` → replaced=1, found=true
