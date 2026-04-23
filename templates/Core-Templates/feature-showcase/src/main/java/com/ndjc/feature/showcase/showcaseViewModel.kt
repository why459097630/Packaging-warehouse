package com.ndjc.feature.showcase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.map
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.currentCoroutineContext
/**
 * 编辑页用的本地草稿状态：
 * - isNew       : 是否新增
 * - index       : 旧菜在 dishes 列表中的下标（新增为 null）
 * - original    : 旧菜原始 DemoDish（编辑模式用来 copy）
 * - 各种表单字段：name / price / discountPrice / description / category / 推荐 / 售罄 / 封面图
 */
data class EditDraftState(
    val isNew: Boolean,
    val index: Int?,
    val original: DemoDish?,
    val name: String,
    val price: String,
    val discountPrice: String,
    val description: String,
    val category: String?,
    val isRecommended: Boolean,

    // ✅ 必须叫 isSoldOut（与你下面的 named argument 对齐）
    val isSoldOut: Boolean,

    val isHidden: Boolean = false,
    // ✅ 方案 B：编辑态多图（最多 9）
    val imageUris: List<Uri>
)

class ShowcaseViewModel : ViewModel() {
    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 200

        private const val PRODUCT_IMAGE_LONG_EDGE = 1600
        private const val PRODUCT_IMAGE_JPEG_QUALITY = 88

        private const val CHAT_IMAGE_LONG_EDGE = 1080
        private const val CHAT_IMAGE_JPEG_QUALITY = 84

        private const val ANNOUNCEMENT_IMAGE_LONG_EDGE = 1280
        private const val ANNOUNCEMENT_IMAGE_JPEG_QUALITY = 86

        private const val STORE_COVER_IMAGE_LONG_EDGE = 1280
        private const val STORE_COVER_IMAGE_JPEG_QUALITY = 86

        private const val STORE_LOGO_IMAGE_LONG_EDGE = 512
        private const val STORE_LOGO_IMAGE_JPEG_QUALITY = 90

        private const val TEMP_CAMERA_DIR_NAME = "ndjc_camera"
        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        private const val LOCAL_TEMP_FILE_EXPIRE_MS = 24L * 60L * 60L * 1000L

        private const val PREF_NAME = "Showcase_admin_prefs"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_ADMIN_LOGGED_IN = "admin_logged_in"
        private const val KEY_MERCHANT_LOGIN_NAME = "merchant_login_name"
        private const val KEY_MERCHANT_ACCESS_TOKEN = "merchant_access_token"
        private const val KEY_MERCHANT_REFRESH_TOKEN = "merchant_refresh_token"
        private const val KEY_MERCHANT_AUTH_USER_ID = "merchant_auth_user_id"

