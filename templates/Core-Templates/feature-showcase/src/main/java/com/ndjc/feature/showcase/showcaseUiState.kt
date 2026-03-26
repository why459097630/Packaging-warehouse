package com.ndjc.feature.showcase

enum class ShowcaseScreen {
    Home,
    Login,

    // Admin Home（只放 4 个按钮）
    Admin,

    // ✅ 方案 B：Admin 子操作抽成同级 Screen
    AdminItems,
    AdminCategories,

    Detail,
    Edit,

    // ✅ StoreProfile：展示态（只读）
    StoreProfileView,
    StoreProfile,
    ChangePassword,
    // ✅ 商家聊天列表页
    MerchantChatList,
    Chat,
    ChatSearchResults,
    ChatMedia,
    Favorites,
    Announcements,
    AdminAnnouncementEdit,

}

enum class AdminEntryMode {
    None,
    RenameCategory,
    EditCategories,
    EditStoreProfile,
}



/**
 * 同步总览状态：为“离线/重试/失败提示”预留
 * - 不强依赖现有 ViewModel，实现时再接入即可。
 */
enum class SyncOverviewState {
    Idle,
    Syncing,
    HasPending,
    Failed
}

/**
 * 弱网/错误提示的“可重试操作”标记（纯 Kotlin，不依赖 Android/Compose）
 */
sealed interface ShowcaseRetryOp {
    object LoadFromCloud : ShowcaseRetryOp
    object RetryPendingSync : ShowcaseRetryOp
    object RefreshStoreProfile : ShowcaseRetryOp
}

enum class SortMode {
    Price,
    Name
}

