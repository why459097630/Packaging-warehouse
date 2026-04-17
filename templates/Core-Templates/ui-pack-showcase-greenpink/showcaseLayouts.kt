@file:OptIn(ExperimentalFoundationApi::class)

package com.ndjc.feature.showcase.ui


//region 1 Imports
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.ime
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.SwitchDefaults
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.SnackbarHostState
import com.ndjc.feature.showcase.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.animateScrollBy
import kotlin.math.abs
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import android.app.Activity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.remember
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Favorite
import com.ndjc.feature.showcase.ShowcaseChatUiState
import com.ndjc.feature.showcase.ShowcaseChatActions
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.text.withStyle
import androidx.compose.material.DropdownMenu
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import coil.compose.AsyncImagePainter
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ListItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

import androidx.compose.material.icons.filled.Favorite
//endregion

//region 2 TOC / 约束说明
// ShowcaseLayouts.kt (UI-only)
// - 本文件是 Showcase UI 包主入口（单文件开发模式）
// - 禁止写入业务逻辑：不得调用 Repository/Supabase、不得修改数据，只负责展示与交互外观
// - Renderer / Wiring 只应调用本文件内标记为 internal 的 Screen Entrypoints
//
// 文件分区（12区）：
// 1 Imports
// 2 TOC / 约束说明
// 3 Tokens（通用 + 局部）
// 4 Home
// 5 Login/Auth
// 6 Edit/CRUD
// 7 Detail/Content
// 8 StoreProfile
// 9 Admin
// 10 Previews
// 11 Chat（UI-only helpers）
// 12 BottomBar（UI-only）
//endregion




//region 3 Tokens（通用 + 局部）
// ------------------------- NDJC 通用 Tokens（自动收敛硬编码常量）-------------------------
// 说明：本文件内禁止直接写死 dp/sp/Color/alpha（除 Tokens 定义区）。
//      UI 只引用 NdjcCommonTokens.* 或现有的 *Tokens。
private object NdjcCommonTokens {
    object Dp {
        val Dp265 = 265.dp
        val Dp0 = 0.dp
        val Dp1 = 1.dp
        val Dp1_5 = 1.5.dp
        val Dp2 = 2.dp
        val Dp3 = 3.dp
        val Dp4 = 4.dp
        val Dp6 = 6.dp
        val Dp7 = 7.dp
        val Dp8 = 8.dp
        val Dp10 = 10.dp
        val Dp12 = 12.dp
        val Dp14 = 14.dp
        val Dp15 = 15.dp
        val Dp16 = 16.dp
        val Dp18 = 18.dp
        val Dp20 = 20.dp
        val Dp22 = 22.dp
        val Dp24 = 24.dp
        val Dp25 = 25.dp
        val Dp26 = 26.dp
        val Dp28 = 28.dp
        val Dp32 = 32.dp
        val Dp34 = 34.dp
        val Dp35 = 35.dp
        val Dp36 = 36.dp
        val Dp40 = 40.dp
        val Dp42 = 42.dp
        val Dp44 = 44.dp
        val Dp46 = 46.dp
        val Dp48 = 48.dp
        val Dp50 = 50.dp
        val Dp52 = 52.dp
        val Dp54 = 54.dp
        val Dp56 = 56.dp
        val Dp60 = 60.dp
        val Dp72 = 72.dp
        val Dp80 = 80.dp
        val Dp84 = 84.dp
        val Dp88 = 88.dp
        val Dp92 = 92.dp
        val Dp96 = 96.dp
        val Dp248 = 248.dp
        val Dp104 = 104.dp
        val Dp100 = 100.dp
        val Dp120 = 120.dp
        val Dp132 = 132.dp
        val Dp140 = 140.dp
        val Dp150 = 150.dp
        val Dp160 = 160.dp
        val Dp220 = 220.dp
        val Dp240 = 240.dp
        val Dp320 = 320.dp
        val Dp500 = 500.dp
        val Dp999 = 999.dp
    }
    object Sp {
        val Sp14 = 14.sp
        val Sp18 = 18.sp
        val Sp21 = 21.sp
        val Sp22 = 22.sp
        val Sp28 = 28.sp
        val Sp30 = 30.sp
        val Sp36 = 36.sp
    }

    object Colors {
        val C_008E5CFF = Color(0x008E5CFF)
        val C_33000000 = Color(0x33000000)
        val C_FF111827 = Color(0xFF111827)
        val C_FF26C6A4 = Color(0xFF26C6A4)
        val C_FF2FD1AE = Color(0xFF2FD1AE)
        val C_FF1FBF9D = Color(0xFF1FBF9D)
        val C_FF374151 = Color(0xFF374151)
        val C_FF4CAF50 = Color(0xFF4CAF50)
        val C_FF6B7280 = Color(0xFF6B7280)
        val C_FF8E5CFF = Color(0xFF8E5CFF)
        val C_FFB37BFF = Color(0xFFB37BFF)
        val C_FFCDD0E0 = Color(0xFFCDD0E0)
        val C_FFDAE9E6 = Color(0xFFDAE9E6)
        val C_FFE4E5F0 = Color(0xFFE4E5F0)
        val C_FFE53935 = Color(0xFFE53935)
        val C_FFE5E7EB = Color(0xFFE5E7EB)
        val C_FFEFF3F2 = Color(0xFFEFF3F2)
        val C_FFF1F1F1 = Color(0xFFF1F1F1)
        val C_FFF3F4F6 = Color(0xFFF3F4F6)
        val C_FFF7F7F7 = Color(0xFFF7F7F7)
        val C_FFF8FAFC = Color(0xFFF8FAFC)
        val C_FFFE9595 = Color(0xFFFE9595)
        val C_FFFF8E8E = Color(0xFFFF8E8E)
        val C_FFFF99B2 = Color(0xFFFF99B2)
        val C_FFFF9ECF = Color(0xFFFF9ECF)
        val C_FFFFC1D9 = Color(0xFFFFC1D9)
    }

    object Alpha {
        val a0 = 0.0f
        val a04 = 0.04f
        val a05 = 0.05f
        val a06 = 0.06f
        val a10 = 0.1f
        val a100 = 1.0f
        val a14 = 0.14f
        val a18 = 0.18f
        val a25 = 0.25f
        val a30 = 0.3f
        val a35 = 0.35f
        val a45 = 0.45f
        val a50 = 0.5f
        val a55 = 0.55f
        val a60 = 0.6f
        val a65 = 0.65f
        val a70 = 0.7f
        val a75 = 0.75f
        val a80 = 0.8f
        val a85 = 0.85f
        val a90 = 0.9f
        val a92 = 0.92f
    }
}
private object NdjcPrimaryActionButtonTokens {

    // ✅ 按钮「最小高度」：统一主操作按钮的高度下限（避免不同页面/不同文案导致按钮忽高忽低）
    // - 用在 Button 的 modifier.heightIn(min = MinHeight)
    val MinHeight = NdjcCommonTokens.Dp.Dp56

    // ✅ 按钮「水平内边距」：按钮内容（文字/Loading）到左右边界的留白
    // - 用在 contentPadding = PaddingValues(horizontal = PaddingH, ...)
    // - 影响按钮“左右胖瘦”
    val PaddingH = NdjcCommonTokens.Dp.Dp18

    // ✅ 按钮「垂直内边距」：按钮内容（文字/Loading）到上下边界的留白
    // - 用在 contentPadding = PaddingValues(..., vertical = PaddingV)
    // - 影响按钮“上下厚度”，也会影响视觉高度（配合 MinHeight 更稳定）
    val PaddingV = NdjcCommonTokens.Dp.Dp12

    // ✅ 按钮「圆角半径」：控制按钮外轮廓的圆角大小（越大越圆）
    // - 用在 shape = RoundedCornerShape(Corner)
    val Corner = NdjcCommonTokens.Dp.Dp16

    // ✅ 按钮「默认海拔/阴影」：按钮静止状态的阴影高度（越大阴影越明显）
    // - 用在 ButtonDefaults.buttonElevation(defaultElevation = Elevation)
    val Elevation = NdjcCommonTokens.Dp.Dp6

    // ✅ 按钮「按下海拔/阴影」：按钮按下时的阴影高度（通常更小，模拟被压下去）
    // - 用在 ButtonDefaults.buttonElevation(pressedElevation = PressedElevation)
    val PressedElevation = NdjcCommonTokens.Dp.Dp2

    // ✅ 外层阴影（用于按钮“悬浮感”），默认 0.dp：不改变现有 UI
    val OuterShadowElevation = NdjcCommonTokens.Dp.Dp10

    // ✅ 外层阴影形状：与按钮圆角一致
    val OuterShadowShape = RoundedCornerShape(Corner)

    // ✅ 外层阴影是否裁剪：一般 false，保留阴影外扩
    val OuterShadowClip = false
}


// --- White Card Layout Tokens（统一白卡与屏幕距离）---
private object NdjcWhiteCardLayoutTokens {

    // ✅ 屏幕级外边距：白卡整体距离屏幕 左右 / 底部 的统一留白
    // - 控制页面整体“呼吸感”
    // - 所有使用 NdjcWhiteCardLayout 的页面都会受影响
    val ScreenPadding = NdjcCommonTokens.Dp.Dp16

    // ✅ 白卡顶部偏移量：白卡距离屏幕顶部的下移距离
    // - 主要用于“顶部有Banner或渐变背景”的页面
    // - 数值越大，卡片越往下
    val CardTopOffset = NdjcCommonTokens.Dp.Dp96

    // ✅ 白卡内部内容边距：卡片内部文字/输入框/按钮的内边距
    // - 控制内容是否贴边
    // - 调大 → 内容更松散
    val CardInnerPaddingHorizontal = NdjcCommonTokens.Dp.Dp16
    val CardInnerPaddingVertical = NdjcCommonTokens.Dp.Dp16

    // ✅ 白卡圆角：控制整张卡片的圆角半径
    // - 数值越大越圆
    val CardRadius = NdjcCommonTokens.Dp.Dp24

    // ✅ 白卡阴影高度：后台白卡用轻阴影更干净
    val CardShadow = NdjcCommonTokens.Dp.Dp3
}

private object NdjcSnackbarTokens {
    val HorizontalPadding = NdjcCommonTokens.Dp.Dp16
    val BottomPadding = NdjcCommonTokens.Dp.Dp22
    val CornerRadius = NdjcCommonTokens.Dp.Dp16
    val ShadowElevation = NdjcCommonTokens.Dp.Dp1
    val ContainerColor = Color.White
    val ContentColor = NdjcCommonTokens.Colors.C_FF111827
}

object NdjcInputTokens {

    // ✅ 输入框圆角形状：统一 OutlinedTextField 的圆角
    // - 当前为 16dp 圆角
    // - 所有 NdjcTextField 都会使用这个 shape
    val Shape = RoundedCornerShape(size = NdjcCommonTokens.Dp.Dp16)

    // ✅ 输入框最小高度：统一单行输入框的高度基准
    // - 用在 heightIn(min = MinHeight)
    // - 默认 56dp（Material 标准高度）
    val MinHeight: Dp = NdjcCommonTokens.Dp.Dp56

    // ==============================
    // 下方是输入框颜色体系 Token
    // ==============================

    // ✅ 聚焦状态下的下划线颜色
    // - 光标在输入框时显示
    val FocusedIndicator = Color.Black

    // ✅ 未聚焦状态下的下划线颜色
    // - 默认淡一点
    val UnfocusedIndicator =
        Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)

    // ✅ 禁用状态下的下划线颜色
    val DisabledIndicator =
        Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a30)

    // ✅ 聚焦状态下的文字颜色
    val FocusedText = Color.Black

    // ✅ 未聚焦状态下的文字颜色
    val UnfocusedText = Color.Black

    // ✅ 禁用状态下的文字颜色
    val DisabledText =
        Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a50)

    @Composable
    fun colors(): TextFieldColors =
        TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,

            focusedIndicatorColor = FocusedIndicator,
            unfocusedIndicatorColor = UnfocusedIndicator,
            disabledIndicatorColor = DisabledIndicator,
            errorIndicatorColor = MaterialTheme.colorScheme.error,

            focusedTextColor = FocusedText,
            unfocusedTextColor = UnfocusedText,
            disabledTextColor = DisabledText,
            errorTextColor = MaterialTheme.colorScheme.error,

            cursorColor = Color.Black,

            focusedLabelColor = Color.Black.copy(alpha = 0.75f),
            unfocusedLabelColor = Color.Black.copy(alpha = 0.60f),
            disabledLabelColor = Color.Black.copy(alpha = 0.40f),
            errorLabelColor = MaterialTheme.colorScheme.error,

            focusedPlaceholderColor = Color.Black.copy(alpha = 0.45f),
            unfocusedPlaceholderColor = Color.Black.copy(alpha = 0.45f),
            disabledPlaceholderColor = Color.Black.copy(alpha = 0.35f),
            errorPlaceholderColor = MaterialTheme.colorScheme.error
        )
}
// ✅ 通用纯外观：胶囊按钮 Tokens（所有 Sort/Filter/Price/Category 共用）
private object NdjcPillButtonTokens {
    val Shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999)

    // 统一内边距（沿用你现有 HomeTokens）
    val PaddingH = HomeTokens.ChipInnerHorizontalPadding
    val PaddingV = HomeTokens.ChipInnerVerticalPadding

    // 统一颜色（沿用你现有 HomeTokens）
    val SelectedBg = HomeTokens.ChipSelectedBackground
    val UnselectedBg = HomeTokens.ChipUnselectedBackground

    // ✅ 细边框：未选中更“精致”
    val BorderWidth = HomeTokens.ChipBorderWidth
    val BorderColor = Color.Black.copy(alpha = 0.12f)

    val SelectedText = Color.White
    val UnselectedText = Color.Black
}
// ✅ 通用纯外观：Label + Switch 行（商品编辑页 / 登录页 等共用）
private object NdjcToggleRowTokens {
    // Switch 颜色（保持你现有商品编辑页开关视觉不变）
    val CheckedTrackColor = TopBannerTokens.Background
    val CheckedThumbColor = Color.White
    val UncheckedTrackColor = NdjcCommonTokens.Colors.C_FFE4E5F0
    val UncheckedThumbColor = NdjcCommonTokens.Colors.C_FFCDD0E0

    // 文案颜色（保持现有 EditDish 的黑色）
    val LabelColor = Color.Black
}
// ✅ 通用纯外观：商品卡片（Favorites / Admin Items 复用）
// - 只负责长相：封面、名称、现价/原价、分类
// - 页面右侧按钮/checkbox 由页面层传入 trailingContent 拼装
private object NdjcCatalogItemCardTokens {
    val Shape = RoundedCornerShape(size = NdjcCommonTokens.Dp.Dp14)              // 卡片外框圆角（整张卡片的圆角）
    val ShadowElevation = NdjcCommonTokens.Dp.Dp2                                // 卡片阴影高度（阴影强弱/浮起感）
    val ContainerColor = Color.White                                             // 卡片背景色（Surface/容器底色）

    val PaddingStart = NdjcCommonTokens.Dp.Dp10                                  // 卡片内容区左内边距（内容离左边框距离：影响图片/文字/按钮整体）
    val PaddingEnd = NdjcCommonTokens.Dp.Dp10                                    // 卡片内容区右内边距（内容离右边框距离：影响图片/文字/按钮整体）
    val PaddingTop = NdjcCommonTokens.Dp.Dp10                                    // 卡片内容区上内边距（内容离上边框距离：影响图片/文字/按钮整体）
    val PaddingBottom = NdjcCommonTokens.Dp.Dp10                                 // 卡片内容区下内边距（内容离下边框距离：影响图片/文字/按钮整体）
    val Gap = NdjcCommonTokens.Dp.Dp10                                           // 卡片主横向间距（封面图 与 右侧信息区/按钮区之间的间距）

    val ImageSize = NdjcCommonTokens.Dp.Dp92                            // 封面图尺寸（宽=高：方形缩略图边长）
    val ImageShape = RoundedCornerShape(size = NdjcCommonTokens.Dp.Dp12)         // 封面图圆角（只影响图片/占位块的圆角）

    val CategoryChipShape = RoundedCornerShape(size = NdjcCommonTokens.Dp.Dp999) // 分类/标签 Chip 形状（999=胶囊形/全圆角）
    val CategoryChipPaddingH = NdjcCommonTokens.Dp.Dp10                          // Chip 内部左右 padding（文字离 chip 边缘的水平留白）
    val CategoryChipPaddingV = NdjcCommonTokens.Dp.Dp6                           // Chip 内部上下 padding（文字离 chip 边缘的垂直留白）
    // ✅ 仅 NdjcCatalogItemCard 内分类“选中chip”外观（不影响 CategoryChipsRow / NdjcPillButton）
    val CardCategorySelectedBg = HomeTokens.ChipSelectedBackground
    val CardCategorySelectedShape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999)
    val CardCategorySelectedPaddingH = HomeTokens.ChipInnerHorizontalPadding
    val CardCategorySelectedPaddingV = HomeTokens.ChipInnerVerticalPadding
    // 注意：tokens 里不要用 MaterialTheme（否则触发 @Composable invocations）
    val CategoryChipBg = NdjcCommonTokens.Colors.C_FFE4E5F0                      // Chip 背景色（分类/标签底色；无图占位用到的话也会影响占位块底色）

    val PriceRowGap = NdjcCommonTokens.Dp.Dp8                                    // 价格行内元素间距（原价/现价/折扣等在同一行时的间隔）
    val PriceTopGap = NdjcCommonTokens.Dp.Dp0                                    // 价格区与上方内容的间距（价格行顶部留白/与标题或标签的垂直间隔）
}
// ===========================
// 商品卡片四项可调 Tokens
// ===========================
private object NdjcCatalogItemTextTokens {

    // ----------------------
    // 1️⃣ 名称
    // ----------------------
    val TitleFontSize = 25.sp
    val TitleLineHeight = 20.sp
    val TitleFontWeight = FontWeight.Bold
    val TitleColor = NdjcCommonTokens.Colors.C_FF111827
    val TitleTopExtra = NdjcCommonTokens.Dp.Dp0

    // 名称位置偏移（真正移动位置）
    val TitleOffsetX = NdjcCommonTokens.Dp.Dp0
    val TitleOffsetY = NdjcCommonTokens.Dp.Dp0

    // ----------------------
    // 2️⃣ 分类
    // ----------------------

    val CategoryColor = NdjcCommonTokens.Colors.C_FF374151

    val CategoryOffsetX = NdjcCommonTokens.Dp.Dp0
    val CategoryOffsetY = NdjcCommonTokens.Dp.Dp0
    // ----------------------
// 2️⃣ 分类（仅卡片内用这个字号；不会影响 CategoryChipsRow）
// ----------------------
    val CategoryFontSize = 12.sp
    val CategoryLineHeight = 12.sp
    val CategoryFontWeight = FontWeight.SemiBold
    val CategorySelectedTextColor = Color.White

    // ----------------------
    // 3️⃣ 折后价
    // ----------------------
    val DiscountFontSize = 22.sp
    val DiscountLineHeight = 18.sp
    val DiscountFontWeight = FontWeight.SemiBold
    val DiscountColor = HomeTokens.ChipSelectedBackground

    val DiscountOffsetX = NdjcCommonTokens.Dp.Dp0
    val DiscountOffsetY = NdjcCommonTokens.Dp.Dp0

    // ----------------------
    // 4️⃣ 原价
    // ----------------------
    val OriginalFontSize = 15.sp
    val OriginalLineHeight = 16.sp
    val OriginalColor = NdjcCommonTokens.Colors.C_FF111827
    val OriginalAlpha = NdjcCommonTokens.Alpha.a60

    val OriginalOffsetX = NdjcCommonTokens.Dp.Dp0
    val OriginalOffsetY = NdjcCommonTokens.Dp.Dp0

    // 折后价与原价之间间距
    val PriceGap = NdjcCommonTokens.Dp.Dp8
    val PriceBottomExtra = NdjcCommonTokens.Dp.Dp0

    // ✅ 让字形更贴近容器边缘：去掉字体自带上下 padding + 裁剪行高留白
    val NoFontPadding = PlatformTextStyle(includeFontPadding = false)
    val TrimLineHeight = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
}
// ------------------------------------------------------------------------------

// ------------------------- NDJC 通用 Tokens（本文件自动收敛硬编码）-------------------------

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
        unfocusedIndicatorColor = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70),
        disabledIndicatorColor = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a30),
        errorIndicatorColor = MaterialTheme.colorScheme.error,

        // 字体颜色保持默认就行（也可以显式写）
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a50),
        errorTextColor = MaterialTheme.colorScheme.error
    )
// ---------- Detail 页局部 Token ----------

object DetailTokens {
    // 顶部图片区域高度 & 圆角
    val HeroHeight = NdjcCommonTokens.Dp.Dp320
    val HeroCornerRadius = NdjcCommonTokens.Dp.Dp25

    // 顶部图片背景渐变（接近你给的粉色饮料卡）
    val HeroGradientTop = NdjcCommonTokens.Colors.C_FFFF9ECF
    val HeroGradientBottom = NdjcCommonTokens.Colors.C_FFFFC1D9

    // 下半部分内容卡片
    val ContentCornerRadius = NdjcCommonTokens.Dp.Dp32
    val ContentHorizontalPadding = NdjcCommonTokens.Dp.Dp24
    val ContentVerticalPadding = NdjcCommonTokens.Dp.Dp24
    val MutedText = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a60)
    // 内容卡片往上叠一小截，制造「卡片压在图片上」的效果
    val ContentOverlap = (-32).dp
}
object TopBannerTokens {
    // ✅ 兼容旧引用（Switch / 其它组件仍在用纯色）
    val Background = NdjcCommonTokens.Colors.C_FF26C6A4

    // 更“轻高级”的顶部卡片：用渐变替代纯色（仍然是绿色体系）
    val GradientTop = NdjcCommonTokens.Colors.C_FF2FD1AE
    val GradientBottom = NdjcCommonTokens.Colors.C_FF1FBF9D

    // 顶部卡片圆角、高度、阴影（进一步减重：让商品成为主角）
    val CornerRadius = NdjcCommonTokens.Dp.Dp22
    val Height = NdjcCommonTokens.Dp.Dp60
    val ShadowElevation = NdjcCommonTokens.Dp.Dp4
}
object HomeTokens {
    // 背景渐变
    val BackgroundGradientTop = NdjcCommonTokens.Colors.C_FFEFF3F2
    val BackgroundGradientBottom = NdjcCommonTokens.Colors.C_FFEFF3F2
    val SectionGap = NdjcCommonTokens.Dp.Dp12
    val ControlsGap = NdjcCommonTokens.Dp.Dp2

    // ✅ 分类 chips ↔ 商品列表专用间距（解决“太近”）
    val ChipsToListGap = NdjcCommonTokens.Dp.Dp14

    // 背景圆
    val HeroCircleSize = NdjcCommonTokens.Dp.Dp0
    val HeroCircleOffsetLeftX = (-100).dp
    val HeroCircleOffsetLeftY = (-40).dp
    val HeroCircleOffsetRightX = NdjcCommonTokens.Dp.Dp100
    val HeroCircleOffsetRightY = NdjcCommonTokens.Dp.Dp500

    // 品牌色
    val BrandPurple = NdjcCommonTokens.Colors.C_FFB37BFF
    val BrandPurpleAlt = NdjcCommonTokens.Colors.C_FF8E5CFF
    val BrandPurpleTransparent = NdjcCommonTokens.Colors.C_008E5CFF

    // 页面间距
    val ScreenHorizontalPadding = NdjcCommonTokens.Dp.Dp16
    val ScreenVerticalPadding = NdjcCommonTokens.Dp.Dp10
    val ScreenSectionTopPadding = NdjcCommonTokens.Dp.Dp0
    val TopBarHorizontalPadding = NdjcCommonTokens.Dp.Dp16

    // ✅ 搜索栏整体往上：减少额外 top padding（安全区仍由 statusBars insets 保留）
    val TopBarVerticalPadding = NdjcCommonTokens.Dp.Dp2

    val ListItemSpacing = NdjcCommonTokens.Dp.Dp12

    // 分类 Chip 区
    val ChipRowHorizontalPadding = NdjcCommonTokens.Dp.Dp16
    val ChipSpacing = NdjcCommonTokens.Dp.Dp6
    // 单个 Chip 内部（更薄、更精致）
    val ChipInnerHorizontalPadding = NdjcCommonTokens.Dp.Dp12
    val ChipInnerVerticalPadding = NdjcCommonTokens.Dp.Dp6

    // ✅ 用于 Pill 的细边框（更“产品化”）
    val ChipBorderWidth = NdjcCommonTokens.Dp.Dp1_5

    // ✅ 未选中更轻：白底轻透明（减少“测试按钮”质感）
    val ChipUnselectedBackground = Color.White.copy(alpha = 0.72f)

    // ✅ 选中统一品牌紫（减少粉色“杂”感）
    val ChipSelectedBackground = BackButtonTokens.Background

    // 搜索 / Icon 尺寸（整体减重：搜索是工具，商品才是主角）
    val SearchBarHeight = NdjcCommonTokens.Dp.Dp46
    val SearchIconSize = NdjcCommonTokens.Dp.Dp20
    val SearchInnerHorizontalPadding = NdjcCommonTokens.Dp.Dp12
    val SearchTextStartSpacing = NdjcCommonTokens.Dp.Dp8
    val SearchIconColor = Color.White
    val ProfileIconColor = Color.White

    // 头像按钮（略减重）
    val ProfileButtonSize = NdjcCommonTokens.Dp.Dp36
    val ProfileButtonAlpha = 0f
    val ProfileIconSize = NdjcCommonTokens.Dp.Dp24
}

object DishCardTokens {
    // ✅ 结构：上图下白区
    val ImageHeightRatio = 0.64f
    val BottomBackground = Color.White

    // ✅ 按压动效（保留方案A）：只保留缩放，不再做阴影变化
    val PressedScale = 0.965f
    val PressedShadowFactor = 0.0f

    // ✅ 文本颜色：白区用深色
    val TitleTextColor = Color.Black
    val PriceTextColor = Color.Black
    val OriginalPriceTextColor = Color.Black

    // ✅ 价格字号
    val PriceFontSize = NdjcCommonTokens.Sp.Sp22

    // ✅ 原价字号：稍微大一点（更接近折后价，避免“看不清”）
    val OriginalPriceFontSize = NdjcCommonTokens.Sp.Sp14

    // ✅ 白区 padding（给 Featured/价格更多呼吸感）
    val ContentPaddingTop = NdjcCommonTokens.Dp.Dp10

    // ✅ Surface 背景：透明（让“上图/下白区”自己决定颜色，不被整体底色污染）
    val CardBackground = Color.Transparent
    // ✅ 卡片阴影：彻底关闭
    val ShadowElevation = NdjcCommonTokens.Dp.Dp0

    // ✅ 卡片内容内边距（标题/价格区域）
    val ContentPaddingStart = NdjcCommonTokens.Dp.Dp12
    val ContentPaddingEnd = NdjcCommonTokens.Dp.Dp12
    val ContentPaddingBottom = NdjcCommonTokens.Dp.Dp12
    val RecColor = HomeTokens.BrandPurpleAlt
    val Height = NdjcCommonTokens.Dp.Dp265
    val BottomMinHeight = NdjcCommonTokens.Dp.Dp72
    val CornerRadius = NdjcCommonTokens.Dp.Dp10

    // ✅ 给 LoginTokens / 其它旧用法兜底（纯 UI 透明度）
    val GlassBgAlpha = 0.26f


    // ✅ 只显示“标题 + 价格”，meta 不再需要（保留 token 不影响别处引用）
    val MetaTextColor = Color.White.copy(alpha = 0.72f)

    // ✅ 两行结构给更好的呼吸感（标题↔价格）
    val LinesSpacing = NdjcCommonTokens.Dp.Dp6

    // ✅ 原价更靠近折后价：行内间距更紧一点
    val PriceRowSpacing = NdjcCommonTokens.Dp.Dp4

    // ✅ Badge（图标 + 文本）
    val BadgeText = "Pick"
    val BadgeTextColor = Color.White
    val BadgeFontSize = 12.sp
    val BadgeBg = BackButtonTokens.Background
    val BadgeBorderColor = Color.White.copy(alpha = 0.38f)
    val BadgeBorderWidth = NdjcCommonTokens.Dp.Dp1
    val BadgeRadius = NdjcCommonTokens.Dp.Dp999
    val BadgePaddingH = NdjcCommonTokens.Dp.Dp8
    val BadgePaddingV = NdjcCommonTokens.Dp.Dp3

    // ✅ 原价透明度（更弱，层级更清晰）
    val OriginalPriceAlpha = 0.55f

    // ✅ 底部渐变安全区：高度+强度微调，让复杂图也稳
    val OverlayHeight = NdjcCommonTokens.Dp.Dp104
    val OverlayTop = Color.Transparent
    val OverlayBottom = Color.Black.copy(alpha = 0.68f)

    // ✅ 收藏状态浮层：扁平化（无底无边框，只显示品牌色爱心）
    val FavContainerSize = NdjcCommonTokens.Dp.Dp18
    val FavBg = Color.Transparent
    val FavBorderColor = Color.Transparent
    val FavBorderWidth = NdjcCommonTokens.Dp.Dp0
    val FavIconSize = NdjcCommonTokens.Dp.Dp18
    val FavIconTint = BackButtonTokens.Background

    // 保留你现有边框/内圆角体系（不影响其他页面）

    val BorderGradientStart = Color.White.copy(alpha = NdjcCommonTokens.Alpha.a50)
    val BorderGradientMidSoft = Color.White.copy(alpha = NdjcCommonTokens.Alpha.a0)
    val BorderGradientMidHard = HomeTokens.BrandPurpleAlt.copy(alpha = NdjcCommonTokens.Alpha.a0)
    val BorderGradientEnd = HomeTokens.BrandPurpleAlt.copy(alpha = NdjcCommonTokens.Alpha.a100)
    val BorderWidth = NdjcCommonTokens.Dp.Dp1_5
    val ImageAreaBackground = Color.White.copy(alpha = 0.92f)
    val ContentInsetFromBorder = NdjcCommonTokens.Dp.Dp1_5
    val InnerCornerRadius = CornerRadius - ContentInsetFromBorder
}
object HomeControlsTokens {
    // ✅ 控制条容器（轻底色 + 轻边框）
    val ContainerBg = Color.White.copy(alpha = 0.60f)
    val ContainerBorder = Color.Black.copy(alpha = 0.08f)
    val ContainerRadius = NdjcCommonTokens.Dp.Dp16
    val ContainerPaddingH = NdjcCommonTokens.Dp.Dp12
    val ContainerPaddingV = NdjcCommonTokens.Dp.Dp8

    // ✅ 控制条 pill（与分类 chips 的“选中态”对齐）
    val PillBg = Color.White.copy(alpha = 0.78f)
    val PillBgActive = HomeTokens.ChipSelectedBackground
    val PillBorder = Color.Black.copy(alpha = 0.12f)
    val PillBorderActive = HomeTokens.ChipSelectedBackground
    val PillBorderWidth = NdjcCommonTokens.Dp.Dp1
    val PillText = Color.Black
    val PillTextActive = Color.White
}
// ---------- 登录页局部 Token ----------

object LoginTokens {
    // 对齐首页的品牌与背景
    val BackgroundGradientTop = HomeTokens.BackgroundGradientTop
    val BackgroundGradientBottom = HomeTokens.BackgroundGradientBottom
    val BrandPurple = HomeTokens.BrandPurple
    val BrandPurpleAlt = HomeTokens.BrandPurpleAlt

    // 卡片样式
    val CardCornerRadius = NdjcCommonTokens.Dp.Dp24
    val CardBgAlpha = DishCardTokens.GlassBgAlpha
    val CardBorderWidth = NdjcCommonTokens.Dp.Dp1_5

    // 布局间距
    val ScreenHorizontalPadding = NdjcCommonTokens.Dp.Dp16
    val CardHorizontalPadding = NdjcCommonTokens.Dp.Dp24
    val CardVerticalPadding = NdjcCommonTokens.Dp.Dp24
    val BackButtonVerticalPadding = NdjcCommonTokens.Dp.Dp16
    val BackFontSize = NdjcCommonTokens.Sp.Sp21       // 后退大小就调这里
    val BackFontWeight = FontWeight.SemiBold     // 后退字体粗细
}

// ---------- Edit Dish 页局部 Token ----------

object EditDishTokens {
    val CardCornerRadius = NdjcCommonTokens.Dp.Dp24
    val CardHorizontalPadding = NdjcCommonTokens.Dp.Dp24
    val CardVerticalPadding = NdjcCommonTokens.Dp.Dp24

    val ImageBoxHeight = NdjcCommonTokens.Dp.Dp160
    val SectionSpacing = NdjcCommonTokens.Dp.Dp16
    val FieldSpacing = NdjcCommonTokens.Dp.Dp12
    val ToggleRowSpacing = NdjcCommonTokens.Dp.Dp12

    val PrimaryButtonHeight = NdjcCommonTokens.Dp.Dp52
    val ButtonCornerRadius = NdjcCommonTokens.Dp.Dp999

    // 顶部返回按钮与卡片外边距
    val BackButtonHorizontalPadding = NdjcCommonTokens.Dp.Dp24
    val BackButtonVerticalPadding = NdjcCommonTokens.Dp.Dp42
    val CardTopPaddingFromScreenTop = NdjcCommonTokens.Dp.Dp100

    // 详情页图片圆角
    val ImageCornerRadius = NdjcCommonTokens.Dp.Dp20
}

// ---------- Admin 管理页局部 Token ----------

object AdminTokens {
    val CardCornerRadius = NdjcCommonTokens.Dp.Dp24
    val CardHorizontalPadding = NdjcCommonTokens.Dp.Dp24
    val CardVerticalPadding = NdjcCommonTokens.Dp.Dp24
    val SectionSpacing = NdjcCommonTokens.Dp.Dp16
    val RowSpacing = NdjcCommonTokens.Dp.Dp8
    val IconSize = NdjcCommonTokens.Dp.Dp20
}
// Admin 内部子页面：Home 只放入口按钮；Categories / Items 为独立页面
enum class AdminSubPage { Home, Categories, Items }

// ---------- 弹窗局部 Token ----------

object DialogTokens {
    val CornerRadius = NdjcCommonTokens.Dp.Dp24
    val TonalElevation = NdjcCommonTokens.Dp.Dp10
    val ContainerColor = Color.White
    val ContentColor = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a80)
    val TitleColor = Color.Black
    val MessageColor = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
    val SecondaryActionColor = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a60)
    val PrimaryActionColor = TopBannerTokens.Background
}

@Composable
private fun NdjcBaseDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String? = null,
    dismissText: String? = null,
    confirmText: String,
    onDismissClick: (() -> Unit)? = null,
    onConfirmClick: () -> Unit,
    confirmEnabled: Boolean = true,
    destructiveConfirm: Boolean = false,
    textContent: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(DialogTokens.CornerRadius),
        containerColor = DialogTokens.ContainerColor,
        tonalElevation = DialogTokens.TonalElevation,
        titleContentColor = DialogTokens.TitleColor,
        textContentColor = DialogTokens.ContentColor,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = DialogTokens.TitleColor
            )
        },
        text = {
            if (textContent != null) {
                textContent()
            } else if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DialogTokens.MessageColor
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmClick,
                enabled = confirmEnabled
            ) {
                Text(
                    text = confirmText,
                    color = when {
                        !confirmEnabled -> Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a30)
                        destructiveConfirm -> MaterialTheme.colorScheme.error
                        else -> TopBannerTokens.Background
                    }
                )
            }
        },
        dismissButton = {
            if (!dismissText.isNullOrBlank() && onDismissClick != null) {
                TextButton(onClick = onDismissClick) {
                    Text(
                        text = dismissText,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a60)
                    )
                }
            }
        }
    )
}

object ChatTokens {
    // 气泡最大宽度（屏幕比例）
    val BubbleMaxWidth = 0.78f

    // ✅ 高级感：圆角更克制（原来 18 太“可爱风”）
    val BubbleCorner = NdjcCommonTokens.Dp.Dp14

    // ✅ Chat 气泡“方向感”小角（让它更像聊天而不是 tag）
    val BubbleCornerTight = NdjcCommonTokens.Dp.Dp10

    // ✅ 气泡轻阴影：只要一点点立体感
    val BubbleShadow = NdjcCommonTokens.Dp.Dp1

    // ✅ 日期分割胶囊
    val DatePillCorner = NdjcCommonTokens.Dp.Dp999
    val DatePillPaddingH = NdjcCommonTokens.Dp.Dp12
    val DatePillPaddingV = NdjcCommonTokens.Dp.Dp6

    // 引用块
    val QuoteCorner = NdjcCommonTokens.Dp.Dp12
    val QuoteGap = NdjcCommonTokens.Dp.Dp3
    val QuoteInnerPaddingH = NdjcCommonTokens.Dp.Dp10
    val QuoteInnerPaddingV = NdjcCommonTokens.Dp.Dp6
    val QuoteBorderWidth = NdjcCommonTokens.Dp.Dp1

    // ✅ 商品卡片（Chat 内）降权：更轻、更贴合聊天
    val ProductCorner = NdjcCommonTokens.Dp.Dp14
    val ProductShadow = NdjcCommonTokens.Dp.Dp0
    val ProductPaddingH = NdjcCommonTokens.Dp.Dp12
    val ProductPaddingV = NdjcCommonTokens.Dp.Dp10
    val ProductMaxWidth = NdjcCommonTokens.Dp.Dp320

    // ✅ 输入区
    val InputCorner = NdjcCommonTokens.Dp.Dp14
    val InputMinHeight = NdjcCommonTokens.Dp.Dp42
    val InputMaxHeight = NdjcCommonTokens.Dp.Dp220
    val SendCorner = NdjcCommonTokens.Dp.Dp999
    val SendHeight = NdjcCommonTokens.Dp.Dp40

    // ✅ 输入区左侧工具按钮（与 Send 同体系）
    val ToolBtnSize = NdjcCommonTokens.Dp.Dp40
    val ToolBtnCorner = NdjcCommonTokens.Dp.Dp12
    val ToolIconSize = NdjcCommonTokens.Dp.Dp18

    // ✅ Chat 输入框上方“待发送图片预览条”
    val DraftPreviewSize = NdjcCommonTokens.Dp.Dp96
    val DraftPreviewCorner = NdjcCommonTokens.Dp.Dp14
    val DraftPreviewRemoveButtonSize = NdjcCommonTokens.Dp.Dp24
}
object BackButtonTokens {
    // ✅ 回退到修改前的 Back 背景色（你工程里已存在这个常量）
    val Background = NdjcCommonTokens.Colors.C_FFFE9595

    val IconTint = Color.White
    val Size = NdjcCommonTokens.Dp.Dp50
    val CornerRadius = NdjcCommonTokens.Dp.Dp12
    val ShadowElevation = NdjcCommonTokens.Dp.Dp6
}

/**
 * ✅ 顶部导航（Back/Home）统一 token
 * - 左右边距：对齐白卡外边距
 * - 顶部：离状态栏留一点点
 */
