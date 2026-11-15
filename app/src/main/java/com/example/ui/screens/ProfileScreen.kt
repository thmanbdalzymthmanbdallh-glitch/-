package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CurrencyOption
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OrderHistorySearchBar
import com.example.ui.components.OrderProgressTracker
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.filterOrdersByQuery
import com.example.ui.components.formatOrderShortRef
import com.example.ui.components.formatPrice
import com.example.ui.components.shareOrderSummary
import com.example.ui.viewmodel.UiState

@Composable
fun ProfileScreen(
    userEmail: String?,
    profileState: UiState<UserProfile?>,
    buyerOrdersState: UiState<List<Order>>,
    isBusy: Boolean,
    onSaveProfile: (
        displayName: String,
        storeName: String,
        storeBio: String,
        phone: String,
        defaultAddress: String,
        currency: CurrencyOption,
        shippingAddresses: List<String>
    ) -> Unit,
    onUpdateShippingAddresses: (
        defaultAddress: String,
        shippingAddresses: List<String>
    ) -> Unit,
    onCancelOrder: (String) -> Unit,
    onExploreMarket: () -> Unit,
    onSignOut: () -> Unit,
    notificationsEnabled: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    wishlistCount: Int = 0,
    onOpenWishlist: () -> Unit = {},
    reorderingOrderIds: Set<String> = emptySet(),
    onReorder: (Order) -> Unit = {}
) {
    val profile = (profileState as? UiState.Success)?.data
    val orders = (buyerOrdersState as? UiState.Success)?.data.orEmpty()

    val savedAddresses = remember(profile?.defaultAddress, profile?.shippingAddresses) {
        buildList {
            val defaultAddr = profile?.defaultAddress?.trim().orEmpty()
            if (defaultAddr.length >= 4) add(defaultAddr)
            profile?.shippingAddresses.orEmpty()
                .map { it.trim() }
                .filter { it.length >= 4 && it !in this }
                .forEach { add(it) }
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Header Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.displayName?.ifBlank { "عميل متجري" } ?: "عميل متجري",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${orders.size} طلب",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${savedAddresses.size} عنوان",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onOpenWishlist,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("profile_open_wishlist_button")
                ) {
                    Icon(
                        imageVector = if (wishlistCount > 0) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "قائمة المفضلة",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("المفضلة ($wishlistCount)")
                }
            }
        }

        // Section Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("بياناتي", maxLines = 1) },
                modifier = Modifier.testTag("profile_tab_contact")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("عناويني", maxLines = 1) },
                modifier = Modifier.testTag("profile_tab_addresses")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("سجل الطلبات", maxLines = 1) },
                modifier = Modifier.testTag("profile_tab_orders")
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> PersonalContactTab(
                    userEmail = userEmail,
                    profile = profile,
                    savedAddresses = savedAddresses,
                    isBusy = isBusy,
                    notificationsEnabled = notificationsEnabled,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onSaveProfile = onSaveProfile,
                    onSignOut = onSignOut
                )
                1 -> ShippingAddressesTab(
                    defaultAddress = profile?.defaultAddress ?: "",
                    savedAddresses = savedAddresses,
                    isBusy = isBusy,
                    onUpdateShippingAddresses = onUpdateShippingAddresses
                )
                2 -> OrderHistoryTab(
                    buyerOrdersState = buyerOrdersState,
                    reorderingOrderIds = reorderingOrderIds,
                    onReorder = onReorder,
                    onCancelOrder = onCancelOrder,
                    onExploreMarket = onExploreMarket
                )
            }
        }
    }
}

