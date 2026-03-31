package com.ndjc.feature.showcase


/**
 * NDJC Showcase v1 UI Contract (Frozen)
 *
 * 目标：
 * 1) contracts 模块保持“纯 Kotlin”，不依赖 Compose / Android。
 * 2) UI 包负责把这些 State/Actions 渲染成 Composable。
 * 3) 逻辑模块只负责产出 ShowcaseUiModel（screen + 各页面 state/actions）。
 *
 * 注意：这里不引入 @Composable，不依赖 androidx.compose.*。
 */

// ------------------------- Home -------------------------
enum class ShowcaseHomeSortMode {
    Default,    // Home：新上传靠上；Favorites：新收藏靠上
    PriceAsc,   // 价格低 → 高
    PriceDesc   // 价格高 → 低
}

/**
 * Store Profile (Business Card)
 * - 纯 Kotlin：不依赖 Android Uri/Intent
 * - 逻辑模块负责：本地缓存 + 云端读写 + 写回 uiState
 * - UI 包负责：展示 + 编辑入口 + 表单渲染
 */
data class ShowcaseStoreProfile(
    val displayName: String = "",
    val tagline: String = "",
    val phone: String = "",
    val address: String = "",
    val businessHours: String = "",
    val websiteUrl: String = "",
    val mapUrl: String = ""
)

data class ShowcaseStoreProfileDraft(
    val displayName: String = "",
    val tagline: String = "",
    val phone: String = "",
    val address: String = "",
    val businessHours: String = "",
    val websiteUrl: String = "",
    val mapUrl: String = "",
    val isDirty: Boolean = false
)

data class ShowcaseCloudStatusUi(
    val storeId: String = "",
    val planLabel: String = "",
    val statusLabel: String = "",
    val serviceEndAt: String = "",
    val deleteAt: String = "",
    val canWrite: Boolean = true
)

data class ShowcaseHomeDish(
    val clickCount: Int = 0,
    val id: String,
    val title: String,
    val subtitle: String?,
    val category: String?,

    /**
     * 兼容字段：旧 UI/旧排序可能仍读取 price。
     * 约定：price = (discountPrice ?: originalPrice)
     */
    val price: Double,

    /** 原价（展示用） */
    val originalPrice: Double,
    /** 折后价（展示用；null 表示无折扣） */
    val discountPrice: Double? = null,

    val isRecommended: Boolean,
    val isSoldOut: Boolean,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,

    val imagePreviewUrl: String? = null
)

data class ShowcaseHomeUiState(
    val dishes: List<ShowcaseHomeDish> = emptyList(),
    val selectedCategory: String? = null,
    val manualCategories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null,

    // ✅ 店铺名片（展示用）
    val storeProfile: ShowcaseStoreProfile? = null,

    // ✅ P0: search / sort / filters
    val searchQuery: String = "",
    val sortMode: ShowcaseHomeSortMode = ShowcaseHomeSortMode.Default,
    val filterRecommendedOnly: Boolean = false,
    val filterOnSaleOnly: Boolean = false,

    // ✅ P0+: price range（VM 解析并应用；UI 只负责输入）
    val priceMinDraft: String = "",
    val priceMaxDraft: String = "",
    val appliedMinPrice: Int? = null,
    val appliedMaxPrice: Int? = null,

    val allTags: List<String> = emptyList(),
    val selectedTags: List<String> = emptyList(),

    // ✅ 菜单显隐由 VM 托管
    val showSortMenu: Boolean = false,
    val showFilterMenu: Boolean = false,
    val showPriceMenu: Boolean = false
)
data class ShowcaseHomeActions(
    val onRefresh: () -> Unit,
    val onCategorySelected: (String?) -> Unit,
    val onDishSelected: (String) -> Unit,

    val onProfileClick: () -> Unit,

    val onOpenStoreProfileView: () -> Unit = {},
    val onOpenChat: () -> Unit = {},

    val onSearchQueryChange: (String) -> Unit = {},
    val onToggleTag: (String) -> Unit = {},
    val onClearTags: () -> Unit = {},
    val onSortModeChange: (ShowcaseHomeSortMode) -> Unit = {},
    val onFilterRecommendedOnlyChange: (Boolean) -> Unit = {},
    val onFilterOnSaleOnlyChange: (Boolean) -> Unit = {},
    val onSelectedTagsChange: (List<String>) -> Unit = {},
    val onClearSortAndFilters: () -> Unit = {},
    val onClearAll: () -> Unit = {},

    val onShowSortMenuChange: (Boolean) -> Unit = {},
    val onShowFilterMenuChange: (Boolean) -> Unit = {},

    // ✅ price range（UI 只传字符串；VM 解析、应用、纠错）
    val onPriceMinDraftChange: (String) -> Unit = {},
    val onPriceMaxDraftChange: (String) -> Unit = {},
    val onApplyPriceRange: () -> Unit = {},
    val onClearPriceRange: () -> Unit = {},
    val onShowPriceMenuChange: (Boolean) -> Unit = {}
)

