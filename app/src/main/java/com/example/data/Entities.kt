package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactPhone: String = "",
    val contactEmail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["barcode"], unique = false), Index(value = ["categoryId"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long?,
    val barcode: String = "",
    val sellingPrice: Double,
    val costPrice: Double,
    val stockQuantity: Double, // Double to support weighted goods (e.g. 1.5 kg)
    val minStock: Double = 0.0,
    val unit: String = "шт", // "шт", "кг", "л"
    val supplierId: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val totalDebt: Double = 0.0,
    val lastPaymentDate: Long? = null,
    val nextPaymentDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val cashierName: String = "Администратор",
    val customerId: Long? = null,
    val totalAmount: Double,
    val discount: Double = 0.0,
    val paymentMethod: String, // "Наличные", "Карта", "Смешанная", "В долг"
    val isDebt: Boolean = false,
    val cashAmountGiven: Double = 0.0,
    val changeAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"])]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val price: Double,
    val costPrice: Double,
    val unit: String,
    val totalAmount: Double
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val saleId: Long?,
    val amount: Double,
    val remainingAmount: Double,
    val dueDate: Long,
    val notes: String = "",
    val status: String = "Ожидает оплаты", // "Новый", "Ожидает оплаты", "Скоро оплата", "Просрочен", "Частично оплачен", "Оплачен"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debt_payments")
data class DebtPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Double,
    val paymentMethod: String = "Наличные",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val phone: String,
    val message: String,
    val sentAt: Long = System.currentTimeMillis(),
    val type: String, // "3 дня до срока", "1 день до срока", "В день оплаты", "После срока", "Вручную"
    val status: String, // "Отправлено", "Ошибка"
    val errorMessage: String = ""
)

@Entity(tableName = "inventory_movements")
data class InventoryMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val quantity: Double,
    val type: String, // "Приход", "Расход", "Инвентаризация", "Продажа", "Возврат"
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class StoreSettings(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "Изат-Ата",
    val address: String = "ул. Ленина, д. 45",
    val phone: String = "+996 (555) 12-34-56",
    val currency: String = "сом",
    val taxRate: Double = 0.0,
    val receiptHeader: String = "Добро пожаловать в Изат-Ата!",
    val receiptFooter: String = "Спасибо за покупку!",
    val sms3DaysBefore: String = "Здравствуйте, {customer_name}! Напоминаем, что срок оплаты вашей покупки на сумму {amount} {currency} наступает {due_date}. Спасибо!",
    val sms1DayBefore: String = "Здравствуйте, {customer_name}! Напоминаем, что завтра наступает срок оплаты вашего долга в размере {amount} {currency}.",
    val smsDueDay: String = "Здравствуйте, {customer_name}! Сегодня наступает срок оплаты вашей задолженности в размере {amount} {currency}. Спасибо!",
    val smsOverdue: String = "Здравствуйте, {customer_name}! Срок оплаты вашей задолженности в размере {amount} {currency} истек. Пожалуйста, произведите оплату."
)
