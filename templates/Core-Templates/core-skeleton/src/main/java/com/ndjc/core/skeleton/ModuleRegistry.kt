package com.ndjc.core.skeleton

/**
 * 描述一个功能模块在骨架里的 “对外身份”：
 *
 * - id:         对应 assembly.modules 里的字符串，例如 "feature-home-basic"
 * - entryRouteId:  该模块推荐的入口 routed（用于 App 入口）
 * - tabRoutes:  底栏里要展示的 routed 列表（顺序即显示顺序）
 */
data class ModuleDescriptor(
    val id: String,
    val entryRouteId: String? = null,
    val tabRoutes: List<String> = emptyList(),
)

/**
 * NDJC 核心模板内置模块注册表。
 *
 * 注意：这里只关心 “模块 ID → routeId 字符串”，
 * 不关心具体页面实现（页面实现交给各个 feature module 自己）。
 */
val MODULE_REGISTRY: List<ModuleDescriptor> = listOf(
ModuleDescriptor(
    id = "feature-restaurant-menu",
    entryRouteId = "home",
    tabRoutes = listOf("home"),  // 餐厅 App 不需要底栏，可以为空
),
)

/**
 * 根据当前 modules 决定应用入口路由。
 */
fun resolveEntryRouteFromModules(
    modules: List<String>,
    fallback: String,
): String {
    val fromModule = modules
        .mapNotNull { id -> MODULE_REGISTRY.firstOrNull { it.id == id }?.entryRouteId }
        .firstOrNull()

    return fromModule ?: fallback
}

/**
 * 根据当前 modules 生成 tabBar 要用的 routeId 列表。
 */
fun resolveTabRoutesFromModules(
    modules: List<String>,
    fallback: List<String>,
): List<String> {
    val tabRoutes = modules.flatMap { id ->
        MODULE_REGISTRY.firstOrNull { it.id == id }?.tabRoutes ?: emptyList()
    }.distinct()

    return if (tabRoutes.isEmpty()) fallback else tabRoutes
}