        // ✅ StoreProfile 仅本地预览：true=只更新内存(重启/重新run即复位)，不落盘、不触网
        private const val STORE_PROFILE_PREVIEW_LOCAL_ONLY: Boolean = false
    }
    private val showcaseCloudRepository = ShowcaseCloudRepository()

    private var lastRegisteredPushOk: Boolean = false
    private var lastRegisteredPushToken: String? = null
    private var lastRegisteredPushAudience: String? = null
    private var lastRegisteredPushConversationId: String? = null

    private fun deriveCategories(
        dishes: List<DemoDish>,
        manualCategories: List<String>
    ): List<String> {
        val fromDishes = dishes.map { it.category }.filter { it.isNotBlank() }
        return (fromDishes + manualCategories)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    // ✅ 通用图片压缩：按不同业务场景传入最长边和 JPEG 质量，返回压缩后的本地 cache Uri
    private suspend fun compressImage(
        context: Context,
        sourceUri: Uri,
        maxLongEdge: Int,
        jpegQuality: Int
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext null

            var inSampleSize = 1
            var currentWidth = srcWidth
            var currentHeight = srcHeight
            while (
                currentWidth > maxLongEdge * 2 ||
                currentHeight > maxLongEdge * 2
            ) {
                currentWidth /= 2
                currentHeight /= 2
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val decoded = resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return@withContext null

            val rotationDegrees = resolver.openInputStream(sourceUri)?.use { input ->
                val exif = ExifInterface(input)
                when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val rotatedBitmap =
                if (rotationDegrees == 0f) {
                    decoded
                } else {
                    val matrix = Matrix().apply {
                        postRotate(rotationDegrees)
                    }
                    Bitmap.createBitmap(
                        decoded,
                        0,
                        0,
                        decoded.width,
                        decoded.height,
                        matrix,
                        true
                    ).also {
                        if (it !== decoded) {
                            decoded.recycle()
                        }
                    }
                }

            val longEdge = maxOf(rotatedBitmap.width, rotatedBitmap.height)
            val outputBitmap =
                if (longEdge <= maxLongEdge) {
                    rotatedBitmap
                } else {
                    val scale = maxLongEdge.toFloat() / longEdge.toFloat()
                    val targetWidth = (rotatedBitmap.width * scale).toInt().coerceAtLeast(1)
                    val targetHeight = (rotatedBitmap.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(
                        rotatedBitmap,
                        targetWidth,
                        targetHeight,
                        true
                    ).also {
                        if (it !== rotatedBitmap) {
                            rotatedBitmap.recycle()
                        }
                    }
                }

            val fileName = "ndjc_img_${System.currentTimeMillis()}.jpg"
            val outFile = File(context.cacheDir, fileName)

            java.io.FileOutputStream(outFile).use { out ->
                val ok = outputBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    jpegQuality,
                    out
                )
                if (!ok) {
                    outputBitmap.recycle()
                    if (outFile.exists()) {
                        outFile.delete()
                    }
                    return@withContext null
                }
            }

            outputBitmap.recycle()
            Uri.fromFile(outFile)
        } catch (e: Exception) {
            Log.e("ShowcaseViewModel", "compressImage failed", e)
            null
        }
    }

    private fun isAppOwnedLocalFileUri(uri: Uri?): Boolean {
        return runCatching {
            if (uri == null) return@runCatching false

            val file =
                when {
                    uri.scheme == "file" -> uri.path?.let { File(it) }
                    uri.scheme.isNullOrBlank() && !uri.path.isNullOrBlank() -> File(uri.path!!)
                    else -> null
                } ?: return@runCatching false

            val target = file.canonicalFile
            val cacheRoot = appContext?.cacheDir?.canonicalFile
            val externalRoot = appContext?.getExternalFilesDir(null)?.canonicalFile

            (cacheRoot != null && target.path.startsWith(cacheRoot.path)) ||
                    (externalRoot != null && target.path.startsWith(externalRoot.path))
        }.getOrDefault(false)
    }

    private fun deleteAppOwnedLocalFileUri(uri: Uri?) {
        runCatching {
            if (!isAppOwnedLocalFileUri(uri)) return@runCatching
            val path = uri?.path ?: return@runCatching
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun deleteAppOwnedLocalFileUriString(raw: String?) {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return
        deleteAppOwnedLocalFileUri(Uri.parse(value))
    }

    private fun clearExpiredLocalTempFiles(context: Context) {
        runCatching {
            val now = System.currentTimeMillis()

            context.cacheDir.listFiles()
                ?.filter { file ->
                    file.isFile && file.name.startsWith("ndjc_img_")
                }
                ?.forEach { file ->
                    val expired = now - file.lastModified() > LOCAL_TEMP_FILE_EXPIRE_MS
                    if (expired && file.exists()) {
                        file.delete()
                    }
                }

            val cameraDir = File(context.cacheDir, TEMP_CAMERA_DIR_NAME)
            cameraDir.listFiles()
                ?.forEach { file ->
                    val expired = now - file.lastModified() > LOCAL_TEMP_FILE_EXPIRE_MS
                    if (expired && file.exists()) {
                        file.delete()
                    }
                }
        }
    }

    private fun clearEditDraftLocalImages() {
        editDraft?.imageUris?.forEach { uri ->
            deleteAppOwnedLocalFileUri(uri)
        }
    }

    private fun clearAnnouncementDraftLocalImages() {
        deleteAppOwnedLocalFileUriString(uiState.adminAnnouncementCoverDraftUrl)
    }

    private fun clearStoreProfileDraftLocalImages() {
        val draft = uiState.storeProfileDraft ?: return
        deleteAppOwnedLocalFileUriString(draft.logoUrl)

        parseCoverList(draft.coverUrl).forEach { raw ->
            deleteAppOwnedLocalFileUriString(raw)
        }
    }

    private fun createTempCameraUri(context: Context): Uri? {
        return runCatching {
            val tempDir = File(context.cacheDir, TEMP_CAMERA_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }

            val tempFile = File(
                tempDir,
                "chat_${System.currentTimeMillis()}.jpg"
            ).apply {
                if (exists()) {
                    delete()
                }
                createNewFile()
            }

            chatPendingCameraFile = tempFile

            FileProvider.getUriForFile(
                context,
                context.packageName + FILE_PROVIDER_SUFFIX,
                tempFile
            )
        }.getOrElse { e ->
            Log.e("ShowcaseViewModel", "createTempCameraUri failed", e)
            null
        }
    }
    // ✅ VM 状态不依赖 Compose：用 StateFlow 做状态源
    private val _uiState = MutableStateFlow(ShowcaseUiState())
    val uiStateFlow: StateFlow<ShowcaseUiState> = _uiState.asStateFlow()

    // ✅ 兼容你现有大量写法：uiState = uiState.copy(...)
    var uiState: ShowcaseUiState
        get() = _uiState.value
        private set(value) { _uiState.value = value }

    // 编辑页本地草稿（所有输入/图片先写这里，点保存时再写 dishes + 云端）
// 编辑页本地草稿（所有输入/图片先写这里，点保存时再写 dishes + 云端）
    private val _editDraft = MutableStateFlow<EditDraftState?>(null)
    val editDraftFlow: StateFlow<EditDraftState?> = _editDraft.asStateFlow()

    var editDraft: EditDraftState?
        get() = _editDraft.value
        private set(value) { _editDraft.value = value }

    // ✅ Edit 页返回到哪里：进入 Edit 前的 screen
    private var editBackTarget: ShowcaseScreen = ShowcaseScreen.Admin


    private var hasLoadedOnce = false

    // 旧云端仓库：给店铺/商品/分类/资料等继续用（别动它）
    private val cloudRepository = ShowcaseCloudRepository()

    // -----------------------
    // ✅ Favorites（收藏）：VM 不依赖 Compose，但要可观察（StateFlow）
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIdsFlow: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    // ✅ Home 底栏 Chat 入口红点：独立状态，不再依赖 Chat 页面 UI 态
    private val _chatEntryDot = MutableStateFlow(false)
    val chatEntryDotFlow: StateFlow<Boolean> = _chatEntryDot.asStateFlow()

    // ✅ Home 底栏 Announcements 入口红点：独立状态，不再依赖 Announcements 页面 UI 态
    private val _announcementsEntryDot = MutableStateFlow(false)
    val announcementsEntryDotFlow: StateFlow<Boolean> = _announcementsEntryDot.asStateFlow()

    // ✅ 兼容你现有大量写法：favoriteIds = favoriteIds + id / favoriteIds.contains(id)
    private var favoriteIds: Set<String>
        get() = _favoriteIds.value
        set(value) { _favoriteIds.value = value }
    // ✅ 记录“收藏发生的时间”，用于 Favorites 默认排序：新收藏靠上
    private val favoriteAddedAt = mutableMapOf<String, Long>()

    private fun persistFavoritesToStorage() {
        val context = appContext ?: return
        saveFavoriteIdsToStorage(context, favoriteIds)
        saveFavoriteAddedAtToStorage(context, favoriteAddedAt)
    }

    // ✅ 统一金额格式：最多 2 位小数，末尾 0 自动去掉
// 例：11.00 -> "11"；11.10 -> "11.1"；11.12 -> "11.12"
    private val moneyTrim2Formatter: DecimalFormat = DecimalFormat("0.##").apply {
        roundingMode = RoundingMode.HALF_UP
    }
    private fun moneyTrim2(v: Float): String = moneyTrim2Formatter.format(v.toDouble())


    private val _favoritesUiState = MutableStateFlow(ShowcaseFavoritesUiState())
    val favoritesUiStateFlow: StateFlow<ShowcaseFavoritesUiState> = _favoritesUiState.asStateFlow()

    var favoritesUiState: ShowcaseFavoritesUiState
        get() = _favoritesUiState.value
        private set(value) { _favoritesUiState.value = value }


    fun openFavorites() {
        uiState = uiState.copy(screen = ShowcaseScreen.Favorites)
        refreshFavoritesList()
    }

    fun closeFavorites() {
        uiState = uiState.copy(screen = ShowcaseScreen.Home)
    }
// ----------------------- Announcements（活动公告） -----------------------

    private var announcementsBackTarget: ShowcaseScreen = ShowcaseScreen.Home

    // ✅ 逻辑侧“公告仓库”（v1：内存；后续接 Supabase）
    private enum class AnnouncementStatus {
        Draft,
        Published
    }

    private data class AnnouncementEntity(
        val id: String,
        val coverUrl: String?,
        val body: String,
        val status: AnnouncementStatus,
        val updatedAt: Long,
        val viewCount: Int
    )

    private val adminAnnouncements = mutableListOf<AnnouncementEntity>()
    private val seenAnnouncementIds = mutableSetOf<String>()
    private val countedAnnouncementClickIds = mutableSetOf<String>()

    private fun toAnnouncementEntity(
        cloud: ShowcaseCloudRepository.CloudAnnouncement
    ): AnnouncementEntity {
        return AnnouncementEntity(
            id = cloud.id,
            coverUrl = cloud.coverUrl,
            body = cloud.body,
            status = if (cloud.status.equals("published", ignoreCase = true)) {
                AnnouncementStatus.Published
            } else {
                AnnouncementStatus.Draft
            },
            updatedAt = cloud.updatedAt ?: cloud.createdAt ?: System.currentTimeMillis(),
            viewCount = cloud.viewCount
        )
    }

    private fun toPublishedAnnouncementEntity(
        item: CachedPublishedAnnouncement
    ): AnnouncementEntity {
        return AnnouncementEntity(
            id = item.id,
            coverUrl = item.coverUrl,
            body = item.body,
            status = AnnouncementStatus.Published,
            updatedAt = item.updatedAt,
            viewCount = item.viewCount
        )
    }

    private fun persistPublishedAnnouncementsLocally(items: List<AnnouncementEntity>) {
        val ctx = appContext ?: return
        savePublishedAnnouncementsToStorage(
            context = ctx,
            items = items
                .filter { it.status == AnnouncementStatus.Published }
                .map { e ->
                    CachedPublishedAnnouncement(
                        id = e.id,
                        coverUrl = e.coverUrl,
                        body = e.body,
                        updatedAt = e.updatedAt,
                        viewCount = e.viewCount
                    )
                }
        )
    }

    private fun loadViewedAnnouncementIdsLocally(): MutableSet<String> {
        val ctx = appContext ?: return mutableSetOf()
        return loadViewedAnnouncementIdsFromStorage(ctx).toMutableSet()
    }

    private fun saveViewedAnnouncementIdsLocally(ids: Set<String>) {
        val ctx = appContext ?: return
        saveViewedAnnouncementIdsToStorage(ctx, ids)
    }

    private fun hasAnnouncementBeenViewedLocally(id: String): Boolean {
        if (id.isBlank()) return false
        return loadViewedAnnouncementIdsLocally().contains(id)
    }

    private fun loadCountedAnnouncementClickIdsLocally(): MutableSet<String> {
        val ctx = appContext ?: return mutableSetOf()
        return loadCountedAnnouncementClickIdsFromStorage(ctx).toMutableSet()
    }

    private fun saveCountedAnnouncementClickIdsLocally(ids: Set<String>) {
        val ctx = appContext ?: return
        saveCountedAnnouncementClickIdsToStorage(ctx, ids)
    }

    private fun hasAnnouncementClickBeenCountedLocally(id: String): Boolean {
        if (id.isBlank()) return false
        return countedAnnouncementClickIds.contains(id) ||
                loadCountedAnnouncementClickIdsLocally().contains(id)
    }

    private fun markAnnouncementClickCountedLocally(id: String) {
        if (id.isBlank()) return
        countedAnnouncementClickIds.add(id)
        val ids = loadCountedAnnouncementClickIdsLocally()
        if (ids.add(id)) {
            saveCountedAnnouncementClickIdsLocally(ids)
        }
    }

    private fun isAnnouncementSeen(id: String): Boolean {
        if (id.isBlank()) return false
        return seenAnnouncementIds.contains(id) || hasAnnouncementBeenViewedLocally(id)
    }

    private fun markAnnouncementViewedLocally(id: String) {
        if (id.isBlank()) return
        seenAnnouncementIds.add(id)
        val ids = loadViewedAnnouncementIdsLocally()
        if (ids.add(id)) {
            saveViewedAnnouncementIdsLocally(ids)
        }
    }

    private fun markAnnouncementsViewedLocally(ids: Collection<String>) {
        val normalized = ids
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        if (normalized.isEmpty()) return

        seenAnnouncementIds.addAll(normalized)

        val localIds = loadViewedAnnouncementIdsLocally()
        if (localIds.addAll(normalized)) {
            saveViewedAnnouncementIdsLocally(localIds)
        }
    }

    private fun trackAnnouncementClickOnce(id: String) {
        val targetId = id.trim()
        if (targetId.isBlank()) return

        if (hasAnnouncementClickBeenCountedLocally(targetId)) {
            Log.d("NDJC_CLICK", "trackAnnouncementClickOnce skip because counted locally, id=$targetId")
            return
        }

        viewModelScope.launch {
            val ok = showcaseCloudRepository.incrementAnnouncementViewCount(
                storeId = chatStoreId,
                announcementId = targetId
            )

            Log.d("NDJC_CLICK", "trackAnnouncementClickOnce increment finished ok=$ok id=$targetId")

            if (ok) {
                markAnnouncementClickCountedLocally(targetId)
                syncPublicAnnouncementsFromCloud()
            } else {
                Log.e(
                    "NDJC_CLICK",
                    "trackAnnouncementClickOnce: incrementAnnouncementViewCount failed for id=$targetId"
                )
            }
        }
    }

    private fun syncPublicAnnouncementsFromCloud(markViewedAfterSync: Boolean = false) {
        viewModelScope.launch {
            val safeContext = appContext
            val cloudItems = showcaseCloudRepository.fetchAnnouncements(
                storeId = chatStoreId,
                includeDrafts = false,
                actor = ShowcaseCloudConfig.AuthActor.PUBLIC
            )

            val cachedEntities = safeContext
                ?.let { ctx ->
                    loadPublishedAnnouncementsFromStorage(ctx).map { item ->
                        toPublishedAnnouncementEntity(item)
                    }
                }
                .orEmpty()

            if (cloudItems.isNotEmpty()) {
                val entities = cloudItems.map { cloud ->
                    toAnnouncementEntity(cloud)
                }

                adminAnnouncements.clear()
                adminAnnouncements.addAll(entities)

                if (markViewedAfterSync) {
                    markAnnouncementsViewedLocally(
                        entities
                            .filter { it.status == AnnouncementStatus.Published }
                            .map { it.id }
                    )
                }

                rebuildAnnouncementsList()
                persistPublishedAnnouncementsLocally(entities)
            } else if (cachedEntities.isNotEmpty()) {
                adminAnnouncements.clear()
                adminAnnouncements.addAll(cachedEntities)

                if (markViewedAfterSync) {
                    markAnnouncementsViewedLocally(
                        cachedEntities
                            .filter { it.status == AnnouncementStatus.Published }
                            .map { it.id }
                    )
                }

                rebuildAnnouncementsList()
            }
        }
    }

    private fun syncMerchantAnnouncementsFromCloud() {
        viewModelScope.launch {
            val cloudItems = showcaseCloudRepository.fetchAnnouncements(
                storeId = chatStoreId,
                includeDrafts = true,
                actor = ShowcaseCloudConfig.AuthActor.MERCHANT
            )
            adminAnnouncements.clear()
            adminAnnouncements.addAll(cloudItems.map { toAnnouncementEntity(it) })
            rebuildAnnouncementsList()
        }
    }

    fun openAnnouncementsFromBottomBar() {
        announcementsBackTarget = uiState.screen

        uiState = uiState.copy(screen = ShowcaseScreen.Announcements)

        syncPublicAnnouncementsFromCloud(markViewedAfterSync = true)
        rebuildAnnouncementsList()
    }

    fun backFromAnnouncements() {
        uiState = uiState.copy(screen = announcementsBackTarget)
    }

    fun refreshAnnouncements() {
        syncPublicAnnouncementsFromCloud(
            markViewedAfterSync = uiState.screen == ShowcaseScreen.Announcements
        )
        rebuildAnnouncementsList()
    }

    fun onAnnouncementPushArrived() {
        _announcementsEntryDot.value = true
        syncPublicAnnouncementsFromCloud(markViewedAfterSync = false)
        rebuildAnnouncementsList()
    }

    fun handlePushRoute(
        context: Context,
        route: ShowcasePushRoute
    ) {
        when (route.pushType.trim().lowercase()) {
            "chat" -> {
                val openAs = route.openAs?.trim()?.lowercase().orEmpty()

                when {
                    openAs == "merchant" && !route.conversationId.isNullOrBlank() -> {
                        openChatFromMerchantList(
                            context = context,
                            threadId = route.conversationId,
                            threadTitle = "Chat"
                        )
                    }

                    openAs == "client" -> {
                        openChatFromHome(context)
                    }

                    uiState.isAdminLoggedIn && !route.conversationId.isNullOrBlank() -> {
                        openChatFromMerchantList(
                            context = context,
                            threadId = route.conversationId,
                            threadTitle = "Chat"
                        )
                    }

                    else -> {
                        openChatFromHome(context)
                    }
                }
            }

            "announcement" -> {
                announcementsBackTarget = ShowcaseScreen.Home

                uiState = uiState.copy(
                    screen = ShowcaseScreen.Announcements,
                    pushTargetAnnouncementId = route.announcementId
                )

                route.announcementId
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        trackAnnouncementClickOnce(it)
                    }

                syncPublicAnnouncementsFromCloud(markViewedAfterSync = true)
                rebuildAnnouncementsList()
            }
        }
    }

    fun consumePushAnnouncementTarget() {
        if (uiState.pushTargetAnnouncementId == null) return
        uiState = uiState.copy(
            pushTargetAnnouncementId = null
        )
    }

    fun shouldShowChatEntryDot(): Boolean {
        if (uiState.screen == ShowcaseScreen.Chat) return false
        return chatEntryDotFlow.value
    }

    fun shouldShowAnnouncementsEntryDot(): Boolean {
        if (uiState.screen == ShowcaseScreen.Announcements) return false
        return announcementsEntryDotFlow.value
    }

    fun openAdminAnnouncementPublisher() {
        syncMerchantAnnouncementsFromCloud()

        val cached = loadAdminAnnouncementEditorDraftLocally()

        uiState = if (cached != null) {
            uiState.copy(
                screen = ShowcaseScreen.AdminAnnouncementEdit,
                adminAnnouncementComposerExpanded = true,
                adminAnnouncementCoverDraftUrl = null,
                adminAnnouncementBodyDraft = cached.body,
                adminAnnouncementEditingId = cached.editingId,
                adminAnnouncementSelectedIds = emptySet(),
                adminAnnouncementPreviewId = null,
                adminAnnouncementError = null,
                adminAnnouncementSuccess = null
            )
        } else {
            uiState.copy(
                screen = ShowcaseScreen.AdminAnnouncementEdit,
                adminAnnouncementComposerExpanded = false,
                adminAnnouncementCoverDraftUrl = null,
                adminAnnouncementBodyDraft = "",
                adminAnnouncementEditingId = null,
                adminAnnouncementSelectedIds = emptySet(),
                adminAnnouncementPreviewId = null,
                adminAnnouncementError = null,
                adminAnnouncementSuccess = null
            )
        }
    }
    fun onAdminAnnouncementStartNew() {
        clearAdminAnnouncementEditorDraftLocally()
        uiState = uiState.copy(
            adminAnnouncementComposerExpanded = true,
            adminAnnouncementCoverDraftUrl = null,
            adminAnnouncementBodyDraft = "",
            adminAnnouncementEditingId = null,
            adminAnnouncementSelectedIds = emptySet(),
            adminAnnouncementPreviewId = null,
            adminAnnouncementError = null,
            adminAnnouncementSuccess = null
        )
    }
    fun onAdminAnnouncementBodyDraftChange(v: String) {
        uiState = uiState.copy(
            adminAnnouncementComposerExpanded = true,
            adminAnnouncementBodyDraft = v,
            adminAnnouncementError = null,
            adminAnnouncementSuccess = null
        )
        persistAdminAnnouncementEditorDraftLocally()
    }

    fun onAdminAnnouncementCoverPicked(url: String) {
        uiState = uiState.copy(
            adminAnnouncementComposerExpanded = true,
            adminAnnouncementCoverDraftUrl = url,
            adminAnnouncementError = null,
            adminAnnouncementSuccess = null
        )
        persistAdminAnnouncementEditorDraftLocally()
    }

    fun onAdminAnnouncementClearCover() {
        uiState = uiState.copy(
            adminAnnouncementCoverDraftUrl = null,
            adminAnnouncementError = null,
            adminAnnouncementSuccess = null
        )
        persistAdminAnnouncementEditorDraftLocally()
    }

    private fun announcementDraftTimeText(ts: Long): String {
        val ctx = appContext
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }

        val mo = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val h24 = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val mm = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')

        return if (ctx != null && android.text.format.DateFormat.is24HourFormat(ctx)) {
            val hh = h24.toString().padStart(2, '0')
            "$mo-$d $hh:$mm"
        } else {
            val ampm = if (h24 < 12) "AM" else "PM"
            val h12 = (h24 % 12).let { if (it == 0) 12 else it }.toString().padStart(2, '0')
            "$mo-$d $ampm $h12:$mm"
        }
    }

    private fun announcementPublishedTimeText(ts: Long): String {
        val ctx = appContext
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }

        val y = cal.get(java.util.Calendar.YEAR)
        val mo = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val h24 = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val mm = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')

        return if (ctx != null && android.text.format.DateFormat.is24HourFormat(ctx)) {
            val hh = h24.toString().padStart(2, '0')
            "$y-$mo-$d $hh:$mm"
        } else {
            val ampm = if (h24 < 12) "AM" else "PM"
            val h12 = (h24 % 12).let { if (it == 0) 12 else it }.toString().padStart(2, '0')
            "$y-$mo-$d $ampm $h12:$mm"
        }
    }
    private fun persistAdminAnnouncementEditorDraftLocally() {
        val ctx = appContext ?: return

        val body = uiState.adminAnnouncementBodyDraft
        val editingId = uiState.adminAnnouncementEditingId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (body.trim().isBlank() && editingId == null) {
            clearAdminAnnouncementEditorDraftFromStorage(ctx)
            return
        }

        saveAdminAnnouncementEditorDraftToStorage(
            context = ctx,
            draft = CachedAdminAnnouncementEditorDraft(
                editingId = editingId,
                body = body
            )
        )
    }

    private fun loadAdminAnnouncementEditorDraftLocally(): CachedAdminAnnouncementEditorDraft? {
        val ctx = appContext ?: return null
        return loadAdminAnnouncementEditorDraftFromStorage(ctx)
    }

    private fun clearAdminAnnouncementEditorDraftLocally() {
        val ctx = appContext ?: return
        clearAdminAnnouncementEditorDraftFromStorage(ctx)
    }

    private fun persistItemEditorDraftLocally() {
        val ctx = appContext ?: return
        val draft = editDraft ?: run {
            clearItemEditorDraftFromStorage(ctx)
            return
        }

        val editingId = draft.original?.id
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val name = draft.name
        val price = draft.price
        val discountPrice = draft.discountPrice
        val description = draft.description
        val category = draft.category
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (
            editingId == null &&
            name.trim().isBlank() &&
            price.trim().isBlank() &&
            discountPrice.trim().isBlank() &&
            description.trim().isBlank() &&
            category == null &&
            !draft.isRecommended &&
            !draft.isHidden
        ) {
            clearItemEditorDraftFromStorage(ctx)
            return
        }

        saveItemEditorDraftToStorage(
            context = ctx,
            draft = CachedItemEditorDraft(
                editingId = editingId,
                isNew = draft.isNew,
                name = name,
                price = price,
                discountPrice = discountPrice,
                description = description,
                category = category
            )
        )
    }

    private fun loadItemEditorDraftLocally(): CachedItemEditorDraft? {
        val ctx = appContext ?: return null
        return loadItemEditorDraftFromStorage(ctx)
    }

    private fun clearItemEditorDraftLocally() {
        val ctx = appContext ?: return
        clearItemEditorDraftFromStorage(ctx)
    }

    private fun toCard(
        e: AnnouncementEntity,
        showYear: Boolean = true
    ): ShowcaseAnnouncementCard {
        val normalizedBody = e.body.trim()

        val preview = normalizedBody
            .replace("\n", " ")
            .trim()
            .let { if (it.length <= 120) it else it.take(120) + "…" }

        return ShowcaseAnnouncementCard(
            id = e.id,
            coverUrl = e.coverUrl,
            bodyPreview = preview,
            bodyText = normalizedBody,
            timeText = if (showYear) {
                announcementPublishedTimeText(e.updatedAt)
            } else {
                announcementDraftTimeText(e.updatedAt)
            },
            viewCount = e.viewCount
        )
    }

    private fun rebuildAnnouncementsList() {
        val published = adminAnnouncements
            .filter { it.status == AnnouncementStatus.Published }
            .sortedByDescending { it.updatedAt }
            .map { toCard(it, showYear = true) }

        val drafts = adminAnnouncements
            .filter { it.status == AnnouncementStatus.Draft }
            .sortedByDescending { it.updatedAt }
            .map { toCard(it, showYear = false) }

        uiState = uiState.copy(
            announcements = published,
            adminAnnouncementDraftItems = drafts
        )
    }
    fun onAdminAnnouncementOpenItem(id: String) {
        val e = adminAnnouncements.firstOrNull {
            it.id == id && it.status == AnnouncementStatus.Draft
        } ?: return

        uiState = uiState.copy(
            adminAnnouncementComposerExpanded = true,
            adminAnnouncementCoverDraftUrl = e.coverUrl,
            adminAnnouncementBodyDraft = e.body,
            adminAnnouncementEditingId = e.id,
            adminAnnouncementSelectedIds = emptySet(),
            adminAnnouncementPreviewId = null,
            adminAnnouncementError = null,
            adminAnnouncementSuccess = null
        )
        persistAdminAnnouncementEditorDraftLocally()
    }
    fun onAdminAnnouncementSaveDraft() {
        val body = uiState.adminAnnouncementBodyDraft.trim()
        if (body.isBlank()) {
            uiState = uiState.copy(adminAnnouncementError = "Content is required.")
            return
        }

        val now = System.currentTimeMillis()
        val id = uiState.adminAnnouncementEditingId ?: UUID.randomUUID().toString()
        val oldEntity = adminAnnouncements.firstOrNull { it.id == id }

        val newEntity = AnnouncementEntity(
            id = id,
            coverUrl = uiState.adminAnnouncementCoverDraftUrl,
            body = body,
            status = AnnouncementStatus.Draft,
            updatedAt = now,
            viewCount = oldEntity?.viewCount ?: 0
        )

        uiState = uiState.copy(
            adminAnnouncementError = null,
            adminAnnouncementSuccess = null,
            adminAnnouncementIsSubmitting = true,
            adminAnnouncementIsBlocking = true,
            statusMessage = null
        )

        viewModelScope.launch {
            runCatching {
                val safeContext = appContext

                val uploadedCoverUrl = newEntity.coverUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { raw ->
                        if (isLocalImageUri(raw) && safeContext != null) {
                            val uploaded = uploadStoreImageIfNeeded(
                                context = safeContext,
                                uri = Uri.parse(raw),
                                kind = "announcement"
                            )
                            if (uploaded.isNullOrBlank() && handleMerchantStoreImageUploadExpiredIfNeeded(safeContext)) {
                                return@launch
                            }
                            uploaded
                        } else {
                            raw
                        }
                    }

                val cloudEntity = newEntity.copy(
                    coverUrl = uploadedCoverUrl
                )

                val upsertOk = showcaseCloudRepository.upsertAnnouncement(
                    ShowcaseCloudRepository.CloudAnnouncement(
                        id = cloudEntity.id,
                        storeId = chatStoreId,
                        coverUrl = cloudEntity.coverUrl,
                        body = cloudEntity.body,
                        status = "draft",
                        updatedAt = cloudEntity.updatedAt,
                        createdAt = null,
                        viewCount = cloudEntity.viewCount
                    )
                )
                Log.d("NDJC_PUSH", "upsertAnnouncement draft result=$upsertOk, id=${cloudEntity.id}, storeId=$chatStoreId")

                if (upsertOk) {
                    clearAnnouncementDraftLocalImages()
                    clearAdminAnnouncementEditorDraftLocally()
                    syncMerchantAnnouncementsFromCloud()
                    uiState = uiState.copy(
                        adminAnnouncementComposerExpanded = false,
                        adminAnnouncementCoverDraftUrl = null,
                        adminAnnouncementBodyDraft = "",
                        adminAnnouncementEditingId = null,
                        adminAnnouncementSelectedIds = emptySet(),
                        adminAnnouncementPreviewId = null,
                        adminAnnouncementError = null,
                        adminAnnouncementSuccess = "Draft saved.",
                        adminAnnouncementIsSubmitting = false,
                        adminAnnouncementIsBlocking = false,
                        statusMessage = "Draft saved."
                    )
                } else {
                    val safeContext = appContext
                    if (safeContext != null && handleMerchantAuthExpiredIfNeeded(
                            context = safeContext,
                            code = showcaseCloudRepository.lastAnnouncementUpsertCode,
                            body = showcaseCloudRepository.lastAnnouncementUpsertBody
                        )
                    ) {
                        return@launch
                    }

                    uiState = uiState.copy(
                        adminAnnouncementError = "Cloud save failed.",
                        adminAnnouncementSuccess = null,
                        adminAnnouncementIsSubmitting = false,
                        adminAnnouncementIsBlocking = false,
                        statusMessage = "Couldn't save draft. Please try again."
                    )
                }
            }.onFailure { e ->
                Log.e("NDJC_ANNOUNCEMENT", "onAdminAnnouncementSaveDraft failed", e)
                uiState = uiState.copy(
                    adminAnnouncementError = e.message ?: "Cloud save failed.",
                    adminAnnouncementSuccess = null,
                    adminAnnouncementIsSubmitting = false,
                    adminAnnouncementIsBlocking = false,
                    statusMessage = "Couldn't save draft. Please try again."
                )
            }
        }
    }
    fun onAdminAnnouncementPushNow() {
        val now = System.currentTimeMillis()
        val draftBody = uiState.adminAnnouncementBodyDraft.trim()
        val selectedIds = uiState.adminAnnouncementSelectedIds

        val selectedDraft = if (draftBody.isBlank() && selectedIds.size == 1) {
            val selectedId = selectedIds.first()
            adminAnnouncements.firstOrNull {
                it.id == selectedId && it.status == AnnouncementStatus.Draft
            }
        } else {
            null
        }

        if (draftBody.isBlank() && selectedDraft == null) {
            uiState = uiState.copy(
                adminAnnouncementError = "Content is required.",
                adminAnnouncementSuccess = null
            )
            return
        }

        val targetId = selectedDraft?.id
            ?: uiState.adminAnnouncementEditingId
            ?: UUID.randomUUID().toString()

        val existingEntity = adminAnnouncements.firstOrNull { it.id == targetId }

        val newEntity = AnnouncementEntity(
            id = targetId,
            coverUrl = selectedDraft?.coverUrl ?: uiState.adminAnnouncementCoverDraftUrl,
            body = selectedDraft?.body ?: draftBody,
            status = AnnouncementStatus.Published,
            updatedAt = now,
            viewCount = selectedDraft?.viewCount ?: existingEntity?.viewCount ?: 0
        )

        uiState = uiState.copy(
            adminAnnouncementError = null,
            adminAnnouncementSuccess = null,
            adminAnnouncementIsSubmitting = true,
            adminAnnouncementIsBlocking = true,
            statusMessage = null
        )

        viewModelScope.launch {
            runCatching {
                val safeContext = appContext

                val uploadedCoverUrl = newEntity.coverUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { raw ->
                        if (isLocalImageUri(raw) && safeContext != null) {
                            val uploaded = uploadStoreImageIfNeeded(
                                context = safeContext,
                                uri = Uri.parse(raw),
                                kind = "announcement"
                            )
                            if (uploaded.isNullOrBlank() && handleMerchantStoreImageUploadExpiredIfNeeded(safeContext)) {
                                return@launch
                            }
                            uploaded
                        } else {
                            raw
                        }
                    }

                val cloudEntity = newEntity.copy(
                    coverUrl = uploadedCoverUrl
                )

                val upsertOk = showcaseCloudRepository.upsertAnnouncement(
                    ShowcaseCloudRepository.CloudAnnouncement(
                        id = cloudEntity.id,
                        storeId = chatStoreId,
                        coverUrl = cloudEntity.coverUrl,
                        body = cloudEntity.body,
                        status = "published",
                        updatedAt = cloudEntity.updatedAt,
                        createdAt = null,
                        viewCount = cloudEntity.viewCount
                    )
                )
                Log.d("NDJC_PUSH", "upsertAnnouncement published result=$upsertOk, id=${cloudEntity.id}, storeId=$chatStoreId")

                if (upsertOk) {
                    val pushOk = showcaseCloudRepository.dispatchAnnouncementPush(
                        storeId = chatStoreId,
                        announcementId = targetId,
                        bodyPreview = "Posted a new announcement"
                    )
                    Log.d("NDJC_PUSH", "dispatchAnnouncementPush result=$pushOk, announcementId=$targetId, storeId=$chatStoreId")

                    val safeContext = appContext
                    if (!pushOk && safeContext != null && handleMerchantAuthExpiredIfNeeded(
                            context = safeContext,
                            code = showcaseCloudRepository.lastAnnouncementPushCode,
                            body = showcaseCloudRepository.lastAnnouncementPushBody
                        )
                    ) {
                        return@launch
                    }
                    clearAnnouncementDraftLocalImages()
                    clearAdminAnnouncementEditorDraftLocally()
                    syncMerchantAnnouncementsFromCloud()
                    uiState = uiState.copy(
                        adminAnnouncementComposerExpanded = false,
                        adminAnnouncementCoverDraftUrl = null,
                        adminAnnouncementBodyDraft = "",
                        adminAnnouncementEditingId = null,
                        adminAnnouncementSelectedIds = emptySet(),
                        adminAnnouncementPreviewId = null,
                        adminAnnouncementError = null,
                        adminAnnouncementSuccess = "Announcement published.",
                        adminAnnouncementIsSubmitting = false,
                        adminAnnouncementIsBlocking = false,
                        statusMessage = "Announcement published."
                    )

                    kotlinx.coroutines.delay(800L)

                    uiState = uiState.copy(
                        screen = ShowcaseScreen.Admin,
                        statusMessage = null
                    )
                } else {
                    val safeContext = appContext
                    if (safeContext != null && handleMerchantAuthExpiredIfNeeded(
                            context = safeContext,
                            code = showcaseCloudRepository.lastAnnouncementUpsertCode,
                            body = showcaseCloudRepository.lastAnnouncementUpsertBody
                        )
                    ) {
                        return@launch
                    }

                    uiState = uiState.copy(
                        adminAnnouncementError = "Cloud publish failed.",
                        adminAnnouncementSuccess = null,
                        adminAnnouncementIsSubmitting = false,
                        adminAnnouncementIsBlocking = false,
                        statusMessage = "Couldn't publish announcement. Please try again."
                    )
                }
            }.onFailure { e ->
                Log.e("NDJC_ANNOUNCEMENT", "onAdminAnnouncementPushNow failed", e)
                uiState = uiState.copy(
                    adminAnnouncementError = e.message ?: "Cloud publish failed.",
                    adminAnnouncementSuccess = null,
                    adminAnnouncementIsSubmitting = false,
                    adminAnnouncementIsBlocking = false,
                    statusMessage = "Couldn't publish announcement. Please try again."
                )
            }
        }
    }
    fun onAnnouncementExpanded(id: String) {
        Log.d("NDJC_CLICK", "onAnnouncementExpanded enter id=$id")

        val idx = adminAnnouncements.indexOfFirst {
            it.id == id && it.status == AnnouncementStatus.Published
        }

        Log.d(
            "NDJC_CLICK",
            "onAnnouncementExpanded matchResult idx=$idx publishedCount=${adminAnnouncements.count { it.status == AnnouncementStatus.Published }}"
        )

        if (idx < 0) {
            Log.e("NDJC_CLICK", "onAnnouncementExpanded return because idx < 0, id=$id")
            return
        }

        markAnnouncementViewedLocally(id)
        trackAnnouncementClickOnce(id)
    }

    fun onAnnouncementImageOpened(id: String) {
        val targetId = id.trim()
        if (targetId.isBlank()) return

        val exists = adminAnnouncements.any {
            it.id == targetId && it.status == AnnouncementStatus.Published
        }
        if (!exists) return

        markAnnouncementViewedLocally(targetId)
        trackAnnouncementClickOnce(targetId)
    }

    fun onAdminAnnouncementPreviewItem(id: String) {
        val exists = adminAnnouncements.any { it.id == id && it.status == AnnouncementStatus.Draft }
        if (!exists) return

        uiState = uiState.copy(
            adminAnnouncementPreviewId = id,
            adminAnnouncementError = null
        )
    }

    fun onAdminAnnouncementDismissPreview() {
        uiState = uiState.copy(adminAnnouncementPreviewId = null)
    }

    fun onAdminAnnouncementToggleSelect(id: String) {
        val cur = uiState.adminAnnouncementSelectedIds
        val next = if (cur.contains(id)) cur - id else cur + id
        uiState = uiState.copy(adminAnnouncementSelectedIds = next)
    }

    fun onAdminAnnouncementClearSelection() {
        uiState = uiState.copy(adminAnnouncementSelectedIds = emptySet())
    }

    fun onAdminAnnouncementDeleteSelected() {
        val selected = uiState.adminAnnouncementSelectedIds
        if (selected.isEmpty()) return

        adminAnnouncements.removeAll { it.status == AnnouncementStatus.Draft && selected.contains(it.id) }
        rebuildAnnouncementsList()
        viewModelScope.launch {
            val deleteOk = showcaseCloudRepository.deleteAnnouncements(
                storeId = chatStoreId,
                ids = selected.toList()
            )
            Log.d("NDJC_PUSH", "deleteAnnouncements result=$deleteOk, ids=$selected")

            if (!deleteOk) {
                val safeContext = appContext
                if (safeContext != null && handleMerchantDeleteExpiredIfNeeded(safeContext)) {
                    return@launch
                }
            }
        }
        val clearedEditingId = if (uiState.adminAnnouncementEditingId != null && selected.contains(uiState.adminAnnouncementEditingId)) {
            null
        } else {
            uiState.adminAnnouncementEditingId
        }

        val clearedCover =
            if (clearedEditingId == null) null else uiState.adminAnnouncementCoverDraftUrl

        val clearedBody =
            if (clearedEditingId == null) "" else uiState.adminAnnouncementBodyDraft

        val clearedPreviewId =
            if (uiState.adminAnnouncementPreviewId != null && selected.contains(uiState.adminAnnouncementPreviewId)) {
                null
            } else {
                uiState.adminAnnouncementPreviewId
            }

        uiState = uiState.copy(
            adminAnnouncementEditingId = clearedEditingId,
            adminAnnouncementCoverDraftUrl = clearedCover,
            adminAnnouncementBodyDraft = clearedBody,
            adminAnnouncementSelectedIds = emptySet(),
            adminAnnouncementPreviewId = clearedPreviewId,
            adminAnnouncementError = null,
            adminAnnouncementSuccess = "Deleted ${selected.size} draft(s)."
        )

        if (clearedEditingId == null) {
            clearAdminAnnouncementEditorDraftLocally()
        } else {
            persistAdminAnnouncementEditorDraftLocally()
        }
    }

    private fun clearAdminAnnouncementComposerState() {
        clearAnnouncementDraftLocalImages()
        clearAdminAnnouncementEditorDraftLocally()
        uiState = uiState.copy(
            adminAnnouncementComposerExpanded = false,
            adminAnnouncementCoverDraftUrl = null,
            adminAnnouncementBodyDraft = "",
            adminAnnouncementEditingId = null,
            adminAnnouncementSelectedIds = emptySet(),
            adminAnnouncementPreviewId = null,
            adminAnnouncementError = null,
            adminAnnouncementSuccess = null
        )
    }

    fun hasUnsavedAdminAnnouncementDraft(): Boolean {
        val currentBody = uiState.adminAnnouncementBodyDraft.trim()
        val currentCover = uiState.adminAnnouncementCoverDraftUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val editingId = uiState.adminAnnouncementEditingId

        if (editingId == null) {
            return currentBody.isNotBlank() || currentCover != null
        }

        val original = adminAnnouncements.firstOrNull {
            it.id == editingId && it.status == AnnouncementStatus.Draft
        } ?: return currentBody.isNotBlank() || currentCover != null

        val originalBody = original.body.trim()
        val originalCover = original.coverUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return currentBody != originalBody || currentCover != originalCover
    }

    fun discardAdminAnnouncementDraftAndBack() {
        clearAdminAnnouncementComposerState()
        uiState = uiState.copy(screen = ShowcaseScreen.Admin)
    }

    fun discardAdminAnnouncementDraftAndGoHome() {
        clearAdminAnnouncementComposerState()
        backToHome()
    }

    fun getAdminDraftCardsForUi(): List<ShowcaseAnnouncementCard> {
        return uiState.adminAnnouncementDraftItems
    }

    fun getAdminPublishedCardsForUi(): List<ShowcaseAnnouncementCard> {
        return adminAnnouncements
            .filter { it.status == AnnouncementStatus.Published }
            .sortedByDescending { it.updatedAt }
            .map { toCard(it, showYear = true) }
    }
    fun onFavoritesQueryChange(q: String) {
        favoritesUiState = favoritesUiState.copy(query = q)
        refreshFavoritesList()
    }

    fun onFavoritesSortModeChange(mode: ShowcaseHomeSortMode) {
        favoritesUiState = favoritesUiState.copy(sortMode = mode)
        refreshFavoritesList()
    }

    fun onFavoritesFilterRecommendedOnlyChange(checked: Boolean) {
        favoritesUiState = favoritesUiState.copy(filterRecommendedOnly = checked)
        refreshFavoritesList()
    }

    fun onFavoritesFilterOnSaleOnlyChange(checked: Boolean) {
        favoritesUiState = favoritesUiState.copy(filterOnSaleOnly = checked)
        refreshFavoritesList()
    }


    fun onFavoritesShowSortMenuChange(show: Boolean) {
        favoritesUiState = favoritesUiState.copy(showSortMenu = show)
    }

    fun onFavoritesShowFilterMenuChange(show: Boolean) {
        favoritesUiState = favoritesUiState.copy(showFilterMenu = show)
    }
    fun onFavoritesShowPriceMenuChange(show: Boolean) {
        favoritesUiState = favoritesUiState.copy(showPriceMenu = show)
    }

    fun onFavoritesPriceMinDraftChange(v: String) {
        favoritesUiState = favoritesUiState.copy(priceMinDraft = v)
    }

    fun onFavoritesPriceMaxDraftChange(v: String) {
        favoritesUiState = favoritesUiState.copy(priceMaxDraft = v)
    }

    fun onFavoritesApplyPriceRange() {
        val min = favoritesUiState.priceMinDraft.trim().toIntOrNull()
        val max = favoritesUiState.priceMaxDraft.trim().toIntOrNull()
        val (mn, mx) = if (min != null && max != null && min > max) (max to min) else (min to max)

        favoritesUiState = favoritesUiState.copy(
            appliedMinPrice = mn,
            appliedMaxPrice = mx,
            showPriceMenu = false
        )
        refreshFavoritesList()
    }

    fun onFavoritesClearPriceRange() {
        favoritesUiState = favoritesUiState.copy(
            priceMinDraft = "",
            priceMaxDraft = "",
            appliedMinPrice = null,
            appliedMaxPrice = null,
            showPriceMenu = false
        )
        refreshFavoritesList()
    }
    fun onFavoritesClearSortAndFilters() {
        favoritesUiState = favoritesUiState.copy(
            sortMode = ShowcaseHomeSortMode.Default,
            filterRecommendedOnly = false,
            filterOnSaleOnly = false,
            showSortMenu = false,
            showFilterMenu = false
        )
        refreshFavoritesList()
    }

    fun onFavoritesCategorySelected(category: String?) {
        favoritesUiState = favoritesUiState.copy(selectedCategory = category)
        refreshFavoritesList()
    }

    // ✅ 勾选/取消勾选（只改 favoritesUiState，不越界）
    fun toggleFavoritesSelection(dishId: String) {
        val id = dishId.trim()
        if (id.isBlank()) return

        val cur = favoritesUiState.selectedIds
        val next = if (cur.contains(id)) cur - id else cur + id
        favoritesUiState = favoritesUiState.copy(selectedIds = next)
    }

    // ✅ 清空勾选
    fun clearFavoritesSelection() {
        if (favoritesUiState.selectedIds.isEmpty()) return
        favoritesUiState = favoritesUiState.copy(selectedIds = emptySet())
    }

    // ✅ 删除已勾选（真正取消收藏）
    fun deleteSelectedFavorites() {
        val sel = favoritesUiState.selectedIds
        if (sel.isEmpty()) return
// 从收藏集合里移除
        favoriteIds = favoriteIds - sel

// 同步移除时间戳
        sel.forEach { favoriteAddedAt.remove(it) }

        persistFavoritesToStorage()

        // 清空勾选 + 刷新列表
        favoritesUiState = favoritesUiState.copy(selectedIds = emptySet())
        refreshFavoritesList()
    }

    /**
     * 详情页/列表页点击“收藏”按钮调用
     */
    fun toggleFavorite(dishId: String) {
        val id = dishId.trim()
        if (id.isBlank()) return

        val isRemoving = favoriteIds.contains(id)
        favoriteIds = if (isRemoving) {
            favoriteIds - id
        } else {
            favoriteIds + id
        }

        if (isRemoving) {
            favoriteAddedAt.remove(id)
        } else {
            favoriteAddedAt[id] = System.currentTimeMillis()
        }

        persistFavoritesToStorage()
        refreshFavoritesList()
    }


    fun isFavorite(dishId: String): Boolean {
        return favoriteIds.contains(dishId)
    }
    fun refreshFavoritesList() {
        val all = uiState.dishes

        // 1) 收藏全集（不受筛选/搜索影响，用它派生分类 chips）
        val favsAll = all.filter { d -> favoriteIds.contains(d.id.toString()) }

        // 2) 分类 chips：只显示收藏里出现过的分类
        val categories = favsAll
            .map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // 如果当前选中分类已消失（取消收藏导致），回退到 All（null）
        val selectedCategory = favoritesUiState.selectedCategory
            ?.trim()
            ?.takeIf { it.isNotBlank() && categories.contains(it) }

        // 3) 从收藏全集派生“可见列表”
        var seq = favsAll.asSequence()

        // 分类筛选
        if (selectedCategory != null) {
            seq = seq.filter { it.category == selectedCategory }
        }

        // Home 同款 filters
        if (favoritesUiState.filterRecommendedOnly) {
            seq = seq.filter { it.isRecommended }
        }
        if (favoritesUiState.filterOnSaleOnly) {
            seq = seq.filter { it.discountPrice != null && it.discountPrice > 0f }
        }

        // 搜索（按名称）
        val q = favoritesUiState.query.trim()
        if (q.isNotBlank()) {
            val qq = q.lowercase()
            seq = seq.filter { d ->
                val title = (d.nameZh.ifBlank { d.nameEn }).lowercase()
                title.contains(qq)
            }
        }
// ✅ 价格区间筛选
        val minP = favoritesUiState.appliedMinPrice
        val maxP = favoritesUiState.appliedMaxPrice
        if (minP != null || maxP != null) {
            seq = seq.filter { d ->
                val p = (d.discountPrice ?: d.originalPrice)
                (minP == null || p >= minP.toFloat()) &&
                        (maxP == null || p <= maxP.toFloat())
            }
        }

        // Home 同款排序（Price / Name）
// 排序（Default / PriceAsc / PriceDesc）
        val sorted = when (favoritesUiState.sortMode) {
            // ✅ Favorites 默认：新收藏靠上（按收藏时间倒序）
            ShowcaseHomeSortMode.Default ->
                seq.sortedByDescending { favoriteAddedAt[it.id] ?: 0L }.toList()

            ShowcaseHomeSortMode.PriceAsc ->
                seq.sortedBy { it.discountPrice ?: it.originalPrice }.toList()

            ShowcaseHomeSortMode.PriceDesc ->
                seq.sortedByDescending { it.discountPrice ?: it.originalPrice }.toList()
        }
        val cards = sorted.map { d ->
            val title = d.nameZh.ifBlank { d.nameEn }

            val originalText = moneyTrim2(d.originalPrice)
            val discountText = d.discountPrice?.let { moneyTrim2(it) }

            ShowcaseFavoriteCard(
                dishId = d.id,
                title = title,

                category = d.category,

                originalPriceText = originalText,
                discountPriceText = discountText,

                // ✅ 兼容：旧字段仍然填（折后优先）
                priceText = (discountText ?: originalText),

                imageUrl = d.imageUri?.toString()
            )
        }


        // 勾选集交集（避免已取消收藏的 id 还在 selectedIds 里）
        val sel = favoritesUiState.selectedIds.filter { favoriteIds.contains(it) }.toSet()

        favoritesUiState = favoritesUiState.copy(
            items = cards,
            selectedIds = sel,
            categories = categories,
            selectedCategory = selectedCategory
        )
    }


// 新增：Chat 专用云端仓库
    private val chatCloudRepository = ShowcaseChatCloudRepository()

    // Chat Repo 注入 Chat 云端（只影响 chat）
    private val chatRepository = ShowcaseChatRepository(cloud = chatCloudRepository)

    // ✅ 关键：把 chatCloudRepository 注入 Domain，isCloud() 才会为 true
    private val chatDomain = ShowcaseChatDomain(
        repo = chatRepository,
        cloudRepo = chatCloudRepository
    )

    private val chatListDomain = ShowcaseChatListDomain(chatRepository)

    // ✅ Chat 页是否处于可见（由 onChatScreenVisible/Hidden 控制）
    private var chatScreenVisible: Boolean = false

    private suspend fun awaitFcmToken(): String? {
        val ctx = appContext
        Log.d("NDJC_PUSH", "awaitFcmToken start, appContextNull=${ctx == null}")

        if (ctx != null) {
            val app = FirebaseApp.initializeApp(ctx)
            Log.d("NDJC_PUSH", "FirebaseApp.initializeApp result=$app")
        }

        return withTimeoutOrNull(15000L) {
            suspendCancellableCoroutine { cont ->
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        Log.d(
                            "NDJC_PUSH",
                            "awaitFcmToken complete, success=${task.isSuccessful}, exception=${task.exception}"
                        )

                        if (!cont.isActive) {
                            Log.d("NDJC_PUSH", "awaitFcmToken ignored because continuation is not active")
                            return@addOnCompleteListener
                        }

                        if (task.isSuccessful) {
                            Log.d("NDJC_PUSH", "awaitFcmToken token length=${task.result?.length ?: -1}")
                            cont.resume(task.result)
                        } else {
                            Log.e("NDJC_PUSH", "awaitFcmToken failed", task.exception)
                            cont.resume(null)
                        }
                    }
            }
        }.also {
            if (it == null) {
                Log.e("NDJC_PUSH", "awaitFcmToken timeout after 15000ms")
            }
        }
    }

    fun ensurePushRegistration(context: Context, audience: String) {
        appContext = context.applicationContext
        Log.d("NDJC_PUSH", "ensurePushRegistration called, audience=$audience")

        viewModelScope.launch {
            val token = awaitFcmToken()?.trim().orEmpty()
            Log.d("NDJC_PUSH", "fcm token length=${token.length}")

            if (token.isBlank()) {
                Log.e("NDJC_PUSH", "fcm token is blank")
                return@launch
            }

            val conversationId =
                if (audience == "chat_client") {
                    chatDomain.uiState.conversationId
                        ?: uiState.chat.conversationId
                } else {
                    null
                }

            if (audience == "chat_client" && conversationId.isNullOrBlank()) {
                Log.d("NDJC_PUSH", "skip chat_client registration because conversationId is still blank")
                return@launch
            }

            Log.d(
                "NDJC_PUSH",
                "push registration continue even if token/audience/conversation unchanged; skip optimization disabled"
            )

            val clientId =
                if (audience == "chat_client" || audience == "announcement_subscriber") {
                    runCatching {
                        val id = chatDomain.ensureClientId(context.applicationContext)
                        Log.d("NDJC_PUSH", "clientId=$id")
                        id
                    }.getOrNull()
                } else {
                    null
                }

            Log.d(
                "NDJC_PUSH",
                "upsert push device start, storeId=$chatStoreId, audience=$audience, conversationId=$conversationId, clientId=$clientId"
            )

            val ok = cloudRepository.upsertPushDevice(
                ShowcaseCloudRepository.PushDeviceUpsert(
                    storeId = chatStoreId,
                    audience = audience,
                    token = token,
                    conversationId = conversationId,
                    clientId = clientId,
                    appVersion = null
                )
            )

            Log.d("NDJC_PUSH", "upsert push device result=$ok")

            lastRegisteredPushOk = ok

            if (ok) {
                lastRegisteredPushToken = token
                lastRegisteredPushAudience = audience
                lastRegisteredPushConversationId = conversationId
            }
        }
    }
    fun ensureAnnouncementRegistrationOnHome(context: Context) {
        appContext = context.applicationContext
        clearExpiredLocalTempFiles(context.applicationContext)

        viewModelScope.launch {
            ensurePushRegistration(context.applicationContext, "announcement_subscriber")
            kotlinx.coroutines.delay(2500L)
            ensurePushRegistration(context.applicationContext, "announcement_subscriber")
        }
    }
    init {
        viewModelScope.launch {
            refreshCloudServiceStatus()
        }

        // 1) domain -> uiState
        viewModelScope.launch {
            chatDomain.uiStateFlow
                .collect { chat ->
                    uiState = uiState.copy(chat = chat)
                }
        }

        // 2) conversationId 就绪后，自动重启 Chat 实时链路
        viewModelScope.launch {
            chatDomain.uiStateFlow
                .map { it.conversationId }
                .distinctUntilChanged()
                .collect { convId ->
                    if (!chatScreenVisible) return@collect
                    if (convId.isNullOrBlank()) return@collect

                    android.util.Log.e(
                        "ChatTrace",
                        "VM_CONV_READY convId=$convId (auto start realtime)"
                    )

                    activeConversationId = convId
                    ShowcaseRuntimeState.setActiveConversationId(convId)
                    if (chatScreenVisible) {
                        ShowcaseRuntimeState.markConversationVisible(convId)
                    }

                    val ctx = appContext ?: return@collect
                    stopChatDbObserve()
                    stopChatPolling()
                    chatLastAppliedLatestId = null
                    startChatDbObserve(ctx, convId)
                    startChatPolling()
                }
        }

        // 3) MerchantChatList 进入/离开时，自动管理本地观察 + 轮询
        viewModelScope.launch {
            uiStateFlow
                .map { it.screen }
                .distinctUntilChanged()
                .collect { screen ->
                    if (screen == ShowcaseScreen.MerchantChatList) {
                        startMerchantChatListDbObserve()
                        startMerchantChatListPolling()
                    } else {
                        stopMerchantChatListDbObserve()
                        stopMerchantChatListPolling()
                    }
                }
        }
    }

    // ✅ 保证：只要 convId 有了，就一定启动 observe + polling
    private fun ensureChatRealtimeStarted(convId: String) {
        activeConversationId = convId

        val ctx = appContext
        if (ctx == null) {
            Log.e("ChatTrace", "ensureRealtime: appContext=null (cannot start observe yet) convId=$convId")
            // polling 也依赖 ctx 时，你可以选择不启；这里先不启，等 visible 设置 ctx 后再触发一次
            return
        }

        startChatDbObserve(ctx, convId)
        startChatPolling()
    }
    // Chat 返回目标
    private var chatBackTarget: ShowcaseScreen = ShowcaseScreen.Home
    // ✅ 当前 Chat 身份只看稳定的 chatMode，不看导航返回目标
    private fun currentChatRole(): String {
        return when (chatMode) {
            ChatMode.Merchant -> "merchant"
            ChatMode.Client -> "client"
        }
    }

    // ✅ Detail 返回目标：从哪个 screen 打开详情，就回哪个
    private var detailBackTarget: ShowcaseScreen = ShowcaseScreen.Home

    // ✅ 关键：从“Detail -> Chat”进入时，缓存 Detail 的上下文，保证 Chat Back 能恢复回原 Detail
    private var chatBackDetailDish: DemoDish? = null
    private var chatBackDetailBackTarget: ShowcaseScreen? = null

    // ✅ 仅用于“从查找页点结果跳回 Chat”这一种场景：让 Chat 的 Back 先回查找页一次


    private var chatPollingJob: kotlinx.coroutines.Job? = null
    private var chatDbObserveJob: kotlinx.coroutines.Job? = null
    private var chatEntryPollingJob: kotlinx.coroutines.Job? = null
    private var announcementsEntryPollingJob: kotlinx.coroutines.Job? = null
    // ✅ 关键：Chat 页实时链路只认这个稳定会话ID，避免 uiState/chatDomain 状态被重置导致 convId=null
    private var activeConversationId: String? = null
    private var chatSearchBackTarget: ShowcaseScreen = ShowcaseScreen.Chat

    // ✅ 搜索范围：从 Chat 进入=仅当前会话；从 ChatList 进入=现存会话(名称+消息)
    private enum class ChatSearchScope { InConversation, InExistingThreads }
    private var chatSearchScope: ChatSearchScope = ChatSearchScope.InExistingThreads

    private var chatBackTargetBeforeSearch: ShowcaseScreen = ShowcaseScreen.Home

    // ✅ 兜底：记录“最近一次已经同步到 UI 的 Room 最新消息 id”
