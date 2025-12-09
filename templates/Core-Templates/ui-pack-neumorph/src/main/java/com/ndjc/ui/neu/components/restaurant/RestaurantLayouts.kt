package com.ndjc.ui.neu.components.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextDecoration
import android.util.Log
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.ndjc.ui.neu.theme.NDJCTheme



// ---------- 局部 Token：只在本文件里用，保证 UI 数值不变 ----------
@Composable
private fun neuOutlinedTextFieldColors(): TextFieldColors =
    TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,

        // ✅ 关键：边框颜色改成黑色
        focusedIndicatorColor = Color.Black,
        unfocusedIndicatorColor = Color.Black.copy(alpha = 0.7f),
        disabledIndicatorColor = Color.Black.copy(alpha = 0.3f),
        errorIndicatorColor = MaterialTheme.colorScheme.error,

        // 字体颜色保持默认就行（也可以显式写）
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color.Black.copy(alpha = 0.5f),
        errorTextColor = MaterialTheme.colorScheme.error
    )


private object HomeTokens {
    // 背景渐变
    val BackgroundGradientTop = Color(0xFFF7F1FF)
    val BackgroundGradientBottom = Color(0xFFFDF9FF)

    // 背景圆
    val HeroCircleSize = 500.dp
    val HeroCircleOffsetLeftX = (-100).dp
    val HeroCircleOffsetLeftY = (-40).dp
    val HeroCircleOffsetRightX = 100.dp
    val HeroCircleOffsetRightY = 500.dp

    // 品牌色
    val BrandPurple = Color(0xFFB37BFF)
    val BrandPurpleAlt = Color(0xFF8E5CFF)
    val BrandPurpleTransparent = Color(0x008E5CFF)

    // 页面间距
    val ScreenHorizontalPadding = 16.dp
    val ScreenVerticalPadding = 8.dp
    val ScreenSectionTopPadding = 12.dp
    val TopBarHorizontalPadding = 16.dp
    val TopBarVerticalPadding = 12.dp
    val ListItemSpacing = 12.dp

    // 分类 Chip 区
    val ChipRowHorizontalPadding = 16.dp
    val ChipSpacing = 8.dp
    // 单个 Chip 内部
    val ChipInnerHorizontalPadding = 12.dp      // 文本左右边距
    val ChipInnerVerticalPadding = 10.dp        // 文本上下边距
    val ChipBorderWidth = 3.dp

    // 搜索 / Icon 尺寸
    val SearchBarHeight = 60.dp
    val SearchIconSize = 35.dp
    val SearchInnerHorizontalPadding = 12.dp
    val SearchTextStartSpacing = 8.dp
    val SearchIconColor = BrandPurple
    val ProfileIconColor = BrandPurple

    // 头像按钮
    val ProfileButtonSize = 40.dp
    val ProfileButtonAlpha = 0f
    val ProfileIconSize = 35.dp
}

private object DishCardTokens {
    val Height = 96.dp
    val CornerRadius = 24.dp
    val PriceTextColor = Color.Black

    val GlassBgAlpha = 0.26f
    val GlassBorderAlpha = 1f

    val ContentPaddingStart = 16.dp       // 整个文字块离卡片左边的距离
    val ContentPaddingEnd = 12.dp        // 整个文字块离卡片右边的距离
    val ContentPaddingVertical = 10.dp   // 整个文字块离卡片上下的距离

    val LinesSpacing = 0.dp              // 金额行 和 名称行 之间的间距
    val PriceRowSpacing = 6.dp           // 金额行内部，多个元素之间的水平间距
    val PriceRowOffsetTop = 28.dp        // 金额行 距离 顶部 的偏移量
    val RecLabelTopOffset = 0.dp         // 推荐标签相对文本区域顶部的偏移量
    val RecFontSize = 22.sp              // 推荐字体大小
    val RecColor = HomeTokens.BrandPurple

    val ImagePaddingVertical = 12.dp
    val ImagePaddingEnd = 16.dp

    // 1：左上 白 50%
    val BorderGradientStart = Color.White.copy(alpha = 0.5f)
    // 2：中段开始透明
    val BorderGradientMidSoft = Color.White.copy(alpha = 0f)
    // 3：中段结束仍然透明
    val BorderGradientMidHard = HomeTokens.BrandPurpleAlt.copy(alpha = 0f)
    // 4：右下 品牌色 50%
    val BorderGradientEnd = HomeTokens.BrandPurpleAlt.copy(alpha = 1f)
    val BorderWidth = 1.5.dp
    val ImageAreaBackground = Color(0xFF4CAF50)
    val ContentInsetFromBorder = 1.5.dp
    val InnerCornerRadius = CornerRadius - ContentInsetFromBorder
    val PriceFontSize = 21.sp
}

// ---------- 登录页局部 Token ----------

private object LoginTokens {
    // 对齐首页的品牌与背景
    val BackgroundGradientTop = HomeTokens.BackgroundGradientTop
    val BackgroundGradientBottom = HomeTokens.BackgroundGradientBottom
    val BrandPurple = HomeTokens.BrandPurple
    val BrandPurpleAlt = HomeTokens.BrandPurpleAlt


