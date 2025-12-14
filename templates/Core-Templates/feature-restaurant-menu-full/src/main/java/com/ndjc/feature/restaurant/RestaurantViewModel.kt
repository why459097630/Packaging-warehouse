package com.ndjc.feature.restaurant

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID


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
    val isSoldOut: Boolean,
    val imageUri: Uri?
)

class RestaurantViewModel : ViewModel() {

    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 200   // ✅ 描述最大 200 字
        private const val MAX_IMAGE_LONG_EDGE = 1080     // ✅ 图片最长边限制
        private const val IMAGE_JPEG_QUALITY = 80        // ✅ 压缩质量（0~100）
    }

    // ✅ 图片压缩：长边不超过 1080，质量 80%，返回压缩后的本地 Uri
    private suspend fun compressImage(
        context: Context,
        sourceUri: Uri
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            // 1）只读尺寸
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext null

            // 2）根据最长边计算采样比例
            var inSampleSize = 1
            var halfWidth = srcWidth / 2
            var halfHeight = srcHeight / 2
            while (
                (halfWidth / inSampleSize) >= MAX_IMAGE_LONG_EDGE ||
                (halfHeight / inSampleSize) >= MAX_IMAGE_LONG_EDGE
            ) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val bitmap = resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return@withContext null

            // 3）输出到缓存目录
            val fileName = "ndjc_img_${System.currentTimeMillis()}.jpg"
            val outFile = java.io.File(context.cacheDir, fileName)

            java.io.FileOutputStream(outFile).use { out ->
                bitmap.compress(
                    android.graphics.Bitmap.CompressFormat.JPEG,
                    IMAGE_JPEG_QUALITY,
                    out
                )
            }

            bitmap.recycle()

            Uri.fromFile(outFile)
        } catch (e: Exception) {
            Log.e("RestaurantViewModel", "compressImage failed", e)
            null
        }
    }

    var uiState by mutableStateOf(RestaurantUiState())
        private set

    // 编辑页本地草稿（所有输入/图片先写这里，点保存时再写 dishes + 云端）
    var editDraft by mutableStateOf<EditDraftState?>(null)
        private set

    private var hasLoadedOnce = false

    private val cloudRepository = RestaurantCloudRepository()

    fun ensureLoaded(context: Context) {
        if (hasLoadedOnce) return
        hasLoadedOnce = true
        viewModelScope.launch { loadFromSources(context) }
    }

    fun refresh(context: Context) {
        viewModelScope.launch { loadFromSources(context) }
    }

    /**
     * 云端 + 本地 合并策略：
     * - 以云端 remote 为主
     * - 把本地 local 中 “id 非空且云端不存在同 id” 的菜追加进去
     *   （通常就是刚新增、云端还没同步成功的本地菜）
     */
    private fun mergeRemoteAndLocal(
        remote: List<DemoDish>,
        local: List<DemoDish>
    ): List<DemoDish> {
        if (remote.isEmpty()) return local
        if (local.isEmpty()) return remote

        val remoteIds = remote.map { it.id }.toHashSet()

        val extras = local.filter { dish ->
            dish.id.isNotBlank() && !remoteIds.contains(dish.id)
        }

        // remote 为主，extras 追加（避免把本地新增但云端未同步的菜冲掉）
        return remote + extras
    }

    /**
     * 优先云端，其次本地缓存，最后兜底（现在兜底列表为空，不再强塞 demo dish）。
     *
     * 关键改动：当云端有数据时，不再简单覆盖本地，
     * 而是调用 mergeRemoteAndLocal(remote, local) 做合并，
     * 避免刷新时把刚在本地新增但云端尚未同步的菜品冲掉。
     */
    private suspend fun loadFromSources(context: Context) {
        uiState = uiState.copy(
            isLoading = true,
            statusMessage = "Loading from cloud..."
        )

        // 1) 云端
        val fromCloud = tryLoadFromCloud()
        // 2) 本地缓存（包含你之前 Save 的新增菜品）
        val stored = loadDishesFromStorage(context)

        val effectiveList: List<DemoDish>
        val status: String

        effectiveList = when {
            fromCloud.isNotEmpty() -> {
                // 云端成功返回时，以云端为准，避免把“云端已删除”的本地缓存又追加回来
                status = "Loaded from cloud."
                fromCloud
            }
            stored.isNotEmpty() -> {
                status = "Loaded from local cache."
                stored
            }
            else -> {
                status = "No data."
                emptyList()
            }
        }

        uiState = uiState.copy(
            dishes = effectiveList,
            isLoading = false,
            statusMessage = status
        )

        // 把最终列表再写回本地，保持一致（尤其是 image_url 替换后的 http url）
        saveDishesToStorage(context, effectiveList)
    }


    /**
     * 从 Supabase 拉 categories + dishes：
     * 1) 菜品 → DemoDish
     * 2) 只存在于 categories 表、没有菜品引用的分类 → 写入 manualCategories
     */
    private suspend fun tryLoadFromCloud(): List<DemoDish> {
        return try {
            val categories = cloudRepository.fetchCategories()
            val dishes = cloudRepository.fetchDishes()

            val categoryMap = categories.associate { it.id to it.name }

            // 1) 菜品列表
            val dishList = dishes.map { cloud ->
                val categoryName = cloud.categoryId?.let { categoryMap[it] }.orEmpty()
                DemoDish(
                    id = cloud.id ?: "",
                    nameZh = cloud.nameZh,
                    nameEn = cloud.nameEn,
                    descriptionEn = cloud.descriptionEn ?: "",
                    category = categoryName,
                    originalPrice = cloud.price.toFloat(),
                    discountPrice = cloud.discountPrice?.toFloat(),
                    isRecommended = cloud.recommended,
                    isSoldOut = cloud.soldOut,
                    imageResId = null,
                    imageUri = cloud.imageUrl?.let { Uri.parse(it) }
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
                statusMessage = "Failed to load from cloud, using local data if available."
            )
            emptyList()
        }
    }

    fun onCategorySelected(category: String?) {
        uiState = uiState.copy(selectedCategory = category)
    }

    fun onDishSelected(dish: DemoDish?) {
        uiState = uiState.copy(selectedDish = dish)
    }

    fun onAdminFabClicked() {
        uiState =
            if (uiState.isAdminLoggedIn) {
                uiState.copy(screen = RestaurantScreen.Admin)
            } else {
                uiState.copy(
                    screen = RestaurantScreen.Login,
                    loginError = null
                )
            }
    }

    /**
     * 进入详情：
     * 现在的设计是：只要 selectedDish != null 就显示详情层，
     * 不再切换到单独的 Detail screen，也不再用 previousScreen。
     */
    fun openDetail(dish: DemoDish) {
        uiState = uiState.copy(
            selectedDish = dish,
            statusMessage = null
        )
    }

    /**
     * 从详情层返回：只需把 selectedDish 清空即可。
     */
    fun backFromDetail() {
        Log.d(
            "NDJC_DETAIL",
            "backFromDetail BEFORE, selectedDish=${uiState.selectedDish?.id}, screen=${uiState.screen}"
        )

        uiState = uiState.copy(
            selectedDish = null,
            statusMessage = "Back from detail pressed"
        )

        Log.d(
            "NDJC_DETAIL",
            "backFromDetail AFTER, selectedDish=${uiState.selectedDish?.id}, screen=${uiState.screen}"
        )
    }

    fun backToHome() {
        uiState = uiState.copy(
            screen = RestaurantScreen.Home,
            loginError = null
        )
    }

    fun tryAdminLogin(username: String, password: String) {
        val u = username.trim()
        val p = password.trim()
        if (u.equals("admin", ignoreCase = true) && p == "1234") {
            uiState = uiState.copy(
                isAdminLoggedIn = true,
                screen = RestaurantScreen.Admin,
                loginError = null
            )
        } else {
            uiState = uiState.copy(
                loginError = "Invalid username or password (hint: admin / 1234)"
            )
        }
    }

    /**
     * 打开“编辑已有菜品”的编辑页。
     * 新增菜品的入口用 openNewDishScreen()。
     */
    fun openEditScreen(dishId: String) {
        val dishes = uiState.dishes
        val index = dishes.indexOfFirst { it.id == dishId }
        val targetDish = dishes.getOrNull(index) ?: return

        // 初始化编辑草稿
        editDraft = EditDraftState(
            isNew = false,
            index = index.takeIf { it >= 0 },
            original = targetDish,
            name = targetDish.nameZh.ifBlank { targetDish.nameEn },
            price = targetDish.originalPrice.toString(),
            discountPrice = targetDish.discountPrice?.toString() ?: "",
            description = targetDish.descriptionEn,
            category = targetDish.category.ifBlank { null },
            isRecommended = targetDish.isRecommended,
            isSoldOut = targetDish.isSoldOut,
            imageUri = targetDish.imageUri
        )

        uiState = uiState.copy(
            selectedDish = targetDish,
            screen = RestaurantScreen.Edit,
            statusMessage = null,
        )
    }

    /**
     * 打开“新增菜品”的编辑页：不依赖 selectedDish。
     * UI 层看到 selectedDish == null → 走 isNew = true 的分支。
     */
    fun openNewDishScreen() {
        editDraft = EditDraftState(
            isNew = true,
            index = null,
            original = null,
            name = "",
            price = "",
            discountPrice = "",
            description = "",
            category = null,
            isRecommended = false,
            isSoldOut = false,
            imageUri = null
        )

        uiState = uiState.copy(
            selectedDish = null,
            screen = RestaurantScreen.Edit,
            statusMessage = null
        )
    }

    /**
     * 从编辑页返回 Admin：只做页面切换，并清理当前编辑态（不保存）。
     */
    fun backToAdminFromEdit() {
        editDraft = null
        uiState = uiState.copy(
            screen = RestaurantScreen.Admin,
            selectedDish = null,
            statusMessage = null
        )
    }

    // --------------------
    // 编辑页：本地草稿更新函数
    // --------------------

    private fun updateEditDraft(transform: (EditDraftState) -> EditDraftState) {
        val current = editDraft ?: return
        editDraft = transform(current)
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

    fun onEditToggleSoldOut(checked: Boolean) {
        updateEditDraft { it.copy(isSoldOut = checked) }
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
            val compressed = compressImage(context, sourceUri)
            if (compressed == null) {
                uiState = uiState.copy(statusMessage = "Image compress failed.")
                return@launch
            }

            // 1) 写入编辑草稿
            updateEditDraft { it.copy(imageUri = compressed) }

            // 2) 让 UI 立刻能预览
            uiState = uiState.copy(statusMessage = "Image selected.")
        }
    }

    // ✅ 注意：这个函数必须放在 onEditImageSelected() 结束大括号之后（同级）
    fun onEditRemoveSelectedImage() {
        updateEditDraft { it.copy(imageUri = null) }
        uiState = uiState.copy(statusMessage = "Image removed.")
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
                statusMessage = "Name cannot be empty."
            )
            return
        }

        val price = draft.price.trim().toFloatOrNull()
        if (price == null || price <= 0f) {
            uiState = uiState.copy(
                statusMessage = "Invalid price."
            )
            return
        }

        val discountPrice = draft.discountPrice.trim().takeIf { it.isNotEmpty() }?.toFloatOrNull()
        if (draft.discountPrice.isNotEmpty() && discountPrice == null) {
            uiState = uiState.copy(
                statusMessage = "Invalid discount price."
            )
            return
        }

        val index = draft.index ?: -1

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
            isSoldOut = draft.isSoldOut,
            imageResId = null,
            imageUri = draft.imageUri
        )

        val updated = base.copy(
            nameZh = name,
            nameEn = name,
            descriptionEn = draft.description.take(MAX_DESCRIPTION_LENGTH),
            category = draft.category.orEmpty(),
            originalPrice = price,
            discountPrice = discountPrice,
            isRecommended = draft.isRecommended,
            isSoldOut = draft.isSoldOut,
            imageResId = null,
            imageUri = draft.imageUri
        )

        // ✅ 复用原有 updateDish：本地列表 + 本地存储 + 云端 upsert
        updateDish(context, index, updated)

        // ⚠️ 注意：updateDish 内已经修改了 uiState，这里再基于最新 uiState 做一次 copy
        uiState = uiState.copy(
            screen = RestaurantScreen.Admin,
            selectedDish = null
        )

        editDraft = null
    }

    fun updateDish(context: Context, index: Int, updated: DemoDish) {
        // ✅ 统一把描述裁剪到 MAX_DESCRIPTION_LENGTH 以内
        val normalized = updated.copy(
            descriptionEn = updated.descriptionEn.take(MAX_DESCRIPTION_LENGTH)
        )

        val current = uiState.dishes.toMutableList()
        if (index in current.indices) {
            current[index] = normalized
        } else {
            current.add(normalized)
        }

        // 先本地落盘（哪怕云端失败也不丢）
        uiState = uiState.copy(
            dishes = current,
            statusMessage = "Saved locally (cloud sync later)."
        )
        saveDishesToStorage(context, current)

        // 云端 upsert（包含：如有本地图片，则先上传拿 publicUrl）
        viewModelScope.launch {
            try {
                var imageUrlToSave: String? = normalized.imageUri?.toString()

                // 1) 若是本地 URI（content/file），先上传图片拿 publicUrl
                val localUri = normalized.imageUri
                if (localUri != null && isLocalImageUri(imageUrlToSave)) {
                    val uploadedPublicUrl = uploadDishImageIfNeeded(
                        context = context,
                        uri = localUri,
                    )

                    if (!uploadedPublicUrl.isNullOrBlank()) {
                        imageUrlToSave = uploadedPublicUrl

                        // 2) 把内存态 + 本地存储里的 imageUri 也替换为 publicUrl（保证重启后还能显示）
                        val refreshed = uiState.dishes.toMutableList()
                        val idx = refreshed.indexOfFirst { it.id == normalized.id }
                        if (idx >= 0) {
                            refreshed[idx] = refreshed[idx].copy(imageUri = Uri.parse(uploadedPublicUrl))
                            uiState = uiState.copy(dishes = refreshed)
                            saveDishesToStorage(context, refreshed)

                            // 让编辑页预览也立即从本地 file:// 切到云端 https://
                            editDraft = editDraft?.copy(imageUri = Uri.parse(uploadedPublicUrl))
                        }
                    }
                }

                // 3) 最终写入 dishes 表：image_url 必须是可公开访问的 URL（或 null）
                val ok = cloudRepository.upsertDishFromDemo(
                    id = normalized.id,
                    nameZh = normalized.nameZh,
                    nameEn = normalized.nameEn,
                    descriptionEn = normalized.descriptionEn,
                    category = normalized.category,
                    originalPrice = normalized.originalPrice.toInt(),
                    discountPrice = normalized.discountPrice?.toInt(),
                    isRecommended = normalized.isRecommended,
                    isSoldOut = normalized.isSoldOut,
                    imageUri = imageUrlToSave
                )

                uiState = uiState.copy(
                    statusMessage = if (ok) "Saved to cloud." else "Cloud sync failed, changes are saved locally."
                )
            } catch (_: Exception) {
                uiState = uiState.copy(
                    statusMessage = "Cloud sync failed, changes are saved locally."
                )
            }
        }
    }

    private fun isLocalImageUri(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return uriString.startsWith("content://") || uriString.startsWith("file://")
    }

    private suspend fun uploadDishImageIfNeeded(
        context: Context,
        uri: Uri
    ): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bytes =
                resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: runCatching {
                        val p = uri.path ?: return@runCatching null
                        java.io.File(p).readBytes()
                    }.getOrNull()
                    ?: return@withContext null
            cloudRepository.uploadDishImageBytes(
                bytes = bytes,
                fileExt = "jpg",
                contentType = "image/jpeg"
            )
        } catch (_: Exception) {
            null
        }
    }


    /**
     * 从编辑页选择了一张图片（老逻辑）：
     * 目前还没通过 UI 包接入，可保留兼容；
     * 真正编辑页用的是 onEditImageSelected()。
     */
    fun setDishImageFromPicker(
        context: Context,
        index: Int,
        sourceUri: Uri
    ) {
        viewModelScope.launch {
            val compressed = compressImage(context, sourceUri)
            if (compressed == null) {
                uiState = uiState.copy(
                    statusMessage = "Image compress failed."
                )
                return@launch
            }

            val current = uiState.dishes.toMutableList()
            if (index !in current.indices) {
                uiState = uiState.copy(
                    statusMessage = "Dish index out of range."
                )
                return@launch
            }

            val dish = current[index]

            // ✅ 这里先只更新单张封面图，未来扩展为 gallery 时在这里改为 list 逻辑
            val updated = dish.copy(
                imageUri = compressed
            )

            // 复用现有 updateDish 逻辑（含本地保存 + 云端 upsert）
            updateDish(context, index, updated)
        }
    }

    fun requestDeleteDish(index: Int) {
        uiState = uiState.copy(pendingDeleteIndex = index)
    }

    fun deleteDish(context: Context, index: Int) {
        val current = uiState.dishes.toMutableList()
        if (index in current.indices) {
            val dish = current[index]
            current.removeAt(index)
            uiState = uiState.copy(
                dishes = current,
                statusMessage = "Dish deleted locally.",
                pendingDeleteIndex = null
            )
            saveDishesToStorage(context, current)

            viewModelScope.launch {
                try {
                    if (dish.id.isNotBlank()) {
                        cloudRepository.deleteDishById(dish.id)
                    }
                } catch (_: Exception) {
                    uiState = uiState.copy(
                        statusMessage = "Dish deleted locally, but cloud delete failed."
                    )
                }
            }
        } else {
            uiState = uiState.copy(pendingDeleteIndex = null)
        }
    }

    /**
     * 新增分类：
     * 1) 本地加到 manualCategories
     * 2) 异步确保 Supabase categories 表中存在这一条
     */
    fun addCategory(name: String) {
        val cat = name.trim()
        if (cat.isBlank()) return

        val existing = deriveCategories(uiState.dishes, uiState.manualCategories)
        if (existing.contains(cat)) return

        // 1) 本地 UI 更新
        uiState = uiState.copy(
            manualCategories = uiState.manualCategories + cat,
            statusMessage = "Category \"$cat\" added."
        )

        // 2) 云端确保存在该分类
        viewModelScope.launch {
            try {
                cloudRepository.ensureCategoryExists(cat)
            } catch (_: Exception) {
                uiState = uiState.copy(
                    statusMessage = "Category \"$cat\" added locally, but cloud sync failed."
                )
            }
        }
    }

    /**
     * 删除分类：
     * 1) 本地：清空菜品中的 category + 移除 manualCategories
     * 2) 云端：删除 categories 表记录 + 把受影响菜品回写 Supabase
     */
    fun removeCategory(context: Context, category: String) {
        val cat = category.trim()
        if (cat.isBlank()) return

        // ✅ 先做“云端引用检查”：只要还有 dish.category_id 引用该分类，就不允许删除
        uiState = uiState.copy(statusMessage = "Checking category references...")

        viewModelScope.launch {
            try {
                val catId = cloudRepository.getCategoryIdByName(cat)

                // 找不到云端分类：视为“已不存在”，直接做本地移除即可
                if (catId.isNullOrBlank()) {
                    // 本地移除（不触发云端 delete）
                    val updatedManual = uiState.manualCategories.filterNot { it == cat }
                    val newSelected = uiState.selectedCategory.takeUnless { it == cat }

                    uiState = uiState.copy(
                        manualCategories = updatedManual,
                        selectedCategory = newSelected,
                        statusMessage = "Category \"$cat\" removed (not found in cloud)."
                    )
                    return@launch
                }

                val hasRef = cloudRepository.hasAnyDishReferencingCategoryId(catId)
                if (hasRef) {
                    // ✅ 你期望的策略：有菜品引用则禁止删除
                    uiState = uiState.copy(
                        statusMessage = "Cannot delete category \"$cat\": there are dishes under this category. Move them to another category first."
                    )
                    return@launch
                }

                // 走到这里说明：云端没有任何 dish 引用该 category_id，可以安全删除

                // 1) 本地：移除 manualCategories + 选中项（不需要改 dishes，因为既然无引用，dishes 里也不会显示这个分类）
                val updatedManual = uiState.manualCategories.filterNot { it == cat }
                val newSelected = uiState.selectedCategory.takeUnless { it == cat }

                uiState = uiState.copy(
                    manualCategories = updatedManual,
                    selectedCategory = newSelected,
                    statusMessage = "Deleting category \"$cat\"..."
                )

                // 2) 云端：删除 categories 记录（按 name 找 id 再删）
                val ok = cloudRepository.deleteCategoryByName(cat)

                uiState = uiState.copy(
                    statusMessage = if (ok) "Category \"$cat\" deleted." else "Category deleted locally, but cloud delete failed."
                )
            } catch (e: Exception) {
                Log.e("RestaurantViewModel", "removeCategory: failed", e)
                uiState = uiState.copy(
                    statusMessage = "Delete failed: ${e.message ?: "unknown error"}"
                )
            }
        }
    }
}