// 目的：就算 DB observe job 偶发没推 UI，poll 也能把 UI 拉到最新
    private var chatLastAppliedLatestId: String? = null




// -------------------- Chat lifecycle (wiring hooks) --------------------

    fun onChatScreenVisible(context: Context) {
        // ✅ Chat 页可见时，停止底栏入口轻量轮询，避免双轮询
        stopChatEntryPolling()

        // ✅ 关键：Chat 页进入时立刻打“可见”标记
        chatScreenVisible = true
        ShowcaseRuntimeState.setChatVisible(true)

        // ✅ 进入 Chat 页即视为入口已读，先清红点缓存值
        _chatEntryDot.value = false

        val domainConv = chatDomain.uiState.conversationId
        val uiConv = uiState.chat.conversationId
        android.util.Log.e(
            "ChatTrace",
            "VM_VISIBLE enter activeConv=$activeConversationId domainConv=$domainConv uiConv=$uiConv " +
                    "pollActive=${chatPollingJob?.isActive == true} obsActive=${chatDbObserveJob?.isActive == true}"
        )

        // ✅ 只负责更新 ctx
        appContext = context.applicationContext

// ✅ 进入 Chat 时优先用 domainConv；如果 domain 还没回填，就退回 activeConversationId
        val convId = chatDomain.uiState.conversationId ?: activeConversationId

        android.util.Log.e(
            "ChatTrace",
            "VM_VISIBLE use convId=$convId activeConv=$activeConversationId domainConv=${chatDomain.uiState.conversationId} uiConv=$uiConv"
        )

        if (convId.isNullOrBlank()) {
            ShowcaseRuntimeState.setActiveConversationId(null)
            android.util.Log.e(
                "ChatTrace",
                "VM_VISIBLE convId not ready yet -> wait VM_CONV_READY"
            )
            return
        }

// ✅ 缓存回 activeConversationId，避免别处继续读到 null
        activeConversationId = convId
        ShowcaseRuntimeState.markConversationVisible(convId)

        startChatDbObserve(context.applicationContext, conversationId = convId)
        startChatPolling()
    }

    fun onChatScreenHidden() {
        chatScreenVisible = false

        val leavingConvId = chatDomain.uiState.conversationId ?: activeConversationId
        ShowcaseRuntimeState.markConversationRecentlySeen(leavingConvId)
        ShowcaseRuntimeState.setChatVisible(false)
        ShowcaseRuntimeState.setActiveConversationId(null)

        val domainConv = chatDomain.uiState.conversationId
        val uiConv = uiState.chat.conversationId
        Log.e(
            "ChatTrace",
            "VM_HIDDEN enter activeConv=$activeConversationId domainConv=$domainConv uiConv=$uiConv " +
                    "screen=${uiState.screen} chatVisible=$chatScreenVisible role=${currentChatRole()} " +
                    "pollActive=${chatPollingJob?.isActive == true} obsActive=${chatDbObserveJob?.isActive == true}"
        )

        stopChatPolling()
        stopChatDbObserve()

        viewModelScope.launch {
            kotlinx.coroutines.delay(120L)
            runCatching { refreshChatEntryDotOnce() }
        }
    }
    // ✅ Merchant ChatList 轮询 job：商家停在列表页时自动刷新（拉 relay / 刷 threads）
    private var merchantChatListPollJob: kotlinx.coroutines.Job? = null

    // ✅ Merchant ChatList 刷新 job：保证同一时刻只跑一个刷新，避免堆叠/覆盖导致 UI 不更新
    private var merchantChatListRefreshJob: kotlinx.coroutines.Job? = null

    // ✅ Merchant ChatList 本地观察 job：Room 一有变化就立刻重建列表
    private var merchantChatListDbObserveJob: kotlinx.coroutines.Job? = null


    private fun stopMerchantChatListPolling() {
        merchantChatListPollJob?.cancel()
        merchantChatListPollJob = null
    }

    private fun stopMerchantChatListDbObserve() {
        merchantChatListDbObserveJob?.cancel()
        merchantChatListDbObserveJob = null
    }

    fun startChatEntryPolling(context: Context) {
        appContext = context.applicationContext

        if (chatEntryPollingJob?.isActive == true) return

        chatEntryPollingJob = viewModelScope.launch {
            refreshChatEntryDotOnce()

            while (currentCoroutineContext().isActive) {
                try {
                    refreshChatEntryDotOnce()
                } catch (t: Throwable) {
                    android.util.Log.e(
                        "ChatTrace",
                        "[entry-dot] FAILED ${t.javaClass.simpleName}: ${t.message}",
                        t
                    )
                }
                kotlinx.coroutines.delay(2000L)
            }
        }
    }

    fun stopChatEntryPolling() {
        chatEntryPollingJob?.cancel()
        chatEntryPollingJob = null
    }

    fun startAnnouncementsEntryPolling(context: Context) {
        appContext = context.applicationContext

        if (announcementsEntryPollingJob?.isActive == true) return

        announcementsEntryPollingJob = viewModelScope.launch {
            refreshAnnouncementsEntryDotOnce()

            while (currentCoroutineContext().isActive) {
                try {
                    refreshAnnouncementsEntryDotOnce()
                } catch (t: Throwable) {
                    android.util.Log.e(
                        "AnnouncementTrace",
                        "[entry-dot] FAILED ${t.javaClass.simpleName}: ${t.message}",
                        t
                    )
                }
                kotlinx.coroutines.delay(2500L)
            }
        }
    }

    fun stopAnnouncementsEntryPolling() {
        announcementsEntryPollingJob?.cancel()
        announcementsEntryPollingJob = null
    }

    private suspend fun refreshAnnouncementsEntryDotOnce() {
        val ctx = appContext ?: return

        val cloudItems = showcaseCloudRepository.fetchAnnouncements(
            storeId = chatStoreId,
            includeDrafts = false,
            actor = ShowcaseCloudConfig.AuthActor.PUBLIC
        )

        val cachedEntities = loadPublishedAnnouncementsFromStorage(ctx).map { item ->
            toPublishedAnnouncementEntity(item)
        }

        val entities = if (cloudItems.isNotEmpty()) {
            cloudItems.map { cloud -> toAnnouncementEntity(cloud) }
        } else {
            cachedEntities
        }

        if (entities.isNotEmpty()) {
            adminAnnouncements.clear()
            adminAnnouncements.addAll(entities)
            rebuildAnnouncementsList()

            if (cloudItems.isNotEmpty()) {
                persistPublishedAnnouncementsLocally(entities)
            }
        } else {
            adminAnnouncements.clear()
            rebuildAnnouncementsList()
        }

        _announcementsEntryDot.value = entities.any {
            it.status == AnnouncementStatus.Published && !isAnnouncementSeen(it.id)
        }
    }

    private suspend fun refreshChatEntryDotOnce() {
        val ctx = appContext ?: return
        val clientId = chatDomain.ensureClientId(ctx)
        val fallbackConvId = chatDomain.resolveClientConversationId(ctx, chatStoreId)

        if (chatRepository.isChatRelayEnabled()) {
            chatRepository.consumeRelayForClient(
                context = ctx,
                storeId = chatStoreId,
                clientId = clientId,
                traceId = "T${System.currentTimeMillis()}_entryRelay"
            )
        } else {
            chatRepository.syncConversationFromCloud(
                context = ctx,
                storeId = chatStoreId,
                conversationId = fallbackConvId,
                perspectiveRole = "client",
                clientId = clientId,
                traceId = "T${System.currentTimeMillis()}_entrySync"
            )
        }

        val actualConvId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            chatRepository.findLatestConversationIdByStoreAndClient(
                context = ctx,
                storeId = chatStoreId,
                clientId = clientId
            )
        } ?: fallbackConvId

        if (activeConversationId.isNullOrBlank()) {
            activeConversationId = actualConvId
        }

        if (chatScreenVisible && currentChatRole() != "merchant" && activeConversationId == actualConvId) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                chatDomain.acknowledgeClientVisibleConversation(ctx, actualConvId)
            }
            _chatEntryDot.value = false
            return
        }

        val unread = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            chatRepository.countUnreadForUserEntryByStoreAndClient(
                context = ctx,
                storeId = chatStoreId,
                clientId = clientId
            )
        }

        android.util.Log.e(
            "ChatTrace",
            "ENTRY_DOT clientId=$clientId fallbackConvId=$fallbackConvId actualConvId=$actualConvId unread=$unread activeConv=$activeConversationId screen=${uiState.screen} chatVisible=$chatScreenVisible role=${currentChatRole()}"
        )

        _chatEntryDot.value = unread > 0
    }

    fun startMerchantChatListPolling() {
        if (merchantChatListPollJob?.isActive == true) return

        merchantChatListPollJob = viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                try {
                    if (merchantChatListRefreshJob?.isActive != true) {
                        refreshMerchantChatListSilently()
                    }
                } catch (_: Throwable) {
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun startMerchantChatListDbObserve() {
        val ctx = appContext ?: return
        if (merchantChatListDbObserveJob?.isActive == true) return

        merchantChatListDbObserveJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                chatRepository.observeLocalByStore(ctx, chatStoreId),
                chatRepository.observeThreadMetaByStore(ctx, chatStoreId)
            ) { _, _ ->
                Unit
            }.collect {
                if (uiState.screen != ShowcaseScreen.MerchantChatList) {
                    return@collect
                }

                if (chatRepository.isChatCloudEnabled() || chatRepository.isChatRelayEnabled()) {
                    android.util.Log.e(
                        "ChatTrace",
                        "merchantChatListDbObserve skip local-only rebuild because cloud/relay mode is enabled"
                    )
                    return@collect
                }

                val threads = chatListDomain.buildMerchantThreadsFromLocal(ctx, chatStoreId)
                uiState = uiState.copy(
                    merchantChatThreads = threads.toList(),
                    statusMessage = null
                )
            }
        }
    }
    private fun startChatPolling() {
        android.util.Log.e("ChatTrace", "### START_CHAT_POLLING_ENTERED ###")
        android.util.Log.e(
            "ChatTrace",
            "POLL_DEBUG domainConv=${chatDomain.uiState.conversationId} uiConv=${uiState.chat.conversationId}"
        )

        // 避免重复启动
        if (chatPollingJob?.isActive == true) return

        chatPollingJob = viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                try {
                    val ctx = appContext
                    if (ctx == null) {
                        android.util.Log.e("ChatTrace", "[poll] STOP: appContext=null (break)")
                        break
                    }

                    // ✅ 当前轮询“角色”只由 chatMode 决定，避免被旧变量污染
                    val roleForPoll = currentChatRole()

                    // ✅ 每轮必打：确认“轮询循环是否还活着”（即使 convId blank / chat disabled 也能看到）
                    android.util.Log.e(
                        "ChatTrace",
                        "[poll] HEARTBEAT role=$roleForPoll activeConv=$activeConversationId " +
                                "domainConv=${chatDomain.uiState.conversationId} uiConv=${uiState.chat.conversationId} " +
                                "relayEnabled=${chatRepository.isChatRelayEnabled()} " +
                                "cloudEnabled=${chatRepository.isChatCloudEnabled()}"
                    )

                    // ✅ relay / cloud 两种模式都允许轮询；只有两者都关掉才跳过
                    if (!chatRepository.isChatRelayEnabled() && !chatRepository.isChatCloudEnabled()) {
                        android.util.Log.e("ChatTrace", "[poll] SKIP: relay and cloud both disabled")
                        kotlinx.coroutines.delay(1200)
                        continue
                    }

                    val convId = activeConversationId
                    if (convId.isNullOrBlank()) {
                        android.util.Log.e(
                            "ChatTrace",
                            "[poll] SKIP: convId blank domainConv=${chatDomain.uiState.conversationId} uiConv=${uiState.chat.conversationId}"
                        )
                        kotlinx.coroutines.delay(1200)
                        continue
                    }

                    android.util.Log.e(
                        "ChatTrace",
                        "POLL_DEBUG tick role=$roleForPoll convId=$convId activeConv=$activeConversationId " +
                                "domainConv=${chatDomain.uiState.conversationId} uiConv=${uiState.chat.conversationId}"
                    )

                    val inserted = when {
                        roleForPoll == "merchant" && chatRepository.isChatRelayEnabled() -> {
                            chatRepository.consumeRelayForMerchant(
                                context = ctx,
                                storeId = chatStoreId,
                                traceId = "T${System.currentTimeMillis()}_pollM"
                            )
                        }

                        roleForPoll == "merchant" && chatRepository.isChatCloudEnabled() -> {
                            chatRepository.syncConversationFromCloud(
                                context = ctx,
                                storeId = chatStoreId,
                                conversationId = convId,
                                perspectiveRole = "merchant",
                                traceId = "T${System.currentTimeMillis()}_pollMCloud"
                            )
                        }

                        roleForPoll != "merchant" && chatRepository.isChatRelayEnabled() -> {
                            val clientId = chatDomain.ensureClientId(ctx)

                            chatRepository.consumeRelayForClient(
                                context = ctx,
                                storeId = chatStoreId,
                                clientId = clientId,
                                traceId = "T${System.currentTimeMillis()}_pollC"
                            )
                        }

                        roleForPoll != "merchant" && chatRepository.isChatCloudEnabled() -> {
                            val clientId = chatDomain.ensureClientId(ctx)

                            chatRepository.syncConversationFromCloud(
                                context = ctx,
                                storeId = chatStoreId,
                                conversationId = convId,
                                perspectiveRole = "client",
                                clientId = clientId,
                                traceId = "T${System.currentTimeMillis()}_pollCCloud"
                            )
                        }

                        else -> 0
                    }

                    android.util.Log.e(
                        "ChatTrace",
                        "POLL_CONSUME inserted=$inserted role=$roleForPoll convId=$convId"
                    )

                    val (cnt, latest) = chatRepository.debugRoomSnapshot(ctx)
                    val latestId = latest?.id

                    android.util.Log.e(
                        "ChatTrace",
                        "ROOM_SNAP count=$cnt latestId=$latestId latestConv=${latest?.conversationId} latestDir=${latest?.direction}"
                    )

                    val isCurrentVisibleConversation =
                        chatScreenVisible && activeConversationId == convId

                    val shouldApplyRoomToUi =
                        inserted > 0 ||
                                (latestId != null && latestId != chatLastAppliedLatestId) ||
                                isCurrentVisibleConversation

                    if (shouldApplyRoomToUi) {
                        val (list, unread) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            if (roleForPoll == "merchant") {
                                if (isCurrentVisibleConversation) {
                                    chatDomain.acknowledgeMerchantVisibleConversation(
                                        context = ctx,
                                        storeId = chatStoreId,
                                        conversationId = convId
                                    )
                                }
                                val localList = chatRepository.listLocal(ctx, convId)
                                val localUnread = chatRepository.countUnread(ctx, convId)
                                localList to localUnread
                            } else {
                                if (isCurrentVisibleConversation) {
                                    chatDomain.acknowledgeClientVisibleConversation(ctx, convId)
                                }
                                val localList = chatRepository.listLocal(ctx, convId)
                                val localUnread = chatRepository.countUnreadForUserEntry(ctx, convId)
                                localList to localUnread
                            }
                        }
                        val roomLastId = list.lastOrNull()?.id

                        android.util.Log.e(
                            "ChatTrace",
                            "[poll] room-refresh start conv=$convId roomSize=${list.size} roomLast=$roomLastId unread=$unread latestId=$latestId inserted=$inserted"
                        )

                        chatDomain.applyLocalSnapshot(convId, list, unread)
                        syncChat()

                        val uiLastId = uiState.chat.messages.lastOrNull()?.id
                        android.util.Log.e(
                            "ChatTrace",
                            "[poll] applied latestId=$latestId uiSize=${uiState.chat.messages.size} uiLast=$uiLastId inserted=$inserted"
                        )

                        chatLastAppliedLatestId = roomLastId ?: latestId
                    }

                } catch (t: Throwable) {
                    val roleForPoll = currentChatRole()
                    android.util.Log.e(
                        "ChatTrace",
                        "[poll] FAILED role=$roleForPoll store=$chatStoreId conv=${chatDomain.uiState.conversationId ?: uiState.chat.conversationId} ${t.javaClass.simpleName}: ${t.message}",
                        t
                    )
                }
                kotlinx.coroutines.delay(1200L)
            }
        }
    }
    private fun startChatDbObserve(context: Context, conversationId: String) {
        if (conversationId.isBlank()) return

        chatDbObserveJob?.cancel()
        chatDbObserveJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                chatRepository.observeLocal(context, conversationId),
                chatRepository.observeUnread(context, conversationId)
            ) { list, unread ->
                list to unread
            }.collect { (list, unread) ->
                // ✅ DB 一变，立刻刷新 domain + vm state
                chatDomain.applyLocalSnapshot(conversationId, list, unread)
                syncChat()
            }
        }
    }

    fun stopChatDbObserve() {
        android.util.Log.e("ChatTrace", "STOP_DB_OBSERVE cancel=${chatDbObserveJob?.isActive == true}")
        chatDbObserveJob?.cancel()
        chatDbObserveJob = null
    }



    private fun extractClientIdFromConversationId(conversationId: String): String? {
        // 你的 buildConversationId 是：cloud:$storeId:$clientId
        val parts = conversationId.split(":")
        return if (parts.size >= 3) parts[2] else null
    }


    fun stopChatPolling() {
        android.util.Log.e("ChatTrace", "STOP_POLLING cancel=${chatPollingJob?.isActive == true}")
        chatPollingJob?.cancel()
        chatPollingJob = null
    }
    // 逻辑模块统一只从 Session 取当前 storeId
    private val chatStoreId: String
        get() = ShowcaseStoreSession.requireStoreId()

    private fun mapCloudPlanType(raw: String): ShowcaseCloudPlanType {
        return when (raw.trim().lowercase()) {
            "trial" -> ShowcaseCloudPlanType.Trial
            "paid" -> ShowcaseCloudPlanType.Paid
            else -> ShowcaseCloudPlanType.Unknown
        }
    }

    private fun mapCloudServiceStatus(raw: String): ShowcaseCloudServiceStatus {
        return when (raw.trim().lowercase()) {
            "active" -> ShowcaseCloudServiceStatus.Active
            "read_only" -> ShowcaseCloudServiceStatus.ReadOnly
            "deleted" -> ShowcaseCloudServiceStatus.Deleted
            else -> ShowcaseCloudServiceStatus.Unknown
        }
    }

    private suspend fun refreshCloudServiceStatus() {
        val result = showcaseCloudRepository.fetchStoreServiceStatus(chatStoreId)
        if (result == null) {
            uiState = uiState.copy(
                cloudStatus = uiState.cloudStatus.copy(
                    storeId = chatStoreId,
                    lastSyncAtMs = System.currentTimeMillis()
                )
            )
            return
        }

        uiState = uiState.copy(
            cloudStatus = ShowcaseCloudStatus(
                storeId = result.storeId,
                planType = mapCloudPlanType(result.planType),
                serviceStatus = mapCloudServiceStatus(result.serviceStatus),
                serviceEndAt = result.serviceEndAt,
                deleteAt = result.deleteAt,
                canWrite = result.isWriteAllowed,
                lastSyncAtMs = System.currentTimeMillis()
            )
        )
    }

    // 当前发送角色
    private enum class ChatMode {
        Client,
        Merchant
    }

    private var chatMode: ChatMode = ChatMode.Client

    private var clientChatUiStateSnapshot: ShowcaseChatUiState =
        ShowcaseChatUiState(title = "Chat")

    private var merchantChatUiStateSnapshot: ShowcaseChatUiState =
        ShowcaseChatUiState(title = "Chat")

    private var clientActiveConversationId: String? = null
    private var merchantActiveConversationId: String? = null

    private var clientChatLastAppliedLatestId: String? = null
    private var merchantChatLastAppliedLatestId: String? = null

    private fun snapshotCurrentChatContext() {
        when (chatMode) {
            ChatMode.Client -> {
                clientChatUiStateSnapshot = chatDomain.uiState
                clientActiveConversationId = activeConversationId
                clientChatLastAppliedLatestId = chatLastAppliedLatestId
            }
            ChatMode.Merchant -> {
                merchantChatUiStateSnapshot = chatDomain.uiState
                merchantActiveConversationId = activeConversationId
                merchantChatLastAppliedLatestId = chatLastAppliedLatestId
            }
        }
    }

    private fun restoreClientChatContext() {
        chatMode = ChatMode.Client
        chatDomain.restoreSnapshot(
            snapshot = clientChatUiStateSnapshot,
            perspectiveRole = "client"
        )
        activeConversationId = clientActiveConversationId
        chatLastAppliedLatestId = clientChatLastAppliedLatestId
        uiState = uiState.copy(chat = clientChatUiStateSnapshot)
    }

    private fun restoreMerchantChatContext() {
        chatMode = ChatMode.Merchant
        chatDomain.restoreSnapshot(
            snapshot = merchantChatUiStateSnapshot,
            perspectiveRole = "merchant"
        )
        activeConversationId = merchantActiveConversationId
        chatLastAppliedLatestId = merchantChatLastAppliedLatestId
        uiState = uiState.copy(chat = merchantChatUiStateSnapshot)
    }

    private fun resolvedMerchantSenderLabel(): String {
        return uiState.storeProfile
            ?.title
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Merchant"
    }

    private suspend fun resolveChatPushSenderName(
        context: Context,
        roleForSend: String,
        conversationId: String
    ): String {
        return if (roleForSend == "merchant") {
            uiState.storeProfile
                ?.title
                ?.trim()
                .orEmpty()
                .ifBlank { "Merchant" }
        } else {
            runCatching {
                chatRepository.resolveMerchantThreadPushDisplayName(
                    context = context.applicationContext,
                    storeId = chatStoreId,
                    conversationId = conversationId
                )
            }.getOrDefault("New Customer")
        }
    }

    private fun resolvedCurrentConversationDisplayName(): String {
        return when (chatMode) {
            ChatMode.Merchant -> {
                chatDomain.uiState.title
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?: "Customer"
            }
            ChatMode.Client -> {
                "Customer"
            }
        }
    }

    private fun buildChatPushBodyPreview(
        text: String,
        hasDraftImages: Boolean
    ): String {
        val raw = text.trim()
        return when {
            raw.contains("⟪P⟫") && raw.contains("⟪/P⟫") -> "Sent you a product card"
            hasDraftImages -> "Sent you a photo"
            raw.isNotBlank() -> raw
            else -> "Sent you a photo"
        }
    }

    private fun resolvedSenderLabelForSearchMessage(message: ChatMessageEntity): String {
        return when (chatMode) {
            ChatMode.Merchant -> {
                if (message.direction == "out") {
                    resolvedMerchantSenderLabel()
                } else {
                    chatDomain.uiState.title
                        .trim()
                        .takeIf { it.isNotEmpty() }
                        ?: "Customer"
                }
            }
            ChatMode.Client -> {
                if (message.direction == "out") {
                    resolvedMerchantSenderLabel()
                } else {
                    "Me"
                }
            }
        }
    }

    // 列表页返回目标
    private var merchantChatListBackTarget: ShowcaseScreen = ShowcaseScreen.Admin


    private fun syncChat() {
        uiState = uiState.copy(chat = chatDomain.uiState)

        when (chatMode) {
            ChatMode.Client -> {
                clientChatUiStateSnapshot = chatDomain.uiState
                clientActiveConversationId = activeConversationId
                clientChatLastAppliedLatestId = chatLastAppliedLatestId
            }
            ChatMode.Merchant -> {
                merchantChatUiStateSnapshot = chatDomain.uiState
                merchantActiveConversationId = activeConversationId
                merchantChatLastAppliedLatestId = chatLastAppliedLatestId
            }
        }
    }



    private var adminUser: String = ""
    private fun persistMerchantSession(context: Context) {
        try {
            ShowcaseMerchantSessionManager.init(context)
            ShowcaseMerchantSessionManager.persistCurrentSession()
        } catch (_: Exception) {
        }
    }

    private fun clearStoredMerchantSession(context: Context, clearRememberMe: Boolean) {
        try {
            ShowcaseMerchantSessionManager.init(context)
            ShowcaseMerchantSessionManager.clearPersistedSession(clearRememberMe = clearRememberMe)
        } catch (_: Exception) {
        }
    }

    private fun validateRestoredMerchantSession(context: Context) {
        val authUserId = ShowcaseStoreSession.currentMerchantAuthUserId()?.trim().orEmpty()
        if (authUserId.isBlank()) {
            ShowcaseStoreSession.clearMerchantSession()
            clearStoredMerchantSession(context, clearRememberMe = false)
            uiState = uiState.copy(
                isAdminLoggedIn = false,
                loginError = null,
                statusMessage = "Saved login expired. Please sign in again."
            )
            return
        }

        viewModelScope.launch {
            val binding = showcaseCloudRepository.fetchMerchantBindingForStoreAndAuthUser(
                storeId = chatStoreId,
                authUserId = authUserId
            )

            if (binding == null) {
                ShowcaseStoreSession.clearMerchantSession()
                clearStoredMerchantSession(context, clearRememberMe = false)
                adminUser = ""

                uiState = uiState.copy(
                    isAdminLoggedIn = false,
                    screen = ShowcaseScreen.Home,
                    loginError = "This account is not bound to current store.",
                    loginUsernameDraft = "",
                    adminUsernameDraft = "",
                    adminPasswordDraft = "",
                    statusMessage = "Saved login was cleared because this account does not belong to current store."
                )
                return@launch
            }

            val effectiveLoginName = binding.loginName?.trim().orEmpty()
                .ifBlank { ShowcaseStoreSession.currentMerchantLoginName()?.trim().orEmpty() }

            if (effectiveLoginName.isNotBlank()) {
                adminUser = effectiveLoginName
                ShowcaseStoreSession.updateMerchantLoginName(effectiveLoginName)
                persistMerchantSession(context)

                uiState = uiState.copy(
                    isAdminLoggedIn = true,
                    loginError = null,
                    loginUsernameDraft = effectiveLoginName,
                    adminUsernameDraft = effectiveLoginName
                )
            }
        }
    }
    // -------------------- Change Password (独立 Screen) --------------------

    private var changePasswordBackTarget: ShowcaseScreen = ShowcaseScreen.StoreProfileView

    fun openChangePasswordPage() {
        changePasswordBackTarget = uiState.screen
        uiState = uiState.copy(
            screen = ShowcaseScreen.ChangePassword,
            changePasswordCurrentDraft = "",
            changePasswordNewDraft = "",
            changePasswordConfirmDraft = "",
            changePasswordError = null,
            changePasswordSuccess = null
        )
    }

    fun backFromChangePassword() {
        uiState = uiState.copy(
            screen = changePasswordBackTarget,
            changePasswordError = null
        )
    }

    fun onChangePasswordCurrentDraftChange(v: String) {
        uiState = uiState.copy(
            changePasswordCurrentDraft = v,
            changePasswordError = null,
            changePasswordSuccess = null
        )
    }

    fun onChangePasswordNewDraftChange(v: String) {
        uiState = uiState.copy(
            changePasswordNewDraft = v,
            changePasswordError = null,
            changePasswordSuccess = null
        )
    }

    fun onChangePasswordConfirmDraftChange(v: String) {
        uiState = uiState.copy(
            changePasswordConfirmDraft = v,
            changePasswordError = null,
            changePasswordSuccess = null
        )
    }

    fun submitChangePassword(context: Context) {
        val current = uiState.changePasswordCurrentDraft.trim()
        val next = uiState.changePasswordNewDraft.trim()
        val confirm = uiState.changePasswordConfirmDraft.trim()
        val loginName = ShowcaseStoreSession.currentMerchantLoginName()?.trim().orEmpty()

        if (current.isBlank() || next.isBlank() || confirm.isBlank()) {
            uiState = uiState.copy(changePasswordError = "Please fill all fields.")
            return
        }
        if (loginName.isBlank()) {
            uiState = uiState.copy(changePasswordError = "Merchant session is missing.")
            return
        }
        if (next.length < 4) {
            uiState = uiState.copy(changePasswordError = "New password must be at least 4 characters.")
            return
        }
        if (next != confirm) {
            uiState = uiState.copy(changePasswordError = "Passwords do not match.")
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            changePasswordError = null,
            changePasswordSuccess = null
        )

        viewModelScope.launch {
            val reAuth = showcaseCloudRepository.signInMerchant(
                loginName = loginName,
                password = current
            )
            if (reAuth == null) {
                uiState = uiState.copy(
                    isLoading = false,
                    changePasswordError = "Current password is incorrect."
                )
                return@launch
            }

            ShowcaseStoreSession.setMerchantSession(
                accessToken = reAuth.accessToken,
                refreshToken = reAuth.refreshToken,
                authUserId = reAuth.authUserId,
                loginName = reAuth.loginName,
                expiresAt = reAuth.expiresAt
            )

            val ok = showcaseCloudRepository.updateMerchantPassword(next)
            if (!ok) {
                uiState = uiState.copy(
                    isLoading = false,
                    changePasswordError = "Password update failed."
                )
                return@launch
            }

            if (uiState.loginRememberMeDraft) {
                persistMerchantSession(context)
            }

            uiState = uiState.copy(
                isLoading = false,
                changePasswordSuccess = "Password updated.",
                changePasswordError = null
            )

            backFromChangePassword()
        }
    }



