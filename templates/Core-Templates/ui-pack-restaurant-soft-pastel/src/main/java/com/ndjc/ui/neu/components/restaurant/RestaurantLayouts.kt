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
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.SwitchDefaults
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi










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
// ---------- Detail 页局部 Token ----------

private object DetailTokens {
    // 顶部图片区域高度 & 圆角
    val HeroHeight = 320.dp
    val HeroCornerRadius = 25.dp

    // 顶部图片背景渐变（接近你给的粉色饮料卡）
    val HeroGradientTop = Color(0xFFFF9ECF)
    val HeroGradientBottom = Color(0xFFFFC1D9)

    // 下半部分内容卡片
    val ContentCornerRadius = 32.dp
    val ContentHorizontalPadding = 24.dp
    val ContentVerticalPadding = 24.dp

    // 内容卡片往上叠一小截，制造「卡片压在图片上」的效果
    val ContentOverlap = (-32).dp
}

private object TopBannerTokens {
    // 卡片背景色：取 Buy 按钮那种绿色
    val Background = Color(0xFF26C6A4)   // 需要再微调，就改这一行

    // 顶部卡片圆角、高度、阴影
    val CornerRadius = 24.dp
    val Height = 80.dp
    val ShadowElevation = 8.dp
}   // ✅ 在这里补上这一个右花括号，结束 TopBannerTokens

private object HomeTokens {
    // 背景渐变
    val BackgroundGradientTop = Color(0xFFEFF3F2)
    val BackgroundGradientBottom = Color(0xFFEFF3F2)



    // 背景圆
    val HeroCircleSize = 0.dp
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
    val ScreenVerticalPadding = 10.dp
    val ScreenSectionTopPadding = 0.dp
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
    val ChipUnselectedBackground = Color(0xFFDAE9E6)     // 分类按钮未选中色
    val ChipSelectedBackground = Color(0xFFFE9595)       // 分类按钮选中色

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
    val Height = 220.dp
    val CornerRadius = 24.dp
    val PriceTextColor = Color.Black
    val ShadowElevation = 8.dp

    val GlassBgAlpha = 0.26f
    val GlassBorderAlpha = 1f

    val ContentPaddingStart = 16.dp       // 整个文字块离卡片左边的距离
    val ContentPaddingEnd = 12.dp        // 整个文字块离卡片右边的距离
    val ContentPaddingTop = 100.dp       // 整个文字块距离顶部距离
    val ContentPaddingBottom = 15.dp    // 整个文字块距离底部距离


    val LinesSpacing = 0.dp              // 金额行 和 名称行 之间的间距
    val PriceRowSpacing = 6.dp           // 金额行内部，多个元素之间的水平间距
    val PriceRowOffsetTop = 28.dp        // 金额行 距离 顶部 的偏移量
    val RecLabelTopOffset = 0.dp         // 推荐标签相对文本区域顶部的偏移量
    val RecFontSize = 22.sp              // 推荐字体大小
    val RecColor = HomeTokens.ChipSelectedBackground

    val ImagePaddingVertical = 12.dp
    val ImagePaddingEnd = 16.dp
    val CardBackground = Color(0xFFFF99B2)

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
    val BackButtonVerticalPadding = 42.dp
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

    // 顶部返回按钮与卡片外边距
    val BackButtonHorizontalPadding = 24.dp
    val BackButtonVerticalPadding = 42.dp
    val CardTopPaddingFromScreenTop = 100.dp

