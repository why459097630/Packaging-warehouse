package com.ndjc.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 为 Context 创建 DataStore 委托（文件名可自定义）
private const val DS_NAME = "settings"
val Context.dataStore by preferencesDataStore(name = DS_NAME)

object ThemePrefs {
    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    private val KEY_ACCENT    = intPreferencesKey("accent_color")

    /** 是否深色模式 */
    fun isDark(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs: Preferences ->
            prefs[KEY_DARK_MODE] ?: false
        }

    /** 设置深色模式 */
    suspend fun setDark(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = value }
    }

    /** 强调色（示例：0 表示默认） */
    fun accent(context: Context): Flow<Int> =
        context.dataStore.data.map { prefs: Preferences ->
            prefs[KEY_ACCENT] ?: 0
        }

    suspend fun setAccent(context: Context, value: Int) {
        context.dataStore.edit { it[KEY_ACCENT] = value }
    }
}