// -------------------- Merchant Chat List --------------------

    fun openMerchantChatList(context: Context) {
        if (!uiState.isAdminLoggedIn) {
            uiState = uiState.copy(
                screen = ShowcaseScreen.Login,
                loginError = null
            )
            return
        }

        appContext = context.applicationContext
        ensureMerchantSessionLoadedForCloud(context.applicationContext)
        merchantChatListBackTarget = ShowcaseScreen.Admin

        uiState = uiState.copy(
            screen = ShowcaseScreen.MerchantChatList,
            statusMessage = null
        )

        startMerchantChatListDbObserve()
        refreshMerchantChatListSilently()
        startMerchantChatListPolling()
    }
    fun backFromMerchantChatList() {
        stopMerchantChatListDbObserve()
        stopMerchantChatListPolling()
        uiState = uiState.copy(screen = merchantChatListBackTarget)
    }
    fun merchantChatListDeleteThread(conversationId: String) {
        val ctx = appContext ?: return
        viewModelScope.launch {
            clearMerchantConversationLocalArtifacts(conversationId)
            chatListDomain.deleteThread(ctx, chatStoreId, conversationId)
            refreshMerchantChatListSilently()
        }
    }

    fun merchantChatListTogglePin(conversationId: String, pinned: Boolean) {
        val ctx = appContext ?: return
        viewModelScope.launch {
            // ✅ 1) 写入同一份数据源：chat_thread_meta.pinnedAtMs
            chatListDomain.setPinned(ctx, chatStoreId, conversationId, pinned)

            // ✅ 2) 列表立刻刷新（置顶排序/📌）
            refreshMerchantChatListSilently()

            // ✅ 3) 如果当前正在看的 Chat 就是这条会话，同步 Chat 页 UI 状态
            val currentConv = chatDomain.uiState.conversationId ?: uiState.chat.conversationId
            if (currentConv == conversationId) {
                // 只写回 UIState（不越界，不做业务）
                chatDomain.setPinnedUi(pinned)
                syncChat()
            }
        }
    }


    fun merchantChatListMarkRead(conversationId: String) {
        val ctx = appContext ?: return
        viewModelScope.launch {
            chatListDomain.markThreadRead(ctx, conversationId)
            refreshMerchantChatListSilently()
        }
    }
    fun merchantChatListRenameThread(conversationId: String, newName: String) {
        val ctx = appContext ?: return
        val alias = newName.trim()
        if (alias.isBlank()) return

        viewModelScope.launch {
            chatListDomain.setThreadAlias(ctx, chatStoreId, conversationId, alias)
            refreshMerchantChatListSilently()
        }
    }


    private fun refreshMerchantChatListInternal(showRefreshing: Boolean) {
        val ctx = appContext ?: return
        ensureMerchantSessionLoadedForCloud(ctx)

        // ✅ 已经有刷新在跑时，不要再 cancel；直接跳过这次，避免永远跑不完
        if (merchantChatListRefreshJob?.isActive == true) {
            return
        }

        if (showRefreshing) {
            uiState = uiState.copy(merchantChatListRefreshing = true)
        }

        merchantChatListRefreshJob = viewModelScope.launch {
            try {
                ensureMerchantSessionLoadedForCloud(ctx)

                val threads = chatListDomain.buildMerchantThreads(ctx, storeId = chatStoreId)
                uiState = uiState.copy(
                    merchantChatThreads = threads.toList(),
                    statusMessage = null
                )
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) return@launch

                uiState = uiState.copy(
                    statusMessage = t.message ?: "refreshMerchantChatList failed"
                )
            } finally {
                if (showRefreshing) {
                    uiState = uiState.copy(merchantChatListRefreshing = false)
                }
            }
        }
    }

    /** ✅ 给 UI 下拉用：显示刷新动画 */
    fun refreshMerchantChatListByUser() {
        refreshMerchantChatListInternal(showRefreshing = true)
    }

    /** ✅ 给轮询用：静默刷新，不要让 UI 一直转圈 */
    private fun refreshMerchantChatListSilently() {
        refreshMerchantChatListInternal(showRefreshing = false)
    }


    /**
     * 从商家列表进入 Chat：
     * - Back 先回列表页
     * - 发送角色切换为 merchant
     */
    // 放在现有 openChatFromMerchantList(context, storeId, threadId) 上面或下面都行
    fun openChatFromMerchantList(
        context: Context,
        threadId: String,
        threadTitle: String
    ) {
        openChatFromMerchantList(
            context = context,
            storeId = chatStoreId,
            threadId = threadId,
            threadTitle = threadTitle
        )
    }

    fun openChatFromMerchantList(
        context: Context,
        storeId: String,
        threadId: String,
        threadTitle: String
    ) {
        // ✅ 轮询依赖 appContext，不设会直接 break
        appContext = context.applicationContext
        ensureMerchantSessionLoadedForCloud(context.applicationContext)

        stopMerchantChatListPolling() // ✅ 离开列表页了，停掉轮询

        snapshotCurrentChatContext()
        restoreMerchantChatContext()

        chatBackTarget = ShowcaseScreen.MerchantChatList
        uiState = uiState.copy(
            screen = ShowcaseScreen.Chat,
            chat = uiState.chat.copy(
                conversationId = threadId,
                title = threadTitle.trim().ifBlank { "Chat" },
                useStoreTitle = false,
                isConnecting = true,
                errorMessage = null
            )
        )

        viewModelScope.launch {
            ensureMerchantSessionLoadedForCloud(context.applicationContext)

            chatDomain.openMerchantConversation(
                context = context,
                storeId = storeId,
                conversationId = threadId
            )
            syncChat()

            activeConversationId = chatDomain.uiState.conversationId ?: threadId
            val convId = activeConversationId
            if (!convId.isNullOrBlank()) {
                stopChatDbObserve()
                stopChatPolling()
                chatLastAppliedLatestId = null
                startChatDbObserve(context.applicationContext, convId)
                startChatPolling()
            }
        }
    }





