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
import com.example.data.Customer
import com.example.data.Debt
import com.example.data.Sale
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val debts by viewModel.debts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    
    // Add/Edit Customer Modal States
    var showCustomerModal by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }

    // Dialog inputs
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Repayment Modal state
    var showRepayModalForCustomer by remember { mutableStateOf<Customer?>(null) }
    var repaymentAmount by remember { mutableStateOf("") }
    var repaymentMethod by remember { mutableStateOf("Наличные") }
    var repaymentNotes by remember { mutableStateOf("") }

    // Details Modal
    var activeDetailsCustomer by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = customers.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    LaunchedEffect(showCustomerModal) {
        if (!showCustomerModal) {
            editingCustomer = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    name = ""
                    phone = ""
                    address = ""
                    note = ""
                    showCustomerModal = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск клиента по имени или номеру телефона") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredCustomers.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("База клиентов пуста или ничего не найдено", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers) { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeDetailsCustomer = customer },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(
                                            text = "Телефон: ${customer.phone.ifBlank { "Не указан" }}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    // Debt Status Badge
                                    if (customer.totalDebt > 0.0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Долг: ${customer.totalDebt.toInt()} сом",
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Нет долгов",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                if (customer.address.isNotBlank() || customer.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Адрес: ${customer.address.ifBlank { "не указан" }} | ${customer.note}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                repaymentAmount = ""
                                                repaymentNotes = ""
                                                repaymentMethod = "Наличные"
                                                showRepayModalForCustomer = customer
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            enabled = customer.totalDebt > 0.0
                                        ) {
                                            Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Погасить долг", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            editingCustomer = customer
                                            name = customer.name
                                            phone = customer.phone
                                            address = customer.address
                                            note = customer.note
                                            showCustomerModal = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = {
                                            viewModel.deleteCustomer(customer.id)
                                            Toast.makeText(context, "Клиент удален", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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

    // --- DIALOG: Add/Edit Customer ---
    if (showCustomerModal) {
        Dialog(onDismissRequest = { showCustomerModal = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (editingCustomer == null) "Зарегистрировать клиента" else "Изменить профиль клиента",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Имя Фамилия") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Номер телефона") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Адрес проживания") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Заметки (комментарии)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCustomerModal = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val ec = editingCustomer
                                    if (ec == null) {
                                        viewModel.addCustomer(name, phone, address, note)
                                        Toast.makeText(context, "Клиент добавлен", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateCustomer(ec.copy(name = name, phone = phone, address = address, note = note))
                                        Toast.makeText(context, "Профиль обновлен", Toast.LENGTH_SHORT).show()
                                    }
                                    showCustomerModal = false
                                } else {
                                    Toast.makeText(context, "Заполните имя клиента!", Toast.LENGTH_SHORT).show()
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

    // --- DIALOG: Repayment process ---
    val repCustomer = showRepayModalForCustomer
    if (repCustomer != null) {
        Dialog(onDismissRequest = { showRepayModalForCustomer = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Внести платеж: ${repCustomer.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Общий долг: ${repCustomer.totalDebt.toInt()} сом", fontSize = 13.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    OutlinedTextField(
                        value = repaymentAmount,
                        onValueChange = { repaymentAmount = it },
                        label = { Text("Сумма к оплате (сом)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = repaymentNotes,
                        onValueChange = { repaymentNotes = it },
                        label = { Text("Комментарий к платежу") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Способ оплаты:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val methods = listOf("Наличные", "Карта")
                        methods.forEach { method ->
                            val isSel = repaymentMethod == method
                            FilterChip(
                                selected = isSel,
                                onClick = { repaymentMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRepayModalForCustomer = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                val amt = repaymentAmount.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0 && amt <= repCustomer.totalDebt) {
                                    // Query customer debts to apply repayment towards oldest debt
                                    val customerDebts = debts.filter { it.customerId == repCustomer.id && it.remainingAmount > 0.0 }
                                    var remainingRepay = amt
                                    
                                    for (debt in customerDebts) {
                                        if (remainingRepay <= 0.0) break
                                        val payAmt = remainingRepay.coerceAtMost(debt.remainingAmount)
                                        viewModel.submitDebtRepayment(debt.id, payAmt, repaymentMethod, repaymentNotes)
                                        remainingRepay -= payAmt
                                    }
                                    
                                    Toast.makeText(context, "Платеж внесен успешно!", Toast.LENGTH_LONG).show()
                                    showRepayModalForCustomer = null
                                } else {
                                    Toast.makeText(context, "Введите корректную сумму!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Подтвердить")
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: Customer Profile Details ---
    val detailsCust = activeDetailsCustomer
    if (detailsCust != null) {
        val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
        val customerSales = sales.filter { it.customerId == detailsCust.id }
        val customerDebts = debts.filter { it.customerId == detailsCust.id }

        Dialog(onDismissRequest = { activeDetailsCustomer = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(detailsCust.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { activeDetailsCustomer = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Text("Тел: ${detailsCust.phone} | Адрес: ${detailsCust.address.ifBlank { "не указан" }}", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Общий долг", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${detailsCust.totalDebt.toInt()} сом", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                            }
                            Column {
                                Text("Покупок всего", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${customerSales.size} чеков", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("История покупок & долгов:", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    LazyColumn(
                        modifier = Modifier.height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (customerSales.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    Text("История покупок пуста", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(customerSales) { sale ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Чек №${sale.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(sdf.format(Date(sale.createdAt)), fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Text("${sale.totalAmount.toInt()} сом (${sale.paymentMethod})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { activeDetailsCustomer = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}
