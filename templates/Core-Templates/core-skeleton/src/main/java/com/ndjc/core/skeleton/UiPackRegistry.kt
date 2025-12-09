package com.ndjc.core.skeleton

/**
 * 描述一个 UI 包在骨架里的元信息。
 */
data class UiPackDescriptor(
    val id: String,
    val label: String,
    val supportsDarkTheme: Boolean = true,
    val supportsDynamicColor: Boolean = false
)

/**
 * NDJC 核心模板内置 UI 包注册表。
 */
val UIPACK_REGISTRY: List<UiPackDescriptor> = listOf(
    UiPackDescriptor(
        id = "ui-pack-neumorph",
        label = "Neumorph UI Pack",
        supportsDarkTheme = true,
        supportsDynamicColor = false
    )
)

/**
 * 根据 id 查找 UI 包描述。
 */
fun findUiPackById(id: String): UiPackDescriptor? =
    UIPACK_REGISTRY.firstOrNull { it.id == id }