// -------------------- Chat (from Home) --------------------

    fun openChatFromHome(context: Context) {
        appContext = context.applicationContext
        chatBackTarget = ShowcaseScreen.Home

// ✅ Home -> Chat 永远是“客户视角”
        snapshotCurrentChatContext()
        restoreClientChatContext()

        // ✅ 如果当前还在轮询商家会话列表，进入 Chat 前先停掉
        stopMerchantChatListPolling()

        uiState = uiState.copy(
            screen = ShowcaseScreen.Chat,
            chat = uiState.chat.copy(errorMessage = null)
        )

        viewModelScope.launch {
            chatDomain.openLocal(context, storeId = chatStoreId)
            syncChat()

            activeConversationId = chatDomain.uiState.conversationId
            val convId = activeConversationId
            if (!convId.isNullOrBlank()) {
                stopChatDbObserve()
                stopChatPolling()
                chatLastAppliedLatestId = null
                startChatDbObserve(context.applicationContext, convId)
                startChatPolling()
            }
        }

    }
    fun backFromChat() {

        stopChatPolling()

        // ✅ 离开 Chat：立刻清掉“定位/闪烁”信号，避免再次进入 Chat 重复触发
        chatDomain.clearJumpOnExit()
        chatDomain.clearFlash()
        syncChat()

        snapshotCurrentChatContext()

        val target = chatBackTarget

        if (target == ShowcaseScreen.MerchantChatList) {
            restoreMerchantChatContext()
        } else {
            restoreClientChatContext()
        }

// ✅ 如果 Chat 的返回目标是 Detail：恢复“原详情菜品 + 原详情返回目标”
        if (target == ShowcaseScreen.Detail && chatBackDetailDish != null) {
            val dish = chatBackDetailDish
            val backT = chatBackDetailBackTarget

            // 恢复 detailBackTarget（否则会被“Chat 内打开的 Detail”覆盖成 Chat）
            detailBackTarget = backT ?: ShowcaseScreen.Home

            // 恢复 selectedDish（否则 screen=Detail 但 selectedDish=null，会空白/错层）
            uiState = uiState.copy(
                screen = ShowcaseScreen.Detail,
                selectedDish = dish
            )

            // 用完即清，避免脏数据影响下一次
            chatBackDetailDish = null
            chatBackDetailBackTarget = null

            return
        }

        // ✅ 若 Chat 的返回目标是“聊天记录搜索页”，恢复搜索页显示（保留 query/结果）
        // 注意：我们上面只清了 jump/flash，不清 search 结果，因此这里仍然能正常回到结果页
        if (target == ShowcaseScreen.ChatSearchResults) {
            chatDomain.openFind()
            syncChat()
        }

        uiState = uiState.copy(screen = target)
        syncChat()

// ✅ 关键：如果是回到商家聊天列表页，立刻刷新 threads + 恢复本地观察和轮询
        if (target == ShowcaseScreen.MerchantChatList) {
            startMerchantChatListDbObserve()
            refreshMerchantChatListSilently()
            startMerchantChatListPolling()
        }
    }
    fun onChatDraftChange(text: String) {
        chatDomain.onDraftChange(text)
        syncChat()
    }

    /**
     * ✅ 给 UI “复制商品卡片”用：生成可粘贴的 payload
     * UI 不拼协议；协议由 Domain 统一提供
     */
    fun buildChatProductClipboardPayload(p: ShowcaseChatProductShare): String {
        return chatDomain.buildProductSharePayloadForClipboard(p)
    }

    // ✅ 新增：复制“已发送商品卡片”时，把它变成输入框上方的待发送商品条
    fun chatUseProductCardAsPending(p: ShowcaseChatProductShare) {
        chatDomain.setPendingProductShare(p)
        syncChat()
    }

    fun onChatImageLimitReached() {
        uiState = uiState.copy(
            chat = uiState.chat.copy(errorMessage = "Reached max 9 images.")
        )
    }

    fun onEditImageLimitReached() {
        uiState = uiState.copy(
            statusMessage = "Reached max 9 images."
        )
    }

    fun onStoreProfileCoverLimitReached() {
        uiState = uiState.copy(
            statusMessage = "Reached max 9 images."
        )
    }

    fun onChatImagesSelected(context: Context, sourceUris: List<Uri>) {
        viewModelScope.launch {
            val current = chatDomain.uiState.draftImageUris.size
            val remaining = (9 - current).coerceAtLeast(0)
            if (remaining == 0) {
                uiState = uiState.copy(chat = uiState.chat.copy(errorMessage = "Reached max 9 images."))
                return@launch
            }

            val picked = sourceUris.take(remaining)

            val compressed = mutableListOf<Uri>()
            for (u in picked) {
                val c = compressImage(
                    context = context,
                    sourceUri = u,
                    maxLongEdge = CHAT_IMAGE_LONG_EDGE,
                    jpegQuality = CHAT_IMAGE_JPEG_QUALITY
                )
                if (c != null) compressed.add(c)
            }

            if (compressed.isEmpty()) {
                uiState = uiState.copy(chat = uiState.chat.copy(errorMessage = "Image compress failed."))
                return@launch
            }

            chatDomain.onDraftImagesAdd(compressed.map { it.toString() })
            syncChat()
        }
    }

    fun chatRemoveDraftImage(uriString: String) {
        chatDomain.onRemoveDraftImage(uriString)
        syncChat()
    }

    // ✅ 相机：pending Uri（TakePicture 用）
    private var chatPendingCameraUri: Uri? = null
    private var chatPendingCameraFile: File? = null

    private fun setChatCameraError(message: String) {
        uiState = uiState.copy(
            chat = uiState.chat.copy(errorMessage = message)
        )
    }

    private fun clearChatCameraError() {
        if (uiState.chat.errorMessage.isNullOrBlank()) return
        uiState = uiState.copy(
            chat = uiState.chat.copy(errorMessage = null)
        )
    }

    private fun deletePendingChatCameraFile() {
        runCatching {
            chatPendingCameraFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
        }
        chatPendingCameraFile = null
    }

    private fun clearPendingChatCameraState(deleteTempFile: Boolean = false) {
        if (deleteTempFile) {
            deletePendingChatCameraFile()
        } else {
            chatPendingCameraFile = null
        }
        chatPendingCameraUri = null
        chatDomain.setPendingCameraUri(null)
    }

    fun onChatCameraPermissionDenied() {
        setChatCameraError("Camera permission denied.")
    }

    fun onChatFullCameraUnavailable() {
        setChatCameraError("Full-resolution camera is unavailable on this device.")
        clearPendingChatCameraState(deleteTempFile = true)
        syncChat()
    }

    private fun isReadableNonEmptyContentUri(context: Context, uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.read() != -1
            } ?: false
        }.getOrDefault(false)
    }

    private fun deleteLocalFileUri(uri: Uri?) {
        runCatching {
            if (uri?.scheme == "file") {
                uri.path?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun clearMerchantConversationLocalArtifacts(conversationId: String) {
        if (merchantChatUiStateSnapshot.conversationId == conversationId) {
            merchantChatUiStateSnapshot.draftImageUris
                .map { Uri.parse(it) }
                .forEach { deleteLocalFileUri(it) }

            merchantChatUiStateSnapshot.pendingCameraUri
                ?.let { Uri.parse(it) }
                ?.let { deleteLocalFileUri(it) }

            merchantChatUiStateSnapshot = ShowcaseChatUiState(title = "Chat")
        }

        if (merchantActiveConversationId == conversationId) {
            merchantActiveConversationId = null
            merchantChatLastAppliedLatestId = null
        }

        if (chatDomain.uiState.conversationId == conversationId) {
            chatDomain.uiState.draftImageUris
                .map { Uri.parse(it) }
                .forEach { deleteLocalFileUri(it) }

            chatDomain.uiState.pendingCameraUri
                ?.let { Uri.parse(it) }
                ?.let { deleteLocalFileUri(it) }

            chatDomain.restoreSnapshot(
                snapshot = ShowcaseChatUiState(title = "Chat"),
                perspectiveRole = "merchant"
            )

            activeConversationId = null
            chatLastAppliedLatestId = null

            if (chatMode == ChatMode.Merchant) {
                uiState = uiState.copy(chat = ShowcaseChatUiState(title = "Chat"))
            }
        }

        if (chatPendingCameraUri != null) {
            deleteLocalFileUri(chatPendingCameraUri)
            chatPendingCameraUri = null
        }

        if (chatPendingCameraFile != null) {
            deletePendingChatCameraFile()
        }
    }

    private suspend fun saveLocalImageUriToGallery(
        context: Context,
        localUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val fileName = "NDJC_${System.currentTimeMillis()}.jpg"
            val mime = "image/jpeg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/NDJC"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val galleryUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: return@withContext false

            val copied = resolver.openInputStream(localUri)?.use { input ->
                resolver.openOutputStream(galleryUri)?.use { output ->
                    input.copyTo(output)
                    true
                }
            } == true

            if (!copied) {
                resolver.delete(galleryUri, null, null)
                return@withContext false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(galleryUri, done, null, null)
            }

            true
        } catch (e: Throwable) {
            Log.e("ShowcaseViewModel", "saveLocalImageUriToGallery failed", e)
            false
        }
    }

    fun prepareChatCameraCapture(context: Context): Uri? {
        clearChatCameraError()

        val uri = createTempCameraUri(context)
        if (uri == null) {
            clearPendingChatCameraState(deleteTempFile = true)
            setChatCameraError("Unable to create camera file.")
            return null
        }

        chatPendingCameraUri = uri
        chatDomain.setPendingCameraUri(uri.toString())
        syncChat()
        return uri
    }

    fun onChatCameraPreviewResult(
        context: Context,
        bitmap: android.graphics.Bitmap?
    ) {
        viewModelScope.launch {
            Log.e(
                "NDJC_CAMERA",
                "preview path hit width=${bitmap?.width} height=${bitmap?.height}"
            )
            setChatCameraError("Preview camera result is disabled. Please use full-resolution capture.")
            clearPendingChatCameraState(deleteTempFile = true)
            syncChat()
        }
    }

    fun onChatCameraResult(context: Context, success: Boolean) {
        viewModelScope.launch {
            val pending = chatPendingCameraUri
                ?: chatDomain.uiState.pendingCameraUri?.let { Uri.parse(it) }

            Log.e("NDJC_CAMERA", "full picture path hit success=$success pending=$pending")

            if (pending == null) {
                setChatCameraError("Photo capture failed.")
                clearPendingChatCameraState(deleteTempFile = true)
                syncChat()
                return@launch
            }

            val hasReadablePhoto = isReadableNonEmptyContentUri(context, pending)

            if (!success && !hasReadablePhoto) {
                setChatCameraError("Photo capture cancelled.")
                clearPendingChatCameraState(deleteTempFile = true)
                syncChat()
                return@launch
            }

            if (!hasReadablePhoto) {
                setChatCameraError("Captured photo is empty or unreadable.")
                clearPendingChatCameraState(deleteTempFile = true)
                syncChat()
                return@launch
            }

            val compressedUri = compressImage(
                context = context,
                sourceUri = pending,
                maxLongEdge = CHAT_IMAGE_LONG_EDGE,
                jpegQuality = CHAT_IMAGE_JPEG_QUALITY
            )
            if (compressedUri == null) {
                setChatCameraError("Image compress failed.")
                clearPendingChatCameraState(deleteTempFile = true)
                syncChat()
                return@launch
            }

            chatDomain.onDraftImagesAdd(listOf(compressedUri.toString()))

            val savedToGallery = saveLocalImageUriToGallery(context, compressedUri)
            if (!savedToGallery) {
                Log.e("NDJC_CAMERA", "final image save to gallery failed")
            }

            clearChatCameraError()
            clearPendingChatCameraState(deleteTempFile = true)
            syncChat()
        }
    }

// --- Chat（本地版：不连云端，卸载即清空）---



    // ✅ 防重复发送：同一时刻只允许一个 send job
    private var chatSendJob: kotlinx.coroutines.Job? = null

    fun sendChat() {
        val text = uiState.chat.draftText
        Log.e("ChatRoomTest", "VM onSend reached textLen=${text.length}")

        val ctx = appContext ?: run {
            uiState = uiState.copy(chat = uiState.chat.copy(errorMessage = "Chat not initialized."))
            return
        }

        if (chatSendJob?.isActive == true) return

        chatSendJob = viewModelScope.launch {
            try {
                val roleForSend = currentChatRole()

                if (chatDomain.uiState.conversationId.isNullOrBlank()) {
                    if (roleForSend == "merchant") {
                        uiState = uiState.copy(chat = uiState.chat.copy(errorMessage = "No merchant conversation selected."))
                        return@launch
                    } else {
                        chatDomain.openLocal(ctx, storeId = chatStoreId)
                    }
                }

                val cloudSendOk = chatDomain.sendAsClientLocal(
                    ctx,
                    storeId = chatStoreId,
                    role = roleForSend
                )

                val conversationId = chatDomain.uiState.conversationId
                    ?: uiState.chat.conversationId
                    ?: return@launch

                if (roleForSend != "merchant") {
                    Log.d(
                        "NDJC_PUSH",
                        "force ensurePushRegistration for chat_client after conversation resolved, conversationId=$conversationId"
                    )
                    ensurePushRegistration(ctx, "chat_client")
                }

                if (!cloudSendOk) {
                    Log.d(
                        "NDJC_PUSH",
                        "skip dispatchChatPush because cloud send failed, conversationId=$conversationId, storeId=$chatStoreId"
                    )

                    if (roleForSend.equals("merchant", ignoreCase = true) &&
                        chatCloudRepository.isMerchantAuthExpired(
                            code = chatCloudRepository.lastChatImageUploadCode,
                            body = chatCloudRepository.lastChatImageUploadBody
                        )
                    ) {
                        handleMerchantSessionExpired(ctx)
                        return@launch
                    }

                    if (handleMerchantSessionExpiredIfRefreshUnrecoverable(
                            context = ctx,
                            role = roleForSend
                        )
                    ) {
                        return@launch
                    }

                    return@launch
                }

                val targetAudience =
                    if (roleForSend == "merchant") {
                        "chat_client"
                    } else {
                        "chat_merchant"
                    }

                val senderName = resolveChatPushSenderName(
                    context = ctx,
                    roleForSend = roleForSend,
                    conversationId = conversationId
                )

                val senderClientId =
                    if (roleForSend == "merchant") {
                        null
                    } else {
                        runCatching {
                            chatDomain.ensureClientId(ctx.applicationContext)
                        }.getOrNull()
                    }

                val pushBodyPreview = buildChatPushBodyPreview(
                    text = text,
                    hasDraftImages = chatDomain.uiState.draftImageUris.isNotEmpty()
                )

                val pushOk = showcaseCloudRepository.dispatchChatPush(
                    storeId = chatStoreId,
                    conversationId = conversationId,
                    targetAudience = targetAudience,
                    senderRole = roleForSend,
                    senderName = senderName,
                    bodyPreview = pushBodyPreview,
                    senderClientId = senderClientId
                )
                Log.d(
                    "NDJC_PUSH",
                    "dispatchChatPush result=$pushOk, conversationId=$conversationId, storeId=$chatStoreId, targetAudience=$targetAudience, senderClientId=$senderClientId"
                )
            } finally {
                syncChat()
            }
        }
    }

    /**
     * 兼容旧入口：从 Home 进入时默认 user
     */
    fun sendChatAsClient() {
        // ✅ 保留兼容，但不再修改全局发送角色；改为切换模式再发送
        chatMode = ChatMode.Client
        sendChat()
    }
    fun sendPendingProductShare() {
        val ctx = appContext ?: return
        if (chatSendJob?.isActive == true) return

        chatSendJob = viewModelScope.launch {
            try {
                if (chatDomain.uiState.conversationId.isNullOrBlank()) {
                    chatDomain.openLocal(ctx, storeId = chatStoreId)
                }

                val roleForSend = currentChatRole()

                val cloudSendOk = chatDomain.sendPendingProductShareAsClientLocal(
                    ctx,
                    storeId = chatStoreId,
                    role = roleForSend
                )

                val conversationId = chatDomain.uiState.conversationId
                    ?: uiState.chat.conversationId
                    ?: return@launch

                if (roleForSend != "merchant") {
                    Log.d(
                        "NDJC_PUSH",
                        "force ensurePushRegistration for chat_client after product share resolved, conversationId=$conversationId"
                    )
                    ensurePushRegistration(ctx, "chat_client")
                }

                if (!cloudSendOk) {
                    Log.d(
                        "NDJC_PUSH",
                        "skip dispatchChatPush because product share cloud send failed, conversationId=$conversationId, storeId=$chatStoreId"
                    )

                    if (handleMerchantSessionExpiredIfRefreshUnrecoverable(
                            context = ctx,
                            role = roleForSend
                        )
                    ) {
                        return@launch
                    }

                    return@launch
                }

                val targetAudience =
                    if (roleForSend == "merchant") {
                        "chat_client"
                    } else {
                        "chat_merchant"
                    }

                val senderName = resolveChatPushSenderName(
                    context = ctx,
                    roleForSend = roleForSend,
                    conversationId = conversationId
                )

                val senderClientId =
                    if (roleForSend == "merchant") {
                        null
                    } else {
                        runCatching {
                            chatDomain.ensureClientId(ctx.applicationContext)
                        }.getOrNull()
                    }

                val pushOk = showcaseCloudRepository.dispatchChatPush(
                    storeId = chatStoreId,
                    conversationId = conversationId,
                    targetAudience = targetAudience,
                    senderRole = roleForSend,
                    senderName = senderName,
                    bodyPreview = "Sent you a product card",
                    senderClientId = senderClientId
                )
                Log.d(
                    "NDJC_PUSH",
                    "dispatchChatPush(product) result=$pushOk, conversationId=$conversationId, storeId=$chatStoreId, targetAudience=$targetAudience, senderClientId=$senderClientId"
                )
            } finally {
                syncChat()
            }
        }
    }
    fun clearPendingProductShare() {
        chatDomain.clearPendingProductShare()
        syncChat()
    }



    fun retryChat(messageId: String) {
        val ctx = appContext ?: return
        viewModelScope.launch {
            val roleForRetry = currentChatRole()
            chatDomain.retryLocal(
                ctx,
                storeId = chatStoreId,
                messageId = messageId,
                role = roleForRetry
            )
            syncChat()
        }
    }

    fun refreshChatLatest() {
        val ctx = appContext ?: return
        viewModelScope.launch {
            when (chatMode) {
                ChatMode.Merchant -> {
                    val convId = chatDomain.uiState.conversationId
                        ?: uiState.chat.conversationId
                        ?: activeConversationId

                    if (!convId.isNullOrBlank()) {
                        chatDomain.openMerchantConversation(
                            context = ctx,
                            storeId = chatStoreId,
                            conversationId = convId
                        )
                    }
                }

                ChatMode.Client -> {
                    chatDomain.openLocal(ctx, storeId = chatStoreId)
                }
            }
            syncChat()
        }
    }
    fun saveChatPreviewImage(context: Context, url: String) {
        val u = url.trim()
        if (u.isBlank()) {
            uiState = uiState.copy(statusMessage = "No image url.")
            return
        }

        uiState = uiState.copy(statusMessage = "Saving image...")

        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                saveImageUrlToGallery(context, u)
            }
            uiState = uiState.copy(
                statusMessage = if (ok) "Image saved to gallery." else "Save failed."
            )
        }
    }

    private suspend fun saveImageUrlToGallery(context: Context, url: String): Boolean {
        return try {
            val loader = ImageLoader(context)
            val req = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()

            val result = loader.execute(req)
            val drawable = (result as? SuccessResult)?.drawable ?: return false
            val bitmap = drawable.toBitmap()

            val resolver = context.contentResolver
            val fileName = "NDJC_${System.currentTimeMillis()}.jpg"
            val mime = "image/jpeg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/NDJC"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)) {
                    return false
                }
            } ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, done, null, null)
            }

            true
        } catch (_: Throwable) {
            false
        }
    }


    fun chatDeleteMessage(messageId: String) {
        val ctx = appContext ?: return
        viewModelScope.launch {
            chatDomain.deleteOneLocal(ctx, storeId = chatStoreId, messageId = messageId)
            syncChat()
        }
    }

    fun chatQuoteMessage(messageId: String) {
        chatDomain.quoteMessage(messageId)
        syncChat()
    }

    fun chatCancelQuote() {
        chatDomain.cancelQuote()
        syncChat()
    }

    fun chatEnterSelection(messageId: String) {
        chatDomain.enterSelection(messageId)
        syncChat()
    }

    fun chatToggleSelection(messageId: String) {
        chatDomain.toggleSelection(messageId)
        syncChat()
    }

    fun chatExitSelection() {
        chatDomain.exitSelection()
        syncChat()
    }

    fun chatDeleteSelected() {
        val ctx = appContext ?: return
        viewModelScope.launch {
            chatDomain.deleteSelectedLocal(ctx, storeId = chatStoreId)
            syncChat()
        }
    }
    fun chatOpenSearchResults() {
        // ✅ 记录来源页：从哪里进搜索页，关闭就回哪里
        chatSearchBackTarget = uiState.screen

        // ✅ 关键：记录“进入搜索页之前 Chat 的 backTarget”
        // ChatList -> Chat 时通常是 MerchantChatList，这里必须保存下来
        chatBackTargetBeforeSearch = chatBackTarget

        // ✅ 入口决定搜索范围（你下面 chatSetFindQuery 依赖这个）
        chatSearchScope = if (uiState.screen == ShowcaseScreen.Chat) {
            ChatSearchScope.InConversation
        } else {
            ChatSearchScope.InExistingThreads
        }

        uiState = uiState.copy(screen = ShowcaseScreen.ChatSearchResults)

        // 查找页每次进入都重置
        chatDomain.closeFind()
        chatDomain.openFind()

        syncChat()
    }
    fun chatOpenMediaGallery() {
        // 只允许从 Chat 打开；若不是 Chat，就不做动作（避免越界入口）
        if (uiState.screen != ShowcaseScreen.Chat) return
        uiState = uiState.copy(screen = ShowcaseScreen.ChatMedia)
    }

    fun chatCloseMediaGallery() {
        // 从图片墙返回 Chat
        uiState = uiState.copy(screen = ShowcaseScreen.Chat)
        syncChat()
    }


    fun chatCloseSearchResults() {
        chatDomain.onFindQueryChange("")
        chatDomain.closeFind()

        // ✅ 关键：退出搜索页后恢复 Chat 的 backTarget
        // 否则 Chat 仍会按 backTarget 回到 ChatSearchResults，造成 Chat ↔ 搜索页循环
        chatBackTarget = chatBackTargetBeforeSearch

        // ✅ 回到来源页
        uiState = uiState.copy(screen = chatSearchBackTarget)
        syncChat()
    }
    fun chatSetFindQuery(q: String) {
        val ctx = appContext ?: return

        // ✅ 只更新输入框内容
        chatDomain.setSearchResultsQuery(q)

        val query = q.trim()
        if (query.isBlank()) {
            chatDomain.setGlobalSearchResults(emptyList())
            syncChat()
            return
        }

        viewModelScope.launch {
            val results = when (chatSearchScope) {

                ChatSearchScope.InConversation -> {
                    // 仅搜索当前聊天窗口
                    val convId = chatDomain.uiState.conversationId ?: activeConversationId
                    if (convId.isNullOrBlank()) {
                        emptyList()
                    } else {
                        fun extractMainBodyForSearch(raw: String): String {
                            if (raw.isBlank()) return ""
                            if (raw.contains("⟪P⟫")) return "" // 商品卡整条消息：不参与

                            var s = raw.trim()

                            val firstMarker = s.indexOf("⟪")
                            if (firstMarker >= 0 && firstMarker <= 3) {
                                s = s.substring(firstMarker)
                            }

                            // 1) 剥离图片块
                            while (true) {
                                val si = s.indexOf("⟪I⟫")
                                if (si < 0) break
                                val ei = s.indexOf("⟪/I⟫", startIndex = si + "⟪I⟫".length)
                                if (ei < 0) break
                                s = (s.substring(0, si) + s.substring(ei + "⟪/I⟫".length)).trim()
                            }

                            // 2) 剥离引用块（不匹配引用内容）
                            while (true) {
                                val sq = s.indexOf("⟪Q⟫")
                                if (sq < 0) break
                                val eq = s.indexOf("⟪/Q⟫", startIndex = sq + "⟪Q⟫".length)
                                if (eq < 0) break
                                s = (s.substring(0, sq) + s.substring(eq + "⟪/Q⟫".length)).trim()
                            }

                            // 3) 剥离商品块
                            while (true) {
                                val sp = s.indexOf("⟪P⟫")
                                if (sp < 0) break
                                val ep = s.indexOf("⟪/P⟫", startIndex = sp + "⟪P⟫".length)
                                if (ep < 0) break
                                s = (s.substring(0, sp) + s.substring(ep + "⟪/P⟫".length)).trim()
                            }

                            return s
                                .replace('\n', ' ')
                                .replace(Regex("\\s+"), " ")
                                .trim()
                        }

                        val hits = chatRepository.listLocal(ctx, convId)
                            .asSequence()
                            .map { m -> m to extractMainBodyForSearch(m.text) }
                            .filter { (_, body) -> body.contains(query, ignoreCase = true) }
                            .sortedByDescending { (m, _) -> m.timeMs }
                            .take(80)
                            .toList()

                        hits.mapNotNull { (m, body) ->
                            val snip = body.take(80)
                            if (snip.isBlank()) {
                                null
                            } else {
                                ShowcaseChatGlobalSearchResultUi(
                                    conversationId = m.conversationId,
                                    messageId = m.id,
                                    displayName = resolvedCurrentConversationDisplayName(),
                                    senderLabel = resolvedSenderLabelForSearchMessage(m),
                                    snippet = snip,
                                    timeMs = m.timeMs,
                                    timeText = formatYmdAmpmHm(m.timeMs),
                                    matchedInName = false
                                )
                            }
                        }
                    }
                }
                ChatSearchScope.InExistingThreads -> {
                    // 仅搜索“现存聊天窗口”（ChatList 里当前 threads）
                    val allowedIds = uiState.merchantChatThreads
                        .map { it.threadId }   // ✅ threadId 就是 conversationId（稳定唯一）
                        .filter { it.isNotBlank() }
                        .toSet()


                    chatListDomain.searchMessagesAndNames(
                        context = ctx,
                        storeId = chatStoreId,
                        keyword = query,
                        merchantSenderLabel = resolvedMerchantSenderLabel(),
                        allowedConversationIds = allowedIds
                    )
                }
            }

            chatDomain.setGlobalSearchResults(results)
            syncChat()
        }
    }

    fun chatJumpToFoundMessage(messageId: String) {
        // ✅ 若当前处于“查找聊天记录独立页”，先关闭它
        chatDomain.closeFind()
        syncChat()

        chatDomain.jumpToMessage(messageId)

        uiState = uiState.copy(screen = ShowcaseScreen.Chat)
        syncChat()
    }

    // ✅ 新增：引用块点击跳转到被引用原消息
    fun chatJumpToMessageFromQuote(messageId: String) {
        chatDomain.jumpToMessage(messageId)
        syncChat()
    }

    private fun reopenCurrentChatWithoutRoleFlip() {
        uiState = uiState.copy(screen = ShowcaseScreen.Chat)
        syncChat()
    }

    fun chatOpenThreadFromSearch(conversationId: String, messageId: String?) {
        val ctx = appContext ?: return

        /**
         * 规则：
         * 1. 当前聊天内搜索（InConversation）：
         *    - 不切线程
         *    - 只跳到命中消息
         *
         * 2. 现存线程搜索（InExistingThreads）：
         *    - 商家视角：打开对应 merchant conversation
         *    - 游客视角：理论上不会走到这里；若误入，也只能回本地会话，禁止切成商家视角
         */

        if (chatSearchScope == ChatSearchScope.InConversation) {
            chatDomain.hideFindKeepState()
            syncChat()

            reopenCurrentChatWithoutRoleFlip()

            if (!messageId.isNullOrBlank()) {
                chatDomain.jumpToMessage(messageId)
                syncChat()
            }
            return
        }

        if (chatSearchScope == ChatSearchScope.InExistingThreads) {
            chatSearchBackTarget = ShowcaseScreen.MerchantChatList
            chatBackTargetBeforeSearch = ShowcaseScreen.MerchantChatList
        }

        chatBackTarget = ShowcaseScreen.ChatSearchResults

        chatDomain.hideFindKeepState()
        syncChat()

        uiState = uiState.copy(screen = ShowcaseScreen.Chat)
        syncChat()

        viewModelScope.launch {
            when (chatMode) {
                ChatMode.Merchant -> {
                    chatDomain.openMerchantConversation(
                        context = ctx,
                        storeId = chatStoreId,
                        conversationId = conversationId
                    )
                }

                ChatMode.Client -> {
                    // 游客视角禁止因为搜索结果点击而切成商家线程
                    chatDomain.openLocal(ctx, storeId = chatStoreId)
                }
            }

            syncChat()

            activeConversationId = chatDomain.uiState.conversationId ?: conversationId
            val convId = chatDomain.uiState.conversationId ?: activeConversationId
            if (!convId.isNullOrBlank()) {
                startChatDbObserve(ctx.applicationContext, convId)
                startChatPolling()
            }

            if (!messageId.isNullOrBlank()) {
                chatDomain.jumpToMessage(messageId)
            }

            syncChat()
        }
    }

    fun chatTogglePinned() {
        val ctx = appContext ?: return
        val conversationId = chatDomain.uiState.conversationId
            ?: uiState.chat.conversationId
            ?: return

        val next = !chatDomain.uiState.isPinned

        viewModelScope.launch {
            // ✅ 同一份数据源：写入 chat_thread_meta.pinnedAtMs
            chatListDomain.setPinned(ctx, chatStoreId, conversationId, next)

            // ✅ Chat 页 UI 立即跟随（不越界：只是写回 uiState）
            chatDomain.setPinnedUi(next)

            // ✅ 回到 ChatList 也立刻正确（置顶排序/图标）
            refreshMerchantChatListSilently()

            syncChat()
        }
    }


    fun chatOpenFind() {
        chatDomain.openFind()
        syncChat()
    }

    fun chatCloseFind() {
        chatDomain.closeFind()
        syncChat()
    }

    fun chatFindQueryChange(q: String) {
        chatDomain.onFindQueryChange(q)
        syncChat()
    }

    fun setFindQuery(q: String) {
        chatDomain.onFindQueryChange(q)
        syncChat()
    }

    fun jumpToMessage(messageId: String) {
        chatDomain.jumpToMessage(messageId)
        syncChat()
    }


    fun chatFindNext() {
        chatDomain.findNext()
        syncChat()
    }

    fun chatFindPrev() {
        chatDomain.findPrev()
        syncChat()
    }



    private var appContext: Context? = null
    fun ensureLoaded(context: Context) {
        if (hasLoadedOnce) return
        hasLoadedOnce = true

        // ✅ 只缓存 applicationContext，避免泄漏 Activity
        appContext = context.applicationContext

        viewModelScope.launch { loadFromSources(context) }
    }

    fun refresh(context: Context) {
        viewModelScope.launch {
            loadFromSources(context)
            refreshCloudServiceStatus()
        }
    }

    /**
     * 清理“弱网/失败提示”
     */
    fun clearSyncError() {
        uiState = uiState.copy(
            syncErrorMessage = null,
            lastRetryOp = null
        )
    }

    /**
     * 弱网/失败统一重试入口（逻辑模块做业务，不让 UI 包越界）
     */
    fun retryLast(context: Context) {
        when (uiState.lastRetryOp) {
            ShowcaseRetryOp.LoadFromCloud -> refresh(context)
            ShowcaseRetryOp.RetryPendingSync -> retryPendingSync(context)
            ShowcaseRetryOp.RefreshStoreProfile -> refreshStoreProfileFromCloud(context)
            null -> refresh(context) // 兜底
        }
    }

    /**
     * 展示型必备：搜索/筛选/排序
     * - UI 包可直接绑定这些 setter；不要求 UIState 里有“派生列表”，通过 visibleDishes() 暴露即可。
     */
    fun onSearchQueryChange(value: String) {
        uiState = uiState.copy(searchQuery = value)
    }

    data class EditDerived(
        val cleanedCategories: List<String>,
        val cleanedImages: List<String>,
        val maxImages: Int,
        val canAddImageSlot: Boolean,

        val isDiscountInvalidNumber: Boolean,
        val isDiscountGEPrice: Boolean,
        val discountErrorText: String?,
        val canSave: Boolean
    )

    /**
     * Edit：所有“清洗 + 校验 + 门控”一次性在 VM 产出
     * wiring/UI 不再做 trim/filter/distinct/take/parseFloat/when/canSave 组合
     */
    fun getEditDeleteAction(): (() -> Unit)? {
        val d = editDraft ?: return null
        if (d.isNew) return null
        val idx = d.index ?: return null
        val id = uiState.dishes.getOrNull(idx)?.id
        if (id.isNullOrBlank()) return null

        return {
            requestDeleteDish(id)
            backToAdminFromEdit()
        }
    }


    fun deriveEditState(
        editDraft: EditDraftState?,
        categories: List<String>,
        isLoading: Boolean
    ): EditDerived {
        val cleanedCategories = categories
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        val maxImages = 9

        if (editDraft == null) {
            return EditDerived(
                cleanedCategories = cleanedCategories,
                cleanedImages = emptyList(),
                maxImages = maxImages,
                canAddImageSlot = true,
                isDiscountInvalidNumber = false,
                isDiscountGEPrice = false,
                discountErrorText = null,
                canSave = false
            )
        }

        val cleanedImages = editDraft.imageUris
            .asSequence()
            .map { it.toString() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(maxImages)
            .toList()

        // 折扣/价格校验
        val priceF = editDraft.price.trim().toFloatOrNull()

        val discountText = editDraft.discountPrice.trim()
        val discountF = discountText.takeIf { it.isNotEmpty() }?.toFloatOrNull()

        val isDiscountInvalidNumber = discountText.isNotEmpty() && discountF == null
        val isDiscountGEPrice = (discountF != null && priceF != null && discountF >= priceF)

        val discountErrorText: String? = when {
            isDiscountGEPrice -> "Discount price must be lower than price"
            isDiscountInvalidNumber -> "Invalid discount price."
            else -> null
        }

        val canSave = !isLoading && !isDiscountGEPrice && !isDiscountInvalidNumber

        return EditDerived(
            cleanedCategories = cleanedCategories,
            cleanedImages = cleanedImages,
            maxImages = maxImages,
            canAddImageSlot = cleanedImages.size < maxImages,
            isDiscountInvalidNumber = isDiscountInvalidNumber,
            isDiscountGEPrice = isDiscountGEPrice,
            discountErrorText = discountErrorText,
            canSave = canSave
        )
    }

    data class DetailImagesDerived(
        val preview: String?,
        val imageUrls: List<String>,
        val safeIndex: Int
    )

    /**
     * Detail：图片列表最终结果（去空 + fallback + 安全 index）
     * wiring/UI 不再做 filter/ifEmpty/coerceIn
     */
    fun deriveDetailImages(selectedDish: DemoDish, detailImageIndex: Int): DetailImagesDerived {
        val preview = selectedDish.imageUri?.toString()

        val resolvedImages = selectedDish.imageUrls
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
            .ifEmpty { preview?.let { listOf(it) } ?: emptyList() }

        val safeIndex =
            if (resolvedImages.isEmpty()) 0
            else detailImageIndex.coerceIn(0, resolvedImages.lastIndex)

        return DetailImagesDerived(
            preview = preview,
            imageUrls = resolvedImages,
            safeIndex = safeIndex
        )
    }


    fun onSortModeChange(mode: ShowcaseHomeSortMode) {
        uiState = uiState.copy(sortMode = mode)
    }

    fun onAdminItemsSortModeChange(mode: ShowcaseHomeSortMode) {
        // 同一个按钮重复点：切换升/降
        if (uiState.adminItemsSortMode == mode) {
            uiState = uiState.copy(adminItemsSortAscending = !uiState.adminItemsSortAscending)
        } else {
            // 切换 sort key：默认回到升序
            uiState = uiState.copy(
                adminItemsSortMode = mode,
                adminItemsSortAscending = true
            )
        }
    }

    fun onAdminItemsSearchQueryChange(value: String) {
        uiState = uiState.copy(adminItemsSearchQuery = value)
    }

    fun clearAdminItemsSearchQuery() {
        uiState = uiState.copy(adminItemsSearchQuery = "")
    }

    fun onAdminItemsFilterRecommendedChange(checked: Boolean) {
        uiState = uiState.copy(adminItemsFilterRecommended = checked)
    }

    fun onAdminItemsFilterHiddenOnlyChange(checked: Boolean) {
        uiState = uiState.copy(adminItemsFilterHiddenOnly = checked)
    }
    fun onAdminItemsFilterDiscountOnlyChange(checked: Boolean) {
        uiState = uiState.copy(adminItemsFilterDiscountOnly = checked)
    }

    fun onAdminItemsPriceMinDraftChange(v: String) {
        uiState = uiState.copy(adminItemsPriceMinDraft = v)
    }

    fun onAdminItemsPriceMaxDraftChange(v: String) {
        uiState = uiState.copy(adminItemsPriceMaxDraft = v)
    }

    fun onAdminItemsApplyPriceRange() {
        val min = uiState.adminItemsPriceMinDraft.trim().toIntOrNull()
        val max = uiState.adminItemsPriceMaxDraft.trim().toIntOrNull()
        val (mn, mx) = if (min != null && max != null && min > max) (max to min) else (min to max)

        uiState = uiState.copy(
            adminItemsAppliedMinPrice = mn,
            adminItemsAppliedMaxPrice = mx
        )
    }

    fun onAdminItemsClearPriceRange() {
        uiState = uiState.copy(
            adminItemsPriceMinDraft = "",
            adminItemsPriceMaxDraft = "",
            adminItemsAppliedMinPrice = null,
            adminItemsAppliedMaxPrice = null
        )
    }

    fun onFilterRecommendedOnlyChange(checked: Boolean) {
        uiState = uiState.copy(filterRecommendedOnly = checked)
    }

    fun onFilterOnSaleOnlyChange(checked: Boolean) {
        uiState = uiState.copy(filterOnSaleOnly = checked)
    }
    // ✅ Home：排序/筛选菜单显隐（原来 UI remember）
    fun onHomeShowSortMenuChange(show: Boolean) {
        uiState = uiState.copy(homeShowSortMenu = show)
    }

    fun onHomeShowFilterMenuChange(show: Boolean) {
        uiState = uiState.copy(homeShowFilterMenu = show)
    }
    fun onHomeShowPriceMenuChange(show: Boolean) {
        uiState = uiState.copy(homeShowPriceMenu = show)
    }

    fun onHomePriceMinDraftChange(v: String) {
        uiState = uiState.copy(homePriceMinDraft = v)
    }

    fun onHomePriceMaxDraftChange(v: String) {
        uiState = uiState.copy(homePriceMaxDraft = v)
    }

    fun onHomeApplyPriceRange() {
        val min = uiState.homePriceMinDraft.trim().toIntOrNull()
        val max = uiState.homePriceMaxDraft.trim().toIntOrNull()

        // 如果两者都存在且 min > max：自动交换（比直接报错体验更好）
        val (mn, mx) = if (min != null && max != null && min > max) (max to min) else (min to max)

        uiState = uiState.copy(
            homeAppliedMinPrice = mn,
            homeAppliedMaxPrice = mx,
            homeShowPriceMenu = false
        )
    }

    fun onHomeClearPriceRange() {
        uiState = uiState.copy(
            homePriceMinDraft = "",
            homePriceMaxDraft = "",
            homeAppliedMinPrice = null,
            homeAppliedMaxPrice = null,
            homeShowPriceMenu = false
        )
    }

    fun onSelectedTagsChange(tags: List<String>) {
        uiState = uiState.copy(selectedTags = tags)
    }


    /**
     * UI 渲染时建议读取这个派生列表（展示型标配：搜索/筛选/排序/分类）
     * - 不改 uiState.dishes，保证数据源单一且可维护。
     */
    fun visibleDishes(includeHidden: Boolean = false): List<DemoDish> {
        val base = uiState.dishes
        val selectedCategory = uiState.selectedCategory?.trim()?.takeIf { it.isNotBlank() }
        val query = uiState.searchQuery.trim()
        val tags = uiState.selectedTags.map { it.trim() }.filter { it.isNotBlank() }.toSet()

        var list = base.asSequence()

        // ✅ 首页默认隐藏 isHidden=true 的商品；管理页可传 includeHidden=true 看到全部
        if (!includeHidden) {
            list = list.filter { !it.isHidden }
        }

        // 分类筛选（保留你现有 onCategorySelected 语义）
        if (selectedCategory != null) {
            list = list.filter { it.category == selectedCategory }
        }

        // 推荐/折扣/库存筛选
        if (uiState.filterRecommendedOnly) {
            list = list.filter { it.isRecommended }
        }
        if (uiState.filterOnSaleOnly) {
            list = list.filter { it.discountPrice != null && it.discountPrice > 0f }
        }

        // tags 筛选（只要包含任一 tag 即可；需要时可改为“包含全部”）
        if (tags.isNotEmpty()) {
            list = list.filter { dish ->
                dish.tags.any { it in tags }
            }
        }

        // 关键字搜索（nameZh/nameEn/description/category）
        if (query.isNotBlank()) {
            val q = query.lowercase()
            list = list.filter { dish ->
                (dish.nameZh.lowercase().contains(q)) ||
                        (dish.nameEn.lowercase().contains(q)) ||
                        (dish.descriptionEn.lowercase().contains(q)) ||
                        (dish.category.lowercase().contains(q))
            }
        }
        // ✅ 价格区间筛选（用折后价优先，否则原价）
        val minP = uiState.homeAppliedMinPrice
        val maxP = uiState.homeAppliedMaxPrice
        if (minP != null || maxP != null) {
            list = list.filter { dish ->
                val p = (dish.discountPrice ?: dish.originalPrice)
                (minP == null || p >= minP.toFloat()) &&
                        (maxP == null || p <= maxP.toFloat())
            }
        }

        // 排序
        val sorted = when (uiState.sortMode) {
            // ✅ Home 默认：新上传靠上
            // 约定：uiState.dishes 的顺序就是“新在前”，所以默认不额外排序，保持原顺序
            ShowcaseHomeSortMode.Default ->
                list.toList()

            ShowcaseHomeSortMode.PriceAsc ->
                list.sortedBy { it.discountPrice ?: it.originalPrice }.toList()

            ShowcaseHomeSortMode.PriceDesc ->
                list.sortedByDescending { it.discountPrice ?: it.originalPrice }.toList()
        }

        return sorted

    }
    fun visibleAdminItems(): List<DemoDish> {
        val base = uiState.dishes
        val selectedCategory = uiState.selectedCategory?.trim()?.takeIf { it.isNotBlank() }
        val query = uiState.adminItemsSearchQuery.trim()

        var seq = base.asSequence()

        // category
        if (selectedCategory != null) {
            seq = seq.filter { it.category == selectedCategory }
        }

        // search（按 nameZh/nameEn；你也可以加 description/category）
        if (query.isNotBlank()) {
            val q = query.lowercase()
            seq = seq.filter { d ->
                d.nameZh.lowercase().contains(q) ||
                        d.nameEn.lowercase().contains(q)
            }
        }

        // filter: recommended
        if (uiState.adminItemsFilterRecommended) {
            seq = seq.filter { it.isRecommended }
        }

        // filter: hidden only（注意是“只看隐藏”，不是“隐藏掉隐藏”）
        if (uiState.adminItemsFilterHiddenOnly) {
            seq = seq.filter { it.isHidden }
        }
// filter: discount only（只看有折扣的：discountPrice != null）
        if (uiState.adminItemsFilterDiscountOnly) {
            seq = seq.filter { it.discountPrice != null }
        }

// ✅ price range（对齐 Home/Favorites）：按折后价（若无折扣则原价）过滤
        val mn = uiState.adminItemsAppliedMinPrice
        val mx = uiState.adminItemsAppliedMaxPrice
        if (mn != null) {
            seq = seq.filter { (it.discountPrice ?: it.originalPrice) >= mn }
        }
        if (mx != null) {
            seq = seq.filter { (it.discountPrice ?: it.originalPrice) <= mx }
        }

// ✅ 仅保留三种排序：Default / PriceAsc / PriceDesc
        val sorted = when (uiState.adminItemsSortMode) {
            ShowcaseHomeSortMode.Default ->
                seq.toList()

            ShowcaseHomeSortMode.PriceAsc ->
                seq.sortedBy { it.discountPrice ?: it.originalPrice }.toList()

            ShowcaseHomeSortMode.PriceDesc ->
                seq.sortedByDescending { it.discountPrice ?: it.originalPrice }.toList()
        }

        return sorted

    }

    private fun loadAdminCredentials(context: Context) {
        try {
            ShowcaseMerchantSessionManager.init(context)

            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)
            val canResume = ShowcaseMerchantSessionManager.restoreSessionFromDisk()
            val loginName = ShowcaseStoreSession.currentMerchantLoginName()?.trim().orEmpty()

            adminUser = if (canResume) loginName else ""

            uiState = uiState.copy(
                loginRememberMeDraft = rememberMe,
                isAdminLoggedIn = canResume,
                loginUsernameDraft = if (canResume) loginName else uiState.loginUsernameDraft,
                adminUsernameDraft = if (loginName.isNotBlank()) loginName else uiState.adminUsernameDraft
            )

            if (canResume) {
                validateRestoredMerchantSession(context)
            }
        } catch (_: Exception) {
        }
    }

    fun setAdminCredentials(context: Context, username: String, password: String) {
        val nextLoginName = username.trim()
        val nextPassword = password.trim()
        val currentLoginName = ShowcaseStoreSession.currentMerchantLoginName()?.trim().orEmpty()

        if (!uiState.isAdminLoggedIn) {
            uiState = uiState.copy(statusMessage = "Please log in first.")
            return
        }
        if (nextLoginName.isBlank()) {
            uiState = uiState.copy(statusMessage = "Account cannot be blank.")
            return
        }

        uiState = uiState.copy(isLoading = true, statusMessage = null)

        viewModelScope.launch {
            var changed = false

            if (!nextLoginName.equals(currentLoginName, ignoreCase = true)) {
                val accountOk = showcaseCloudRepository.updateMerchantLoginName(
                    storeId = chatStoreId,
                    newLoginName = nextLoginName
                )
                if (!accountOk) {
                    if (handleMerchantAuthExpiredIfNeeded(
                            context = context,
                            code = showcaseCloudRepository.lastMerchantAuthCode,
                            body = showcaseCloudRepository.lastMerchantAuthBody
                        )
                    ) {
                        return@launch
                    }

                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = "Account update failed."
                    )
                    return@launch
                }
                ShowcaseStoreSession.updateMerchantLoginName(nextLoginName)
                adminUser = nextLoginName
                changed = true
            }

            if (nextPassword.isNotBlank()) {
                if (nextPassword.length < 4) {
                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = "Password must be at least 4 characters."
                    )
                    return@launch
                }
                val passwordOk = showcaseCloudRepository.updateMerchantPassword(nextPassword)
                if (!passwordOk) {
                    if (handleMerchantAuthExpiredIfNeeded(
                            context = context,
                            code = showcaseCloudRepository.lastMerchantAuthCode,
                            body = showcaseCloudRepository.lastMerchantAuthBody
                        )
                    ) {
                        return@launch
                    }

                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = "Password update failed."
                    )
                    return@launch
                }
                changed = true
            }

            if (uiState.loginRememberMeDraft) {
                persistMerchantSession(context)
            }

            uiState = uiState.copy(
                isLoading = false,
                adminUsernameDraft = nextLoginName,
                adminPasswordDraft = "",
                statusMessage = if (changed) "Admin credentials updated." else "Nothing changed."
            )
        }
    }
    // ✅ 退出登录（仅本机）：清掉本机登录态，回到 Login
    fun adminLogout(context: Context) {
        ShowcaseStoreSession.clearMerchantSession()
        clearStoredMerchantSession(context, clearRememberMe = true)

        uiState = uiState.copy(
            isAdminLoggedIn = false,
            screen = ShowcaseScreen.Login,
            loginUsernameDraft = "",
            loginPasswordDraft = "",
            loginRememberMeDraft = false,
            loginError = null,
            statusMessage = null
        )
    }

    private fun handleMerchantSessionExpired(context: Context) {
        ShowcaseMerchantSessionManager.init(context)
        ShowcaseStoreSession.clearMerchantSession()
        clearStoredMerchantSession(context, clearRememberMe = false)

        adminUser = ""

        uiState = uiState.copy(
            isLoading = false,
            isAdminLoggedIn = false,
            screen = ShowcaseScreen.Login,
            loginPasswordDraft = "",
            adminPasswordDraft = "",
            loginError = null,
            statusMessage = "Session expired, please sign in again."
        )
    }

    private fun handleMerchantAuthExpiredIfNeeded(
        context: Context,
        code: Int?,
        body: String?
    ): Boolean {
        val expired = showcaseCloudRepository.isMerchantAuthExpired(code, body)
        if (expired) {
            handleMerchantSessionExpired(context)
            return true
        }
        return false
    }

    private fun handleMerchantSessionExpiredIfRefreshUnrecoverable(
        context: Context,
        role: String
    ): Boolean {
        if (!role.equals("merchant", ignoreCase = true)) return false

        ShowcaseMerchantSessionManager.init(context)
        val token = ShowcaseMerchantSessionManager.ensureValidMerchantAccessToken()
            ?.trim()
            .orEmpty()

        if (token.isNotBlank()) return false

        handleMerchantSessionExpired(context)
        return true
    }

    private fun handleMerchantDeleteExpiredIfNeeded(context: Context): Boolean {
        return handleMerchantAuthExpiredIfNeeded(
            context = context,
            code = showcaseCloudRepository.lastDeleteCode,
            body = showcaseCloudRepository.lastDeleteBody
        )
    }

    private fun handleMerchantDishImageUploadExpiredIfNeeded(context: Context): Boolean {
        return handleMerchantAuthExpiredIfNeeded(
            context = context,
            code = showcaseCloudRepository.lastDishImageUploadCode,
            body = showcaseCloudRepository.lastDishImageUploadBody
        )
    }

    private fun handleMerchantStoreImageUploadExpiredIfNeeded(context: Context): Boolean {
        return handleMerchantAuthExpiredIfNeeded(
            context = context,
            code = showcaseCloudRepository.lastStoreImageUploadCode,
            body = showcaseCloudRepository.lastStoreImageUploadBody
        )
    }

    // ✅ Admin credentials：草稿由 VM 托管（UI 不再 remember）
    fun onAdminUsernameDraftChange(value: String) {
        uiState = uiState.copy(adminUsernameDraft = value)
    }

    fun onAdminPasswordDraftChange(value: String) {
        uiState = uiState.copy(adminPasswordDraft = value)
    }

    fun saveAdminCredentialsFromDraft(context: Context) {
        // trim/校验都在 setAdminCredentials 内完成
        setAdminCredentials(
            context = context,
            username = uiState.adminUsernameDraft,
            password = uiState.adminPasswordDraft
        )
    }

    /**
     * 优先云端，其次本地缓存，最后兜底（现在兜底列表为空，不再强塞 demo dish）。
     *
     * 展示型必备改动：
     * - 云端与本地做 merge，避免云端刷新覆盖本地离线新增/编辑。
     * - 同步总览状态（pending/failed）预留给 UI。
     * - StoreProfile 本地/云端读取入口补齐（不破坏现有流程）。
     */
    private suspend fun loadFromSources(context: Context) {
        loadAdminCredentials(context)

        uiState = uiState.copy(
            isLoading = true,
            statusMessage = "Loading from cloud...",
            syncOverviewState = SyncOverviewState.Syncing,
            syncErrorMessage = null
        )

        val fromCloud = tryLoadFromCloud()
        val cloudManualCategories = uiState.manualCategories
        val stored = loadDishesFromStorage(context)
        val storedManualCategories = loadManualCategoriesFromStorage(context)

        val effectiveList: List<DemoDish>
        val effectiveManualCategories: List<String>
        val status: String

        effectiveList = when {
            fromCloud.isNotEmpty() -> {
                status = "Loaded from cloud."
                fromCloud
            }
            stored.isNotEmpty() -> {
                status = "Cloud unavailable, loaded from local cache."
                stored
            }
            else -> {
                status = "No data."
                emptyList()
            }
        }

        effectiveManualCategories = when {
            cloudManualCategories.isNotEmpty() -> cloudManualCategories
            storedManualCategories.isNotEmpty() -> storedManualCategories
            else -> emptyList()
        }

        val pendingCount = effectiveList.count { it.syncState == SyncState.Pending }
        val failedCount = effectiveList.count { it.syncState == SyncState.Failed }
        val overview = when {
            failedCount > 0 -> SyncOverviewState.Failed
            pendingCount > 0 -> SyncOverviewState.HasPending
            else -> SyncOverviewState.Idle
        }

        val localProfile = loadStoreProfileFromStorage(context)
        val cloudProfile = try {
            cloudRepository.fetchStoreProfile(chatStoreId)?.let { cloud ->
                StoreProfile(
                    title = cloud.title,
                    subtitle = cloud.subtitle,
                    description = cloud.description,
                    address = cloud.address,
                    hours = cloud.hours,
                    mapUrl = cloud.mapUrl,
                    services = try {
                        val arr = org.json.JSONArray(cloud.servicesJson.ifBlank { "[]" })
                        buildList {
                            for (i in 0 until arr.length()) {
                                arr.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
                            }
                        }
                    } catch (_: Exception) {
                        emptyList()
                    },
                    extraContacts = decodeExtraContactsJson(cloud.extraContactsJson),
                    coverUrl = cloud.coverUrl,
                    logoUrl = cloud.logoUrl,
                    businessStatus = cloud.businessStatus
                )
            }
        } catch (_: Exception) {
            null
        }

        val effectiveProfile = cloudProfile ?: localProfile

        val prevSelectedId = uiState.selectedDish?.id
        val reboundSelected: DemoDish? =
            if (uiState.screen == ShowcaseScreen.Detail && !prevSelectedId.isNullOrBlank()) {
                effectiveList.firstOrNull { it.id == prevSelectedId } ?: uiState.selectedDish
            } else {
                uiState.selectedDish
            }

        val validDishIds = effectiveList.map { it.id }.toSet()
        val storedFavoriteIds = loadFavoriteIdsFromStorage(context)
        val effectiveFavoriteIds = storedFavoriteIds.filter { validDishIds.contains(it) }.toSet()
        val storedFavoriteAddedAt = loadFavoriteAddedAtFromStorage(context)
        val effectiveFavoriteAddedAt = storedFavoriteAddedAt
            .filterKeys { effectiveFavoriteIds.contains(it) }
            .toMutableMap()

        uiState = uiState.copy(
            dishes = effectiveList,
            manualCategories = effectiveManualCategories,
            selectedDish = reboundSelected,
            isLoading = false,
            statusMessage = status,
            lastSyncAt = System.currentTimeMillis(),
            pendingSyncCount = pendingCount,
            syncOverviewState = overview,
            storeProfile = effectiveProfile,
            syncErrorMessage = if (fromCloud.isNotEmpty()) null else uiState.syncErrorMessage,
            lastRetryOp = uiState.lastRetryOp
        )

        favoriteIds = effectiveFavoriteIds
        favoriteAddedAt.clear()
        favoriteAddedAt.putAll(effectiveFavoriteAddedAt)
        saveFavoriteIdsToStorage(context, effectiveFavoriteIds)
        saveFavoriteAddedAtToStorage(context, effectiveFavoriteAddedAt)
        refreshFavoritesList()

        if (fromCloud.isNotEmpty()) {
            saveDishesToStorage(context, effectiveList)
            saveManualCategoriesToStorage(context, effectiveManualCategories)
        }
        if (cloudProfile != null) {
            saveStoreProfileToStorage(context, cloudProfile)
        }
    }
    private fun ensureMerchantSessionLoadedForCloud(context: Context) {
        if (ShowcaseStoreSession.isMerchantLoggedIn()) {
            return
        }
        loadAdminCredentials(context)
    }
    /**
     * 展示型离线必备：云端/本地 merge
     *
     * 规则（最小可用，后续可升级成更严格的冲突策略）：
     * - 以 id 为主键
     * - 本地 dirty 或 syncState!=Synced 的条目优先保留本地版本（避免丢失离线更改）
     * - 其他情况使用 updatedAt 更大的版本；updatedAt 缺失则优先云端（防止把云端删除/更新回滚）
     */
    private fun mergeRemoteAndLocal(remote: List<DemoDish>, local: List<DemoDish>): List<DemoDish> {
        if (remote.isEmpty()) return local
        if (local.isEmpty()) return remote

        val remoteMap = remote.filter { it.id.isNotBlank() }.associateBy { it.id }.toMutableMap()
        val merged = mutableListOf<DemoDish>()

        // 先遍历本地：本地有 dirty/pending/failed 的优先
        for (l in local) {
            val id = l.id
            if (id.isBlank()) continue

            val r = remoteMap.remove(id)
            if (r == null) {
                // 云端没有：本地若是待同步/失败/dirty，保留；否则不强行保留（避免把云端已删除的又加回来）
                if (l.dirty || l.syncState != SyncState.Synced) {
                    merged.add(l)
                }
                continue
            }

            val localPriority = l.dirty || l.syncState != SyncState.Synced
            if (localPriority) {
                merged.add(l)
            } else {
                // 谁 updatedAt 更新用谁；0 表示未知
                val lu = l.updatedAt
                val ru = r.updatedAt
                val pick = if (lu > 0L && ru > 0L) {
                    if (lu >= ru) l else r
                } else {
                    // 没有时间戳：优先云端，避免旧本地覆盖
                    r
                }
                merged.add(pick)
            }
        }

        // 云端剩余：直接追加
        merged.addAll(remoteMap.values)

        // 维持一个稳定顺序：优先按 updatedAt desc，否则按 name
        return merged.sortedWith(
            compareByDescending<DemoDish> { it.updatedAt }
                .thenBy { (it.nameZh.ifBlank { it.nameEn }).lowercase() }
        )
    }

    /**
     * 从 Supabase 拉 categories + dishes：
     * 1) 菜品 → DemoDish
     * 2) 只存在于 categories 表、没有菜品引用的分类 → 写入 manualCategories
     */
    private suspend fun tryLoadFromCloud(): List<DemoDish> {
        return try {
            val categories = cloudRepository.fetchCategories(chatStoreId)
            val dishes = cloudRepository.fetchDishes(chatStoreId)

            val categoryMap = categories.associate { it.id to it.name }

            // 1) 菜品列表
            val dishList = dishes.map { cloud ->
                val categoryName = cloud.categoryId?.let { categoryMap[it] }.orEmpty()
                DemoDish(
                    clickCount = cloud.clickCount,
                    id = cloud.id ?: "",
                    nameZh = cloud.nameZh,
                    nameEn = cloud.nameEn,
                    descriptionEn = cloud.descriptionEn ?: "",
                    category = categoryName,
                    originalPrice = cloud.price.toFloat(),
                    discountPrice = cloud.discountPrice?.toFloat(),
                    isRecommended = cloud.recommended,
                    isSoldOut = cloud.soldOut,
                    isHidden = cloud.hidden,
                    imageResId = null,
                    imageUrls = cloud.imageUrls.ifEmpty { cloud.imageUrl?.let { listOf(it) } ?: emptyList() },
                    imageUri = cloud.imageUrls.firstOrNull()?.let { Uri.parse(it) }
                        ?: cloud.imageUrl?.let { Uri.parse(it) },
                    tags = cloud.tags,
                    externalLink = cloud.externalLink,
                    updatedAt = cloud.updatedAt ?: 0L,
                    syncState = SyncState.Synced,
                    dirty = false
                )
            }

            // 2) categories 表全量分类名（首页/编辑页/管理页统一用这一份）
            val allCategoryNames = categories
                .map { it.name }
                .filter { it.isNotBlank() }
                .distinct()

            uiState = uiState.copy(
                manualCategories = allCategoryNames
            )

            dishList
        } catch (e: Exception) {
            uiState = uiState.copy(
                statusMessage = "Failed to load from cloud, using local data if available.",
                syncErrorMessage = "Failed to load from cloud.",
                lastRetryOp = ShowcaseRetryOp.LoadFromCloud
            )
            emptyList()
        }

    }

    fun onCategorySelected(category: String?) {
        uiState = uiState.copy(selectedCategory = category)
    }
    fun clearHomeSortAndFilters() {
        uiState = uiState.copy(
            searchQuery = "",
            sortMode = ShowcaseHomeSortMode.Default,
            filterRecommendedOnly = false,
            filterOnSaleOnly = false
        )
    }

    fun onAdminFabClicked() {
        uiState =
            if (uiState.isAdminLoggedIn) {
                uiState.copy(screen = ShowcaseScreen.Admin)
            } else {
                uiState.copy(
                    screen = ShowcaseScreen.Login,
                    loginError = null
                )
            }
    }
    fun openAdminItemsScreen(context: Context) {
        // 确保数据在（不阻塞 UI）
        if (!hasLoadedOnce) ensureLoaded(context)
        uiState = uiState.copy(screen = ShowcaseScreen.AdminItems)
    }

    fun openAdminCategoriesScreen(context: Context) {
        if (!hasLoadedOnce) ensureLoaded(context)
        uiState = uiState.copy(screen = ShowcaseScreen.AdminCategories)
    }


    /**
     * StoreProfile：进入“店铺名片页”
     * - UI 包入口（Home 的 profile click）应调用此函数
     * - 若数据未加载，尽量先触发一次 ensureLoaded / refresh
     */
