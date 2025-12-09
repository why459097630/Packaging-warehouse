package com.ndjc.feature.restaurant

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeContent(
    dishes: List<DemoDish>,
    allCategories: List<String>,
    currentCategory: String?,
    isLoading: Boolean,
    statusMessage: String?,
    onCategorySelected: (String?) -> Unit,
    onSelectDish: (DemoDish?) -> Unit,
    onRefresh: () -> Unit
) {
    val refreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = onRefresh
    )

    var searchText by remember { mutableStateOf("") }

    val visibleCount = remember(dishes, searchText) {
        dishes.count { dish ->
            searchText.isBlank() ||
                    dish.nameEn.contains(searchText, ignoreCase = true) ||
                    dish.descriptionEn.contains(searchText, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(refreshState)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (statusMessage != null) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            // 首页搜索框
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search dishes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // 分类横向列表
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                FilterChip(
                    selected = currentCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                allCategories.forEach { cat ->
                    FilterChip(
                        selected = currentCategory == cat,
                        onClick = { onCategorySelected(cat) },
                        label = { Text(cat) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            if (!isLoading && visibleCount == 0) {
                // 空状态提示（无菜品 / 搜索结果为空）
                Text(
                    text = "No dishes to display. Please contact staff or try a different search.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    itemsIndexed(dishes) { _, dish ->
                        val matches = searchText.isBlank() ||
                                dish.nameEn.contains(searchText, ignoreCase = true) ||
                                dish.descriptionEn.contains(searchText, ignoreCase = true)

                        if (matches) {
                            DishItemCard(
                                dish = dish,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = { onSelectDish(dish) }
                            )
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isLoading,
            state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun AdminLoginContent(
    errorMessage: String?,
    onLogin: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Admin Login", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Back")
            }
            Button(onClick = { onLogin(username, password) }) {
                Text("Login")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelContent(
    dishes: List<DemoDish>,
    allCategories: List<String>,
    statusMessage: String?,
    onBackToHome: () -> Unit,
    onUpdateDish: (Int, DemoDish) -> Unit,
    onDeleteDish: (Int) -> Unit,
    onConfirmDeleteDish: (Int) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingDish by remember { mutableStateOf<DemoDish?>(null) }
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }

    var searchText by remember { mutableStateOf("") }

    val visibleCount = remember(dishes, searchText) {
        dishes.count { dish ->
            searchText.isBlank() ||
                    dish.nameEn.contains(searchText, ignoreCase = true) ||
                    dish.descriptionEn.contains(searchText, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBackToHome) {
                Text("Back to home")
            }
        }

        if (statusMessage != null) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }

        CategoryManagementSection(
            allCategories = allCategories,
            onAddCategory = onAddCategory,
            onDeleteCategory = onDeleteCategory
        )

        // 后台搜索框
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search dishes (admin)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // 新增菜品按钮：显示在分类下面、列表上面
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    editingIndex = -1
                    editingDish = DemoDish(
                        id = "",
                        nameZh = "",
                        nameEn = "",
                        descriptionEn = "",
                        category = allCategories.firstOrNull().orEmpty(),
                        originalPrice = 0f,
                        discountPrice = null,
                        isRecommended = false,
                        isSoldOut = false,
                        imageResId = null,
                        imageUri = null
                    )
                }
            ) {
                Text("Add dish")
            }
        }

        if (visibleCount == 0) {
            Text(
                text = "No dishes found in admin list.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(dishes) { index, dish ->
                val matches = searchText.isBlank() ||
                        dish.nameEn.contains(searchText, ignoreCase = true) ||
                        dish.descriptionEn.contains(searchText, ignoreCase = true)

                if (matches) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DishItemCard(
                            dish = dish,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            onClick = {
                                editingIndex = index
                                editingDish = dish
                            }
                        )
                        IconButton(onClick = {
                            pendingDeleteIndex = index
                            onDeleteDish(index) // 保留原有逻辑
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "delete"
                            )
                        }
                    }
                }
            }
        }
    }

    val pendingIndex = editingIndex
    val pendingDish = editingDish
    if (pendingIndex != null && pendingDish != null) {
        EditDishDialog(
            original = pendingDish,
            availableCategories = allCategories,
            onDismiss = {
                editingIndex = null
                editingDish = null
            },
            onSave = { updated ->
                onUpdateDish(pendingIndex, updated)
                editingIndex = null
                editingDish = null
            }
        )
    }

    val indexToDelete = pendingDeleteIndex
    if (indexToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteIndex = null },
            title = { Text("Delete dish") },
            text = { Text("Are you sure you want to delete this dish?") },
            confirmButton = {
                TextButton(onClick = {
                    onConfirmDeleteDish(indexToDelete)
                    pendingDeleteIndex = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryManagementSection(
    allCategories: List<String>,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
) {
    var newCategoryName by remember { mutableStateOf("") }
    var pendingDeleteCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("New category") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val trimmed = newCategoryName.trim()
                if (trimmed.isNotEmpty()) {
                    onAddCategory(trimmed)
                    newCategoryName = ""
                }
            }) {
                Text("Add")
            }
        }

        // 简单版 FlowRow
        Row {
            allCategories.forEach { cat ->
                AssistChip(
                    onClick = { /* 这里只做展示 */ },
                    label = { Text(cat) },
                    trailingIcon = {
                        Text(
                            text = "x",
                            modifier = Modifier.clickable {
                                pendingDeleteCategory = cat
                            },
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                )
            }
        }
    }

    val catToDelete = pendingDeleteCategory
    if (catToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            title = { Text("Delete category") },
            text = {
                Text(
                    "Delete category \"$catToDelete\"?\n\n" +
                            "All dishes in this category will have their category cleared."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(catToDelete)
                    pendingDeleteCategory = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DishDetailDialog(
    dish: DemoDish,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dish.nameEn) }, // 标题使用 EN
        text = {
            Column {
                if (dish.imageUri != null || dish.imageResId != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        DishImageBox(
                            imageUri = dish.imageUri,
                            imageResId = dish.imageResId,
                            contentDescription = dish.nameEn,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(text = dish.nameEn, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = dish.descriptionEn)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Category: ${dish.category}")
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Price: ${dish.originalPrice}",
                        textDecoration = if (dish.discountPrice != null) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
                    )
                    if (dish.discountPrice != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Now: ${dish.discountPrice}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (dish.isRecommended) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recommended",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (dish.isSoldOut) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SOLD OUT",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DishImageBox(
    imageUri: Uri?,
    imageResId: Int?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (imageUri != null) {
        var imageBitmap by remember(imageUri) { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(imageUri) {
            imageBitmap = withContext(Dispatchers.IO) {
                try {
                    when (imageUri.scheme) {
                        "content", "file" -> {
                            context.contentResolver.openInputStream(imageUri)?.use { input ->
                                BitmapFactory.decodeStream(input)?.asImageBitmap()
                            }
                        }

                        "http", "https" -> {
                            URL(imageUri.toString()).openStream().use { input ->
                                BitmapFactory.decodeStream(input)?.asImageBitmap()
                            }
                        }

                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else if (imageResId != null) {
            Image(
                painter = painterResource(imageResId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
    } else if (imageResId != null) {
        Image(
            painter = painterResource(imageResId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun DishItemCard(
    dish: DemoDish,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 8.dp)
            ) {
                DishImageBox(
                    imageUri = dish.imageUri,
                    imageResId = dish.imageResId,
                    contentDescription = dish.nameEn,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // 标题使用 EN
                Text(text = dish.nameEn, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = dish.descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
                Text(
                    text = dish.category,
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "¥${dish.originalPrice}",
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (dish.discountPrice != null) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
                    )
                    if (dish.discountPrice != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "¥${dish.discountPrice}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row {
                    if (dish.isRecommended) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Recommended") },
                            modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                        )
                    }
                    if (dish.isSoldOut) {
                        Text(
                            text = "SOLD OUT",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditDishDialog(
    original: DemoDish,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (DemoDish) -> Unit
) {
    var nameZh by remember { mutableStateOf(original.nameZh) } // 逻辑保留，不显示在 UI
    var nameEn by remember { mutableStateOf(original.nameEn) }
    var desc by remember { mutableStateOf(original.descriptionEn) }
    var category by remember { mutableStateOf(original.category) }
    var priceText by remember { mutableStateOf(original.originalPrice.toString()) }
    var discountText by remember { mutableStateOf(original.discountPrice?.toString().orEmpty()) }
    var isRecommended by remember { mutableStateOf(original.isRecommended) }
    var isSoldOut by remember { mutableStateOf(original.isSoldOut) }

    var imageUriText by remember { mutableStateOf(original.imageUri?.toString().orEmpty()) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(original.imageUri) }
    var formError by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                pickedImageUri = uri
                imageUriText = uri.toString()
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit dish") },
        text = {
            Column {

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Name (EN)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 分类快捷选择
                if (availableCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        availableCategories.forEach { cat ->
                            AssistChip(
                                onClick = { category = cat },
                                label = { Text(cat) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    label = { Text("Discount price") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = imageUriText,
                    onValueChange = { imageUriText = it },
                    label = { Text("Image URL (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                ) {
                    Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Pick from gallery")
                    }
                    if (pickedImageUri != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Image selected",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRecommended,
                        onCheckedChange = { isRecommended = it }
                    )
                    Text("Recommended")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSoldOut,
                        onCheckedChange = { isSoldOut = it }
                    )
                    Text("Sold out")
                }

                if (formError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {

                val price = priceText.toFloatOrNull()
                if (price == null || price <= 0f) {
                    formError = "Price must be a positive number"
                    return@TextButton
                }
                val discount = discountText.takeIf { it.isNotBlank() }?.toFloatOrNull()

                if (discount != null && (discount <= 0f || discount >= price)) {
                    formError = "Discount price must be > 0 and less than price"
                    return@TextButton
                }

                val finalImageUri = when {
                    imageUriText.isBlank() -> null
                    pickedImageUri != null -> pickedImageUri
                    else -> try {
                        Uri.parse(imageUriText)
                    } catch (_: Exception) {
                        null
                    }
                }

                formError = null

                onSave(
                    original.copy(
                        nameZh = nameZh, // 逻辑上仍保存，避免破坏云端结构
                        nameEn = nameEn,
                        descriptionEn = desc,
                        category = category,
                        originalPrice = price,
                        discountPrice = discount,
                        isRecommended = isRecommended,
                        isSoldOut = isSoldOut,
                        imageUri = finalImageUri
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
