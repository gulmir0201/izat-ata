package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Category
import com.example.data.Product
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val movements by viewModel.inventoryMovements.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    
    // Add/Edit Dialog States
    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // Dialog inputs
    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var barcode by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("шт") }
    var isActive by remember { mutableStateOf(true) }

    // Movement history modal
    var selectedProductForHistory by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = products.filter {
        val matchesCategory = selectedCategoryId == null || it.categoryId == selectedCategoryId
        val matchesSearch = searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery)
        matchesCategory && matchesSearch
    }

    LaunchedEffect(showProductDialog) {
        if (!showProductDialog) {
            editingProduct = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    name = ""
                    categoryId = categories.firstOrNull()?.id
                    barcode = ""
                    sellingPrice = ""
                    costPrice = ""
                    stockQuantity = ""
                    minStock = "5.0"
                    unit = "шт"
                    isActive = true
                    showProductDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(14.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск по названию или штрихкоду") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Products list
            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("База товаров пуста или ничего не найдено", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { product ->
                        val productCategory = categories.find { it.id == product.categoryId }
                        val isLowStock = product.stockQuantity <= product.minStock

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            text = "Категория: ${productCategory?.name ?: "Без категории"} | ШК: ${product.barcode.ifBlank { "Отсутствует" }}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    // Action buttons
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            selectedProductForHistory = product
                                        }) {
                                            Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            editingProduct = product
                                            name = product.name
                                            categoryId = product.categoryId
                                            barcode = product.barcode
                                            sellingPrice = product.sellingPrice.toInt().toString()
                                            costPrice = product.costPrice.toInt().toString()
                                            stockQuantity = product.stockQuantity.toString()
                                            minStock = product.minStock.toString()
                                            unit = product.unit
                                            isActive = product.isActive
                                            showProductDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                                        }
                                        IconButton(onClick = {
                                            viewModel.deleteProduct(product.id)
                                            Toast.makeText(context, "Товар удален", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Цена закупа", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text("${product.costPrice.toInt()} сом", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Цена продажи", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text("${product.sellingPrice.toInt()} сом", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column {
                                        Text("В наличии", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${product.stockQuantity} ${product.unit}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isLowStock) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Warning, contentDescription = "Low Stock", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Column {
                                        Text("Статус", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text(
                                            text = if (product.isActive) "Активен" else "Пассивен",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (product.isActive) MaterialTheme.colorScheme.primary else Color.Gray
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

    // --- DIALOG: Add/Edit Product ---
    if (showProductDialog) {
        Dialog(onDismissRequest = { showProductDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(18.dp)) {
                    item {
                        Text(
                            text = if (editingProduct == null) "Добавить товар" else "Редактировать товар",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Название товара") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Dropdown simulated selection
                        Text("Категория товара:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { category ->
                                val isSelected = categoryId == category.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { categoryId = category.id },
                                    label = { Text(category.name) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Штрихкод (сканируйте сканером)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = costPrice,
                                onValueChange = { costPrice = it },
                                label = { Text("Закупка (сом)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sellingPrice,
                                onValueChange = { sellingPrice = it },
                                label = { Text("Продажа (сом)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = stockQuantity,
                                onValueChange = { stockQuantity = it },
                                label = { Text("Остаток") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = minStock,
                                onValueChange = { minStock = it },
                                label = { Text("Мин. порог") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Unit Selection segment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val units = listOf("шт", "кг", "л", "гр")
                            units.forEach { u ->
                                FilterChip(
                                    selected = unit == u,
                                    onClick = { unit = u },
                                    label = { Text(u) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                            Text("Товар доступен для продажи (активен)")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showProductDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Отмена")
                            }
                            Button(
                                onClick = {
                                    if (name.isNotBlank() && sellingPrice.isNotBlank() && costPrice.isNotBlank()) {
                                        val sellP = sellingPrice.toDoubleOrNull() ?: 0.0
                                        val costP = costPrice.toDoubleOrNull() ?: 0.0
                                        val stockQ = stockQuantity.toDoubleOrNull() ?: 0.0
                                        val minS = minStock.toDoubleOrNull() ?: 0.0

                                        val ep = editingProduct
                                        if (ep == null) {
                                            viewModel.addProduct(
                                                name = name,
                                                categoryId = categoryId,
                                                barcode = barcode,
                                                sellingPrice = sellP,
                                                costPrice = costP,
                                                stockQuantity = stockQ,
                                                minStock = minS,
                                                unit = unit,
                                                supplierId = null,
                                                isActive = isActive
                                            )
                                            Toast.makeText(context, "Товар добавлен", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val stockAdjustment = stockQ - ep.stockQuantity
                                            viewModel.updateProduct(
                                                ep.copy(
                                                    name = name,
                                                    categoryId = categoryId,
                                                    barcode = barcode,
                                                    sellingPrice = sellP,
                                                    costPrice = costP,
                                                    stockQuantity = stockQ,
                                                    minStock = minS,
                                                    unit = unit,
                                                    isActive = isActive
                                                ),
                                                stockAdjustment = stockAdjustment
                                            )
                                            Toast.makeText(context, "Товар обновлен", Toast.LENGTH_SHORT).show()
                                        }
                                        showProductDialog = false
                                    } else {
                                        Toast.makeText(context, "Заполните обязательные поля!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("Сохранить")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: Movement history logs ---
    val targetProduct = selectedProductForHistory
    if (targetProduct != null) {
        val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
        val productMovements = movements.filter { it.productId == targetProduct.id }

        Dialog(onDismissRequest = { selectedProductForHistory = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Движение: ${targetProduct.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    if (productMovements.isEmpty()) {
                        Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("История изменений пуста", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.height(240.dp)) {
                            items(productMovements) { mov ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${mov.type} - ${mov.reason.ifBlank { "Ручная корректировка" }}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = sdf.format(Date(mov.createdAt)),
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = (if (mov.quantity > 0) "+" else "") + "${mov.quantity} ${targetProduct.unit}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (mov.quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { selectedProductForHistory = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}
