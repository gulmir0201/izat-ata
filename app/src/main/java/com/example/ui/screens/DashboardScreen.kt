package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.Sale
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onNavigateTo: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val lowStockList by viewModel.lowStockProducts.collectAsState()
    val productsList by viewModel.products.collectAsState()

    // Calculations based on live database flows
    val todayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todaySales = sales.filter { it.createdAt >= todayMillis }
    val totalSalesToday = todaySales.sumOf { it.totalAmount }
    val receiptsCountToday = todaySales.size
    val cashInRegister = todaySales.filter { it.paymentMethod == "Наличные" }.sumOf { it.totalAmount }

    // Aggregate remaining debt amounts
    val activeDebts = debts.filter { it.remainingAmount > 0.0 }
    val totalDebtsAmount = activeDebts.sumOf { it.remainingAmount }
    
    val overdueDebts = activeDebts.filter { 
        it.status == "Просрочен" || it.dueDate < System.currentTimeMillis() 
    }
    val overdueDebtsAmount = overdueDebts.sumOf { it.remainingAmount }

    // Due in 3 days warning
    val threeDaysFromNow = System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000
    val upcomingDebtsCount = activeDebts.filter { 
        it.dueDate in System.currentTimeMillis()..threeDaysFromNow 
    }.size

    val configuration = LocalConfiguration.current
    val gridCells = if (configuration.screenWidthDp > 900) 4 else if (configuration.screenWidthDp > 600) 2 else 1

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Smart Dynamic Notifications
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (upcomingDebtsCount > 0) {
                    AlertNotificationCard(
                        message = "$upcomingDebtsCount клиентов должны оплатить в ближайшие 3 дня",
                        color = MaterialTheme.colorScheme.secondary,
                        icon = Icons.Default.Warning,
                        onClick = { onNavigateTo(AppScreen.DEBTS) }
                    )
                }
                if (overdueDebts.isNotEmpty()) {
                    AlertNotificationCard(
                        message = "${overdueDebts.size} клиентов имеют просроченные долги!",
                        color = MaterialTheme.colorScheme.error,
                        icon = Icons.Default.Error,
                        onClick = { onNavigateTo(AppScreen.DEBTS) }
                    )
                }
                if (lowStockList.isNotEmpty()) {
                    AlertNotificationCard(
                        message = "${lowStockList.size} товаров имеют низкий остаток на складе",
                        color = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Default.Inventory,
                        onClick = { onNavigateTo(AppScreen.PRODUCTS) }
                    )
                }
            }
        }

        // 2. Metrics Grid Layout
        item {
            Text(
                text = "Показатели за сегодня",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Flex Grid implementation in Jetpack Compose
            if (gridCells == 4) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(title = "Продажи сегодня", valStr = "${totalSalesToday.toInt()} сом", subStr = "$receiptsCountToday чеков", icon = Icons.Default.TrendingUp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    MetricCard(title = "Касса (наличные)", valStr = "${cashInRegister.toInt()} сом", subStr = "В ящике", icon = Icons.Default.Payments, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                    MetricCard(title = "Сумма долгов", valStr = "${totalDebtsAmount.toInt()} сом", subStr = "${activeDebts.size} клиентов", icon = Icons.Default.AssignmentLate, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                    MetricCard(title = "Просрочено", valStr = "${overdueDebtsAmount.toInt()} сом", subStr = "Срочно к оплате", icon = Icons.Default.NewReleases, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(title = "Продажи сегодня", valStr = "${totalSalesToday.toInt()} сом", subStr = "$receiptsCountToday чеков", icon = Icons.Default.TrendingUp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        MetricCard(title = "Наличные касса", valStr = "${cashInRegister.toInt()} сом", subStr = "В ящике", icon = Icons.Default.Payments, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(title = "Сумма долгов", valStr = "${totalDebtsAmount.toInt()} сом", subStr = "${activeDebts.size} клиентов", icon = Icons.Default.AssignmentLate, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                        MetricCard(title = "Просрочено", valStr = "${overdueDebtsAmount.toInt()} сом", subStr = "Внимание", icon = Icons.Default.NewReleases, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 3. Quick Action Buttons
        item {
            Text(
                text = "Быстрые действия",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    title = "Новая продажа",
                    icon = Icons.Default.AddShoppingCart,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigateTo(AppScreen.POS) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Добавить товар",
                    icon = Icons.Default.AddCircleOutline,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onNavigateTo(AppScreen.PRODUCTS) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Клиенты в долг",
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { onNavigateTo(AppScreen.CUSTOMERS) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Отчеты",
                    icon = Icons.Default.Assessment,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigateTo(AppScreen.REPORTS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Recent Sales History List
        item {
            Text(
                text = "Последние продажи",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        if (sales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Продаж пока нет", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        } else {
            val recentSales = sales.take(5)
            items(recentSales) { sale ->
                RecentSaleRow(sale = sale, onClick = { onNavigateTo(AppScreen.SALES) })
            }
        }
    }
}

@Composable
fun AlertNotificationCard(
    message: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = "Alert", tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = color.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    valStr: String,
    subStr: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(18.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = valStr,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subStr,
                    fontSize = 11.sp,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(95.dp)
            .clickable(onClick = onClick)
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
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RecentSaleRow(
    sale: Sale,
    onClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = sdf.format(Date(sale.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(18.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Чек №${sale.receiptNumber}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (sale.paymentMethod) {
                                    "В долг" -> MaterialTheme.colorScheme.errorContainer
                                    "Наличные" -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sale.paymentMethod,
                            fontSize = 10.sp,
                            color = when (sale.paymentMethod) {
                                "В долг" -> MaterialTheme.colorScheme.error
                                "Наличные" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.secondary
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "${sale.totalAmount.toInt()} сом",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (sale.isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
