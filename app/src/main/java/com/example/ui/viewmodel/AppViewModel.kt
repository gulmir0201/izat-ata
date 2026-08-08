package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PosRepository(database.posDao())
        
        // Populate default database tables with premium starter data if empty
        viewModelScope.launch {
            prepopulateDatabaseIfNeeded()
        }
    }

    // --- Core Database Flows ---
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProducts: StateFlow<List<Product>> = repository.activeProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<Debt>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smsLogs: StateFlow<List<SmsLog>> = repository.allSmsLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storeSettings: StateFlow<StoreSettings?> = repository.storeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryMovements: StateFlow<List<InventoryMovement>> = repository.allInventoryMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- POS Checkout Shopping Cart States ---
    val cart = mutableStateMapOf<Product, Double>()
    
    val selectedCustomer = mutableStateOf<Customer?>(null)
    val checkoutDiscount = mutableStateOf(0.0)
    val cashReceived = mutableStateOf(0.0)
    val paymentMethod = mutableStateOf("Наличные") // "Наличные", "Карта", "Смешанная", "В долг"
    val debtNotes = mutableStateOf("")
    val dueDate = mutableStateOf(System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000) // Default 14 days

    // State for latest completed checkout receipt to show print dialog dialog
    val lastCompletedSale = mutableStateOf<Sale?>(null)
    val showCheckoutSuccessDialog = mutableStateOf(false)

    // --- POS Cart Operations ---
    fun addToCart(product: Product, quantity: Double = 1.0) {
        val currentQty = cart[product] ?: 0.0
        val newQty = (currentQty + quantity).coerceAtLeast(0.0)
        if (newQty == 0.0) {
            cart.remove(product)
        } else {
            cart[product] = newQty
        }
    }

    fun updateCartQuantity(product: Product, quantity: Double) {
        if (quantity <= 0.0) {
            cart.remove(product)
        } else {
            cart[product] = quantity
        }
    }

    fun removeFromCart(product: Product) {
        cart.remove(product)
    }

    fun clearCart() {
        cart.clear()
        selectedCustomer.value = null
        checkoutDiscount.value = 0.0
        cashReceived.value = 0.0
        paymentMethod.value = "Наличные"
        debtNotes.value = ""
        dueDate.value = System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000
    }

    val cartSubtotal: Double
        get() = cart.entries.sumOf { it.key.sellingPrice * it.value }

    val cartTotal: Double
        get() = (cartSubtotal - checkoutDiscount.value).coerceAtLeast(0.0)

    val changeAmount: Double
        get() = if (paymentMethod.value == "Наличные" && cashReceived.value > 0.0) {
            (cashReceived.value - cartTotal).coerceAtLeast(0.0)
        } else {
            0.0
        }

    fun generateReceiptNumber(): String {
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        return "IZ-" + sdf.format(Date())
    }

    // --- POS Checkout Action ---
    fun processCheckout() {
        if (cart.isEmpty()) return

        viewModelScope.launch {
            val receiptNo = generateReceiptNumber()
            val sale = repository.completeSale(
                receiptNumber = receiptNo,
                customerId = selectedCustomer.value?.id,
                cartItems = cart.map { Pair(it.key, it.value) },
                totalAmount = cartTotal,
                discount = checkoutDiscount.value,
                paymentMethod = paymentMethod.value,
                cashGiven = if (paymentMethod.value == "Наличные") cashReceived.value else cartTotal,
                change = changeAmount,
                dueDate = if (paymentMethod.value == "В долг") dueDate.value else null,
                debtNotes = debtNotes.value
            )
            lastCompletedSale.value = sale
            showCheckoutSuccessDialog.value = true
            clearCart()
        }
    }

    // --- Category Actions ---
    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name))
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    // --- Product Actions ---
    fun addProduct(
        name: String,
        categoryId: Long?,
        barcode: String,
        sellingPrice: Double,
        costPrice: Double,
        stockQuantity: Double,
        minStock: Double,
        unit: String,
        supplierId: Long?,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                categoryId = categoryId,
                barcode = barcode,
                sellingPrice = sellingPrice,
                costPrice = costPrice,
                stockQuantity = stockQuantity,
                minStock = minStock,
                unit = unit,
                supplierId = supplierId,
                isActive = isActive
            )
            val insertedId = repository.insertProduct(product)
            // Log movement
            repository.allInventoryMovements // trigger flow update
            AppDatabase.getDatabase(getApplication()).posDao().insertInventoryMovement(
                InventoryMovement(
                    productId = insertedId,
                    quantity = stockQuantity,
                    type = "Приход",
                    reason = "Первичное поступление"
                )
            )
        }
    }

    fun updateProduct(product: Product, stockAdjustment: Double = 0.0) {
        viewModelScope.launch {
            repository.updateProduct(product)
            if (stockAdjustment != 0.0) {
                AppDatabase.getDatabase(getApplication()).posDao().insertInventoryMovement(
                    InventoryMovement(
                        productId = product.id,
                        quantity = stockAdjustment,
                        type = if (stockAdjustment > 0.0) "Приход" else "Расход",
                        reason = "Редактирование остатка"
                    )
                )
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
        }
    }

    // --- Customer Actions ---
    fun addCustomer(name: String, phone: String, address: String, note: String) {
        viewModelScope.launch {
            repository.insertCustomer(Customer(name = name, phone = phone, address = address, note = note))
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
        }
    }

    // --- Debt Payments ---
    fun submitDebtRepayment(debtId: Long, amount: Double, paymentMethod: String, notes: String) {
        viewModelScope.launch {
            repository.payDebt(debtId, amount, paymentMethod, notes)
        }
    }

    // --- Settings Saving ---
    fun saveSettings(settings: StoreSettings) {
        viewModelScope.launch {
            repository.saveStoreSettings(settings)
        }
    }

    // --- Manual SMS Notification ---
    fun triggerSmsNow(customerName: String, phone: String, messageText: String, type: String) {
        viewModelScope.launch {
            repository.sendSmsNotification(customerName, phone, messageText, type)
        }
    }

    // --- Pre-population logic ---
    private suspend fun prepopulateDatabaseIfNeeded() {
        val dao = AppDatabase.getDatabase(getApplication()).posDao()
        
        // 1. Pre-populate Settings if absent
        val existingSettings = dao.getSettingsSync()
        if (existingSettings == null) {
            dao.insertSettings(StoreSettings())
        }

        // 2. Pre-populate Categories if absent
        val allCats = dao.getAllCategories().firstOrNull() ?: emptyList()
        if (allCats.isEmpty()) {
            val dairyId = dao.insertCategory(Category(name = "Молочные продукты"))
            val breadId = dao.insertCategory(Category(name = "Хлеб"))
            val drinksId = dao.insertCategory(Category(name = "Напитки"))
            val groceryId = dao.insertCategory(Category(name = "Бакалея"))
            val meatId = dao.insertCategory(Category(name = "Мясо"))
            val vegId = dao.insertCategory(Category(name = "Овощи"))
            val fruitId = dao.insertCategory(Category(name = "Фрукты"))
            val sweetsId = dao.insertCategory(Category(name = "Сладости"))
            val houseId = dao.insertCategory(Category(name = "Бытовые товары"))

            // Pre-populate Products
            dao.insertProduct(
                Product(
                    name = "Молоко Веселый Молочник 2.5%, 1л",
                    categoryId = dairyId,
                    barcode = "4601234567890",
                    sellingPrice = 85.0,
                    costPrice = 65.0,
                    stockQuantity = 45.0,
                    minStock = 5.0,
                    unit = "шт"
                )
            )
            dao.insertProduct(
                Product(
                    name = "Кефир Веселый Молочник 3.2%, 1л",
                    categoryId = dairyId,
                    barcode = "4601234567899",
                    sellingPrice = 90.0,
                    costPrice = 70.0,
                    stockQuantity = 20.0,
                    minStock = 5.0,
                    unit = "шт"
                )
            )
            dao.insertProduct(
                Product(
                    name = "Батон Нарезной",
                    categoryId = breadId,
                    barcode = "4601234567891",
                    sellingPrice = 30.0,
                    costPrice = 22.0,
                    stockQuantity = 60.0,
                    minStock = 10.0,
                    unit = "шт"
                )
            )
            dao.insertProduct(
                Product(
                    name = "Coca-Cola 1.5л",
                    categoryId = drinksId,
                    barcode = "4601234567892",
                    sellingPrice = 95.0,
                    costPrice = 72.0,
                    stockQuantity = 30.0,
                    minStock = 5.0,
                    unit = "шт"
                )
            )
            dao.insertProduct(
                Product(
                    name = "Сахар-песок (весовой)",
                    categoryId = groceryId,
                    barcode = "4601234567893",
                    sellingPrice = 78.0,
                    costPrice = 62.0,
                    stockQuantity = 120.0,
                    minStock = 20.0,
                    unit = "кг"
                )
            )
            dao.insertProduct(
                Product(
                    name = "Макароны Шебекинские 450г",
                    categoryId = groceryId,
                    barcode = "4601234567894",
                    sellingPrice = 65.0,
                    costPrice = 48.0,
                    stockQuantity = 80.0,
                    minStock = 15.0,
                    unit = "шт"
                )
            )
            dao.insertProduct(
                Product(
                    name = "Картофель свежий",
                    categoryId = vegId,
                    barcode = "4601234567895",
                    sellingPrice = 45.0,
                    costPrice = 30.0,
                    stockQuantity = 250.0,
                    minStock = 50.0,
                    unit = "кг"
                )
            )
            dao.insertProduct(
                Product(
                    name = "Яблоки Ред Делишес",
                    categoryId = fruitId,
                    barcode = "4601234567896",
                    sellingPrice = 130.0,
                    costPrice = 95.0,
                    stockQuantity = 60.0,
                    minStock = 10.0,
                    unit = "кг"
                )
            )
            dao.insertProduct(
                Product(
                    name = "Конфеты Степ золотой, 1кг",
                    categoryId = sweetsId,
                    barcode = "4601234567897",
                    sellingPrice = 380.0,
                    costPrice = 290.0,
                    stockQuantity = 15.0,
                    minStock = 3.0,
                    unit = "кг"
                )
            )

            // Pre-populate Customers
            val cust1 = dao.insertCustomer(
                Customer(
                    name = "Алишер Маматов",
                    phone = "+996 (700) 11-22-33",
                    address = "мкр. Асанбай, д. 12, кв. 4",
                    note = "Постоянный покупатель, платит вовремя",
                    totalDebt = 1250.0,
                    nextPaymentDate = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000
                )
            )
            val cust2 = dao.insertCustomer(
                Customer(
                    name = "Бакыт Асанов",
                    phone = "+996 (772) 77-88-99",
                    address = "ул. Советская, д. 156",
                    note = "Берет часто в долг",
                    totalDebt = 3500.0,
                    nextPaymentDate = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000 // Overdue debt
                )
            )
            val cust3 = dao.insertCustomer(
                Customer(
                    name = "Гульнара Садыкова",
                    phone = "+996 (555) 44-55-66",
                    address = "ул. Киевская, д. 42",
                    note = "Соседка",
                    totalDebt = 0.0
                )
            )

            // Create initial sales
            val sale1Id = dao.insertSale(
                Sale(
                    receiptNumber = "IZ-260801121015",
                    customerId = cust1,
                    totalAmount = 1250.0,
                    discount = 0.0,
                    paymentMethod = "В долг",
                    isDebt = true,
                    createdAt = System.currentTimeMillis() - 4 * 24 * 60 * 60 * 1000
                )
            )
            dao.insertSaleItem(
                SaleItem(
                    saleId = sale1Id,
                    productId = 5,
                    productName = "Сахар-песок (весовой)",
                    quantity = 10.0,
                    price = 78.0,
                    costPrice = 62.0,
                    unit = "кг",
                    totalAmount = 780.0
                )
            )
            dao.insertSaleItem(
                SaleItem(
                    saleId = sale1Id,
                    productId = 1,
                    productName = "Молоко Веселый Молочник 2.5%, 1л",
                    quantity = 5.0,
                    price = 85.0,
                    costPrice = 65.0,
                    unit = "шт",
                    totalAmount = 425.0
                )
            )

            // Create debt record for sale1
            dao.insertDebt(
                Debt(
                    customerId = cust1,
                    saleId = sale1Id,
                    amount = 1250.0,
                    remainingAmount = 1250.0,
                    dueDate = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000,
                    notes = "Закупился на неделю",
                    status = "Ожидает оплаты",
                    createdAt = System.currentTimeMillis() - 4 * 24 * 60 * 60 * 1000
                )
            )

            val sale2Id = dao.insertSale(
                Sale(
                    receiptNumber = "IZ-260802094200",
                    customerId = cust2,
                    totalAmount = 3500.0,
                    discount = 0.0,
                    paymentMethod = "В долг",
                    isDebt = true,
                    createdAt = System.currentTimeMillis() - 16 * 24 * 60 * 60 * 1000
                )
            )
            dao.insertSaleItem(
                SaleItem(
                    saleId = sale2Id,
                    productId = 9,
                    productName = "Конфеты Степ золотой, 1кг",
                    quantity = 5.0,
                    price = 380.0,
                    costPrice = 290.0,
                    unit = "кг",
                    totalAmount = 1900.0
                )
            )
            dao.insertSaleItem(
                SaleItem(
                    saleId = sale2Id,
                    productId = 3,
                    productName = "Батон Нарезной",
                    quantity = 10.0,
                    price = 30.0,
                    costPrice = 22.0,
                    unit = "шт",
                    totalAmount = 300.0
                )
            )

            // Create debt record for sale2 (Overdue)
            dao.insertDebt(
                Debt(
                    customerId = cust2,
                    saleId = sale2Id,
                    amount = 3500.0,
                    remainingAmount = 3500.0,
                    dueDate = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000,
                    notes = "День рождения сына",
                    status = "Просрочен",
                    createdAt = System.currentTimeMillis() - 16 * 24 * 60 * 60 * 1000
                )
            )

            // Create cash sales
            val sale3Id = dao.insertSale(
                Sale(
                    receiptNumber = "IZ-260807183012",
                    customerId = null,
                    totalAmount = 570.0,
                    discount = 15.0,
                    paymentMethod = "Наличные",
                    isDebt = false,
                    cashAmountGiven = 1000.0,
                    changeAmount = 430.0,
                    createdAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000 // Today
                )
            )
            dao.insertSaleItem(
                SaleItem(
                    saleId = sale3Id,
                    productId = 4,
                    productName = "Coca-Cola 1.5л",
                    quantity = 6.0,
                    price = 95.0,
                    costPrice = 72.0,
                    unit = "шт",
                    totalAmount = 570.0
                )
            )
        }
    }
}