object NdjcTopNavTokens {
    val HorizontalPadding = NdjcWhiteCardLayoutTokens.ScreenPadding
    val TopPadding = NdjcCommonTokens.Dp.Dp2

    // ✅ 统一：Back/Home 到“页面内容/白卡”的垂直距离
    val ToCardSpacing = 30.dp

    // ✅ Chat / Announcement 共用壳子：Back/Home 到下方分割线的距离
    val ConversationHeaderSpacing = NdjcCommonTokens.Dp.Dp0

    // ✅ Chat / Announcement 共用壳子：标题栏本体高度
    val ConversationHeaderBarHeight = 58.dp
}
private object StoreProfileTokens {
    // ✅ 仅 StoreProfile：Back/Home 到内容/白卡的距离（不跟随 NdjcTopNavTokens.ToCardSpacing）
    // 你想要多大就改这里，比如 24.dp / 28.dp / 20.dp
    val ToCardSpacing: Dp = 75.dp

    // ✅ StoreProfile 内容区顶部起始位置
    val TopContentPadding: Dp =
        BackButtonTokens.Size + NdjcTopNavTokens.TopPadding + ToCardSpacing
}
/** ✅ 统一顶部导航配置（UI 只负责转发回调，不负责导航逻辑） */
data class NdjcTopNavConfig(
    val onBack: () -> Unit,
    val onHome: () -> Unit,
    // 可选：保持某些页面“原本的按钮位置”不变（不传则使用 NdjcTopNavTokens）
    val horizontalPadding: Dp? = null,
    val topPadding: Dp? = null,
    val iconOnly: Boolean = false,
    val iconTint: Color = BackButtonTokens.IconTint
)
// ✅ TopAppBar 页面：为统一顶部 Back/Home 预留的垂直空间（避免标题区被遮挡）
private val NdjcTopNavReservedHeight: Dp =
    BackButtonTokens.Size + NdjcTopNavTokens.TopPadding + NdjcTopNavTokens.ToCardSpacing

// ✅ 全站统一：页面“内容/白卡”距离屏幕顶部的起始位置（所有页面都依赖它）
private val NdjcTopContentPadding: Dp = NdjcTopNavReservedHeight
@Composable
private fun NdjcCardBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.ArrowBack,
    contentDescription: String = "Back",
    iconOnly: Boolean = false,
    iconTint: Color = BackButtonTokens.IconTint,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "NdjcCardBackButtonScale"
    )

    if (iconOnly) {
        Box(
            modifier = modifier
                .size(BackButtonTokens.Size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false)
                ) {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint
            )
        }
    } else {
        Surface(
            modifier = modifier
                .size(BackButtonTokens.Size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = RoundedCornerShape(BackButtonTokens.CornerRadius),
            color = BackButtonTokens.Background,
            shadowElevation = if (pressed) NdjcCommonTokens.Dp.Dp2 else BackButtonTokens.ShadowElevation
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(BackButtonTokens.CornerRadius))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true)
                    ) {
                        onClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = BackButtonTokens.IconTint
                )
            }
        }
    }
}
@Composable
fun NdjcPrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.965f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "NdjcPrimaryActionButtonScale"
    )

    Surface(
        modifier = modifier
            .heightIn(min = NdjcPrimaryActionButtonTokens.MinHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(NdjcPrimaryActionButtonTokens.Corner),
        color = if (enabled) {
            TopBannerTokens.Background
        } else {
            Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10)
        },
        shadowElevation = if (enabled) {
            if (pressed) NdjcPrimaryActionButtonTokens.PressedElevation else NdjcPrimaryActionButtonTokens.Elevation
        } else {
            NdjcCommonTokens.Dp.Dp0
        },
        tonalElevation = NdjcCommonTokens.Dp.Dp0
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = NdjcPrimaryActionButtonTokens.MinHeight)
                .clip(RoundedCornerShape(NdjcPrimaryActionButtonTokens.Corner))
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true)
                ) {
                    onClick()
                }
                .padding(
                    horizontal = NdjcPrimaryActionButtonTokens.PaddingH,
                    vertical = NdjcPrimaryActionButtonTokens.PaddingV
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(NdjcCommonTokens.Dp.Dp18),
                    strokeWidth = NdjcCommonTokens.Dp.Dp2,
                    color = Color.White
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        Color.White
                    } else {
                        Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a45)
                    }
                )
            }
        }
    }
}
@Composable
private fun NdjcBlockingProgressOverlay(
    visible: Boolean,
    text: String
) {
    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .clickable(
                enabled = true,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {},
        contentAlignment = Alignment.Center
    ) {
        NdjcWhiteCard(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(min = 180.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NdjcFilterBottomSheet(
    onDismissRequest: () -> Unit,
    onClear: () -> Unit,
    priceMinDraft: String,
    onPriceMinDraftChange: (String) -> Unit,
    priceMaxDraft: String,
    onPriceMaxDraftChange: (String) -> Unit,
    onApply: () -> Unit,
    filterContent: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White.copy(alpha = 0.98f),
        tonalElevation = NdjcCommonTokens.Dp.Dp0
    ) {
        SheetHeader(
            title = "Filter",
            onClear = onClear
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = NdjcCommonTokens.Dp.Dp16,
                    end = NdjcCommonTokens.Dp.Dp16,
                    bottom = NdjcCommonTokens.Dp.Dp24
                ),
            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NdjcTextField(
                    value = priceMinDraft,
                    onValueChange = onPriceMinDraftChange,
                    label = "Min price",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                NdjcTextField(
                    value = priceMaxDraft,
                    onValueChange = onPriceMaxDraftChange,
                    label = "Max price",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            filterContent()

            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))

            NdjcPrimaryActionButton(
                text = "Apply",
                onClick = onApply,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
@Composable
private fun NdjcSystemBars(
    color: Color,
    darkIcons: Boolean,
    key: Any? = Unit,
    navigationBarColor: Color? = null,
    lightNavIcons: Boolean? = null,
    decorFitsSystemWindows: Boolean? = null
) {
    val view = LocalView.current
    val activity = view.context as? Activity

    DisposableEffect(key, color, darkIcons, navigationBarColor, lightNavIcons, decorFitsSystemWindows) {
        val window = activity?.window
        if (window == null) return@DisposableEffect onDispose { }

        val oldStatus = window.statusBarColor
        val oldNav = window.navigationBarColor
        val controller = WindowInsetsControllerCompat(window, view)
        val oldLightStatus = controller.isAppearanceLightStatusBars
        val oldLightNav = controller.isAppearanceLightNavigationBars

        if (decorFitsSystemWindows != null) {
            WindowCompat.setDecorFitsSystemWindows(window, decorFitsSystemWindows)
        }

        window.statusBarColor = color.toArgb()
        controller.isAppearanceLightStatusBars = darkIcons

        if (navigationBarColor != null) {
            window.navigationBarColor = navigationBarColor.toArgb()
        }
        if (lightNavIcons != null) {
            controller.isAppearanceLightNavigationBars = lightNavIcons
        }

        onDispose {
            window.statusBarColor = oldStatus
            window.navigationBarColor = oldNav
            controller.isAppearanceLightStatusBars = oldLightStatus
            controller.isAppearanceLightNavigationBars = oldLightNav
        }
    }
}

@Composable
private fun NdjcSystemBarsTransparent(
    darkIcons: Boolean,
    key: Any? = Unit
) {
    NdjcSystemBars(
        color = Color.Transparent,
        darkIcons = darkIcons,
        key = key,
        decorFitsSystemWindows = false
    )
}
internal object NdjcFullscreenViewerRegistry {
    val visible = androidx.compose.runtime.mutableStateOf(false)
}
@Composable
private fun NdjcUnifiedBackground(
    modifier: Modifier = Modifier,
    topNav: NdjcTopNavConfig? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // ✅ 全站默认：透明状态栏 + 深色图标 + 内容可顶到状态栏区域（纯 UI 外观控制，不越界）
    NdjcSystemBarsTransparent(
        darkIcons = true
    )

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val fullscreenViewerVisible by NdjcFullscreenViewerRegistry.visible

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }
                }
            }
            // ✅ 统一所有页面背景色 = Home 的“纯色背景”
            .background(HomeTokens.BackgroundGradientTop)
    ) {
        content()

        if (topNav != null && !fullscreenViewerVisible) {
            NdjcTopNavOverlay(config = topNav)
        }
    }
}
@Composable
private fun BoxScope.NdjcTopNavOverlay(
    config: NdjcTopNavConfig,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = (config.horizontalPadding ?: NdjcTopNavTokens.HorizontalPadding),
                end = (config.horizontalPadding ?: NdjcTopNavTokens.HorizontalPadding),
                top = (config.topPadding ?: NdjcTopNavTokens.TopPadding)
            )
            .align(Alignment.TopCenter)
            .zIndex(10f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NdjcCardBackButton(
            onClick = config.onBack,
            contentDescription = "Back",
            iconOnly = config.iconOnly,
            iconTint = config.iconTint
        )
        NdjcCardBackButton(
            onClick = config.onHome,
            icon = Icons.Filled.Home,
            contentDescription = "Home",
            iconOnly = config.iconOnly,
            iconTint = config.iconTint
        )
    }
}
/**
 * ✅ 所有非 Home 页面：右上角提供“一键回到 Home”的入口按钮（复用 NdjcCardBackButton 外观）
 * - UI 包：只负责长相与布局
 * - 逻辑模块：通过 actions.onBackToHome 处理跳转
 */
@Composable
private fun BoxScope.NdjcHomeEntryOverlay(
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = NdjcCommonTokens.Dp.Dp16,
                vertical = NdjcCommonTokens.Dp.Dp8
            ),
        contentAlignment = Alignment.TopEnd
    ) {
        NdjcCardBackButton(
            onClick = onHome,
            icon = Icons.Filled.Home,
            contentDescription = "Home"
        )
    }
}

@Composable
internal fun NdjcWhiteCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = NdjcWhiteCardLayoutTokens.CardInnerPaddingHorizontal,
        vertical = NdjcWhiteCardLayoutTokens.CardInnerPaddingVertical
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NdjcWhiteCardLayoutTokens.CardRadius),
        shadowElevation = NdjcWhiteCardLayoutTokens.CardShadow,
        tonalElevation = NdjcCommonTokens.Dp.Dp0,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}
@Composable
internal fun NdjcSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            Snackbar(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = NdjcSnackbarTokens.ShadowElevation,
                        shape = RoundedCornerShape(NdjcSnackbarTokens.CornerRadius),
                        clip = false
                    ),
                shape = RoundedCornerShape(NdjcSnackbarTokens.CornerRadius),
                containerColor = NdjcSnackbarTokens.ContainerColor,
                contentColor = NdjcSnackbarTokens.ContentColor
            ) {
                Text(
                    text = snackbarData.visuals.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
fun NdjcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    minLines: Int = 1,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    fillContentWidth: Boolean = true
) {
    val baseModifier = if (fillContentWidth) {
        modifier.fillMaxWidth()
    } else {
        modifier
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        // ✅ 关键：统一最小高度走 Token（多行时仍可自然增高）
        modifier = baseModifier.heightIn(min = NdjcInputTokens.MinHeight),
        enabled = enabled,
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        label = label?.let { { Text(text = it) } },
        placeholder = placeholder?.let { { Text(text = it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = NdjcInputTokens.Shape,
        colors = NdjcInputTokens.colors(),
        textStyle = textStyle
    )
}
/**
 * ✅ 通用纯外观胶囊按钮（Sort/Filter/Price/Category 共用）
 * - 只负责外观（shape/颜色/padding/字体）
 * - 不包含任何业务逻辑
 */
@Composable
private fun NdjcPillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor =
        if (selected) NdjcPillButtonTokens.SelectedBg else NdjcPillButtonTokens.UnselectedBg

    val textColor =
        if (selected) NdjcPillButtonTokens.SelectedText else NdjcPillButtonTokens.UnselectedText

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "NdjcPillButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(NdjcPillButtonTokens.Shape)
            .border(
                width = if (selected) NdjcCommonTokens.Dp.Dp0 else NdjcPillButtonTokens.BorderWidth,
                color = NdjcPillButtonTokens.BorderColor,
                shape = NdjcPillButtonTokens.Shape
            )
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(
                horizontal = NdjcPillButtonTokens.PaddingH,
                vertical = NdjcPillButtonTokens.PaddingV
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
private fun NdjcInlineTextTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) {
        BackButtonTokens.Background
    } else {
        Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
    }
    val weight = if (selected) FontWeight.SemiBold else FontWeight.Medium

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp10)) // 只是点击热区圆角，不是“容器外观”
            .clickable { onClick() }
            .padding(vertical = NdjcCommonTokens.Dp.Dp6),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
// ✅ 通用纯外观：Label + Switch 行（不含任何业务逻辑）
@Composable
internal fun NdjcToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelColor: Color = NdjcToggleRowTokens.LabelColor
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = NdjcToggleRowTokens.CheckedTrackColor,
                checkedThumbColor = NdjcToggleRowTokens.CheckedThumbColor,
                uncheckedTrackColor = NdjcToggleRowTokens.UncheckedTrackColor,
                uncheckedThumbColor = NdjcToggleRowTokens.UncheckedThumbColor
            )
        )
    }
}
@Composable
private fun NdjcSelectionCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp8)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "NdjcSelectionCheckboxScale"
    )

    Box(
        modifier = modifier
            .size(NdjcCommonTokens.Dp.Dp24)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(NdjcCommonTokens.Dp.Dp24)
                .clip(shape),
            colors = CheckboxDefaults.colors(
                checkedColor = TopBannerTokens.Background,
                checkmarkColor = Color.White,
                uncheckedColor = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a35)
            )
        )
    }
}
@Composable
internal fun NdjcCatalogItemCard(
    title: String,
    imageUrl: String?,
    // ✅ 字符串价格（Favorites 等已有用法继续走这套）
    priceText: String?,
    originalPriceText: String?,
    discountPriceText: String?,

    // ✅ Double 价格（Admin Items 直接喂 Double；不再需要外部工具函数）
    price: Double? = null,
    originalPrice: Double? = null,
    discountPrice: Double? = null,

    categoryText: String?,
    metaText: String? = null,
    modifier: Modifier = Modifier,

    // ✅ 新增：统一把点击/按压态收进通用组件
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,

    // ✅ 新增：UI 覆盖（默认 null，不影响现有页面）
    containerColorOverride: Color? = null,
    shadowElevationOverride: Dp? = null,
    shapeOverride: androidx.compose.ui.graphics.Shape? = null,
    contentPaddingOverride: PaddingValues? = null,

    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    // ✅ 价格统一加美元符号；如果本身已有 $ 就不重复加
    fun withUsd(s: String): String {
        val t = s.trim()
        return if (t.startsWith("$")) t else "\$$t"
    }

    val resolvedPriceText = priceText ?: price?.let { ndjcMoneyTrim2(it) }
    val resolvedOriginalText = originalPriceText ?: originalPrice?.let { ndjcMoneyTrim2(it) }
    val resolvedDiscountText = discountPriceText ?: discountPrice?.let { ndjcMoneyTrim2(it) }

    val resolvedShape = shapeOverride ?: NdjcCatalogItemCardTokens.Shape

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val pressedScale by animateFloatAsState(
        targetValue = if (pressed && enabled && onClick != null) 0.965f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "NdjcCatalogItemCardScale"
    )

    val resolvedShadow = when {
        shadowElevationOverride != null -> shadowElevationOverride
        pressed && enabled && onClick != null -> NdjcCommonTokens.Dp.Dp2
        else -> NdjcCatalogItemCardTokens.ShadowElevation
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            },
        shape = resolvedShape,
        color = containerColorOverride ?: NdjcCatalogItemCardTokens.ContainerColor,
        shadowElevation = resolvedShadow,
        tonalElevation = NdjcCommonTokens.Dp.Dp0
    ) {
        val resolvedPadding = contentPaddingOverride ?: PaddingValues(
            start = NdjcCatalogItemCardTokens.PaddingStart,
            end = NdjcCatalogItemCardTokens.PaddingEnd,
            top = NdjcCatalogItemCardTokens.PaddingTop,
            bottom = NdjcCatalogItemCardTokens.PaddingBottom
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(resolvedShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            enabled = enabled,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(resolvedPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!imageUrl.isNullOrBlank()) {
                NdjcShimmerImage(
                    imageUrl = imageUrl,
                    modifier = Modifier
                        .size(NdjcCatalogItemCardTokens.ImageSize)
                        .clip(NdjcCatalogItemCardTokens.ImageShape),
                    placeholderCornerRadius = NdjcCommonTokens.Dp.Dp12,
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(NdjcCatalogItemCardTokens.ImageSize)
                        .clip(NdjcCatalogItemCardTokens.ImageShape)
                        .background(NdjcCatalogItemCardTokens.CategoryChipBg)
                )
            }

            Spacer(Modifier.width(NdjcCatalogItemCardTokens.Gap))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(NdjcCatalogItemCardTokens.ImageSize)
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .padding(top = NdjcCatalogItemTextTokens.TitleTopExtra)
                        .offset(
                            x = NdjcCatalogItemTextTokens.TitleOffsetX,
                            y = NdjcCatalogItemTextTokens.TitleOffsetY
                        ),
                    style = TextStyle(
                        fontSize = NdjcCatalogItemTextTokens.TitleFontSize,
                        lineHeight = NdjcCatalogItemTextTokens.TitleLineHeight,
                        fontWeight = NdjcCatalogItemTextTokens.TitleFontWeight,
                        platformStyle = NdjcCatalogItemTextTokens.NoFontPadding,
                        lineHeightStyle = NdjcCatalogItemTextTokens.TrimLineHeight
                    ),
                    color = NdjcCatalogItemTextTokens.TitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.padding(bottom = NdjcCatalogItemTextTokens.PriceBottomExtra)
                ) {
                    val original = resolvedOriginalText
                    val discount = resolvedDiscountText

                    when {
                        !discount.isNullOrBlank() && !original.isNullOrBlank() && discount != original -> {
                            Spacer(Modifier.height(NdjcCatalogItemCardTokens.PriceTopGap))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(NdjcCatalogItemCardTokens.PriceRowGap)
                                ) {
                                    Text(
                                        text = withUsd(discount),
                                        modifier = Modifier.offset(
                                            x = NdjcCatalogItemTextTokens.DiscountOffsetX,
                                            y = NdjcCatalogItemTextTokens.DiscountOffsetY
                                        ),
                                        style = TextStyle(
                                            fontSize = NdjcCatalogItemTextTokens.DiscountFontSize,
                                            lineHeight = NdjcCatalogItemTextTokens.DiscountLineHeight,
                                            fontWeight = NdjcCatalogItemTextTokens.DiscountFontWeight,
                                            platformStyle = NdjcCatalogItemTextTokens.NoFontPadding,
                                            lineHeightStyle = NdjcCatalogItemTextTokens.TrimLineHeight
                                        ),
                                        color = NdjcCatalogItemTextTokens.DiscountColor
                                    )

                                    Text(
                                        text = withUsd(original),
                                        modifier = Modifier.offset(
                                            x = NdjcCatalogItemTextTokens.OriginalOffsetX,
                                            y = NdjcCatalogItemTextTokens.OriginalOffsetY
                                        ),
                                        style = TextStyle(
                                            fontSize = NdjcCatalogItemTextTokens.OriginalFontSize,
                                            lineHeight = NdjcCatalogItemTextTokens.OriginalLineHeight,
                                            fontWeight = NdjcCatalogItemTextTokens.DiscountFontWeight,
                                            textDecoration = TextDecoration.LineThrough,
                                            platformStyle = NdjcCatalogItemTextTokens.NoFontPadding,
                                            lineHeightStyle = NdjcCatalogItemTextTokens.TrimLineHeight
                                        ),
                                        color = NdjcCatalogItemTextTokens.OriginalColor.copy(alpha = NdjcCatalogItemTextTokens.OriginalAlpha)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                if (!metaText.isNullOrBlank()) {
                                    Text(
                                        text = metaText,
                                        style = TextStyle(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        !original.isNullOrBlank() -> {
                            Spacer(Modifier.height(NdjcCatalogItemCardTokens.PriceTopGap))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = withUsd(original),
                                    modifier = Modifier.offset(
                                        x = NdjcCatalogItemTextTokens.DiscountOffsetX,
                                        y = NdjcCatalogItemTextTokens.DiscountOffsetY
                                    ),
                                    style = TextStyle(
                                        fontSize = NdjcCatalogItemTextTokens.DiscountFontSize,
                                        lineHeight = NdjcCatalogItemTextTokens.DiscountLineHeight,
                                        fontWeight = NdjcCatalogItemTextTokens.DiscountFontWeight,
                                        platformStyle = NdjcCatalogItemTextTokens.NoFontPadding,
                                        lineHeightStyle = NdjcCatalogItemTextTokens.TrimLineHeight
                                    ),
                                    color = NdjcCatalogItemTextTokens.DiscountColor
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                if (!metaText.isNullOrBlank()) {
                                    Text(
                                        text = metaText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        !resolvedPriceText.isNullOrBlank() -> {
                            Spacer(Modifier.height(NdjcCatalogItemCardTokens.PriceTopGap))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = withUsd(resolvedPriceText),
                                    modifier = Modifier.offset(
                                        x = NdjcCatalogItemTextTokens.DiscountOffsetX,
                                        y = NdjcCatalogItemTextTokens.DiscountOffsetY
                                    ),
                                    style = TextStyle(
                                        fontSize = NdjcCatalogItemTextTokens.DiscountFontSize,
                                        lineHeight = NdjcCatalogItemTextTokens.DiscountLineHeight,
                                        fontWeight = NdjcCatalogItemTextTokens.DiscountFontWeight,
                                        platformStyle = NdjcCatalogItemTextTokens.NoFontPadding,
                                        lineHeightStyle = NdjcCatalogItemTextTokens.TrimLineHeight
                                    ),
                                    color = NdjcCatalogItemTextTokens.DiscountColor
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                if (!metaText.isNullOrBlank()) {
                                    Text(
                                        text = metaText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (trailingContent != null) {
                Spacer(Modifier.width(NdjcCatalogItemCardTokens.Gap))

                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        trailingContent()
                    }
                }
            }
        }
    }
}
@Composable
private fun NdjcChatToolButton(
    iconOutlined: ImageVector,
    iconFilled: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val icon = if (pressed) iconFilled else iconOutlined
    val tint = if (pressed) BackButtonTokens.Background else Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)

    Box(
        modifier = modifier
            .size(ChatTokens.ToolBtnSize)
            .clip(RoundedCornerShape(ChatTokens.ToolBtnCorner))
            // ✅ 去掉白色容器：不再 background
            // ✅ 按压态切换：用 interactionSource + indication=null（避免灰色胶囊）
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(ChatTokens.ToolIconSize)
        )
    }
}
internal enum class NdjcPillTone {
    Normal,
    Accent,
    Subtle
}
@Composable
internal fun NdjcControlPillButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: NdjcPillTone = NdjcPillTone.Normal
) {
    val shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999)

    val (bg, border, textColor, borderWidth) = when (tone) {
        NdjcPillTone.Normal -> Quad(
            if (active) HomeControlsTokens.PillBgActive else HomeControlsTokens.PillBg,
            if (active) HomeControlsTokens.PillBorderActive else HomeControlsTokens.PillBorder,
            if (active) HomeControlsTokens.PillTextActive else HomeControlsTokens.PillText,
            if (active) NdjcCommonTokens.Dp.Dp0 else HomeControlsTokens.PillBorderWidth
        )

        NdjcPillTone.Accent -> Quad(
            if (active) HomeControlsTokens.PillBgActive else Color.Transparent,
            if (active) HomeControlsTokens.PillBorderActive else HomeTokens.BrandPurpleAlt.copy(alpha = 0.32f),
            if (active) HomeControlsTokens.PillTextActive else HomeTokens.BrandPurpleAlt,
            if (active) NdjcCommonTokens.Dp.Dp0 else HomeControlsTokens.PillBorderWidth
        )

        NdjcPillTone.Subtle -> Quad(
            if (active) HomeControlsTokens.PillBgActive else Color.Transparent,
            if (active) HomeControlsTokens.PillBorderActive else Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a14),
            if (active) HomeControlsTokens.PillTextActive else Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55),
            if (active) NdjcCommonTokens.Dp.Dp0 else HomeControlsTokens.PillBorderWidth
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = borderWidth,
                color = border,
                shape = shape
            )
            .background(bg)
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
private data class Quad(
    val bg: Color,
    val border: Color,
    val text: Color,
    val borderWidth: Dp
)
// ---------- 首页布局（供逻辑模块调用） ----------
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
//endregion
private fun ndjcMoneyTrim2(v: Double): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val df = DecimalFormat("0.##", symbols)
    df.roundingMode = RoundingMode.HALF_UP
    df.minimumFractionDigits = 0
    df.maximumFractionDigits = 2
    df.isGroupingUsed = false
    return df.format(v)
}
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ShowcaseHome(
    uiState: ShowcaseHomeUiState,
    actions: ShowcaseHomeActions,
    modifier: Modifier = Modifier
) {


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
    NdjcUnifiedBackground(
        modifier = modifier.pullRefresh(pullRefreshState)
    ) {

        // 背景圆（如果你把 HeroCircleSize 设成 NdjcCommonTokens.Dp.Dp0 就等于关闭）
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
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0), // ✅ 关键：关闭默认 safeDrawing 下沉
            topBar = {
                TopSearchBar(
                    searchQuery = uiState.searchQuery,
                    onSearchChange = { actions.onSearchQueryChange(it) },
                    onProfileClick = actions.onProfileClick
                )
            },
            snackbarHost = {
                NdjcSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = NdjcSnackbarTokens.HorizontalPadding,
                            end = NdjcSnackbarTokens.HorizontalPadding,
                            bottom = NdjcSnackbarTokens.BottomPadding
                        )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ✅ 统一间距：搜索栏 ↔ 排序筛选栏
                Spacer(modifier = Modifier.height(HomeTokens.ControlsGap))

// ✅ Tags filter row（多选）
                if (uiState.allTags.isNotEmpty()) {
                    TagsFilterRow(
                        allTags = uiState.allTags,
                        selectedTags = uiState.selectedTags,
                        onClearTags = actions.onClearTags,
                        onToggleTag = actions.onToggleTag,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HomeTokens.ChipRowHorizontalPadding)
                            // ✅ 统一间距：Tags ↔ 排序筛选栏
                            .padding(bottom = HomeTokens.ControlsGap)
                    )
                }

// ✅ 方案二：Sort / Filter / 价格 右对齐（放在分类上方）
                HomeSortNavEqualRow(
                    sortMode = uiState.sortMode,
                    onSortModeChange = actions.onSortModeChange,
                    filterRecommendedOnly = uiState.filterRecommendedOnly,
                    filterOnSaleOnly = uiState.filterOnSaleOnly,
                    appliedMinPrice = uiState.appliedMinPrice,
                    appliedMaxPrice = uiState.appliedMaxPrice,
                    showFilterMenu = uiState.showFilterMenu,
                    onFilterClick = { actions.onShowFilterMenuChange(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HomeTokens.ChipRowHorizontalPadding)
                        // ✅ 统一间距：排序筛选栏 ↔ 分类栏
                        .padding(bottom = HomeTokens.ControlsGap)
                )

                CategoryChipsRow(
                    selectedCategory = uiState.selectedCategory,
                    manualCategories = uiState.manualCategories,
                    onCategorySelected = actions.onCategorySelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        // ✅ 分类栏 ↔ 商品列表：增加呼吸感
                        .padding(bottom = HomeTokens.ChipsToListGap)
                )
                if (uiState.showFilterMenu) {
                    NdjcFilterBottomSheet(
                        onDismissRequest = {
                            actions.onShowFilterMenuChange(false)
                        },
                        onClear = {
                            actions.onClearSortAndFilters()
                            actions.onClearPriceRange()
                            actions.onShowFilterMenuChange(false)
                        },
                        priceMinDraft = uiState.priceMinDraft,
                        onPriceMinDraftChange = actions.onPriceMinDraftChange,
                        priceMaxDraft = uiState.priceMaxDraft,
                        onPriceMaxDraftChange = actions.onPriceMaxDraftChange,
                        onApply = {
                            actions.onApplyPriceRange()
                            actions.onShowFilterMenuChange(false)
                        }
                    ) {
                        NdjcToggleRow(
                            label = "Pick",
                            checked = uiState.filterRecommendedOnly,
                            onCheckedChange = actions.onFilterRecommendedOnlyChange,
                            modifier = Modifier.padding(vertical = NdjcCommonTokens.Dp.Dp6)
                        )
                        NdjcToggleRow(
                            label = "On sale",
                            checked = uiState.filterOnSaleOnly,
                            onCheckedChange = actions.onFilterOnSaleOnlyChange,
                            modifier = Modifier.padding(vertical = NdjcCommonTokens.Dp.Dp6)
                        )
                    }
                }
// ✅ 关键：为“悬浮胶囊底栏”预留底部空间，避免最后一行卡片被遮挡
                val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
// 胶囊自身高度(约44dp) + 外边距(12~16dp) + 视觉安全余量(约16~20dp)
                val floatingBottomBarReserve = 84.dp + navBottom

// 一排两个卡片：纯 UI 排版，业务仍然是「1 个菜品 = 1 张卡片」
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(HomeTokens.ListItemSpacing),
                    contentPadding = PaddingValues(
                        start = HomeTokens.ScreenHorizontalPadding,
                        end = HomeTokens.ScreenHorizontalPadding,
                        top = NdjcCommonTokens.Dp.Dp0,
                        bottom = HomeTokens.ScreenVerticalPadding + floatingBottomBarReserve
                    )
                ) {
                    val rows = uiState.dishes
                        .chunked(2)

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
                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp16))
                    }

                    // ✅ 双保险：底部再留一段可滚动空白，确保不会被悬浮胶囊底栏遮挡
                    item {
                        Spacer(modifier = Modifier.height(floatingBottomBarReserve))
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = uiState.isLoading,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )

    }
}

// ---------- 登录页布局（Admin 登录） ----------

@Composable
//endregion

//region 5 Login/Auth
internal fun ShowcaseLogin(
    uiState: ShowcaseLoginUiState,
    actions: ShowcaseLoginActions,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        actions.onBackToHome()
    }

    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBackToHome() },
            onHome = { actions.onBackToHome() },
        )
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
                // ✅ Back/Home 已由 NdjcUnifiedBackground(topNav=...) 统一渲染；这里不再重复绘制
// 中间的登录卡片（通用白卡容器）
        NdjcWhiteCard(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(
                    top = NdjcTopContentPadding,
                    start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    bottom = NdjcWhiteCardLayoutTokens.ScreenPadding
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Title
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                // Subtitle（更中性：不写死 admin，不只 items/categories）
                Text(
                    text = "Use your account to manage content and settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                )

                Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp16))

                // Fields
                NdjcTextField(
                    value = uiState.usernameDraft,
                    onValueChange = actions.onUsernameDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = "Email",
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp12))

                NdjcTextField(
                    value = uiState.passwordDraft,
                    onValueChange = actions.onPasswordDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = "Password",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                // Status（错误/提示区：给“系统感”）
                if (!uiState.loginError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp10))
                    Text(
                        text = uiState.loginError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
                    )
                }

                Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp16))

                // Remember device（文案更成熟）
                NdjcToggleRow(
                    label = "Remember this device",
                    checked = uiState.rememberMe,
                    onCheckedChange = { actions.onRememberMeChange(it) },
                    labelColor = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp16))

                // CTA（文案更准确）
                NdjcPrimaryActionButton(
                    text = "Sign in",
                    onClick = { actions.onLogin(uiState.usernameDraft, uiState.passwordDraft) },
                    enabled = uiState.canLogin,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp12))

                // Security note（中性安全提示，不引入逻辑）
                Text(
                    text = "Access is limited to administrators.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                )
            }
        }
    }
}
@Composable
internal fun NdjcSyncErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 纯 UI：不做任何业务，业务通过回调交给逻辑模块
    Surface(
        tonalElevation = NdjcCommonTokens.Dp.Dp6,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NdjcCommonTokens.Dp.Dp12, vertical = NdjcCommonTokens.Dp.Dp10),
        shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp16)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NdjcCommonTokens.Dp.Dp12, vertical = NdjcCommonTokens.Dp.Dp10),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = onRetry) { Text("Retry") }
            TextButton(onClick = onDismiss) { Text("Close") }
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
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                start = HomeTokens.TopBarHorizontalPadding,
                end = HomeTokens.TopBarHorizontalPadding,
                top = HomeTokens.TopBarVerticalPadding,
                bottom = NdjcCommonTokens.Dp.Dp0
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopBannerTokens.Height)
                .clip(RoundedCornerShape(TopBannerTokens.CornerRadius))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(TopBannerTokens.GradientTop, TopBannerTokens.GradientBottom)
                    )
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(
                        horizontal = NdjcCommonTokens.Dp.Dp12,
                        vertical = NdjcCommonTokens.Dp.Dp6
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

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
                        tint = HomeTokens.SearchIconColor
                    )

                    Spacer(modifier = Modifier.width(HomeTokens.SearchTextStartSpacing))

                    val base = MaterialTheme.typography.titleMedium

                    val searchTextStyle = base.copy(
                        color = Color.White,
                        lineHeight = base.fontSize,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = searchTextStyle,
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isBlank()) {
                                    Text(
                                        text = "Search…",
                                        color = Color.White.copy(alpha = 0.66f),
                                        style = searchTextStyle
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(NdjcCommonTokens.Dp.Dp8))

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    tonalElevation = NdjcCommonTokens.Dp.Dp0,
                    shadowElevation = NdjcCommonTokens.Dp.Dp0,
                    modifier = Modifier.size(HomeTokens.ProfileButtonSize)
                ) {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(HomeTokens.ProfileIconSize),
                            tint = HomeTokens.ProfileIconColor
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipsRow(
    selectedCategory: String?,
    manualCategories: List<String>,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    showAllChip: Boolean = true,
    trailingFixedContent: (@Composable () -> Unit)? = null,
    useOuterHorizontalPadding: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val maxVisibleCategories = 6
    val shouldShowMore = manualCategories.size > maxVisibleCategories
    val visibleCategories = if (shouldShowMore) manualCategories.take(maxVisibleCategories) else manualCategories

    val rowState = rememberLazyListState()

    Column(modifier = modifier) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (useOuterHorizontalPadding) HomeTokens.ChipRowHorizontalPadding else NdjcCommonTokens.Dp.Dp0
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                state = rowState,
                horizontalArrangement = Arrangement.spacedBy(HomeTokens.ChipSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showAllChip) {
                    item(key = "cat_all") {
                        NdjcPillButton(
                            text = "All",
                            selected = selectedCategory == null,
                            onClick = {
                                onCategorySelected(null)
                                expanded = false
                            }
                        )
                    }
                }

                items(
                    items = visibleCategories,
                    key = { it }
                ) { cat ->
                    NdjcPillButton(
                        text = cat,
                        selected = selectedCategory == cat,
                        onClick = {
                            onCategorySelected(cat)
                            expanded = false
                        }
                    )
                }

                if (shouldShowMore) {
                    item(key = "cat_more_arrow") {
                        val bg = HomeTokens.ChipUnselectedBackground
                        val border = HomeTokens.ChipBorderWidth
                        val shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp24)

                        val expandInteractionSource = remember { MutableInteractionSource() }
                        val expandPressed by expandInteractionSource.collectIsPressedAsState()
                        val expandScale by animateFloatAsState(
                            targetValue = if (expandPressed) 0.965f else 1f,
                            animationSpec = tween(durationMillis = 120),
                            label = "CategoryExpandScale"
                        )

                        Surface(
                            color = if (expandPressed) {
                                bg.copy(alpha = NdjcCommonTokens.Alpha.a90)
                            } else {
                                bg
                            },
                            shape = shape,
                            border = BorderStroke(border, Color.Black.copy(alpha = 0.10f)),
                            tonalElevation = NdjcCommonTokens.Dp.Dp0,
                            shadowElevation = NdjcCommonTokens.Dp.Dp0,
                            modifier = Modifier
                                .height(NdjcCommonTokens.Dp.Dp32)
                                .graphicsLayer {
                                    scaleX = expandScale
                                    scaleY = expandScale
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(shape)
                                    .clickable(
                                        interactionSource = expandInteractionSource,
                                        indication = ripple(bounded = true)
                                    ) {
                                        expanded = !expanded
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (expanded) "Collapse categories" else "Expand categories"
                                )
                            }
                        }
                    }
                }
            }

            if (trailingFixedContent != null) {
                Spacer(modifier = Modifier.width(NdjcCommonTokens.Dp.Dp8))
                trailingFixedContent()
            }
        }

        if (expanded && shouldShowMore) {
            Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp8))

            val remainingCategories = manualCategories.drop(maxVisibleCategories)

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HomeTokens.ChipRowHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(HomeTokens.ChipSpacing),
                verticalArrangement = Arrangement.spacedBy(HomeTokens.ChipSpacing)
            ) {
                remainingCategories.forEach { cat ->
                    NdjcPillButton(
                        text = cat,
                        selected = selectedCategory == cat,
                        onClick = {
                            onCategorySelected(cat)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun HomeSortNavEqualRow(
    sortMode: ShowcaseHomeSortMode,
    onSortModeChange: (ShowcaseHomeSortMode) -> Unit,
    filterRecommendedOnly: Boolean,
    filterOnSaleOnly: Boolean,
    appliedMinPrice: Int?,
    appliedMaxPrice: Int?,
    showFilterMenu: Boolean,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ 选中态颜色：与底栏选中态完全一致
    val selectedColor = BackButtonTokens.Background

    val hasToggleFilter = filterRecommendedOnly || filterOnSaleOnly
    val hasPriceFilter = (appliedMinPrice != null || appliedMaxPrice != null)
    val filterActive = showFilterMenu || hasToggleFilter || hasPriceFilter

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortNavEqualItem(
            text = "Default",
            selected = sortMode == ShowcaseHomeSortMode.Default,
            selectedColor = selectedColor,
            onClick = { onSortModeChange(ShowcaseHomeSortMode.Default) },
            modifier = Modifier.weight(1f)
        )

        SortNavEqualItem(
            text = "Low–High",
            selected = sortMode == ShowcaseHomeSortMode.PriceAsc,
            selectedColor = selectedColor,
            onClick = { onSortModeChange(ShowcaseHomeSortMode.PriceAsc) },
            modifier = Modifier.weight(1f)
        )

        SortNavEqualItem(
            text = "High–Low",
            selected = sortMode == ShowcaseHomeSortMode.PriceDesc,
            selectedColor = selectedColor,
            onClick = { onSortModeChange(ShowcaseHomeSortMode.PriceDesc) },
            modifier = Modifier.weight(1f)
        )

        SortNavEqualItem(
            text = "Filter",
            selected = filterActive,
            selectedColor = selectedColor,
            onClick = onFilterClick,
            modifier = Modifier.weight(1f)
        )
    }
}
@Composable
private fun SortNavEqualItem(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(horizontal = NdjcCommonTokens.Dp.Dp4)
            .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp10))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .fillMaxWidth()
            .padding(vertical = NdjcCommonTokens.Dp.Dp10),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) selectedColor else Color.Black.copy(alpha = 0.60f)
        )
    }
}
@Composable
private fun SheetHeader(
    title: String,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NdjcCommonTokens.Dp.Dp16)
                .padding(top = NdjcCommonTokens.Dp.Dp6, bottom = NdjcCommonTokens.Dp.Dp10),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            NdjcControlPillButton(
                text = "Clear",
                active = false,
                tone = NdjcPillTone.Subtle,
                onClick = onClear
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = NdjcCommonTokens.Dp.Dp16),
            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10)
        )
    }
}
@Composable
private fun SortRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

