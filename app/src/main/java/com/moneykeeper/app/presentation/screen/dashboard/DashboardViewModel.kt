package com.moneykeeper.app.presentation.screen.dashboard

import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneykeeper.app.data.repository.CategoryRepository
import com.moneykeeper.app.data.repository.PendingEventRepository
import com.moneykeeper.app.data.repository.TransactionRepository
import com.moneykeeper.app.domain.model.Category
import com.moneykeeper.app.domain.model.Transaction
import com.moneykeeper.app.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategoryBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val fraction: Float,
)

data class DashboardUiState(
    val transactions: List<Transaction> = emptyList(),
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val pendingCount: Int = 0,
    val trashCount: Int = 0,
    val selectedDate: Long = startOfDay(System.currentTimeMillis()),
    val isToday: Boolean = true,
    val isNotificationListenerGranted: Boolean = true,
    val isBatteryOptimizationIgnored: Boolean = true,
)

internal fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun endOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

private fun monthRange(millis: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val from = cal.timeInMillis
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
    return from to cal.timeInMillis
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val pendingEventRepository: PendingEventRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    private val _isNLSGranted = MutableStateFlow(true)
    private val _isBattOptIgnored = MutableStateFlow(true)

    private val innerUiState = combine(
        combine(
            _selectedDate.flatMapLatest { date ->
                transactionRepository.getRealByDateRange(date, endOfDay(date))
            },
            _selectedDate.flatMapLatest { date ->
                val (from, to) = monthRange(date)
                transactionRepository.getRealByDateRange(from, to)
            },
            _selectedDate,
        ) { dayTx, monthTx, date -> Triple(dayTx, monthTx, date) },
        combine(
            pendingEventRepository.pendingCount(),
            transactionRepository.getTrashCount(),
            categoryRepository.getAll(),
        ) { pending, trash, cats -> Triple(pending, trash, cats) },
    ) { t1, t2 ->
        val (dayTx, monthTx, date) = t1
        val (pending, trash, cats) = t2
        val expenses = monthTx.filter { it.transactionType == TransactionType.EXPENSE }
        val income = monthTx.filter { it.transactionType == TransactionType.INCOME }
        val monthlyExpense = expenses.sumOf { it.amount }
        val monthlyIncome = income.sumOf { it.amount }
        val breakdown = buildBreakdown(expenses, cats)
        DashboardUiState(
            transactions = dayTx,
            monthlyExpense = monthlyExpense,
            monthlyIncome = monthlyIncome,
            categoryBreakdown = breakdown,
            pendingCount = pending,
            trashCount = trash,
            selectedDate = date,
            isToday = date == startOfDay(System.currentTimeMillis()),
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        innerUiState,
        _isNLSGranted,
        _isBattOptIgnored,
    ) { state, nlsGranted, battOptIgnored ->
        state.copy(
            isNotificationListenerGranted = nlsGranted,
            isBatteryOptimizationIgnored = battOptIgnored,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        viewModelScope.launch {
            transactionRepository.cleanOldTrash(System.currentTimeMillis() - DAY_MS)
        }
        refreshPermissionStatus()
    }

    fun refreshPermissionStatus() {
        _isNLSGranted.value = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isBattOptIgnored.value = pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun onPreviousDay() {
        _selectedDate.value -= 86_400_000L
    }

    fun onNextDay() {
        val today = startOfDay(System.currentTimeMillis())
        if (_selectedDate.value < today) _selectedDate.value += 86_400_000L
    }

    fun onDateSelected(yearMonth: Pair<Int, Int>) {
        val (year, month) = yearMonth
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        _selectedDate.value = cal.timeInMillis
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { transactionRepository.delete(transaction) }
    }

    private fun buildBreakdown(expenses: List<Transaction>, categories: List<Category>): List<CategoryBreakdown> {
        val total = expenses.sumOf { it.amount }
        if (total == 0.0) return emptyList()
        val catMap = categories.associateBy { it.id }
        return expenses
            .groupBy { it.categoryId }
            .mapNotNull { (catId, txs) ->
                val amount = txs.sumOf { it.amount }
                val name = catMap[catId]?.name ?: "其他"
                CategoryBreakdown(
                    categoryId = catId,
                    categoryName = name,
                    amount = amount,
                    fraction = (amount / total).toFloat(),
                )
            }
            .sortedByDescending { it.amount }
    }

    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
