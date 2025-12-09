
package com.ndjc.core.skeleton

/** Design token bridge: UI pack supplies values, skeleton consumes names only */
data class Tokens(
    val density: String = "comfortable",
    val theme: String = "system",
)