private fun ShowcaseHomeSortMode.label(): String {
    return when (this) {
        ShowcaseHomeSortMode.Default -> "Default"
        ShowcaseHomeSortMode.PriceAsc -> "Low→High"
        ShowcaseHomeSortMode.PriceDesc -> "High→Low"
    }
}
@Composable
private fun ActiveSortFilterRow(
    sortMode: ShowcaseHomeSortMode,
    filterRecommendedOnly: Boolean,
    filterOnSaleOnly: Boolean,
    onClearRecommended: () -> Unit,
    onClearOnSale: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ 排序不显示 Clear，也不在这里展示排序 chip
    val shouldShowRow = filterRecommendedOnly || filterOnSaleOnly
    if (!shouldShowRow) return

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(HomeTokens.ChipSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (filterRecommendedOnly) SmallActiveChip("Recommended", onClearRecommended)
        if (filterOnSaleOnly) SmallActiveChip("On sale", onClearOnSale)

        TextButton(onClick = onClearAll) {
            Text(
                text = "Clear",
                color = HomeTokens.ChipSelectedBackground,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
private fun SmallActiveChip(
    text: String,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999)
    Surface(shape = shape, color = HomeTokens.ChipUnselectedBackground) {
        Row(
            modifier = Modifier
                .clip(shape)
                .clickable { onRemove() }
                .padding(horizontal = NdjcCommonTokens.Dp.Dp12, vertical = NdjcCommonTokens.Dp.Dp8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
        ) {
            Text(text = text, color = Color.Black, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text("×", color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a80), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun NdjcHomeStyleMediaCard(
    title: String,
    imageUrl: String?,
    primaryText: String,
    secondaryText: String? = null,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitleInBottom: Boolean = true,
    primaryTextStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = DishCardTokens.PriceFontSize
    ),
    trailingOverlay: (@Composable BoxScope.() -> Unit)? = null,
    bottomTrailingContent: (@Composable RowScope.() -> Unit)? = null,
    bottomContentOverride: (@Composable ColumnScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(DishCardTokens.CornerRadius)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val pressedScale by animateFloatAsState(
        targetValue = if (pressed) DishCardTokens.PressedScale else 1f,
        label = "homeStyleCardScale"
    )

    BoxWithConstraints(modifier = modifier) {
        val imageSide = this.maxWidth
        val required = imageSide + DishCardTokens.BottomMinHeight
        val targetHeight = if (required > DishCardTokens.Height) required else DishCardTokens.Height

        Surface(
            modifier = Modifier
                .height(targetHeight)
                .graphicsLayer {
                    scaleX = pressedScale
                    scaleY = pressedScale
                },
            shape = shape,
            color = DishCardTokens.CardBackground,
            shadowElevation = if (pressed) NdjcCommonTokens.Dp.Dp2 else DishCardTokens.ShadowElevation,
            tonalElevation = NdjcCommonTokens.Dp.Dp0
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true)
                    ) {
                        onClick()
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imageSide)
                            .background(DishCardTokens.ImageAreaBackground)
                    ) {
                        if (!imageUrl.isNullOrBlank()) {
                            NdjcShimmerImage(
                                imageUrl = imageUrl,
                                modifier = Modifier.fillMaxSize(),
                                placeholderCornerRadius = DishCardTokens.InnerCornerRadius,
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (trailingOverlay != null) {
                            trailingOverlay()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(DishCardTokens.BottomBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = DishCardTokens.ContentPaddingStart,
                                    end = DishCardTokens.ContentPaddingEnd,
                                    top = DishCardTokens.ContentPaddingTop,
                                    bottom = DishCardTokens.ContentPaddingBottom
                                ),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (bottomContentOverride != null) {
                                bottomContentOverride()
                            } else {
                                if (showTitleInBottom) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DishCardTokens.TitleTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp0))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(DishCardTokens.PriceRowSpacing)
                                    ) {
                                        Text(
                                            text = primaryText,
                                            modifier = Modifier.alignByBaseline(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = primaryTextStyle,
                                            color = DishCardTokens.PriceTextColor
                                        )

                                        if (!secondaryText.isNullOrBlank()) {
                                            Text(
                                                text = secondaryText,
                                                modifier = Modifier.alignByBaseline(),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = DishCardTokens.OriginalPriceFontSize,
                                                    textDecoration = TextDecoration.LineThrough
                                                ),
                                                color = DishCardTokens.OriginalPriceTextColor.copy(alpha = DishCardTokens.OriginalPriceAlpha)
                                            )
                                        }
                                    }

                                    if (!badgeText.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.width(NdjcCommonTokens.Dp.Dp8))
                                        Surface(
                                            shape = RoundedCornerShape(DishCardTokens.BadgeRadius),
                                            color = DishCardTokens.BadgeBg,
                                            border = BorderStroke(
                                                DishCardTokens.BadgeBorderWidth,
                                                DishCardTokens.BadgeBorderColor
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = DishCardTokens.BadgePaddingH,
                                                    vertical = DishCardTokens.BadgePaddingV
                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = DishCardTokens.BadgeTextColor,
                                                    modifier = Modifier.size(NdjcCommonTokens.Dp.Dp14)
                                                )
                                                Text(
                                                    text = badgeText,
                                                    color = DishCardTokens.BadgeTextColor,
                                                    fontSize = DishCardTokens.BadgeFontSize,
                                                    lineHeight = DishCardTokens.BadgeFontSize
                                                )
                                            }
                                        }
                                    }

                                    if (bottomTrailingContent != null) {
                                        Spacer(modifier = Modifier.width(NdjcCommonTokens.Dp.Dp8))
                                        bottomTrailingContent()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DishListCard(
    dish: ShowcaseHomeDish,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val discountPrice = dish.discountPrice
    val hasDiscount =
        (discountPrice != null && discountPrice > 0.0 && discountPrice < dish.originalPrice)

    NdjcHomeStyleMediaCard(
        title = dish.title,
        imageUrl = dish.imagePreviewUrl,
        primaryText = if (hasDiscount) {
            "$" + ndjcMoneyTrim2(discountPrice!!)
        } else {
            "$" + ndjcMoneyTrim2(dish.originalPrice)
        },
        secondaryText = if (hasDiscount) {
            "$" + ndjcMoneyTrim2(dish.originalPrice)
        } else {
            null
        },
        badgeText = if (dish.isRecommended) DishCardTokens.BadgeText else null,
        onClick = onClick,
        modifier = modifier,
        trailingOverlay = {
            if (dish.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = NdjcCommonTokens.Dp.Dp6, end = NdjcCommonTokens.Dp.Dp6)
                        .size(DishCardTokens.FavContainerSize),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = DishCardTokens.FavIconTint,
                        modifier = Modifier.size(DishCardTokens.FavIconSize)
                    )
                }
            }
        }
    )
}
@Composable
private fun AnnouncementDraftCard(
    card: ShowcaseAnnouncementCard,
    selected: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    NdjcHomeStyleMediaCard(
        title = "",
        imageUrl = card.coverUrl,
        primaryText = "",
        secondaryText = null,
        badgeText = null,
        onClick = onClick,
        modifier = modifier,
        showTitleInBottom = false,
        primaryTextStyle = MaterialTheme.typography.labelSmall.copy(
            fontSize = NdjcCommonTokens.Sp.Sp14
        ),
        trailingOverlay = null,
        bottomContentOverride = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NdjcCommonTokens.Dp.Dp6)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.timeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a75),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.4f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .height(NdjcCommonTokens.Dp.Dp24)
                                .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp8))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    onEditClick()
                                }
                                .padding(horizontal = NdjcCommonTokens.Dp.Dp2),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = NdjcCommonTokens.Sp.Sp14,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = BackButtonTokens.Background
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        NdjcSelectionCheckbox(
                            checked = selected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier.size(NdjcCommonTokens.Dp.Dp24)
                        )
                    }
                }
            }
        }
    )
}
// ---------- Edit Dish 页面 ----------

@OptIn(
    ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
//endregion

//region 6 Edit/CRUD
internal fun ShowcaseEditDish(
    uiState: ShowcaseEditDishUiState,
    actions: ShowcaseEditDishActions,
    modifier: Modifier = Modifier
) {
    var pendingExitTarget by rememberSaveable { mutableStateOf<String?>(null) }

    fun requestExit(target: String) {
        if (uiState.hasUnsavedChanges) {
            pendingExitTarget = target
        } else {
            if (target == "home") {
                actions.onBackToHome()
            } else {
                actions.onBack()
            }
        }
    }

    BackHandler(
        enabled = pendingExitTarget == null && !uiState.isBlocking
    ) {
        requestExit("back")
    }

    if (pendingExitTarget != null) {
        NdjcBaseDialog(
            onDismissRequest = { pendingExitTarget = null },
            title = "Leave this page?",
            message = "Your item changes will be saved as a draft.",
            confirmText = "Leave",
            dismissText = "Stay",
            onConfirmClick = {
                val target = pendingExitTarget
                pendingExitTarget = null
                if (target == "home") {
                    actions.onBackToHome()
                } else {
                    actions.onBack()
                }
            },
            onDismissClick = { pendingExitTarget = null },
            destructiveConfirm = false
        )
    }

    // 背景与首页保持一致
    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { requestExit("back") },
            onHome = { requestExit("home") },
        )
    ) {

        // ✅ 对齐 Chat：图片全屏 Pager 预览状态（纯 UI，不走导航，不越界）
        data class NdjcImagePreviewState(
            val images: List<String>,
            val startIndex: Int
        )

        var imagePreview by remember { mutableStateOf<NdjcImagePreviewState?>(null) }

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.statusMessage) {
            val message = uiState.statusMessage
            if (!message.isNullOrBlank()) {
                snackbarHostState.showSnackbar(message)
            }
        }

        val editScrollState = rememberScrollState()
        val uiScope = rememberCoroutineScope()
        val fieldScrollTopOffsetPx = with(LocalDensity.current) { 260.dp.roundToPx() }

        var formTopInRootPx by remember { mutableStateOf(0) }
        var nameTopInRootPx by remember { mutableStateOf(0) }
        var priceTopInRootPx by remember { mutableStateOf(0) }
        var descriptionTopInRootPx by remember { mutableStateOf(0) }
        var categoryTopInRootPx by remember { mutableStateOf(0) }
        var imagesTopInRootPx by remember { mutableStateOf(0) }

        val requiredItemErrorMessages = remember {
            setOf(
                "Please enter Name.",
                "Please enter Price.",
                "Please enter Description.",
                "Please enter Category.",
                "Please add at least 1 image."
            )
        }

        val showRequiredFieldErrors =
            !uiState.errorMessage.isNullOrBlank() &&
                    requiredItemErrorMessages.contains(uiState.errorMessage)

        val nameRequiredError =
            showRequiredFieldErrors && uiState.nameZh.trim().isBlank()

        val priceRequiredError =
            showRequiredFieldErrors && uiState.originalPrice.trim().isBlank()

        val descriptionRequiredError =
            showRequiredFieldErrors && uiState.descriptionEn.trim().isBlank()

        val categoryRequiredError =
            showRequiredFieldErrors && uiState.category.orEmpty().trim().isBlank()

        val imagesRequiredError =
            showRequiredFieldErrors && uiState.imageUrls.isEmpty()

        fun scrollToField(targetTopInRootPx: Int) {
            val target = (
                    editScrollState.value +
                            targetTopInRootPx -
                            formTopInRootPx -
                            fieldScrollTopOffsetPx
                    ).coerceAtLeast(0)

            uiScope.launch {
                editScrollState.animateScrollTo(target)
            }
        }

        LaunchedEffect(
            nameRequiredError,
            priceRequiredError,
            descriptionRequiredError,
            categoryRequiredError,
            imagesRequiredError
        ) {
            when {
                nameRequiredError -> scrollToField(nameTopInRootPx)
                priceRequiredError -> scrollToField(priceTopInRootPx)
                descriptionRequiredError -> scrollToField(descriptionTopInRootPx)
                categoryRequiredError -> scrollToField(categoryTopInRootPx)
                imagesRequiredError -> scrollToField(imagesTopInRootPx)
            }
        }

// ✅ 拖拽图片时禁用父滚动，避免 detectDragGesturesAfterLongPress 被 cancel
        var isDraggingImages by remember { mutableStateOf(false) }


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
                // ✅ Back/Home 已由 NdjcUnifiedBackground(topNav=...) 统一渲染；这里不再重复绘制
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    formTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                }
                .verticalScroll(editScrollState, enabled = !isDraggingImages)
                .navigationBarsPadding()
                .imePadding()
        ) {
            NdjcWhiteCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = NdjcTopContentPadding,
                        start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        bottom = NdjcWhiteCardLayoutTokens.ScreenPadding
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ====== Store Profile 同款排版节奏（不 tokens）======
                    val sectionTop = 18.dp          // Section -> Section
                    val titleToHint = 6.dp          // Section Title -> Description
                    val hintToContent = 10.dp       // Description -> Content
                    val fieldGap = 10.dp            // Field -> Field
                    val sectionBottom = 8.dp        // 小收口（少量地方还需要）

                    val sectionLabelColor = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                    val subLabelColor = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                    val chipGap = 10.dp
                    val mediaGridTop = 10.dp

// 标题 + 副标题（中性、与模式一致）
                    Column(
                        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                    ) {
                        Text(
                            text = if (uiState.isNew) "Create item" else "Edit item",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.isNew) {
                                "Set details, category, and visibility."
                            } else {
                                "Update details, category, and visibility."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                        )
                    }

                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))
                    Spacer(Modifier.height(sectionBottom))

// ====== Details ======
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    Text(
                        text = "Fields marked * are required. Sale price is optional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                    )

                    Spacer(Modifier.height(hintToContent))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(fieldGap)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    nameTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                                },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NdjcTextField(
                                value = uiState.nameZh,
                                onValueChange = actions.onNameChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = "Name *",
                                singleLine = true,
                                isError = nameRequiredError
                            )

                            if (nameRequiredError) {
                                Text(
                                    text = "Name is required.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    priceTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                                },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NdjcTextField(
                                value = uiState.originalPrice,
                                onValueChange = actions.onPriceChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = "Price *",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                isError = priceRequiredError
                            )

                            if (priceRequiredError) {
                                Text(
                                    text = "Price is required.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
                                )
                            }
                        }

                        NdjcTextField(
                            value = uiState.discountPrice,
                            onValueChange = actions.onDiscountPriceChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = "Sale price",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )

                        Text(
                            text = "Leave empty if no discount. If set, it should be lower than Price.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        // ---------- Validation (derived by logic module) ----------
                        uiState.discountErrorText?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
                            )
                        }

                        val descMax = 200
                        val descText = uiState.descriptionEn
                        val descLen = descText.length.coerceAtMost(descMax)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    descriptionTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                                },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NdjcTextField(
                                value = descText,
                                onValueChange = { v ->
                                    actions.onDescriptionChange(v.take(descMax))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = NdjcCommonTokens.Dp.Dp80),
                                label = "Description *",
                                singleLine = false,
                                minLines = 3,
                                isError = descriptionRequiredError
                            )

                            if (descriptionRequiredError) {
                                Text(
                                    text = "Description is required.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Appears on the item detail page.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "$descLen/$descMax",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                            )
                        }
                    }

                    Spacer(Modifier.height(sectionTop))

// ====== Organization ======
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            text = "Organization",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(titleToHint))

                        Text(
                            text = "Required. You can select an existing category or type to create a new one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        Spacer(Modifier.height(hintToContent))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    categoryTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                                },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NdjcTextField(
                                value = uiState.category.orEmpty(),
                                onValueChange = { v ->
                                    actions.onCategorySelected(v)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = "Category *",
                                isError = categoryRequiredError
                            )

                            if (categoryRequiredError) {
                                Text(
                                    text = "Category is required.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
                                )
                            }
                        }

// ✅ 已有分类快捷选择（可点选回填）
                        val options = uiState.availableCategories

                        if (options.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Or select an existing category:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                            )

                            Spacer(Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(chipGap),
                                verticalArrangement = Arrangement.spacedBy(chipGap)
                            ) {
                                options.forEach { cat ->
                                    NdjcPillButton(
                                        text = cat,
                                        selected = uiState.category == cat,
                                        onClick = { actions.onCategorySelected(cat) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(sectionTop))

// ====== Media ======
                    Text(
                        text = "Media",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(titleToHint))

                    Text(
                        text = "Images displayed on the item detail page.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                    )

                    Spacer(Modifier.height(hintToContent))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                imagesTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                            },
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            text = "Images *",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (imagesRequiredError) {
                                MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
                            } else {
                                Color.Black
                            }
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Add images displayed on the item detail page. At least 1 image is required. The first image is used as the cover. Up to 9 images.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        Spacer(Modifier.height(mediaGridTop))

                        val previewUrls = uiState.imageUrls

                        NdjcEditableImageGrid(
                            images = previewUrls,
                            enabled = true,
                            onAddClick = { actions.onPickImage() },
                            onRemoveClick = { url -> actions.onRemoveImage(url) },
                            onPreview = { images, startIndex ->
                                imagePreview = NdjcImagePreviewState(
                                    images = images,
                                    startIndex = startIndex
                                )
                            },
                            maxCount = uiState.maxImages,
                            columns = 3,
                            cellGap = NdjcCommonTokens.Dp.Dp8,
                            cornerRadius = NdjcCommonTokens.Dp.Dp16,
                            onMove = { from, to ->
                                actions.onMoveImage(from, to)
                            }
                        )

                        if (imagesRequiredError) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "At least 1 image is required.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
                            )
                        }
                    }

                    Spacer(Modifier.height(sectionTop))

// ====== Visibility ======
                    Text(
                        text = "Visibility",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))

                Text(
                    text = "Choose how this item appears in the list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                )

                Spacer(Modifier.height(hintToContent))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        NdjcToggleRow(
                            label = "Pick",
                            checked = uiState.isRecommended,
                            onCheckedChange = actions.onToggleRecommended
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Marks this item as Pick in the list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        Spacer(Modifier.height(12.dp))

                        NdjcToggleRow(
                            label = "Hidden from list",
                            checked = uiState.isHidden,
                            onCheckedChange = actions.onToggleHidden
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Hides this item from customers. It can still be edited.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )
                    }

// 错误信息
                    if (!uiState.errorMessage.isNullOrBlank() && !showRequiredFieldErrors) {
                        NdjcBaseDialog(
                            onDismissRequest = actions.onDismissError,
                            title = "Cannot save item",
                            message = uiState.errorMessage,
                            confirmText = "OK",
                            onConfirmClick = actions.onDismissError
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (uiState.isNew) {
                            "After creation, the item is visible unless Hidden from list is enabled."
                        } else {
                            "Changes take effect after saving."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                    )

                    Spacer(Modifier.height(10.dp))

// 保存按钮（Create / Save 区分）
                    NdjcPrimaryActionButton(
                        text = if (uiState.isNew) "Create" else "Save",
                        onClick = actions.onSave,
                        enabled = uiState.canSave,
                        isLoading = uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        val st = imagePreview
        if (st != null) {
            NdjcFullscreenImageViewerScreen(
                images = st.images,
                startIndex = st.startIndex,
                onDismiss = { imagePreview = null }
            )
        }

        NdjcSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = NdjcSnackbarTokens.HorizontalPadding,
                    end = NdjcSnackbarTokens.HorizontalPadding,
                    bottom = NdjcSnackbarTokens.BottomPadding
                )
        )
    }
}

@Composable
private fun FullscreenImagePreviewDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = "Preview image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(NdjcCommonTokens.Dp.Dp16)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NdjcFullscreenImageViewerScreen(
    images: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit,
    onSave: ((String) -> Unit)? = null
) {
    if (images.isEmpty()) return

    val safeStart = startIndex.coerceIn(0, images.lastIndex)
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        NdjcFullscreenViewerRegistry.visible.value = true
        onDispose {
            NdjcFullscreenViewerRegistry.visible.value = false
        }
    }

    NdjcSystemBars(
        color = Color.Black,
        darkIcons = false,
        key = "fullscreen_image_${images.hashCode()}_$safeStart",
        navigationBarColor = Color.Black,
        lightNavIcons = false,
        decorFitsSystemWindows = false
    )
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = safeStart,
        pageCount = { images.size }
    )
    var zoomedPage by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(999f),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            userScrollEnabled = zoomedPage == null || zoomedPage != pagerState.currentPage,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val url = images[page]
            var scale by remember(page) { mutableStateOf(1f) }
            var offsetX by remember(page) { mutableStateOf(0f) }
            var offsetY by remember(page) { mutableStateOf(0f) }

            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
                val wasScaled = scale > 1.01f
                val willBeScaled = nextScale > 1.01f

                scale = nextScale

                if (willBeScaled) {
                    if (wasScaled) {
                        offsetX += panChange.x
                        offsetY += panChange.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                    zoomedPage = page
                } else {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                    if (zoomedPage == page) zoomedPage = null
                }
            }

            val imageGestureModifier = Modifier.transformable(
                state = transformState,
                canPan = { scale > 1.01f }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(page, images, onSave, pagerState.currentPage) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.01f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    if (zoomedPage == page) zoomedPage = null
                                } else {
                                    scale = 2f
                                    offsetX = 0f
                                    offsetY = 0f
                                    zoomedPage = page
                                }
                            },
                            onTap = {
                                if (scale > 1.01f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    if (zoomedPage == page) zoomedPage = null
                                } else if (!pagerState.isScrollInProgress) {
                                    onDismiss()
                                }
                            },
                            onLongPress = {
                                if (onSave == null) return@detectTapGestures
                                val idx = pagerState.currentPage.coerceIn(0, images.lastIndex)
                                pendingSaveUrl = images[idx]
                            }
                        )
                    }
                    .then(imageGestureModifier),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(url),
                    contentDescription = "Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    contentScale = ContentScale.Fit
                )

                LaunchedEffect(scale) {
                    if (scale <= 1.01f) {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                        if (zoomedPage == page) zoomedPage = null
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(NdjcCommonTokens.Dp.Dp14),
            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onSave != null) {
                Text(
                    text = "Download",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp8))
                        .clickable {
                            val idx = pagerState.currentPage.coerceIn(0, images.lastIndex)
                            pendingSaveUrl = images[idx]
                        }
                        .padding(
                            horizontal = NdjcCommonTokens.Dp.Dp4,
                            vertical = NdjcCommonTokens.Dp.Dp2
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(NdjcCommonTokens.Dp.Dp34)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        if (images.size > 1) {
            val current = pagerState.currentPage.coerceIn(0, images.lastIndex) + 1
            Text(
                text = "$current/${images.size}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = NdjcCommonTokens.Alpha.a85),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = NdjcCommonTokens.Dp.Dp14,
                        bottom = NdjcCommonTokens.Dp.Dp18
                    )
                    .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp10))
                    .background(Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a35))
                    .padding(
                        horizontal = NdjcCommonTokens.Dp.Dp10,
                        vertical = NdjcCommonTokens.Dp.Dp6
                    )
            )
        }

        val p = pendingSaveUrl
        if (p != null) {
            NdjcBaseDialog(
                onDismissRequest = { pendingSaveUrl = null },
                title = "Download image",
                message = "Save this image to your device?",
                confirmText = "Save",
                dismissText = "Cancel",
                onConfirmClick = {
                    pendingSaveUrl = null
                    onSave?.invoke(p)
                },
                onDismissClick = { pendingSaveUrl = null }
            )
        }
    }
}
@Composable
internal fun ShowcaseChangePassword(
    state: ShowcaseChangePasswordUiState,
    actions: ShowcaseChangePasswordActions,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() },
        )
    ) {

        Box(modifier = Modifier.fillMaxSize()) {

            // ✅ Back 按钮移到白卡外：左上角固定
                    // ✅ Back/Home 已由 NdjcUnifiedBackground(topNav=...) 统一渲染；这里不再重复绘制
            // ✅ 内容整体下移，给 Back 让位（避免与白卡重叠）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        bottom = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        top = NdjcTopContentPadding
                    )
            ) {
                NdjcWhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // ✅ 标题 + 副标题（更中性、更专业）
                        Text(
                            text = "Change password",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                        Text(
                            text = "Update your credentials for this account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                        )

// ✅ 微调 1：副标题到字段更利落（16 → 12）
                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp16))

                        NdjcTextField(
                            value = state.current,
                            onValueChange = actions.onCurrentChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = "Current password",
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp12))

                        NdjcTextField(
                            value = state.next,
                            onValueChange = actions.onNextChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = "New password",
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                        Text(
                            text = "Use at least 8 characters. Avoid common passwords.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp12))

                        NdjcTextField(
                            value = state.confirm,
                            onValueChange = actions.onConfirmChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = "Confirm new password",
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp4))

                        Text(
                            text = "Must match the new password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        if (!state.error.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp10))
                            Text(
                                text = state.error!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
                            )
                        }

                        if (!state.success.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp10))
                            Text(
                                text = state.success!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                            )
                        }

                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp16))

                        NdjcPrimaryActionButton(
                            text = if (state.isSaving) "Updating..." else "Update password",
                            onClick = actions.onSubmit,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        )

// ✅ 微调 2：按钮与脚注更舒展（8 → 12）
                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp16))

                        Text(
                            text = "You may need to sign in again after updating your password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )
                    }
                }
            }
        }
    }
}

// ---------- Detail 页面（查看详情） ----------
@Composable
private fun NdjcShimmerImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    placeholderCornerRadius: Dp = NdjcCommonTokens.Dp.Dp16,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    val painter = rememberAsyncImagePainter(model = imageUrl)
    val painterState = painter.state
    val isSuccess = painterState is AsyncImagePainter.State.Success

    val imageAlpha by animateFloatAsState(
        targetValue = if (isSuccess) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "ndjc_image_fade_in"
    )

    val shimmerTransition = rememberInfiniteTransition(label = "ndjc_image_shimmer")

    val shimmerTranslate by shimmerTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(
                durationMillis = 1100,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ndjc_image_shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            NdjcCommonTokens.Colors.C_FFF1F1F1,
            NdjcCommonTokens.Colors.C_FFF8FAFC,
            NdjcCommonTokens.Colors.C_FFF1F1F1
        ),
        start = Offset(shimmerTranslate, 0f),
        end = Offset(shimmerTranslate + 300f, 300f)
    )

    Box(
        modifier = modifier
            .background(NdjcCommonTokens.Colors.C_FFF1F1F1)
    ) {
        if (!isSuccess) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(placeholderCornerRadius))
                    .background(shimmerBrush)
            )
        }

        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .matchParentSize()
                .alpha(imageAlpha),
            contentScale = contentScale
        )
    }
}
@Composable
internal fun ShowcaseDishDetail(
    uiState: ShowcaseDetailUiState,
    actions: ShowcaseDetailActions,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    // 背景与首页保持一致
    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() },
            iconOnly = true
        )
    ) {

        // ✅ 沉浸页面：透明状态栏（纯 UI 外观控制，不越界）
        NdjcSystemBarsTransparent(
            darkIcons = true,
            key = uiState.dishId
        )
        // ✅ 对齐 Chat：图片全屏 Pager 预览状态（纯 UI，不走导航，不越界）
        data class NdjcImagePreviewState(
            val images: List<String>,
            val startIndex: Int
        )
        var imagePreview by remember { mutableStateOf<NdjcImagePreviewState?>(null) }



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
                // ✅ Back/Home 已由 NdjcUnifiedBackground(topNav=...) 统一渲染；这里不再重复绘制
        // 中间内容：上半部分大图 + 下半部分直接文本
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

// ---------- 图片区（上半屏）：多图滑动 + 指示器 + 全屏预览 + 空占位 ----------
            val images = uiState.imageUrls
            val initialIndex = uiState.safeImageIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))

// ✅ 1) LazyRow 的 state：初始滚到 initialIndex
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

// ✅ 2) UI 侧“当前页”计算：用 firstVisibleItemIndex + scrollOffset 判断是否已经滑到下一页
//    这样圆点会跟着动，不依赖 VM 是否及时回写 uiState.safeImageIndex
            val currentIndex by remember {
                derivedStateOf {
                    if (images.isEmpty()) return@derivedStateOf 0
                    val first = listState.firstVisibleItemIndex
                    val offset = listState.firstVisibleItemScrollOffset
                    val itemWidthPx = listState.layoutInfo.viewportSize.width.coerceAtLeast(1)

                    // offset 过半就认为到下一页（Pager 体验）
                    val add = if (offset > itemWidthPx / 2) 1 else 0
                    (first + add).coerceIn(0, images.lastIndex)
                }
            }

// ✅ 3) 当 images / initialIndex 变化时，确保滚到对应位置
            LaunchedEffect(images.size, initialIndex) {
                if (images.isEmpty()) return@LaunchedEffect
                listState.scrollToItem(initialIndex.coerceIn(0, images.lastIndex))
            }

// ✅ 4) 仍然把 index 上报给逻辑层（不越界）：逻辑层想用就用，不影响 UI 圆点
            LaunchedEffect(images.size) {
                if (images.isEmpty()) return@LaunchedEffect

                snapshotFlow { currentIndex }
                    .distinctUntilChanged()
                    .collect { idx ->
                        actions.onImageIndexChanged(idx)
                    }
            }
            val side = LocalConfiguration.current.screenWidthDp.dp  // ✅ 屏幕多宽，side 就多宽

            Box(
                modifier = Modifier
                    .size(size = side)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DetailTokens.HeroGradientTop,
                                DetailTokens.HeroGradientBottom
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (images.isEmpty()) {
                    Text(
                        text = "No image",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                } else {
                    LazyRow(
                        state = listState,
                        modifier = Modifier.matchParentSize()
                    ) {
                        items(images) { url ->
                            Box(
                                modifier = Modifier
                                    .size(side)
                                    .clickable {
                                        val imgs = images.filter { it.isNotBlank() }
                                        if (imgs.isNotEmpty()) {
                                            val idx = currentIndex.coerceIn(0, imgs.lastIndex)
                                            imagePreview = NdjcImagePreviewState(images = imgs, startIndex = idx)
                                        }
                                    }
                            ) {
                                NdjcShimmerImage(
                                    imageUrl = url,
                                    modifier = Modifier.matchParentSize(),
                                    placeholderCornerRadius = NdjcCommonTokens.Dp.Dp16,
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    if (images.size > 1) {
                        Text(
                            text = "${currentIndex + 1}/${images.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = NdjcCommonTokens.Alpha.a85),
                            modifier = Modifier
                                .align(Alignment.BottomCenter) // ✅ 居中
                                .padding(bottom = NdjcCommonTokens.Dp.Dp10)
                                .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp10))
                                .background(Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a25))
                                .padding(horizontal = NdjcCommonTokens.Dp.Dp10, vertical = NdjcCommonTokens.Dp.Dp6)
                        )
                    }
                }
            }

// ✅ HeaderRow：详情页比 Home 大一档，和整体排版比例更平衡
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DetailTokens.ContentHorizontalPadding,
                        vertical = NdjcCommonTokens.Dp.Dp12
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (uiState.isRecommended) {
                    Surface(
                        shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999),
                        color = DishCardTokens.BadgeBg,
                        border = BorderStroke(
                            DishCardTokens.BadgeBorderWidth,
                            DishCardTokens.BadgeBorderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = NdjcCommonTokens.Dp.Dp12,
                                vertical = NdjcCommonTokens.Dp.Dp6
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = DishCardTokens.BadgeTextColor,
                                modifier = Modifier.size(NdjcCommonTokens.Dp.Dp18)
                            )
                            Text(
                                text = "Pick",
                                color = DishCardTokens.BadgeTextColor,
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp24))
                }

                val favoriteInteractionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .size(NdjcCommonTokens.Dp.Dp24)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = favoriteInteractionSource,
                            indication = null
                        ) { actions.onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isFavorite) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = DishCardTokens.FavIconTint,
                            modifier = Modifier.size(NdjcCommonTokens.Dp.Dp24)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = BackButtonTokens.Background,
                            modifier = Modifier.size(NdjcCommonTokens.Dp.Dp24)
                        )
                    }
                }
            }

// 2）图片下方直接文本，不再用 Card
            Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DetailTokens.ContentHorizontalPadding)
                    .padding(top = NdjcCommonTokens.Dp.Dp6, bottom = DetailTokens.ContentVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp12)
            ) {
// 方案 A：信息区做“层级 + 分隔 + 小分区”，减少空旷感（纯 UI，不动逻辑）
                var descExpanded by remember(uiState.dishId) { mutableStateOf(false) }
// 标题 + 价格（Rec 在标题上方；标题与价格左对齐）
                Column(verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)) {

                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = NdjcCommonTokens.Sp.Sp28
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)
                    ) {
                        if (!uiState.discountPrice.isNullOrBlank()) {
                            Text(
                                text = "$${uiState.discountPrice}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = NdjcCommonTokens.Sp.Sp28
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Text(
                                text = "$${uiState.price}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a50)
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
                Divider(
                    modifier = Modifier.padding(top = NdjcCommonTokens.Dp.Dp6),
                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10),
                    thickness = NdjcCommonTokens.Dp.Dp1
                )

                // Description 分区（默认收起 3 行，点文字展开；纯 UI 状态，不越界）
                if (uiState.description.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                        )

                        Text(
                            text = uiState.description,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = NdjcCommonTokens.Sp.Sp18
                            ),
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
                            maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (descExpanded) "Show less" else "Show more",
                            style = MaterialTheme.typography.labelMedium,
                            color = HomeTokens.ChipSelectedBackground,
                            modifier = Modifier.clickable { descExpanded = !descExpanded }
                        )
                    }
                }

                // Category / Status 分区
                Column(verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)) {

                    if (!uiState.category.isNullOrBlank()) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)) {
                            NdjcPillButton(
                                text = uiState.category!!,
                                selected = true,
                                onClick = { },
                                modifier = Modifier
                            )
                        }
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
            Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp32))
        }

        val st = imagePreview
        if (st != null) {
            NdjcFullscreenImageViewerScreen(
                images = st.images,
                startIndex = st.startIndex,
                onDismiss = { imagePreview = null },
                onSave = { url -> actions.onSavePreviewImage(url) }
            )
        }
    }
}

// ---------- Admin 管理页面 ----------
// ---------- StoreProfile 店铺信息页面（可编辑/可保存） ----------
@Composable
//endregion

//region 8 StoreProfile
internal fun ShowcaseStoreProfileView(
    uiState: ShowcaseStoreProfileUiState,
    actions: ShowcaseStoreProfileActions,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    // ✅ StoreProfile：点击图片 -> 全屏 Pager 预览（对齐 Detail/Chat）
    data class NdjcImagePreviewState(
        val images: List<String>,
        val startIndex: Int
    )
    var imagePreview by remember { mutableStateOf<NdjcImagePreviewState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() },
            iconOnly = true,
            iconTint = BackButtonTokens.Background
        )
    ) {

        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetLeftX,
            offsetY = HomeTokens.HeroCircleOffsetLeftY,
            colors = listOf(HomeTokens.BrandPurple, HomeTokens.BrandPurpleTransparent)
        )
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetRightX,
            offsetY = HomeTokens.HeroCircleOffsetRightY,
            colors = listOf(HomeTokens.BrandPurpleAlt, HomeTokens.BrandPurpleTransparent)
        )
                // ✅ Back/Home 已由 NdjcUnifiedBackground(topNav=...) 统一渲染；这里不再重复绘制
        val scrollState = rememberScrollState()

// ✅ 拖拽期间禁止父级滚动，避免 dragCancel
        var isDraggingImages by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState, enabled = !isDraggingImages)
                .padding(horizontal = NdjcWhiteCardLayoutTokens.ScreenPadding) // ✅
        ) {
            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp36))

            // ===== 品牌封面 + 信息白卡（同一个 Box 叠层，避免 offset 幽灵占位）=====
            val covers = uiState.coverUrl
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(9)
                .toList()

            // 顶部封面高度与白卡起始位置
            val coverHeight = NdjcCommonTokens.Dp.Dp150
            val cardTopPadding = StoreProfileTokens.TopContentPadding

            val hasDescription = uiState.description.trim().isNotBlank()
            val hasAbout = hasDescription

            Box(modifier = Modifier.fillMaxWidth()) {

                if (covers.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(coverHeight)
                            .clip(RoundedCornerShape(NdjcWhiteCardLayoutTokens.CardRadius)),
                        horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
                        contentPadding = PaddingValues(horizontal = NdjcCommonTokens.Dp.Dp0)
                    ) {
                        itemsIndexed(covers) { idx, url ->
                            val coverShape = RoundedCornerShape(NdjcWhiteCardLayoutTokens.CardRadius)
                            val coverInteractionSource = remember { MutableInteractionSource() }
                            val coverPressed by coverInteractionSource.collectIsPressedAsState()
                            val coverScale by animateFloatAsState(
                                targetValue = if (coverPressed) 0.965f else 1f,
                                animationSpec = tween(durationMillis = 120),
                                label = "StoreProfileTopCoverScale"
                            )

                            Surface(
                                shape = coverShape,
                                color = NdjcCommonTokens.Colors.C_FFF3F4F6,
                                shadowElevation = NdjcCommonTokens.Dp.Dp0,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(NdjcCommonTokens.Dp.Dp240)
                                    .graphicsLayer {
                                        scaleX = coverScale
                                        scaleY = coverScale
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(coverShape)
                                        .clickable(
                                            interactionSource = coverInteractionSource,
                                            indication = ripple(bounded = true)
                                        ) {
                                            imagePreview = NdjcImagePreviewState(
                                                images = covers,
                                                startIndex = idx
                                            )
                                        }
                                ) {
                                    NdjcShimmerImage(
                                        imageUrl = url,
                                        contentDescription = "Cover ${idx + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        placeholderCornerRadius = NdjcWhiteCardLayoutTokens.CardRadius,
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a30)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(coverHeight),
                        horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                    ) {
                        UniversalStoreCoverPlaceholderCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        UniversalStoreCoverPlaceholderCard(
                            modifier = Modifier
                                .width(NdjcCommonTokens.Dp.Dp84)
                                .fillMaxHeight()
                        )
                    }
                }

                NdjcWhiteCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = cardTopPadding)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                    ) {

                        val sectionDivider: @Composable () -> Unit = {
                            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp14))
                            Divider(
                                color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a10 * 0.6f),
                                thickness = NdjcCommonTokens.Dp.Dp1
                            )
                            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp14))
                        }

                        UniversalStoreBrandHeader(
                            coverUrl = "",
                            logoUrl = uiState.logoUrl,
                            title = uiState.title,
                            subtitle = uiState.subtitle,
                            businessStatus = uiState.businessStatus,
                            onPreview = { images, startIndex ->
                                imagePreview = NdjcImagePreviewState(images = images, startIndex = startIndex)
                            }
                        )

                        sectionDivider()

                        UniversalStoreAboutSection(description = uiState.description)

                        sectionDivider()

                        UniversalStoreServicesSection(services = uiState.services)

                        sectionDivider()

                        UniversalStoreLocationSection(
                            address = uiState.address,
                            hours = uiState.hours,
                            mapUrl = uiState.mapUrl,
                            onOpenMap = { actions.onOpenMap(it) }
                        )

                        sectionDivider()

                        UniversalStoreExtraContactsSection(
                            extraContacts = uiState.extraContacts,
                            onCopyAccountValue = { label, value ->
                                actions.onCopy(label, value)
                            }
                        )
                    }
                }
            }

            // ✅ 页面底部呼吸感（这个在白卡外部，不影响白卡内 padding=16）
            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp32))
        }
