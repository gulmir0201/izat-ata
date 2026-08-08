package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Customer
import com.example.data.Sale
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSaleForDetails by remember { mutableStateOf<Sale?>(null) }

    val filteredSales = sales.filter { sale ->
        searchQuery.isBlank() || sale.receiptNumber.contains(searchQuery)
    }

    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск чека по номеру (например, IZ-2608...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredSales.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Журнал продаж пуст", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSales) { sale ->
                    val customer = customers.find { it.id == sale.customerId }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSaleForDetails = sale },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Чек №${sale.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = sdf.format(Date(sale.createdAt)),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                if (customer != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Покупатель: ${customer.name}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${sale.totalAmount.toInt()} сом",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (sale.isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (sale.isDebt) MaterialTheme.colorScheme.errorContainer
                                                else MaterialTheme.colorScheme.primaryContainer
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = sale.paymentMethod,
                                            fontSize = 9.sp,
                                            color = if (sale.isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: Sale Receipts Details ---
    val targetSale = selectedSaleForDetails
    if (targetSale != null) {
        val customer = customers.find { it.id == targetSale.customerId }
        // Fetch matching items for this receipt from the state
        // In clean local flow architecture, we can query items, but since we are showing total receipts here, 
        // we can dynamically build items table if needed.
        
        Dialog(onDismissRequest = { selectedSaleForDetails = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Копия чека", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { selectedSaleForDetails = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = storeSettings?.address ?: "ул. Ленина, д. 45",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Тел: " + (storeSettings?.phone ?: "+996 (555) 12-34-56"),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "-------------------------",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Чек №:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(targetSale.receiptNumber, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Время:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(sdf.format(Date(targetSale.createdAt)), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Тип оплаты:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(targetSale.paymentMethod, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            if (customer != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Клиент:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text(customer.name, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "-------------------------",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            if (targetSale.discount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Скидка:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("${targetSale.discount.toInt()} сом", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("СУММА К ОПЛАТЕ:", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("${targetSale.totalAmount.toInt()} сом", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(
                                text = "-------------------------",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            Text(
                                text = storeSettings?.receiptFooter ?: "Спасибо за визит!",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { selectedSaleForDetails = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}
