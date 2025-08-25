package com.ndjc.generated

data class Catalog(
    var title: String? = null,
    var generatedAt: String? = null,
    var template: String? = null,
    var smart: Boolean = false,
    var models: MutableList<Model> = mutableListOf()
)