// ✅ 点击封面/Logo -> 全屏预览（复用商品详情页同款 Dialog）
        val st = imagePreview
        if (st != null && st.images.isNotEmpty()) {
            NdjcFullscreenImageViewerScreen(
                images = st.images,
                startIndex = st.startIndex,
                onDismiss = { imagePreview = null },
                onSave = { url -> actions.onSavePreviewImage(url) }
            )
        }

        // ✅ “Copied” 提示（overlay）
        NdjcSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = NdjcSnackbarTokens.HorizontalPadding,
                    end = NdjcSnackbarTokens.HorizontalPadding,
                    bottom = NdjcSnackbarTokens.BottomPadding
                )
        )



    }
}
@Composable
internal fun ShowcaseStoreProfileEdit(
    uiState: ShowcaseStoreProfileUiState,
    actions: ShowcaseStoreProfileActions,
    modifier: Modifier = Modifier
) {
    var pendingExitTarget by rememberSaveable { mutableStateOf<String?>(null) }

    fun requestExit(target: String) {
        if (uiState.hasUnsavedChanges) {
            pendingExitTarget = target
        } else {
            if (target == "home") {
                actions.onBackToHome()
            } else {
                actions.onBack()
            }
        }
    }

    BackHandler(enabled = pendingExitTarget == null) {
        requestExit("back")
    }

    if (pendingExitTarget != null) {
        NdjcBaseDialog(
            onDismissRequest = { pendingExitTarget = null },
            title = "Discard unsaved changes?",
            message = "You have unsaved merchant profile changes. Leave this page and discard them?",
            confirmText = "Discard",
            dismissText = "Stay",
            onConfirmClick = {
                val target = pendingExitTarget
                pendingExitTarget = null
                if (target == "home") {
                    actions.onBackToHome()
                } else {
                    actions.onBack()
                }
            },
            onDismissClick = { pendingExitTarget = null },
            destructiveConfirm = true
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    val mapUrlDialogMessage = uiState.saveError?.takeIf {
        it.contains("Map URL") && (it.contains("http://") || it.contains("https://"))
    }
    var showMapUrlDialog by rememberSaveable(mapUrlDialogMessage) {
        mutableStateOf(mapUrlDialogMessage != null)
    }

    if (showMapUrlDialog && !mapUrlDialogMessage.isNullOrBlank()) {
        NdjcBaseDialog(
            onDismissRequest = { showMapUrlDialog = false },
            title = "Invalid Map URL",
            message = mapUrlDialogMessage,
            confirmText = "OK",
            dismissText = null,
            onConfirmClick = { showMapUrlDialog = false },
            onDismissClick = { showMapUrlDialog = false },
            destructiveConfirm = false
        )
    }

    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { requestExit("back") },
            onHome = { requestExit("home") },
        )
    ) {

        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetLeftX,
            offsetY = HomeTokens.HeroCircleOffsetLeftY,
            colors = listOf(HomeTokens.BrandPurple, HomeTokens.BrandPurpleTransparent)
        )
        BgCircle(
            size = HomeTokens.HeroCircleSize,
            offsetX = HomeTokens.HeroCircleOffsetRightX,
            offsetY = HomeTokens.HeroCircleOffsetRightY,
            colors = listOf(HomeTokens.BrandPurpleAlt, HomeTokens.BrandPurpleTransparent)
        )

        // ✅ Back/Home 已由 NdjcUnifiedBackground(topNav=...) 统一渲染；这里不再重复绘制
        val scrollState = rememberScrollState()
        val uiScope = rememberCoroutineScope()
        val fieldScrollTopOffsetPx = with(LocalDensity.current) { 260.dp.roundToPx() }

        var formTopInRootPx by remember { mutableStateOf(0) }
        var titleTopInRootPx by remember { mutableStateOf(0) }
        var addressTopInRootPx by remember { mutableStateOf(0) }
        var contactsTopInRootPx by remember { mutableStateOf(0) }

        val titleRequiredError = uiState.saveError == "Store title is required."
        val addressRequiredError =
            uiState.saveError == "已填写 Map URL，但文本地址（Address）为空：请先填写地址，否则无法保存。"
        val contactRequiredError =
            uiState.saveError == "有联系方式只填了一半（Name/Value），请补全或清空后再保存。"

        fun scrollToField(targetTopInRootPx: Int) {
            val target = (
                    scrollState.value +
                            targetTopInRootPx -
                            formTopInRootPx -
                            fieldScrollTopOffsetPx
                    ).coerceAtLeast(0)

            uiScope.launch {
                scrollState.animateScrollTo(target)
            }
        }

        LaunchedEffect(titleRequiredError, addressRequiredError, contactRequiredError) {
            when {
                titleRequiredError -> scrollToField(titleTopInRootPx)
                addressRequiredError -> scrollToField(addressTopInRootPx)
                contactRequiredError -> scrollToField(contactsTopInRootPx)
            }
        }

        // ✅ Cover 拖拽时禁用父级滚动，避免 dragCancel
        var isDraggingCoverImages by remember { mutableStateOf(false) }

        data class NdjcImagePreviewState(
            val images: List<String>,
            val startIndex: Int
        )
        var imagePreview by remember { mutableStateOf<NdjcImagePreviewState?>(null) }

// ✅ 自定义联系方式：底部那一行“空输入框”的临时输入（只属于 UI，不存储）
        var extraNewName by rememberSaveable { mutableStateOf("") }
        var extraNewValue by rememberSaveable { mutableStateOf("") }

// ✅ 业务范围：永远预留的那一行空输入框（UI-only）
        var serviceNewValue by rememberSaveable { mutableStateOf("") }

// ✅ 可选：本地提示（只用于拦截保存时提示）
        var extraLocalError by rememberSaveable { mutableStateOf<String?>(null) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    formTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                }
                .verticalScroll(scrollState, enabled = !isDraggingCoverImages)
                .padding(
                    top = NdjcTopContentPadding,
                    start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    bottom = NdjcWhiteCardLayoutTokens.ScreenPadding
                ),
            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)
        ) {

            NdjcWhiteCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Title + subtitle
                    Text(
                        text = "Edit Store Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp4))
                    Text(
                        text = "Update your public store information shown to customers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                    )

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp12))

// ------------------------- Section: Brand -------------------------
                    StoreEditSectionTitle(
                        title = "Brand",
                        subtitle = "Displayed at the top of your public profile."
                    )
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    ProfileField(
                        label = "Title *",
                        value = uiState.draftTitle,
                        onChange = actions.onTitleChange,
                        enabled = !uiState.isSaving,
                        isError = titleRequiredError,
                        errorText = if (titleRequiredError) "Store title is required." else null,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            titleTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                        }
                    )

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp10))

                    ProfileField(
                        "Subtitle",
                        uiState.draftSubtitle,
                        actions.onSubtitleChange,
                        enabled = !uiState.isSaving
                    )

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp18))

// ------------------------- Section: About -------------------------
                    StoreEditSectionTitle(
                        title = "About",
                        subtitle = "This description appears in your public profile."
                    )
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    val descMax = 200
                    val descText = uiState.draftDescription
                    val descLen = descText.length.coerceAtMost(descMax)

                    NdjcTextField(
                        value = descText,
                        onValueChange = { actions.onDescriptionChange(it.take(descMax)) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 4,
                        label = "Description"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "$descLen/$descMax",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )
                    }

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp18))

// ------------------------- Section: Business Scope -------------------------
                    StoreEditSectionTitle(
                        title = "Business Scope",
                        subtitle = "List the services or categories you provide. Empty entries will not be saved."
                    )
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    StoreServicesEditor(
                        enabled = !uiState.isSaving,
                        items = uiState.draftServices,
                        errorText = uiState.validationError,
                        newValue = serviceNewValue,
                        onNewValueChange = { serviceNewValue = it },
                        onChange = actions.onServiceChange,
                        onAdd = actions.onServiceAdd,
                        onRemove = actions.onServiceRemove
                    )

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp18))

// ------------------------- Section: Location & Hours -------------------------
                    StoreEditSectionTitle(
                        title = "Location & Hours",
                        subtitle = "If left empty, this section will not appear in your public profile."
                    )
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    ProfileField(
                        label = "Address",
                        value = uiState.draftAddress,
                        onChange = actions.onAddressChange,
                        enabled = !uiState.isSaving,
                        minLines = 1,
                        isError = addressRequiredError,
                        errorText = if (addressRequiredError) "Address is required when Map URL is filled." else null,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            addressTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                        }
                    )

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp10))

                    ProfileField(
                        "Hours",
                        uiState.draftHours,
                        actions.onHoursChange,
                        enabled = !uiState.isSaving,
                        minLines = 1
                    )

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp10))

                    ProfileField(
                        "Map URL (optional)",
                        uiState.draftMapUrl,
                        actions.onMapUrlChange,
                        enabled = !uiState.isSaving,
                        minLines = 1
                    )

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp18))

// ------------------------- Section: Contact -------------------------
                    StoreEditSectionTitle(
                        title = "Contact",
                        subtitle = "These details will be visible to customers in your profile."
                    )
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                contactsTopInRootPx = coordinates.positionInRoot().y.roundToInt()
                            }
                    ) {
                        StoreExtraContactsEditor(
                            enabled = !uiState.isSaving,
                            items = uiState.draftExtraContacts,
                            errorText =
                                when {
                                    contactRequiredError -> "A contact item is incomplete (Name/Value). Please complete it or remove it before saving."
                                    !extraLocalError.isNullOrBlank() -> extraLocalError
                                    else -> null
                                },
                            newName = extraNewName,
                            newValue = extraNewValue,
                            onNewNameChange = { extraNewName = it; extraLocalError = null },
                            onNewValueChange = { extraNewValue = it; extraLocalError = null },
                            onNameChange = actions.onExtraContactNameChange,
                            onValueChange = actions.onExtraContactValueChange,
                            onAdd = actions.onExtraContactAdd,
                            onRemove = actions.onExtraContactRemove
                        )
                    }

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp18))

// ------------------------- Section: Media -------------------------
                    StoreEditSectionTitle(
                        title = "Media",
                        subtitle = "Images displayed in your public profile. The first cover image is featured prominently. Up to 9 images."
                    )
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    // ✅ Cover（最多 9 张）
                    StoreProfileCoverPicker(
                        coverRaw = uiState.draftCoverUrl,
                        enabled = !uiState.isSaving,
                        onPick = actions.onPickCovers,
                        onRemove = actions.onRemoveCover,
                        onMove = actions.onMoveCover,
                        onDraggingChange = { isDraggingCoverImages = it },
                        onPreview = { images, startIndex ->
                            imagePreview = NdjcImagePreviewState(
                                images = images,
                                startIndex = startIndex
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp10))

                    // ✅ Logo（1 张）
                    StoreProfileLogoPicker(
                        logoRaw = uiState.draftLogoUrl,
                        enabled = !uiState.isSaving,
                        onPick = actions.onPickLogo,
                        onRemove = actions.onRemoveLogo,
                        onPreview = { images, startIndex ->
                            imagePreview = NdjcImagePreviewState(
                                images = images,
                                startIndex = startIndex
                            )
                        }
                    )

                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp14))

                    NdjcPrimaryActionButton(
                        text = "Save",
                        onClick = {
                            val n = extraNewName.trim()
                            val v = extraNewValue.trim()
                            val halfFilled =
                                (n.isBlank() && v.isNotBlank()) || (n.isNotBlank() && v.isBlank())

                            if (halfFilled) {
                                extraLocalError = "A contact item is incomplete (Name/Value). Please complete it or remove it before saving."
                                scrollToField(contactsTopInRootPx)
                                return@NdjcPrimaryActionButton
                            }
                            actions.onSave()
                        },
                        enabled = !uiState.isSaving,
                        isLoading = uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

                    Text(
                        text = "Changes are saved immediately and reflected in your public profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                    )
                }
            }
            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp32))
        }

        val st = imagePreview
        if (st != null) {
            NdjcFullscreenImageViewerScreen(
                images = st.images,
                startIndex = st.startIndex,
                onDismiss = { imagePreview = null }
            )
        }

        NdjcSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = NdjcSnackbarTokens.HorizontalPadding,
                    end = NdjcSnackbarTokens.HorizontalPadding,
                    bottom = NdjcSnackbarTokens.BottomPadding
                )
        )
    }
}
@Composable
internal fun StoreProfileHeaderBlock(
    coverUrl: String,
    logoUrl: String,
    businessStatus: String,
    onPreview: (images: List<String>, startIndex: Int) -> Unit
) {

    // ✅ 介绍图片（最多 9 张）：coverUrl 用 "\n" 拼接时，这里拆开
    val covers = coverUrl
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(9)
        .toList()

    if (covers.isNotEmpty()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(NdjcCommonTokens.Dp.Dp140),
            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
            contentPadding = PaddingValues(horizontal = NdjcCommonTokens.Dp.Dp2)
        ) {
            items(covers.size) { idx ->
                val url = covers[idx]
                Surface(
                    shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp14),
                    color = NdjcCommonTokens.Colors.C_FFF3F4F6,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(NdjcCommonTokens.Dp.Dp220)
                        .clickable { onPreview(covers, idx) } // ✅ 点击封面 -> 全屏 Pager
                ) {
                    NdjcShimmerImage(
                        imageUrl = url,
                        modifier = Modifier.fillMaxSize(),
                        placeholderCornerRadius = NdjcCommonTokens.Dp.Dp14,
                        contentDescription = "Store cover ${idx + 1}",
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))
    }

    // Logo + 营业状态（都为可选，非空才画）
    val logo = logoUrl.trim()
    val status = businessStatus.trim()

    if (logo.isNotBlank() || status.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (logo.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999),
                    color = NdjcCommonTokens.Colors.C_FFE5E7EB,
                    modifier = Modifier
                        .size(NdjcCommonTokens.Dp.Dp56)
                        .clickable { onPreview(listOf(logo), 0) } // ✅ 点击 logo -> 单图全屏
                ) {
                    NdjcShimmerImage(
                        imageUrl = logo,
                        modifier = Modifier.fillMaxSize(),
                        placeholderCornerRadius = NdjcCommonTokens.Dp.Dp999,
                        contentDescription = "Store logo",
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Spacer(Modifier.size(NdjcCommonTokens.Dp.Dp56))
            }

            if (status.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999),
                    color = HomeTokens.ChipSelectedBackground
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = NdjcCommonTokens.Dp.Dp12, vertical = NdjcCommonTokens.Dp.Dp6),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}



@Composable
private fun StoreProfileMapPreview(
    address: String,
    mapUrl: String,
    onOpenMap: (String) -> Unit
) {
    val url = mapUrl.trim()
    val a = address.trim()

    // ✅ 空字段：直接不渲染（符合“有输入才显示”）
    if (url.isBlank() && a.isBlank()) return

    // ✅ 允许：mapUrl 或 address 任一存在即可点击
    val canOpen = url.isNotBlank() || a.isNotBlank()

    Surface(
        shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp14),
        color = NdjcCommonTokens.Colors.C_FFF3F4F6,
        modifier = Modifier
            .fillMaxWidth()
            .height(NdjcCommonTokens.Dp.Dp120)
            .then(if (canOpen) Modifier.clickable { onOpenMap(url) } else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(NdjcCommonTokens.Dp.Dp12),
            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
        ) {
            Text("Map", style = MaterialTheme.typography.labelLarge, color = NdjcCommonTokens.Colors.C_FF111827)
            Text(
                text = a, // 这里 a 一定不是空（否则上面已 return）
                style = MaterialTheme.typography.bodyMedium,
                color = NdjcCommonTokens.Colors.C_FF374151
            )
            if (canOpen) {
                Text(
                    text = "Tap to open",
                    style = MaterialTheme.typography.labelMedium,
                    color = NdjcCommonTokens.Colors.C_FF6B7280
                )
            }
        }
    }
}
// ------------------------- Universal Store Profile UI -------------------------
@Composable
private fun UniversalStoreCoverPlaceholderCard(
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp14),
        color = NdjcCommonTokens.Colors.C_FFF3F4F6,
        shadowElevation = NdjcCommonTokens.Dp.Dp0,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.38f),
                            NdjcCommonTokens.Colors.C_FFF3F4F6,
                            NdjcCommonTokens.Colors.C_FFE5E7EB
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            radius = 700f
                        )
                    )
            )
        }
    }
}
@Composable
private fun UniversalStoreLogoPlaceholder(
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = NdjcCommonTokens.Colors.C_FFE5E7EB,
        shadowElevation = NdjcCommonTokens.Dp.Dp0,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.34f),
                            NdjcCommonTokens.Colors.C_FFE5E7EB
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint = Color(0xFF9CA3AF).copy(alpha = 0.42f),
                modifier = Modifier.size(NdjcCommonTokens.Dp.Dp34)
            )
        }
    }
}
@Composable
private fun UniversalStoreBrandHeader(
    coverUrl: String,
    logoUrl: String,
    title: String,
    subtitle: String,
    businessStatus: String,
    onPreview: (images: List<String>, startIndex: Int) -> Unit
) {
    val covers = coverUrl
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(5)
        .toList()

    if (covers.isNotEmpty()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(NdjcCommonTokens.Dp.Dp150),
            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
            contentPadding = PaddingValues(horizontal = NdjcCommonTokens.Dp.Dp2)
        ) {
            itemsIndexed(covers) { idx, url ->
                val coverShape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp14)
                val coverInteractionSource = remember { MutableInteractionSource() }
                val coverPressed by coverInteractionSource.collectIsPressedAsState()
                val coverScale by animateFloatAsState(
                    targetValue = if (coverPressed) 0.965f else 1f,
                    animationSpec = tween(durationMillis = 120),
                    label = "StoreCoverScale"
                )

                Surface(
                    shape = coverShape,
                    color = NdjcCommonTokens.Colors.C_FFF3F4F6,
                    shadowElevation = NdjcCommonTokens.Dp.Dp0,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(NdjcCommonTokens.Dp.Dp240)
                        .graphicsLayer {
                            scaleX = coverScale
                            scaleY = coverScale
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(coverShape)
                            .clickable(
                                interactionSource = coverInteractionSource,
                                indication = ripple(bounded = true)
                            ) {
                                onPreview(covers, idx)
                            }
                    ) {
                        NdjcShimmerImage(
                            imageUrl = url,
                            modifier = Modifier.fillMaxSize(),
                            placeholderCornerRadius = NdjcCommonTokens.Dp.Dp14,
                            contentDescription = "Cover ${idx + 1}",
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp12))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val logo = logoUrl.trim()
        if (logo.isNotBlank()) {
            val logoInteractionSource = remember { MutableInteractionSource() }
            val logoPressed by logoInteractionSource.collectIsPressedAsState()
            val logoScale by animateFloatAsState(
                targetValue = if (logoPressed) 0.965f else 1f,
                animationSpec = tween(durationMillis = 120),
                label = "StoreLogoScale"
            )

            Surface(
                shape = CircleShape,
                color = NdjcCommonTokens.Colors.C_FFE5E7EB,
                shadowElevation = if (logoPressed) NdjcCommonTokens.Dp.Dp0 else NdjcCommonTokens.Dp.Dp2,
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = logoInteractionSource,
                            indication = ripple(bounded = true)
                        ) {
                            onPreview(listOf(logo), 0)
                        }
                ) {
                    NdjcShimmerImage(
                        imageUrl = logo,
                        modifier = Modifier.fillMaxSize(),
                        placeholderCornerRadius = NdjcCommonTokens.Dp.Dp999,
                        contentDescription = "Logo",
                        contentScale = ContentScale.Crop
                    )
                }
            }
        } else {
            UniversalStoreLogoPlaceholder(
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp12))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.ifBlank { "Store" },
                style = MaterialTheme.typography.titleLarge,
                color = NdjcCommonTokens.Colors.C_FF111827,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NdjcCommonTokens.Colors.C_FF6B7280,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
@Composable
private fun UniversalStoreEmptyInfoText() {
    Text(
        text = "No information added yet",
        style = MaterialTheme.typography.bodySmall.copy(
            lineHeight = 20.sp
        ),
        color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a45)
    )
}

@Composable
private fun UniversalStoreAboutSection(description: String) {
    val desc = description.trim()
    var expanded by rememberSaveable(desc) { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(NdjcCommonTokens.Dp.Dp2)
                .height(NdjcCommonTokens.Dp.Dp14)
                .background(HomeTokens.ChipSelectedBackground, RoundedCornerShape(999.dp))
        )
        Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))
        Text(
            text = "About",
            style = MaterialTheme.typography.labelLarge,
            color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a70),
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))

    if (desc.isBlank()) {
        UniversalStoreEmptyInfoText()
        return
    }

    val aboutBase = MaterialTheme.typography.bodyMedium.fontSize
    val aboutFontSize = if (aboutBase == androidx.compose.ui.unit.TextUnit.Unspecified) {
        15.sp
    } else {
        (aboutBase.value + 1f).sp
    }

    Text(
        text = desc,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = aboutFontSize
        ),
        color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a90),
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis
    )

    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))

    Text(
        text = if (expanded) "Show less" else "Show more",
        style = MaterialTheme.typography.labelLarge,
        color = HomeTokens.ChipSelectedBackground,
        modifier = Modifier.clickable { expanded = !expanded }
    )
}

@Composable
private fun UniversalStoreLocationSection(
    address: String,
    hours: String,
    mapUrl: String,
    onOpenMap: (String) -> Unit
) {
    val a = address.trim()
    val h = hours.trim()
    val m = mapUrl.trim()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(NdjcCommonTokens.Dp.Dp2)
                .height(NdjcCommonTokens.Dp.Dp14)
                .background(HomeTokens.ChipSelectedBackground, RoundedCornerShape(999.dp))
        )
        Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))
        Text(
            text = "Location & Hours",
            style = MaterialTheme.typography.labelLarge,
            color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a70),
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

    if (a.isBlank() && h.isBlank() && m.isBlank()) {
        UniversalStoreEmptyInfoText()
        return
    }

    if (a.isNotBlank() || m.isNotBlank()) {
        val openValue = if (m.isNotBlank()) m else a
        Surface(
            shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp16),
            color = Color.White,
            border = BorderStroke(
                width = NdjcCommonTokens.Dp.Dp1,
                color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a10 * 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMap(openValue) }
        ) {
            Column(
                modifier = Modifier.padding(NdjcCommonTokens.Dp.Dp16),
                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
            ) {
                Text(
                    text = "Address",
                    style = MaterialTheme.typography.labelLarge,
                    color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a70)
                )

                Text(
                    text = if (a.isNotBlank()) a else openValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a90)
                )

                Text(
                    text = "Tap to open",
                    style = MaterialTheme.typography.labelMedium,
                    color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a45)
                )
            }
        }

        if (h.isNotBlank()) {
            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp12))
        }
    }

    if (h.isNotBlank()) {
        Surface(
            shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp16),
            color = Color.White,
            border = BorderStroke(
                width = NdjcCommonTokens.Dp.Dp1,
                color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a10 * 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(NdjcCommonTokens.Dp.Dp16),
                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
            ) {
                Text(
                    text = "Hours",
                    style = MaterialTheme.typography.labelLarge,
                    color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a70)
                )
                Text(
                    text = h,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a90)
                )
            }
        }
    }
}
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun UniversalStoreServicesSection(
    services: List<String>
) {
    val list = services.map { it.trim() }.filter { it.isNotBlank() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(NdjcCommonTokens.Dp.Dp2)
                .height(NdjcCommonTokens.Dp.Dp14)
                .background(HomeTokens.ChipSelectedBackground, RoundedCornerShape(999.dp))
        )
        Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))
        Text(
            text = "Business Scope",
            style = MaterialTheme.typography.labelLarge,
            color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a70),
            fontWeight = FontWeight.Medium
        )
    }
    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

    if (list.isEmpty()) {
        UniversalStoreEmptyInfoText()
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8),
        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)
    ) {
        list.forEach { s ->
            Surface(
                shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999),
                color = NdjcCommonTokens.Colors.C_FFF8FAFC,
                border = BorderStroke(
                    width = 1.dp,
                    color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a10 * 0.6f)
                )
            ) {
                Text(
                    text = s,
                    modifier = Modifier.padding(
                        horizontal = NdjcCommonTokens.Dp.Dp12,
                        vertical = NdjcCommonTokens.Dp.Dp8
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a90)
                )
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UniversalStoreExtraContactsSection(
    extraContacts: List<ExtraContact>,
    onCopyAccountValue: (String, String) -> Unit
) {
    val list = extraContacts
        .map { it.copy(name = it.name.trim(), value = it.value.trim()) }
        .filterNot { it.name.isBlank() || it.value.isBlank() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(NdjcCommonTokens.Dp.Dp2)
                .height(NdjcCommonTokens.Dp.Dp14)
                .background(HomeTokens.ChipSelectedBackground, RoundedCornerShape(999.dp))
        )
        Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))
        Text(
            text = "More",
            style = MaterialTheme.typography.labelLarge,
            color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a70),
            fontWeight = FontWeight.Medium
        )
    }
    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

    if (list.isEmpty()) {
        UniversalStoreEmptyInfoText()
        return
    }

    Surface(
        shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp16),
        color = Color.White,
        border = BorderStroke(
            width = NdjcCommonTokens.Dp.Dp1,
            color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a10 * 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            list.forEachIndexed { idx, c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                onCopyAccountValue(c.name, c.value)
                            }
                        )
                        .padding(horizontal = NdjcCommonTokens.Dp.Dp16),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = c.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a60),
                        modifier = Modifier.width(NdjcCommonTokens.Dp.Dp96),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp10))

                    Text(
                        text = c.value,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = NdjcCommonTokens.Colors.C_FF111827,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }

                if (idx != list.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(horizontal = NdjcCommonTokens.Dp.Dp16),
                        color = NdjcCommonTokens.Colors.C_FF111827.copy(alpha = NdjcCommonTokens.Alpha.a10),
                        thickness = NdjcCommonTokens.Dp.Dp1
                    )
                }
            }
        }
    }
}
@Composable
private fun UniversalContactRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    onCopy: () -> Unit
) {
    val clickableMod = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableMod)
            .padding(vertical = NdjcCommonTokens.Dp.Dp6),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = NdjcCommonTokens.Colors.C_FF111827,
            modifier = Modifier.width(NdjcCommonTokens.Dp.Dp92)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) HomeTokens.ChipSelectedBackground else NdjcCommonTokens.Colors.C_FF374151,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(NdjcCommonTokens.Dp.Dp36)
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copy",
                tint = NdjcCommonTokens.Colors.C_FF6B7280
            )
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    minLines: Int = 1,
    isError: Boolean = false,
    errorText: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
    ) {
        NdjcTextField(
            value = value,
            onValueChange = { onChange(it) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = minLines == 1,
            minLines = minLines,
            label = label,
            isError = isError
        )

        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a80)
            )
        }
    }
}
@Composable
private fun StoreExtraContactsEditor(
    enabled: Boolean,
    items: List<ExtraContactDraft>,
    errorText: String?,

    newName: String,
    newValue: String,
    onNewNameChange: (String) -> Unit,
    onNewValueChange: (String) -> Unit,

    onNameChange: (Int, String) -> Unit,
    onValueChange: (Int, String) -> Unit,
    onAdd: (String, String) -> Unit,
    onRemove: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp0)
    ) {
        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red.copy(alpha = NdjcCommonTokens.Alpha.a75)
            )
            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))
        }

        Text(
            text = "Other contact methods",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

        // 已添加行（可编辑、可删除）
        items.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
            ) {
                NdjcTextField(
                    value = row.name,
                    onValueChange = { onNameChange(index, it) },
                    enabled = enabled,
                    modifier = Modifier.weight(0.38f),
                    label = "Name",
                    singleLine = true
                )
                NdjcTextField(
                    value = row.value,
                    onValueChange = { onValueChange(index, it) },
                    enabled = enabled,
                    modifier = Modifier.weight(0.62f),
                    label = "Value",
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Remove",
                    modifier = Modifier
                        .padding(top = NdjcCommonTokens.Dp.Dp4, bottom = NdjcCommonTokens.Dp.Dp8)
                        .clickable(enabled = enabled) { onRemove(index) },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                )
            }
        }

        // 永远只保留 1 行预留输入行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
        ) {
            NdjcTextField(
                value = newName,
                onValueChange = onNewNameChange,
                enabled = enabled,
                modifier = Modifier.weight(0.38f),
                label = "Name",
                singleLine = true
            )
            NdjcTextField(
                value = newValue,
                onValueChange = onNewValueChange,
                enabled = enabled,
                modifier = Modifier.weight(0.62f),
                label = "Value",
                singleLine = true
            )
        }

        // ✅ 输入框 → Add 的距离精确为 Dp10（不会再被父级 spacedBy 叠加）
        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))

            NdjcPrimaryActionButton(
                text = "Add",
                onClick = {
                    val n = newName.trim()
                    val v = newValue.trim()
                    onAdd(n, v)
                    onNewNameChange("")
                    onNewValueChange("")
                },
                // ✅ 没内容时灰色
                enabled = enabled && newName.isNotBlank() && newValue.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
@Composable
private fun StoreServicesEditor(
    enabled: Boolean,
    items: List<String>,
    errorText: String?,

    newValue: String,
    onNewValueChange: (String) -> Unit,

    onChange: (Int, String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp0)
    ) {
        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red.copy(alpha = NdjcCommonTokens.Alpha.a75)
            )
            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))
        }

        // 已添加项（每项一个输入框）
        items.forEachIndexed { index, value ->
            NdjcTextField(
                value = value,
                onValueChange = { onChange(index, it) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                label = "Service",
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Remove",
                    modifier = Modifier
                        .padding(top = NdjcCommonTokens.Dp.Dp4, bottom = NdjcCommonTokens.Dp.Dp8)
                        .clickable(enabled = enabled) { onRemove(index) },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                )
            }
        }

        // 永远预留一行空输入框
        NdjcTextField(
            value = newValue,
            onValueChange = onNewValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = "Add new service",
            singleLine = true
        )

        // ✅ 输入框 → Add 的距离精确为 Dp10（不会再被父级 spacedBy 叠加）
        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))

            NdjcPrimaryActionButton(
                text = "Add",
                onClick = {
                    onAdd(newValue)
                    onNewValueChange("")
                },
                enabled = enabled && newValue.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StoreExtraContactsEditorRow(
    enabled: Boolean,
    items: List<ExtraContactDraft>,
    validationError: String?,
    onNameChange: (String, String) -> Unit,
    onValueChange: (String, String) -> Unit,
    onAdd: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onSave: () -> Unit
) {
    var newName by rememberSaveable { mutableStateOf("") }
    var newValue by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    // 顶部错误（VM校验）+ 本地错误（底部行半填）
    val err = validationError ?: localError
    if (!err.isNullOrBlank()) {
        Text(
            text = err,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Red.copy(alpha = NdjcCommonTokens.Alpha.a75)
        )
        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))
    }

    Text(
        text = "Other contact methods",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

    // 已添加条目（两列，可编辑；右侧 Remove）
    items.forEach { it ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NdjcTextField(
                value = it.name,
                onValueChange = { v -> onNameChange(it.id, v) },
                enabled = enabled,
                modifier = Modifier.weight(0.38f),
                label = "Name",
                singleLine = true
            )
            NdjcTextField(
                value = it.value,
                onValueChange = { v -> onValueChange(it.id, v) },
                enabled = enabled,
                modifier = Modifier.weight(0.62f),
                label = "Value",
                singleLine = true
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Remove",
                modifier = Modifier
                    .padding(top = NdjcCommonTokens.Dp.Dp4, bottom = NdjcCommonTokens.Dp.Dp8)
                    .clickable(enabled = enabled) { onRemove(it.id) },
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
            )
        }
    }

    // 永远只保留 1 行空输入（Name短 + Value长）
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
    ) {
        NdjcTextField(
            value = newName,
            onValueChange = { newName = it; localError = null },
            enabled = enabled,
            modifier = Modifier.weight(0.38f),
            label = "Name",
            singleLine = true,
            fillContentWidth = false
        )
        NdjcTextField(
            value = newValue,
            onValueChange = { newValue = it; localError = null },
            enabled = enabled,
            modifier = Modifier.weight(0.62f),
            label = "Value",
            singleLine = true,
            fillContentWidth = false
        )

    }


    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

    NdjcPrimaryActionButton(
        text = "Add",
        onClick = {
            val n = newName.trim()
            val v = newValue.trim()
            if (n.isBlank() && v.isBlank()) return@NdjcPrimaryActionButton
            if (n.isBlank() || v.isBlank()) {
                localError = "Both Name and Value are required to add a contact."
                return@NdjcPrimaryActionButton
            }
            onAdd(n, v)
            newName = ""
            newValue = ""
            localError = null
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    )

    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

    // 拦截保存：底部空行半填时禁止保存
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        // 这里不画 Save 按钮本身（你的页面底部已有 Save）
        // 但我们需要在外层 Save onClick 前先校验底部行
        // 你要做的是：把外层 Save 的 onClick 改成调用这个检查逻辑（见下方“保存按钮改法”）
    }
}

private fun hasHalfFilled(name: String, value: String): Boolean {
    val n = name.trim()
    val v = value.trim()
    return (n.isBlank() && v.isNotBlank()) || (n.isNotBlank() && v.isBlank())
}

@Composable
private fun StoreEditSectionTitle(
    title: String,
    subtitle: String? = null
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
        fontWeight = FontWeight.SemiBold
    )
    if (!subtitle.isNullOrBlank()) {
        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp4))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
        )
    }
    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))
}
@Composable
private fun StoreOtherContactMethodsEditor(
    enabled: Boolean,
    whatsapp: String,
    wechat: String,
    instagram: String,
    facebook: String,
    onWhatsappChange: (String) -> Unit,
    onWechatChange: (String) -> Unit,
    onInstagramChange: (String) -> Unit,
    onFacebookChange: (String) -> Unit
) {
    // ✅ Step 1：仍然复用既有四个字段，只是 UI 呈现为“可添加列表”
    data class Method(
        val key: String,
        val label: String,
        val value: String,
        val onChange: (String) -> Unit
    )

    val methods = listOf(
        Method("whatsapp", "WhatsApp", whatsapp, onWhatsappChange),
        Method("wechat", "WeChat", wechat, onWechatChange),
        Method("instagram", "Instagram", instagram, onInstagramChange),
        Method("facebook", "Facebook", facebook, onFacebookChange)
    )

    // 初始启用：已有值的项默认显示；否则隐藏，等待“+ Add”启用
    val initiallyEnabled = methods.filter { it.value.trim().isNotBlank() }.map { it.key }

    var enabledKeys by rememberSaveable { mutableStateOf(initiallyEnabled) }
    var menuOpen by remember { mutableStateOf(false) }

    val enabledSet = enabledKeys.toSet()
    val availableToAdd = methods.filter { it.key !in enabledSet }

    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Other contact methods",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a85),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Box {
            NdjcPrimaryActionButton(
                text = "Add",
                onClick = { if (enabled) menuOpen = true },
                enabled = enabled && availableToAdd.isNotEmpty(),
                modifier = Modifier.widthIn(min = NdjcCommonTokens.Dp.Dp100)
            )

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                availableToAdd.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.label) },
                        onClick = {
                            menuOpen = false
                            enabledKeys = (enabledKeys + m.key)
                        }
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

    // 渲染已启用项（仍使用原来的 ProfileField + onChange，不引入新逻辑）
    methods.forEach { m ->
        if (m.key in enabledSet) {
            ProfileField(
                m.label,
                m.value,
                m.onChange,
                enabled = enabled
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = NdjcCommonTokens.Dp.Dp2, bottom = NdjcCommonTokens.Dp.Dp6),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55),
                    modifier = Modifier.clickable(enabled = enabled) {
                        // UI 触发清空：仍然走既有 onChange（不越界）
                        m.onChange("")
                        enabledKeys = enabledKeys.filterNot { it == m.key }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileReadOnlyRowIfNotBlank(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val v = value.trim()
    if (v.isBlank()) return
    ProfileReadOnlyRow(label = label, value = v, modifier = modifier)
}

@Composable
private fun ProfileReadOnlyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = NdjcCommonTokens.Colors.C_FF6B7280 // 灰
        )
        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp2))
        Text(
            text = if (value.isBlank()) "-" else value,
            style = MaterialTheme.typography.bodyMedium,
            color = NdjcCommonTokens.Colors.C_FF111827 // 深色
        )
    }
}

