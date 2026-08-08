package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.SmsLog
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val debts by viewModel.debts.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val smsLogs by viewModel.smsLogs.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Список долгов, 1 = SMS-уведомления
    var filterStatus by remember { mutableStateOf("Все") }

    val sdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    // Repayment Modal
    var repayDebtTarget by remember { mutableStateOf<Debt?>(null) }
    var repayAmountInput by remember { mutableStateOf("") }
    var repayMethodInput by remember { mutableStateOf("Наличные") }
    var repayNotesInput by remember { mutableStateOf("") }

    // Manual SMS Modal
    var smsTargetDebt by remember { mutableStateOf<Debt?>(null) }
    var smsCustomText by remember { mutableStateOf("") }

    val filteredDebts = debts.filter { debt ->
        val customer = customers.find { it.id == debt.customerId }
        val matchesStatus = when (filterStatus) {
            "Все" -> true
            "Активные" -> debt.remainingAmount > 0.1
            "Просроченные" -> debt.remainingAmount > 0.1 && (debt.status == "Просрочен" || debt.dueDate < System.currentTimeMillis())
            "Оплаченные" -> debt.remainingAmount <= 0.1
            else -> debt.status == filterStatus
        }
        matchesStatus
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Headers
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AssignmentLate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Учет долгов", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SMS-напоминания", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // TAB CONTENT
        if (activeTab == 0) {
            // --- TAB 0: DEBTS LIST ---
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusFilters = listOf("Все", "Активные", "Просроченные", "Оплаченные")
                    statusFilters.forEach { status ->
                        FilterChip(
                            selected = filterStatus == status,
                            onClick = { filterStatus = status },
                            label = { Text(status) }
                        )
                    }
                }

                if (filteredDebts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Нет долгов по выбранному фильтру", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredDebts) { debt ->
                            val customer = customers.find { it.id == debt.customerId }
                            val isOverdue = debt.remainingAmount > 0.0 && debt.dueDate < System.currentTimeMillis()
                            val formattedDueDate = sdf.format(Date(debt.dueDate))
                            val formattedPurchaseDate = sdf.format(Date(debt.createdAt))

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
                                        Column {
                                            Text(customer?.name ?: "Удаленный клиент", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Тел: ${customer?.phone ?: ""}", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        // Status badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (debt.remainingAmount <= 0.1) MaterialTheme.colorScheme.primaryContainer
                                                    else if (isOverdue) MaterialTheme.colorScheme.errorContainer
                                                    else MaterialTheme.colorScheme.secondaryContainer
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (debt.remainingAmount <= 0.1) "Оплачен" else if (isOverdue) "Просрочен" else "Активен",
                                                color = if (debt.remainingAmount <= 0.1) MaterialTheme.colorScheme.primary else if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                                    // Debt Details Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Взят долг", fontSize = 10.sp, color = Color.Gray)
                                            Text(formattedPurchaseDate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Column {
                                            Text("Срок оплаты", fontSize = 10.sp, color = Color.Gray)
                                            Text(
                                                text = formattedDueDate,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Column {
                                            Text("Сумма долга", fontSize = 10.sp, color = Color.Gray)
                                            Text("${debt.amount.toInt()} сом", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Остаток", fontSize = 10.sp, color = Color.Gray)
                                            Text(
                                                text = "${debt.remainingAmount.toInt()} сом",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (debt.remainingAmount > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    if (debt.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Примечание: ${debt.notes}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    // Action bar inside card
                                    if (debt.remainingAmount > 0.0) {
                                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Manual SMS Trigger
                                            IconButton(onClick = {
                                                val remainingAmount = debt.remainingAmount.toInt()
                                                val dueDateStr = sdf.format(Date(debt.dueDate))
                                                val smsTemplate = storeSettings?.smsDueDay ?: "Срок оплаты долга {amount} наступает {due_date}."
                                                smsCustomText = smsTemplate
                                                    .replace("{customer_name}", customer?.name ?: "")
                                                    .replace("{amount}", remainingAmount.toString())
                                                    .replace("{due_date}", dueDateStr)
                                                    .replace("{currency}", storeSettings?.currency ?: "сом")
                                                smsTargetDebt = debt
                                            }) {
                                                Icon(Icons.Default.Sms, contentDescription = "Send Reminder", tint = MaterialTheme.colorScheme.secondary)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = {
                                                    repayAmountInput = debt.remainingAmount.toInt().toString()
                                                    repayNotesInput = ""
                                                    repayMethodInput = "Наличные"
                                                    repayDebtTarget = debt
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Оплатить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- TAB 1: SMS LOGS & REMINDER TRIGGERS ---
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Text(
                    text = "Журнал отправленных SMS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (smsLogs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("SMS уведомления еще не отправлялись", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(smsLogs) { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(log.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(log.phone, fontSize = 11.sp, color = Color.Gray)
                                        }

                                        // Status badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (log.status == "Отправлено") MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.errorContainer
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = log.status,
                                                color = if (log.status == "Отправлено") MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.error,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = log.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )

                                    if (log.status != "Отправлено" && log.errorMessage.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ошибка: ${log.errorMessage}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
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

    // --- DIALOG: Repay Debt ---
    val targetRepay = repayDebtTarget
    if (targetRepay != null) {
        val customer = customers.find { it.id == targetRepay.customerId }

        Dialog(onDismissRequest = { repayDebtTarget = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Погашение долга", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Клиент: ${customer?.name ?: ""}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Остаток долга: ${targetRepay.remainingAmount.toInt()} сом", fontSize = 13.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    OutlinedTextField(
                        value = repayAmountInput,
                        onValueChange = { repayAmountInput = it },
                        label = { Text("Сумма к оплате (сом)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = repayNotesInput,
                        onValueChange = { repayNotesInput = it },
                        label = { Text("Комментарий / заметка") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Способ оплаты:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val methods = listOf("Наличные", "Карта")
                        methods.forEach { method ->
                            val isSel = repayMethodInput == method
                            FilterChip(
                                selected = isSel,
                                onClick = { repayMethodInput = method },
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
                            onClick = { repayDebtTarget = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                val amt = repayAmountInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0 && amt <= targetRepay.remainingAmount) {
                                    viewModel.submitDebtRepayment(targetRepay.id, amt, repayMethodInput, repayNotesInput)
                                    Toast.makeText(context, "Платеж успешно проведен!", Toast.LENGTH_SHORT).show()
                                    repayDebtTarget = null
                                } else {
                                    Toast.makeText(context, "Сумма введена неверно!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Принять")
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: Manual Custom SMS Trigger ---
    val smsDebt = smsTargetDebt
    if (smsDebt != null) {
        val customer = customers.find { it.id == smsDebt.customerId }

        Dialog(onDismissRequest = { smsTargetDebt = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Отправить SMS напоминание", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Клиент: ${customer?.name ?: ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Телефон: ${customer?.phone ?: ""}", fontSize = 11.sp, color = Color.Gray)

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    OutlinedTextField(
                        value = smsCustomText,
                        onValueChange = { smsCustomText = it },
                        label = { Text("Текст сообщения") },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { smsTargetDebt = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                if (smsCustomText.isNotBlank() && customer != null) {
                                    viewModel.triggerSmsNow(
                                        customerName = customer.name,
                                        phone = customer.phone,
                                        messageText = smsCustomText,
                                        type = "Вручную"
                                    )
                                    Toast.makeText(context, "Запрос отправки SMS отправлен!", Toast.LENGTH_SHORT).show()
                                    smsTargetDebt = null
                                }
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Отправить сейчас")
                        }
                    }
                }
            }
        }
    }
}