data class ShowcaseUiState(
    val dishes: List<DemoDish> = emptyList(),
    val manualCategories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val loginUsernameDraft: String = "",
    val loginPasswordDraft: String = "",

    // ✅ Login：自动登录（记住我）草稿（默认不勾选）
    val loginRememberMeDraft: Boolean = false,
    // ✅ ChangePassword：草稿（逻辑模块托管，UI 只读）
    val changePasswordCurrentDraft: String = "",
    val changePasswordNewDraft: String = "",
    val changePasswordConfirmDraft: String = "",
    val changePasswordError: String? = null,
    val changePasswordSuccess: String? = null,



    // ✅ VM 用到的：页面状态机
    val screen: ShowcaseScreen = ShowcaseScreen.Home,

    // ✅ VM 用到的：管理员是否已登录
    val isAdminLoggedIn: Boolean = false,

// ✅ VM 用到的：删除确认弹窗（要删哪一条）——用 id 避免排序/筛选导致 index 错位
    val pendingDeleteDishId: String? = null,

    val adminEntryMode: AdminEntryMode = AdminEntryMode.None,


    val isLoading: Boolean = false,
    val isSavingEditDish: Boolean = false,
    val isBlockingEditDish: Boolean = false,
    val statusMessage: String? = null,
    val editValidationError: String? = null,
    val loginError: String? = null,
    val selectedDish: DemoDish? = null,
    val detailImageIndex: Int = 0,
    val searchQuery: String = "",
    val sortMode: ShowcaseHomeSortMode = ShowcaseHomeSortMode.Default,
    val adminItemsSortMode: ShowcaseHomeSortMode = ShowcaseHomeSortMode.Default,
    // ✅ AdminItems: Search / Filters / SortDirection（从 UI 包抽离出来）
    val adminItemsSearchQuery: String = "",
    val adminItemsFilterRecommended: Boolean = false,
    val adminItemsFilterHiddenOnly: Boolean = false,
    val adminItemsFilterDiscountOnly: Boolean = false,
// ✅ AdminItems: 当前 sortMode 下的升/降
    val adminItemsSortAscending: Boolean = true,
    // ✅ AdminItems：价格区间（VM 解析并应用；UI 只负责输入）
    val adminItemsPriceMinDraft: String = "",
    val adminItemsPriceMaxDraft: String = "",
    val adminItemsAppliedMinPrice: Int? = null,
    val adminItemsAppliedMaxPrice: Int? = null,
    val adminSelectedDishIds: Set<String> = emptySet(),
    val filterRecommendedOnly: Boolean = false,
    val filterOnSaleOnly: Boolean = false,

// ✅ Home：排序/筛选弹出菜单显隐（原来在 UI 包 remember）
    val homeShowSortMenu: Boolean = false,
    val homeShowFilterMenu: Boolean = false,
// ✅ Home：价格区间（VM 解析并应用）
    val homePriceMinDraft: String = "",
    val homePriceMaxDraft: String = "",
    val homeAppliedMinPrice: Int? = null,
    val homeAppliedMaxPrice: Int? = null,

// ✅ Home：价格弹窗显隐（原来在 UI remember）
    val homeShowPriceMenu: Boolean = false,

// ✅ Admin：设置管理员口令的草稿（原来在 UI 包 remember）
    val adminUsernameDraft: String = "",
    val adminPasswordDraft: String = "",

// ✅ AdminCategories：删除分类弹窗状态（原来在 UI 包 remember + 业务判断）
    val adminPendingDeleteCategory: String? = null,
    val adminCannotDeleteCategory: String? = null,

    /**
     * 展示型必备：搜索 / 筛选 / 排序（状态位先补齐，逻辑后续在 VM/QueryEngine 落地）
     */


    val selectedTags: List<String> = emptyList(),

    /**
     * 展示型必备：同步体验（状态位先补齐）
     */
    val syncOverviewState: SyncOverviewState = SyncOverviewState.Idle,
    val lastSyncAt: Long? = null,
    val pendingSyncCount: Int = 0,
    val syncErrorMessage: String? = null,

// ✅ 弱网/失败时：记录“下一次点重试应该做什么”
    val lastRetryOp: ShowcaseRetryOp? = null,

    /**
     * 展示型必备：店铺信息 / 触达入口（展示态）
     */
    val storeProfile: StoreProfile? = null,

    /**
     * StoreProfile：编辑态草稿 + 保存状态（可编辑/可保存闭环必需）
     */
    val storeProfileDraft: StoreProfile? = null,
    val isSavingStoreProfile: Boolean = false,
    val storeProfileSaveError: String? = null,
    val storeProfileSaveSuccess: Boolean = false,

    /**
     * 展示型必备：线索收集（最简）
     */
    val chat: ShowcaseChatUiState = ShowcaseChatUiState(title = "Chat"),

// ✅ 商家聊天列表数据（逻辑模块产出）
    // ✅ 商家聊天列表数据（逻辑模块产出）
    val merchantChatThreads: List<ShowcaseChatThreadSummaryUi> = emptyList(),

    // ✅ Chat List 下拉刷新状态（逻辑模块维护，UI 只读）
    val merchantChatListRefreshing: Boolean = false,
    // ------------------------- Announcements -------------------------
// ------------------------- Announcements -------------------------

// ✅ 用户端公告列表（仅展示“已发布”）
    val announcements: List<ShowcaseAnnouncementCard> = emptyList(),

// ✅ Admin 公告编辑页：可观察草稿列表
    val adminAnnouncementDraftItems: List<ShowcaseAnnouncementCard> = emptyList(),

// ✅ Admin 公告编辑页状态（折叠/展开 + 单图 + 文案 + 草稿列表 + 多选）
    val adminAnnouncementComposerExpanded: Boolean = false,
    val adminAnnouncementCoverDraftUrl: String? = null,
    val adminAnnouncementBodyDraft: String = "",
    val adminAnnouncementEditingId: String? = null,
    val adminAnnouncementSelectedIds: Set<String> = emptySet(),
    val adminAnnouncementPreviewId: String? = null,

    val adminAnnouncementError: String? = null,
    val adminAnnouncementSuccess: String? = null,
    val adminAnnouncementIsSubmitting: Boolean = false,
    val adminAnnouncementIsBlocking: Boolean = false,

    val pushTargetAnnouncementId: String? = null,

    )

