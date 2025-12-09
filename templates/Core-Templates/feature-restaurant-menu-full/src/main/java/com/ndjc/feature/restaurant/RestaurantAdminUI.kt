package com.ndjc.ui.neu.entry.restaurant  // 和 RestaurantHomeUI.kt 保持一致

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ndjc.ui.neu.components.restaurant.RestaurantAdminActions
import com.ndjc.ui.neu.components.restaurant.RestaurantAdminNeu
import com.ndjc.ui.neu.components.restaurant.RestaurantAdminUiState

@Composable
fun RestaurantAdminUI(
    uiState: RestaurantAdminUiState,
    actions: RestaurantAdminActions,
    modifier: Modifier = Modifier
) {
    RestaurantAdminNeu(
        uiState = uiState,
        actions = actions,
        modifier = modifier
    )
}
