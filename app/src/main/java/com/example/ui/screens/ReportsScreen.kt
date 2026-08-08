package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AppViewModel

@Composable
fun ReportsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()
    val debts by viewModel.debts.collectAsState()

    // Sales metrics calculations
    val totalSalesSum = sales.sumOf { it.totalAmount }
    val totalReceiptsCount = sales.size
    val averageReceiptSize = if (totalReceiptsCount > 0) totalSalesSum / totalReceiptsCount else 0.0

    val cashSales = sales.filter { it.paymentMethod == "Наличные" }.sumOf { it.totalAmount }
    val cardSales = sales.filter { it.paymentMethod == "Карта" }.sumOf { it.totalAmount }
    val debtSales = sales.filter { it.paymentMethod == "В долг" }.sumOf { it.totalAmount }

    // Active debts metrics
    val totalRemainingDebt = debts.filter { it.remainingAmount > 0.0 }.sumOf { it.remainingAmount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Primary Report Stats
        item {
            Text(
                text = "Сводные финансовые отчеты",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Общий оборот", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${totalSalesSum.toInt()} сом", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Средний чек", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${averageReceiptSize.toInt()} сом", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Sales by payment methods (Visual Progress Bars)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Структура выручки",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val totalCalculated = cashSales + cardSales + debtSales
                    val cashWeight = if (totalCalculated > 0) (cashSales / totalCalculated).toFloat() else 0f
                    val cardWeight = if (totalCalculated > 0) (cardSales / totalCalculated).toFloat() else 0f
                    val debtWeight = if (totalCalculated > 0) (debtSales / totalCalculated).toFloat() else 0f

                    // Cash Progress Row
                    PaymentMethodProgressRow(
                        label = "Наличные оплаты",
                        amount = cashSales,
                        percentage = (cashWeight * 100).toInt(),
                        color = MaterialTheme.colorScheme.primary,
                        progress = cashWeight
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Card Progress Row
                    PaymentMethodProgressRow(
                        label = "Безналичные (карта)",
                        amount = cardSales,
                        percentage = (cardWeight * 100).toInt(),
                        color = MaterialTheme.colorScheme.secondary,
                        progress = cardWeight
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Debt Progress Row
                    PaymentMethodProgressRow(
                        label = "Оформили в долг",
                        amount = debtSales,
                        percentage = (debtWeight * 100).toInt(),
                        color = MaterialTheme.colorScheme.error,
                        progress = debtWeight
                    )
                }
            }
        }

        // Section: Active Debts Summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Общая дебиторская задолженность",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${totalRemainingDebt.toInt()} сом",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodProgressRow(
    label: String,
    amount: Double,
    percentage: Int,
    color: Color,
    progress: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${amount.toInt()} сом ($percentage%)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
