package com.ndjc.ui.neu.components.restaurant

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RestaurantHomeUI(
    uiState: RestaurantHomeUiState,
    actions: RestaurantHomeActions,
    modifier: Modifier = Modifier
) {
    RestaurantHomeNeu(uiState = uiState, actions = actions, modifier = modifier)
}

@Composable
fun RestaurantLoginUI(
    uiState: RestaurantLoginUiState,
    actions: RestaurantLoginActions,
    modifier: Modifier = Modifier
) {
    RestaurantLoginNeu(uiState = uiState, actions = actions, modifier = modifier)
}

@Composable
fun RestaurantAdminUI(
    uiState: RestaurantAdminUiState,
    actions: RestaurantAdminActions,
    modifier: Modifier = Modifier
) {
    RestaurantAdminNeu(uiState = uiState, actions = actions, modifier = modifier)
}

@Composable
fun RestaurantEditDishUI(
    uiState: RestaurantEditDishUiState,
    actions: RestaurantEditDishActions,
    modifier: Modifier = Modifier
) {
    RestaurantEditDishNeu(uiState = uiState, actions = actions, modifier = modifier)
}

@Composable
fun RestaurantDetailUI(
    uiState: RestaurantDetailUiState,
    actions: RestaurantDetailActions,
    modifier: Modifier = Modifier
) {
    RestaurantDishDetailNeu(uiState = uiState, actions = actions, modifier = modifier)
}
