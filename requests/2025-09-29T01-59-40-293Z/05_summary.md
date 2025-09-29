# NDJC Run 2025-09-29T01-59-40-293Z

- mode: **A**
- allowCompanions: **false**
- template: **circle-basic**
- appName: **Test App**
- packageId: **com.example.testapp**
- repo: `/var/task`
- templates: **remote** @ `/tmp/ndjc-templates/2025-09-29T01-59-40-293Z/templates`

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
- 03a2_seed_json.txt
- 03b_cleanup.txt
- 04_materialize.txt

## Anchor Changes
- `NDJC:PACKAGE_NAME` @ `/tmp/ndjc/app/src/main/AndroidManifest.xml` → replaced=1, found=true
- `NDJC:APP_LABEL` @ `/tmp/ndjc/app/src/main/res/values/strings.xml` → replaced=1, found=true
- `NDJC:HOME_TITLE` @ `/tmp/ndjc/app/src/main/res/values/strings.xml` → replaced=1, found=true
- `NDJC:PRIMARY_BUTTON_TEXT` @ `/tmp/ndjc/app/src/main/res/values/strings.xml` → replaced=1, found=true
- `NDJC:PACKAGE_NAME` @ `/tmp/ndjc/app/build.gradle` → replaced=1, found=true
- `NDJC:PACKAGE_NAME` @ `/tmp/ndjc/app/src/main/java/com/ndjc/app/App.kt` → replaced=1, found=true
- `NDJC:PACKAGE_NAME` @ `/tmp/ndjc/app/src/main/java/com/ndjc/app/MainActivity.kt` → replaced=1, found=true