    // 卡片样式
    val CardCornerRadius = 24.dp
    val CardBgAlpha = DishCardTokens.GlassBgAlpha
    val CardBorderWidth = 1.5.dp

    // 布局间距
    val ScreenHorizontalPadding = 16.dp
    val CardHorizontalPadding = 24.dp
    val CardVerticalPadding = 24.dp
    val BackFontSize = 21.sp       // 后退大小就调这里

    val BackFontWeight = FontWeight.SemiBold     // 后退字体粗细
}

// ---------- Edit Dish 页局部 Token ----------

private object EditDishTokens {
    val CardCornerRadius = 24.dp
    val CardHorizontalPadding = 24.dp
    val CardVerticalPadding = 24.dp

    val ImageBoxHeight = 160.dp
    val SectionSpacing = 16.dp
    val FieldSpacing = 12.dp
    val ToggleRowSpacing = 12.dp

    val PrimaryButtonHeight = 52.dp
    val ButtonCornerRadius = 999.dp
}

// ---------- Admin 管理页局部 Token ----------

private object AdminTokens {
    val CardCornerRadius = 24.dp
    val CardHorizontalPadding = 24.dp
    val CardVerticalPadding = 24.dp
    val SectionSpacing = 16.dp
    val RowSpacing = 8.dp
    val IconSize = 20.dp
}

// ---------- UI 包自己的契约模型（不再引用 DemoDish / RestaurantUiState） ----------

data class RestaurantHomeDish(
    val id: String,
    val title: String,
    val subtitle: String?,
    val category: String?,
    val price: Double,
    val isRecommended: Boolean,
    val isSoldOut: Boolean
)

