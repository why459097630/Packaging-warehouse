package com.ndjc.core.skeleton

/**
 * NDJC 骨架用的“导航接口”
 * - currentRouteId: 当前 route 的 id
 * - navigate(routeId): 切换到指定 route
 */
interface Navigator {
    val currentRouteId: String
    fun navigate(routeId: String)
}
