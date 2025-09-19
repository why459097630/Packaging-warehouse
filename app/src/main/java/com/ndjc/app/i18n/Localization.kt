package com.ndjc.app.i18n

import android.content.Context
import android.os.LocaleList
import android.content.res.Configuration
import java.util.Locale

object Localization {
    // 运行时切换/应用语言等逻辑在此注入

    fun wrapContext(base: Context, langTag: String?): Context {
        if (langTag.isNullOrBlank()) return base
        val locale = Locale.forLanguageTag(langTag)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}
