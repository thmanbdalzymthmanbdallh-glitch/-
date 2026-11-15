package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CurrencyOption
import com.example.data.model.ImagePreset
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.compressImageUriToBase64
import com.example.ui.components.presetIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductEditorScreen(
    existingProduct: Product?,
    defaultSellerName: String,
    defaultCurrency: CurrencyOption,
    isBusy: Boolean,
    onNavigateBack: () -> Unit,
    onSaveProduct: (
        editingProductId: String?,
        sellerName: String,
        title: String,
        description: String,
        category: String,
        price: Double,
        currency: String,
        stock: Long,
        isPublished: Boolean,
        imagePreset: String,
        imageData: String?,
        additionalImages: List<String>
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by rememberSaveable(existingProduct?.id) { mutableStateOf(existingProduct?.title ?: "") }
    var description by rememberSaveable(existingProduct?.id) { mutableStateOf(existingProduct?.description ?: "") }
    var priceText by rememberSaveable(existingProduct?.id) {
        mutableStateOf(existingProduct?.price?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var stockText by rememberSaveable(existingProduct?.id) {
        mutableStateOf(existingProduct?.stock?.toString() ?: "10")
    }
    var selectedCategory by rememberSaveable(existingProduct?.id) {
        mutableStateOf(existingProduct?.category ?: ProductCategory.ELECTRONICS.key)
    }
    var selectedCurrency by rememberSaveable(existingProduct?.id) {
        mutableStateOf(existingProduct?.currency ?: defaultCurrency.code)
    }
    var isPublished by rememberSaveable(existingProduct?.id) {
        mutableStateOf(existingProduct?.isPublished ?: true)
    }
    var imagePreset by rememberSaveable(existingProduct?.id) {
        mutableStateOf(existingProduct?.imagePreset ?: ImagePreset.ELECTRONICS.key)
    }
    var imageData by remember(existingProduct?.id) { mutableStateOf(existingProduct?.imageData) }
    var additionalImages by remember(existingProduct?.id) {
        mutableStateOf(existingProduct?.additionalImages.orEmpty())
    }
    var isCompressingPhoto by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isCompressingPhoto = true
                val base64 = compressImageUriToBase64(context, uri)
                isCompressingPhoto = false
                if (base64 != null) {
                    imageData = base64
                } else {
                    formError = "تعذر معالجة الصورة المختارة"
                }
            }
        }
    }

    val extraPhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isCompressingPhoto = true
                val remaining = (4 - additionalImages.size).coerceAtLeast(0)
                val encoded = uris.take(remaining).mapNotNull { uri ->
                    compressImageUriToBase64(context, uri)
                }
                isCompressingPhoto = false
                if (encoded.isNotEmpty()) {
                    additionalImages = (additionalImages + encoded).take(4)
                } else if (remaining > 0) {
                    formError = "تعذر معالجة الصور الإضافية المختارة"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingProduct == null) "إضافة منتج جديد" else "تعديل المنتج",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Visual Preview & Photo Picker
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ProductThumbnail(
                                imageData = imageData,
                                imagePreset = imagePreset,
                                contentDescription = "معاينة صورة المنتج",
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "صورة المنتج",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "اختر صورة من جهازك أو اعتمد أيقونة الفئة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = !isCompressingPhoto,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("pick_product_photo_button")
                                    ) {
                                        if (isCompressingPhoto) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.AddPhotoAlternate,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("اختر صورة")
                                        }
                                    }
                                    if (!imageData.isNullOrBlank()) {
                                        IconButton(onClick = { imageData = null }) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "إزالة الصورة",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (imageData.isNullOrBlank()) {
                            Text(
                                text = "النمط البصري الجاهز:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ImagePreset.entries.forEach { preset ->
                                    val selected = imagePreset == preset.key
                                    FilterChip(
                                        selected = selected,
                                        onClick = { imagePreset = preset.key },
                                        label = { Text(preset.labelAr) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = presetIcon(preset.key),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Additional Gallery Images (up to 4)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "صور إضافية (${additionalImages.size}/4)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                if (additionalImages.size < 4) {
                                    OutlinedButton(
                                        onClick = {
                                            extraPhotosLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = !isCompressingPhoto,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 40.dp)
                                            .testTag("pick_additional_photos_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("إضافة صور", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            if (additionalImages.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(additionalImages) { index, extraImg ->
                                        Box(modifier = Modifier.size(76.dp)) {
                                            ProductThumbnail(
                                                imageData = extraImg,
                                                imagePreset = imagePreset,
                                                contentDescription = "صورة إضافية ${index + 1}",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                            Surface(
                                                onClick = {
                                                    additionalImages = additionalImages.filterIndexed { i, _ -> i != index }
                                                },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = "حذف الصورة الإضافية",
                                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                                        modifier = Modifier.size(14.dp)
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

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100); formError = null },
                    label = { Text("اسم المنتج *") },
                    placeholder = { Text("مثال: ساعة ذكية رياضية") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(600) },
                    label = { Text("وصف المنتج") },
                    placeholder = { Text("اذكر المميزات والمقاس أو الضمان...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_description_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) {
                                priceText = input.take(10)
                                formError = null
                            }
                        },
                        label = { Text("السعر *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_price_input")
                    )

                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                stockText = input.take(6)
                                formError = null
                            }
                        },
                        label = { Text("الكمية المتاحة *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_stock_input")
                    )
                }

                // Category selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "التصنيف",
                        style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProductCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.key,
                                onClick = { selectedCategory = cat.key },
                                label = { Text(cat.labelAr) },
                                leadingIcon = if (selectedCategory == cat.key) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Currency selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "العملة",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyOption.entries.forEach { curr ->
                            FilterChip(
                                selected = selectedCurrency == curr.code,
                                onClick = { selectedCurrency = curr.code },
                                label = { Text("${curr.labelAr} (${curr.symbolAr})") }
                            )
                        }
                    }
                }

                // Publish Switch
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPublished = !isPublished }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "عرض المنتج في السوق",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isPublished) "متاح الآن للعملاء للشراء" else "محفوظ كمسودة في متجرك فقط",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPublished,
                            onCheckedChange = { isPublished = it }
                        )
                    }
                }

                if (!formError.isNullOrBlank()) {
                    Text(
                        text = formError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        val trimmedTitle = title.trim()
                        val parsedPrice = priceText.toDoubleOrNull()
                        val parsedStock = stockText.toLongOrNull()

                        when {
                            trimmedTitle.length < 2 -> formError = "يرجى إدخال اسم منتج من حرفين على الأقل"
                            parsedPrice == null || parsedPrice <= 0.0 -> formError = "يرجى إدخال سعر صحيح أكبر من صفر"
                            parsedStock == null || parsedStock < 0L -> formError = "يرجى إدخال كمية مخزون صحيحة"
                            else -> onSaveProduct(
                                existingProduct?.id,
                                defaultSellerName,
                                trimmedTitle,
                                description.trim(),
                                selectedCategory,
                                parsedPrice,
                                selectedCurrency,
                                parsedStock,
                                isPublished,
                                imagePreset,
                                imageData,
                                additionalImages
                            )
                        }
                    },
                    enabled = !isBusy && !isCompressingPhoto,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_product_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (existingProduct == null) "نشر وحفظ المنتج" else "حفظ التعديلات",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
