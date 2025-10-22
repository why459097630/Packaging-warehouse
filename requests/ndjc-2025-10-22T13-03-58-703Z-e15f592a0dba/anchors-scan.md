# Anchors Scan (baseline=registry.circle-basic.json)

template: circle-basic | runId: ndjc-2025-10-22T13-03-58-703Z-e15f592a0dba

| Anchor | InTemplate | InSanitized | File |
|---|---|---|---|
|  type: string(packageId) | example: com.ndjc.demo.core|NDJC:PACKAGE_NAME // desc: Android applicationId / package name | type: string(packageId) | example: com.ndjc.demo.core | MISS | NO |  |
|  type: string | example: NDJC App|NDJC:APP_LABEL // desc: App 显示名称 | type: string | example: NDJC App | MISS | NO |  |
|  type: string | example: Home|NDJC:HOME_TITLE // desc: 首页标题 | type: string | example: Home | MISS | NO |  |
|  type: string | example: Create|NDJC:PRIMARY_BUTTON_TEXT // desc: 首页主按钮文案 | type: string | example: Create | MISS | NO |  |
|  type: enum(Light|Dark|System) | example: Light|NDJC:THEME_NAME // desc: 主题名称 | type: enum(Light|Dark|System) | example: Light | MISS | NO |  |
|  type: boolean | example: true|NDJC:DARK_MODE // desc: 是否启用深色模式 | type: boolean | example: true | MISS | NO |  |
|  type: string | example: Inter|NDJC:FONT_FAMILY_PRIMARY // desc: 主字体族 | type: string | example: Inter | MISS | NO |  |
|  type: string | example: Roboto|NDJC:FONT_FAMILY_SECONDARY // desc: 次字体族 | type: string | example: Roboto | MISS | NO |  |
|  type: boolean | example: true|NDJC:I18N_ENABLED // desc: 是否启用多语言 | type: boolean | example: true | MISS | NO |  |
|  type: boolean | example: true|NDJC:ANIM_ENABLED // desc: 是否启用动效 | type: boolean | example: true | MISS | NO |  |
|  type: number | example: 300|NDJC:ANIM_DURATION_MS // desc: 动效时长(毫秒) | type: number | example: 300 | MISS | NO |  |
|  type: enum(ldpi|mdpi|hdpi|xhdpi|xxhdpi) | example: mdpi|NDJC:LAYOUT_DENSITY // desc: 版式密度/目标密度 | type: enum(ldpi|mdpi|hdpi|xhdpi|xxhdpi) | example: mdpi | MISS | NO |  |
|  type: string(url) | example: https://api.example.com|NDJC:DATA_SOURCE // desc: 远端数据源根URL | type: string(url) | example: https://api.example.com | MISS | NO |  |
|  type: boolean | example: true|NDJC:SEED_ENABLED // desc: 是否启用种子数据 | type: boolean | example: true | MISS | NO |  |
|  type: enum(date|popularity|rating) | example: date|NDJC:FEED_SORT // desc: 列表排序策略 | type: enum(date|popularity|rating) | example: date | MISS | NO |  |
|  type: number | example: 10|NDJC:PAGING_SIZE // desc: 分页大小 | type: number | example: 10 | MISS | NO |  |
|  type: enum(cache|network|mixed) | example: cache|NDJC:OFFLINE_POLICY // desc: 离线策略 | type: enum(cache|network|mixed) | example: cache | MISS | NO |  |
|  type: string(https-url) | example: https://example.com/privacy|NDJC:PRIVACY_POLICY // desc: 隐私政策URL | type: string(https-url) | example: https://example.com/privacy | MISS | NO |  |
|  type: boolean | example: true|NDJC:DRAFT_ENABLED // desc: 是否启用草稿 | type: boolean | example: true | MISS | NO |  |
|  type: boolean | example: true|NDJC:USE_APP_BUNDLE // desc: 是否构建AAB | type: boolean | example: true | MISS | NO |  |
|  type: string | example: run_20251022_001|NDJC:BUILD_META:RUNID // desc: 本次构建运行ID | type: string | example: run_20251022_001 | MISS | NO |  |
|  type: number | example: 1|NDJC:FEATURE_TOGGLE:MIN // desc: 最小功能级别开关 | type: number | example: 1 | MISS | NO |  |
|  type: boolean | example: true|NDJC:DEBUG_LOG_ENABLED // desc: 调试日志开关 | type: boolean | example: true | MISS | NO |  |
|  type: enum(DEBUG|INFO|WARN|ERROR) | example: INFO|NDJC:LOG_LEVEL // desc: 日志级别 | type: enum(DEBUG|INFO|WARN|ERROR) | example: INFO | MISS | NO |  |
|  type: boolean | example: true|NDJC:CRASHLYTICS_ENABLED // desc: 崩溃上报开关 | type: boolean | example: true | MISS | NO |  |
|  type: string | example: exp-001|NDJC:EXPERIMENT_BUCKET // desc: 实验分桶名 | type: string | example: exp-001 | MISS | NO |  |
|  type: object({primary:#RRGGBB,secondary:#RRGGBB}) | example: {"primary":"#7C3AED","secondary":"#10B981"}|NDJC:THEME_COLORS // desc: 主题主副色 | type: object({primary:#RRGGBB,secondary:#RRGGBB}) | example: {"primary":"#7C3AED","secondary":"#10B981"} | MISS | NO |  |
|  type: string | example: none|NDJC:TYPOCASE_OVERRIDES // desc: 文案大小写覆盖策略 | type: string | example: none | MISS | NO |  |
|  type: object | example: {"extra":"value"}|NDJC:STRINGS_EXTRA // desc: 额外字符串资源 | type: object | example: {"extra":"value"} | MISS | NO |  |
|  type: string(route-id) | example: home|NDJC:ROUTE_HOME // desc: 首页路由ID | type: string(route-id) | example: home | MISS | NO |  |
|  type: string(route-id) | example: detail|NDJC:ROUTE_DETAIL // desc: 详情页路由ID | type: string(route-id) | example: detail | MISS | NO |  |
|  type: block:compose | example: Column { /* header */ }|BLOCK:HOME_HEADER // desc: 首页头部可替换UI片段 | type: block:compose | example: Column { /* header */ } | MISS | NO |  |
|  type: block:compose | example: Column { /* content */ }|BLOCK:HOME_BODY // desc: 首页主体内容片段 | type: block:compose | example: Column { /* content */ } | MISS | NO |  |
|  type: block:compose | example: Row { /* actions */ }|BLOCK:HOME_ACTIONS // desc: 首页操作区片段(按钮/动作) | type: block:compose | example: Row { /* actions */ } | MISS | NO |  |
|  type: block:compose | example: Box { /* splash */ }|BLOCK:SPLASH_CONTENT // desc: 启动页内容片段 | type: block:compose | example: Box { /* splash */ } | MISS | NO |  |
|  type: block:compose | example: Column { /* empty */ }|BLOCK:EMPTY_STATE // desc: 空数据态片段 | type: block:compose | example: Column { /* empty */ } | MISS | NO |  |
|  type: block:compose | example: Column { /* error */ }|BLOCK:ERROR_STATE // desc: 错误态片段 | type: block:compose | example: Column { /* error */ } | MISS | NO |  |
|  type: block:compose | example: Column { /* debug */ }|BLOCK:DEBUG_PANEL // desc: 调试信息面板片段 | type: block:compose | example: Column { /* debug */ } | MISS | NO |  |
|  type: block:compose | example: Box { /* ad */ }|BLOCK:HEADER_AD_SLOT // desc: 头部广告位片段 | type: block:compose | example: Box { /* ad */ } | MISS | NO |  |
|  type: block:compose | example: Card { /* profile */ }|BLOCK:USER_PROFILE_CARD // desc: 用户卡片片段 | type: block:compose | example: Card { /* profile */ } | MISS | NO |  |
|  type: block:compose | example: Card { /* sponsored */ }|BLOCK:SPONSORED_CARD // desc: 商业赞助卡片片段 | type: block:compose | example: Card { /* sponsored */ } | MISS | NO |  |
|  type: block:compose | example: Column { /* profile section */ }|BLOCK:PROFILE_SECTION // desc: 个人资料区片段 | type: block:compose | example: Column { /* profile section */ } | MISS | NO |  |
|  type: block:compose | example: Column { /* settings */ }|BLOCK:SETTINGS_SECTION // desc: 设置页片段 | type: block:compose | example: Column { /* settings */ } | MISS | NO |  |
|  type: block:compose | example: Column { /* settings extra */ }|BLOCK:SETTINGS_SECTION:EXTRA // desc: 设置页额外扩展片段 | type: block:compose | example: Column { /* settings extra */ } | MISS | NO |  |
|  type: list<string(route-id)> | example: ["home","detail"]|LIST:ROUTES // desc: 路由ID列表 | type: list<string(route-id)> | example: ["home","detail"] | MISS | NO |  |
|  type: list<string> | example: ["implementation 'androidx.core:core-ktx:1.10.0'"]|LIST:DEPENDENCY_SNIPPETS // desc: 额外Gradle依赖行 | type: list<string> | example: ["implementation 'androidx.core:core-ktx:1.10.0'"] | MISS | NO |  |
|  type: list<string> | example: ["-keep class com.ndjc.** { *; }"]|LIST:PROGUARD_EXTRA // desc: 额外Proguard规则 | type: list<string> | example: ["-keep class com.ndjc.** { *; }"] | MISS | NO |  |
|  type: list<string> | example: ["resources.exclude META-INF/DEPENDENCIES"]|LIST:PACKAGING_RULES // desc: 打包规则(排除/合并等) | type: list<string> | example: ["resources.exclude META-INF/DEPENDENCIES"] | MISS | NO |  |
|  type: list<string> | example: ["title","description"]|LIST:POST_FIELDS // desc: 发布/表单字段列表 | type: list<string> | example: ["title","description"] | MISS | NO |  |
|  type: list<string> | example: ["items_count"]|LIST:PLURAL_STRINGS // desc: 需复数化的字符串键 | type: list<string> | example: ["items_count"] | MISS | NO |  |
|  type: list<string> | example: ["material3"]|LIST:COMPONENT_STYLES // desc: 组件风格标识 | type: list<string> | example: ["material3"] | MISS | NO |  |
|  type: list<string(url|pattern)> | example: ["https://example.com/home"]|LIST:DEEPLINK_PATTERNS // desc: DeepLink URL 模式 | type: list<string(url|pattern)> | example: ["https://example.com/home"] | MISS | NO |  |
|  type: list<string(domain)> | example: ["api.example.com"]|LIST:NETWORK_CLEAR_TEXT // desc: 需允许HTTP明文的域名 | type: list<string(domain)> | example: ["api.example.com"] | MISS | NO |  |
|  type: list<string(locale)> | example: ["en","zh-CN"]|LIST:RES_CONFIGS_OVERRIDE // desc: 追加的语言资源配置 | type: list<string(locale)> | example: ["en","zh-CN"] | MISS | NO |  |
|  type: boolean | example: true|IF:NAV_TRANSITIONS // desc: 是否启用导航转场 | type: boolean | example: true | MISS | NO |  |
|  type: boolean | example: true|IF:BUILD_SUMMARY // desc: 构建结束输出摘要 | type: boolean | example: true | MISS | NO |  |
|  type: boolean | example: false|IF:NATIVE_SPLITS // desc: 是否启用ABI拆分 | type: boolean | example: false | MISS | NO |  |
|  type: boolean | example: true|IF:AFTER_BUILD // desc: 构建后执行hook | type: boolean | example: true | MISS | NO |  |
|  type: boolean | example: false|IF:PRE_COMMIT // desc: 提交前执行hook | type: boolean | example: false | MISS | NO |  |
|  type: boolean | example: true|IF:AFTER_INSTALL // desc: 安装后执行hook | type: boolean | example: true | MISS | NO |  |
|  type: boolean | example: true|IF:BEFORE_BUILD // desc: 构建前执行hook | type: boolean | example: true | MISS | NO |  |
|  type: boolean | example: true|IF:POST_INJECT // desc: 模板注入后执行hook | type: boolean | example: true | MISS | NO |  |
|  type: boolean | example: false|IF:BEFORE_BUNDLE // desc: 打AAB前执行hook | type: boolean | example: false | MISS | NO |  |
|  type: boolean | example: false|IF:AFTER_BUNDLE // desc: 打AAB后执行hook | type: boolean | example: false | MISS | NO |  |
|  type: hook:enum(noop|enabled|emit_event:NAME) | example: noop|HOOK:INTENT_SHARE // desc: 分享意图hook | type: hook:enum(noop|enabled|emit_event:NAME) | example: noop | MISS | NO |  |
|  type: hook:enum(noop|enabled|emit_event:NAME) | example: enabled|HOOK:FILE_PROVIDER // desc: 文件提供器hook | type: hook:enum(noop|enabled|emit_event:NAME) | example: enabled | MISS | NO |  |
|  type: hook:enum(noop|enabled|emit_event:NAME) | example: emit_event:PERM_REQUESTED|HOOK:PERMISSIONS:ON_REQUEST // desc: 动态权限请求hook | type: hook:enum(noop|enabled|emit_event:NAME) | example: emit_event:PERM_REQUESTED | MISS | NO |  |
|  type: hook:enum(noop|enabled|emit_event:NAME) | example: noop|HOOK:PRE_COMMIT:HOOK // desc: 提交前hook实现 | type: hook:enum(noop|enabled|emit_event:NAME) | example: noop | MISS | NO |  |
|  type: hook:enum(noop|enabled|emit_event:NAME) | example: emit_event:BUILD_DONE|HOOK:POST_BUILD:HOOK // desc: 构建后hook实现 | type: hook:enum(noop|enabled|emit_event:NAME) | example: emit_event:BUILD_DONE | MISS | NO |  |
|  type: hook:enum(noop|enabled|emit_event:NAME) | example: enabled|HOOK:AFTER_INSTALL:HOOK // desc: 安装后hook实现 | type: hook:enum(noop|enabled|emit_event:NAME) | example: enabled | MISS | NO |  |

Total: 70 | HIT: 0 | MISS: 70

_Non-blocking preflight; this step never fails the build._
