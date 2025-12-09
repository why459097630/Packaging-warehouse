
package com.ndjc.core.skeleton

interface Hooks {
    fun onAppStart() {}
    fun onForeground() {}
    fun onBackground() {}
    fun onEnterRoute(routeId: String, params: Map<String, Any?> = emptyMap()) {}
    fun onLeaveRoute(routeId: String) {}
}