    // 详情页图片圆角
    val ImageCornerRadius = 20.dp
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

// ---------- 弹窗局部 Token ----------

private object DialogTokens {
    val CornerRadius = 24.dp
    val TonalElevation = 10.dp
    val ContainerColor = Color.White
    val ContentColor = Color.Black.copy(alpha = 0.8f)
    val TitleColor = Color.Black
    val MessageColor = Color.Black.copy(alpha = 0.7f)
}

// ---------- UI 包自己的契约模型（不再引用 DemoDish / RestaurantUiState） ----------

data class RestaurantHomeDish(
    val id: String,
    val title: String,
    val subtitle: String?,
    val category: String?,
    val price: Double,
    val isRecommended: Boolean,
    val isSoldOut: Boolean,
    val imagePreviewUrl: String? = null
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
    val imageUrls: List<String> = emptyList(), // 最多 6 张预览
    val imagePreviewUrl: String? = null,       // 可选：兼容老逻辑/单图回退
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
    val onRemoveImage: (String) -> Unit,
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

    // Snackbar：展示 ViewModel 的状态消息
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    // ✅ 下拉刷新 state：放在最外层，保证指示器能从整屏顶部出现
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = actions.onRefresh
    )

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
            .pullRefresh(pullRefreshState)
    ) {
        // 背景圆（如果你把 HeroCircleSize 设成 0.dp 就等于关闭）
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

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 36.dp),   // ← 你原来控制下移距离的逻辑保留
            topBar = {
                TopSearchBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onProfileClick = actions.onProfileClick
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 顶部分类 Chip 区域
                CategoryChipsRow(
                    selectedCategory = uiState.selectedCategory,
                    manualCategories = uiState.manualCategories,
                    onCategorySelected = actions.onCategorySelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HomeTokens.ScreenSectionTopPadding)
                )

                // 根据搜索和分类筛选菜品
                val filteredDishes = uiState.dishes.filter { dish ->
                    val matchesCategory = uiState.selectedCategory == null ||
                            dish.category == uiState.selectedCategory

                    val matchesSearch = searchQuery.isBlank() ||
                            dish.title.contains(searchQuery, ignoreCase = true) ||
                            (dish.subtitle?.contains(searchQuery, ignoreCase = true) == true)

                    matchesCategory && matchesSearch
                }

                // 一排两个卡片：纯 UI 排版，业务仍然是「1 个菜品 = 1 张卡片」
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = HomeTokens.ScreenHorizontalPadding,
                            vertical = HomeTokens.ScreenVerticalPadding
                        ),
                    verticalArrangement = Arrangement.spacedBy(HomeTokens.ListItemSpacing)
                ) {
                    val rows = filteredDishes.chunked(2)

                    items(rows) { rowDishes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(HomeTokens.ListItemSpacing)
                        ) {
                            DishListCard(
                                dish = rowDishes[0],
                                onClick = { actions.onDishSelected(rowDishes[0].id) },
                                modifier = Modifier.weight(1f)
                            )

                            if (rowDishes.size > 1) {
                                DishListCard(
                                    dish = rowDishes[1],
                                    onClick = { actions.onDishSelected(rowDishes[1].id) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // ✅ 顶部下拉刷新指示器：放到最外层 Box，让它从整屏顶部出现
        PullRefreshIndicator(
            refreshing = uiState.isLoading,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
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
// 顶部 Back 按钮（卡片样式）
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    horizontal = LoginTokens.ScreenHorizontalPadding,
                    vertical = LoginTokens.BackButtonVerticalPadding
                )
        ) {
            Surface(
                modifier = Modifier
                    .size(50.dp)                     // 卡片大小
                    .clickable { actions.onBackToHome() },
                shape = RoundedCornerShape(12.dp),   // 卡片圆角
                color = HomeTokens.ChipSelectedBackground, // 用分类选中同色
                shadowElevation = 6.dp               // 阴影
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White           // 图标改为白色
                    )
                }
            }
        }


        // 中间的登录卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)            // 在 Box 里顶部居中
                .padding(
                    top = 96.dp,                       // 卡片整体往下移，数值可以自己再调
                    start = LoginTokens.ScreenHorizontalPadding,
                    end = LoginTokens.ScreenHorizontalPadding,
                    bottom = LoginTokens.CardVerticalPadding
                )
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0x33000000),
                    spotColor = Color(0x33000000)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
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
                        containerColor = TopBannerTokens.Background,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(   // ✅ 使用 M3 自带阴影
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp,
                        disabledElevation = 0.dp
                    )
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
        // 用 Box 叠放：底层是绿色卡片，上层是搜索 + 登录按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopBannerTokens.Height)
        ) {
            // 底层：绿色卡片 + 阴影
            Surface(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = TopBannerTokens.ShadowElevation,
                        shape = RoundedCornerShape(TopBannerTokens.CornerRadius),
                        clip = false
                    ),
                shape = RoundedCornerShape(TopBannerTokens.CornerRadius),
                color = TopBannerTokens.Background
            ) { }

            // 上层：搜索 + 登录按钮（覆盖在卡片上面）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧搜索框（透明背景，只显示图标 + 文本）
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(HomeTokens.SearchBarHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(HomeTokens.SearchIconSize),
                        tint = Color.White
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

                // 右侧登录 / 头像按钮
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
                            tint = Color.White
                        )
                    }
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

    // 选中 = 图二的粉色；未选中 = 绿色灰背景
    val backgroundColor =
        if (selected) HomeTokens.ChipSelectedBackground
        else HomeTokens.ChipUnselectedBackground

    val textColor =
        if (selected) Color.White
        else Color.Black

    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(backgroundColor)
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
    val shape = RoundedCornerShape(DishCardTokens.CornerRadius)

    // 外层整张卡片：固定高度 + 阴影 + 圆角 + 点击
    Surface(
        modifier = modifier
            .height(DishCardTokens.Height)
            .shadow(
                elevation = DishCardTokens.ShadowElevation,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = DishCardTokens.CardBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!dish.imagePreviewUrl.isNullOrBlank()) {
                androidx.compose.foundation.Image(
                    painter = rememberAsyncImagePainter(dish.imagePreviewUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }


            // 文本整体靠底对齐：底部间距只由 ContentPaddingBottom 决定
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = DishCardTokens.ContentPaddingStart,
                        end = DishCardTokens.ContentPaddingEnd,
                        top = DishCardTokens.ContentPaddingTop,
                        bottom = DishCardTokens.ContentPaddingBottom
                    ),
                // 这里的 LinesSpacing 控制：
                //  Rec. ↔ 价格  &  价格 ↔ 名称 的间距
                verticalArrangement = Arrangement.spacedBy(DishCardTokens.LinesSpacing)
            ) {
                // 把 Rec. 放进同一个 Column 里，位于价格上方
                if (dish.isRecommended) {
                    Text(
                        text = "Rec.",
                        color = DishCardTokens.RecColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = DishCardTokens.RecFontSize
                        )
                    )
                }

                Row(
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
        }
    }
}