// ------------------------- Merchant Chat List -------------------------
data class ShowcaseMerchantChatListActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onOpenThread: (String, String) -> Unit = { _, _ -> },
    val onOpenChatSearch: () -> Unit = {},
    val onDeleteThread: (String) -> Unit = {},
    val onTogglePin: (String, Boolean) -> Unit = { _, _ -> },
    val onMarkRead: (String) -> Unit = {},
    val onRenameThread: (threadId: String, newName: String) -> Unit = { _, _ -> }
)


// ------------------------- Chat -------------------------
/**
 * ✅ 商品分享（从商品详情页带到 Chat 页）
 * - dishId  : 商品ID
 * - title   : 商品标题
 * - price   : 价格展示文案（UI 直接展示，不做计算）
 * - imageUrl: 首图（可为空）
 */
data class ShowcaseChatProductShare(
    val dishId: String,
    val title: String,
    val price: String,
    val imageUrl: String? = null,
)


data class ShowcaseChatActions(
    val onUseProductCardAsPending: (ShowcaseChatProductShare) -> Unit = { _ -> },
    val onJumpToMessage: (messageId: String) -> Unit = {},
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onDraftChange: (String) -> Unit = {},
    val onSend: () -> Unit = {},
    val onRetry: (String) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onQuoteMessage: (String) -> Unit = {},
    val onCancelQuote: () -> Unit = {},
    val onEnterSelection: (String) -> Unit = {},
    val onToggleSelection: (String) -> Unit = {},
    val onExitSelection: () -> Unit = {},
    val onDeleteMessage: (String) -> Unit = {},
    val onDeleteSelected: () -> Unit = {},
    val onOpenSearchResults: () -> Unit = {},
    val onCloseSearchResults: () -> Unit = {},
    val onOpenMediaGallery: () -> Unit = {},
    val onJumpToFoundMessage: (String) -> Unit = {},
    val onOpenThreadFromSearch: (conversationId: String, messageId: String?) -> Unit = { _, _ -> },
    val onTogglePinned: () -> Unit = {},
    val onOpenFind: () -> Unit = {},
    val onCloseFind: () -> Unit = {},
    val onFindQueryChange: (String) -> Unit = {},
    val onFindNext: () -> Unit = {},
    val onFindPrev: () -> Unit = {},
    val onPickImages: () -> Unit = {},
    val onOpenCamera: () -> Unit = {},
    val onRemoveDraftImage: (String) -> Unit = {},
    val onSavePreviewImage: (String) -> Unit = {},
    val onSendPendingProduct: () -> Unit = {},
    val onClearPendingProduct: () -> Unit = {},
    val onOpenProductDetail: (dishId: String) -> Unit = {},
    val buildProductClipboardPayload: (ShowcaseChatProductShare) -> String = { _ -> "" },
    )
data class ShowcaseChatMediaActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSavePreviewImage: (String) -> Unit = {}
)

// ------------------------- Login -------------------------
data class ShowcaseLoginUiState(
    val isLoading: Boolean = false,
    val loginError: String? = null,

    // ✅ drafts：由 VM 持有，UI 不再 remember
    val usernameDraft: String = "",
    val passwordDraft: String = "",

    // ✅ 自动登录（默认不勾选）
    val rememberMe: Boolean = false,

    // ✅ enabled 门控：由 VM 计算后透出
    val canLogin: Boolean = true
)
data class ShowcaseLoginActions(
    // ✅ drafts change：UI -> VM
    val onUsernameDraftChange: (String) -> Unit,
    val onPasswordDraftChange: (String) -> Unit,

    // ✅ 自动登录勾选
    val onRememberMeChange: (Boolean) -> Unit,

    val onLogin: (String, String) -> Unit,
    val onBackToHome: () -> Unit
)

// ------------------------- Admin -------------------------

