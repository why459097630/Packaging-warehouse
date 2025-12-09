
package com.ndjc.core.skeleton

/** Contract-UI v1: fixed slot names */
object Slots {
    const val HEADER = "header"
    const val HERO = "hero"
    const val PRIMARY = "primary"
    const val SECONDARY = "secondary"
    const val DETAIL = "detail"
    const val SHEET = "sheet"
    const val TAB_BAR = "tabBar"
    // optional
    const val FAB = "fab"
    const val DIALOG = "dialog"
    const val SETTINGS = "settings"
}

data class SlotMeta(
    val name: String,
    val layoutIntent: String, // single / grid / full / sidebar
    val scrollable: Boolean,
    val priority: String, // entry / secondary / aux
    val gestures: List<String> = emptyList(), // collapsible / sticky / pageable
)

/** Routes & navigation */
data class Route(
    val routeId: String,
    val entry: Boolean = false,
    val params: Map<String, String> = emptyMap(), // name -> type
    val deepLinks: List<String> = emptyList(),
    val returnPolicy: String = "back" // back / close / replace
)

/** Module manifest subset to host feature demo */
data class ModuleDecl(
    val moduleId: String,
    val type: String, // feature-ui / flow / service
    val supportedSlots: List<String>,
    val routes: List<Route>,
)

/** Assembly manifest */
data class Assembly(
    val template: String,
    val uiPack: String,
    val modules: List<String>,
    val routingEntry: String,
    val locale: String? = null,
    val theme: String? = null,
    val density: String? = null
)