@Composable
internal fun ShowcaseAdmin(
    uiState: ShowcaseAdminUiState,
    actions: ShowcaseAdminActions,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() }
        )
    ) {

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    bottom = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    top = NdjcTopContentPadding
                )
                .navigationBarsPadding()
        ) {


            NdjcWhiteCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                ) {

                    // 标题
                    Text(
                        text = "Admin",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    if (uiState.syncNoticeLabel.isNotBlank()) {
                        Text(
                            text = uiState.syncNoticeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black
                        )
                    }

                    uiState.cloudStatus?.let { cloud ->
                        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                        ) {
                            Text(
                                text = "Cloud",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )

                            Text(
                                text = "${cloud.planLabel} · ${cloud.storeId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475467)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                            ) {
                                Text(
                                    text = cloud.statusLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF344054)
                                )

                                if (cloud.daysRemainingLabel.isNotBlank()) {
                                    Text(
                                        text = cloud.daysRemainingLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF101828)
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                            ) {
                                if (cloud.serviceEndAtLabel.isNotBlank()) {
                                    Text(
                                        text = "Expires · ${cloud.serviceEndAtLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF667085)
                                    )
                                }

                                if (cloud.deleteAtLabel.isNotBlank()) {
                                    Text(
                                        text = "Deletes · ${cloud.deleteAtLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF667085)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))
                        HorizontalDivider()
                    }

                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    // 主次：Quick action（把最常用动作单独放一组）
                    Text(
                        text = "Quick action",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black
                    )

                    NdjcPrimaryActionButton(
                        text = "Add Item",
                        onClick = actions.onAddNewDish,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))
                    HorizontalDivider()
                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    // 分组：Catalog
                    Text(
                        text = "Catalog",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black
                    )

                    NdjcPrimaryActionButton(
                        text = "Items",
                        onClick = actions.onOpenItemsManager,
                        modifier = Modifier.fillMaxWidth()
                    )

                    NdjcPrimaryActionButton(
                        text = "Categories",
                        onClick = actions.onOpenCategoriesManager,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))
                    HorizontalDivider()
                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    // 分组：Store
                    Text(
                        text = "Store",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black
                    )

                    NdjcPrimaryActionButton(
                        text = "Store settings",
                        onClick = actions.onOpenStoreProfile,
                        modifier = Modifier.fillMaxWidth()
                    )

                    NdjcPrimaryActionButton(
                        text = "Messages",
                        onClick = actions.onOpenMerchantChatList,
                        modifier = Modifier.fillMaxWidth()
                    )

                    NdjcPrimaryActionButton(
                        text = "Announcements",
                        onClick = actions.onOpenAnnouncementPublisher,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))
                    HorizontalDivider()
                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))

                    // 分组：Account
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black
                    )

                    NdjcPrimaryActionButton(
                        text = "Password",
                        onClick = actions.onOpenChangePassword,
                        modifier = Modifier.fillMaxWidth()
                    )

                    NdjcPrimaryActionButton(
                        text = "Sign out",
                        onClick = actions.onLogout,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 状态展示（可选，纯渲染，不是逻辑）
            if (!uiState.statusMessage.isNullOrBlank()) {
                Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp14))
                Text(
                    text = uiState.statusMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ShowcaseAdminCategories(
    uiState: ShowcaseAdminUiState,
    actions: ShowcaseAdminActions,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    // 删除确认弹窗（VM 决策是否可删：pendingDeleteCategory / cannotDeleteCategory）
    uiState.pendingDeleteCategory?.let { cat ->
        NdjcBaseDialog(
            onDismissRequest = actions.onDismissCategoryDeleteDialogs,
            title = "Delete category?",
            message = "This will delete \"$cat\".",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirmClick = actions.onConfirmPendingDeleteCategory,
            onDismissClick = actions.onDismissCategoryDeleteDialogs,
            destructiveConfirm = true
        )
    }

    uiState.cannotDeleteCategory?.let { cat ->
        NdjcBaseDialog(
            onDismissRequest = actions.onDismissCategoryDeleteDialogs,
            title = "Cannot delete",
            message = "Category \"$cat\" still has items. Remove/move those items first.",
            confirmText = "OK",
            onConfirmClick = actions.onDismissCategoryDeleteDialogs
        )
    }

    // UI 输入态（允许 UI 持有输入文本；不做业务校验/规则）
    var addText by rememberSaveable { mutableStateOf("") }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var renameFrom by rememberSaveable { mutableStateOf<String?>(null) }
    var renameTo by rememberSaveable { mutableStateOf("") }
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }

    if (showRenameDialog && renameFrom != null) {
        NdjcBaseDialog(
            onDismissRequest = {
                showRenameDialog = false
                renameFrom = null
                renameTo = ""
            },
            title = "Edit category",
            confirmText = "Save",
            dismissText = "Cancel",
            onConfirmClick = {
                val oldName = renameFrom
                if (oldName != null) {
                    actions.onRenameCategory(oldName, renameTo)
                }
                showRenameDialog = false
                renameFrom = null
                renameTo = ""
            },
            onDismissClick = {
                showRenameDialog = false
                renameFrom = null
                renameTo = ""
            },
            confirmEnabled = renameTo.isNotBlank(),
            textContent = {
                NdjcTextField(
                    value = renameTo,
                    onValueChange = { renameTo = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = "Category name"
                )
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() }
        )
    ) {

        // ✅ Back 按钮移到白卡外：左上角固定（和 Items 页同款方式）
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // ✅ 长内容滚动
                    .padding(
                        start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        bottom = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        top = NdjcTopContentPadding
                    )
            ) {
                NdjcWhiteCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp) // ✅ 取消叠加空隙，改用显式 Spacer 控节奏（对齐 Store Profile）
                    ) {
                        // ===== Store Profile 同款节奏（不 tokens）=====
                        val sectionGap = 14.dp          // Section -> Section
                        val titleToHint = 4.dp          // Section Title -> Hint
                        val hintToContent = 10.dp       // Hint -> Content
                        val rowGap = 10.dp              // Row spacing

                        // ✅ 标题（Back 已在白卡外，这里只保留标题本身）
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (uiState.isLoading) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(Modifier.height(sectionGap))

                        // =========================
                        // Create category（新增区）
                        // =========================
                        Text(
                            text = "Create category",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(titleToHint))

                        Text(
                            text = "Category names must be unique.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        Spacer(Modifier.height(hintToContent))

                        NdjcTextField(
                            value = addText,
                            onValueChange = { addText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = "Category name"
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(rowGap)
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            NdjcPrimaryActionButton(
                                text = "Add",
                                onClick = {
                                    actions.onAddCategory(addText)
                                    addText = ""
                                },
                                enabled = addText.trim().isNotEmpty(),
                                modifier = Modifier.weight(1f) // ✅ 让 Add 宽度 = 半行宽（对齐 Edit/Delete）
                            )
                        }

                        Spacer(Modifier.height(sectionGap))

                        // =========================
                        // Manage categories（管理区）
                        // =========================
                        Text(
                            text = "Edit or delete categories",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(titleToHint))

                        val selectedName = selectedCategoryName
                        Text(
                            text = if (selectedName.isNullOrBlank()) {
                                "Select a category below to edit or delete."
                            } else {
                                "Selected: $selectedName"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        Spacer(Modifier.height(hintToContent))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(rowGap)
                        ) {
                            NdjcPrimaryActionButton(
                                text = "Edit",
                                onClick = {
                                    val name = selectedCategoryName ?: return@NdjcPrimaryActionButton
                                    renameFrom = name
                                    renameTo = name
                                    showRenameDialog = true
                                },
                                enabled = selectedCategoryName != null,
                                modifier = Modifier.weight(1f)
                            )

                            NdjcPrimaryActionButton(
                                text = "Delete",
                                onClick = {
                                    val name = selectedCategoryName ?: return@NdjcPrimaryActionButton
                                    actions.onRequestDeleteCategory(name)
                                },
                                enabled = selectedCategoryName != null,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(sectionGap))

                        // =========================
                        // All categories
                        // =========================
                        Text(
                            text = "All categories",
                            style = MaterialTheme.typography.titleMedium, // ✅ 对齐 Store Profile 的 Section Title 层级
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(titleToHint))

                        Text(
                            text = "Tap a category to select.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        Spacer(Modifier.height(hintToContent))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(rowGap),
                            verticalArrangement = Arrangement.spacedBy(rowGap)
                        ) {
                            uiState.manualCategories.forEach { name ->
                                val selected = (selectedCategoryName == name)

                                NdjcPillButton(
                                    text = name,
                                    selected = selected,
                                    onClick = { selectedCategoryName = name }
                                )
                            }
                        }
                    }
                }
            }

            NdjcSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = NdjcSnackbarTokens.HorizontalPadding,
                        end = NdjcSnackbarTokens.HorizontalPadding,
                        bottom = NdjcSnackbarTokens.BottomPadding
                    )
            )
        }
    }
}
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
internal fun ShowcaseAdminItems(
    uiState: ShowcaseAdminUiState,
    actions: ShowcaseAdminActions,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }

    // ✅ Items 页：Filter 抽屉开关（保持你原逻辑）
    var showItemsFilterSheet by rememberSaveable { mutableStateOf(false) }

    val selectedIds = uiState.selectedDishIds
    val selectedCount = selectedIds.size
    val singleSelectedDishTitle = if (selectedCount == 1) {
        val selectedId = selectedIds.first()
        uiState.dishes.firstOrNull { it.id == selectedId }?.title.orEmpty()
    } else {
        ""
    }

    if (showDeleteSelectedConfirm && selectedCount > 0) {
        NdjcBaseDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = if (selectedCount == 1) {
                "Delete selected item?"
            } else {
                "Delete selected items?"
            },
            message = if (selectedCount == 1) {
                "This will permanently remove \"$singleSelectedDishTitle\"."
            } else {
                "This will permanently remove $selectedCount selected items."
            },
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirmClick = {
                showDeleteSelectedConfirm = false
                actions.onDeleteSelectedDishes()
            },
            onDismissClick = { showDeleteSelectedConfirm = false },
            destructiveConfirm = true
        )
    }

    // ✅ Sort 选中态（保持你原逻辑）
    val defaultSelected = uiState.itemsSortMode == ShowcaseHomeSortMode.Default
    val lowHighSelected = uiState.itemsSortMode == ShowcaseHomeSortMode.PriceAsc
    val highLowSelected = uiState.itemsSortMode == ShowcaseHomeSortMode.PriceDesc

    // ✅ Filter 激活态：任一过滤条件 or 价格区间生效（保持你原逻辑）
    val hasPriceFilter = (uiState.appliedMinPrice != null || uiState.appliedMaxPrice != null)
    val filterActive =
        uiState.filterRecommended ||
                uiState.filterHiddenOnly ||
                uiState.filterDiscountOnly ||
                hasPriceFilter

    val selectedColor = BackButtonTokens.Background

    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() }
        )
    ) {

        // 顶部 Back 按钮：悬浮固定
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    horizontal = EditDishTokens.BackButtonHorizontalPadding,
                    vertical = EditDishTokens.BackButtonVerticalPadding
                )
                .zIndex(1f)
        ) {
                    }

        // ✅ 改成：上半固定白卡 + 下半列表可滑动
// ✅ 改成：列表在底层滚动，顶部白卡覆盖（卡片上滑会被白卡遮住）
        var headerHeightPx by remember { mutableStateOf(0) }
        val density = LocalDensity.current
        val headerHeightDp = with(density) { headerHeightPx.toDp() }
        val headerTopPadding = NdjcTopContentPadding
        val listTopPadding = headerTopPadding + headerHeightDp + NdjcCommonTokens.Dp.Dp10

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // =========================
            // 底层：商品列表（可上下滑动），并且允许滑到白卡背后
            // =========================
            val list = uiState.dishes

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
                contentPadding = PaddingValues(
                    top = listTopPadding,
                    start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    bottom = NdjcWhiteCardLayoutTokens.CardInnerPaddingVertical
                )
            ) {
                if (list.isEmpty()) {
                    item(key = "empty_state") {
                        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp24))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                            ) {
                                Text(
                                    text = "No items yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Add your first product from Admin → Add Item.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = list,
                        key = { it.id }
                    ) { dishItem ->
                        val checked = selectedIds.contains(dishItem.id)

                        val itemInteractionSource = remember { MutableInteractionSource() }
                        val itemPressed by itemInteractionSource.collectIsPressedAsState()
                        val itemScale by animateFloatAsState(
                            targetValue = if (itemPressed) 0.965f else 1f,
                            animationSpec = tween(durationMillis = 120),
                            label = "AdminItemsCardScale"
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                },
                            shape = NdjcCatalogItemCardTokens.Shape,
                            color = Color.Transparent,
                            shadowElevation = NdjcCommonTokens.Dp.Dp0,
                            tonalElevation = NdjcCommonTokens.Dp.Dp0
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(NdjcCatalogItemCardTokens.Shape)
                                    .clickable(
                                        interactionSource = itemInteractionSource,
                                        indication = ripple(bounded = true)
                                    ) {
                                        actions.onEditDish(dishItem.id)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
                                ) {
                                    NdjcCatalogItemCard(
                                        title = dishItem.title,
                                        metaText = "${dishItem.clickCount} views",
                                        imageUrl = dishItem.imagePreviewUrl,
                                        priceText = ndjcMoneyTrim2(dishItem.price),
                                        originalPriceText = ndjcMoneyTrim2(dishItem.originalPrice),
                                        discountPriceText = dishItem.discountPrice?.let { ndjcMoneyTrim2(it) },
                                        categoryText = dishItem.category,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { actions.onEditDish(dishItem.id) },
                                        trailingContent = {
                                            Box(
                                                modifier = Modifier.fillMaxHeight(),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                NdjcSelectionCheckbox(
                                                    checked = checked,
                                                    onCheckedChange = { actions.onToggleSelectDish(dishItem.id) }
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================
            // 顶层：固定白卡（输入框 + 按钮 + Sort/Filter + 分类），覆盖在列表上面
            // =========================
            NdjcWhiteCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = headerTopPadding,
                        start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        bottom = NdjcCommonTokens.Dp.Dp10
                    )
                    .onSizeChanged { headerHeightPx = it.height }
                    .zIndex(2f)
            ) {
                val selectedIds = uiState.selectedDishIds

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                ) {

                    val itemsCount = uiState.dishes.size

                    Column(
                        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                    ) {
                        Text(
                            text = "Items",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Text(
                            text = "Manage your product catalog and visibility.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                        )
                        Text(
                            text = "$itemsCount items • Loaded locally",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                        )
                    }

                    if (uiState.isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    NdjcTextField(
                        value = uiState.itemsSearchQuery,
                        onValueChange = { actions.onItemsSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = "Search items",
                        trailingIcon = {
                            if (uiState.itemsSearchQuery.isNotBlank()) {
                                IconButton(onClick = { actions.onClearItemsSearchQuery() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        }
                    )

// ✅ 先排序/筛选
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SortNavEqualItem(
                            text = "Default",
                            selected = defaultSelected,
                            selectedColor = selectedColor,
                            onClick = { actions.onItemsSortModeChange(ShowcaseHomeSortMode.Default) },
                            modifier = Modifier.weight(1f)
                        )

                        SortNavEqualItem(
                            text = "Low–High",
                            selected = lowHighSelected,
                            selectedColor = selectedColor,
                            onClick = { actions.onItemsSortModeChange(ShowcaseHomeSortMode.PriceAsc) },
                            modifier = Modifier.weight(1f)
                        )

                        SortNavEqualItem(
                            text = "High–Low",
                            selected = highLowSelected,
                            selectedColor = selectedColor,
                            onClick = { actions.onItemsSortModeChange(ShowcaseHomeSortMode.PriceDesc) },
                            modifier = Modifier.weight(1f)
                        )

                        SortNavEqualItem(
                            text = "Filter",
                            selected = filterActive,
                            selectedColor = selectedColor,
                            onClick = { showItemsFilterSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

// ✅ 再分类
                    CategoryChipsRow(
                        selectedCategory = uiState.selectedCategory,
                        manualCategories = uiState.manualCategories,
                        onCategorySelected = { actions.onSelectCategory(it) },
                        modifier = Modifier.fillMaxWidth(),
                        showAllChip = true,
                        useOuterHorizontalPadding = false
                    )

// ✅ 再批量操作（增加一行提示，不改外观 token，只是 Text）
                    Text(
                        text = "Select items to delete or clear selection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                    ) {
                        NdjcPrimaryActionButton(
                            text = "Clear",
                            onClick = actions.onClearSelectedDishes,
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(1f)
                        )

                        NdjcPrimaryActionButton(
                            text = "Delete ($selectedCount)",
                            onClick = { showDeleteSelectedConfirm = true },
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        }

    // ✅ Filter 抽屉：直接复用 Home / Favorites 已可输入的通用底部抽屉
    if (showItemsFilterSheet) {
        NdjcFilterBottomSheet(
            onDismissRequest = {
                actions.onApplyPriceRange()
                showItemsFilterSheet = false
            },
            onClear = {
                actions.onItemsSortModeChange(ShowcaseHomeSortMode.Default)
                actions.onItemsFilterRecommendedChange(false)
                actions.onItemsFilterHiddenOnlyChange(false)
                actions.onItemsFilterDiscountOnlyChange(false)
                actions.onClearPriceRange()
                showItemsFilterSheet = false
            },
            priceMinDraft = uiState.priceMinDraft,
            onPriceMinDraftChange = actions.onPriceMinDraftChange,
            priceMaxDraft = uiState.priceMaxDraft,
            onPriceMaxDraftChange = actions.onPriceMaxDraftChange,
            onApply = {
                actions.onApplyPriceRange()
                showItemsFilterSheet = false
            }
        ) {
            NdjcToggleRow(
                label = "Pick",
                checked = uiState.filterRecommended,
                onCheckedChange = actions.onItemsFilterRecommendedChange
            )

            NdjcToggleRow(
                label = "Hidden",
                checked = uiState.filterHiddenOnly,
                onCheckedChange = actions.onItemsFilterHiddenOnlyChange
            )

            NdjcToggleRow(
                label = "Discount",
                checked = uiState.filterDiscountOnly,
                onCheckedChange = actions.onItemsFilterDiscountOnlyChange
            )
        }
    }
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

//endregion

//region 10 Previews
// ---------- 预览 ----------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ShowcaseChatThread(
    uiState: com.ndjc.feature.showcase.ShowcaseChatUiState,
    actions: com.ndjc.feature.showcase.ShowcaseChatActions,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    android.util.Log.e(
        "ChatTrace",
        "RENDER_SNAP conv=${uiState.conversationId} size=${uiState.messages.size} unread=${uiState.unreadCount}"
    )


    val rf = uiState.messages.firstOrNull()
    val rl = uiState.messages.lastOrNull()
    val rLast5 = uiState.messages.takeLast(5).joinToString(" | ") { m ->
        val id6 = (m.id ?: "").takeLast(6)
        "$id6@${m.timeText}"
    }
    android.util.Log.e(
        "ChatTrace",
        "RENDER_RANGE size=${uiState.messages.size} " +
                "first=${(rf?.id ?: "").takeLast(6)}/${rf?.timeText} " +
                "last=${(rl?.id ?: "").takeLast(6)}/${rl?.timeText} " +
                "last5=$rLast5"
    )

    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val uiScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val imeBottom = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottom > 0

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var menuMsgId by remember { mutableStateOf<String?>(null) }
    var draftHasFocus by remember { mutableStateOf(false) }

    val lockChatListAtBottom = isImeVisible && draftHasFocus

    var plusMenuExpanded by remember { mutableStateOf(false) }

    var footerTopInRootPx by remember { mutableStateOf<Float?>(null) }
    var lastMessageBottomInRootPx by remember { mutableStateOf<Float?>(null) }

    var draftVisibleLineCount by remember { mutableStateOf(1) }
    var lastHandledImeVisible by remember { mutableStateOf(isImeVisible) }
    var lastHandledDraftVisibleLineCount by remember { mutableStateOf(1) }
    var chatAlignSignal by remember { mutableStateOf(0L) }

    val chatFooterReservedBottom = NdjcCommonTokens.Dp.Dp20
    val chatBubbleToDividerGapPx = with(density) { NdjcCommonTokens.Dp.Dp36.toPx() }

    suspend fun alignLastBubbleToFooterLine(animated: Boolean) {
        val lastIndex = uiState.messages.lastIndex
        if (lastIndex < 0) return

        runCatching { listState.scrollToItem(lastIndex) }

        delay(16)

        val footerTop = footerTopInRootPx ?: return
        val lastBottom = lastMessageBottomInRootPx ?: return
        val targetBottom = footerTop - chatBubbleToDividerGapPx
        val delta = lastBottom - targetBottom

        if (abs(delta) <= 1f) return

        if (animated) {
            runCatching {
                listState.animateScrollBy(
                    value = delta,
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = LinearOutSlowInEasing
                    )
                )
            }

            delay(16)

            val footerTopAfter = footerTopInRootPx ?: return
            val lastBottomAfter = lastMessageBottomInRootPx ?: return
            val targetBottomAfter = footerTopAfter - chatBubbleToDividerGapPx
            val remainDelta = lastBottomAfter - targetBottomAfter

            if (abs(remainDelta) > 1f) {
                runCatching { listState.scrollBy(remainDelta) }
            }
        } else {
            runCatching { listState.scrollBy(delta) }
        }
    }

    LaunchedEffect(chatAlignSignal) {
        if (chatAlignSignal == 0L) return@LaunchedEffect
        if (!draftHasFocus && !isImeVisible) return@LaunchedEffect

        if (isImeVisible) {
            delay(72)
            alignLastBubbleToFooterLine(animated = true)
        } else {
            delay(16)
            alignLastBubbleToFooterLine(animated = false)
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible != lastHandledImeVisible) {
            lastHandledImeVisible = isImeVisible
            chatAlignSignal = System.currentTimeMillis()
        }
    }

    LaunchedEffect(draftVisibleLineCount, draftHasFocus) {
        if (!draftHasFocus) return@LaunchedEffect
        if (draftVisibleLineCount != lastHandledDraftVisibleLineCount) {
            lastHandledDraftVisibleLineCount = draftVisibleLineCount
            chatAlignSignal = System.currentTimeMillis()
        }
    }

    // ✅ Chat 图片全屏预览（纯 UI 状态，不走导航，不越界）
    data class NdjcChatImagePreviewState(
        val images: List<String>,
        val startIndex: Int
    )

    var chatImagePreview by remember { mutableStateOf<NdjcChatImagePreviewState?>(null) }
    var chatPreviewPendingSaveUrl by remember { mutableStateOf<String?>(null) }
// ✅ 记录“定位滚动”是否已被 UI 消费（纯 UI 状态，不越界）
    var consumedScrollToMessageSignal by remember { mutableStateOf(0L) }
// ✅ 刚刚完成定位后的保护窗口：避免“自动滚底”立即覆盖定位
    var lastJumpAtMs by remember { mutableStateOf(0L) }

// ✅ UI-only：从“搜索结果跳转”定位到某条消息时，让目标气泡“整体闪烁”
    var jumpFlashMessageId by remember { mutableStateOf<String?>(null) }
    var jumpFlashSignal by remember { mutableStateOf(0L) }
    val jumpFlash = remember { Animatable(0f) }

// ✅ 闪烁动画：3 次（亮→暗）
    LaunchedEffect(jumpFlashSignal) {
        if (jumpFlashSignal <= 0L) return@LaunchedEffect

        jumpFlash.snapTo(0f)

        repeat(3) {
            jumpFlash.animateTo(1f, animationSpec = tween(durationMillis = 90))
            jumpFlash.animateTo(0f, animationSpec = tween(durationMillis = 180))
        }

        jumpFlashMessageId = null
    }

    /**
     * ✅ 新增：消费逻辑模块发出的“定位到某条消息”的信号
     * - 来自 chatDomain.jumpToMessage(messageId)
     * - UI 只负责滚动，不负责决定滚动策略（不越界）
     */
    LaunchedEffect(uiState.scrollToMessageSignal, uiState.scrollToMessageId, uiState.messages.size) {
        val signal = uiState.scrollToMessageSignal
        val targetId = uiState.scrollToMessageId

        if (signal <= 0L) return@LaunchedEffect
        if (signal == consumedScrollToMessageSignal) return@LaunchedEffect
        if (targetId.isNullOrBlank()) return@LaunchedEffect

        val idx = uiState.messages.indexOfFirst { it.id == targetId }
        if (idx >= 0) {
            // ✅ 布局稳定后再滚动（避免被 cancel）
            delay(30)
            runCatching { listState.animateScrollToItem(idx) }

            consumedScrollToMessageSignal = signal
            lastJumpAtMs = System.currentTimeMillis()

// ✅ UI-only：定位完成后，触发目标气泡“整体闪烁”
            jumpFlashMessageId = targetId
            jumpFlashSignal = signal
        }else {
            // idx 还没出现：等消息列表更新（messages.size 变化）后本 LaunchedEffect 会再跑一次
            android.util.Log.e("ChatTrace", "SCROLL_TO_MSG pending id=${targetId.takeLast(6)} size=${uiState.messages.size}")
        }
    }

    /**
     * ✅ 修改：自动滚到底
     * - 如果当前有“定位消息”的 pending（signal 未消费），不要滚到底覆盖它
     * - 如果刚刚完成定位（保护窗口内），也不要立刻滚到底
     */
    LaunchedEffect(uiState.messages.lastOrNull()?.id, uiState.scrollToBottomSignal) {
        val lastIndex = uiState.messages.lastIndex
        if (lastIndex < 0) return@LaunchedEffect

        val hasPendingJump =
            !uiState.scrollToMessageId.isNullOrBlank() &&
                    uiState.scrollToMessageSignal > 0L &&
                    uiState.scrollToMessageSignal != consumedScrollToMessageSignal

        if (hasPendingJump) {
            android.util.Log.e("ChatTrace", "AUTO_BOTTOM skipped: pending jump")
            return@LaunchedEffect
        }

        val now = System.currentTimeMillis()
        if (lastJumpAtMs > 0L && now - lastJumpAtMs < 900L) {
            android.util.Log.e("ChatTrace", "AUTO_BOTTOM skipped: just jumped ${(now - lastJumpAtMs)}ms ago")
            return@LaunchedEffect
        }

        // ✅ 布局稳定后再滚动，避免 animateScroll 被 cancel
        delay(30)
        runCatching { listState.scrollToItem(lastIndex) }

        // ✅ 兜底第二次（部分机型/状态第一次 scroll 发生在测量前）
        delay(30)
        runCatching { listState.scrollToItem(lastIndex) }

        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == lastIndex }
        android.util.Log.e("ChatTrace", "VISIBLE_CHECK lastIndex=$lastIndex visible=$visible")
    }

    /**
     * ✅ 保留：会话内 find（原有逻辑）
     */
    LaunchedEffect(uiState.findScrollSignal, uiState.findFocusedId, uiState.messages.size) {
        val id = uiState.findFocusedId ?: return@LaunchedEffect
        val idx = uiState.messages.indexOfFirst { it.id == id }
        if (idx >= 0) {
            listState.animateScrollToItem(idx)
            lastJumpAtMs = System.currentTimeMillis()
        }
    }

// ✅ 独立页：查找聊天记录结果
    if (uiState.isSearchResults) {
        ShowcaseChatSearchResults(
            uiState = uiState,
            actions = actions,
            modifier = modifier
        )
        return
    }

    val msgs = uiState.messages

    val allThreadImages = msgs
        .flatMap { msg -> parseNdjcChatPayloadUi(msg.text).images }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    NdjcUnifiedBackground(
        modifier = modifier,
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() }
        )
    ) {
        NdjcConversationPageScaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            showTopBar = true,
            title = uiState.title,
            subtitle = uiState.subtitle,
            isSelectionMode = uiState.isSelectionMode,
            selectedCount = uiState.selectedIds.size,
            listState = listState,
            listUserScrollEnabled = !lockChatListAtBottom,
            contentPadding = PaddingValues(
                top = NdjcCommonTokens.Dp.Dp12,
                bottom = chatFooterReservedBottom
            ),
            verticalItemSpacing = NdjcCommonTokens.Dp.Dp8,
            topBarActions = {
                if (uiState.isSelectionMode) {
                    IconButton(onClick = { actions.onDeleteSelected() }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Selected")
                    }
                    IconButton(onClick = { actions.onExitSelection() }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Exit Selection")
                    }
                }
            },
            footer = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .onGloballyPositioned { coordinates ->
                            footerTopInRootPx = coordinates.positionInRoot().y
                        }
                        .drawBehind {
                            val strokeWidth = NdjcCommonTokens.Dp.Dp1.toPx()
                            drawLine(
                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = strokeWidth
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = NdjcCommonTokens.Dp.Dp12,
                                end = NdjcCommonTokens.Dp.Dp12,
                                top = NdjcCommonTokens.Dp.Dp8,
                                bottom = NdjcCommonTokens.Dp.Dp6
                            )
                    ) {

                    // ✅ 多选模式：输入框位置改为“横栏 + 居中垃圾桶”
                    if (uiState.isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(NdjcCommonTokens.Dp.Dp52),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { actions.onDeleteSelected() },
                                modifier = Modifier
                                    .size(NdjcCommonTokens.Dp.Dp40)
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(NdjcCommonTokens.Dp.Dp18)
                                )
                            }
                        }

                        return@Column
                    }
// ✅ 引用条（未发送状态）：优先显示“商品卡片引用条”（截图2），否则显示文本引用条（截图3）
                    val quoteProduct = uiState.quoteProduct
                    val quoteText = uiState.quotePreviewText.takeIf { it.isNotBlank() } ?: ""

                    if (quoteProduct != null) {
                        // ✅ 商品卡片引用条：无“发送商品”按钮，只保留 X
                        val uiProduct = NdjcProductCardUi(
                            id = quoteProduct.dishId,
                            title = quoteProduct.title,
                            priceText = quoteProduct.price,
                            imageUrl = quoteProduct.imageUrl
                        )
                        NdjcQuotedProductBar(
                            product = uiProduct,
                            onDismiss = { actions.onCancelQuote() },
                            onClick = { actions.onOpenProductDetail(uiProduct.id) }
                        )
                        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))
                    } else if (quoteText.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = NdjcCommonTokens.Dp.Dp10)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp12)
                                )
                                .border(
                                    width = NdjcCommonTokens.Dp.Dp1,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = NdjcCommonTokens.Alpha.a18),
                                    shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp12)
                                )
                                .padding(
                                    start = NdjcCommonTokens.Dp.Dp10,
                                    end = NdjcCommonTokens.Dp.Dp6,
                                    top = NdjcCommonTokens.Dp.Dp8,
                                    bottom = NdjcCommonTokens.Dp.Dp8
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .width(NdjcCommonTokens.Dp.Dp3)
                                    .height(NdjcCommonTokens.Dp.Dp24)
                                    .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp999))
                                    .background(BackButtonTokens.Background)
                            )

                            Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp2)
                            ) {
                                Text(
                                    text = "Replying to",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                                )

                                Text(
                                    text = quoteText,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { actions.onCancelQuote() },
                                modifier = Modifier.size(NdjcCommonTokens.Dp.Dp32)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Cancel reply",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = NdjcCommonTokens.Alpha.a60
                                    )
                                )
                            }
                        }
                    }
