package com.ndjc.feature.showcase

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ndjc.feature.showcase.ui.ShowcaseUiRenderer

@Composable
fun ShowcaseHost(
    nav: NavHostController,
    ui: ShowcaseUiRenderer,
) {
    ShowcaseHomeScreen(nav = nav, ui = ui)
}