data class RestaurantHomeUiState(
    val dishes: List<RestaurantHomeDish> = emptyList(),
    val selectedCategory: String? = null,
    val manualCategories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

data class RestaurantHomeActions(
    val onRefresh: () -> Unit,
    val onCategorySelected: (String?) -> Unit,
    val onDishSelected: (String) -> Unit,
    val onProfileClick: () -> Unit
)

// ---------- 登录页 UI 契约 ----------

data class RestaurantLoginUiState(
    val isLoading: Boolean = false,
    val loginError: String? = null
)

data class RestaurantLoginActions(
    val onLogin: (String, String) -> Unit,
    val onBackToHome: () -> Unit
)

// ---------- Edit Dish UI 契约 ----------

data class RestaurantEditDishUiState(
    val name: String = "",
    val price: String = "",
    val discountPrice: String = "",
    val description: String = "",
    val availableCategories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val isRecommended: Boolean = false,
    val isSoldOut: Boolean = false,
    val imagePreviewUrl: String? = null, // 当前只做占位
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isNew: Boolean = false
)

data class RestaurantEditDishActions(
    val onBack: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onPriceChange: (String) -> Unit,
    val onDiscountPriceChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onCategorySelected: (String?) -> Unit,
    val onToggleRecommended: (Boolean) -> Unit,
    val onToggleSoldOut: (Boolean) -> Unit,
    val onPickImage: () -> Unit,
    val onSave: () -> Unit,
    val onDelete: (() -> Unit)? = null
)

// ---------- Detail 页 UI 契约 ----------

data class RestaurantDetailUiState(
    val name: String = "",
    val price: String = "",
    val discountPrice: String? = null,
    val description: String = "",
    val category: String? = null,
    val isRecommended: Boolean = false,
    val isUnavailable: Boolean = false,
    val imagePreviewUrl: String? = null,      // 单图兼容
    val imageUrls: List<String> = emptyList(), // 多图占位（最多 6）
    val currentImageIndex: Int = 0
)

data class RestaurantDetailActions(
    val onBack: () -> Unit,
    val onEdit: () -> Unit,
    val onOpenImage: () -> Unit
)

// ---------- Admin 管理页 UI 契约 ----------

data class RestaurantAdminUiState(
    val categories: List<String> = emptyList(),
    val dishes: List<RestaurantHomeDish> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

data class RestaurantAdminActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onEditDish: (String) -> Unit,
    val onDeleteDish: (String) -> Unit,
    val onDeleteCategory: (String) -> Unit,
    val onAddCategory: (String) -> Unit
)

// ---------- 首页布局（供逻辑模块调用） ----------

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RestaurantHomeNeu(
    uiState: RestaurantHomeUiState,
    actions: RestaurantHomeActions,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // ✅ Snackbar 状态：用来显示 ViewModel 里的 statusMessage
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    // ✅ 下拉刷新状态：绑定到 uiState.isLoading + actions.onRefresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = actions.onRefresh
    )

    // 先在 UI 层做一个简单过滤：按名称/简介包含关键字
    val filteredDishes = remember(uiState.dishes, uiState.selectedCategory, searchQuery) {
        uiState.dishes
            .filter { dish ->
                val matchCategory = uiState.selectedCategory?.let {
                    dish.category == it
                } ?: true

                val matchSearch = searchQuery.isBlank() ||
                        dish.title.contains(searchQuery, ignoreCase = true) ||
                        (dish.subtitle ?: "").contains(searchQuery, ignoreCase = true)

                matchCategory && matchSearch
            }
    }

    // 背景渐变
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            HomeTokens.BackgroundGradientTop,
            HomeTokens.BackgroundGradientBottom
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground)
            // ✅ 整个内容支持下拉刷新
            .pullRefresh(pullRefreshState)
    ) {
        // 左上角大圆
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetLeftX,
            offsetY = HomeTokens.HeroCircleOffsetLeftY,
            colors = listOf(
                HomeTokens.BrandPurple,
                HomeTokens.BrandPurpleTransparent
            )
        )

        // 右下角圆
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetRightX,
            offsetY = HomeTokens.HeroCircleOffsetRightY,
            colors = listOf(
                HomeTokens.BrandPurpleAlt,
                HomeTokens.BrandPurpleTransparent
            )
        )

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopSearchBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onProfileClick = actions.onProfileClick
                )
            },
            // ✅ 在 Scaffold 里挂一个 SnackbarHost，用来显示状态提醒
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 顶部分类 Chip 区域（横向滚动）
                CategoryChipsRow(
                    selectedCategory = uiState.selectedCategory,
                    manualCategories = uiState.manualCategories,
                    onCategorySelected = actions.onCategorySelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HomeTokens.ScreenSectionTopPadding)
                )

                // 菜品列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = HomeTokens.ScreenHorizontalPadding,
                            vertical = HomeTokens.ScreenVerticalPadding
                        ),
                    verticalArrangement = Arrangement.spacedBy(HomeTokens.ListItemSpacing)
                ) {
                    items(filteredDishes) { dish ->
                        DishListCard(
                            dish = dish,
                            onClick = { actions.onDishSelected(dish.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // ✅ 下拉时顶部的刷新指示器
        PullRefreshIndicator(
            refreshing = uiState.isLoading,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp),  // 让刷新圈落在搜索栏下面
            contentColor = HomeTokens.BrandPurple,
            backgroundColor = Color.White,
            scale = true
        )
    }
}

// ---------- 登录页布局（Admin 登录） ----------

@Composable
fun RestaurantLoginNeu(
    uiState: RestaurantLoginUiState,
    actions: RestaurantLoginActions,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 背景渐变和首页统一
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            LoginTokens.BackgroundGradientTop,
            LoginTokens.BackgroundGradientBottom
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground)
    ) {
        // 复用首页的两个大圆
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetLeftX,
            offsetY = HomeTokens.HeroCircleOffsetLeftY,
            colors = listOf(
                LoginTokens.BrandPurple,
                HomeTokens.BrandPurpleTransparent
            )
        )

        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetRightX,
            offsetY = HomeTokens.HeroCircleOffsetRightY,
            colors = listOf(
                LoginTokens.BrandPurpleAlt,
                HomeTokens.BrandPurpleTransparent
            )
        )

        // 顶部 Back 按钮
        TextButton(
            onClick = actions.onBackToHome,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    horizontal = LoginTokens.ScreenHorizontalPadding,
                    vertical = 16.dp
                )
        ) {
            Text(
                text = "Back",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = LoginTokens.BackFontSize,
                    fontWeight = LoginTokens.BackFontWeight
                ),
                color = LoginTokens.BrandPurpleAlt
            )
        }

        // 中间的登录卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(LoginTokens.CardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = LoginTokens.CardBgAlpha)
            ),
            border = BorderStroke(
                width = LoginTokens.CardBorderWidth,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.2f to DishCardTokens.BorderGradientStart,
                        0.40f to DishCardTokens.BorderGradientMidSoft,
                        0.10f to DishCardTokens.BorderGradientMidHard,
                        1.0f to DishCardTokens.BorderGradientEnd
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LoginTokens.CardHorizontalPadding,
                        vertical = LoginTokens.CardVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Admin login",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Text(
                    text = "Use your admin account to manage items and categories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.7f)
                )

                // 用户名
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                    colors = neuOutlinedTextFieldColors()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = neuOutlinedTextFieldColors()
                )

                // 错误提示
                if (!uiState.loginError.isNullOrBlank()) {
                    Text(
                        text = uiState.loginError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // 登录按钮
                Button(
                    onClick = { actions.onLogin(username, password) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoginTokens.BrandPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "Login",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = HomeTokens.TopBarHorizontalPadding,
                vertical = HomeTokens.TopBarVerticalPadding
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧搜索框（无背景矩形）
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(HomeTokens.SearchBarHeight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = HomeTokens.SearchInnerHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(HomeTokens.SearchIconSize),
                        tint = HomeTokens.SearchIconColor
                    )

                    Spacer(modifier = Modifier.width(HomeTokens.SearchTextStartSpacing))

                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search…") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            // 右侧头像按钮
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = HomeTokens.ProfileButtonAlpha),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.size(HomeTokens.ProfileButtonSize)
            ) {
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = "Profile",
                        modifier = Modifier.size(HomeTokens.ProfileIconSize),
                        tint = HomeTokens.SearchIconColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipsRow(
    selectedCategory: String?,
    manualCategories: List<String>,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    showAllChip: Boolean = true,   // 新增参数：是否显示 “All”
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = HomeTokens.ChipRowHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(HomeTokens.ChipSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 可选的 “All” 芯片
        if (showAllChip) {
            CategoryChipPill(
                text = "All",
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }

        // 其它分类
        manualCategories.forEach { cat: String ->
            CategoryChipPill(
                text = cat,
                selected = selectedCategory == cat,
                onClick = { onCategorySelected(cat) }
            )
        }
    }
}


/**
 * 自画一个胶囊按钮：背景和描边用同一个 shape，
 * 不再用 FilterChip，这样形状和描边一定对齐。
 */
@Composable
private fun CategoryChipPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(999.dp)

    val backgroundColor =
        if (selected) HomeTokens.BrandPurple
        else HomeTokens.BrandPurple.copy(alpha = 0.18f)

    val textColor =
        if (selected) Color.White
        else Color.Black

    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(backgroundColor)
            .border(
                border = BorderStroke(
                    width = DishCardTokens.BorderWidth,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to DishCardTokens.BorderGradientStart,
                            0.3f to DishCardTokens.BorderGradientMidSoft,
                            0.1f to DishCardTokens.BorderGradientMidHard,
                            1.0f to DishCardTokens.BorderGradientEnd
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 400f)
                    )
                ),
                shape = chipShape
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = HomeTokens.ChipInnerHorizontalPadding,
                vertical = HomeTokens.ChipInnerVerticalPadding
            )
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@Composable
private fun DishListCard(
    dish: RestaurantHomeDish,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // 整张卡片的外形圆角
            .clip(RoundedCornerShape(DishCardTokens.CornerRadius))
            // 一圈渐变描边
            .border(
                border = BorderStroke(
                    width = DishCardTokens.BorderWidth,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to DishCardTokens.BorderGradientStart,
                            0.30f to DishCardTokens.BorderGradientMidSoft,
                            0.10f to DishCardTokens.BorderGradientMidHard,
                            1.0f to DishCardTokens.BorderGradientEnd
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                ),
                shape = RoundedCornerShape(DishCardTokens.CornerRadius)
            )
            .padding(DishCardTokens.ContentInsetFromBorder)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(DishCardTokens.Height),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧文本内容区（毛玻璃效果）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = DishCardTokens.InnerCornerRadius,
                            bottomStart = DishCardTokens.InnerCornerRadius,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .background(
                        Color.White.copy(alpha = DishCardTokens.GlassBgAlpha)
                    )
                    .padding(
                        start = DishCardTokens.ContentPaddingStart,
                        top = DishCardTokens.ContentPaddingVertical,
                        bottom = DishCardTokens.ContentPaddingVertical,
                        end = DishCardTokens.ContentPaddingEnd
                    )
            ) {
                // ✅ 价格 + 名称：原来的布局，位置固定，不受推荐影响
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(DishCardTokens.LinesSpacing)
                ) {
                    Row(
                        modifier = Modifier.padding(top = DishCardTokens.PriceRowOffsetTop),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DishCardTokens.PriceRowSpacing)
                    ) {
                        Text(
                            text = "$${"%.2f".format(dish.price)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = DishCardTokens.PriceFontSize
                            ),
                            fontWeight = FontWeight.Medium,
                            color = DishCardTokens.PriceTextColor
                        )
                    }

                    Text(
                        text = dish.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // ✅ 推荐标识：叠加在左上角，不推挤下面的价格和名称
                if (dish.isRecommended) {
                    Text(
                        text = "Rec.",
                        color = DishCardTokens.RecColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = DishCardTokens.RecFontSize
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = DishCardTokens.RecLabelTopOffset)
                    )
                }
            }

            // 右侧图片区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topEnd = DishCardTokens.InnerCornerRadius,
                            bottomEnd = DishCardTokens.InnerCornerRadius
                        )
                    )
                    .background(DishCardTokens.ImageAreaBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = DishCardTokens.ImagePaddingVertical,
                            bottom = DishCardTokens.ImagePaddingVertical,
                            end = DishCardTokens.ImagePaddingEnd
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Image",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ---------- Edit Dish 页面 ----------

@Composable
fun RestaurantEditDishNeu(
    uiState: RestaurantEditDishUiState,
    actions: RestaurantEditDishActions,
    modifier: Modifier = Modifier
) {
    // 背景与首页保持一致
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            HomeTokens.BackgroundGradientTop,
            HomeTokens.BackgroundGradientBottom
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground)
    ) {
        // 背景圆
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetLeftX,
            offsetY = HomeTokens.HeroCircleOffsetLeftY,
            colors = listOf(
                HomeTokens.BrandPurple,
                HomeTokens.BrandPurpleTransparent
            )
        )

        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetRightX,
            offsetY = HomeTokens.HeroCircleOffsetRightY,
            colors = listOf(
                HomeTokens.BrandPurpleAlt,
                HomeTokens.BrandPurpleTransparent
            )
        )

        // 顶部 Back
        TextButton(
            onClick = actions.onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                )
                .zIndex(1f) 
        ) {
            Text(
                text = "Back",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = LoginTokens.BackFontSize,
                    fontWeight = LoginTokens.BackFontWeight
                ),
                color = HomeTokens.BrandPurpleAlt
            )
        }

        // 中间编辑卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .align(Alignment.TopCenter)
                .padding(
                    top = 80.dp,
                    start = 24.dp,
                    bottom = 24.dp,
                    end = 24.dp
                ),
            shape = RoundedCornerShape(EditDishTokens.CardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = DishCardTokens.GlassBgAlpha)
            ),
            border = BorderStroke(
                width = DishCardTokens.BorderWidth,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to DishCardTokens.BorderGradientStart,
                        0.30f to DishCardTokens.BorderGradientMidSoft,
                        0.10f to DishCardTokens.BorderGradientMidHard,
                        1.0f to DishCardTokens.BorderGradientEnd
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = EditDishTokens.CardHorizontalPadding,
                        vertical = EditDishTokens.CardVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(EditDishTokens.SectionSpacing)
            ) {

                // 标题 + 副标题
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (uiState.isNew) "Add item" else "Edit item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Update item information, category, and status.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }

                // 文本字段
                Column(
                    verticalArrangement = Arrangement.spacedBy(EditDishTokens.FieldSpacing)
                ) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = actions.onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        singleLine = true,
                        colors = neuOutlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = uiState.price,
                        onValueChange = actions.onPriceChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Price") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = neuOutlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = uiState.discountPrice,
                        onValueChange = actions.onDiscountPriceChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Discount price") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = neuOutlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = actions.onDescriptionChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        label = { Text("Description") },
                        singleLine = false,
                        maxLines = 4,
                        supportingText = {
                            Text(text = "Up to 200 characters")
                        },
                        colors = neuOutlinedTextFieldColors()
                    )
                }

                // 图片预览 + 添加图片按钮
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // 如果已有图片，则在左边放一个预览占位
                        if (!uiState.imagePreviewUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        HomeTokens.BrandPurple.copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Image",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HomeTokens.BrandPurpleAlt
                                )
                            }
                        }

                        // 右侧加号按钮
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(HomeTokens.BrandPurple)
                                .border(
                                    width = DishCardTokens.BorderWidth,
                                    brush = Brush.linearGradient(
                                        colorStops = arrayOf(
                                            0.0f to DishCardTokens.BorderGradientStart,
                                            0.30f to DishCardTokens.BorderGradientMidSoft,
                                            0.10f to DishCardTokens.BorderGradientMidHard,
                                            1.0f to DishCardTokens.BorderGradientEnd
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(400f, 400f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { actions.onPickImage() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 35.sp
                                ),
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        }
                    }

                    // 最多 6 张图片提示
                    Text(
                        text = "Up to 6 images",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }

                // 分类选择
                if (uiState.availableCategories.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black.copy(alpha = 0.8f)
                        )

                        CategoryChipsRow(
                            selectedCategory = uiState.selectedCategory,
                            manualCategories = uiState.availableCategories,
                            onCategorySelected = actions.onCategorySelected,
                            modifier = Modifier.fillMaxWidth(),
                            showAllChip = false
                        )
                    }
                }

                // 推荐 / 不可用 开关
                Column(
                    verticalArrangement = Arrangement.spacedBy(EditDishTokens.ToggleRowSpacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        Switch(
                            checked = uiState.isRecommended,
                            onCheckedChange = actions.onToggleRecommended
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Unavailable",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        Switch(
                            checked = uiState.isSoldOut,
                            onCheckedChange = actions.onToggleSoldOut
                        )
                    }
                }

                // 错误信息
                if (!uiState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // 保存按钮
                Button(
                    onClick = actions.onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EditDishTokens.PrimaryButtonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HomeTokens.BrandPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(EditDishTokens.ButtonCornerRadius)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = if (uiState.isNew) "Create item" else "Save changes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ---------- Detail 页面（查看详情） ----------

@Composable
fun RestaurantDishDetailNeu(
    uiState: RestaurantDetailUiState,
    actions: RestaurantDetailActions,
    modifier: Modifier = Modifier
) {
    var backClickCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        Log.d("NDJC_DETAIL", "RestaurantDishDetailNeu composed")
    }

    // 背景与首页保持一致
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            HomeTokens.BackgroundGradientTop,
            HomeTokens.BackgroundGradientBottom
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground)
            .clickable {
                Log.d("NDJC_DETAIL", ">>> OUTER BOX CLICKED <<<")
            }
    ) {

        // 背景圆
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetLeftX,
            offsetY = HomeTokens.HeroCircleOffsetLeftY,
            colors = listOf(
                HomeTokens.BrandPurple,
                HomeTokens.BrandPurpleTransparent
            )
        )
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetRightX,
            offsetY = HomeTokens.HeroCircleOffsetRightY,
            colors = listOf(
                HomeTokens.BrandPurpleAlt,
                HomeTokens.BrandPurpleTransparent
            )
        )

        TextButton(
            onClick = actions.onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .background(Color.Transparent)  // 保持透明背景
                .zIndex(1f)  // 确保点击事件可以被接收
        ) {
            Text(
                text = "Back",  // 保持文本
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = LoginTokens.BackFontSize,  // 确保和登录页一致
                    fontWeight = LoginTokens.BackFontWeight  // 保持字体粗细一致
                ),
                color = LoginTokens.BrandPurpleAlt  // 保持与登录页一致的颜色
            )
        }

        // 中间详情卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .align(Alignment.TopCenter)
                .padding(
                    top = 80.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                ),
            shape = RoundedCornerShape(EditDishTokens.CardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = DishCardTokens.GlassBgAlpha)
            ),
            border = BorderStroke(
                width = DishCardTokens.BorderWidth,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to DishCardTokens.BorderGradientStart,
                        0.30f to DishCardTokens.BorderGradientMidSoft,
                        0.10f to DishCardTokens.BorderGradientMidHard,
                        1.0f to DishCardTokens.BorderGradientEnd
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = EditDishTokens.CardHorizontalPadding,
                        vertical = EditDishTokens.CardVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(EditDishTokens.SectionSpacing)
            ) {

                // 标题
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Item details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "View item information, images and status.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }

                // 图片区域：点击查看大图 + 多图指示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EditDishTokens.ImageBoxHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .background(HomeTokens.BrandPurple.copy(alpha = 0.18f))
                        .clickable(
                            enabled = uiState.imagePreviewUrl != null || uiState.imageUrls.isNotEmpty(),
                            onClick = actions.onOpenImage        // 真正的左右滑动 & 缩放在全屏查看里做
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val hasImage =
                        uiState.imageUrls.isNotEmpty() || uiState.imagePreviewUrl != null

                    // 中间提示文案
                    Text(
                        text = if (hasImage) "Tap to view image" else "No image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HomeTokens.BrandPurpleAlt
                    )

                    // 底部多图指示小圆点（最多 6 个）
                    val imageCount = when {
                        uiState.imageUrls.isNotEmpty() -> uiState.imageUrls.size.coerceAtMost(6)
                        uiState.imagePreviewUrl != null -> 1
                        else -> 0
                    }

                    if (imageCount > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(imageCount) { index ->
                                val isActive =
                                    index == uiState.currentImageIndex.coerceIn(0, imageCount - 1)
                                Box(
                                    modifier = Modifier
                                        .size(if (isActive) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive)
                                                HomeTokens.BrandPurpleAlt
                                            else
                                                HomeTokens.BrandPurpleAlt.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }
                }

                // 名称 + 价格 + 推荐标识
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // ✅ Rec. 放到名称上面，并加大字号
                    if (uiState.isRecommended) {
                        Text(
                            text = "Rec.",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = DishCardTokens.RecFontSize
                            ),
                            fontWeight = FontWeight.Bold,
                            color = HomeTokens.BrandPurpleAlt
                        )
                    }

                    Text(
                        text = uiState.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!uiState.discountPrice.isNullOrBlank()) {
                            Text(
                                text = "$${uiState.discountPrice}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = HomeTokens.BrandPurpleAlt
                            )
                            Text(
                                text = "$${uiState.price}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                color = Color.Black.copy(alpha = 0.5f)
                            )
                        } else {
                            Text(
                                text = "$${uiState.price}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // 描述
                if (uiState.description.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                        Text(
                            text = uiState.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.9f)
                        )
                    }
                }

                // 分类 + 状态标签
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!uiState.category.isNullOrBlank()) {
                        Text(
                            text = "Category: ${uiState.category}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isUnavailable) {
                            Text(
                                text = "Unavailable",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

            }
        }
    }
}