data class ShowcaseAdminUiState(
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val cloudStatus: ShowcaseCloudStatusUi? = null,
    val itemsSortMode: ShowcaseHomeSortMode = ShowcaseHomeSortMode.Default,
    val itemsSortAscending: Boolean = true,
    val itemsSearchQuery: String = "",
    val filterRecommended: Boolean = false,
    val filterHiddenOnly: Boolean = false,
    val filterDiscountOnly: Boolean = false,

    // ✅ AdminItems：price range（VM 解析并应用；UI 只负责输入）
    val priceMinDraft: String = "",
    val priceMaxDraft: String = "",
    val appliedMinPrice: Int? = null,
    val appliedMaxPrice: Int? = null,

    val selectedCategory: String? = null,
    val manualCategories: List<String> = emptyList(),
    val dishes: List<ShowcaseHomeDish> = emptyList(),
    val pendingDeleteDishId: String? = null,
    val selectedDishIds: Set<String> = emptySet(),

    // ✅ 店铺信息（展示 + 编辑草稿 + 保存状态）
    val storeProfile: ShowcaseStoreProfile? = null,
    val storeProfileDraft: ShowcaseStoreProfileDraft? = null,
    val isSavingStoreProfile: Boolean = false,
    val storeProfileSaveError: String? = null,
    val storeProfileSaveSuccess: Boolean = false,

    // ✅ 同步概览（VM 内已有 pendingSyncCount / syncOverviewState / syncErrorMessage 这类状态）
    val pendingSyncCount: Int = 0,
    val syncErrorMessage: String? = null,
    val syncOverviewState: ShowcaseSyncOverviewState = ShowcaseSyncOverviewState.Idle,
    // ✅ Admin credentials 草稿（UI 包不再 remember）
    val adminUsernameDraft: String = "",
    val adminPasswordDraft: String = "",

// ✅ AdminCategories：删除分类弹窗状态（UI 包不再 remember）
    val pendingDeleteCategory: String? = null,
    val cannotDeleteCategory: String? = null,

    )


enum class ShowcaseSyncOverviewState {
    Idle, HasPending, Syncing, Failed
}


data class ShowcaseAdminActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit,
    val onLogout: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onItemsSortModeChange: (ShowcaseHomeSortMode) -> Unit = {},
    val onItemsSearchQueryChange: (String) -> Unit = {},
    val onClearItemsSearchQuery: () -> Unit = {},
    val onItemsFilterRecommendedChange: (Boolean) -> Unit = {},
    val onItemsFilterHiddenOnlyChange: (Boolean) -> Unit = {},
    val onItemsFilterDiscountOnlyChange: (Boolean) -> Unit = {},
    val onPriceMinDraftChange: (String) -> Unit = {},
    val onPriceMaxDraftChange: (String) -> Unit = {},
    val onApplyPriceRange: () -> Unit = {},
    val onClearPriceRange: () -> Unit = {},
    val onSelectCategory: (String?) -> Unit,
    val onAddCategory: (String) -> Unit,
    val onDeleteCategory: (String) -> Unit,
    val onRenameCategory: (oldName: String, newName: String) -> Unit = { _, _ -> },
    val onOpenItemsManager: () -> Unit = {},
    val onOpenCategoriesManager: () -> Unit = {},
    val onOpenStoreProfile: () -> Unit = {},
    val onOpenChangePassword: () -> Unit = {},
    val onOpenMerchantChatList: () -> Unit,
    val onAddNewDish: () -> Unit,
    val onEditDish: (dishId: String) -> Unit,
    val onDeleteDish: (dishId: String) -> Unit,
    val onToggleSelectDish: (dishId: String) -> Unit = {},
    val onClearSelectedDishes: () -> Unit = {},
    val onDeleteSelectedDishes: () -> Unit = {},
    val onDismissPendingDelete: () -> Unit,
    val onConfirmPendingDelete: () -> Unit,
    val onRetryPendingSync: () -> Unit = {},
    val onAdminUsernameDraftChange: (String) -> Unit = {},
    val onAdminPasswordDraftChange: (String) -> Unit = {},
    val onSaveAdminCredentials: () -> Unit = {},
    val onSetAdminCredentials: (username: String, password: String) -> Unit = { _, _ -> },
    val onRequestDeleteCategory: (String) -> Unit = {},
    val onDismissCategoryDeleteDialogs: () -> Unit = {},
    val onConfirmPendingDeleteCategory: () -> Unit = {},
    val onOpenAnnouncementPublisher: () -> Unit = {},
)
// ------------------------- Edit Dish -------------------------