// ---------- Edit Dish 页面 ----------

@OptIn(ExperimentalFoundationApi::class)
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

// 顶部 Back 按钮：和详情页同款卡片按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    horizontal = EditDishTokens.BackButtonHorizontalPadding,
                    vertical = EditDishTokens.BackButtonVerticalPadding
                )
                .zIndex(1f)          // 保证在最上层
        ) {
            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .clickable { actions.onBack() },
                shape = RoundedCornerShape(12.dp),
                color = HomeTokens.ChipSelectedBackground,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }

        // 中间编辑卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .align(Alignment.TopCenter)
                .padding(
                    top = EditDishTokens.CardTopPaddingFromScreenTop,
                    start = EditDishTokens.CardHorizontalPadding,
                    bottom = EditDishTokens.CardVerticalPadding,
                    end = EditDishTokens.CardHorizontalPadding
                )
                .shadow(              // ✅ 新增阴影
                    elevation = 8.dp,
                    shape = RoundedCornerShape(EditDishTokens.CardCornerRadius),
                    clip = false
                ),
            shape = RoundedCornerShape(EditDishTokens.CardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = Color.White   // ✅ 改成不透明白底
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
// 图片预览 + 添加图片按钮
                // 图片预览 + 添加图片（3 列网格，+ 自动右移/换行）
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    // 标题文案
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Image",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black
                        )
                        Text(
                            text = "Upload dish image",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    }

                    // 当前已有图片（最多 6）
                    val previewUrls: List<String> = when {
                        uiState.imageUrls.isNotEmpty() -> uiState.imageUrls.take(6)
                        !uiState.imagePreviewUrl.isNullOrBlank() -> listOf(uiState.imagePreviewUrl)
                        else -> emptyList()
                    }

                    // 是否显示 +
                    val showAddCell = previewUrls.size < 6

                    // 网格数据：图片 +（null 表示 +）
                    val gridItems: List<String?> =
                        if (showAddCell) previewUrls + listOf(null) else previewUrls

                    // 每行 3 个
                    val rows = gridItems.chunked(3)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { item ->
                                    if (item == null) {
                                        // + 槽位
                                        Surface(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clickable { actions.onPickImage() },
                                            shape = RoundedCornerShape(16.dp),
                                            color = HomeTokens.ChipSelectedBackground,
                                            shadowElevation = 6.dp
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "+",
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 36.sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    } else {
                                        // 图片格子
                                        // 放在 previewUrls.forEach 之前（就在 if (previewUrls.isNotEmpty()) { 里面即可）
                                        var selectedUrl by remember { mutableStateOf<String?>(null) }

                                        previewUrls.forEach { url ->
                                            val isSelected = (selectedUrl == url)

                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(HomeTokens.BrandPurple.copy(alpha = 0.18f))
                                                    .combinedClickable(
                                                        onClick = {
                                                            // 点一下：如果当前是选中态，就取消选中；否则不做事（避免误触删除）
                                                            if (isSelected) selectedUrl = null
                                                        },
                                                        onLongClick = {
                                                            // 长按：选中
                                                            selectedUrl = url
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                androidx.compose.foundation.Image(
                                                    painter = rememberAsyncImagePainter(url),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )

                                                // 选中态：右上角红色圆形 "-" 删除按钮
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFE53935)) // 红色
                                                            .clickable {
                                                                // 删除当前选中图片
                                                                actions.onRemoveImage(url)
                                                                selectedUrl = null
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "-",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 补空位，保证一行 3 个对齐
                                if (row.size < 3) {
                                    repeat(3 - row.size) {
                                        Spacer(modifier = Modifier.size(80.dp))
                                    }
                                }
                            }
                        }
                    }

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
// Recommended 开关
                        Switch(
                            checked = uiState.isRecommended,
                            onCheckedChange = actions.onToggleRecommended,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = TopBannerTokens.Background, // ✅ 和 Save changes 同色
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE4E5F0),
                                uncheckedThumbColor = Color(0xFFCDD0E0)
                            )
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
                            onCheckedChange = actions.onToggleSoldOut,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = TopBannerTokens.Background, // ✅ 同样用 Save changes 的颜色
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE4E5F0),
                                uncheckedThumbColor = Color(0xFFCDD0E0)
                            )
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
// 保存按钮：和登录页 Login 同款样式 + 阴影
                Button(
                    onClick = actions.onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EditDishTokens.PrimaryButtonHeight)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp),
                            clip = false
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TopBannerTokens.Background, // 和 Login 一样的绿色
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)               // 和 Login 一样的圆角
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