// ✅ 草稿图片缩略图条（只画 UI，不做逻辑）
// ✅ 放大预览尺寸 + 点击可全屏查看 + 复用现有全屏查看器（支持左右滑动）
                        if (uiState.draftImageUris.isNotEmpty()) {
                            val previewImages = uiState.draftImageUris
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()
                                .take(9)

                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = NdjcCommonTokens.Dp.Dp10),
                                horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                            ) {
                                itemsIndexed(previewImages) { index, uriString ->
                                    Box(
                                        modifier = Modifier
                                            .size(ChatTokens.DraftPreviewSize)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(ChatTokens.DraftPreviewCorner))
                                                .background(Color.White.copy(alpha = NdjcCommonTokens.Alpha.a92))
                                                .border(
                                                    width = NdjcCommonTokens.Dp.Dp1,
                                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10),
                                                    shape = RoundedCornerShape(ChatTokens.DraftPreviewCorner)
                                                )
                                                .clickable {
                                                    chatImagePreview = NdjcChatImagePreviewState(
                                                        images = previewImages,
                                                        startIndex = index
                                                    )
                                                }
                                        ) {
                                            NdjcShimmerImage(
                                                imageUrl = uriString,
                                                contentDescription = null,
                                                modifier = Modifier.matchParentSize(),
                                                placeholderCornerRadius = ChatTokens.DraftPreviewCorner,
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(
                                                    top = NdjcCommonTokens.Dp.Dp4,
                                                    end = NdjcCommonTokens.Dp.Dp4
                                                )
                                                .size(ChatTokens.DraftPreviewRemoveButtonSize)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55))
                                                .clickable {
                                                    actions.onRemoveDraftImage(uriString)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove draft image",
                                                tint = Color.White,
                                                modifier = Modifier.size(NdjcCommonTokens.Dp.Dp14)
                                            )
                                        }

                                        if (previewImages.size > 1) {
                                            Text(
                                                text = "${index + 1}/${previewImages.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(
                                                        end = NdjcCommonTokens.Dp.Dp4,
                                                        bottom = NdjcCommonTokens.Dp.Dp4
                                                    )
                                                    .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp8))
                                                    .background(Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a45))
                                                    .padding(
                                                        horizontal = NdjcCommonTokens.Dp.Dp6,
                                                        vertical = NdjcCommonTokens.Dp.Dp2
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
// ✅ 顶部商品卡片条（只画 UI，不做逻辑）
                    uiState.pendingProduct?.let { product ->
                        val uiProduct = NdjcProductCardUi(
                            id = product.dishId,
                            title = product.title,
                            priceText = product.price,
                            imageUrl = product.imageUrl
                        )

                        NdjcPendingProductBar(
                            product = uiProduct,
                            onDismiss = { actions.onClearPendingProduct() },
                            onSend = { actions.onSendPendingProduct() }
                        )
                        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))
                    }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // ✅ 1) 附件按钮：触控区与 Send 同高，视觉更成套
                            IconButton(
                                onClick = { plusMenuExpanded = true },
                                modifier = Modifier
                                    .size(ChatTokens.SendHeight)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AttachFile,
                                    contentDescription = "Attachments",
                                    tint = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                                )
                            }

                            // ✅ 统一节奏：📎 到输入框 8dp
                            Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))

                            DropdownMenu(
                                expanded = plusMenuExpanded,
                                onDismissRequest = { plusMenuExpanded = false }
                            ) {
                                if (uiState.canTogglePinned) {
                                    DropdownMenuItem(
                                        text = { Text(if (uiState.isPinned) "Unpin chat" else "Pin chat") },
                                        onClick = {
                                            plusMenuExpanded = false
                                            actions.onTogglePinned()
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("View photos") },
                                    onClick = {
                                        plusMenuExpanded = false
                                        actions.onOpenMediaGallery()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Search messages") },
                                    onClick = {
                                        plusMenuExpanded = false
                                        actions.onOpenSearchResults()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Photo library") },
                                    onClick = {
                                        plusMenuExpanded = false
                                        actions.onPickImages()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Camera") },
                                    onClick = {
                                        plusMenuExpanded = false
                                        actions.onOpenCamera()
                                    }
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(
                                        min = ChatTokens.InputMinHeight,
                                        max = ChatTokens.InputMaxHeight
                                    )
                                    .animateContentSize()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { state ->
                                        draftHasFocus = state.isFocused
                                        if (state.isFocused) {
                                            lastHandledDraftVisibleLineCount = draftVisibleLineCount
                                            chatAlignSignal = System.currentTimeMillis()
                                        }
                                    },
                                shape = RoundedCornerShape(ChatTokens.InputCorner),
                                color = Color.White.copy(alpha = NdjcCommonTokens.Alpha.a85),
                                tonalElevation = NdjcCommonTokens.Dp.Dp0,
                                shadowElevation = NdjcCommonTokens.Dp.Dp0,
                                border = BorderStroke(
                                    width = NdjcCommonTokens.Dp.Dp1,
                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10)
                                )
                            ) {
                                BasicTextField(
                                    value = uiState.draftText,
                                    onValueChange = { actions.onDraftChange(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = NdjcCommonTokens.Dp.Dp12,
                                            vertical = NdjcCommonTokens.Dp.Dp10
                                        ),
                                    singleLine = false,
                                    minLines = 1,
                                    maxLines = 8,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a85),
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        lineHeightStyle = LineHeightStyle(
                                            alignment = LineHeightStyle.Alignment.Center,
                                            trim = LineHeightStyle.Trim.Both
                                        )
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Default
                                    ),
                                    keyboardActions = KeyboardActions.Default,
                                    cursorBrush = SolidColor(Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)),
                                    onTextLayout = { textLayoutResult ->
                                        val newLineCount = textLayoutResult.lineCount.coerceIn(1, 8)
                                        if (newLineCount != draftVisibleLineCount) {
                                            draftVisibleLineCount = newLineCount
                                        }
                                    },
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = ChatTokens.InputMinHeight - NdjcCommonTokens.Dp.Dp20),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (uiState.draftText.isBlank()) {
                                                Text(
                                                    text = "Message",
                                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a45),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                        lineHeightStyle = LineHeightStyle(
                                                            alignment = LineHeightStyle.Alignment.Center,
                                                            trim = LineHeightStyle.Trim.Both
                                                        )
                                                    )
                                                )
                                            }

                                            innerTextField()
                                        }
                                    }
                                )
                            }

                            // ✅ 统一节奏：输入框到 Send 8dp（原来 10dp 偏松）
                            Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))

                            val sendInteractionSource = remember { MutableInteractionSource() }
                            val sendPressed by sendInteractionSource.collectIsPressedAsState()
                            val sendScale by animateFloatAsState(
                                targetValue = if (sendPressed) 0.965f else 1f,
                                animationSpec = tween(durationMillis = 120),
                                label = "ChatSendButtonScale"
                            )

                            Surface(
                                modifier = Modifier
                                    .height(ChatTokens.SendHeight)
                                    .graphicsLayer {
                                        scaleX = sendScale
                                        scaleY = sendScale
                                    },
                                shape = RoundedCornerShape(ChatTokens.SendCorner),
                                color = HomeTokens.ChipSelectedBackground,
                                shadowElevation = if (sendPressed) NdjcCommonTokens.Dp.Dp0 else NdjcCommonTokens.Dp.Dp2,
                                tonalElevation = NdjcCommonTokens.Dp.Dp0
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(ChatTokens.SendCorner))
                                        .clickable(
                                            interactionSource = sendInteractionSource,
                                            indication = ripple(bounded = true)
                                        ) {
                                            actions.onSend()
                                            keyboard?.hide()
                                            focusManager.clearFocus()
                                        }
                                        .padding(horizontal = NdjcCommonTokens.Dp.Dp16),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Send",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) {
            itemsIndexed(
                items = msgs,
                key = { _, m ->
                    val idPart = m.id ?: ""
                    if (idPart.isNotBlank()) idPart
                    else "${m.timeText}|${m.direction}|${m.text.hashCode()}"
                }
            ) { idx, m ->
                    // ✅ 断言：最后一条消息是否真的进入 item 渲染
                    val lastId = msgs.lastOrNull()?.id
                    if (m.id == lastId) {
                        android.util.Log.e(
                            "ChatTrace",
                            "RENDER_ITEM_LAST id=${(m.id ?: "").takeLast(6)} idx=$idx textLen=${m.text.length}"
                        )
                    }
// timeText 统一来自逻辑模块：yyyy-MM-dd a hh:mm（含 AM/PM）
                    val curDate = if (m.timeText.length >= 10) m.timeText.substring(0, 10) else ""
                    val curClock = m.timeText.substringAfter(' ', m.timeText)

                    val prevDate = msgs
                        .getOrNull(idx - 1)
                        ?.timeText
                        ?.let { if (it.length >= 10) it.substring(0, 10) else "" }
                        ?: ""

                    val showDateHeader = curDate.isNotBlank() && curDate != prevDate

// ✅ 同一分钟只显示一次时间：按“date + clock”去重
                    val curMinuteKey = if (curDate.isNotBlank() && curClock.isNotBlank()) "${curDate}|${curClock}" else ""
                    val prevMinuteKey = msgs
                        .getOrNull(idx - 1)
                        ?.timeText
                        ?.let {
                            val d = if (it.length >= 10) it.substring(0, 10) else ""
                            val c = it.substringAfter(' ', it)
                            if (d.isNotBlank() && c.isNotBlank()) "${d}|${c}" else ""
                        }
                        ?: ""
                    val showMinuteHeader = curMinuteKey.isNotBlank() && curMinuteKey != prevMinuteKey

// ✅ 方向/气泡/状态：只用于展示，不改业务逻辑
                    val isOutgoing = m.direction == com.ndjc.feature.showcase.ShowcaseChatDirection.Outgoing

                    val baseCorner = ChatTokens.ProductCorner

                    val bubbleShape = RoundedCornerShape(
                        topStart = if (!isOutgoing) 0.dp else baseCorner,
                        topEnd = if (isOutgoing) 0.dp else baseCorner,
                        bottomStart = baseCorner,
                        bottomEnd = baseCorner
                    )

                    val statusText = when (m.status) {
                        com.ndjc.feature.showcase.ShowcaseChatSendStatus.Sending -> "Sending"
                        com.ndjc.feature.showcase.ShowcaseChatSendStatus.Sent -> "Sent"
                        com.ndjc.feature.showcase.ShowcaseChatSendStatus.Failed -> "Failed"
                        else -> ""
                    }

// ✅ 已读/未读：仅对“我方已发送的消息”展示
                    val readText = if (isOutgoing && m.status == com.ndjc.feature.showcase.ShowcaseChatSendStatus.Sent) {
                        if (m.isRead) "Read" else "Unread"
                    } else {
                        ""
                    }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (idx == msgs.lastIndex) {
                                Modifier.onGloballyPositioned { coordinates ->
                                    lastMessageBottomInRootPx = coordinates.boundsInRoot().bottom
                                }
                            } else {
                                Modifier
                            }
                        ),
                    horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
                ) {
// ✅ 时间显示策略：
// 1) 取消“每条消息都显示时间”
// 2) 改为“同一分钟只显示一次”
// 3) 时间显示居中：
//    - 跨天的第一条："2026-02-24 PM 07:23"（时间在日期后面）
//    - 同一天后续分钟："PM 07:24"
                        if (showMinuteHeader) {
                            val headerText = if (curDate.isNotBlank()) "${curDate}  ${curClock}" else curClock
                            if (headerText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = NdjcCommonTokens.Dp.Dp8),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(ChatTokens.DatePillCorner),
                                        color = Color.White.copy(alpha = NdjcCommonTokens.Alpha.a70),
                                        tonalElevation = NdjcCommonTokens.Dp.Dp0,
                                        shadowElevation = NdjcCommonTokens.Dp.Dp0
                                    ) {
                                        Text(
                                            text = headerText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a45),
                                            modifier = Modifier.padding(
                                                horizontal = ChatTokens.DatePillPaddingH,
                                                vertical = ChatTokens.DatePillPaddingV
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        val isSelected = uiState.isSelectionMode && uiState.selectedIds.contains(m.id)
                        val isMatched = uiState.findMatchIds.contains(m.id)
                        val isFocused = uiState.findFocusedId == m.id
                        val isJumpFlashing = (jumpFlashMessageId == m.id)

                        // ✅ 解析：正文 + 引用
                        val parsed = remember(m.text) { parseNdjcChatPayloadUi(m.text) }


                        Row(
                            verticalAlignment = Alignment.CenterVertically, // ✅ 保证按钮与“正文气泡”对齐
                            horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // ✅ 失败重试：红底白箭头，小尺寸（只触发 actions）
                            if (isOutgoing && m.status == com.ndjc.feature.showcase.ShowcaseChatSendStatus.Failed) {
                                Box(
                                    modifier = Modifier
                                        .size(NdjcCommonTokens.Dp.Dp26)                // ✅ 按钮更小
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error) // ✅ 红色实底
                                        .clickable { actions.onRetry(m.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Retry",
                                        tint = Color.White,          // ✅ 箭头白色
                                        modifier = Modifier.size(NdjcCommonTokens.Dp.Dp14) // ✅ 箭头缩小
                                    )
                                }
                                Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp6))
                            }
                            val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp.dp * 0.76f)

                            Column(
                                // ✅ 关键：限制内容最大宽度（按屏幕比例）
                                modifier = Modifier.widthIn(max = maxBubbleWidth),
                                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
                            ) {
                                val hasImages = parsed.images.isNotEmpty()
                                val hasProduct = (parsed.product != null)


                                val displayBody = remember(m.text, parsed.body, hasImages, hasProduct) {
                                    when {
                                        parsed.body.isNotBlank() -> parsed.body
                                        hasImages || hasProduct -> ""   // ✅ 富内容消息：不要回退原文
                                        else -> m.text                  // ✅ 纯文本/解析失败：回退原文
                                    }
                                }

                                val hasQuote = parsed.quotePreview?.isNotBlank() == true
                                val hasBody = displayBody.isNotBlank()

                                if (hasBody || hasQuote) {
                                    // ✅ 按压缩放 + 同圆角 ripple（纯 UI，不改业务）
                                    val bubbleInteraction = remember(m.id) { MutableInteractionSource() }
                                    val bubblePressed by bubbleInteraction.collectIsPressedAsState()
                                    val bubblePressedScale by animateFloatAsState(
                                        targetValue = if (bubblePressed) 0.98f else 1f,
                                        label = "chatBubbleScale"
                                    )

                                    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp.dp * 0.76f)

                                    Surface(
                                        modifier = Modifier
                                            .widthIn(max = maxBubbleWidth)
                                            .then(
                                                when {
                                                    isSelected -> Modifier.border(
                                                        width = NdjcCommonTokens.Dp.Dp1,
                                                        color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a55),
                                                        shape = bubbleShape
                                                    )

                                                    isFocused -> Modifier.border(
                                                        width = NdjcCommonTokens.Dp.Dp2,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = NdjcCommonTokens.Alpha.a90),
                                                        shape = bubbleShape
                                                    )

                                                    isMatched -> Modifier.border(
                                                        width = NdjcCommonTokens.Dp.Dp1,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = NdjcCommonTokens.Alpha.a35),
                                                        shape = bubbleShape
                                                    )

                                                    else -> Modifier.border(
                                                        width = NdjcCommonTokens.Dp.Dp1,
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                                        shape = bubbleShape
                                                    )
                                                }
                                            )
                                            .graphicsLayer {
                                                scaleX = bubblePressedScale
                                                scaleY = bubblePressedScale
                                            }
                                            .clip(bubbleShape)
                                            .drawWithContent {
                                                drawContent()
                                                if (isJumpFlashing) {
                                                    // ✅ 整个气泡闪烁：白色半透明遮罩，不吃底色（发送方粉色气泡也能看出来）
                                                    val a = 0.35f * jumpFlash.value
                                                    if (a > 0.001f) {
                                                        drawRect(Color.White.copy(alpha = a))
                                                    }
                                                }
                                            }
                                            .then(
                                                if (uiState.isSelectionMode) {
                                                    Modifier.clickable(
                                                        interactionSource = bubbleInteraction,
                                                        indication = ripple(bounded = true)
                                                    ) { actions.onToggleSelection(m.id) }
                                                } else {
                                                    // ✅ 只拦截长按：不吞短按事件，让子组件（引用块 clickable）能收到点击
                                                    Modifier.pointerInput(m.id) {
                                                        awaitEachGesture {
                                                            val down = awaitFirstDown(requireUnconsumed = false)

                                                            // ✅ 只在确认长按时才触发，并消费事件；短按不消费 → 子组件(引用块 clickable)可正常收到点击
                                                            val longPress = awaitLongPressOrCancellation(down.id)
                                                            if (longPress != null) {
                                                                menuMsgId = m.id
                                                                down.consume()
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                            .align(if (isOutgoing) Alignment.End else Alignment.Start),
                                        shape = bubbleShape,
                                        shadowElevation = ChatTokens.BubbleShadow,
                                        tonalElevation = NdjcCommonTokens.Dp.Dp0,
                                        color = if (isOutgoing) BackButtonTokens.Background else Color(0xFFF2F3F5)
                                    ) {
                                        // ✅ 外层统一 6dp（对齐“商品卡片气泡内卡片距边”）
                                        Column(
                                            modifier = Modifier.padding(NdjcCommonTokens.Dp.Dp6),
                                            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
                                        ) {
                                            val density = LocalDensity.current
                                            var mainBodyWidthPx by remember(m.id) { mutableStateOf(0) }

                                            if (displayBody.isNotBlank()) {
                                                Text(
                                                    text = highlightQueryText(displayBody, uiState.findQuery, isOutgoing),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier
                                                        .padding(
                                                            horizontal = NdjcCommonTokens.Dp.Dp6,
                                                            vertical = NdjcCommonTokens.Dp.Dp4
                                                        )
                                                        .onSizeChanged { mainBodyWidthPx = it.width },
                                                    color = if (isOutgoing)
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            // ✅ 引用块放下面：不再 fillMaxWidth，避免撑宽气泡
                                            parsed.quotePreview?.takeIf { it.isNotBlank() }?.let { q ->
                                                val quotedMessageId = parsed.quoteMessageId

                                                val quotedProductBlock = remember(q) { findBetween(q, NDJC_PRODUCT_START, NDJC_PRODUCT_END) }
                                                val quotedProduct = remember(quotedProductBlock) { quotedProductBlock?.let { parseNdjcProductBlock(it) } }

                                                if (quotedProduct != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .widthIn(max = ChatTokens.ProductMaxWidth)
                                                            .border(
                                                                width = NdjcCommonTokens.Dp.Dp1,
                                                                color = MaterialTheme.colorScheme.outline.copy(alpha = NdjcCommonTokens.Alpha.a18),
                                                                shape = RoundedCornerShape(ChatTokens.ProductCorner)
                                                            )
                                                            .padding(NdjcCommonTokens.Dp.Dp2)
                                                            .combinedClickable(
                                                                onClick = { actions.onOpenProductDetail(quotedProduct.id) },
                                                                onLongClick = { menuMsgId = m.id }
                                                            )
                                                    ) {
                                                        NdjcCatalogItemCard(
                                                            title = quotedProduct.title,
                                                            imageUrl = quotedProduct.imageUrl,
                                                            priceText = quotedProduct.priceText,
                                                            originalPriceText = quotedProduct.originalPriceText,
                                                            discountPriceText = quotedProduct.discountPriceText,
                                                            price = null,
                                                            originalPrice = null,
                                                            discountPrice = null,
                                                            categoryText = null,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                } else {
                                                    val textMeasurer = rememberTextMeasurer()

                                                    // ✅ styles 先取出来（避免 remember{} 内读 MaterialTheme）
                                                    val replyStyle = MaterialTheme.typography.labelSmall
                                                    val quoteStyle = MaterialTheme.typography.labelMedium
                                                    val bodyStyle = MaterialTheme.typography.bodyMedium

                                                    // ✅ 内容区最大宽度（外层 Column 已 padding=6dp，所以内容最大=气泡最大-12dp）
                                                    val contentMaxPx = remember(maxBubbleWidth, density) {
                                                        with(density) { (maxBubbleWidth - (NdjcCommonTokens.Dp.Dp6 * 2)).roundToPx() }
                                                    }

                                                    // ✅ 1) 计算“正文需要宽度”（包含正文自身的 horizontal padding=6*2）
                                                    val bodyNeededWidthPx = remember(displayBody, contentMaxPx, bodyStyle, density) {
                                                        if (displayBody.isBlank()) 0 else {
                                                            val bodyPadHPx = with(density) { (NdjcCommonTokens.Dp.Dp6 * 2).roundToPx() }
                                                            val availableForBodyTextPx = (contentMaxPx - bodyPadHPx).coerceAtLeast(0)

                                                            val bodyTextWidth = textMeasurer.measure(
                                                                text = androidx.compose.ui.text.AnnotatedString(displayBody),
                                                                style = bodyStyle,
                                                                constraints = androidx.compose.ui.unit.Constraints(maxWidth = availableForBodyTextPx)
                                                            ).size.width

                                                            (bodyTextWidth + bodyPadHPx).coerceAtMost(contentMaxPx)
                                                        }
                                                    }

                                                    // ✅ 2) 计算“引用块需要宽度”（你原来的算法，限制在 contentMaxPx 内）
                                                    val quoteNeededWidthPx = remember(q, contentMaxPx, replyStyle, quoteStyle, density) {
                                                        val barPx = with(density) { NdjcCommonTokens.Dp.Dp3.roundToPx() }
                                                        val gapPx = with(density) { NdjcCommonTokens.Dp.Dp8.roundToPx() }
                                                        val padHPx = with(density) { ChatTokens.QuoteInnerPaddingH.roundToPx() }

                                                        val availableForTextPx = (contentMaxPx - (padHPx * 2) - barPx - gapPx).coerceAtLeast(0)

                                                        val replyWidth = textMeasurer.measure(
                                                            text = androidx.compose.ui.text.AnnotatedString("Replying to"),
                                                            style = replyStyle,
                                                            constraints = androidx.compose.ui.unit.Constraints(maxWidth = availableForTextPx)
                                                        ).size.width

                                                        val quoteWidth = textMeasurer.measure(
                                                            text = androidx.compose.ui.text.AnnotatedString(q.replace("\n", " ")),
                                                            style = quoteStyle,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            constraints = androidx.compose.ui.unit.Constraints(maxWidth = availableForTextPx)
                                                        ).size.width

                                                        val colWidth = maxOf(replyWidth, quoteWidth)
                                                        ((padHPx * 2) + barPx + gapPx + colWidth).coerceAtMost(contentMaxPx)
                                                    }

                                                    // ✅ 3) 内容区最终宽度 = max(正文宽, 引用宽)（跟随更宽者）
                                                    val contentWidthPx = remember(bodyNeededWidthPx, quoteNeededWidthPx, contentMaxPx) {
                                                        maxOf(bodyNeededWidthPx, quoteNeededWidthPx).coerceAtMost(contentMaxPx)
                                                    }

                                                    val contentWidthDp = remember(contentWidthPx, density) {
                                                        with(density) { contentWidthPx.toDp() }
                                                    }

                                                    // ✅ 用一个固定宽度的内容壳子包住“正文+引用”，让两者对齐到同一宽度 → 三边距离永远一致
                                                    Column(
                                                        // ✅ 这个宽度仍然用 max(bodyNeededWidthPx, quoteNeededWidthPx)
                                                        // ✅ 但主信息 displayBody 已在 quote 分支外渲染过了，这里不要再渲染一次
                                                        modifier = Modifier.width(contentWidthDp)
                                                    ) {
                                                        val jumpTargetId = remember(quotedMessageId, q, uiState.messages) {
                                                            quotedMessageId
                                                                ?: fallbackFindQuotedMessageId(
                                                                    quotePreview = q,
                                                                    messages = uiState.messages
                                                                )
                                                        }

                                                        Surface(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .then(
                                                                    if (!jumpTargetId.isNullOrBlank()) {
                                                                        Modifier.clickable { actions.onJumpToMessage(jumpTargetId) }
                                                                    } else {
                                                                        Modifier
                                                                    }
                                                                ),
                                                            shape = RoundedCornerShape(ChatTokens.QuoteCorner),
                                                            tonalElevation = NdjcCommonTokens.Dp.Dp0,
                                                            shadowElevation = NdjcCommonTokens.Dp.Dp0,
                                                            color = Color.White
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .border(
                                                                        width = ChatTokens.QuoteBorderWidth,
                                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = NdjcCommonTokens.Alpha.a18),
                                                                        shape = RoundedCornerShape(ChatTokens.QuoteCorner)
                                                                    )
                                                                    .padding(
                                                                        horizontal = ChatTokens.QuoteInnerPaddingH,
                                                                        vertical = ChatTokens.QuoteInnerPaddingV
                                                                    ),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .width(NdjcCommonTokens.Dp.Dp3)
                                                                        .height(NdjcCommonTokens.Dp.Dp22)
                                                                        .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp999))
                                                                        .background(BackButtonTokens.Background)
                                                                )

                                                                Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))

                                                                Column(
                                                                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp2)
                                                                ) {
                                                                    Text(
                                                                        text = "Replying to",
                                                                        style = replyStyle,
                                                                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                                                                    )
                                                                    Text(
                                                                        text = q.replace("\n", " "),
                                                                        style = quoteStyle,
                                                                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70),
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // ✅ 是否商品卡片气泡：用它决定菜单项
                                val isProductBubble = (parsed.product != null)

                                DropdownMenu(
                                    expanded = (menuMsgId == m.id),
                                    onDismissRequest = { menuMsgId = null }
                                ) {
                                    // 1) 复制：普通气泡复制文本；商品卡片复制 payload（供粘贴触发待发送条）
                                    DropdownMenuItem(
                                        text = { Text("Copy") },
                                        onClick = {
                                            if (isProductBubble) {
                                                val p = parsed.product!!
                                                val share = ShowcaseChatProductShare(
                                                    dishId = p.id,
                                                    title = p.title,
                                                    price = p.priceText ?: "",
                                                    imageUrl = p.imageUrl
                                                )

                                                actions.onUseProductCardAsPending(share)
                                            } else {
                                                clipboard.setText(AnnotatedString(displayBody))
                                            }
                                            menuMsgId = null
                                        }
                                    )

                                    // 2) 引用：只给普通气泡保留；商品卡片气泡不显示
                                    if (!isProductBubble) {
                                        DropdownMenuItem(
                                            text = { Text("Quote") },
                                            onClick = {
                                                actions.onQuoteMessage(m.id)
                                                menuMsgId = null
                                            }
                                        )
                                    }

                                    // 3) 多选 / 退出多选：两种气泡都保留
                                    DropdownMenuItem(
                                        text = { Text(if (uiState.isSelectionMode) "Exit selection" else "Select") },
                                        onClick = {
                                            if (uiState.isSelectionMode) {
                                                actions.onExitSelection()
                                            } else {
                                                actions.onEnterSelection(m.id)
                                            }
                                            menuMsgId = null
                                        }
                                    )

                                    // 4) 删除：两种气泡都保留
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            actions.onDeleteMessage(m.id)
                                            menuMsgId = null
                                        }
                                    )
                                }

                            }

                        }

// ✅ 已发送/已入库图片：一张图 = 一个气泡；靠左/靠右跟随消息方向（纯 UI）
                        val imgs = remember(parsed.images) { parsed.images.distinct() } // 防止重复渲染（同一条消息里）
                        if (imgs.isNotEmpty()) {
                            imgs.forEachIndexed { idx, uriString ->
                                Row(

                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = if (idx == 0) NdjcCommonTokens.Dp.Dp6 else NdjcCommonTokens.Dp.Dp8),
                                    horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
                                ) {
                                    NdjcChatImageBubbleUi(
                                        url = uriString,
                                        onTap = {
                                            val pool = allThreadImages
                                                .ifEmpty { imgs }
                                                .distinct()

                                            val start = pool.indexOf(uriString).let { if (it >= 0) it else 0 }

                                            chatImagePreview = NdjcChatImagePreviewState(
                                                images = pool,
                                                startIndex = start
                                            )
                                        },
                                        onLongPress = {
                                            // ✅ 图片气泡长按：走同一套菜单（复制/引用/多选/删除）
                                            menuMsgId = m.id
                                        }
                                    )
                                }
                            }
                        }

// ✅ 新增：消息里渲染“商品卡片气泡”（纯 UI，不做逻辑）
                        parsed.product?.let { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = NdjcCommonTokens.Dp.Dp8),
                                horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
                            ) {
                                val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp.dp * 0.76f)
                                // ✅ 商品卡片也接入“长按菜单 + 多选点击切换 + 选中高亮”
                                val productBubbleModifier = Modifier
                                    .widthIn(max = maxBubbleWidth)
                                    .then(
                                        when {
                                            isSelected -> Modifier.border(
                                                width = NdjcCommonTokens.Dp.Dp1,
                                                color = MaterialTheme.colorScheme.error.copy(alpha = NdjcCommonTokens.Alpha.a55),
                                                shape = bubbleShape
                                            )
                                            isFocused -> Modifier.border(
                                                width = NdjcCommonTokens.Dp.Dp2,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = NdjcCommonTokens.Alpha.a90),
                                                shape = bubbleShape
                                            )
                                            isMatched -> Modifier.border(
                                                width = NdjcCommonTokens.Dp.Dp1,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = NdjcCommonTokens.Alpha.a35),
                                                shape = bubbleShape
                                            )
                                            else -> Modifier.border(
                                                width = NdjcCommonTokens.Dp.Dp1,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                                shape = bubbleShape
                                            )
                                        }
                                    )
                                    .clip(bubbleShape)
                                    .combinedClickable(
                                        onClick = {
                                            if (uiState.isSelectionMode) {
                                                actions.onToggleSelection(m.id)
                                            } else {
                                                // ⚠️ UI 只发事件，不做导航
                                                actions.onOpenProductDetail(p.id)
                                            }
                                        },
                                        onLongClick = {
                                            // ✅ 与普通气泡一致：长按打开该条消息的菜单
                                            menuMsgId = m.id
                                        }
                                    )

                                // ✅ 外层气泡：用 bubbleShape 包裹商品卡片
                                Surface(
                                    modifier = productBubbleModifier,
                                    shape = bubbleShape,
                                    shadowElevation = ChatTokens.BubbleShadow,
                                    tonalElevation = NdjcCommonTokens.Dp.Dp0,
                                    color = if (isOutgoing) BackButtonTokens.Background else Color(0xFFF2F3F5)
                                ) {
                                    // ✅ 气泡内留一点边距，让卡片不贴边
                                    Box(modifier = Modifier.padding(NdjcCommonTokens.Dp.Dp6)) {
                                        NdjcCatalogItemCard(
                                            title = p.title,
                                            imageUrl = p.imageUrl,
                                            priceText = p.priceText,
                                            originalPriceText = p.originalPriceText,
                                            discountPriceText = p.discountPriceText,
                                            price = null,
                                            originalPrice = null,
                                            discountPrice = null,
                                            categoryText = null,
                                            modifier = Modifier.fillMaxWidth(),

                                            // ✅ 卡片自身维持白底（更像“卡片”），外层气泡提供“聊天语境壳”
                                            containerColorOverride = Color.White,
                                            shadowElevationOverride = ChatTokens.ProductShadow,
                                            shapeOverride = RoundedCornerShape(ChatTokens.ProductCorner),
                                            contentPaddingOverride = PaddingValues(
                                                horizontal = ChatTokens.ProductPaddingH,
                                                vertical = ChatTokens.ProductPaddingV
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
// ✅ 发送状态：更小、更淡、贴合气泡底部（不抢视线）
                    if (isOutgoing && (statusText.isNotBlank() || readText.isNotBlank())) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = NdjcCommonTokens.Dp.Dp2),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)) {
                                if (statusText.isNotBlank()) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.Black.copy(alpha = 0.32f)
                                    )
                                }
                                if (readText.isNotBlank()) {
                                    Text(
                                        text = readText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.Black.copy(alpha = 0.32f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


// ✅ Chat：点击图片 -> 全屏预览（对齐编辑页：Pager + 系统栏黑 + 右上角关闭 + 点击退出）
    val st = chatImagePreview
    if (st != null && st.images.isNotEmpty()) {
        NdjcFullscreenImageViewerScreen(
            images = st.images,
            startIndex = st.startIndex,
            onDismiss = { chatImagePreview = null },
            onSave = { url -> actions.onSavePreviewImage(url) }
        )
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShowcaseChatMedia(
    uiState: com.ndjc.feature.showcase.ShowcaseChatUiState,
    actions: com.ndjc.feature.showcase.ShowcaseChatMediaActions,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    val msgs = uiState.messages

    // ✅ ChatMedia：按“消息发送日期”分组展示图片（UI 只做展示层聚合，不做业务写入）
    data class NdjcChatMediaItemUi(
        val url: String,
        val timeMs: Long
    )

// ✅ 解析逻辑模块提供的 timeText：
// 24 小时制 -> yyyy-MM-dd HH:mm
// 12 小时制 -> yyyy-MM-dd a hh:mm
    val timeTextParsers = remember {
        listOf(
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd a hh:mm", java.util.Locale.US)
        )
    }

    fun parseTimeMsFromTimeText(timeText: String): Long? {
        val t = timeText.trim()
        if (t.isBlank()) return null

        for (parser in timeTextParsers) {
            try {
                val parsed = parser.parse(t)?.time
                if (parsed != null) {
                    return parsed
                }
            } catch (_: Throwable) {
            }
        }
        return null
    }

    // 1) 从消息里提取图片 + “消息发送时间”
    val mediaItems = remember(msgs) {
        msgs
            .flatMapIndexed { index, msg ->
                val ts = parseTimeMsFromTimeText(msg.timeText)
                // ✅ 兜底：如果 timeText 解析失败，至少用 index 保证排序稳定（不会全变成今天）
                    ?: (index.toLong() * 1000L)

                parseNdjcChatPayloadUi(msg.text)
                    .images
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { url -> NdjcChatMediaItemUi(url = url, timeMs = ts) }
            }
            // 同一 url 多次出现时：保留“最早出现”的时间（用于分组更稳定）
            .groupBy { it.url }
            .map { (_, xs) -> xs.minBy { it.timeMs } }
            .sortedByDescending { it.timeMs }
    }

    // 2) 分组：dayKey(String "yyyy-MM-dd") -> items（按日期倒序；字符串字典序=日期顺序）
    val dayKeyFmt = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    }

    val mediaByDate: List<Pair<String, List<NdjcChatMediaItemUi>>> = remember(mediaItems) {
        mediaItems
            .groupBy { item -> dayKeyFmt.format(java.util.Date(item.timeMs)) }
            .toList()
            .sortedByDescending { (dayKey, _) -> dayKey }
    }


// 3) Pager 的“全量图片列表”（时间倒序）
    val allImagesSorted = remember(mediaItems) { mediaItems.map { it.url } }

    // ✅ 本页面自己的预览状态（避免引用不存在的 ImagePreviewState）
    data class NdjcChatMediaPreviewState(
        val images: List<String>,
        val startIndex: Int
    )

    var preview by remember { mutableStateOf<NdjcChatMediaPreviewState?>(null) }

    NdjcUnifiedBackground(
        modifier = Modifier.fillMaxSize(),
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() }
        )
    ) {
        val flatUrls = remember(mediaItems) { mediaItems.map { it.url } }

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = NdjcTopContentPadding,
                    start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    bottom = NdjcWhiteCardLayoutTokens.ScreenPadding
                )
                .navigationBarsPadding()
        ) {
            NdjcWhiteCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                ) {

                    // ✅ 标题区：对齐“商家详情编辑页”的节奏（Title/Subtitle：4dp，区块：10dp）
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                    ) {
                        Text(
                            text = "Shared photos (${mediaItems.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Images exchanged in this conversation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)
                    ) {

                        if (mediaItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = NdjcCommonTokens.Dp.Dp40),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No photos yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )
                                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))
                                    Text(
                                        text = "Photos shared here will appear automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            val groups = mediaByDate.toList()

                            groups.forEachIndexed { index, (dayKey, itemsInDay) ->
                                Surface(
                                    shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp16),
                                    tonalElevation = NdjcCommonTokens.Dp.Dp0,
                                    shadowElevation = NdjcCommonTokens.Dp.Dp0,
                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10 * 0.45f),
                                    modifier = Modifier.padding(start = NdjcCommonTokens.Dp.Dp2)
                                ) {
                                    Text(
                                        text = dayKey,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = NdjcCommonTokens.Dp.Dp10,
                                            vertical = NdjcCommonTokens.Dp.Dp6
                                        )
                                    )
                                }

                                // ✅ 图片间距：更紧凑（8dp → 6dp）
                                itemsInDay.chunked(3).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
                                    ) {
                                        row.forEach { m ->
                                            val tileShape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp12)
                                            val interactionSource = remember { MutableInteractionSource() }
                                            val pressed by interactionSource.collectIsPressedAsState()

                                            // ✅ 点击反馈升级：scale + overlay + 轻边框 + ripple
                                            val scale by animateFloatAsState(
                                                targetValue = if (pressed) 0.97f else 1f,
                                                label = "mediaTileScale"
                                            )
                                            val overlayAlpha by animateFloatAsState(
                                                targetValue = if (pressed) 0.10f else 0f,
                                                label = "mediaTileOverlay"
                                            )
                                            val borderAlpha by animateFloatAsState(
                                                targetValue = if (pressed) 0.35f else 0f,
                                                label = "mediaTileBorder"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .graphicsLayer {
                                                        scaleX = scale
                                                        scaleY = scale
                                                    }
                                                    .clip(tileShape)
                                                    .background(Color.Black.copy(alpha = 0.06f))
                                                    .border(
                                                        width = NdjcCommonTokens.Dp.Dp1,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                                                        shape = tileShape
                                                    )
                                                    .clickable(
                                                        interactionSource = interactionSource,
                                                        indication = ripple(bounded = true)
                                                    ) {
                                                        val startIndex = flatUrls.indexOf(m.url).coerceAtLeast(0)
                                                        preview = NdjcChatMediaPreviewState(
                                                            images = flatUrls,
                                                            startIndex = startIndex
                                                        )
                                                    }
                                            ) {
                                                NdjcShimmerImage(
                                                    imageUrl = m.url,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    placeholderCornerRadius = NdjcCommonTokens.Dp.Dp16,
                                                    contentScale = ContentScale.Crop
                                                )

                                                if (overlayAlpha > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = overlayAlpha))
                                                    )
                                                }
                                            }
                                        }
                                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }

                                // ✅ 仅“分组之间”加间距：最后一个分组不加，避免白卡底部多出一截
                                if (index != groups.lastIndex) {
                                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
        val st = preview
        if (st != null) {
            NdjcFullscreenImageViewerScreen(
                images = st.images,
                startIndex = st.startIndex,
                onDismiss = { preview = null },
                onSave = { url -> actions.onSavePreviewImage(url) }
            )
        }
    }


data class NdjcProductCardUi(
    val id: String,
    val title: String,

    // ✅ 新：用于卡片显示两档价
    val originalPriceText: String? = null,
    val discountPriceText: String? = null,

    // ✅ 兼容：聊天 payload 仍只有一个 priceText
    val priceText: String? = null,

    val imageUrl: String? = null,
)

// ✅ Chat payload markers（UI 层自持有一份，避免引用逻辑层 private const）
private const val NDJC_PRODUCT_START = "⟪P⟫"
private const val NDJC_PRODUCT_END = "⟪/P⟫"
private const val NDJC_QUOTE_START = "⟪Q⟫"
private const val NDJC_QUOTE_END = "⟪/Q⟫"
private const val NDJC_IMG_START = "⟪I⟫"
private const val NDJC_IMG_END = "⟪/I⟫"

private data class NdjcParsedChatPayloadUi(
    val body: String,
    val quoteRaw: String?,
    val quoteMessageId: String? = null,
    val quotePreview: String? = null,
    val images: List<String>,
    val product: NdjcProductCardUi? = null,
)

private fun parseNdjcChatPayloadUi(text: String): NdjcParsedChatPayloadUi {
    val normalized = text
        .replace("《P》", NDJC_PRODUCT_START)
        .replace("《/P》", NDJC_PRODUCT_END)
        .replace("《Q》", NDJC_QUOTE_START)
        .replace("《/Q》", NDJC_QUOTE_END)
        .replace("《I》", NDJC_IMG_START)
        .replace("《/I》", NDJC_IMG_END)

    val imgsBlock = findBetween(normalized, NDJC_IMG_START, NDJC_IMG_END)
    val imgs = imgsBlock
        ?.split("|")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    val quoteRaw = findBetween(normalized, NDJC_QUOTE_START, NDJC_QUOTE_END)?.takeIf { it.isNotBlank() }

    // ✅ 新协议：<quotedMessageId>\n<preview...>；旧协议：只有 preview
    val quoteMessageId: String? = quoteRaw
        ?.indexOf('\n')
        ?.takeIf { it > 0 }
        ?.let { nl -> quoteRaw.substring(0, nl).trim() }
        ?.takeIf { it.isNotBlank() }

    val quotePreview: String? = if (quoteRaw != null) {
        val nl = quoteRaw.indexOf('\n')
        if (nl > 0) quoteRaw.substring(nl + 1).trimStart('\n', ' ').takeIf { it.isNotBlank() }
        else quoteRaw.trim().takeIf { it.isNotBlank() }
    } else null

    val textWithoutQuote = if (quoteRaw != null) {
        normalized.replace("$NDJC_QUOTE_START$quoteRaw$NDJC_QUOTE_END", "")
    } else normalized

    val productBlock = findBetween(textWithoutQuote, NDJC_PRODUCT_START, NDJC_PRODUCT_END)
    val product = productBlock?.let { parseNdjcProductBlock(it) }

    var body = normalized
    if (imgsBlock != null) body = body.replace("$NDJC_IMG_START$imgsBlock$NDJC_IMG_END", "")
    if (quoteRaw != null) body = body.replace("$NDJC_QUOTE_START$quoteRaw$NDJC_QUOTE_END", "")
    if (productBlock != null) body = body.replace("$NDJC_PRODUCT_START$productBlock$NDJC_PRODUCT_END", "")
    body = body.trim()

    return NdjcParsedChatPayloadUi(
        body = body,
        quoteRaw = quoteRaw,
        quoteMessageId = quoteMessageId,
        quotePreview = quotePreview,
        images = imgs,
        product = product
    )
}

private fun fallbackFindQuotedMessageId(
    quotePreview: String,
    messages: List<ShowcaseChatMessageUi>
): String? {
    val key = quotePreview.trim()
    if (key.isBlank()) return null

    // 从后往前找：更接近“被引用”的那条
    for (m in messages.asReversed()) {
        val parsed = parseNdjcChatPayloadUi(m.text)
        val body = parsed.body.trim()
        if (body.isNotBlank() && body.contains(key, ignoreCase = true)) {
            return m.id
        }
    }
    return null
}

/** UI 辅助：从 text 中取出 start/end 之间的内容（不包含标记本身）；找不到则返回 null。 */
private fun findBetween(text: String, start: String, end: String): String? {
    val s = text.indexOf(start)
    if (s < 0) return null
    val from = s + start.length
    val e = text.indexOf(end, startIndex = from)
    if (e < 0) return null
    return text.substring(from, e)
}

private fun parseNdjcProductBlock(block: String): NdjcProductCardUi? {
    // block: "<id>|<title>|<price>|<imageUrl>"
    val parts = block.split("|").map { it.trim() }
    if (parts.size < 2) return null

    val id = parts.getOrNull(0).orEmpty()
    val title = parts.getOrNull(1).orEmpty()
    val price = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
    val imageUrl = parts.getOrNull(3)?.takeIf { it.isNotBlank() }

    if (id.isBlank() || title.isBlank()) return null

    return NdjcProductCardUi(
        id = id,
        title = title,
        priceText = price,
        imageUrl = imageUrl
    )
}



// ✅ 兼容旧 UI 层 quote 解析（仍被部分组件复用）
private data class NdjcParsedQuoteUi(
    val body: String,
    val quote: String?
)

private fun parseNdjcQuotePayloadUi(text: String): NdjcParsedQuoteUi {
    if (!text.startsWith(NDJC_QUOTE_START)) return NdjcParsedQuoteUi(body = text, quote = null)

    val endIdx = text.indexOf(NDJC_QUOTE_END)
    if (endIdx <= NDJC_QUOTE_START.length) return NdjcParsedQuoteUi(body = text, quote = null)

    val quote = text.substring(NDJC_QUOTE_START.length, endIdx).trim()
    val rest = text.substring(endIdx + NDJC_QUOTE_END.length).trimStart('\n', ' ')
    return NdjcParsedQuoteUi(body = rest, quote = quote.ifBlank { null })
}
@Composable
private fun NdjcChatSingleImageBubble(
    uriString: String,
    isOutgoing: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val bubbleShape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp18)

    Surface(
        shape = bubbleShape,
        tonalElevation = NdjcCommonTokens.Dp.Dp1,
        shadowElevation = NdjcCommonTokens.Dp.Dp0,
        modifier = Modifier
            .widthIn(max = NdjcCommonTokens.Dp.Dp220)
            .heightIn(max = NdjcCommonTokens.Dp.Dp220)
            .clip(bubbleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        NdjcShimmerImage(
            imageUrl = uriString,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            placeholderCornerRadius = NdjcCommonTokens.Dp.Dp18,
            contentDescription = "ChatImage",
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun NdjcChatImagesGridUi(
    images: List<String>,
    modifier: Modifier = Modifier
) {
    // 纯展示：最多渲染 9 张（3x3），不做任何业务逻辑
    val show = remember(images) { images.take(9) }
    val rows = remember(show) { show.chunked(3) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
            ) {
                row.forEach { uriString ->
                    NdjcShimmerImage(
                        imageUrl = uriString,
                        modifier = Modifier
                            .size(NdjcCommonTokens.Dp.Dp88)
                            .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp12)),
                        placeholderCornerRadius = NdjcCommonTokens.Dp.Dp12,
                        contentScale = ContentScale.Crop
                    )
                }

                // 不满 3 张时补空位，保证对齐（纯 UI）
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.size(NdjcCommonTokens.Dp.Dp88))
                }
            }
        }
    }
}
    @Composable
    private fun NdjcChatImageBubbleUi(
        url: String,
        onTap: () -> Unit,
        onLongPress: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        NdjcShimmerImage(
            imageUrl = url,
            contentDescription = null,
            modifier = modifier
                .size(NdjcCommonTokens.Dp.Dp132) // 你想更大就改这里
                .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp14))
                .pointerInput(url) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPress() }
                    )
                },
            placeholderCornerRadius = NdjcCommonTokens.Dp.Dp14,
            contentScale = ContentScale.Crop
        )
    }

    @Composable
private fun NdjcChatImageBubbleUi(
    url: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
    ) {
        // 纯展示 + 点击回调（不做任何业务决策）
        NdjcShimmerImage(
            imageUrl = url,
            contentDescription = null,
            modifier = modifier
                .size(NdjcCommonTokens.Dp.Dp120)                 // 你可按需要调大/调小
                .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp14))
                .clickable(onClick = onClick),
            placeholderCornerRadius = NdjcCommonTokens.Dp.Dp14,
            contentScale = ContentScale.Crop
        )
    }


@Composable
private fun MutedText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a75)
    )
}

@Composable
private fun DeleteConfirmDialog(
    title: String = "Confirm deletion",
    message: String = "This action cannot be undone.",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogShape = RoundedCornerShape(DialogTokens.CornerRadius)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
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
@Composable
private fun NdjcProductCardBubble(
    product: NdjcProductCardUi,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp14))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(NdjcCommonTokens.Dp.Dp10),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!product.imageUrl.isNullOrBlank()) {
            NdjcShimmerImage(
                imageUrl = product.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(NdjcCommonTokens.Dp.Dp54)
                    .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp12)),
                placeholderCornerRadius = NdjcCommonTokens.Dp.Dp12,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(NdjcCommonTokens.Dp.Dp54)
                    .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp12))
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp10))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val original = product.originalPriceText
            val discount = product.discountPriceText

            when {
                !discount.isNullOrBlank() && !original.isNullOrBlank() && discount != original -> {
                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp2))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)) {
                        Text(
                            text = discount,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = original,
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NdjcCommonTokens.Alpha.a75)
                        )
                    }
                }
                !original.isNullOrBlank() -> {
                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp2))
                    Text(
                        text = original,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                !product.priceText.isNullOrBlank() -> {
                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp2))
                    Text(
                        text = product.priceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

        }
    }
}


@Composable
private fun TagsFilterRow(
    allTags: List<String>,
    selectedTags: List<String>,
    onClearTags: () -> Unit,
    onToggleTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8),
        contentPadding = PaddingValues(vertical = NdjcCommonTokens.Dp.Dp4)
    ) {
        item {
            FilterChip(
                selected = selectedTags.isEmpty(),
                onClick = { onClearTags() },
                label = { Text("All") }
            )
        }

        items(allTags.size) { idx ->
            val tag = allTags[idx]
            val selected = selectedTags.contains(tag)
            FilterChip(
                selected = selected,
                onClick = { onToggleTag(tag) },
                label = { Text(tag) }
            )
        }
    }
} // ✅ 关键：关闭 TagsFilterRow（否则 BgCircle 会被吞进去）