data class ShowcaseEditDishUiState(
    val id: String = "",
    val nameZh: String = "",
    val nameEn: String = "",
    val descriptionEn: String = "",
    val category: String? = null,
    val availableCategories: List<String> = emptyList(),
    val originalPrice: String = "",
    val discountPrice: String = "",
    val isRecommended: Boolean = false,
    val isHidden: Boolean = false,
    val imageUrls: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isBlocking: Boolean = false,
    val statusMessage: String? = null,
    val isNew: Boolean = false,
    val errorMessage: String? = null,
    val isDiscountInvalidNumber: Boolean = false,
    val isDiscountGEPrice: Boolean = false,
    val discountErrorText: String? = null,
    val canSave: Boolean = true,
    val canAddImageSlot: Boolean = true,
    val maxImages: Int = 9,
    val hasUnsavedChanges: Boolean = false
)

data class ShowcaseEditDishActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onPriceChange: (String) -> Unit,
    val onDiscountPriceChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onCategorySelected: (String?) -> Unit,
    val onToggleRecommended: (Boolean) -> Unit,
    val onToggleHidden: (Boolean) -> Unit = {},
    val onPickImage: () -> Unit,
    val onImagePicked: (uriString: String) -> Unit = {},
    val onRemoveImage: (String) -> Unit,
    val onMoveImage: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    val onSave: () -> Unit,
    val onDelete: (() -> Unit)? = null,
    val onDismissError: () -> Unit = {}
)
// ------------------------- Detail -------------------------

data class ShowcaseDetailUiState(
    val dishId: String = "",              // ✅ 新增：当前详情对应的 dishId（UI 只读）
    val isFavorite: Boolean = false,      // ✅ 新增：是否已收藏（UI 只读）
    val title: String = "",
    val subtitle: String? = null,
    val price: String = "",
    val discountPrice: String? = null,
    val description: String = "",
    val category: String? = null,
    val isRecommended: Boolean = false,
    val isUnavailable: Boolean = false,
    val imagePreviewUrl: String? = null,
    val imageUrls: List<String> = emptyList(),

    val currentImageIndex: Int = 0,

// Derived by logic module (UI should not clamp)
    val safeImageIndex: Int = 0,


    // ✅ 展示型扩展
    val tags: List<String> = emptyList(),
    val externalLink: String? = null,


    )

data class ShowcaseDetailActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit,
    val onEdit: () -> Unit,
    val onToggleFavorite: () -> Unit = {},
    // ✅ 新增：从详情页打开 Chat（由逻辑层负责挂 pending 商品卡片）
    val onOpenChat: () -> Unit = {},

    // 仍保留（不强制用它做全屏；全屏可以纯 UI 内部完成）
    val onOpenImage: (String) -> Unit,
    // ✅ UI -> VM：pager currentPage 同步到 VM
    val onImageIndexChanged: (Int) -> Unit = {},
    val onSavePreviewImage: (String) -> Unit,
)



// ------------------------- StoreProfile (Business Card) -------------------------

/**
 * StoreProfile Screen UI State
 * - UI 包只认这一份结构来渲染“展示/编辑/保存状态”
 * - 逻辑模块在 wiring 中把 VM 的 domain(StoreProfile + draft + saving flags) 映射成这里
 */
data class ExtraContactDraft(
    val id: String,
    val name: String,
    val value: String
)
data class ShowcaseActions(
    val onBack: () -> Unit,
    val onEdit: () -> Unit,
    val onChangePassword: () -> Unit // 新增
)

// ------------------------- ChangePassword -------------------------

data class ShowcaseChangePasswordUiState(
    val current: String = "",
    val next: String = "",
    val confirm: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null,
)

data class ShowcaseChangePasswordActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit,
    val onCurrentChange: (String) -> Unit,
    val onNextChange: (String) -> Unit,
    val onConfirmChange: (String) -> Unit,
    val onSubmit: () -> Unit,
)