// 顶部 Back 按钮：和登录页同款卡片按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    horizontal = LoginTokens.ScreenHorizontalPadding,
                    vertical = LoginTokens.BackButtonVerticalPadding
                )
                .zIndex(1f)   // ✅ 提升到最上层
        ) {
            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .clickable { actions.onBack() },
                shape = RoundedCornerShape(12.dp),
                color = HomeTokens.ChipSelectedBackground,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }


        // 中间内容：上半部分大图 + 下半部分直接文本
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1）顶部图片区域 —— 仿照截图二的整块饮料图
// 图片区（上半屏）
            val heroUrl: String? = when {
                uiState.imageUrls.isNotEmpty() -> {
                    val safeIndex = uiState.currentImageIndex.coerceIn(0, uiState.imageUrls.lastIndex)
                    uiState.imageUrls[safeIndex]
                }
                !uiState.imagePreviewUrl.isNullOrBlank() -> uiState.imagePreviewUrl
                else -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DetailTokens.HeroHeight)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DetailTokens.HeroGradientTop,
                                DetailTokens.HeroGradientBottom
                            )
                        )
                    )
                    .clickable(enabled = heroUrl != null) { actions.onOpenImage() },
                contentAlignment = Alignment.Center
            ) {
                if (heroUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(heroUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "No image",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }


                // 多图指示点（图片内部靠下）
                val imageCount = when {
                    uiState.imageUrls.isNotEmpty() -> uiState.imageUrls.size.coerceAtMost(6)
                    uiState.imagePreviewUrl != null -> 1
                    else -> 0
                }

                if (imageCount > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
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
                                            Color.White
                                        else
                                            Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }

            // 2）图片下方直接文本，不再用 Card
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DetailTokens.ContentHorizontalPadding,
                        vertical = DetailTokens.ContentVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Rec. + 名称 + 价格
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    if (uiState.isRecommended) {
                        Text(
                            text = "Rec.",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 30.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = HomeTokens.ChipSelectedBackground
                        )
                    }

                    Text(
                        text = uiState.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 28.sp   // 想要多大就写多少
                        ),
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
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 28.sp   // 想要多大就写多少
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp   // 想要多大就写多少
                            ),
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                        Text(
                            text = uiState.description,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp   // 想要多大就写多少
                            ),
                            color = Color.Black.copy(alpha = 0.9f)
                        )
                    }
                }

                // 分类 + 状态
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!uiState.category.isNullOrBlank()) {
                        Text(
                            text = "Category: ${uiState.category}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp   // 想要多大就写多少
                            ),
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                    }

                    if (uiState.isUnavailable) {
                        Text(
                            text = "Unavailable",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 最底部留一点空白，防止被系统手势遮住
            Spacer(modifier = Modifier.height(32.dp))
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
    var showCategoryCannotDeleteDialog by remember { mutableStateOf(false) }
    var categoryCannotDeleteMessage by remember { mutableStateOf("") }

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


// 顶部 Back 按钮：和 Edit 页同款卡片按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    horizontal = EditDishTokens.BackButtonHorizontalPadding,
                    vertical = EditDishTokens.BackButtonVerticalPadding
                )
                .zIndex(1f)          // 确保在卡片之上
        ) {
            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .clickable { actions.onBack() },
                shape = RoundedCornerShape(12.dp),
                color = HomeTokens.ChipSelectedBackground,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }


        // 中间管理卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(
                    top = EditDishTokens.CardTopPaddingFromScreenTop,
                    start = AdminTokens.CardHorizontalPadding,
                    end = AdminTokens.CardHorizontalPadding,
                    bottom = AdminTokens.CardVerticalPadding
                )
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(AdminTokens.CardCornerRadius),
                    clip = false
                ),
            shape = RoundedCornerShape(AdminTokens.CardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
            // 注意：这里不再传 border 参数，原来的 BorderStroke 整段删掉
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
                            Text(
                                text = "Refresh",
                                color = HomeTokens.ChipSelectedBackground,   // 和 Back 按钮同色
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }                // 分类列表（胶囊按钮样式 + 多选 + 统一删除）
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
                                // ✅ 先判定：选中的分类里，是否存在“仍被菜品引用”的分类
                                val blocked = selectedCategories.filter { cat ->
                                    uiState.dishes.any { dish -> (dish.category ?: "") == cat }
                                }

                                if (blocked.isNotEmpty()) {
                                    // 有菜品引用 → 只能提示“不可删除”，不能让用户确认
                                    categoryCannotDeleteMessage =
                                        "Cannot delete：${blocked.joinToString(", ")}。\n" +
                                                "This category is still used by one or more items. Move those items to another category and try again."
                                    showCategoryCannotDeleteDialog = true
                                } else {
                                    // 没有菜品引用 → 才允许弹“确认删除”
                                    showCategoryDeleteDialog = true
                                }
                            },
                            enabled = selectedCategories.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete selected categories",
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
                                newCategoryName = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TopBannerTokens.Background,   // 和 Add New Dish / Login 同色
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),                 // 同样 12dp 圆角
                        elevation = ButtonDefaults.buttonElevation(        // 同样的阴影
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp,
                            disabledElevation = 0.dp
                        )
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
// 横线下面的 “Add New Dish” 按钮：和 Edit 页 Save changes 同款
                Button(
                    onClick = {
                        actions.onEditDish("")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EditDishTokens.PrimaryButtonHeight)
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TopBannerTokens.Background,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "Add New Dish",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
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
                                                color = HomeTokens.ChipSelectedBackground   // ✅ 和 Back 同色
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
                                        IconButton(onClick = { actions.onEditDish(dish.id) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit item",
                                                tint = HomeTokens.ChipSelectedBackground   // ✅ 和 Back 按钮同色
                                            )
                                        }
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
        // ✅ 分类不可删除提示弹窗：有菜品引用时只提示，不给确认按钮
        if (showCategoryCannotDeleteDialog) {
            InfoDialog(
                title = "Cannot delete category",
                message = categoryCannotDeleteMessage,
                onDismiss = { showCategoryCannotDeleteDialog = false }
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
private fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

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
            subtitle = "",
            category = "Hot",
            price = 9.99,
            isRecommended = true,
            isSoldOut = false
        ),
        RestaurantHomeDish(
            id = "2",
            title = "Caesar salad",
            subtitle = "",
            category = "Special",
            price = 12.50,
            isRecommended = false,
            isSoldOut = false
        ),
        RestaurantHomeDish(
            id = "3",
            title = "French lamb chops",
            subtitle = "",
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
        onRemoveImage = {},
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
    val dialogShape = RoundedCornerShape(DialogTokens.CornerRadius)

    // 直接用 AlertDialog，不再自己加半透明背景 / 毛玻璃
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
        // ✅ 弹窗本体纯白
        containerColor = DialogTokens.ContainerColor,
        tonalElevation = DialogTokens.TonalElevation,
        textContentColor = DialogTokens.ContentColor,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DialogTokens.TitleColor
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = DialogTokens.MessageColor
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
                    color = DialogTokens.MessageColor
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
