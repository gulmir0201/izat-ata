package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Category
import com.example.data.Customer
import com.example.data.Product
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProducts by viewModel.activeProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    // Screen states
    var searchBarcodeQuery by remember { mutableStateOf("") }
    var searchNameQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    
    // UI selection dialog states
    var showCustomerSelectDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Quick customer data
    var newCustName by remember { mutableStateOf("") }
    var newCustPhone by remember { mutableStateOf("") }

    // Filter products based on search and category
    val filteredProducts = activeProducts.filter { product ->
        val matchesCategory = selectedCategoryId == null || product.categoryId == selectedCategoryId
        val matchesName = searchNameQuery.isBlank() || product.name.contains(searchNameQuery, ignoreCase = true)
        val matchesBarcode = searchBarcodeQuery.isBlank() || product.barcode == searchBarcodeQuery
        matchesCategory && matchesName && matchesBarcode
    }

    // Auto-checkout if barcode exactly matches
    LaunchedEffect(searchBarcodeQuery) {
        if (searchBarcodeQuery.isNotBlank()) {
            val matchedProduct = activeProducts.find { it.barcode == searchBarcodeQuery }
            if (matchedProduct != null) {
                viewModel.addToCart(matchedProduct, 1.0)
                searchBarcodeQuery = "" // Reset search query
                Toast.makeText(context, "Товар добавлен: ${matchedProduct.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 900

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- LEFT PANE: PRODUCT SEARCH & BROWSING ---
        Column(
            modifier = Modifier
                .weight(if (isTablet) 1.6f else 1f)
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            // Search Tools
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchNameQuery,
                    onValueChange = { searchNameQuery = it },
                    label = { Text("Поиск товара по названию") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = searchBarcodeQuery,
                    onValueChange = { searchBarcodeQuery = it },
                    label = { Text("Штрихкод / Сканер") },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
            }

            // Categories Filter Tabs Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                item {
                    SleekCategoryChip(
                        text = "Все товары",
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null }
                    )
                }
                items(categories) { category ->
                    SleekCategoryChip(
                        text = category.name,
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id }
                    )
                }
            }

            // Products Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Товары не найдены",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isTablet) 3 else 2),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductItemCard(
                            product = product,
                            onAdd = { viewModel.addToCart(product, 1.0) }
                        )
                    }
                }
            }

            // Phone layout mobile checkout bar
            if (!isTablet && viewModel.cart.isNotEmpty()) {
                Button(
                    onClick = { /* In mobile we could show shopping cart drawer */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("В корзине ${viewModel.cart.size} товаров на ${viewModel.cartTotal.toInt()} сом")
                }
            }
        }

        // --- RIGHT PANE: ACTIVE SHOPPING CART CHECKOUT PANEL (Tablets only, standard POS layout) ---
        if (isTablet || viewModel.cart.isNotEmpty()) {
            VerticalDivider()
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                // Cart Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Корзина товаров",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Очистить", color = MaterialTheme.colorScheme.error)
                    }
                }

                // Cart items list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.cart.entries.toList()) { entry ->
                        CartItemRow(
                            product = entry.key,
                            quantity = entry.value,
                            onQuantityChange = { qty -> viewModel.updateCartQuantity(entry.key, qty) },
                            onDelete = { viewModel.removeFromCart(entry.key) }
                        )
                    }
                }

                Divider()

                // Customer Selection & Discount Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Customer Indicator for Debt/Sale
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { showCustomerSelectDialog = true }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = viewModel.selectedCustomer.value?.name ?: "Выбрать клиента",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Payment Method selector Segmented buttons
                    Text(
                        text = "Способ оплаты:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val paymentMethods = listOf(
                            Triple("Наличные", Icons.Default.Payments, "Нал"),
                            Triple("Карта", Icons.Default.CreditCard, "Карта"),
                            Triple("В долг", Icons.Default.AccessTime, "В долг")
                        )
                        paymentMethods.forEach { (method, icon, label) ->
                            val isSel = viewModel.paymentMethod.value == method
                            val isDebt = method == "В долг"
                            
                            val containerColor = when {
                                isSel && isDebt -> Color(0xFFFFFBEB)
                                isSel -> MaterialTheme.colorScheme.primary
                                isDebt -> Color(0xFFFBFBFB)
                                else -> Color(0xFFFAFAFA)
                            }
                            val contentColor = when {
                                isSel && isDebt -> Color(0xFFB45309)
                                isSel -> Color.White
                                isDebt -> Color(0xFFB45309).copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                            val borderColor = when {
                                isSel && isDebt -> Color(0xFFFEF3C7)
                                isSel -> MaterialTheme.colorScheme.primary
                                isDebt -> Color(0xFFF1F5F9)
                                else -> Color(0xFFE2E8F0)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(containerColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.paymentMethod.value = method
                                        if (method == "В долг" && viewModel.selectedCustomer.value == null) {
                                            showCustomerSelectDialog = true
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        color = contentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Discount Field
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Скидка на чек (сом):", fontSize = 12.sp)
                        OutlinedTextField(
                            value = if (viewModel.checkoutDiscount.value == 0.0) "" else viewModel.checkoutDiscount.value.toInt().toString(),
                            onValueChange = {
                                viewModel.checkoutDiscount.value = it.toDoubleOrNull() ?: 0.0
                            },
                            modifier = Modifier
                                .width(120.dp)
                                .height(46.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 13.sp)
                        )
                    }

                    // If paying Cash, show Change calculation
                    if (viewModel.paymentMethod.value == "Наличные") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Получено наличных (сом):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = if (viewModel.cashReceived.value == 0.0) "" else viewModel.cashReceived.value.toInt().toString(),
                                onValueChange = {
                                    viewModel.cashReceived.value = it.toDoubleOrNull() ?: 0.0
                                },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(46.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 13.sp)
                            )
                        }
                        if (viewModel.cashReceived.value > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Сдача клиенту:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${viewModel.changeAmount.toInt()} сом",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // If "В долг", show date selection and notes
                    if (viewModel.paymentMethod.value == "В долг") {
                        OutlinedTextField(
                            value = viewModel.debtNotes.value,
                            onValueChange = { viewModel.debtNotes.value = it },
                            label = { Text("Комментарий по долгу (примечание)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Preset payment date chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val intervals = listOf(
                                Pair("7 дн", 7L),
                                Pair("14 дн", 14L),
                                Pair("30 дн", 30L)
                            )
                            intervals.forEach { pair ->
                                val offsetTime = System.currentTimeMillis() + pair.second * 24 * 60 * 60 * 1000
                                val isSel = viewModel.dueDate.value in (offsetTime - 60000)..(offsetTime + 60000)
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.dueDate.value = offsetTime },
                                    label = { Text(pair.first) }
                                )
                            }
                        }
                    }
                }

                Divider()

                // Total Summary & Submit
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Итого к оплате:",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${viewModel.cartTotal.toInt()} сом",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            if (viewModel.paymentMethod.value == "В долг" && viewModel.selectedCustomer.value == null) {
                                Toast.makeText(context, "Для продажи в долг выберите клиента!", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.processCheckout()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "Оплатить ${viewModel.cartTotal.toInt()} сом",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS SECTION ---

    // 1. Customer Selection Dialog
    if (showCustomerSelectDialog) {
        Dialog(onDismissRequest = { showCustomerSelectDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Клиенты", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            showAddCustomerDialog = true
                            showCustomerSelectDialog = false
                        }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customers) { customer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        viewModel.selectedCustomer.value = customer
                                        showCustomerSelectDialog = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(customer.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                if (customer.totalDebt > 0) {
                                    Text(
                                        "Долг: ${customer.totalDebt.toInt()} сом",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showCustomerSelectDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }

    // 2. Add Customer Quick Dialog
    if (showAddCustomerDialog) {
        Dialog(onDismissRequest = { showAddCustomerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Новый клиент", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newCustName,
                        onValueChange = { newCustName = it },
                        label = { Text("Имя Фамилия") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCustPhone,
                        onValueChange = { newCustPhone = it },
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddCustomerDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                if (newCustName.isNotBlank()) {
                                    viewModel.addCustomer(newCustName, newCustPhone, "", "")
                                    newCustName = ""
                                    newCustPhone = ""
                                    showAddCustomerDialog = false
                                    showCustomerSelectDialog = true
                                }
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Создать")
                        }
                    }
                }
            }
        }
    }

    // 3. Receipt / Checkout Success Success Dialog
    val lastSale = viewModel.lastCompletedSale.value
    if (viewModel.showCheckoutSuccessDialog.value && lastSale != null) {
        Dialog(onDismissRequest = { viewModel.showCheckoutSuccessDialog.value = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Чек успешно сохранен!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Номер чека: ${lastSale.receiptNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated receipt slip
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = storeSettings?.storeName ?: "Изат-Ата",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = storeSettings?.address ?: "ул. Ленина, д. 45",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = storeSettings?.phone ?: "+996 (555) 12-34-56",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "=========================",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Способ оплаты:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(lastSale.paymentMethod, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            if (lastSale.discount > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Скидка:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("${lastSale.discount.toInt()} сом", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ИТОГО К ОПЛАТЕ:", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("${lastSale.totalAmount.toInt()} сом", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(
                                text = "=========================",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = storeSettings?.receiptFooter ?: "Спасибо за покупку!",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Имитация печати на принтере...", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Печать")
                        }

                        Button(
                            onClick = { viewModel.showCheckoutSuccessDialog.value = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Готово")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onAdd: () -> Unit
) {
    val isLowStock = product.stockQuantity <= product.minStock
    val isOutOfStock = product.stockQuantity <= 0.0
    val cardAlpha = if (isOutOfStock) 0.6f else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(18.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .alpha(cardAlpha)
        ) {
            // Sleek dynamic emoji container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getProductEmoji(product.name),
                    fontSize = 32.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = product.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${product.sellingPrice.toInt()} сом",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Остаток: ${product.stockQuantity.toInt()} ${product.unit}",
                        fontSize = 10.sp,
                        color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SleekCategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun getProductEmoji(name: String): String {
    val lower = name.lowercase(Locale.ROOT)
    return when {
        lower.contains("молок") || lower.contains("кефир") || lower.contains("сливк") || lower.contains("сметан") || lower.contains("айран") -> "🥛"
        lower.contains("хлеб") || lower.contains("батон") || lower.contains("лепёш") || lower.contains("булоч") || lower.contains("лаваш") -> "🍞"
        lower.contains("кола") || lower.contains("coca") || lower.contains("pepsi") || lower.contains("fanta") || lower.contains("sprite") || lower.contains("напиток") || lower.contains("сок") || lower.contains("вода") || lower.contains("лимонад") -> "🥤"
        lower.contains("яблок") -> "🍎"
        lower.contains("банан") -> "🍌"
        lower.contains("груш") -> "🍐"
        lower.contains("мандарин") || lower.contains("апельсин") -> "🍊"
        lower.contains("лимон") -> "🍋"
        lower.contains("конфет") || lower.contains("сладост") || lower.contains("шоколад") || lower.contains("торт") || lower.contains("печен") || lower.contains("вафл") -> "🍬"
        lower.contains("чай") || lower.contains("кофе") -> "☕"
        lower.contains("масло") -> "🧈"
        lower.contains("сыр") -> "🧀"
        lower.contains("яйц") -> "🥚"
        lower.contains("колбас") || lower.contains("сосиск") || lower.contains("мясо") || lower.contains("фарш") -> "🍖"
        lower.contains("макарон") || lower.contains("спагет") || lower.contains("лапш") -> "🍝"
        lower.contains("рис") || lower.contains("греч") || lower.contains("крупа") -> "🌾"
        else -> "📦"
    }
}

@Composable
fun CartItemRow(
    product: Product,
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = "${product.sellingPrice.toInt()} сом / ${product.unit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChange(quantity - 1.0) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Sub", modifier = Modifier.size(20.dp))
                }
                Text(
                    text = if (product.unit == "кг") "%.1f".format(quantity) else quantity.toInt().toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onQuantityChange(quantity + 1.0) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Add", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Del",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