data class ShowcaseStoreProfileUiState(

    // ✅ 是否允许进入编辑（展示页会置为 false）
    val canEdit: Boolean = true,
// 展示态（未编辑时显示）
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",

// ✅ 新：业务范围（展示态）
    val services: List<String> = emptyList(),
// ✅ 新：自定义联系方式（展示态）
    val extraContacts: List<ExtraContact> = emptyList(),

    val address: String = "",
    val hours: String = "",
    val mapUrl: String = "",
    val businessStatus: String = "",  // 显示营业状态（“营业中”，“已关闭”）


    // draft 字段
    val draftBusinessStatus: String = "",
    // Brand visuals
    val logoUrl: String = "",
    val coverUrl: String = "",

// Open status (由逻辑模块计算，UI只展示)
    val openStatusText: String = "",   // 例："Open now · Closes 20:00" / "Closed · Opens 08:00"
    val isOpenNow: Boolean? = null,    // 可选：null=未知


    // 编辑态（draft 优先）
    val isEditing: Boolean = false,
    val draftTitle: String = "",
    val draftSubtitle: String = "",
    val draftDescription: String = "",
    val draftAddress: String = "",
    val draftHours: String = "",
    val draftMapUrl: String = "",
    val draftLogoUrl: String = "",
    val draftCoverUrl: String = "",
// ✅ 新：业务范围（编辑态）
// - UI 用它来渲染“已添加项列表”
    val draftServices: List<String> = emptyList(),

// ✅ 新：自定义联系方式（编辑态）
    val draftExtraContacts: List<ExtraContactDraft> = emptyList(),

// ✅ 保存校验错误（例如：某行只填了 Name 没填 Value）
    val validationError: String? = null,

    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
    val statusMessage: String? = null,
    val hasUnsavedChanges: Boolean = false,
)
/**
 * StoreProfile Screen Actions
 * - UI 包把输入/按钮事件回调回逻辑模块（VM）
 */
data class ShowcaseStoreProfileActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit,
    val onOpenChat: () -> Unit = {},
    val onStartEdit: () -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onSubtitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onHoursChange: (String) -> Unit = {},
    val onMapUrlChange: (String) -> Unit = {},
    val onBusinessStatusChange: (String) -> Unit = {},
    val onExtraContactNameChange: (index: Int, v: String) -> Unit = { _, _ -> },
    val onExtraContactValueChange: (index: Int, v: String) -> Unit = { _, _ -> },
    val onExtraContactAdd: (name: String, value: String) -> Unit = { _, _ -> },
    val onExtraContactRemove: (index: Int) -> Unit = {},
    val onServiceChange: (Int, String) -> Unit = { _, _ -> },
    val onServiceAdd: (String) -> Unit = {},
    val onServiceRemove: (Int) -> Unit = {},
    val onPickLogo: () -> Unit = {},
    val onPickCovers: () -> Unit = {},
    val onRemoveLogo: () -> Unit = {},
    val onRemoveCover: (String) -> Unit = {},
    val onMoveCover: (from: Int, to: Int) -> Unit = { _, _ -> },
    val onOpenMap: (String) -> Unit = {},
    val onCopy: (label: String, text: String) -> Unit = { _, _ -> },
    val onSavePreviewImage: (String) -> Unit,
    val onChangePassword: () -> Unit = {},
    val onSave: () -> Unit = {},
)


// ------------------------- Favorites -------------------------

enum class ShowcaseFavoritesSort {
    Newest,   // 最近收藏（MVP 可先近似）
    TitleAsc, // 名称 A-Z
    PriceAsc, // 价格低→高
    PriceDesc // 价格高→低
}
data class ShowcaseFavoriteCard(
    val dishId: String,
    val title: String,

    /** 分类名（Favorites 需要显示分类；null 表示未知/无分类） */
    val category: String? = null,

    /** 原价文本（例如 12.00） */
    val originalPriceText: String,
    /** 折后价文本（例如 9.90；null 表示无折扣） */
    val discountPriceText: String? = null,

    /**
     * 兼容字段：旧 UI 用 priceText 渲染单价。
     * 约定：priceText = (discountPriceText ?: originalPriceText)
     */
    val priceText: String,

    val imageUrl: String? = null
)


data class ShowcaseFavoritesUiState(
    val query: String = "",
    val items: List<ShowcaseFavoriteCard> = emptyList(),

    val selectedIds: Set<String> = emptySet(),

    // ✅ Home 同款：sort / filters
    val sortMode: ShowcaseHomeSortMode = ShowcaseHomeSortMode.Default,
    val filterRecommendedOnly: Boolean = false,
    val filterOnSaleOnly: Boolean = false,

    // ✅ price range
    val priceMinDraft: String = "",
    val priceMaxDraft: String = "",
    val appliedMinPrice: Int? = null,
    val appliedMaxPrice: Int? = null,

    // ✅ menu 显隐（VM 托管）
    val showSortMenu: Boolean = false,
    val showFilterMenu: Boolean = false,
    val showPriceMenu: Boolean = false,

    // ✅ 分类 chips：仅显示“收藏中出现的分类”
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList()
)

