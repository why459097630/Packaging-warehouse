package com.ndjc.feature.showcase

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ndjc.feature.showcase.ui.ShowcaseUiRenderer

object ShowcaseFeatureAssembly {

    @Composable
    fun AppRoot(
        nav: NavHostController,
        ui: ShowcaseUiRenderer,
    ) {
        ShowcaseHost(nav = nav, ui = ui)
    }
}