// ✅ 记录 StoreProfile 返回目标（默认回 Home）
    private var storeProfileBackTarget: ShowcaseScreen = ShowcaseScreen.Home

    /**
     * 打开 StoreProfile（编辑态）
     */
    fun openStoreProfile(context: Context) {
        if (!hasLoadedOnce) ensureLoaded(context)
        storeProfileBackTarget = uiState.screen
        val base = uiState.storeProfile ?: StoreProfile()

        uiState = uiState.copy(
            screen = ShowcaseScreen.StoreProfile,
            storeProfileDraft = base,
            storeProfileSaveError = null,
            storeProfileSaveSuccess = false
        )
    }




    /**
     * 打开 StoreProfile（展示态 / 只读）
     */
    fun openStoreProfileView(context: Context) {
        if (!hasLoadedOnce) ensureLoaded(context)

        // ✅ 记住从哪里来：Back 返回上一页
        storeProfileBackTarget = uiState.screen

        // 展示态不应该带 draft
        uiState = uiState.copy(
            screen = ShowcaseScreen.StoreProfileView,
            storeProfileDraft = null,
            storeProfileSaveError = null,
            storeProfileSaveSuccess = false
        )
        // ✅ 进入 StoreProfileView 自动从云端刷新一次（最小接入）
        // 保护：正在保存/仍处于编辑草稿时，不自动刷新，避免覆盖用户编辑体验
        // ✅ 预览模式：禁止云端刷新覆盖本地假数据
        if (!STORE_PROFILE_PREVIEW_LOCAL_ONLY && !uiState.isSavingStoreProfile && uiState.storeProfileDraft == null) {
            refreshStoreProfileFromCloud(context)
        }
    }

    /**
     * StoreProfile（展示/编辑）统一返回上一页
     */
    fun backFromStoreProfile() {
        clearStoreProfileDraftLocalImages()
        uiState = uiState.copy(
            screen = storeProfileBackTarget,
            storeProfileDraft = null,
            storeProfileSaveError = null,
            storeProfileSaveSuccess = false
        )
    }

    private fun normalizeStoreProfileForCompare(profile: StoreProfile): StoreProfile {
        return profile.copy(
            title = profile.title.trim(),
            subtitle = profile.subtitle.trim(),
            description = profile.description.trim(),
            services = profile.services
                .map { it.trim() }
                .filter { it.isNotBlank() },
            address = profile.address.trim(),
            hours = profile.hours.trim(),
            mapUrl = profile.mapUrl.trim(),
            extraContacts = profile.extraContacts
                .map {
                    ExtraContact(
                        name = it.name.trim(),
                        value = it.value.trim()
                    )
                }
                .filter { it.name.isNotBlank() || it.value.isNotBlank() },
            coverUrl = parseCoverList(profile.coverUrl).joinToString("\n"),
            logoUrl = profile.logoUrl.trim(),
            businessStatus = profile.businessStatus.trim()
        )
    }

    fun hasUnsavedStoreProfileDraft(): Boolean {
        val draft = uiState.storeProfileDraft ?: return false
        val base = uiState.storeProfile ?: StoreProfile()
        return normalizeStoreProfileForCompare(draft) != normalizeStoreProfileForCompare(base)
    }

    fun discardStoreProfileDraftAndGoHome() {
        clearStoreProfileDraftLocalImages()
        uiState = uiState.copy(
            storeProfileDraft = null,
            storeProfileSaveError = null,
            storeProfileSaveSuccess = false,
            isSavingStoreProfile = false
        )
        backToHome()
    }

    // -------------------- Chat actions (VM 对外) --------------------
    fun openChatFromStoreProfile(
        context: Context,
        pendingProduct: ShowcaseChatProductShare? = null
    ) {
        // ✅ 轮询依赖 appContext，不设会直接 break
        appContext = context.applicationContext

        // ✅ 普通用户端（Home/StoreProfile 进入 Chat 永远是客户视角）
        snapshotCurrentChatContext()
        restoreClientChatContext()

        // ✅ 避免上一轮停留在商家列表轮询导致状态污染
        stopMerchantChatListPolling()

    // 记录返回目标：从哪个 screen 进入 Chat，就回到哪个
    chatBackTarget = uiState.screen

    // ✅ 如果是从 Detail 进入 Chat：缓存“原详情菜品 + 原详情的 backTarget”
    if (chatBackTarget == ShowcaseScreen.Detail && uiState.selectedDish != null) {
        chatBackDetailDish = uiState.selectedDish
        chatBackDetailBackTarget = detailBackTarget
    } else {
        chatBackDetailDish = null
        chatBackDetailBackTarget = null
    }

    // 先切屏（UI 先出来）
    uiState = uiState.copy(
        screen = ShowcaseScreen.Chat,
        chat = uiState.chat.copy(errorMessage = null)
    )

    viewModelScope.launch {
        // ✅ 关键：openLocal 可能会重置 chatDomain 的临时态
        chatDomain.openLocal(context, storeId = chatStoreId)

        // ✅ 关键：pending 必须在 openLocal 后再挂，避免被清掉
        if (pendingProduct != null) {
            chatDomain.setPendingProductShare(pendingProduct)
        }

        // ✅ 把 chatDomain.uiState（含 pending）同步给 UI
        syncChat()

        activeConversationId = chatDomain.uiState.conversationId
        val convId = activeConversationId
        if (!convId.isNullOrBlank()) {
            stopChatDbObserve()
            stopChatPolling()
            chatLastAppliedLatestId = null
            startChatDbObserve(context.applicationContext, convId)
            startChatPolling()
        }
    }
}

    /**
     * 商品详情页 -> 打开 Chat，并在输入框上方挂“待发送商品卡片”
     * 逻辑层：负责选品、挂 pending、切屏进 Chat
     */
    fun openChatFromBottomBar(context: Context) {
        val pending: ShowcaseChatProductShare? =
            if (uiState.screen == ShowcaseScreen.Detail) buildPendingFromSelectedDish()
            else null

        openChatFromStoreProfile(context, pendingProduct = pending)
    }

    /**
     * 从当前 selectedDish 生成 pending 商品卡片（仅逻辑层）
     */
    private fun buildPendingFromSelectedDish(): ShowcaseChatProductShare? {
        val dish = uiState.selectedDish ?: return null
        return ShowcaseChatProductShare(
            dishId = dish.id.toString(),
            title = dish.nameZh.ifBlank { dish.nameEn },
            price = ((dish.discountPrice ?: dish.originalPrice)).toString(),
            imageUrl = dish.imageUrls.firstOrNull()
        )
    }
    /**
     * StoreProfile：开始编辑
     * - 从当前 storeProfile 拷贝到 storeProfileDraft
     */
    fun startEditStoreProfile() {
        val base = uiState.storeProfile ?: StoreProfile()
        uiState = uiState.copy(
            storeProfileDraft = base,
            storeProfileSaveError = null,
            storeProfileSaveSuccess = false
        )
    }

    /**
     * StoreProfile：取消编辑（丢弃草稿）
     */
    fun cancelEditStoreProfile() {
        clearStoreProfileDraftLocalImages()
        uiState = uiState.copy(
            storeProfileDraft = null,
            storeProfileSaveError = null,
            storeProfileSaveSuccess = false,
            isSavingStoreProfile = false
        )
    }

// ---- Draft 字段变更（UI 包输入框直接绑定）----

    fun onStoreProfileDraftTitleChange(value: String) {
        updateStoreProfileDraft { it.copy(title = value) }
    }
    // ✅ 新增：清空 Logo（逻辑模块职责）
    fun onStoreProfileLogoRemove() {
        onStoreProfileDraftLogoUrlChange("")
    }


    fun onStoreProfileDraftSubtitleChange(value: String) {
        updateStoreProfileDraft { it.copy(subtitle = value) }
    }

    fun onStoreProfileDraftDescriptionChange(value: String) {
        updateStoreProfileDraft { it.copy(description = value) }
    }

    fun onStoreProfileDraftAddressChange(value: String) {
        updateStoreProfileDraft { it.copy(address = value) }
    }

    fun onStoreProfileDraftHoursChange(value: String) {
        updateStoreProfileDraft { it.copy(hours = value) }
    }

    fun onStoreProfileDraftMapUrlChange(value: String) {
        updateStoreProfileDraft { it.copy(mapUrl = value) }
    }

    // ✅ 新增：自定义联系方式（Name + Value）——逻辑层维护 draft.extraContacts
    fun onStoreProfileExtraContactNameChange(index: Int, value: String) {
        updateStoreProfileDraft { draft ->
            val list = draft.extraContacts.toMutableList()
            if (index !in list.indices) return@updateStoreProfileDraft draft
            list[index] = list[index].copy(name = value)
            draft.copy(extraContacts = list)
        }
        uiState = uiState.copy(storeProfileSaveError = null)
    }

    fun onStoreProfileExtraContactValueChange(index: Int, value: String) {
        updateStoreProfileDraft { draft ->
            val list = draft.extraContacts.toMutableList()
            if (index !in list.indices) return@updateStoreProfileDraft draft
            list[index] = list[index].copy(value = value)
            draft.copy(extraContacts = list)
        }
        uiState = uiState.copy(storeProfileSaveError = null)
    }

    fun onStoreProfileExtraContactAdd(name: String, value: String) {
        val n = name.trim()
        val v = value.trim()
        if (n.isBlank() || v.isBlank()) {
            uiState = uiState.copy(storeProfileSaveError = "联系方式 Name 和 Value 必须都填写才能添加。")
            return
        }
        updateStoreProfileDraft { draft ->
            draft.copy(extraContacts = draft.extraContacts + ExtraContact(name = n, value = v))
        }
        uiState = uiState.copy(storeProfileSaveError = null)
    }

    fun onStoreProfileExtraContactRemove(index: Int) {
        updateStoreProfileDraft { draft ->
            val list = draft.extraContacts.toMutableList()
            if (index !in list.indices) return@updateStoreProfileDraft draft
            list.removeAt(index)
            draft.copy(extraContacts = list)
        }
        uiState = uiState.copy(storeProfileSaveError = null)
    }
// =======================
// 业务范围（Services / Business Scope）
// =======================

    fun onStoreProfileServiceChange(index: Int, value: String) {
        updateStoreProfileDraft { draft ->
            val list = draft.services.toMutableList()
            if (index !in list.indices) return@updateStoreProfileDraft draft
            list[index] = value
            draft.copy(services = list)
        }
        uiState = uiState.copy(storeProfileSaveError = null)
    }

    fun onStoreProfileServiceAdd(value: String) {
        val v = value.trim()
        if (v.isBlank()) return
        updateStoreProfileDraft { draft ->
            draft.copy(services = draft.services + v)
        }
        uiState = uiState.copy(storeProfileSaveError = null)
    }

    fun onStoreProfileServiceRemove(index: Int) {
        updateStoreProfileDraft { draft ->
            val list = draft.services.toMutableList()
            if (index !in list.indices) return@updateStoreProfileDraft draft
            list.removeAt(index)
            draft.copy(services = list)
        }
        uiState = uiState.copy(storeProfileSaveError = null)
    }

    // ✅ 新增：Logo / Cover draft
    fun onStoreProfileDraftLogoUrlChange(value: String) {
        updateStoreProfileDraft { it.copy(logoUrl = value) }
    }

    fun onStoreProfileDraftCoverUrlChange(value: String) {
        updateStoreProfileDraft { it.copy(coverUrl = value) }
    }