// ---------- Admin 管理页面 ----------

@Composable
fun RestaurantAdminNeu(
    uiState: RestaurantAdminUiState,
    actions: RestaurantAdminActions,
    modifier: Modifier = Modifier
) {
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var newCategoryName by remember { mutableStateOf("") }

    // Admin 菜品搜索 + 多选删除
    var itemSearchQuery by remember { mutableStateOf("") }
    var selectedDishIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategoryDeleteDialog by remember { mutableStateOf(false) }

    // 搜索过滤菜单（按名称 / 分类）
    val filteredDishes = remember(uiState.dishes, itemSearchQuery) {
        uiState.dishes.filter { dish ->
            itemSearchQuery.isBlank() ||
                    dish.title.contains(itemSearchQuery, ignoreCase = true) ||
                    (dish.category ?: "").contains(itemSearchQuery, ignoreCase = true)
        }
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            HomeTokens.BackgroundGradientTop,
            HomeTokens.BackgroundGradientBottom
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground)
    ) {

        // 背景圆
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetLeftX,
            offsetY = HomeTokens.HeroCircleOffsetLeftY,
            colors = listOf(
                HomeTokens.BrandPurple,
                HomeTokens.BrandPurpleTransparent
            )
        )
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetRightX,
            offsetY = HomeTokens.HeroCircleOffsetRightY,
            colors = listOf(
                HomeTokens.BrandPurpleAlt,
                HomeTokens.BrandPurpleTransparent
            )
        )

        // 顶部 Back 按钮
        TextButton(
            onClick = actions.onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Back",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = LoginTokens.BackFontSize,
                    fontWeight = LoginTokens.BackFontWeight
                ),
                color = HomeTokens.BrandPurpleAlt
            )
        }

        // 中间管理卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(
                    top = 80.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                ),
            shape = RoundedCornerShape(AdminTokens.CardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = DishCardTokens.GlassBgAlpha)
            ),
            border = BorderStroke(
                width = DishCardTokens.BorderWidth,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to DishCardTokens.BorderGradientStart,
                        0.30f to DishCardTokens.BorderGradientMidSoft,
                        0.10f to DishCardTokens.BorderGradientMidHard,
                        1.0f to DishCardTokens.BorderGradientEnd
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = AdminTokens.CardHorizontalPadding,
                        vertical = AdminTokens.CardVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(AdminTokens.SectionSpacing)
            ) {
                // 标题 + 刷新
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Admin panel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Text(
                            text = "Manage categories and items.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                    }

                    TextButton(onClick = actions.onRefresh) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Refresh")
                        }
                    }
                }

                // 分类列表（胶囊按钮样式 + 多选 + 统一删除）
                Column(verticalArrangement = Arrangement.spacedBy(AdminTokens.RowSpacing)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black.copy(alpha = 0.8f)
                        )

                        IconButton(
                            onClick = {
                                // ✅ 不直接删，只先弹出确认框
                                showCategoryDeleteDialog = true
                            },
                            enabled = selectedCategories.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete selected categories",
                                tint = if (selectedCategories.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        }
                    }
                    // ✅ 新增分类输入框
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        label = { Text("New category") },
                        colors = neuOutlinedTextFieldColors()
                    )

                    // ✅ “Add” 按钮（暂时只负责触发逻辑）
                    Button(
                        onClick = {
                            val name = newCategoryName.trim()
                            if (name.isNotEmpty()) {
                                actions.onAddCategory(name)
                                // 先只在这里处理，后面再接到 ViewModel
                                // actions.onAddCategory(name) // 接好逻辑后改成调用 Action
                                newCategoryName = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HomeTokens.BrandPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (uiState.categories.isEmpty()) {
                        Text(
                            text = "No categories yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(HomeTokens.ChipSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            uiState.categories.forEach { cat ->
                                val isSelected = selectedCategories.contains(cat)

                                CategoryChipPill(
                                    text = cat,
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategories =
                                            if (isSelected)
                                                selectedCategories - cat
                                            else
                                                selectedCategories + cat
                                    }
                                )
                            }
                        }
                    }
                }

                Divider()

                // 横线下面的 “Add New Dish” 按钮（UI 包内的按钮）
                Button(
                    onClick = {
                        // 这里通过 onEditDish("") 这种约定方式，让壳子/逻辑层识别为“新增菜品”
                        actions.onEditDish("")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HomeTokens.BrandPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Add New Dish",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // 菜品列表：搜索 + 多选 + 统一删除
                Column(verticalArrangement = Arrangement.spacedBy(AdminTokens.RowSpacing)) {

                    // 标题 + 统一删除按钮（只留这一个垃圾桶）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Items",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black.copy(alpha = 0.8f)
                        )

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = selectedDishIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete selected items",
                                tint = if (selectedDishIds.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // 搜索框：按名称 / 分类搜索已有菜单
                    OutlinedTextField(
                        value = itemSearchQuery,
                        onValueChange = { itemSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                        label = { Text("Search items") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        colors = neuOutlinedTextFieldColors()
                    )

                    if (filteredDishes.isEmpty()) {
                        Text(
                            text = if (uiState.dishes.isEmpty())
                                "No items yet."
                            else
                                "No items match your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    } else {
                        filteredDishes.forEach { dish ->
                            val checked = selectedDishIds.contains(dish.id)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左侧：菜品信息
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = dish.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (dish.isRecommended) {
                                            Text(
                                                text = "Rec.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = HomeTokens.BrandPurpleAlt
                                            )
                                        }
                                        if (dish.isSoldOut) {
                                            Text(
                                                text = "Unavailable",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Text(
                                        text = "$${"%.2f".format(dish.price)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
                                    )

                                    dish.category?.takeIf { it.isNotBlank() }?.let { cat ->
                                        Text(
                                            text = "Category: $cat",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Black.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // 右侧：勾选 + 编辑（单独保留编辑按钮）
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            selectedDishIds =
                                                if (isChecked)
                                                    selectedDishIds + dish.id
                                                else
                                                    selectedDishIds - dish.id
                                        }
                                    )

                                    IconButton(onClick = { actions.onEditDish(dish.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit item",
                                            tint = HomeTokens.BrandPurpleAlt
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 状态提示
                uiState.statusMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // 删除选中菜品的确认弹窗
        if (showDeleteDialog) {
            DeleteConfirmDialog(
                title = "Delete selected items?",
                message = "This action cannot be undone.",
                onConfirm = {
                    selectedDishIds.forEach { id ->
                        actions.onDeleteDish(id)
                    }
                    selectedDishIds = emptySet()
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
        // ✅ 删除选中分类的确认弹窗
        if (showCategoryDeleteDialog) {
            DeleteConfirmDialog(
                title = "Delete selected categories?",
                message = "This action cannot be undone.",
                onConfirm = {
                    selectedCategories.forEach { cat ->
                        actions.onDeleteCategory(cat)
                    }
                    selectedCategories = emptySet()
                    showCategoryDeleteDialog = false
                },
                onDismiss = { showCategoryDeleteDialog = false }
            )
        }

    }
}

// ---------- 背景圆 ----------

@Composable
private fun BgCircle(
    size: Dp,
    offsetX: Dp,
    offsetY: Dp,
    colors: List<Color>
) {
    Box(
        modifier = Modifier
            .size(size)
            .offset(x = offsetX, y = offsetY)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(colors)
            )
    )
}

// ---------- 预览 ----------

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun RestaurantHomeNeuPreview() {
    val demoDishes = listOf(
        RestaurantHomeDish(
            id = "1",
            title = "Tomahawk steak",
            subtitle = "99",
            category = "Hot",
            price = 9.99,
            isRecommended = true,
            isSoldOut = false
        ),
        RestaurantHomeDish(
            id = "2",
            title = "Caesar salad",
            subtitle = "777",
            category = "Special",
            price = 12.50,
            isRecommended = false,
            isSoldOut = false
        ),
        RestaurantHomeDish(
            id = "3",
            title = "French lamb chops",
            subtitle = "88",
            category = "Dessert",
            price = 18.88,
            isRecommended = true,
            isSoldOut = true
        )
    )

    val state = RestaurantHomeUiState(
        dishes = demoDishes,
        selectedCategory = null,
        manualCategories = listOf("Hot", "Special", "Dessert"),
        isLoading = false,
        statusMessage = "Loaded from preview data"
    )

    val actions = RestaurantHomeActions(
        onRefresh = {},
        onCategorySelected = {},
        onDishSelected = {},
        onProfileClick = {}
    )

    NDJCTheme {
        RestaurantHomeNeu(
            uiState = state,
            actions = actions
        )
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun RestaurantLoginNeuPreview() {
    val state = RestaurantLoginUiState(
        isLoading = false,
        loginError = "Wrong username or password"
    )

    val actions = RestaurantLoginActions(
        onLogin = { _, _ -> },
        onBackToHome = {}
    )

    NDJCTheme {
        RestaurantLoginNeu(
            uiState = state,
            actions = actions
        )
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun RestaurantDishDetailNeuPreview() {
    val state = RestaurantDetailUiState(
        name = "Tomahawk steak",
        price = "9.99",
        discountPrice = "7.99",
        description = "Huge bone-in steak with garlic butter.",
        category = "Hot",
        isRecommended = true,
        isUnavailable = false,
        imagePreviewUrl = "demo://image"
    )

    val actions = RestaurantDetailActions(
        onBack = {},
        onEdit = {},
        onOpenImage = {}
    )

    NDJCTheme {
        RestaurantDishDetailNeu(
            uiState = state,
            actions = actions
        )
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun RestaurantEditDishNeuPreview() {
    val state = RestaurantEditDishUiState(
        name = "",
        price = "",
        discountPrice = "",
        description = "",
        availableCategories = listOf("Hot", "Special", "Dessert"),
        selectedCategory = "Hot",
        isRecommended = true,
        isSoldOut = false,
        imagePreviewUrl = null,
        isSaving = false,
        errorMessage = null,
        isNew = false
    )

    val actions = RestaurantEditDishActions(
        onBack = {},
        onNameChange = {},
        onPriceChange = {},
        onDiscountPriceChange = {},
        onDescriptionChange = {},
        onCategorySelected = {},
        onToggleRecommended = {},
        onToggleSoldOut = {},
        onPickImage = {},
        onSave = {},
        onDelete = {}
    )

    NDJCTheme {
        RestaurantEditDishNeu(
            uiState = state,
            actions = actions
        )
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun RestaurantAdminNeuPreview() {
    val demoDishes = listOf(
        RestaurantHomeDish(
            id = "1",
            title = "Tomahawk steak",
            subtitle = "99",
            category = "Hot",
            price = 9.99,
            isRecommended = true,
            isSoldOut = false
        ),
        RestaurantHomeDish(
            id = "2",
            title = "Caesar salad",
            subtitle = "777",
            category = "Special",
            price = 12.50,
            isRecommended = false,
            isSoldOut = true
        )
    )

    val state = RestaurantAdminUiState(
        categories = listOf("Hot", "Special", "Dessert"),
        dishes = demoDishes,
        isLoading = false,
        statusMessage = "Loaded from preview data"
    )

    val actions = RestaurantAdminActions(
        onBack = {},
        onRefresh = {},
        onEditDish = {},
        onDeleteDish = {},
        onDeleteCategory = {},
        onAddCategory = {}
    )

    NDJCTheme {
        RestaurantAdminNeu(
            uiState = state,
            actions = actions
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    title: String = "Confirm deletion",
    message: String = "This action cannot be undone.",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogShape = RoundedCornerShape(24.dp)

    // 直接用 AlertDialog，不再自己加半透明背景 / 毛玻璃
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
        // ✅ 弹窗本体纯白
        containerColor = Color.White,
        tonalElevation = 10.dp,
        textContentColor = Color.Black.copy(alpha = 0.8f),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.7f)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun DeleteConfirmDialogPreview() {
    NDJCTheme {
        DeleteConfirmDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