data class ShowcaseFavoritesActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onOpenDetail: (String) -> Unit = {},
    val onToggleSelect: (String) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onDeleteSelected: () -> Unit = {},
    val onSortModeChange: (ShowcaseHomeSortMode) -> Unit = {},
    val onFilterRecommendedOnlyChange: (Boolean) -> Unit = {},
    val onFilterOnSaleOnlyChange: (Boolean) -> Unit = {},
    val onClearSortAndFilters: () -> Unit = {},
    val onShowSortMenuChange: (Boolean) -> Unit = {},
    val onShowFilterMenuChange: (Boolean) -> Unit = {},
    val onPriceMinDraftChange: (String) -> Unit = {},
    val onPriceMaxDraftChange: (String) -> Unit = {},
    val onApplyPriceRange: () -> Unit = {},
    val onClearPriceRange: () -> Unit = {},
    val onShowPriceMenuChange: (Boolean) -> Unit = {},
    val onCategorySelected: (String?) -> Unit = {}

)
// ------------------------- Announcements -------------------------

/**
 * 活动公告（最简）：
 * - Admin 端发布
 * - 用户端列表查看
 * - 逻辑模块负责：持久化/同步/推送（后续接入），UI 只负责渲染 + 草稿输入
 */
// ------------------------- Announcements -------------------------
data class ShowcaseAnnouncementCard(
    val id: String,
    val coverUrl: String?,              // ✅ 允许为空：无图草稿
    val bodyPreview: String,
    val bodyText: String,               // ✅ 完整正文：展示页展开时显示
    val timeText: String,               // ✅ 显示用：草稿=保存时间，已推送=推送时间
    val viewCount: Int,                 // ✅ 展开正文次数 = 浏览次数
)

data class ShowcaseAnnouncementsUiState(
    val items: List<ShowcaseAnnouncementCard> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val focusedAnnouncementId: String? = null,
)

data class ShowcaseAnnouncementsActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onOpenAnnouncement: (id: String) -> Unit = {},
    val onTrackAnnouncementView: (id: String) -> Unit = {},
    val onOpenAnnouncementImage: (id: String) -> Unit = {},
    val onConsumeFocusedAnnouncement: () -> Unit = {},
)

data class ShowcaseAnnouncementEditUiState(
    val coverDraftUrl: String? = null,
    val bodyDraft: String = "",
    val editingId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val statusMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isBlockingInput: Boolean = false,

    // ✅ 编辑器显示状态
    val composerExpanded: Boolean = false,

    // ✅ 顶部按钮/底部按钮可用态（由逻辑层产出）
    val canStartNew: Boolean = true,
    val canDeleteSelected: Boolean = false,
    val canSaveDraft: Boolean = false,
    val canPublish: Boolean = false,

    // ✅ 白卡下方显示草稿列表
    val draftItems: List<ShowcaseAnnouncementCard> = emptyList(),

    // ✅ 多选删除（仅草稿）
    val selectedIds: Set<String> = emptySet(),

    // ✅ 草稿预览（逻辑模块产出；UI 只展示）
    val previewItem: ShowcaseAnnouncementCard? = null,
    val previewVisible: Boolean = false,
    val hasUnsavedChanges: Boolean = false
)

data class ShowcaseAnnouncementEditActions(
    val onBackToHome: () -> Unit = {},
    val onBack: () -> Unit = {},

    // ✅ 顶部 New
    val onStartNew: () -> Unit = {},

    val onPickCover: () -> Unit = {},
    val onRemoveCover: () -> Unit = {},
    val onOpenCoverPreview: () -> Unit = {},
    val onBodyChange: (String) -> Unit = {},

    val onSaveDraft: () -> Unit = {},
    val onPushNow: () -> Unit = {},

    val onOpenItem: (String) -> Unit = {},
    val onPreviewItem: (String) -> Unit = {},
    val onDismissPreview: () -> Unit = {},
    val onToggleSelect: (String) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onDeleteSelected: () -> Unit = {}
)

// ------------------------- Screen Enum -------------------------

enum class ShowcaseRoute {
    Home, Login, Admin, Detail, Edit, Favorites
}