// --- 下面是给“上传按钮”用的：选中图片 -> 更新草稿（最多 9 张） ---
private fun parseCoverList(raw: String?): MutableList<String> {
    return raw
        .orEmpty()
        // ✅ 同时兼容：真实换行 "\n" 以及字面量 "\\n"
        .split("\n", "\\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toMutableList()
}


    fun onStoreProfileLogoPicked(uriString: String) {
        // Logo 只保留 1 张
        onStoreProfileDraftLogoUrlChange(uriString)
    }

    fun onStoreProfileCoversPicked(uriStrings: List<String>) {
        val draft = uiState.storeProfileDraft ?: return
        val list = parseCoverList(draft.coverUrl)

        // 追加 + 去重 + 限制 9
        uriStrings.forEach { u ->
            val uu = u.trim()
            if (uu.isNotBlank() && !list.contains(uu) && list.size < 9) {
                list.add(uu)
            }
        }

        onStoreProfileDraftCoverUrlChange(list.joinToString("\n"))
    }

    fun onStoreProfileCoverRemove(uriString: String) {
        val draft = uiState.storeProfileDraft ?: return
        val list = parseCoverList(draft.coverUrl)
        list.removeAll { it == uriString }
        onStoreProfileDraftCoverUrlChange(list.joinToString("\n"))
    }
    fun onStoreProfileCoverMove(from: Int, to: Int) {
        val draft = uiState.storeProfileDraft ?: return
        val list = parseCoverList(draft.coverUrl)

        if (from !in list.indices || to !in list.indices) return
        if (from == to) return

        val item = list.removeAt(from)
        list.add(to, item)

        onStoreProfileDraftCoverUrlChange(list.joinToString("\n"))
        uiState = uiState.copy(storeProfileSaveError = null)
    }


    /**
     * 打开电话拨号（逻辑模块负责：Intent 跳转）
     * UI 包只负责点击触发 actions.onOpenPhone(phone)
     */
    fun openPhone(context: Context, phone: String) {
        val p = phone.trim()
        if (p.isBlank()) return

        // 只拨号，不直接呼叫（不需要 CALL_PHONE 权限）
        val uri = Uri.parse("tel:$p")
        runCatching {
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            uiState = uiState.copy(statusMessage = "Cannot open phone.")
        }
    }

    fun openWebsite(context: Context, url: String) {
        val u = url.trim()
        if (u.isBlank()) return

        val normalized = if (u.startsWith("http://") || u.startsWith("https://")) u else "https://$u"
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            uiState = uiState.copy(statusMessage = "Cannot open website.")
        }
    }

    fun openMap(context: Context, url: String) {
        val passedUrl = url.trim()

        // 回退地址：优先当前草稿（编辑态），否则用已保存资料（展示态）
// 回退地址：优先当前草稿（编辑态），否则用已保存资料（展示态）
        val fallbackAddress = uiState.storeProfileDraft?.address
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: uiState.storeProfile?.address
                ?.trim()
                .orEmpty()


        // 1) 优先打开 mapUrl（如果 UI 传空，就当作没有 mapUrl）
        if (passedUrl.isNotBlank()) {
            val ok = runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(passedUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }.isSuccess

            if (ok) return
            // 失败则继续回退 address → geo
        }

        // 2) 回退：address → geo URI
        if (fallbackAddress.isBlank()) {
            uiState = uiState.copy(statusMessage = "No address to open.")
            return
        }

        val geo = "geo:0,0?q=${Uri.encode(fallbackAddress)}"
        val okGeo = runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geo)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.isSuccess

        if (!okGeo) {
            uiState = uiState.copy(statusMessage = "Cannot open map.")
        }
    }
    fun copyToClipboard(context: Context, label: String, text: String) {
        val t = text.trim()
        if (t.isBlank()) return

        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label.ifBlank { "text" }, t))
    }





    fun onStoreProfileDraftBusinessStatusChange(value: String) {
        updateStoreProfileDraft { it.copy(businessStatus = value) }
    }

    fun onClearTags() {
        uiState = uiState.copy(
            selectedTags = emptyList()
        )
    }

    fun onToggleTag(tag: String) {
        val current = uiState.selectedTags
        val next = if (current.contains(tag)) {
            current.filterNot { it == tag }
        } else {
            current + tag
        }
        uiState = uiState.copy(
            selectedTags = next
        )
    }

    /**
     * StoreProfile：保存（可编辑/可保存闭环核心）
     * - 先本地落盘（保证离线也能保存）
     * - 再云端 upsert（失败不影响本地）
     * - 写回 saving/error/success，UI 包可直接展示
     */
    fun saveStoreProfile(context: Context) {
        val draft = uiState.storeProfileDraft
        if (draft == null) {
            uiState = uiState.copy(storeProfileSaveError = "Nothing to save.")
            return
        }

        val title = draft.title.trim()
        if (title.isBlank()) {
            uiState = uiState.copy(storeProfileSaveError = "Store title is required.")
            return
        }

        val cleanedExtraContacts = draft.extraContacts
            .map { it.copy(name = it.name.trim(), value = it.value.trim()) }
            .filterNot { it.name.isBlank() && it.value.isBlank() }

        val hasHalfFilled = cleanedExtraContacts.any {
            (it.name.isBlank() && it.value.isNotBlank()) || (it.name.isNotBlank() && it.value.isBlank())
        }
        if (hasHalfFilled) {
            uiState = uiState.copy(storeProfileSaveError = "有联系方式只填了一半（Name/Value），请补全或清空后再保存。")
            return
        }

        val addressTrimmed = draft.address.trim()
        val mapUrlTrimmed = draft.mapUrl.trim()
        if (mapUrlTrimmed.isNotBlank()) {
            if (addressTrimmed.isBlank()) {
                uiState = uiState.copy(
                    storeProfileSaveError = "已填写 Map URL，但文本地址（Address）为空：请先填写地址，否则无法保存。"
                )
                return
            }
            val ok = mapUrlTrimmed.startsWith("https://") || mapUrlTrimmed.startsWith("http://")
            if (!ok) {
                uiState = uiState.copy(
                    isSavingStoreProfile = false,
                    storeProfileSaveError = "Map URL must start with http:// or https://."
                )
                return
            }
        }

        val normalized = draft.copy(
            title = draft.title.trim(),
            subtitle = draft.subtitle.trim(),
            description = draft.description.trim(),
            address = draft.address.trim(),
            hours = draft.hours.trim(),
            mapUrl = draft.mapUrl.trim(),
            coverUrl = draft.coverUrl.trim(),
            logoUrl = draft.logoUrl.trim(),
            businessStatus = draft.businessStatus.trim(),
            services = draft.services
                .map { it.trim() }
                .filter { it.isNotBlank() },
            extraContacts = cleanedExtraContacts
        )

        uiState = uiState.copy(
            isSavingStoreProfile = true,
            storeProfileSaveError = null,
            storeProfileSaveSuccess = false,
            statusMessage = null
        )

        viewModelScope.launch {
            try {
                var logoUrl = normalized.logoUrl
                if (isLocalImageUri(logoUrl)) {
                    val up = uploadStoreImageIfNeeded(
                        context = context,
                        uri = Uri.parse(logoUrl),
                        kind = "logo"
                    )
                    if (up.isNullOrBlank() && handleMerchantStoreImageUploadExpiredIfNeeded(context)) {
                        return@launch
                    }
                    if (!up.isNullOrBlank()) logoUrl = up
                }

                val coverList = normalized.coverUrl
                    .replace("\\n", "\n")
                    .split("\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(9)

                val uploadedCovers = mutableListOf<String>()
                for (u in coverList) {
                    if (isLocalImageUri(u)) {
                        val up = uploadStoreImageIfNeeded(
                            context = context,
                            uri = Uri.parse(u),
                            kind = "cover"
                        )
                        if (up.isNullOrBlank() && handleMerchantStoreImageUploadExpiredIfNeeded(context)) {
                            return@launch
                        }
                        if (!up.isNullOrBlank()) {
                            uploadedCovers.add(up)
                        }
                    } else {
                        uploadedCovers.add(u)
                    }
                }

                val cloudLogoUrl = if (isLocalImageUri(logoUrl)) "" else logoUrl
                val cloudCoverUrl = uploadedCovers
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !isLocalImageUri(it) }
                    .joinToString("\n")

                val uploadedNormalized = normalized.copy(
                    logoUrl = cloudLogoUrl,
                    coverUrl = cloudCoverUrl
                )

                val extraContactsJson = org.json.JSONArray().apply {
                    uploadedNormalized.extraContacts.forEach { c ->
                        if (c.name.isNotBlank() && c.value.isNotBlank()) {
                            put(org.json.JSONObject().apply {
                                put("name", c.name)
                                put("value", c.value)
                            })
                        }
                    }
                }.toString()

                val servicesJson = org.json.JSONArray().apply {
                    uploadedNormalized.services.forEach { s ->
                        val v = s.trim()
                        if (v.isNotBlank()) {
                            put(v)
                        }
                    }
                }.toString()

                val ok = cloudRepository.upsertStoreProfile(
                    storeId = chatStoreId,
                    title = uploadedNormalized.title,
                    subtitle = uploadedNormalized.subtitle,
                    description = uploadedNormalized.description,
                    address = uploadedNormalized.address,
                    hours = uploadedNormalized.hours,
                    mapUrl = uploadedNormalized.mapUrl,
                    coverUrl = uploadedNormalized.coverUrl,
                    logoUrl = uploadedNormalized.logoUrl,
                    businessStatus = uploadedNormalized.businessStatus,
                    extraContactsJson = extraContactsJson,
                    servicesJson = servicesJson
                )

                if (ok) {
                    clearStoreProfileDraftLocalImages()
                    saveStoreProfileToStorage(context, uploadedNormalized)
                    uiState = uiState.copy(
                        storeProfile = uploadedNormalized,
                        storeProfileDraft = null,
                        isSavingStoreProfile = false,
                        storeProfileSaveError = null,
                        storeProfileSaveSuccess = true,
                        statusMessage = "Store profile saved."
                    )
                } else {
                    val code = cloudRepository.lastUpsertCode
                    val body = cloudRepository.lastUpsertBody

                    if (handleMerchantAuthExpiredIfNeeded(
                            context = context,
                            code = code,
                            body = body
                        )
                    ) {
                        return@launch
                    }

                    val detail = buildString {
                        if (code != null) append(" code=").append(code)
                        if (!body.isNullOrBlank()) append(" body=").append(body.take(300))
                    }

                    uiState = uiState.copy(
                        isSavingStoreProfile = false,
                        storeProfileSaveError = "Cloud save failed.$detail",
                        storeProfileSaveSuccess = false,
                        statusMessage = "Couldn't save store profile. Please try again."
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ShowcaseCloud", "saveStoreProfile failed", e)
                uiState = uiState.copy(
                    isSavingStoreProfile = false,
                    storeProfileSaveError = "Cloud save failed.",
                    storeProfileSaveSuccess = false,
                    statusMessage = "Couldn't save store profile. Please try again."
                )
            }
        }
    }


    /**
     * StoreProfile：手动从云端刷新（可选）
     * - 成功：覆盖本地缓存与展示态
     * - 失败：不影响当前本地展示
     */
    fun refreshStoreProfileFromCloud(context: Context) {
        viewModelScope.launch {
            try {
                val cloud = cloudRepository.fetchStoreProfile(chatStoreId)
                if (cloud != null) {

                    // 1) 云端映射
                    val mapped = StoreProfile(
                        title = cloud.title,
                        subtitle = cloud.subtitle,
                        description = cloud.description,
                        address = cloud.address,
                        hours = cloud.hours,
                        mapUrl = cloud.mapUrl,
                        services = try {
                            val arr = org.json.JSONArray(cloud.servicesJson.ifBlank { "[]" })
                            buildList {
                                for (i in 0 until arr.length()) {
                                    arr.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
                                }
                            }
                        } catch (_: Exception) {
                            emptyList()
                        },
                        extraContacts = decodeExtraContactsJson(cloud.extraContactsJson),
                        coverUrl = cloud.coverUrl,
                        logoUrl = cloud.logoUrl,
                        businessStatus = cloud.businessStatus,
                    )

                    // 2) ✅ 关键兜底：云端空值不能覆盖本地已有图片
                    val local = uiState.storeProfile
                    val merged = if (local != null) {
                        mapped.copy(
                            coverUrl = mapped.coverUrl.trim().takeIf { it.isNotBlank() } ?: local.coverUrl,
                            logoUrl = mapped.logoUrl.trim().takeIf { it.isNotBlank() } ?: local.logoUrl,
                            services = if (mapped.services.isNotEmpty()) mapped.services else local.services,
                            extraContacts = if (mapped.extraContacts.isNotEmpty()) mapped.extraContacts else local.extraContacts,
                        )
                    } else {
                        mapped
                    }

                    runCatching { saveStoreProfileToStorage(context, merged) }
                    uiState = uiState.copy(storeProfile = merged, statusMessage = "Profile refreshed.")
                }
            } catch (_: Exception) {
                uiState = uiState.copy(
                    statusMessage = "Profile refresh failed.",
                    syncErrorMessage = "Profile refresh failed.",
                    lastRetryOp = ShowcaseRetryOp.RefreshStoreProfile
                )
            }

        }
    }



    /**
     * 进入详情：
     * 现在的设计是：只要 selectedDish != null 就显示详情层，
     * 不再切换到单独的 Detail screen，也不再用 previousScreen。
     */
    fun openDetail(dish: DemoDish) {
        detailBackTarget = uiState.screen

        val nextCount = (dish.clickCount + 1).coerceAtLeast(0)
        val updatedDish = dish.copy(clickCount = nextCount)
        val updatedDishes = uiState.dishes.map { if (it.id == dish.id) updatedDish else it }

        uiState = uiState.copy(
            selectedDish = updatedDish,
            dishes = updatedDishes,
            screen = ShowcaseScreen.Detail,
            statusMessage = null,
            detailImageIndex = 0
        )

        appContext?.let { ctx ->
            saveDishesToStorage(ctx, updatedDishes)
        }

        viewModelScope.launch {
            showcaseCloudRepository.incrementDishClickCount(
                storeId = chatStoreId,
                dishId = dish.id
            )
        }
    }
    fun openDetailById(dishId: String) {
        val dish = uiState.dishes.firstOrNull { it.id.toString() == dishId } ?: return
        openDetail(dish)
    }



    /**
     * Detail 多图：UI pager currentPage 回写到 VM
     */
    fun onDetailImageIndexChanged(index: Int) {
        val dish = uiState.selectedDish

        // 没有菜品时：兜底为 0
        if (dish == null) {
            uiState = uiState.copy(detailImageIndex = 0)
            return
        }

        // VM 侧统一决定“有效图片列表长度”
        // 这里不要依赖 UI 包的 images；VM 自己算
        val urls = dish.imageUrls.filter { it.isNotBlank() }
        val preview = dish.imageUri?.toString()?.takeIf { it.isNotBlank() }
        val resolved = if (urls.isNotEmpty()) urls else (preview?.let { listOf(it) } ?: emptyList())

        val maxIndex = (resolved.size - 1).coerceAtLeast(0)
        val safe = index.coerceIn(0, maxIndex)

        uiState = uiState.copy(detailImageIndex = safe)
    }


    /**
     * 从详情层返回：只需把 selectedDish 清空即可。
     */
    fun backFromDetail() {
        Log.d(
            "NDJC_DETAIL",
            "backFromDetail BEFORE, selectedDish=${uiState.selectedDish?.id}, screen=${uiState.screen}"
        )
        val target = detailBackTarget

        uiState = uiState.copy(
            selectedDish = null,
            screen = target,
            statusMessage = "Back from detail pressed"
        )


        Log.d(
            "NDJC_DETAIL",
            "backFromDetail AFTER, selectedDish=${uiState.selectedDish?.id}, screen=${uiState.screen}"
        )
    }

    fun backToHome() {

        // ✅ 若从 Chat 系列页面回 Home：先停轮询并清掉“定位/闪烁”
        if (
            uiState.screen == ShowcaseScreen.Chat ||
            uiState.screen == ShowcaseScreen.ChatSearchResults ||
            uiState.screen == ShowcaseScreen.ChatMedia
        ) {
            stopChatPolling()

            chatDomain.clearJumpOnExit()
            chatDomain.clearFlash()

            // ✅ 回 Home：一般不需要保留搜索页状态，直接关掉 find（清 query/结果）
            chatDomain.closeFind()
            syncChat()
        }

        snapshotCurrentChatContext()
        restoreClientChatContext()

        uiState = uiState.copy(
            screen = ShowcaseScreen.Home,
            loginError = null
        )
    }
    // ✅ Admin 系列页面：Back = 回上一页（AdminItems/AdminCategories → Admin；Admin → Home）
    fun backFromAdmin() {
        val nextState = when (uiState.screen) {
            ShowcaseScreen.AdminItems,
            ShowcaseScreen.AdminCategories,
            ShowcaseScreen.AdminAnnouncementEdit -> {
                uiState.copy(screen = ShowcaseScreen.Admin)
            }
            ShowcaseScreen.Admin -> {
                snapshotCurrentChatContext()
                restoreClientChatContext()
                uiState.copy(
                    screen = ShowcaseScreen.Home,
                    loginError = null
                )
            }
            else -> {
                // 兜底：如果误从非 Admin 页调用，按“回 Home”处理
                snapshotCurrentChatContext()
                restoreClientChatContext()
                uiState.copy(
                    screen = ShowcaseScreen.Home,
                    loginError = null
                )
            }
        }

        uiState = nextState
    }

    fun tryAdminLogin(context: Context, username: String, password: String) {
        val u = username.trim()
        val p = password.trim()

        if (u.isBlank() || p.isBlank()) {
            uiState = uiState.copy(loginError = "Please enter account and password.")
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            loginError = null
        )

        viewModelScope.launch {
            val session = showcaseCloudRepository.signInMerchant(
                loginName = u,
                password = p
            )
            if (session == null) {
                ShowcaseStoreSession.clearMerchantSession()
                clearStoredMerchantSession(context, clearRememberMe = false)
                uiState = uiState.copy(
                    isLoading = false,
                    isAdminLoggedIn = false,
                    loginError = "Invalid account or password."
                )
                return@launch
            }

            ShowcaseMerchantSessionManager.init(context)

            ShowcaseStoreSession.setMerchantSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                authUserId = session.authUserId,
                loginName = session.loginName,
                expiresAt = session.expiresAt
            )

            val binding = showcaseCloudRepository.fetchMerchantBindingForCurrentStore(chatStoreId)
            if (binding == null || !binding.authUserId.equals(session.authUserId, ignoreCase = true)) {
                ShowcaseStoreSession.clearMerchantSession()
                clearStoredMerchantSession(context, clearRememberMe = false)
                uiState = uiState.copy(
                    isLoading = false,
                    isAdminLoggedIn = false,
                    loginError = "This account is not bound to current store."
                )
                return@launch
            }

            val effectiveLoginName = binding.loginName?.trim().orEmpty().ifBlank { session.loginName }
            adminUser = effectiveLoginName
            ShowcaseStoreSession.updateMerchantLoginName(effectiveLoginName)

            if (uiState.loginRememberMeDraft) {
                persistMerchantSession(context)
            } else {
                clearStoredMerchantSession(context, clearRememberMe = false)
            }

            refreshCloudServiceStatus()

            uiState = uiState.copy(
                isLoading = false,
                isAdminLoggedIn = true,
                screen = ShowcaseScreen.Admin,
                loginError = null,
                loginUsernameDraft = effectiveLoginName,
                loginPasswordDraft = "",
                adminUsernameDraft = effectiveLoginName,
                adminPasswordDraft = ""
            )
        }
    }

    fun onLoginUsernameDraftChange(v: String) {
        uiState = uiState.copy(loginUsernameDraft = v)
    }
    fun setLoginRememberMe(context: Context, checked: Boolean) {
        uiState = uiState.copy(loginRememberMeDraft = checked)
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_REMEMBER_ME, checked).apply()

            if (!checked) {
                ShowcaseStoreSession.clearMerchantSession()
                clearStoredMerchantSession(context, clearRememberMe = false)
                uiState = uiState.copy(isAdminLoggedIn = false)
            }
        } catch (_: Exception) {
        }
    }

    fun onLoginPasswordDraftChange(v: String) {
        uiState = uiState.copy(loginPasswordDraft = v)
    }

    fun canLogin(): Boolean {
        return uiState.loginUsernameDraft.trim().isNotBlank() &&
                uiState.loginPasswordDraft.trim().isNotBlank() &&
                !uiState.isLoading
    }

    /**
     * 打开“编辑已有菜品”的编辑页。
     * 新增菜品的入口用 openNewDishScreen()。
     */
    fun openEditScreen(context: Context, dishId: String) {
        appContext = context.applicationContext

        val dishes = uiState.dishes
        val index = dishes.indexOfFirst { it.id == dishId }
        val targetDish = dishes.getOrNull(index) ?: return
        val cached = loadItemEditorDraftLocally()

        val useCachedForEdit = cached != null && cached.isNew == false

        editDraft = EditDraftState(
            isNew = false,
            index = index.takeIf { it >= 0 },
            original = targetDish,
            name = if (useCachedForEdit) cached!!.name else targetDish.nameZh.ifBlank { targetDish.nameEn },
            price = if (useCachedForEdit) cached!!.price else targetDish.originalPrice.toString(),
            discountPrice = if (useCachedForEdit) cached!!.discountPrice else targetDish.discountPrice?.toString() ?: "",
            description = if (useCachedForEdit) cached!!.description else targetDish.descriptionEn,
            category = if (useCachedForEdit) cached!!.category else targetDish.category.ifBlank { null },
            isRecommended = targetDish.isRecommended,
            isSoldOut = targetDish.isSoldOut,
            isHidden = targetDish.isHidden,
            imageUris = targetDish.imageUrls.map { Uri.parse(it) }
                .ifEmpty { targetDish.imageUri?.let { listOf(it) } ?: emptyList() }
        )

        val current = uiState.screen
        if (current != ShowcaseScreen.Edit) {
            editBackTarget = current
        }

        uiState = uiState.copy(
            selectedDish = targetDish,
            screen = ShowcaseScreen.Edit,
            statusMessage = null,
            editValidationError = null,
        )
    }

    /**
     * 打开“新增菜品”的编辑页：不依赖 selectedDish。
     * UI 层看到 selectedDish == null → 走 isNew = true 的分支。
     */
    fun openNewDishScreen(context: Context) {
        appContext = context.applicationContext

        val cached = loadItemEditorDraftLocally()
        val useCachedForNew = cached?.isNew == true

        editDraft = EditDraftState(
            isNew = true,
            index = null,
            original = null,
            name = if (useCachedForNew) cached!!.name else "",
            price = if (useCachedForNew) cached!!.price else "",
            discountPrice = if (useCachedForNew) cached!!.discountPrice else "",
            description = if (useCachedForNew) cached!!.description else "",
            category = if (useCachedForNew) cached!!.category else null,
            isRecommended = false,
            isSoldOut = false,
            isHidden = false,
            imageUris = emptyList()
        )

        val current = uiState.screen
        if (current != ShowcaseScreen.Edit) {
            editBackTarget = current
        }

        uiState = uiState.copy(
            selectedDish = null,
            screen = ShowcaseScreen.Edit,
            statusMessage = null,
            editValidationError = null
        )
    }

    /**
     * 从编辑页返回 Admin：
     * 只清当前会话内编辑态，不清本地文本草稿。
     *
     * 结果：
     * 1. 文本草稿仍保留在本地，下次重新进入编辑页可恢复
     * 2. 图片只存在当前会话内，editDraft 置空后不会继续保留
     */
    fun backToAdminFromEdit() {
        clearEditDraftLocalImages()
        editDraft = null
        uiState = uiState.copy(
            screen = editBackTarget,   // ✅ 回到进入 Edit 前的页面（例如 AdminItems）
            selectedDish = null,
            statusMessage = null
        )

        // 可选：复位，避免极端情况下残留
        editBackTarget = ShowcaseScreen.Admin
    }

    /**
     * 返回 Home：
     * 只清当前会话内编辑态，不清本地文本草稿。
     *
     * 结果：
     * 1. 文本草稿仍保留在本地，下次重新进入编辑页可恢复
     * 2. 图片只存在当前会话内，editDraft 置空后不会继续保留
     */
    fun discardEditDraftAndGoHome() {
        clearEditDraftLocalImages()
        editDraft = null
        uiState = uiState.copy(
            selectedDish = null,
            statusMessage = null
        )
        editBackTarget = ShowcaseScreen.Admin
        backToHome()
    }

    fun hasUnsavedEditDraft(): Boolean {
        val draft = editDraft ?: return false

        val currentImages = draft.imageUris
            .map { it.toString().trim() }
            .filter { it.isNotBlank() }

        if (draft.isNew) {
            return draft.name.trim().isNotBlank() ||
                    draft.price.trim().isNotBlank() ||
                    draft.discountPrice.trim().isNotBlank() ||
                    draft.description.trim().isNotBlank() ||
                    !draft.category.isNullOrBlank() ||
                    draft.isRecommended ||
                    draft.isHidden ||
                    currentImages.isNotEmpty()
        }

        val original = draft.original ?: return false

        val originalName = original.nameZh.ifBlank { original.nameEn }.trim()
        val originalPrice = original.originalPrice.toString()
        val originalDiscountPrice = original.discountPrice?.toString().orEmpty()
        val originalDescription = original.descriptionEn.trim()
        val originalCategory = original.category.trim()
        val originalImages = original.imageUrls
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty {
                original.imageUri?.toString()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { listOf(it) }
                    ?: emptyList()
            }

        return draft.name.trim() != originalName ||
                draft.price.trim() != originalPrice ||
                draft.discountPrice.trim() != originalDiscountPrice ||
                draft.description.trim() != originalDescription ||
                draft.category.orEmpty().trim() != originalCategory ||
                draft.isRecommended != original.isRecommended ||
                draft.isHidden != original.isHidden ||
                currentImages != originalImages
    }


    // --------------------
    // 编辑页：本地草稿更新函数
    // --------------------

    private fun updateEditDraft(transform: (EditDraftState) -> EditDraftState) {
        val current = editDraft ?: return
        editDraft = transform(current)
        persistItemEditorDraftLocally()
        if (uiState.editValidationError != null) {
            uiState = uiState.copy(editValidationError = null)
        }
    }
    fun dismissEditValidationError() {
        if (uiState.editValidationError == null) return
        uiState = uiState.copy(editValidationError = null)
    }
    // --------------------
// StoreProfile：本地草稿更新函数（可编辑/可保存必备）
// --------------------
    private fun updateStoreProfileDraft(transform: (StoreProfile) -> StoreProfile) {
        val current = uiState.storeProfileDraft ?: (uiState.storeProfile ?: StoreProfile())
        uiState = uiState.copy(
            storeProfileDraft = transform(current),
            storeProfileSaveError = null
        )
    }



    fun onEditNameChange(value: String) {
        updateEditDraft { it.copy(name = value) }
    }

    fun onEditPriceChange(value: String) {
        updateEditDraft { it.copy(price = value) }
    }

    fun onEditDiscountPriceChange(value: String) {
        updateEditDraft { it.copy(discountPrice = value) }
    }

    fun onEditDescriptionChange(value: String) {
        // 描述也在这里做一次长度裁剪，保证 UI 和最终保存一致
        val trimmed = value.take(MAX_DESCRIPTION_LENGTH)
        updateEditDraft { it.copy(description = trimmed) }
    }

    fun onEditCategorySelected(category: String?) {
        updateEditDraft { it.copy(category = category) }
    }

    fun onEditToggleRecommended(checked: Boolean) {
        updateEditDraft { it.copy(isRecommended = checked) }
    }

    fun onEditToggleHidden(checked: Boolean) {
        updateEditDraft { it.copy(isHidden = checked) }
    }



    /**
     * 点击“+ 图片”按钮：这里只做占位提示。
     *
     * 真正选完图后，由宿主 Activity/Fragment 拿到 Uri，
     * 再调用 onEditImageSelected(context, uri) 写回编辑草稿。
     */
    fun onEditPickImageClick() {
        uiState = uiState.copy(
            statusMessage = "Image picker not wired yet. (onEditPickImageClick)"
        )
    }

    /**
     * 真正选择到图片后的入口：
     * 1) 压缩
     * 2) 写入 editDraft.imageUri
     *
     * 注意：这里不立刻 upsert 云端，等 onEditSave() 统一提交。
     */
    fun onEditImageSelected(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            val compressed = compressImage(
                context = context,
                sourceUri = sourceUri,
                maxLongEdge = PRODUCT_IMAGE_LONG_EDGE,
                jpegQuality = PRODUCT_IMAGE_JPEG_QUALITY
            )
            if (compressed == null) {
                uiState = uiState.copy(statusMessage = "Image compress failed.")
                return@launch
            }

            // 1) 写入编辑草稿
            updateEditDraft { draft ->
                val merged = (draft.imageUris + compressed)
                    .distinctBy { it.toString() }
                    .take(9)
                draft.copy(imageUris = merged)
            }


            // 2) 图片只保留当前会话内预览；本地编辑态缓存只保留文本
            persistItemEditorDraftLocally()

            // 3) 让 UI 立刻能预览
            uiState = uiState.copy(statusMessage = "Image selected.")
        }
    }
    fun onEditImagesSelected(context: Context, sourceUris: List<Uri>) {
        viewModelScope.launch {
            // editDraft 是 VM 内部草稿；UIState 里没有 editDraft
            val currentCount = editDraft?.imageUris?.size ?: 0
            val remaining = (9 - currentCount).coerceAtLeast(0)
            if (remaining == 0) {
                uiState = uiState.copy(statusMessage = "Reached max 9 images.")
                return@launch
            }

            val picked = sourceUris.take(remaining)

            val compressedUris = mutableListOf<Uri>()
            for (u in picked) {
                val compressed = compressImage(
                    context = context,
                    sourceUri = u,
                    maxLongEdge = PRODUCT_IMAGE_LONG_EDGE,
                    jpegQuality = PRODUCT_IMAGE_JPEG_QUALITY
                )
                if (compressed != null) compressedUris.add(compressed)
            }

            if (compressedUris.isEmpty()) {
                uiState = uiState.copy(statusMessage = "Image compress failed.")
                return@launch
            }

            // ✅ 统一通过 updateEditDraft 写入 imageUris（与 onEditImageSelected 一致）
            updateEditDraft { draft ->
                val merged = (draft.imageUris + compressedUris)
                    .distinctBy { it.toString() }
                    .take(9)
                draft.copy(imageUris = merged)
            }

            // ✅ 本地编辑态缓存只保留文本，不保留图片
            persistItemEditorDraftLocally()

            uiState = uiState.copy(statusMessage = "Images selected.")
        }
    }


    // ✅ 注意：这个函数必须放在 onEditImageSelected() 结束大括号之后（同级）
