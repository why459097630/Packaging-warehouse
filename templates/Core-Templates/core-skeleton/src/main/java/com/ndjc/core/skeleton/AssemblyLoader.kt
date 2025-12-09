
package com.ndjc.core.skeleton

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader

fun loadAssemblyFromAssets(context: Context, assetPath: String): Assembly {
    val am = context.assets
    am.open(assetPath).use { ins ->
        return Gson().fromJson(InputStreamReader(ins), Assembly::class.java)
    }
}