@Composable
private fun BgCircle(
    size: Dp,
    offsetX: Dp,
    offsetY: Dp,
    colors: List<Color>,
    alpha: Float = 0.22f,
    modifier: Modifier = Modifier
) {
    val brush = Brush.radialGradient(
        colors = colors,
        center = Offset(0.5f, 0.5f),
        radius = 0.5f
    )

    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .size(size)
            .alpha(alpha)
            .background(brush = brush, shape = CircleShape)
    )
}
@Composable
private fun StoreProfileLogoPicker(
    logoRaw: String,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    onPreview: (images: List<String>, startIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val logo = remember(logoRaw) { logoRaw.trim() }

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Logo", style = MaterialTheme.typography.labelLarge, color = Color.Black)
        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val logoCellSize = (this.maxWidth - NdjcCommonTokens.Dp.Dp10 * 2) / 3

            NdjcSingleEditableImage(
                imageUrl = logo,
                enabled = enabled,
                onAddClick = onPick,
                onRemoveClick = onRemove,
                onPreviewClick = {
                    if (logo.isNotBlank()) {
                        onPreview(listOf(logo), 0)
                    }
                },
                modifier = Modifier,
                size = logoCellSize,
                cornerRadius = NdjcCommonTokens.Dp.Dp18
            )
        }

        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp14))
    }
}

@Composable
private fun StoreProfileCoverPicker(
    coverRaw: String,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: (String) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onPreview: (images: List<String>, startIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val covers = remember(coverRaw) {
        coverRaw
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(9)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Cover (up to 9 images)", style = MaterialTheme.typography.labelLarge, color = Color.Black)
        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))

        NdjcEditableImageGrid(
            images = covers,
            enabled = enabled,
            onAddClick = onPick,
            onRemoveClick = { url -> onRemove(url) },
            onPreview = { images, startIndex ->
                onPreview(images, startIndex)
            },
            maxCount = 9,
            columns = 3,
            cellGap = NdjcCommonTokens.Dp.Dp10,
            cornerRadius = NdjcCommonTokens.Dp.Dp18,
            onMove = { from, to ->
                onMove(from, to)
            },
            onDraggingChange = onDraggingChange
        )

        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp14))
    }
}
@Composable
private fun NdjcEditableImageGrid(
    images: List<String>,
    enabled: Boolean,
    onAddClick: () -> Unit,
    onRemoveClick: (String) -> Unit,
    onPreview: (images: List<String>, startIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    maxCount: Int = 9,
    columns: Int = 3,
    cellGap: Dp = NdjcCommonTokens.Dp.Dp10,
    cornerRadius: Dp = NdjcCommonTokens.Dp.Dp18,
    onMove: ((Int, Int) -> Unit)? = null,
    onDraggingChange: (Boolean) -> Unit = {}
) {
    val cleanImages = remember(images, maxCount) {
        images
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(maxCount)
    }

    val canAdd = enabled && cleanImages.size < maxCount
    val canReorder = enabled && onMove != null && cleanImages.size > 1

    var cellSize by remember { mutableStateOf(NdjcCommonTokens.Dp.Dp92) }

    val boundsMap = remember {
        mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>()
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragOriginIndex by remember { mutableStateOf<Int?>(null) }
    var pendingTargetIndex by remember { mutableStateOf<Int?>(null) }
    var dragStartLocal by remember { mutableStateOf(Offset.Zero) }
    var dragShadowUrl by remember { mutableStateOf<String?>(null) }
    var dragShadowTopLeftInRoot by remember { mutableStateOf<Offset?>(null) }
    var gridContainerPosInRoot by remember { mutableStateOf(Offset.Zero) }

    val alphaAnimSpec = remember {
        tween<Float>(durationMillis = 140, easing = FastOutSlowInEasing)
    }
    val colorAnimSpec = remember {
        tween<Color>(durationMillis = 160, easing = FastOutSlowInEasing)
    }
    val shadowAnimSpec = remember {
        tween<Dp>(durationMillis = 160, easing = FastOutSlowInEasing)
    }
    val scaleAnimSpec = remember {
        tween<Float>(durationMillis = 160, easing = FastOutSlowInEasing)
    }
    val offsetAnimSpec = remember {
        tween<IntOffset>(durationMillis = 70, easing = LinearOutSlowInEasing)
    }

    fun resetDragState() {
        isDragging = false
        onDraggingChange(false)
        dragOriginIndex = null
        pendingTargetIndex = null
        dragShadowUrl = null
        dragShadowTopLeftInRoot = null
    }

    fun findTargetIndex(pointerInRoot: Offset): Int? {
        val hit = boundsMap.entries
            .firstOrNull { (_, rect) -> rect.contains(pointerInRoot) }
            ?.key

        val maxMovable = cleanImages.size - 1
        return hit?.takeIf { it in 0..maxMovable }
    }

    fun movePreview(list: List<String>, from: Int, to: Int): List<String> {
        if (from == to) return list
        if (from !in list.indices || to !in list.indices) return list

        val mutable = list.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        return mutable
    }

    val previewImages = remember(cleanImages, isDragging, dragOriginIndex, pendingTargetIndex) {
        val from = dragOriginIndex
        val to = pendingTargetIndex

        if (
            isDragging &&
            from != null &&
            to != null &&
            from in cleanImages.indices &&
            to in cleanImages.indices
        ) {
            movePreview(cleanImages, from, to)
        } else {
            cleanImages
        }
    }

    val gridItems = remember(previewImages, canAdd, maxCount) {
        val base = previewImages.map { it as String? }.toMutableList()
        if (canAdd && base.size < maxCount) base.add(null)
        base
    }

    val dragNestedScroll = remember { object : NestedScrollConnection {} }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(dragNestedScroll)
            .onGloballyPositioned { coords ->
                gridContainerPosInRoot = coords.positionInRoot()
            }
    ) {
        val computed = (this.maxWidth - cellGap * (columns - 1)) / columns
        cellSize = computed

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(cellGap)
        ) {
            val rows = gridItems.chunked(columns)

            rows.forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(cellGap)
                ) {
                    row.forEachIndexed { colIdx, item ->
                        val index = rowIdx * columns + colIdx

                        if (item == null) {
                            UploadTile(
                                enabled = enabled,
                                hasImage = false,
                                onClick = onAddClick,
                                modifier = Modifier.size(cellSize)
                            )
                        } else {
                            val originalDraggedItem =
                                if (isDragging && dragOriginIndex != null && dragOriginIndex in cleanImages.indices) {
                                    cleanImages[dragOriginIndex!!]
                                } else {
                                    null
                                }

                            val isDraggedOriginalItem =
                                isDragging &&
                                        originalDraggedItem != null &&
                                        item == originalDraggedItem

                            val animatedTileBg by animateColorAsState(
                                targetValue = if (isDraggedOriginalItem) {
                                    NdjcCommonTokens.Colors.C_FFF3F4F6
                                } else {
                                    HomeTokens.BrandPurple.copy(alpha = NdjcCommonTokens.Alpha.a18)
                                },
                                animationSpec = colorAnimSpec,
                                label = "editable_image_tile_bg"
                            )

                            val animatedImageAlpha by animateFloatAsState(
                                targetValue = if (isDraggedOriginalItem) 0f else 1f,
                                animationSpec = alphaAnimSpec,
                                label = "editable_image_tile_alpha"
                            )

                            val animatedRemoveAlpha by animateFloatAsState(
                                targetValue = if (isDraggedOriginalItem) 0f else 1f,
                                animationSpec = alphaAnimSpec,
                                label = "editable_image_remove_alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(cornerRadius))
                                    .background(animatedTileBg)
                                    .onGloballyPositioned { coords ->
                                        boundsMap[index] = coords.boundsInRoot()
                                    }
                                    .pointerInput(enabled, canReorder, cleanImages) {
                                        if (!enabled || !canReorder) return@pointerInput

                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { start ->
                                                val originalIndex = cleanImages.indexOf(item)
                                                if (originalIndex !in cleanImages.indices) return@detectDragGesturesAfterLongPress

                                                isDragging = true
                                                onDraggingChange(true)
                                                dragOriginIndex = originalIndex
                                                pendingTargetIndex = originalIndex
                                                dragStartLocal = start
                                                dragShadowUrl = cleanImages[originalIndex]
                                                dragShadowTopLeftInRoot = boundsMap[index]?.topLeft
                                            },
                                            onDragEnd = {
                                                val from = dragOriginIndex
                                                val target = pendingTargetIndex

                                                if (from != null && target != null && target != from) {
                                                    onMove?.invoke(from, target)
                                                }

                                                resetDragState()
                                            },
                                            onDragCancel = {
                                                resetDragState()
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()

                                                val origin = dragOriginIndex
                                                    ?: return@detectDragGesturesAfterLongPress
                                                val currentTopLeft = dragShadowTopLeftInRoot
                                                    ?: return@detectDragGesturesAfterLongPress

                                                val newTopLeft = currentTopLeft + dragAmount
                                                dragShadowTopLeftInRoot = newTopLeft

                                                val pointerInRoot = newTopLeft + dragStartLocal
                                                val target = findTargetIndex(pointerInRoot)

                                                if (target != null) {
                                                    pendingTargetIndex = target
                                                } else {
                                                    pendingTargetIndex = origin
                                                }
                                            }
                                        )
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (cleanImages.isNotEmpty()) {
                                            val previewIndex = cleanImages.indexOf(item).let {
                                                if (it >= 0) it else 0
                                            }
                                            onPreview(
                                                cleanImages,
                                                previewIndex.coerceIn(0, cleanImages.lastIndex)
                                            )
                                        }
                                    }
                            ) {
                                NdjcShimmerImage(
                                    imageUrl = item,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(animatedImageAlpha),
                                    placeholderCornerRadius = cornerRadius,
                                    contentScale = ContentScale.Crop
                                )

                                if (enabled) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(NdjcCommonTokens.Dp.Dp6)
                                            .size(NdjcCommonTokens.Dp.Dp22)
                                            .alpha(animatedRemoveAlpha)
                                            .clip(CircleShape)
                                            .background(NdjcCommonTokens.Colors.C_FFE53935)
                                            .clickable { onRemoveClick(item) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "X",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (row.size < columns) {
                        repeat(columns - row.size) {
                            Spacer(modifier = Modifier.size(cellSize))
                        }
                    }
                }
            }
        }

        val shadowUrl = dragShadowUrl
        val shadowTopLeftInRoot = dragShadowTopLeftInRoot
        if (shadowUrl != null && shadowTopLeftInRoot != null) {
            val local = shadowTopLeftInRoot - gridContainerPosInRoot

            val animatedOverlayOffset by animateIntOffsetAsState(
                targetValue = IntOffset(
                    local.x.roundToInt(),
                    local.y.roundToInt()
                ),
                animationSpec = offsetAnimSpec,
                label = "editable_image_overlay_offset"
            )

            val animatedOverlayScale by animateFloatAsState(
                targetValue = if (isDragging) 1.04f else 1f,
                animationSpec = scaleAnimSpec,
                label = "editable_image_overlay_scale"
            )

            val animatedOverlayShadow by animateDpAsState(
                targetValue = if (isDragging) NdjcCommonTokens.Dp.Dp14 else NdjcCommonTokens.Dp.Dp12,
                animationSpec = shadowAnimSpec,
                label = "editable_image_overlay_shadow"
            )

            Box(
                modifier = Modifier
                    .zIndex(999f)
                    .offset { animatedOverlayOffset }
                    .graphicsLayer {
                        scaleX = animatedOverlayScale
                        scaleY = animatedOverlayScale
                    }
                    .size(cellSize)
                    .shadow(
                        animatedOverlayShadow,
                        RoundedCornerShape(cornerRadius)
                    )
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                NdjcShimmerImage(
                    imageUrl = shadowUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    placeholderCornerRadius = cornerRadius,
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
@Composable
private fun NdjcSingleEditableImage(
    imageUrl: String?,
    enabled: Boolean,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = NdjcCommonTokens.Dp.Dp96,
    cornerRadius: Dp = NdjcCommonTokens.Dp.Dp18
) {
    val url = remember(imageUrl) { imageUrl?.trim().orEmpty() }

    if (url.isBlank()) {
        UploadTile(
            enabled = enabled,
            hasImage = false,
            onClick = onAddClick,
            modifier = modifier.size(size)
        )
        return
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(HomeTokens.BrandPurple.copy(alpha = NdjcCommonTokens.Alpha.a18))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onPreviewClick()
            }
    ) {
        NdjcShimmerImage(
            imageUrl = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            placeholderCornerRadius = cornerRadius,
            contentScale = ContentScale.Crop
        )

        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(NdjcCommonTokens.Dp.Dp6)
                    .size(NdjcCommonTokens.Dp.Dp22)
                    .clip(CircleShape)
                    .background(NdjcCommonTokens.Colors.C_FFE53935)
                    .clickable { onRemoveClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
@Composable
private fun UploadTile(
    enabled: Boolean,
    hasImage: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp18))
            .clickable(enabled = enabled) { onClick() },
        color = BackButtonTokens.Background,
        tonalElevation = NdjcCommonTokens.Dp.Dp0,
        shadowElevation = NdjcCommonTokens.Dp.Dp2
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Upload",
                tint = Color.White,
                modifier = Modifier.size(NdjcCommonTokens.Dp.Dp28)
            )
        }
    }
}
@Composable
private fun ImageTile(
    uriString: String,
    enabled: Boolean,
    onRemove: () -> Unit,
    onPreview: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {

        Surface(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp18))
                .pointerInput(enabled) {
                    detectTapGestures(
                        onTap = { onPreview() },                 // ✅ 点击：大图预览
                        onLongPress = { if (enabled) onClick() } // ✅ 长按：替换（可选）
                    )
                },
            color = NdjcCommonTokens.Colors.C_FFF1F1F1,
            tonalElevation = NdjcCommonTokens.Dp.Dp0,
            shadowElevation = NdjcCommonTokens.Dp.Dp2
        ) {
            NdjcShimmerImage(
                imageUrl = uriString,
                contentDescription = "Image",
                modifier = Modifier.fillMaxSize(),
                placeholderCornerRadius = NdjcCommonTokens.Dp.Dp18,
                contentScale = ContentScale.Crop
            )
        }
        // ✅ 删除角标（UI 只触发回调）
        if (enabled) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(NdjcCommonTokens.Dp.Dp6)
                    .size(NdjcCommonTokens.Dp.Dp22)
                    .clip(CircleShape)
                    .clickable { onRemove() },
                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a65),
                shadowElevation = NdjcCommonTokens.Dp.Dp2
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(NdjcCommonTokens.Dp.Dp14)
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShowcaseChatSearchResults(
    uiState: com.ndjc.feature.showcase.ShowcaseChatUiState,
    actions: com.ndjc.feature.showcase.ShowcaseChatActions,
    modifier: Modifier = Modifier
) {
    val q = uiState.findQuery
    val results = uiState.globalSearchResults

    BackHandler(enabled = true) {
        actions.onCloseSearchResults()
    }

    NdjcUnifiedBackground(
        modifier = Modifier.fillMaxSize(),
        topNav = NdjcTopNavConfig(
            onBack = { actions.onCloseSearchResults() },
            onHome = { actions.onBackToHome() }
        )
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = NdjcTopContentPadding,
                    start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    bottom = NdjcWhiteCardLayoutTokens.ScreenPadding
                )
                .navigationBarsPadding()
        ) {
            NdjcWhiteCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Rhythm similar to merchant edit page: Title/Subtitle tight + clear section spacing
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp12)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                        ) {
                            Text(
                                text = "Chat history",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Search your conversation history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {

                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                    ) {
                        NdjcTextField(
                            value = q,
                            onValueChange = { actions.onFindQueryChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = "Search messages"
                        )

                        val hint = when {
                            q.isBlank() -> "Type a keyword to search your chat history"
                            else -> "${results.size} result${if (results.size == 1) "" else "s"}"
                        }

                        Surface(
                            shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp12),
                            tonalElevation = NdjcCommonTokens.Dp.Dp0,
                            shadowElevation = NdjcCommonTokens.Dp.Dp0,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10 * 0.45f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = NdjcCommonTokens.Dp.Dp12,
                                        vertical = NdjcCommonTokens.Dp.Dp8
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(NdjcCommonTokens.Dp.Dp18)
                                )

                                Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))

                                Text(
                                    text = hint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Section title
                        if (results.isNotEmpty()) {
                            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp8))

                            Text(
                                text = "Results",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)) {
                            results.forEach { r ->
                                val cardShape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp14)

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = cardShape,
                                    shadowElevation = NdjcCommonTokens.Dp.Dp2,
                                    tonalElevation = NdjcCommonTokens.Dp.Dp0,
                                    color = Color.White
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(cardShape)
                                            .clickable {
                                                actions.onOpenThreadFromSearch(
                                                    r.conversationId,
                                                    r.messageId
                                                )
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = NdjcCommonTokens.Dp.Dp12,
                                                    vertical = NdjcCommonTokens.Dp.Dp10
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = r.senderLabel,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Text(
                                                    text = r.timeText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp6))

                                            Text(
                                                text = highlightQueryText(r.snippet, q, false),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (q.isNotBlank() && results.isEmpty()) {
                            Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
//endregion

//region 11 Chat（UI-only helpers）
// -------------------- Chat find highlight (UI only) --------------------
@Composable
private fun rememberFindHighlightStyle(isOutgoing: Boolean): androidx.compose.ui.text.SpanStyle {
    val fg = if (isOutgoing) {
        // ✅ 发送方气泡底色=粉色：命中文字用白色，保证对比度
        Color.White
    } else {
        // ✅ 接收方/搜索结果：命中文字用品牌粉色（与 Back 同款）
        BackButtonTokens.Background
    }

    return androidx.compose.ui.text.SpanStyle(
        color = fg,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun highlightQueryText(
    text: String,
    query: String,
    isOutgoing: Boolean
): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)

    val q = query.trim()
    if (q.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)

    val style = rememberFindHighlightStyle(isOutgoing)

    // ✅ 关键：escape，避免 query 里出现 ( ) [ ] 等导致 Regex 崩
    val regex = remember(q) { Regex(Regex.escape(q), RegexOption.IGNORE_CASE) }

    return androidx.compose.ui.text.buildAnnotatedString {
        var last = 0
        for (m in regex.findAll(text)) {
            val start = m.range.first
            val endExclusive = m.range.last + 1
            if (start > last) append(text.substring(last, start))
            withStyle(style) { append(text.substring(start, endExclusive)) }
            last = endExclusive
        }
        if (last < text.length) append(text.substring(last))
    }
}
@Composable
private fun NdjcChatImagesGridUi(
    images: List<String>,
    onClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val show = remember(images) { images.take(9) }
    val rows = remember(show) { show.chunked(3) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)) {
                row.forEachIndexed { colIndex, uriString ->
                    val index = rowIndex * 3 + colIndex
                    NdjcShimmerImage(
                        imageUrl = uriString,
                        modifier = Modifier
                            .size(NdjcCommonTokens.Dp.Dp88)
                            .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp12))
                            .clickable { onClick(index) },
                        placeholderCornerRadius = NdjcCommonTokens.Dp.Dp12,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ShowcaseMerchantChatListScreen(
    state: ShowcaseUiState,
    actions: ShowcaseMerchantChatListActions
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    val pullState = rememberPullRefreshState(
        refreshing = state.merchantChatListRefreshing,
        onRefresh = actions.onRefresh
    )

    // ✅ UI-only：本地搜索用户名（不改业务逻辑/不改数据源）
    var userQuery by rememberSaveable { mutableStateOf("") }

    val threadsAll = state.merchantChatThreads
    val threads = remember(userQuery, threadsAll) {
        val q = userQuery.trim()
        if (q.isBlank()) threadsAll
        else threadsAll.filter { it.title.contains(q, ignoreCase = true) }
    }

    NdjcUnifiedBackground(
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() }
        )
    ) {
        // ✅ UI-only: rename dialog state (UI collects text only, no persistence here)
        var renameDialogOpen by remember { mutableStateOf(false) }
        var renameThreadId by remember { mutableStateOf<String?>(null) }
        var renameText by remember { mutableStateOf("") }
        var pendingDeleteThreadId by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingDeleteThreadTitle by rememberSaveable { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullState)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        bottom = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        top = NdjcTopContentPadding
                    )
            ) {

                // ✅ 1) 顶部白卡：位置/节奏对齐 Items 页
                NdjcWhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                    ) {
                        Text(
                            text = "Customer messages",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Manage conversations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        NdjcTextField(
                            value = userQuery,
                            onValueChange = { userQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = "Search customers"
                        )
                    }
                }

                Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp12))

                // ✅ 2) 白卡下面：聊天列表（与 Items 页一样：卡在上，列表在下）
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = NdjcCommonTokens.Dp.Dp20
                    ),
                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)
                ) {
                    if (threads.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (userQuery.trim().isBlank()) "No messages yet" else "No results",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp6))
                                    Text(
                                        text = if (userQuery.trim().isBlank())
                                            "New conversations will appear here."
                                        else
                                            "Try a different name.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(threads, key = { it.threadId }) { t ->
                            var menuExpanded by remember { mutableStateOf(false) }

                            @OptIn(ExperimentalFoundationApi::class)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val interactionSource = remember { MutableInteractionSource() }
                                val pressed by interactionSource.collectIsPressedAsState()
                                val hasUnread = t.unreadCount > 0

                                val cardColor by animateColorAsState(
                                    targetValue = if (pressed) Color(0xFFF7F8FA) else Color.White,
                                    label = "chat_list_card_color"
                                )

                                val previewColor = if (hasUnread) {
                                    Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val timeColor = if (hasUnread) {
                                    Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val cardShape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp16)

                                Surface(
                                    shape = cardShape,
                                    color = cardColor,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.5.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(cardShape)
                                        .combinedClickable(
                                            interactionSource = interactionSource,
                                            indication = ripple(),
                                            onClick = { actions.onOpenThread(t.threadId, t.title) },
                                            onLongClick = { menuExpanded = true }
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(
                                            horizontal = NdjcCommonTokens.Dp.Dp16,
                                            vertical = NdjcCommonTokens.Dp.Dp12
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = t.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a90),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                if (t.isPinned) {
                                                    Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))
                                                    Icon(
                                                        imageVector = Icons.Filled.PushPin,
                                                        contentDescription = "Pinned",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(NdjcCommonTokens.Dp.Dp14)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp12))

                                            Text(
                                                text = t.lastTimeText,
                                                style = if (hasUnread) {
                                                    MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                                } else {
                                                    MaterialTheme.typography.labelSmall
                                                },
                                                color = timeColor,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp4))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                modifier = Modifier.weight(1f),
                                                text = t.lastPreview,
                                                style = if (hasUnread) {
                                                    MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                                } else {
                                                    MaterialTheme.typography.bodySmall
                                                },
                                                color = previewColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp12))

                                            Box(
                                                modifier = Modifier.height(NdjcCommonTokens.Dp.Dp22),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                if (hasUnread) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(BackButtonTokens.Background)
                                                            .padding(
                                                                horizontal = NdjcCommonTokens.Dp.Dp10,
                                                                vertical = NdjcCommonTokens.Dp.Dp2
                                                            )
                                                    ) {
                                                        Text(
                                                            text = "${t.unreadCount}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete conversation") },
                                        onClick = {
                                            menuExpanded = false
                                            pendingDeleteThreadId = t.threadId
                                            pendingDeleteThreadTitle = t.title
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = {
                                            menuExpanded = false
                                            renameThreadId = t.threadId
                                            renameText = t.title
                                            renameDialogOpen = true
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text(if (t.isPinned) "Unpin" else "Pin") },
                                        onClick = {
                                            menuExpanded = false
                                            actions.onTogglePin(t.threadId, !t.isPinned)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Mark as read") },
                                        enabled = t.unreadCount > 0,
                                        onClick = {
                                            menuExpanded = false
                                            actions.onMarkRead(t.threadId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.merchantChatListRefreshing,
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // ✅ UI-only: rename dialog
            if (renameDialogOpen) {
                NdjcBaseDialog(
                    onDismissRequest = {
                        renameDialogOpen = false
                        renameThreadId = null
                        renameText = ""
                    },
                    title = "Rename conversation",
                    confirmText = "Save",
                    dismissText = "Cancel",
                    onConfirmClick = {
                        val id = renameThreadId
                        if (!id.isNullOrBlank()) actions.onRenameThread(id, renameText)
                        renameDialogOpen = false
                        renameThreadId = null
                        renameText = ""
                    },
                    onDismissClick = {
                        renameDialogOpen = false
                        renameThreadId = null
                        renameText = ""
                    },
                    confirmEnabled = renameText.trim().isNotBlank(),
                    textContent = {
                        NdjcTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Name",
                            singleLine = true
                        )
                    }
                )
            }

            val deleteThreadId = pendingDeleteThreadId
            if (!deleteThreadId.isNullOrBlank()) {
                NdjcBaseDialog(
                    onDismissRequest = {
                        pendingDeleteThreadId = null
                        pendingDeleteThreadTitle = ""
                    },
                    title = "Delete conversation?",
                    message = "This will remove this conversation from the list on this device and delete all local chat messages, local draft images, and local temporary files related to this conversation.",
                    confirmText = "Delete",
                    dismissText = "Cancel",
                    onConfirmClick = {
                        actions.onDeleteThread(deleteThreadId)
                        pendingDeleteThreadId = null
                        pendingDeleteThreadTitle = ""
                    },
                    onDismissClick = {
                        pendingDeleteThreadId = null
                        pendingDeleteThreadTitle = ""
                    },
                    destructiveConfirm = true
                )
            }
        }
    }
}
@Composable
internal fun NdjcQuotedProductBar(
    product: NdjcProductCardUi,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp16))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = NdjcCommonTokens.Dp.Dp10, vertical = NdjcCommonTokens.Dp.Dp10),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp14))
                .clickable { onClick() }
        ) {
            NdjcCatalogItemCard(
                title = product.title,
                imageUrl = product.imageUrl,
                priceText = product.priceText,
                originalPriceText = product.originalPriceText,
                discountPriceText = product.discountPriceText,
                price = null,
                originalPrice = null,
                discountPrice = null,
                categoryText = null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cancel Quote"
            )
        }
    }
}
@Composable
private fun NdjcPendingProductBar(
    product: NdjcProductCardUi,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp.dp * ChatTokens.BubbleMaxWidth)

    // ✅ Pending Attachment（待发送附件）统一壳子：白底 + 轻描边 + 圆角
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp12)
            )
            .border(
                width = NdjcCommonTokens.Dp.Dp1,
                color = MaterialTheme.colorScheme.outline.copy(alpha = NdjcCommonTokens.Alpha.a18),
                shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp12)
            )
            .padding(
                start = NdjcCommonTokens.Dp.Dp10,
                end = NdjcCommonTokens.Dp.Dp6,
                top = NdjcCommonTokens.Dp.Dp8,
                bottom = NdjcCommonTokens.Dp.Dp8
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ✅ 左侧竖条：用“中性灰”表示附件/待发送，不与“引用主色条”抢语义
        Box(
            modifier = Modifier
                .width(NdjcCommonTokens.Dp.Dp3)
                .fillMaxHeight()
                .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp999))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = NdjcCommonTokens.Alpha.a18))
        )

        Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))

        // ✅ 商品卡片本体不动：仅作为待发送预览
        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = maxBubbleWidth)
                .clip(RoundedCornerShape(ChatTokens.ProductCorner))
                .alpha(0.98f) // 轻微弱化：更像“嵌入态预览”，几乎不可见但更高级
        ) {
            NdjcCatalogItemCard(
                title = product.title,
                imageUrl = product.imageUrl,
                priceText = product.priceText,
                originalPriceText = product.originalPriceText,
                discountPriceText = product.discountPriceText,
                price = null,
                originalPrice = null,
                discountPrice = null,
                categoryText = null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp8))

        // ✅ 右侧动作区：X（弱动作）+ Send（主动作）
        Column(
            modifier = Modifier.width(NdjcCommonTokens.Dp.Dp32),
            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(NdjcCommonTokens.Dp.Dp32)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = NdjcCommonTokens.Alpha.a60)
                )
            }

            IconButton(
                onClick = onSend,
                modifier = Modifier.size(NdjcCommonTokens.Dp.Dp32)
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = BackButtonTokens.Background
                )
            }
        }
    }
}
@Composable
internal fun ShowcaseFavoritesScreen(
    state: ShowcaseFavoritesUiState,
    actions: ShowcaseFavoritesActions
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    NdjcUnifiedBackground(
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() }
        )
    ) {
        ShowcaseFavorites(
            uiState = state,
            actions = actions,
            modifier = Modifier.fillMaxSize()
        )
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NdjcConversationPageScaffold(
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    title: String = "",
    subtitle: String = "",
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    listState: androidx.compose.foundation.lazy.LazyListState? = null,
    listUserScrollEnabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        top = NdjcCommonTokens.Dp.Dp12,
        start = NdjcWhiteCardLayoutTokens.ScreenPadding,
        end = NdjcWhiteCardLayoutTokens.ScreenPadding,
        bottom = NdjcWhiteCardLayoutTokens.CardInnerPaddingVertical
    ),
    verticalItemSpacing: Dp = NdjcCommonTokens.Dp.Dp8,
    topBarActions: @Composable (androidx.compose.foundation.layout.RowScope.() -> Unit) = {},
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (showTopBar) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = HomeTokens.BackgroundGradientTop,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(
                                start = NdjcTopNavTokens.HorizontalPadding,
                                end = NdjcTopNavTokens.HorizontalPadding,
                                top = NdjcTopNavTokens.TopPadding
                            )
                            .height(NdjcTopNavTokens.ConversationHeaderBarHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.width(BackButtonTokens.Size),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Spacer(modifier = Modifier.size(BackButtonTokens.Size))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelectionMode) {
                                Text(
                                    text = "Selected $selectedCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a85),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp1)
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a85),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.width(BackButtonTokens.Size),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                content = topBarActions
                            )
                        }
                    }

                    Divider(
                        thickness = NdjcCommonTokens.Dp.Dp1,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = NdjcTopContentPadding)
            ) {
                HorizontalDivider(
                    thickness = NdjcCommonTokens.Dp.Dp1,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
            }
        }

        LazyColumn(
            state = listState ?: rememberLazyListState(),
            userScrollEnabled = listUserScrollEnabled,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = NdjcCommonTokens.Alpha.a30),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = NdjcCommonTokens.Dp.Dp16),
            verticalArrangement = Arrangement.spacedBy(verticalItemSpacing),
            contentPadding = contentPadding,
            content = content
        )

        if (footer != null) {
            footer()
        }
    }
}
//region 12 Announcements（UI-only）
@Composable
internal fun ShowcaseAnnouncementsScreen(
    state: ShowcaseAnnouncementsUiState,
    actions: ShowcaseAnnouncementsActions
) {
    BackHandler(enabled = true) {
        actions.onBack()
    }

    data class NdjcImagePreviewState(val images: List<String>, val startIndex: Int)
    var imagePreview by remember { mutableStateOf<NdjcImagePreviewState?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.focusedAnnouncementId, state.items) {
        val targetId = state.focusedAnnouncementId ?: return@LaunchedEffect
        val targetIndex = state.items.indexOfFirst { it.id == targetId }
        if (targetIndex < 0) return@LaunchedEffect

        listState.scrollToItem(targetIndex)
        actions.onOpenAnnouncement(targetId)
        actions.onConsumeFocusedAnnouncement()
    }

    NdjcUnifiedBackground(
        topNav = NdjcTopNavConfig(
            onBack = { actions.onBack() },
            onHome = { actions.onBackToHome() }
        )
    ) {
        NdjcConversationPageScaffold(
            showTopBar = true,
            title = "Announcements",
            subtitle = "Drafts and published updates",
            listState = listState,
            contentPadding = PaddingValues(
                top = NdjcCommonTokens.Dp.Dp12,
                bottom = NdjcCommonTokens.Dp.Dp16
            ),
            verticalItemSpacing = NdjcCommonTokens.Dp.Dp10
        ) {
            if (state.items.isEmpty() && !state.isLoading) {
                item(key = "announcements_empty") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(top = NdjcCommonTokens.Dp.Dp24),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No announcements yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                        )
                    }
                }
            }

            items(
                items = state.items,
                key = { it.id }
            ) { a: ShowcaseAnnouncementCard ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)
                ) {
                    NdjcConversationTimePill(timeText = a.timeText)

                    AnnouncementFeedCard(
                        card = a,
                        forceExpand = (state.focusedAnnouncementId == a.id),
                        onOpenAnnouncement = { actions.onOpenAnnouncement(a.id) },
                        onExpandAnnouncement = { actions.onTrackAnnouncementView(a.id) },
                        onOpenImagePreview = { url ->
                            actions.onOpenAnnouncementImage(a.id)
                            imagePreview = NdjcImagePreviewState(
                                images = listOf(url),
                                startIndex = 0
                            )
                        }
                    )
                }
            }

            item(key = "announcements_bottom_space") {
                Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp10))
            }
        }

        val st = imagePreview
        if (st != null && st.images.isNotEmpty()) {
            NdjcFullscreenImageViewerScreen(
                images = st.images,
                startIndex = st.startIndex,
                onDismiss = { imagePreview = null },
                onSave = {}
            )
        }
    }
}
@Composable
private fun NdjcConversationTimePill(
    timeText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NdjcCommonTokens.Dp.Dp8),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(ChatTokens.DatePillCorner),
            color = Color.White.copy(alpha = NdjcCommonTokens.Alpha.a70),
            tonalElevation = NdjcCommonTokens.Dp.Dp0,
            shadowElevation = NdjcCommonTokens.Dp.Dp0
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a45),
                modifier = Modifier.padding(
                    horizontal = ChatTokens.DatePillPaddingH,
                    vertical = ChatTokens.DatePillPaddingV
                )
            )
        }
    }
}
@Composable
internal fun ShowcaseAdminAnnouncementEdit(
    state: ShowcaseAnnouncementEditUiState,
    actions: ShowcaseAnnouncementEditActions
) {
    var pendingExitTarget by rememberSaveable { mutableStateOf<String?>(null) }

    fun requestExit(target: String) {
        if (state.hasUnsavedChanges) {
            pendingExitTarget = target
        } else {
            if (target == "home") {
                actions.onBackToHome()
            } else {
                actions.onBack()
            }
        }
    }

    NdjcUnifiedBackground(
        topNav = NdjcTopNavConfig(
            onBack = { requestExit("back") },
            onHome = { requestExit("home") }
        )
    ) {
        data class NdjcImagePreviewState(val images: List<String>, val startIndex: Int)
        var imagePreview by remember { mutableStateOf<NdjcImagePreviewState?>(null) }
        var showDeleteDraftConfirm by rememberSaveable { mutableStateOf(false) }
        var showPublishConfirm by rememberSaveable { mutableStateOf(false) }
        val previewCard = state.previewItem

        BackHandler(
            enabled = pendingExitTarget == null &&
                    previewCard == null &&
                    imagePreview == null &&
                    !showDeleteDraftConfirm &&
                    !showPublishConfirm
        ) {
            requestExit("back")
        }

        if (pendingExitTarget != null) {
            NdjcBaseDialog(
                onDismissRequest = { pendingExitTarget = null },
                title = "Discard unsaved changes?",
                message = "You have unsaved announcement changes. Leave this page and discard them?",
                confirmText = "Discard",
                dismissText = "Stay",
                onConfirmClick = {
                    val target = pendingExitTarget
                    pendingExitTarget = null
                    if (target == "home") {
                        actions.onBackToHome()
                    } else {
                        actions.onBack()
                    }
                },
                onDismissClick = { pendingExitTarget = null },
                destructiveConfirm = true
            )
        }

        if (state.previewVisible && previewCard != null) {
            Dialog(
                onDismissRequest = { actions.onDismissPreview() },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            actions.onDismissPreview()
                        }
                        .padding(horizontal = NdjcCommonTokens.Dp.Dp16),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { }
                    ) {
                        AnnouncementFeedCard(
                            card = previewCard,
                            onOpenAnnouncement = { },
                            onExpandAnnouncement = { },
                            onOpenImagePreview = { imageUrl ->
                                actions.onDismissPreview()
                                imagePreview = NdjcImagePreviewState(
                                    images = listOf(imageUrl),
                                    startIndex = 0
                                )
                            }
                        )
                    }
                }
            }
        }
        if (showDeleteDraftConfirm) {
            NdjcBaseDialog(
                onDismissRequest = { showDeleteDraftConfirm = false },
                title = "Delete selected drafts?",
                message = "This will permanently delete ${state.selectedIds.size} selected draft item(s).",
                confirmText = "Delete",
                dismissText = "Cancel",
                onConfirmClick = {
                    actions.onDeleteSelected()
                    showDeleteDraftConfirm = false
                },
                onDismissClick = { showDeleteDraftConfirm = false },
                destructiveConfirm = true
            )
        }

        if (showPublishConfirm) {
            NdjcBaseDialog(
                onDismissRequest = { showPublishConfirm = false },
                title = "Publish announcement?",
                message = "This will publish the current announcement immediately.",
                confirmText = "Publish",
                dismissText = "Cancel",
                onConfirmClick = {
                    actions.onPushNow()
                    showPublishConfirm = false
                },
                onDismissClick = { showPublishConfirm = false }
            )
        }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.statusMessage) {
            val message = state.statusMessage
            if (!message.isNullOrBlank()) {
                snackbarHostState.showSnackbar(message)
            }
        }

        var headerHeightPx by remember { mutableStateOf(0) }
        val density = LocalDensity.current
        val headerHeightDp = with(density) { headerHeightPx.toDp() }

        val headerTopPadding = NdjcTopContentPadding

        Box(modifier = Modifier.fillMaxSize()) {
            val listTop = headerTopPadding + headerHeightDp + NdjcCommonTokens.Dp.Dp10
            val items = state.draftItems

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
                horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
                contentPadding = PaddingValues(
                    top = listTop,
                    start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    bottom = NdjcWhiteCardLayoutTokens.CardInnerPaddingVertical
                )
            ) {
                items(
                    count = items.size,
                    key = { idx -> items[idx].id }
                ) { idx ->
                    val card = items[idx]
                    val selected = state.selectedIds.contains(card.id)

                    AnnouncementDraftCard(
                        card = card,
                        selected = selected,
                        onClick = { actions.onPreviewItem(card.id) },
                        onEditClick = { actions.onOpenItem(card.id) },
                        onToggleSelect = { actions.onToggleSelect(card.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item(key = "bottom_space") {
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp10))
                }
            }

            NdjcWhiteCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = headerTopPadding,
                        start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                        end = NdjcWhiteCardLayoutTokens.ScreenPadding
                    )
                    .onSizeChanged { headerHeightPx = it.height }
                    .zIndex(2f)
            ) {
                val maxChars = 200
                val body = state.bodyDraft
                val cover = state.coverDraftUrl

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val actionGap = NdjcCommonTokens.Dp.Dp10
                    val actionButtonWidth = (maxWidth - actionGap * 2) / 3

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                            ) {
                                Text(
                                    text = "Publish announcement",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Tap a draft card to preview it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a60)
                                )
                            }

                            Spacer(modifier = Modifier.width(NdjcCommonTokens.Dp.Dp12))

                            NdjcPrimaryActionButton(
                                text = "New",
                                onClick = actions.onStartNew,
                                enabled = state.canStartNew,
                                modifier = Modifier.width(actionButtonWidth)
                            )
                        }

                        if (state.composerExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)
                            ) {
                                Text(
                                    text = "Cover image *",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Black
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    BoxWithConstraints(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val cellSize = this.maxWidth

                                        NdjcSingleEditableImage(
                                            imageUrl = cover,
                                            enabled = true,
                                            onAddClick = { actions.onPickCover() },
                                            onRemoveClick = { actions.onRemoveCover() },
                                            onPreviewClick = {
                                                cover
                                                    ?.trim()
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?.let {
                                                        imagePreview = NdjcImagePreviewState(
                                                            images = listOf(it),
                                                            startIndex = 0
                                                        )
                                                    }
                                            },
                                            modifier = Modifier,
                                            size = cellSize,
                                            cornerRadius = NdjcCommonTokens.Dp.Dp16
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(2f),
                                        verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp6)
                                    ) {
                                        NdjcTextField(
                                            value = body,
                                            onValueChange = { s ->
                                                actions.onBodyChange(s.take(maxChars))
                                            },
                                            label = "Content",
                                            placeholder = "Write the announcement…",
                                            singleLine = false,
                                            minLines = 3
                                        )

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = "${body.length.coerceAtMost(maxChars)}/$maxChars",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(actionGap)
                        ) {
                            val deleteText =
                                if (state.selectedIds.isNotEmpty()) "Delete (${state.selectedIds.size})" else "Delete"

                            NdjcPrimaryActionButton(
                                text = deleteText,
                                onClick = { showDeleteDraftConfirm = true },
                                enabled = state.canDeleteSelected,
                                modifier = Modifier.weight(1f)
                            )

                            NdjcPrimaryActionButton(
                                text = "Save draft",
                                onClick = actions.onSaveDraft,
                                enabled = state.canSaveDraft,
                                isLoading = state.isSubmitting,
                                modifier = Modifier.weight(1f)
                            )

                            NdjcPrimaryActionButton(
                                text = "Publish",
                                onClick = { showPublishConfirm = true },
                                enabled = state.canPublish,
                                isLoading = state.isSubmitting,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a10),
                            thickness = NdjcCommonTokens.Dp.Dp1
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (state.selectedIds.isNotEmpty()) {
                                    "${state.selectedIds.size} selected"
                                } else {
                                    "No selection"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70),
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp8))
                                    .clickable(
                                        enabled = state.selectedIds.isNotEmpty(),
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        actions.onClearSelection()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val clearColor = if (state.selectedIds.isNotEmpty()) {
                                    BackButtonTokens.Background
                                } else {
                                    Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a30)
                                }

                                Text(
                                    text = "Clear",
                                    color = clearColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            val st = imagePreview
            if (st != null) {
                NdjcFullscreenImageViewerScreen(
                    images = st.images,
                    startIndex = st.startIndex,
                    onDismiss = { imagePreview = null }
                )
            }

            NdjcSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = NdjcSnackbarTokens.HorizontalPadding,
                        end = NdjcSnackbarTokens.HorizontalPadding,
                        bottom = NdjcSnackbarTokens.BottomPadding
                    )
            )

            NdjcBlockingProgressOverlay(
                visible = state.isBlockingInput,
                text = "Submitting announcement…"
            )
        }
    }
}
//endregion