@Composable
private fun PersonalContactTab(
    userEmail: String?,
    profile: UserProfile?,
    savedAddresses: List<String>,
    isBusy: Boolean,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSaveProfile: (
        displayName: String,
        storeName: String,
        storeBio: String,
        phone: String,
        defaultAddress: String,
        currency: CurrencyOption,
        shippingAddresses: List<String>
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(profile?.id, profile?.displayName) {
        mutableStateOf(profile?.displayName ?: "")
    }
    var phone by remember(profile?.id, profile?.phone) {
        mutableStateOf(profile?.phone ?: "")
    }
    var defaultAddress by remember(profile?.id, profile?.defaultAddress) {
        mutableStateOf(profile?.defaultAddress ?: "")
    }
    var storeName by remember(profile?.id, profile?.storeName) {
        mutableStateOf(profile?.storeName ?: "")
    }
    var storeBio by remember(profile?.id, profile?.storeBio) {
        mutableStateOf(profile?.storeBio ?: "")
    }
    var currency by remember(profile?.id, profile?.currency) {
        mutableStateOf(CurrencyOption.fromCode(profile?.currency ?: CurrencyOption.SAR.code))
    }
    var contactError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Personal Contact Information Section
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "معلومات التواصل الشخصية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!userEmail.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "البريد الإلكتروني",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(80); contactError = null },
                    label = { Text("الاسم الكامل *") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_display_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(30); contactError = null },
                    label = { Text("رقم الجوال للتواصل والتوصيل") },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_phone_input")
                )

                OutlinedTextField(
                    value = defaultAddress,
                    onValueChange = { defaultAddress = it.take(250) },
                    label = { Text("العنوان الأساسي للشحن") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_default_address_input")
                )
            }
        }

        // Seller Store Preferences Section
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "إعدادات المتجر والعملة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it.take(80) },
                    label = { Text("اسم المتجر") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_store_name_input")
                )

                OutlinedTextField(
                    value = storeBio,
                    onValueChange = { storeBio = it.take(300) },
                    label = { Text("نبذة عن المتجر") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_store_bio_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "العملة الافتراضية",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyOption.entries.forEach { curr ->
                            FilterChip(
                                selected = currency == curr,
                                onClick = { currency = curr },
                                label = { Text("${curr.labelAr} (${curr.symbolAr})") }
                            )
                        }
                    }
                }
            }
        }

        // Order Status Notifications Card (FCM)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_notifications_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (notificationsEnabled) {
                            Icons.Filled.NotificationsActive
                        } else {
                            Icons.Filled.NotificationsOff
                        },
                        contentDescription = null,
                        tint = if (notificationsEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إشعارات حالة الطلبات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (notificationsEnabled) {
                                "مفعّلة — ستصلك تنبيهات فورية عند تأكيد الطلب أو شحنه أو تسليمه."
                            } else {
                                "الإشعارات غير مفعّلة — فعّلها لتصلك تنبيهات شحن وتسليم طلباتك."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!notificationsEnabled) {
                    OutlinedButton(
                        onClick = onRequestNotificationPermission,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("enable_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("تفعيل إشعارات الطلبات")
                    }
                }
            }
        }

        if (!contactError.isNullOrBlank()) {
            Text(
                text = contactError!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                if (displayName.trim().isEmpty()) {
                    contactError = "يرجى إدخال الاسم الكامل"
                } else {
                    contactError = null
                    onSaveProfile(
                        displayName,
                        storeName,
                        storeBio,
                        phone,
                        defaultAddress,
                        currency,
                        savedAddresses
                    )
                }
            },
            enabled = !isBusy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_profile_button")
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("حفظ التغييرات")
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("sign_out_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("تسجيل الخروج")
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ShippingAddressesTab(
    defaultAddress: String,
    savedAddresses: List<String>,
    isBusy: Boolean,
    onUpdateShippingAddresses: (defaultAddress: String, shippingAddresses: List<String>) -> Unit
) {
    var showAddressDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var draftAddress by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "عناوين الشحن المحفوظة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "أضف عناوينك وحدد الافتراضي للطلب السريع",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    editingIndex = null
                    draftAddress = ""
                    dialogError = null
                    showAddressDialog = true
                },
                enabled = !isBusy && savedAddresses.size < 8,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("add_shipping_address_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddLocationAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("إضافة عنوان")
            }
        }

        if (savedAddresses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    title = "لا توجد عناوين شحن محفوظة",
                    subtitle = "أضف عنوان منزلك أو عملك لتسهيل إتمام الطلبات القادمة.",
                    actionLabel = "إضافة أول عنوان",
                    onAction = {
                        editingIndex = null
                        draftAddress = ""
                        dialogError = null
                        showAddressDialog = true
                    }
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("shipping_addresses_list")
            ) {
                items(savedAddresses.indices.toList(), key = { idx -> "${idx}_${savedAddresses[idx]}" }) { index ->
                    val address = savedAddresses[index]
                    val isDefault = address == defaultAddress.trim()

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDefault)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isDefault) 1.5.dp else 1.dp,
                            color = if (isDefault)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shipping_address_card_$index")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isDefault) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "العنوان الافتراضي",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = address,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isDefault) {
                                    TextButton(
                                        onClick = {
                                            onUpdateShippingAddresses(address, savedAddresses)
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("تعيين كافتراضي")
                                    }
                                } else {
                                    Spacer(Modifier.width(1.dp))
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingIndex = index
                                            draftAddress = address
                                            dialogError = null
                                            showAddressDialog = true
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "تعديل العنوان",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val updatedList = savedAddresses.filterIndexed { i, _ -> i != index }
                                            val nextDefault = if (isDefault) {
                                                updatedList.firstOrNull() ?: ""
                                            } else {
                                                defaultAddress
                                            }
                                            onUpdateShippingAddresses(nextDefault, updatedList)
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "حذف العنوان",
                                            tint = MaterialTheme.colorScheme.error
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

    if (showAddressDialog) {
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = {
                Text(if (editingIndex == null) "إضافة عنوان شحن جديد" else "تعديل عنوان الشحن")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draftAddress,
                        onValueChange = { draftAddress = it.take(250); dialogError = null },
                        label = { Text("العنوان بالتفصيل (المدينة، الحي، الشارع) *") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shipping_address_dialog_input")
                    )
                    if (!dialogError.isNullOrBlank()) {
                        Text(
                            text = dialogError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = draftAddress.trim()
                        if (cleaned.length < 4) {
                            dialogError = "يرجى إدخال عنوان واضح من ٤ أحرف على الأقل"
                        } else {
                            val idx = editingIndex
                            val updatedList = if (idx == null) {
                                (savedAddresses + cleaned).distinct().take(8)
                            } else {
                                savedAddresses.mapIndexed { i, existing ->
                                    if (i == idx) cleaned else existing
                                }.distinct()
                            }
                            val nextDefault = when {
                                idx != null && savedAddresses.getOrNull(idx) == defaultAddress.trim() -> cleaned
                                defaultAddress.isBlank() -> cleaned
                                else -> defaultAddress
                            }
                            onUpdateShippingAddresses(nextDefault, updatedList)
                            showAddressDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_shipping_address_confirm_button")
                ) {
                    Text("حفظ العنوان")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun OrderHistoryTab(
    buyerOrdersState: UiState<List<Order>>,
    reorderingOrderIds: Set<String> = emptySet(),
    onReorder: (Order) -> Unit = {},
    onCancelOrder: (String) -> Unit,
    onExploreMarket: () -> Unit
) {
    when (buyerOrdersState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    title = "تعذر تحميل سجل الطلبات",
                    subtitle = buyerOrdersState.message
                )
            }
        }

        is UiState.Success -> {
            val orders = buyerOrdersState.data
            if (orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        title = "سجل طلباتك فارغ",
                        subtitle = "عند الشراء من السوق ستظهر طلباتك وحالتها هنا.",
                        actionLabel = "تصفح السوق",
                        onAction = onExploreMarket
                    )
                }
            } else {
                val context = LocalContext.current
                var searchQuery by rememberSaveable { mutableStateOf("") }
                val filteredOrders = remember(orders, searchQuery) {
                    filterOrdersByQuery(orders, searchQuery)
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    OrderHistorySearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        matchedCount = filteredOrders.size,
                        totalCount = orders.size,
                        inputTestTag = "profile_orders_search_input",
                        clearTestTag = "profile_orders_clear_search_button",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    if (filteredOrders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                title = "لا توجد طلبات مطابقة",
                                subtitle = "لم نعثر على طلبات تطابق \"${searchQuery.trim()}\". جرّب البحث باسم المنتج أو رقم الطلب.",
                                actionLabel = "مسح البحث",
                                onAction = { searchQuery = "" }
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("profile_order_history_list")
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ReceiptLong,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "إجمالي الطلبات: ${orders.size}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        val activeCount = orders.count { !OrderStatus.fromKey(it.status).isTerminal }
                                        Text(
                                            text = "$activeCount نشط",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            items(filteredOrders, key = { it.id }) { order ->
                                val status = OrderStatus.fromKey(order.status)
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_order_card_${order.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = order.productTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "رقم الطلب: ${formatOrderShortRef(order.id)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            OrderStatusChip(status = status)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "المتجر: ${order.sellerName} • الكمية: ${order.quantity}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatPrice(order.totalPrice, order.currency),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (order.discountCode.isNotBlank() && order.discountAmount > 0.0) {
                                            Text(
                                                text = "كود الخصم (${order.discountCode}): -${formatPrice(order.discountAmount, order.currency)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Text(
                                            text = "عنوان الشحن: ${order.shippingAddress}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        OrderProgressTracker(
                                            order = order,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { shareOrderSummary(context, order) },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .defaultMinSize(minHeight = 48.dp)
                                                    .testTag("profile_share_order_button_${order.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Share,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("مشاركة الطلب")
                                            }

                                            if (status == OrderStatus.PENDING) {
                                                OutlinedButton(
                                                    onClick = { onCancelOrder(order.id) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .defaultMinSize(minHeight = 48.dp)
                                                ) {
                                                    Text("إلغاء الطلب")
                                                }
                                            } else if (status == OrderStatus.DELIVERED) {
                                                val isReordering = order.id in reorderingOrderIds
                                                Button(
                                                    onClick = { onReorder(order) },
                                                    enabled = !isReordering,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .defaultMinSize(minHeight = 48.dp)
                                                        .testTag("profile_reorder_button_${order.id}")
                                                ) {
                                                    if (isReordering) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(18.dp),
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            strokeWidth = 2.dp
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Filled.Refresh,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("إعادة الطلب")
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
            }
        }
    }
}