// ✅ 注意：这个函数必须放在 onEditImageSelected() 结束大括号之后（同级）
    fun onEditRemoveSelectedImage() {
        updateEditDraft { it.copy(imageUris = emptyList()) }
        uiState = uiState.copy(statusMessage = "All images removed.")
    }

    /**
     * 编辑页：删除单张图片（UI 只传 string；业务在 VM 决策并落地）
     * - 支持传入 Uri.toString() 或 URL 字符串
     * - 去掉匹配项（按 toString 精确匹配）
     */
    fun onEditRemoveImage(uriOrUrl: String) {
        val target = uriOrUrl.trim()
        if (target.isBlank()) return

        updateEditDraft { draft ->
            val kept = draft.imageUris.filterNot { it.toString() == target }
            if (kept.size == draft.imageUris.size) draft else draft.copy(imageUris = kept)
        }

        uiState = uiState.copy(statusMessage = "Image removed.")
    }

    fun onEditMoveImage(fromIndex: Int, toIndex: Int) {
        android.util.Log.d("NDJC_DND", "VM onEditMoveImage from=$fromIndex to=$toIndex")
        if (fromIndex == toIndex) return

        updateEditDraft { draft ->
            android.util.Log.d("NDJC_DND", "VM BEFORE imageUris=${draft.imageUris}")

            val list = draft.imageUris.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices) {
                android.util.Log.d("NDJC_DND", "VM OUT_OF_RANGE size=${list.size}")
                return@updateEditDraft draft
            }

            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)

            val newDraft = draft.copy(imageUris = list)
            android.util.Log.d("NDJC_DND", "VM AFTER  imageUris=${newDraft.imageUris}")
            newDraft
        }
    }





    /**
     * 编辑页点击“Create item / Save”：
     * 把 editDraft 合成 DemoDish，调用 updateDish() → 本地 + 云端一起写，
     * 然后回到 Admin。
     */
    fun onEditSave(context: Context) {
        val draft = editDraft
        if (draft == null) {
            uiState = uiState.copy(
                statusMessage = "Nothing to save (editDraft is null)."
            )
            return
        }

        val name = draft.name.trim()
        if (name.isEmpty()) {
            uiState = uiState.copy(
                editValidationError = "Please enter Name."
            )
            return
        }

        val priceText = draft.price.trim()
        if (priceText.isEmpty()) {
            uiState = uiState.copy(
                editValidationError = "Please enter Price."
            )
            return
        }

        val price = priceText.toFloatOrNull()
        if (price == null || price <= 0f) {
            uiState = uiState.copy(
                editValidationError = "Please enter a valid Price."
            )
            return
        }

        val description = draft.description.trim()
        if (description.isEmpty()) {
            uiState = uiState.copy(
                editValidationError = "Please enter Description."
            )
            return
        }

        val category = draft.category?.trim().orEmpty()
        if (category.isEmpty()) {
            uiState = uiState.copy(
                editValidationError = "Please enter Category."
            )
            return
        }

        if (draft.imageUris.isEmpty()) {
            uiState = uiState.copy(
                editValidationError = "Please add at least 1 image."
            )
            return
        }

        val discountPriceText = draft.discountPrice.trim()
        val discountPrice = discountPriceText.takeIf { it.isNotEmpty() }?.toFloatOrNull()

        if (discountPriceText.isNotEmpty() && discountPrice == null) {
            uiState = uiState.copy(statusMessage = "Invalid discount price.")
            return
        }
        if (discountPrice != null && discountPrice >= price) {
            uiState = uiState.copy(
                statusMessage = "Discount price must be lower than price."
            )
            return
        }

        if (discountPrice != null) {
            if (discountPrice <= 0f) {
                uiState = uiState.copy(statusMessage = "Discount price must be > 0.")
                return
            }
            if (discountPrice >= price) {
                uiState = uiState.copy(statusMessage = "Discount price must be lower than price.")
                return
            }
        }


        val dishId = draft.original?.id
        val index = if (!draft.isNew && dishId != null) {
            uiState.dishes.indexOfFirst { it.id == dishId }.takeIf { it >= 0 } ?: (draft.index ?: -1)
        } else {
            -1
        }


        // 旧菜用 original 作为 base，新菜创建一条全新的 DemoDish
        val base: DemoDish = draft.original ?: DemoDish(
            id = UUID.randomUUID().toString(),
            nameZh = name,
            nameEn = name,
            descriptionEn = draft.description,
            category = draft.category.orEmpty(),
            originalPrice = price,
            discountPrice = discountPrice,
            isRecommended = draft.isRecommended,
            isSoldOut = false,
            isHidden = draft.isHidden,
            imageResId = null,
            // legacy preview：第一张图
            imageUri = draft.imageUris.firstOrNull(),
            // ✅ 方案 B：多图（先用本地 Uri 字符串占位，保存时会上传并替换为公网 URL）
            imageUrls = draft.imageUris.map { it.toString() }.distinct().take(9)
        )

        val now = System.currentTimeMillis()

        val updated = base.copy(
            nameZh = name,
            nameEn = name,
            descriptionEn = draft.description.take(MAX_DESCRIPTION_LENGTH),
            category = draft.category.orEmpty(),
            originalPrice = price,
            discountPrice = discountPrice,
            isRecommended = draft.isRecommended,
            isSoldOut = false,
            isHidden = draft.isHidden,
            imageResId = null,
            imageUri = draft.imageUris.firstOrNull(),
            imageUrls = draft.imageUris.map { it.toString() }.distinct().take(9),

            // ✅ 离线/同步必备字段：先标记 Pending + dirty
            updatedAt = now,
            syncState = SyncState.Pending,
            dirty = true
        )

// ✅ 保存后的跳转交给 updateDish() 根据云端结果决定：
// - 完全成功：回到进入 Edit 前的页面
// - 图片上传失败：停留在 Edit 页并提示
        updateDish(context, index, updated)

        uiState = uiState.copy(
            editValidationError = null
        )


    }

    fun updateDish(context: Context, index: Int, updated: DemoDish) {
        val normalized = updated.copy(
            descriptionEn = updated.descriptionEn.take(MAX_DESCRIPTION_LENGTH)
        )
        android.util.Log.d(
            "ShowcaseViewModel",
            "updateDish:start id=${updated.id} index=$index nameZh=${updated.nameZh} imageUrls=${updated.imageUrls} imageUri=${updated.imageUri}"
        )
        uiState = uiState.copy(
            isSavingEditDish = true,
            isBlockingEditDish = true,
            statusMessage = null,
            syncOverviewState = SyncOverviewState.Syncing,
            syncErrorMessage = null
        )
        viewModelScope.launch {
            try {
                val rawUrls = normalized.imageUrls
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(9)
                    .ifEmpty {
                        normalized.imageUri
                            ?.toString()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { listOf(it) }
                            ?: emptyList()
                    }
                android.util.Log.d(
                    "ShowcaseViewModel",
                    "updateDish:normalized id=${normalized.id} rawUrls=$rawUrls"
                )

                val effectiveUrls = mutableListOf<String>()
                val remoteUrls = mutableListOf<String>()
                var localUploadFailed = false

                for (u in rawUrls) {
                    android.util.Log.d(
                        "ShowcaseViewModel",
                        "updateDish:imageLoop source=$u isLocal=${isLocalImageUri(u)}"
                    )
                    if (isLocalImageUri(u)) {
                        val uploaded = uploadDishImageIfNeeded(
                            context = context,
                            uri = Uri.parse(u)
                        )
                        if (!uploaded.isNullOrBlank()) {
                            effectiveUrls.add(uploaded)
                            remoteUrls.add(uploaded)
                            android.util.Log.d(
                                "ShowcaseViewModel",
                                "updateDish:imageLoop upload success source=$u uploaded=$uploaded"
                            )
                        } else {
                            if (handleMerchantDishImageUploadExpiredIfNeeded(context)) {
                                return@launch
                            }
                            localUploadFailed = true
                            android.util.Log.w(
                                "ShowcaseViewModel",
                                "updateDish:imageLoop upload failed source=$u"
                            )
                        }
                    } else {
                        effectiveUrls.add(u)
                        remoteUrls.add(u)
                        android.util.Log.d(
                            "ShowcaseViewModel",
                            "updateDish:imageLoop remote passthrough source=$u"
                        )
                    }
                }
                val imageUploadFailed = rawUrls.isNotEmpty() && (remoteUrls.size != rawUrls.size || localUploadFailed)
                android.util.Log.d(
                    "ShowcaseViewModel",
                    "updateDish:imageSummary id=${normalized.id} rawCount=${rawUrls.size} remoteCount=${remoteUrls.size} localUploadFailed=$localUploadFailed imageUploadFailed=$imageUploadFailed effectiveUrls=$effectiveUrls remoteUrls=$remoteUrls"
                )

                android.util.Log.d(
                    "ShowcaseViewModel",
                    "updateDish:beforeUpsertDish id=${normalized.id} imageUri=${remoteUrls.firstOrNull()} category=${normalized.category}"
                )
                val okDish = cloudRepository.upsertDishFromDemo(
                    storeId = chatStoreId,
                    id = normalized.id,
                    nameZh = normalized.nameZh,
                    nameEn = normalized.nameEn,
                    descriptionEn = normalized.descriptionEn,
                    category = normalized.category,
                    originalPrice = normalized.originalPrice.toDouble(),
                    discountPrice = normalized.discountPrice?.toDouble(),
                    isRecommended = normalized.isRecommended,
                    isSoldOut = normalized.isSoldOut,
                    imageUri = remoteUrls.firstOrNull()
                )
                android.util.Log.d(
                    "ShowcaseViewModel",
                    "updateDish:afterUpsertDish id=${normalized.id} okDish=$okDish"
                )
                val okImgs = if (imageUploadFailed) {
                    android.util.Log.w(
                        "ShowcaseViewModel",
                        "updateDish:skipReplaceDishImages id=${normalized.id} because imageUploadFailed=true"
                    )
                    true
                } else {
                    android.util.Log.d(
                        "ShowcaseViewModel",
                        "updateDish:beforeReplaceDishImages id=${normalized.id} remoteUrls=$remoteUrls"
                    )
                    cloudRepository.replaceDishImages(
                        storeId = chatStoreId,
                        dishId = normalized.id,
                        imageUrls = remoteUrls
                    )
                }
                android.util.Log.d(
                    "ShowcaseViewModel",
                    "updateDish:afterReplaceDishImages id=${normalized.id} okImgs=$okImgs"
                )
                val ok = okDish && okImgs
                android.util.Log.d(
                    "ShowcaseViewModel",
                    "updateDish:finalResult id=${normalized.id} okDish=$okDish okImgs=$okImgs ok=$ok"
                )

                if (ok) {
                    android.util.Log.d(
                        "ShowcaseViewModel",
                        "updateDish:enterSuccessBranch id=${normalized.id} imageUploadFailed=$imageUploadFailed"
                    )
                    val refreshedFromCloud = tryLoadFromCloud()
                    val fallbackDish = normalized.copy(
                        imageUrls = effectiveUrls,
                        imageUri = effectiveUrls.firstOrNull()?.let { Uri.parse(it) },
                        updatedAt = System.currentTimeMillis(),
                        syncState = if (imageUploadFailed) SyncState.Pending else SyncState.Synced,
                        dirty = imageUploadFailed
                    )

                    val refreshed = if (refreshedFromCloud.isNotEmpty()) {
                        refreshedFromCloud.map { cloudDish ->
                            if (cloudDish.id == normalized.id &&
                                cloudDish.imageUrls.isEmpty() &&
                                cloudDish.imageUri == null
                            ) {
                                fallbackDish
                            } else {
                                cloudDish
                            }
                        }
                    } else {
                        val current = uiState.dishes.toMutableList()
                        if (index in current.indices) {
                            current[index] = fallbackDish
                        } else {
                            current.add(fallbackDish)
                        }
                        current
                    }

                    val selected = refreshed.firstOrNull { it.id == normalized.id } ?: fallbackDish
                    val refreshedIndex = refreshed.indexOfFirst { it.id == normalized.id }

                    saveDishesToStorage(context, refreshed)

                    if (imageUploadFailed) {
                        android.util.Log.w(
                            "ShowcaseViewModel",
                            "updateDish:imageUploadFailedAfterSave id=${normalized.id} selected=${selected.id} refreshedIndex=$refreshedIndex"
                        )
                        editDraft = editDraft?.copy(
                            original = selected,
                            index = refreshedIndex.takeIf { it >= 0 },
                            name = normalized.nameZh,
                            price = normalized.originalPrice.toString(),
                            discountPrice = normalized.discountPrice?.toString().orEmpty(),
                            description = normalized.descriptionEn,
                            category = normalized.category,
                            isRecommended = normalized.isRecommended,
                            isSoldOut = normalized.isSoldOut,
                            isHidden = normalized.isHidden,
                            imageUris = normalized.imageUrls.map { Uri.parse(it) }
                        )
                        uiState = uiState.copy(
                            dishes = refreshed,
                            selectedDish = selected,
                            screen = ShowcaseScreen.Edit,
                            isSavingEditDish = false,
                            isBlockingEditDish = false,
                            statusMessage = "Item saved, but image upload failed. Please try again.",
                            pendingSyncCount = 0,
                            syncOverviewState = SyncOverviewState.Failed,
                            syncErrorMessage = "Image upload failed.",
                            lastRetryOp = ShowcaseRetryOp.RetryPendingSync,
                            editValidationError = null
                        )
                    } else {
                        android.util.Log.d(
                            "ShowcaseViewModel",
                            "updateDish:saveCompleteAndNavigateBack id=${normalized.id} target=$editBackTarget"
                        )
                        val back = editBackTarget
                        val successMessage = if (editDraft?.isNew == true) {
                            "Item published."
                        } else {
                            "Item updated."
                        }

                        uiState = uiState.copy(
                            dishes = refreshed,
                            screen = ShowcaseScreen.Edit,
                            selectedDish = selected,
                            isSavingEditDish = false,
                            isBlockingEditDish = false,
                            statusMessage = successMessage,
                            pendingSyncCount = 0,
                            syncOverviewState = SyncOverviewState.Idle,
                            syncErrorMessage = null,
                            lastRetryOp = null,
                            editValidationError = null
                        )

                        kotlinx.coroutines.delay(800L)

                        uiState = if (back == ShowcaseScreen.Detail) {
                            uiState.copy(
                                screen = ShowcaseScreen.Detail,
                                selectedDish = selected,
                                statusMessage = null,
                                editValidationError = null
                            )
                        } else {
                            uiState.copy(
                                screen = back,
                                selectedDish = null,
                                statusMessage = null,
                                editValidationError = null
                            )
                        }

                        clearEditDraftLocalImages()
                        editDraft = null
                        clearItemEditorDraftLocally()
                        editBackTarget = ShowcaseScreen.Admin
                    }
                } else {
                    android.util.Log.e(
                        "ShowcaseViewModel",
                        "updateDish:saveFailed id=${normalized.id} okDish=$okDish okImgs=$okImgs remoteUrls=$remoteUrls"
                    )

                    if (!okDish && handleMerchantAuthExpiredIfNeeded(
                            context = context,
                            code = showcaseCloudRepository.lastUpsertCode,
                            body = showcaseCloudRepository.lastUpsertBody
                        )
                    ) {
                        return@launch
                    }

                    uiState = uiState.copy(
                        isSavingEditDish = false,
                        isBlockingEditDish = false,
                        statusMessage = "Couldn't publish item. Please try again.",
                        syncOverviewState = SyncOverviewState.Failed,
                        syncErrorMessage = "Cloud save failed.",
                        lastRetryOp = ShowcaseRetryOp.RetryPendingSync
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "ShowcaseViewModel",
                    "updateDish:exception id=${normalized.id}",
                    e
                )
                uiState = uiState.copy(
                    isSavingEditDish = false,
                    isBlockingEditDish = false,
                    statusMessage = "Couldn't publish item. Please try again.",
                    syncOverviewState = SyncOverviewState.Failed,
                    syncErrorMessage = "Cloud save failed.",
                    lastRetryOp = ShowcaseRetryOp.RetryPendingSync
                )
            }
        }
    }

    /**
     * 展示型必备：手动重试同步（对 Failed/Pending 条目重新 upsert）
     * - 不破坏现有 UI：可先不接按钮，逻辑先补齐。
     */
    fun retryPendingSync(context: Context) {
        val targets = uiState.dishes.filter { it.syncState == SyncState.Failed || it.syncState == SyncState.Pending }
        if (targets.isEmpty()) {
            uiState = uiState.copy(statusMessage = "No pending changes to sync.")
            return
        }

        uiState = uiState.copy(
            syncOverviewState = SyncOverviewState.Syncing,
            statusMessage = "Retrying cloud sync..."
        )

        // 逐条重试（最小实现；后续可做队列/并发控制）
        targets.forEach { dish ->
            val idx = uiState.dishes.indexOfFirst { it.id == dish.id }
            if (idx >= 0) {
                val marked = dish.copy(
                    syncState = SyncState.Pending,
                    dirty = true,
                    updatedAt = System.currentTimeMillis()
                )
                updateDish(context, idx, marked)
            }
        }
    }

    private fun formatHHmm(timeMs: Long): String {
        if (timeMs <= 0L) return ""
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timeMs }
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        return String.format("%02d:%02d", h, m)
    }
    private fun formatYmdAmpmHm(ms: Long): String {
        if (ms <= 0L) return ""
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }

        val y = cal.get(java.util.Calendar.YEAR)
        val mo = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')

        val h24 = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val mm = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')

        val ctx = appContext
        return if (ctx != null && android.text.format.DateFormat.is24HourFormat(ctx)) {
            val hh = h24.toString().padStart(2, '0')
            "$y-$mo-$d $hh:$mm"
        } else {
            val ampm = if (h24 < 12) "AM" else "PM"
            val h12 = (h24 % 12).let { if (it == 0) 12 else it }.toString().padStart(2, '0')
            "$y-$mo-$d $ampm $h12:$mm"
        }
    }

    private suspend fun uploadDishImageIfNeeded(
        context: Context,
        uri: Uri
    ): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            android.util.Log.d(
                "ShowcaseViewModel",
                "uploadDishImageIfNeeded:start uri=$uri scheme=${uri.scheme} path=${uri.path}"
            )
            val localPath = when (uri.scheme) {
                "file" -> uri.path
                null, "" -> uri.toString()
                else -> null
            }
            android.util.Log.d(
                "ShowcaseViewModel",
                "uploadDishImageIfNeeded:localPathResolved uri=$uri localPath=$localPath"
            )
            val bytes = when {
                !localPath.isNullOrBlank() -> {
                    val file = java.io.File(localPath)
                    if (!file.exists()) {
                        android.util.Log.w(
                            "ShowcaseViewModel",
                            "uploadDishImageIfNeeded: local file not found, path=$localPath"
                        )
                        null
                    } else {
                        val fileBytes = file.readBytes()
                        android.util.Log.d(
                            "ShowcaseViewModel",
                            "uploadDishImageIfNeeded:read local file success path=$localPath size=${fileBytes.size}"
                        )
                        fileBytes
                    }
                }
                else -> {
                    resolver.openInputStream(uri)?.use { input ->
                        val streamBytes = input.readBytes()
                        android.util.Log.d(
                            "ShowcaseViewModel",
                            "uploadDishImageIfNeeded:read content resolver success uri=$uri size=${streamBytes.size}"
                        )
                        streamBytes
                    }
                }
            } ?: run {
                android.util.Log.w(
                    "ShowcaseViewModel",
                    "uploadDishImageIfNeeded: failed to read bytes, uri=$uri"
                )
                return@withContext null
            }
            val uploaded = cloudRepository.uploadDishImageBytes(
                bytes = bytes,
                fileExt = "jpg",
                contentType = "image/jpeg"
            )
            if (uploaded.isNullOrBlank()) {
                android.util.Log.w(
                    "ShowcaseViewModel",
                    "uploadDishImageIfNeeded: uploadDishImageBytes returned null, uri=$uri size=${bytes.size}"
                )
                return@withContext null
            }

            deleteAppOwnedLocalFileUri(uri)

            android.util.Log.d(
                "ShowcaseViewModel",
                "uploadDishImageIfNeeded:success uri=$uri uploaded=$uploaded"
            )
            uploaded
        } catch (e: Exception) {
            android.util.Log.e(
                "ShowcaseViewModel",
                "uploadDishImageIfNeeded failed, uri=$uri",
                e
            )
            null
        }
    }
    private fun isLocalImageUri(raw: String): Boolean {
        val v = raw.trim()
        return v.startsWith("content://") ||
                v.startsWith("file://") ||
                (!v.startsWith("http://") && !v.startsWith("https://"))
    }
    private suspend fun uploadStoreImageIfNeeded(
        context: Context,
        uri: Uri,
        kind: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // ✅ 兼容“裸路径”
            val fixedUri = if (uri.scheme.isNullOrBlank() && (uri.toString().startsWith("/") || uri.path?.startsWith("/") == true)) {
                Uri.parse("file://${uri.path ?: uri.toString()}")
            } else uri

            val maxLongEdge =
                when (kind.lowercase()) {
                    "logo" -> STORE_LOGO_IMAGE_LONG_EDGE
                    "cover" -> STORE_COVER_IMAGE_LONG_EDGE
                    "announcement" -> ANNOUNCEMENT_IMAGE_LONG_EDGE
                    else -> STORE_COVER_IMAGE_LONG_EDGE
                }

            val jpegQuality =
                when (kind.lowercase()) {
                    "logo" -> STORE_LOGO_IMAGE_JPEG_QUALITY
                    "cover" -> STORE_COVER_IMAGE_JPEG_QUALITY
                    "announcement" -> ANNOUNCEMENT_IMAGE_JPEG_QUALITY
                    else -> STORE_COVER_IMAGE_JPEG_QUALITY
                }

            val compressedUri = compressImage(
                context = context,
                sourceUri = fixedUri,
                maxLongEdge = maxLongEdge,
                jpegQuality = jpegQuality
            ) ?: fixedUri

            val resolver = context.contentResolver
            val bytes =
                resolver.openInputStream(compressedUri)?.use { it.readBytes() }
                    ?: runCatching {
                        val p = compressedUri.path ?: return@runCatching null
                        java.io.File(p).readBytes()
                    }.getOrNull()

            if (bytes == null || bytes.isEmpty()) {
                android.util.Log.w("ShowcaseCloud", "uploadStoreImageIfNeeded: bytes empty kind=$kind uri=$compressedUri")
                return@withContext null
            }

            android.util.Log.d(
                "ShowcaseCloud",
                "uploadStoreImageIfNeeded: bytes=${bytes.size} kind=$kind uri=$compressedUri longEdge=$maxLongEdge jpeg=$jpegQuality"
            )

            val url = cloudRepository.uploadStoreImageBytes(
                bytes = bytes,
                kind = kind,
                fileExt = "jpg",
                contentType = "image/jpeg"
            )

            if (!url.isNullOrBlank()) {
                deleteAppOwnedLocalFileUri(compressedUri)
                if (compressedUri != fixedUri) {
                    deleteAppOwnedLocalFileUri(fixedUri)
                }
            }

            android.util.Log.d("ShowcaseCloud", "uploadStoreImageIfNeeded: resultUrl=$url kind=$kind")
            url
        } catch (e: Exception) {
            android.util.Log.e("ShowcaseCloud", "uploadStoreImageIfNeeded failed kind=$kind uri=$uri", e)
            null
        }
    }


    // ✅ 删除确认：统一用 pendingDeleteDishId（与 ShowcaseUiState/Contract 对齐）
    fun dismissPendingDelete() {
        uiState = uiState.copy(pendingDeleteDishId = null)
    }

    // UI 传 dishId 进来：直接记录（id 不会因筛选/排序漂移）
    fun requestDeleteDish(dishId: String) {
        val id = dishId.trim()
        if (id.isBlank()) return
        uiState = uiState.copy(pendingDeleteDishId = id)
    }

    // ✅ 兼容入口：如果有地方还在传 index（例如编辑页旧逻辑），这里映射成 id
    fun requestDeleteDish(index: Int) {
        val id = uiState.dishes.getOrNull(index)?.id ?: return
        requestDeleteDish(id)
    }

    // ✅ 按 dishId 删除（避免“点 33 弹 jkk”这种 index 漂移）
    fun deleteDish(context: Context, dishId: String) {
        val id = dishId.trim()
        if (id.isBlank()) {
            uiState = uiState.copy(pendingDeleteDishId = null)
            return
        }

        val dish = uiState.dishes.firstOrNull { it.id == id }
        if (dish == null) {
            uiState = uiState.copy(pendingDeleteDishId = null)
            return
        }

        uiState = uiState.copy(
            pendingDeleteDishId = null,
            statusMessage = "Deleting from cloud..."
        )

        viewModelScope.launch {
            try {
                val url = dish.imageUri?.toString()
                if (!url.isNullOrBlank() && !isLocalImageUri(url)) {
                    runCatching { cloudRepository.deleteDishImageByUrl(url) }
                }

                val ok = if (dish.id.isNotBlank()) {
                    cloudRepository.deleteDishById(
                        storeId = chatStoreId,
                        id = dish.id
                    )
                } else {
                    false
                }

                if (ok) {
                    val refreshed = tryLoadFromCloud()
                    uiState = uiState.copy(
                        dishes = refreshed,
                        statusMessage = "Dish deleted.",
                        syncOverviewState = SyncOverviewState.Idle,
                        syncErrorMessage = null
                    )
                    saveDishesToStorage(context, refreshed)
                } else {
                    if (handleMerchantDeleteExpiredIfNeeded(context)) {
                        return@launch
                    }

                    uiState = uiState.copy(
                        statusMessage = "Cloud delete failed.",
                        syncOverviewState = SyncOverviewState.Failed,
                        syncErrorMessage = "Cloud delete failed."
                    )
                }
            } catch (_: Exception) {
                uiState = uiState.copy(
                    statusMessage = "Cloud delete failed.",
                    syncOverviewState = SyncOverviewState.Failed,
                    syncErrorMessage = "Cloud delete failed."
                )
            }
        }
    }

    // =========================
    // ✅ AdminItems：批量删除勾选（逻辑模块负责操作；UI 只管显示勾选框）
    // =========================

    fun toggleAdminDishSelected(dishId: String) {
        val id = dishId.trim()
        if (id.isBlank()) return
        val current = uiState.adminSelectedDishIds
        uiState = if (current.contains(id)) {
            uiState.copy(adminSelectedDishIds = current - id)
        } else {
            uiState.copy(adminSelectedDishIds = current + id)
        }
    }

    fun clearAdminDishSelection() {
        if (uiState.adminSelectedDishIds.isEmpty()) return
        uiState = uiState.copy(adminSelectedDishIds = emptySet())
    }

    fun deleteSelectedDishes(context: Context) {
        val ids = uiState.adminSelectedDishIds.toList()
        if (ids.isEmpty()) return

        val toDelete = uiState.dishes.filter { ids.contains(it.id) }
        if (toDelete.isEmpty()) {
            uiState = uiState.copy(adminSelectedDishIds = emptySet())
            return
        }

        uiState = uiState.copy(
            adminSelectedDishIds = emptySet(),
            statusMessage = "Deleting ${toDelete.size} item(s) from cloud..."
        )

        viewModelScope.launch {
            try {
                var allOk = true

                toDelete.forEach { dish ->
                    runCatching {
                        val url = dish.imageUri?.toString()
                        if (!url.isNullOrBlank() && !isLocalImageUri(url)) {
                            cloudRepository.deleteDishImageByUrl(url)
                        }
                    }

                    val ok = runCatching {
                        if (dish.id.isNotBlank()) {
                            cloudRepository.deleteDishById(
                                storeId = chatStoreId,
                                id = dish.id
                            )
                        } else {
                            false
                        }
                    }.getOrElse { false }

                    if (!ok) {
                        if (handleMerchantDeleteExpiredIfNeeded(context)) {
                            return@launch
                        }
                        allOk = false
                    }
                }

                if (allOk) {
                    val refreshed = tryLoadFromCloud()
                    uiState = uiState.copy(
                        dishes = refreshed,
                        statusMessage = "Deleted ${toDelete.size} item(s).",
                        syncOverviewState = SyncOverviewState.Idle,
                        syncErrorMessage = null
                    )
                    saveDishesToStorage(context, refreshed)
                } else {
                    uiState = uiState.copy(
                        statusMessage = "Cloud delete failed.",
                        syncOverviewState = SyncOverviewState.Failed,
                        syncErrorMessage = "Cloud delete failed."
                    )
                }
            } catch (_: Exception) {
                uiState = uiState.copy(
                    statusMessage = "Cloud delete failed.",
                    syncOverviewState = SyncOverviewState.Failed,
                    syncErrorMessage = "Cloud delete failed."
                )
            }
        }
    }


    /**
     * 新增分类：
     * - 先写云端
     * - 云端成功后再刷新本地状态
     * - 云端失败时，本地分类列表保持原样，只弹状态提醒
     */
    fun addCategory(name: String) {
        val cat = name.trim()
        if (cat.isBlank()) return

        val existing = deriveCategories(uiState.dishes, uiState.manualCategories)
        if (existing.contains(cat)) return

        viewModelScope.launch {
            try {
                val result = cloudRepository.ensureCategoryExists(
                    storeId = chatStoreId,
                    name = cat
                )

                if (result.ok) {
                    val refreshed = tryLoadFromCloud()
                    val finalDishes = if (refreshed.isNotEmpty()) refreshed else uiState.dishes
                    val allCategoryNames = cloudRepository.fetchCategories(chatStoreId)
                        .map { it.name }
                        .filter { it.isNotBlank() }
                        .distinct()

                    uiState = uiState.copy(
                        dishes = finalDishes,
                        manualCategories = allCategoryNames,
                        statusMessage = null
                    )

                    appContext?.let { safeContext ->
                        saveDishesToStorage(context = safeContext, dishes = finalDishes)
                        saveManualCategoriesToStorage(
                            context = safeContext,
                            categories = allCategoryNames
                        )
                    }
                } else {
                    Log.e(
                        "ShowcaseViewModel",
                        "addCategory: cloud failed code=${result.errorCode} body=${result.errorBody}"
                    )

                    val safeContext = appContext
                    if (safeContext != null && handleMerchantAuthExpiredIfNeeded(
                            context = safeContext,
                            code = result.errorCode,
                            body = result.errorBody
                        )
                    ) {
                        return@launch
                    }

                    uiState = uiState.copy(
                        statusMessage = result.errorMessage ?: "Failed to add category."
                    )
                }
            } catch (e: Exception) {
                Log.e("ShowcaseViewModel", "addCategory: failed", e)
                uiState = uiState.copy(
                    statusMessage = "Failed to add category."
                )
            }
        }
    }
    /**
     * 展示型必备：分类重命名（运营常用）
     * - 云端成功后再更新本地
     * - 云端失败时，本地分类列表保持原样，只弹状态提醒
     */
    fun renameCategory(context: Context, oldName: String, newName: String) {
        val oldCat = oldName.trim()
        val newCat = newName.trim()
        if (oldCat.isBlank() || newCat.isBlank()) return
        if (oldCat == newCat) return

        viewModelScope.launch {
            try {
                val catId = cloudRepository.getCategoryIdByName(
                    storeId = chatStoreId,
                    name = oldCat
                )

                val result = if (!catId.isNullOrBlank()) {
                    cloudRepository.renameCategoryById(
                        storeId = chatStoreId,
                        categoryId = catId,
                        newName = newCat
                    )
                } else {
                    ShowcaseCloudRepository.CategoryWriteResult(
                        ok = false,
                        errorMessage = "Update category failed. Category id was not found in cloud."
                    )
                }

                if (result.ok) {
                    val refreshed = tryLoadFromCloud()
                    val finalDishes = if (refreshed.isNotEmpty()) refreshed else uiState.dishes
                    val allCategoryNames = cloudRepository.fetchCategories(chatStoreId)
                        .map { it.name }
                        .filter { it.isNotBlank() }
                        .distinct()

                    uiState = uiState.copy(
                        dishes = finalDishes,
                        manualCategories = allCategoryNames,
                        selectedCategory = if (uiState.selectedCategory?.trim() == oldCat) newCat else uiState.selectedCategory,
                        statusMessage = null
                    )

                    saveDishesToStorage(context, finalDishes)
                    saveManualCategoriesToStorage(context, allCategoryNames)
                } else {
                    Log.e(
                        "ShowcaseViewModel",
                        "renameCategory: cloud failed code=${result.errorCode} body=${result.errorBody}"
                    )

                    if (handleMerchantAuthExpiredIfNeeded(
                            context = context,
                            code = result.errorCode,
                            body = result.errorBody
                        )
                    ) {
                        return@launch
                    }

                    uiState = uiState.copy(
                        statusMessage = result.errorMessage ?: "Failed to update category."
                    )
                }
            } catch (e: Exception) {
                Log.e("ShowcaseViewModel", "renameCategory: failed", e)
                uiState = uiState.copy(
                    statusMessage = "Failed to update category."
                )
            }
        }
    }

    /**
     * 删除分类：
     * 1) 本地：清空菜品中的 category + 移除 manualCategories
     * 2) 云端：删除 categories 表记录 + 把受影响菜品回写 Supabase
     */
    /**
     * 删除分类：
     * - 云端成功后再更新本地
     * - 云端失败时，本地分类列表保持原样，只弹状态提醒
     */
    fun removeCategory(context: Context, category: String) {
        val cat = category.trim()
        if (cat.isBlank()) return

        viewModelScope.launch {
            try {
                val catId = cloudRepository.getCategoryIdByName(
                    storeId = chatStoreId,
                    name = cat
                )

                if (catId.isNullOrBlank()) {
                    uiState = uiState.copy(
                        statusMessage = "Failed to delete category."
                    )
                    return@launch
                }

                val hasRef = cloudRepository.hasAnyDishReferencingCategoryId(
                    storeId = chatStoreId,
                    categoryId = catId
                )
                if (hasRef) {
                    uiState = uiState.copy(
                        adminCannotDeleteCategory = cat,
                        adminPendingDeleteCategory = null,
                        statusMessage = null
                    )
                    return@launch
                }

                val result = cloudRepository.deleteCategoryByName(
                    storeId = chatStoreId,
                    name = cat
                )

                if (result.ok) {
                    val refreshed = tryLoadFromCloud()
                    val finalDishes = if (refreshed.isNotEmpty()) refreshed else uiState.dishes
                    val allCategoryNames = cloudRepository.fetchCategories(chatStoreId)
                        .map { it.name }
                        .filter { it.isNotBlank() }
                        .distinct()

                    uiState = uiState.copy(
                        dishes = finalDishes,
                        manualCategories = allCategoryNames,
                        selectedCategory = uiState.selectedCategory.takeUnless { it == cat },
                        statusMessage = null
                    )

                    saveDishesToStorage(context, finalDishes)
                    saveManualCategoriesToStorage(context, allCategoryNames)
                } else {
                    Log.e(
                        "ShowcaseViewModel",
                        "removeCategory: cloud failed code=${result.errorCode} body=${result.errorBody}"
                    )

                    if (handleMerchantAuthExpiredIfNeeded(
                            context = context,
                            code = result.errorCode,
                            body = result.errorBody
                        )
                    ) {
                        return@launch
                    }

                    uiState = uiState.copy(
                        statusMessage = result.errorMessage ?: "Failed to delete category."
                    )
                }
            } catch (e: Exception) {
                Log.e("ShowcaseViewModel", "removeCategory: failed", e)
                uiState = uiState.copy(
                    statusMessage = "Failed to delete category."
                )
            }
        }
    }
    // ✅ AdminCategories：点 Delete 时先让 VM 决策弹哪个弹窗（UI 不判断引用）
    fun requestDeleteCategory(category: String) {
        val cat = category.trim()
        if (cat.isBlank()) return

        // 本地快速判断：只要当前 dishes 里还有该分类，就先阻止删除并提示
        val isReferencedLocally = uiState.dishes.any { it.category.orEmpty() == cat }
        uiState = if (isReferencedLocally) {
            uiState.copy(
                adminCannotDeleteCategory = cat,
                adminPendingDeleteCategory = null
            )
        } else {
            uiState.copy(
                adminPendingDeleteCategory = cat,
                adminCannotDeleteCategory = null
            )
        }
    }

    fun dismissCategoryDeleteDialogs() {
        uiState = uiState.copy(
            adminPendingDeleteCategory = null,
            adminCannotDeleteCategory = null
        )
    }

    fun confirmPendingDeleteCategory(context: Context) {
        val cat = uiState.adminPendingDeleteCategory ?: return
        uiState = uiState.copy(adminPendingDeleteCategory = null)
        removeCategory(context, cat)
    }

}