@Composable
private fun FavSortChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999),
        tonalElevation = if (selected) NdjcCommonTokens.Dp.Dp2 else NdjcCommonTokens.Dp.Dp0,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = NdjcCommonTokens.Dp.Dp12, vertical = NdjcCommonTokens.Dp.Dp8),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowcaseFavorites(
    uiState: ShowcaseFavoritesUiState,
    actions: ShowcaseFavoritesActions,
    modifier: Modifier = Modifier
) {
    // ✅ 改成 Items 同款：底层列表滚动 + 顶层白卡覆盖
    var headerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeightDp = with(density) { headerHeightPx.toDp() }

    // 顶部白卡距离屏幕顶的间距：沿用 Items 的 token 体系
    val headerTopPadding = NdjcTopContentPadding
    val listTopPadding = headerTopPadding + headerHeightDp + NdjcCommonTokens.Dp.Dp10

    // ✅ Sort 选中态（沿用 Favorites 自己 state）
    val defaultSelected = uiState.sortMode == ShowcaseHomeSortMode.Default
    val lowHighSelected = uiState.sortMode == ShowcaseHomeSortMode.PriceAsc
    val highLowSelected = uiState.sortMode == ShowcaseHomeSortMode.PriceDesc

    val hasPriceFilter = (uiState.appliedMinPrice != null || uiState.appliedMaxPrice != null)
    val filterActive =
        uiState.filterRecommendedOnly ||
                uiState.filterOnSaleOnly ||
                hasPriceFilter

    val selectedColor = BackButtonTokens.Background

    // ✅ Back 悬浮固定（对齐 Items）
    Box(modifier = modifier.fillMaxSize()) {
        // ✅ Back/Home 已由 NdjcUnifiedBackground(topNav=...) 统一渲染；这里不再重复绘制
        // =========================
        // 底层：列表（可滑动），允许滑到白卡背后
        // =========================
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10),
            contentPadding = PaddingValues(
                top = listTopPadding,
                start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                bottom = NdjcWhiteCardLayoutTokens.CardInnerPaddingVertical
            )
        ) {
            if (uiState.items.isEmpty()) {
                item(key = "empty_state") {
                    Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp32))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)
                        ) {
                            Text(
                                text = "No saved items yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tap the save button on an item's details page to save it here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.items,
                    key = { it.dishId }
                ) { it ->
                    val dishId = it.dishId
                    val checked = uiState.selectedIds.contains(dishId)

                    val itemInteractionSource = remember { MutableInteractionSource() }
                    val itemPressed by itemInteractionSource.collectIsPressedAsState()
                    val itemScale by animateFloatAsState(
                        targetValue = if (itemPressed) 0.965f else 1f,
                        animationSpec = tween(durationMillis = 120),
                        label = "FavoritesCardScale"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                            },
                        shape = NdjcCatalogItemCardTokens.Shape,
                        color = Color.Transparent,
                        shadowElevation = NdjcCommonTokens.Dp.Dp0,
                        tonalElevation = NdjcCommonTokens.Dp.Dp0
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NdjcCatalogItemCardTokens.Shape)
                                .clickable(
                                    interactionSource = itemInteractionSource,
                                    indication = LocalIndication.current
                                ) {
                                    actions.onOpenDetail(dishId)
                                }
                        ) {
                            NdjcCatalogItemCard(
                                title = it.title,
                                imageUrl = it.imageUrl,
                                priceText = it.priceText,
                                originalPriceText = it.originalPriceText,
                                discountPriceText = it.discountPriceText,
                                categoryText = it.category,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { actions.onOpenDetail(dishId) },
                                trailingContent = {
                                    NdjcSelectionCheckbox(
                                        checked = checked,
                                        onCheckedChange = { actions.onToggleSelect(dishId) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // =========================
        // 顶层：固定头部白卡（不滑动）
        // =========================
        NdjcWhiteCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = headerTopPadding,
                    start = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    end = NdjcWhiteCardLayoutTokens.ScreenPadding,
                    bottom = NdjcCommonTokens.Dp.Dp10
                )
                .onSizeChanged { headerHeightPx = it.height }
                .zIndex(2f)
        ) {
            val hasSelection = uiState.selectedIds.isNotEmpty()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
            ) {

                val savedCount = uiState.items.size

// Title + subtitle + count (Items-style)
                Column(verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp4)) {
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Your saved items.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                    )
                    Text(
                        text = "$savedCount saved items",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                    )
                }

// Search (Items-style label)
                NdjcTextField(
                    value = uiState.query,
                    onValueChange = actions.onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = "Search saved"
                )

// ✅ Sort/Filter comes BEFORE batch actions (match Items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SortNavEqualItem(
                        text = "Default",
                        selected = defaultSelected,
                        selectedColor = selectedColor,
                        onClick = { actions.onSortModeChange(ShowcaseHomeSortMode.Default) },
                        modifier = Modifier.weight(1f)
                    )

                    SortNavEqualItem(
                        text = "Low–High",
                        selected = lowHighSelected,
                        selectedColor = selectedColor,
                        onClick = { actions.onSortModeChange(ShowcaseHomeSortMode.PriceAsc) },
                        modifier = Modifier.weight(1f)
                    )

                    SortNavEqualItem(
                        text = "High–Low",
                        selected = highLowSelected,
                        selectedColor = selectedColor,
                        onClick = { actions.onSortModeChange(ShowcaseHomeSortMode.PriceDesc) },
                        modifier = Modifier.weight(1f)
                    )

                    SortNavEqualItem(
                        text = "Filter",
                        selected = filterActive,
                        selectedColor = selectedColor,
                        onClick = { actions.onShowFilterMenuChange(true) },
                        modifier = Modifier.weight(1f)
                    )
                }

// ✅ Category before batch actions (match Items)
                CategoryChipsRow(
                    selectedCategory = uiState.selectedCategory,
                    manualCategories = uiState.categories,
                    onCategorySelected = actions.onCategorySelected,
                    modifier = Modifier.fillMaxWidth(),
                    showAllChip = true,
                    useOuterHorizontalPadding = false
                )

// Hint (Items-style)
                Text(
                    text = "Select items to delete or clear selection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a70)
                )

// ✅ Batch actions (ALWAYS visible; just enabled depends on selection)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
                ) {
                    NdjcPrimaryActionButton(
                        text = "Clear",
                        onClick = {
                            if (!hasSelection) return@NdjcPrimaryActionButton
                            actions.onClearSelection()
                        },
                        enabled = hasSelection,
                        modifier = Modifier.weight(1f)
                    )

                    NdjcPrimaryActionButton(
                        text = "Delete (${uiState.selectedIds.size})",
                        onClick = {
                            if (!hasSelection) return@NdjcPrimaryActionButton
                            actions.onDeleteSelected()
                        },
                        enabled = hasSelection,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // =========================
        // Filter Sheet（保留你原逻辑，不越界）
        // =========================
        if (uiState.showFilterMenu) {
            NdjcFilterBottomSheet(
                onDismissRequest = {
                    actions.onShowFilterMenuChange(false)
                },
                onClear = {
                    actions.onClearSortAndFilters()
                    actions.onClearPriceRange()
                    actions.onShowFilterMenuChange(false)
                },
                priceMinDraft = uiState.priceMinDraft,
                onPriceMinDraftChange = actions.onPriceMinDraftChange,
                priceMaxDraft = uiState.priceMaxDraft,
                onPriceMaxDraftChange = actions.onPriceMaxDraftChange,
                onApply = {
                    actions.onApplyPriceRange()
                    actions.onShowFilterMenuChange(false)
                }
            ) {
                NdjcToggleRow(
                    label = "Pick",
                    checked = uiState.filterRecommendedOnly,
                    onCheckedChange = actions.onFilterRecommendedOnlyChange,
                    modifier = Modifier.padding(vertical = NdjcCommonTokens.Dp.Dp6)
                )

                NdjcToggleRow(
                    label = "On sale",
                    checked = uiState.filterOnSaleOnly,
                    onCheckedChange = actions.onFilterOnSaleOnlyChange,
                    modifier = Modifier.padding(vertical = NdjcCommonTokens.Dp.Dp6)
                )
            }
        }
    }
}
@Composable
private fun FavoritesSortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp999),
        tonalElevation = if (selected) NdjcCommonTokens.Dp.Dp2 else NdjcCommonTokens.Dp.Dp0,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = NdjcCommonTokens.Dp.Dp12, vertical = NdjcCommonTokens.Dp.Dp8),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
//endregion

//region 12 BottomBar（UI-only）
// ------------------------- Bottom Bar (UI only) -------------------------

enum class ShowcaseBottomBarTab { Store, Chat, Favorites, Announcements }
@Composable
fun ShowcaseBottomBar(
    selected: ShowcaseBottomBarTab?,
    showChatDot: Boolean = false,
    showAnnouncementsDot: Boolean = false,
    onOpenStoreProfile: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenAnnouncements: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = BackButtonTokens.Background
    val topRoundedShape = RoundedCornerShape(
        topStart = NdjcCommonTokens.Dp.Dp16,
        topEnd = NdjcCommonTokens.Dp.Dp16,
        bottomStart = NdjcCommonTokens.Dp.Dp0,
        bottomEnd = NdjcCommonTokens.Dp.Dp0
    )

    NdjcSystemBars(
        color = Color.Transparent,
        darkIcons = true,
        key = "ShowcaseBottomBarSystemBars",
        navigationBarColor = Color.White,
        lightNavIcons = true,
        decorFitsSystemWindows = false
    )

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(NdjcCommonTokens.Dp.Dp0),
        tonalElevation = NdjcCommonTokens.Dp.Dp0,
        shadowElevation = NdjcCommonTokens.Dp.Dp0,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {

            HorizontalDivider(
                thickness = NdjcCommonTokens.Dp.Dp1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(
                        horizontal = NdjcCommonTokens.Dp.Dp12
                    ),
                horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NdjcBottomTabVertical(
                    text = "Store",
                    iconOutlined = Icons.Outlined.Storefront,
                    iconFilled = Icons.Filled.Storefront,
                    selected = (selected == ShowcaseBottomBarTab.Store),
                    brand = brand,
                    onClick = onOpenStoreProfile,
                    modifier = Modifier.weight(1f)
                )

                NdjcBottomTabVertical(
                    text = "Chat",
                    iconOutlined = Icons.Outlined.ChatBubbleOutline,
                    iconFilled = Icons.Filled.ChatBubble,
                    selected = (selected == ShowcaseBottomBarTab.Chat),
                    showDot = showChatDot,
                    brand = brand,
                    onClick = onOpenChat,
                    modifier = Modifier.weight(1f)
                )

                NdjcBottomTabVertical(
                    text = "Updates",
                    iconOutlined = Icons.Outlined.NotificationsNone,
                    iconFilled = Icons.Filled.Notifications,
                    selected = (selected == ShowcaseBottomBarTab.Announcements),
                    showDot = showAnnouncementsDot,
                    brand = brand,
                    onClick = onOpenAnnouncements,
                    modifier = Modifier.weight(1f)
                )

                NdjcBottomTabVertical(
                    text = "Saved",
                    iconOutlined = Icons.Outlined.BookmarkBorder,
                    iconFilled = Icons.Filled.Bookmark,
                    selected = (selected == ShowcaseBottomBarTab.Favorites),
                    brand = brand,
                    onClick = onOpenFavorites,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NdjcBottomTabVertical(
    text: String,
    iconOutlined: ImageVector,
    iconFilled: ImageVector,
    selected: Boolean,
    showDot: Boolean = false,
    brand: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fg = if (selected) brand else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
    val weight = if (selected) FontWeight.SemiBold else FontWeight.Medium
    val icon = if (selected) iconFilled else iconOutlined

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp14))
            .background(Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = NdjcCommonTokens.Dp.Dp8,
                vertical = NdjcCommonTokens.Dp.Dp2
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(NdjcCommonTokens.Dp.Dp22),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = fg,
                modifier = Modifier.size(NdjcCommonTokens.Dp.Dp22)
            )

            if (showDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (0).dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
            }
        }

        Spacer(modifier = Modifier.height(NdjcCommonTokens.Dp.Dp1))
        val labelStyle = MaterialTheme.typography.labelMedium.let { base ->
            base.copy(lineHeight = base.fontSize)
        }

        Text(
            text = text,
            color = fg,
            fontWeight = weight,
            style = labelStyle,
            maxLines = 1
        )
    }
}

@Composable
private fun StoreServicesEditor(
    enabled: Boolean,
    items: List<String>,
    onChange: (Int, String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit
) {
    var newValue by rememberSaveable { mutableStateOf("") }

    Text(
        text = "Business Scope",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp10))

    // 已添加项
    items.forEachIndexed { index, value ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp10)
        ) {
            NdjcTextField(
                value = value,
                onValueChange = { onChange(index, it) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                label = "Service",
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Remove",
                modifier = Modifier
                    .padding(top = NdjcCommonTokens.Dp.Dp4, bottom = NdjcCommonTokens.Dp.Dp8)
                    .clickable(enabled = enabled) { onRemove(index) },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    // 永远保留一行预留输入
    NdjcTextField(
        value = newValue,
        onValueChange = { newValue = it },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = "Add new service",
        singleLine = true
    )


    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        OutlinedButton(
            onClick = {
                onAdd(newValue)
                newValue = ""
            },
            enabled = enabled
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(NdjcCommonTokens.Dp.Dp6))
            Text("Add")
        }
    }

    Spacer(Modifier.height(NdjcCommonTokens.Dp.Dp12))
}
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseLoginPreview() {
    val state = ShowcaseLoginUiState(
        isLoading = false,
        loginError = "Wrong username or password",
        usernameDraft = "admin",
        passwordDraft = "1234",
        rememberMe = false,
        canLogin = true
    )

    val actions = ShowcaseLoginActions(
        onUsernameDraftChange = {},
        onPasswordDraftChange = {},
        onRememberMeChange = {},
        onLogin = { _, _ -> },
        onBackToHome = {}
    )

    MaterialTheme {
        ShowcaseLogin(
            uiState = state,
            actions = actions
        )
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseDishDetailPreview() {
    val state = ShowcaseDetailUiState(
        title = "Tomahawk steak",
        price = "9.99",
        discountPrice = "7.99",
        description = "Huge bone-in steak with garlic butter.",
        category = "Hot",
        isRecommended = true,
        isUnavailable = false,
        imagePreviewUrl = "demo://image"
    )
    val actions = ShowcaseDetailActions(
        onBack = {},
        onEdit = {},
        onOpenImage = { _ -> },
        onSavePreviewImage = {} // ✅ 补齐：全屏预览长按保存
    )

    MaterialTheme {
        ShowcaseDishDetail(
            uiState = state,
            actions = actions
        )
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseEditDishPreview() {
    val state = ShowcaseEditDishUiState(
        id = "",
        nameZh = "",
        nameEn = "",
        descriptionEn = "",
        category = "Hot",
        originalPrice = "",
        discountPrice = "",
        isRecommended = true,
        imageUrls = emptyList(),
        isSaving = false,
        statusMessage = null,
        isNew = true,
        errorMessage = null
    )


    val actions = ShowcaseEditDishActions(
        onBack = {},
        onNameChange = {},
        onPriceChange = {},
        onDiscountPriceChange = {},
        onDescriptionChange = {},
        onCategorySelected = {},
        onToggleRecommended = {},

        onToggleHidden = {},

        onPickImage = {},
        onRemoveImage = {},
        onSave = {},
        onDelete = {}
    )


    MaterialTheme {
        ShowcaseEditDish(
            uiState = state,
            actions = actions
        )
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseAdminPreview() {
    val demoDishes = listOf(
        ShowcaseHomeDish(
            id = "1",
            title = "Tomahawk steak",
            subtitle = "99",
            category = "Hot",
            price = 9.99,
            originalPrice = 9.99,
            discountPrice = null,
            isRecommended = true,
            isSoldOut = false
        ),
        ShowcaseHomeDish(
            id = "2",
            title = "Caesar salad",
            subtitle = "777",
            category = "Special",
            price = 12.50,
            originalPrice = 12.50,
            discountPrice = null,
            isRecommended = false,
            isSoldOut = true
        )
    )

    val state = ShowcaseAdminUiState(
        isLoading = false,
        statusMessage = "Categories preview",
        selectedCategory = null,
        manualCategories = listOf("Hot", "Special", "Dessert"),
        dishes = demoDishes,
        pendingDeleteDishId = null
    )



    val actions = ShowcaseAdminActions(
        onBack = {},
        onSelectCategory = { _ -> },
        onAddCategory = { _ -> },
        onDeleteCategory = { _ -> },
        onOpenMerchantChatList = {},
        onAddNewDish = {},
        onEditDish = { _ -> },
        onDeleteDish = { _ -> },
        onDismissPendingDelete = {},
        onConfirmPendingDelete = {}
    )


    MaterialTheme {
        ShowcaseAdmin(
            uiState = state,
            actions = actions
        )
    }
}
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseAdminItemsPreview() {
    val demoDishes = listOf(
        ShowcaseHomeDish(
            id = "1",
            title = "Tomahawk steak",
            subtitle = "99",
            category = "Hot",
            price = 9.99,
            originalPrice = 9.99,
            discountPrice = null,
            isRecommended = true,
            isSoldOut = false
        ),
        ShowcaseHomeDish(
            id = "2",
            title = "Caesar salad",
            subtitle = "777",
            category = "Special",
            price = 12.50,
            originalPrice = 12.50,
            discountPrice = null,
            isRecommended = false,
            isSoldOut = true
        )

    )

    val state = ShowcaseAdminUiState(
        isLoading = false,
        statusMessage = "Items preview",
        selectedCategory = null,
        manualCategories = listOf("Hot", "Special", "Dessert"),
        dishes = demoDishes,
        pendingDeleteDishId = null
    )

    val actions = ShowcaseAdminActions(
        onBack = {},
        onSelectCategory = { _ -> },
        onAddCategory = { _ -> },
        onDeleteCategory = { _ -> },

        // ✅ 新增：聊天列表入口（Preview 用空实现即可）
        onOpenMerchantChatList = {},

        onAddNewDish = {},
        onEditDish = { _ -> },
        onDeleteDish = { _ -> },
        onDismissPendingDelete = {},
        onConfirmPendingDelete = {}
    )


    MaterialTheme {
        ShowcaseAdminItems(
            uiState = state,
            actions = actions
        )
    }
}
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseStoreProfileViewPreview() {
    val state = ShowcaseStoreProfileUiState(
        canEdit = false,
        isEditing = false,

        // ✅ Brand
        title = "Acme Coffee",
        subtitle = "Specialty coffee & bakery",
        businessStatus = "Open",

        // ✅ Media
        coverUrl = "demo://image\n demo://image2\n demo://image3",
        logoUrl = "demo://logo",

        // ✅ About
        description = "Open daily. Fresh beans, pastries, and seasonal drinks. We also offer seasonal drinks and custom cakes.",

        // ✅ CTA / Contact

        // ✅ Location
        address = "123 Main St, City",
        hours = "Mon-Sun 08:00-20:00",
        mapUrl = "https://maps.google.com/?q=123+Main+St",


    )
    val actions = ShowcaseStoreProfileActions(
        onBack = {},
        onCancelEdit = {},
        onBusinessStatusChange = {},
        onSavePreviewImage = {}, // ✅ 补齐：全屏预览长按保存
        onSave = {}
        // 其他回调有默认值，不写也行
    )

    MaterialTheme {
        ShowcaseStoreProfileView(
            uiState = state,
            actions = actions
        )
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseAdminCategoriesPreview() {
    val demoDishes = listOf(
        ShowcaseHomeDish(
            id = "1",
            title = "Tomahawk steak",
            subtitle = "99",
            category = "Hot",
            price = 9.99,
            originalPrice = 9.99,
            discountPrice = null,
            isRecommended = true,
            isSoldOut = false
        )

    )

    val state = ShowcaseAdminUiState(
        isLoading = false,
        statusMessage = "Categories preview",
        selectedCategory = null,
        manualCategories = listOf("Hot", "Special", "Dessert"),
        dishes = demoDishes,
        pendingDeleteDishId = null
    )

    val actions = ShowcaseAdminActions(
        onBack = {},
        onSelectCategory = { _ -> },
        onAddCategory = { _ -> },
        onDeleteCategory = { _ -> },
        onOpenMerchantChatList = {},
        onAddNewDish = {},
        onEditDish = { _ -> },
        onDeleteDish = { _ -> },
        onDismissPendingDelete = {},
        onConfirmPendingDelete = {}
    )

    MaterialTheme {
        ShowcaseAdminCategories(
            uiState = state,
            actions = actions
        )
    }
}
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun DeleteConfirmDialogPreview() {
    MaterialTheme {
        DeleteConfirmDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseHomePreview() {
    val demoDishes = listOf(
        ShowcaseHomeDish(
            id = "1",
            title = "Tomahawk steak",
            subtitle = "",
            category = "Hot",
            price = 9.99,
            originalPrice = 9.99,
            discountPrice = null,
            isRecommended = true,
            isSoldOut = false
        ),
        ShowcaseHomeDish(
            id = "2",
            title = "Caesar salad",
            subtitle = "",
            category = "Special",
            price = 9.90,              // ✅ 折后优先的 price（兼容字段）
            originalPrice = 12.50,
            discountPrice = 9.90,      // ✅ 折后价
            isRecommended = false,
            isSoldOut = false
        ),
        ShowcaseHomeDish(
            id = "3",
            title = "French lamb chops",
            subtitle = "",
            category = "Dessert",
            price = 18.88,
            originalPrice = 18.88,
            discountPrice = null,
            isRecommended = true,
            isSoldOut = true
        )

    )

    val state = ShowcaseHomeUiState(
        dishes = demoDishes,
        selectedCategory = null,
        manualCategories = listOf("Hot", "Special", "Dessert"),
        isLoading = false,
        statusMessage = "Loaded from preview data"
    )

    val actions = ShowcaseHomeActions(
        onRefresh = {},
        onCategorySelected = {},
        onDishSelected = {},
        onProfileClick = {}
    )

    MaterialTheme {
        ShowcaseHome(
            uiState = state,
            actions = actions
        )
    }
}
@Preview(showBackground = true, showSystemUi = false)
@Composable
internal fun ShowcaseChatThreadPreview() {
    val demo = ShowcaseChatUiState(
        title = "Chat",
        subtitle = "Store support",
        isConnecting = false,
        isRefreshing = false,
        isSending = false,
        errorMessage = null,
        conversationId = "demo_convo",
        draftText = "Hello",
        messages = listOf(
            ShowcaseChatMessageUi(
                id = "m1",
                direction = ShowcaseChatDirection.Incoming,
                text = "Hi, how can we help?",
                status = ShowcaseChatSendStatus.Idle
            ),
            ShowcaseChatMessageUi(
                id = "m2",
                direction = ShowcaseChatDirection.Outgoing,
                text = "I'd like to ask about today's special.",
                status = ShowcaseChatSendStatus.Sent
            ),
            ShowcaseChatMessageUi(
                id = "m3",
                direction = ShowcaseChatDirection.Outgoing,
                text = "This one failed (tap retry).",
                status = ShowcaseChatSendStatus.Failed
            )
        )
    )

    ShowcaseChatThread(
        uiState = demo,
        actions = ShowcaseChatActions(
            onBack = {},
            onDraftChange = {},
            onSend = {},
            onRetry = {},
            onRefresh = {}
        ),
        modifier = Modifier
    )
}
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseStoreProfileEditPreview() {
    val state = ShowcaseStoreProfileUiState(
        canEdit = true,
        isEditing = true,

        // 展示数据（非必须，但建议给点值）
        title = "Acme Coffee",
        subtitle = "Specialty coffee & bakery",
        description = "Open daily. Fresh beans, pastries, and seasonal drinks.",

        address = "123 Main St, City",
        hours = "Mon-Sun 08:00-20:00",
        mapUrl = "https://maps.google.com/?q=123+Main+St",

        // 编辑草稿（编辑页使用 draft 字段）
        draftTitle = "Acme Coffee (Draft)",
        draftSubtitle = "Bakery & Coffee",
        draftDescription = "Draft description…",
        draftAddress = "123 Main St, City",
        draftHours = "Mon-Sun 08:00-20:00",
        draftMapUrl = "https://maps.google.com/?q=123+Main+St",

        isSaving = false
    )
    val actions = ShowcaseStoreProfileActions(
        onBack = {},
        onCancelEdit = {},
        onBusinessStatusChange = {},
        onSavePreviewImage = {}, // ✅ 补齐：全屏预览长按保存
        onSave = {}
        // 其他回调都有默认值，不写也行
    )

    MaterialTheme {
        ShowcaseStoreProfileEdit(
            uiState = state,
            actions = actions
        )
    }
}
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseChatSearchResultsPreview() {

    // 构造假数据（仅用于 Preview）
    val fakeMessages = listOf(
        com.ndjc.feature.showcase.ShowcaseChatMessageUi(
            id = "1",
            direction = com.ndjc.feature.showcase.ShowcaseChatDirection.Outgoing,
            text = "This is a sample chat message",
            timeText = "11:20"
        ),
        com.ndjc.feature.showcase.ShowcaseChatMessageUi(
            id = "2",
            direction = com.ndjc.feature.showcase.ShowcaseChatDirection.Incoming,
            text = "Chat message containing the keyword",
            timeText = "11:21"
        ),
        com.ndjc.feature.showcase.ShowcaseChatMessageUi(
            id = "3",
            direction = com.ndjc.feature.showcase.ShowcaseChatDirection.Outgoing,
            text = "Another message containing the keyword",
            timeText = "11:22"
        )
    )


    val fakeState = com.ndjc.feature.showcase.ShowcaseChatUiState(
        messages = fakeMessages,
        findQuery = "test",
        findMatchIds = listOf("2", "3"),
        isSearchResults = true
    )

    val fakeActions = com.ndjc.feature.showcase.ShowcaseChatActions()

    ShowcaseChatSearchResults(
        uiState = fakeState,
        actions = fakeActions
    )
}


@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseChatMediaPreview() {
    val msgWithImages = "See photos: ⟪I⟫https://picsum.photos/seed/ndjc1/800/600|https://picsum.photos/seed/ndjc2/800/600⟪/I⟫"
    val demo = ShowcaseChatUiState(
        title = "Chat Media",
        subtitle = "Demo conversation",
        isConnecting = false,
        isRefreshing = false,
        isSending = false,
        errorMessage = null,
        conversationId = "demo_convo",
        draftText = "",
        messages = listOf(
            ShowcaseChatMessageUi(
                id = "m1",
                direction = ShowcaseChatDirection.Incoming,
                text = msgWithImages,
                status = ShowcaseChatSendStatus.Idle
            ),
            ShowcaseChatMessageUi(
                id = "m2",
                direction = ShowcaseChatDirection.Outgoing,
                text = "Another image ⟪I⟫https://picsum.photos/seed/ndjc3/800/600⟪/I⟫",
                status = ShowcaseChatSendStatus.Sent
            )
        )
    )

    MaterialTheme {
        ShowcaseChatMedia(
            uiState = demo,
            actions = ShowcaseChatMediaActions(
                onBack = {},
                onSavePreviewImage = { _ -> }
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseChangePasswordPreview() {
    val state = ShowcaseChangePasswordUiState(
        current = "old_password",
        next = "new_password",
        confirm = "new_password",
        isSaving = false,
        error = null,
        success = null
    )

    val actions = ShowcaseChangePasswordActions(
        onCurrentChange = {},
        onNextChange = {},
        onConfirmChange = {},
        onBack = {},
        onSubmit = {}
    )

    // 说明：ShowcaseChangePassword 本身是“内容组件”（不负责铺满屏幕）。
    // 真实页面通常会由外层 Screen/Scaffold 负责 fillMaxSize + 背景。
    // Preview 里需要手动铺满，否则会出现“内容很小、周围一圈空/黑”的效果。
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ShowcaseChangePassword(
                state = state,
                actions = actions,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(name = "BottomBar", showBackground = true, showSystemUi = false, backgroundColor = 0xFFFFFFFF)
@Composable
fun ShowcaseBottomBarPreview() {
    MaterialTheme {
        Surface {
            Box(modifier = Modifier.fillMaxSize()) {
                ShowcaseBottomBar(
                    selected = ShowcaseBottomBarTab.Chat,
                    onOpenStoreProfile = {},
                    onOpenChat = {},
                    onOpenFavorites = {},
                    onOpenAnnouncements = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}


@Preview(name = "MerchantChatList", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun ShowcaseMerchantChatListScreenPreview() {
    val state = ShowcaseUiState(
        merchantChatListRefreshing = false,
        merchantChatThreads = emptyList()
    )

    val actions = ShowcaseMerchantChatListActions(
        onBack = {},
        onRefresh = {},
        onOpenChatSearch = {},
        onOpenThread = { _, _ -> },
        onTogglePin = { _, _ -> },
        onMarkRead = { _ -> },
        onDeleteThread = { _ -> },
        onRenameThread = { _, _ -> }
    )

    MaterialTheme {
        Surface {
            ShowcaseMerchantChatListScreen(
                state = state,
                actions = actions
            )
        }
    }
}

@Preview(name = "FavoritesScreen", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun ShowcaseFavoritesScreenPreview() {
    val state = ShowcaseFavoritesUiState(
        query = "",
        items = emptyList(),
        categories = listOf("Hot", "Special", "Dessert"),
        selectedCategory = null,
        selectedIds = emptySet(),
        sortMode = ShowcaseHomeSortMode.Default,
        filterRecommendedOnly = false,
        filterOnSaleOnly = false,
        priceMinDraft = "",
        priceMaxDraft = "",
        appliedMinPrice = null,
        appliedMaxPrice = null,
        showPriceMenu = false,
        showSortMenu = false,
        showFilterMenu = false
    )

    val actions = ShowcaseFavoritesActions(
        onBack = {},
        onQueryChange = {},
        onOpenDetail = {},
        onToggleSelect = { _ -> },
        onDeleteSelected = {},
        onClearSelection = {},
        onClearSortAndFilters = {},
        onCategorySelected = {},
        onSortModeChange = {},
        onFilterRecommendedOnlyChange = {},
        onFilterOnSaleOnlyChange = {},
        onPriceMinDraftChange = {},
        onPriceMaxDraftChange = {},
        onShowPriceMenuChange = {},
        onShowSortMenuChange = {},
        onShowFilterMenuChange = {},
        onApplyPriceRange = {},
        onClearPriceRange = {}
    )

    MaterialTheme {
        Surface {
            ShowcaseFavoritesScreen(
                state = state,
                actions = actions
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseAnnouncementsPreview() {
    ShowcaseAnnouncementsScreen(
        state = ShowcaseAnnouncementsUiState(
            items = listOf(
                ShowcaseAnnouncementCard(
                    id = "ann_1",
                    coverUrl = null,
                    bodyPreview = "Weekend promotion now live. Fresh items and limited offers available today.",
                    bodyText = "Weekend promotion now live. Fresh items and limited offers available today.",
                    timeText = "2026-03-05 18:20",
                    viewCount = 3
                ),
                ShowcaseAnnouncementCard(
                    id = "ann_2",
                    coverUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e",
                    bodyPreview = "New seasonal products have arrived. Tap in store for details and availability.",
                    bodyText = "New seasonal products have arrived. Tap in store for details and availability.",
                    timeText = "2026-03-04 09:10",
                    viewCount = 12
                )
            ),
            isLoading = false,
            statusMessage = "2 announcements"
        ),
        actions = ShowcaseAnnouncementsActions(
            onBackToHome = {},
            onBack = {},
            onRefresh = {},
            onOpenAnnouncement = {}
        )
    )
}
@Composable
private fun AnnouncementFeedCard(
    card: ShowcaseAnnouncementCard,
    forceExpand: Boolean = false,
    onOpenAnnouncement: () -> Unit,
    onExpandAnnouncement: () -> Unit,
    onOpenImagePreview: (String) -> Unit
) {
    var expanded by rememberSaveable(card.id) { mutableStateOf(false) }
    val hasBody = remember(card.bodyText) { card.bodyText.trim().isNotBlank() }

    LaunchedEffect(forceExpand) {
        if (forceExpand) {
            expanded = true
        }
    }

    NdjcWhiteCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = NdjcCommonTokens.Dp.Dp8,
            vertical = NdjcCommonTokens.Dp.Dp8
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            if (!card.coverUrl.isNullOrBlank()) {
                NdjcShimmerImage(
                    imageUrl = card.coverUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp16))
                        .clickable { onOpenImagePreview(card.coverUrl) },
                    placeholderCornerRadius = NdjcCommonTokens.Dp.Dp16,
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(NdjcCommonTokens.Dp.Dp16))
                        .background(Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a06))
                        .clickable { onOpenAnnouncement() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No cover image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a45)
                    )
                }
            }

            if (hasBody) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NdjcCommonTokens.Dp.Dp8),
                    verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp0)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = NdjcCommonTokens.Dp.Dp8),
                        thickness = NdjcCommonTokens.Dp.Dp1,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a04)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = NdjcCommonTokens.Dp.Dp6),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${card.viewCount} views",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .size(NdjcCommonTokens.Dp.Dp24)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a05))
                                .clickable {
                                    val nextExpanded = !expanded
                                    expanded = nextExpanded
                                    if (nextExpanded) {
                                        onExpandAnnouncement()
                                    } else {
                                        onOpenAnnouncement()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse announcement" else "Expand announcement",
                                tint = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a65)
                            )
                        }
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = NdjcCommonTokens.Dp.Dp8,
                                    bottom = NdjcCommonTokens.Dp.Dp8
                                ),
                            verticalArrangement = Arrangement.spacedBy(NdjcCommonTokens.Dp.Dp8)
                        ) {
                            Text(
                                text = card.bodyText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black.copy(alpha = 0.78f),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NdjcCommonTokens.Dp.Dp8)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = NdjcCommonTokens.Dp.Dp8),
                        thickness = NdjcCommonTokens.Dp.Dp1,
                        color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a04)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = NdjcCommonTokens.Dp.Dp6),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${card.viewCount} views",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black.copy(alpha = NdjcCommonTokens.Alpha.a55)
                        )
                    }
                }
            }
        }
    }

}
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ShowcaseAdminAnnouncementEditPreview() {
    ShowcaseAdminAnnouncementEdit(
        state = ShowcaseAnnouncementEditUiState(
            composerExpanded = false,
            canStartNew = true,
            canDeleteSelected = false,
            canSaveDraft = false,
            canPublish = false,
            coverDraftUrl = null,
            bodyDraft = "",
            editingId = null,
            errorMessage = null,
            successMessage = "Saved as draft.",
            draftItems = listOf(
                ShowcaseAnnouncementCard(
                    id = "ann_1",
                    coverUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e",
                    bodyPreview = "Weekend promotion now live. Fresh items and limited offers available today.",
                    bodyText = "Weekend promotion now live. Fresh items and limited offers available today.",
                    timeText = "2026-03-05 18:20",
                    viewCount = 0
                )
            ),
            selectedIds = emptySet(),
            previewItem = null,
            previewVisible = false
        ),
        actions = ShowcaseAnnouncementEditActions(
            onBackToHome = {},
            onBack = {},
            onStartNew = {},
            onPickCover = {},
            onRemoveCover = {},
            onOpenCoverPreview = {},
            onBodyChange = {},
            onSaveDraft = {},
            onPushNow = {},
            onOpenItem = {},
            onPreviewItem = {},
            onDismissPreview = {},
            onToggleSelect = {},
            onClearSelection = {},
            onDeleteSelected = {}
        )
    )
}

//endregion