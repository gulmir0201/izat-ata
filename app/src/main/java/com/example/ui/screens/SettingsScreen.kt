package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StoreSettings
import com.example.ui.viewmodel.AppViewModel

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()

    // Screen-level local form state
    var storeName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("сом") }
    var receiptHeader by remember { mutableStateOf("") }
    var receiptFooter by remember { mutableStateOf("") }
    var smsDueDay by remember { mutableStateOf("") }

    // Synchronize local states with Room flow values when loaded
    LaunchedEffect(storeSettings) {
        storeSettings?.let { settings ->
            storeName = settings.storeName
            address = settings.address
            phone = settings.phone
            currency = settings.currency
            receiptHeader = settings.receiptHeader
            receiptFooter = settings.receiptFooter
            smsDueDay = settings.smsDueDay
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Настройки магазина и POS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Section: Store Info Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Основные реквизиты", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Название магазина") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Адрес") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Телефон") },
                            modifier = Modifier.weight(1.2f)
                        )

                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Валюта") },
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }
        }

        // Section: Receipt Templates
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Шаблон кассового чека", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = receiptHeader,
                        onValueChange = { receiptHeader = it },
                        label = { Text("Заголовок чека") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Подвал чека") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section: SMS Configurations
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Уведомления о долгах (SMS)", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = smsDueDay,
                        onValueChange = { smsDueDay = it },
                        label = { Text("Шаблон SMS напоминания") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        }

        // Save Action Button Card
        item {
            Button(
                onClick = {
                    val original = storeSettings ?: StoreSettings()
                    val updated = original.copy(
                        storeName = storeName,
                        address = address,
                        phone = phone,
                        currency = currency,
                        receiptHeader = receiptHeader,
                        receiptFooter = receiptFooter,
                        smsDueDay = smsDueDay
                    )
                    viewModel.saveSettings(updated)
                    Toast.makeText(context, "Настройки сохранены!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save Settings")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Сохранить настройки", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
