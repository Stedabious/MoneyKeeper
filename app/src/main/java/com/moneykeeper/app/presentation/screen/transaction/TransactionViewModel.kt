package com.moneykeeper.app.presentation.screen.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneykeeper.app.data.repository.CategoryRepository
import com.moneykeeper.app.data.repository.TransactionRepository
import com.moneykeeper.app.domain.model.Category
import com.moneykeeper.app.domain.model.Transaction
import com.moneykeeper.app.domain.model.EventSource
import com.moneykeeper.app.domain.model.TransactionSource
import com.moneykeeper.app.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AddTransactionUiState(
    val displayAmount: String = "0",
    val merchant: String = "",
    val note: String = "",
    val selectedCategoryId: Long = 8L,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val transactionDate: Long = startOfToday(),
    val storedValue: Double? = null,
    val pendingOp: Char? = null,
    val freshInput: Boolean = true,
    val existingTransactionId: Long = 0L,
)

private fun startOfToday(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val expenseCategories: StateFlow<List<Category>> = categoryRepository.getByType("EXPENSE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategories: StateFlow<List<Category>> = categoryRepository.getByType("INCOME")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        val transactionId = savedStateHandle.get<Long>("transactionId") ?: 0L
        if (transactionId > 0L) {
            viewModelScope.launch {
                val tx = transactionRepository.getById(transactionId) ?: return@launch
                _uiState.value = AddTransactionUiState(
                    displayAmount = formatNum(tx.amount),
                    merchant = tx.merchant,
                    note = tx.note,
                    selectedCategoryId = tx.categoryId,
                    transactionType = tx.transactionType,
                    transactionDate = tx.transactionDate,
                    existingTransactionId = transactionId,
                    freshInput = false,
                )
            }
        }
    }

    fun onMerchantChanged(v: String) { _uiState.value = _uiState.value.copy(merchant = v) }
    fun onNoteChanged(v: String) { _uiState.value = _uiState.value.copy(note = v) }
    fun onCategorySelected(id: Long) { _uiState.value = _uiState.value.copy(selectedCategoryId = id) }

    fun onTransactionTypeChanged(type: TransactionType) {
        val currentId = _uiState.value.selectedCategoryId
        // Auto-switch to default category if current doesn't match new type
        val newCategoryId = when (type) {
            TransactionType.EXPENSE -> if (currentId > 8L) 8L else currentId
            TransactionType.INCOME  -> if (currentId <= 8L) 14L else currentId
        }
        _uiState.value = _uiState.value.copy(transactionType = type, selectedCategoryId = newCategoryId)
    }

    fun onDateChanged(utcMidnightMillis: Long) {
        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        utcCal.timeInMillis = utcMidnightMillis
        val localCal = Calendar.getInstance()
        localCal.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        localCal.set(Calendar.MILLISECOND, 0)
        _uiState.value = _uiState.value.copy(transactionDate = localCal.timeInMillis)
    }

    fun onNumpadKey(key: String) {
        val s = _uiState.value
        _uiState.value = when (key) {
            "⌫" -> {
                val cur = if (s.freshInput) "0" else s.displayAmount
                val next = if (cur.length <= 1) "0" else cur.dropLast(1)
                s.copy(displayAmount = next, freshInput = next == "0")
            }
            "+", "-" -> {
                val cur = s.displayAmount.toDoubleOrNull() ?: 0.0
                val result = if (s.storedValue != null && s.pendingOp != null && !s.freshInput)
                    compute(s.storedValue, cur, s.pendingOp) else cur
                s.copy(displayAmount = formatNum(result), storedValue = result, pendingOp = key[0], freshInput = true)
            }
            "=" -> {
                if (s.storedValue != null && s.pendingOp != null) {
                    val cur = s.displayAmount.toDoubleOrNull() ?: 0.0
                    val result = compute(s.storedValue, cur, s.pendingOp)
                    s.copy(displayAmount = formatNum(result), storedValue = null, pendingOp = null, freshInput = true)
                } else s
            }
            "." -> {
                val cur = if (s.freshInput) "0" else s.displayAmount
                if (!cur.contains('.')) s.copy(displayAmount = "$cur.", freshInput = false) else s
            }
            "00" -> {
                if (!s.freshInput && s.displayAmount != "0" && !s.displayAmount.contains('.')) {
                    val next = s.displayAmount + "00"
                    if (next.length <= 9) s.copy(displayAmount = next) else s
                } else s
            }
            else -> {
                val cur = if (s.freshInput) "" else s.displayAmount
                val next = if (cur.isEmpty() || cur == "0") key else cur + key
                if (next.substringBefore('.').length <= 8) s.copy(displayAmount = next, freshInput = false) else s
            }
        }
    }

    private fun compute(a: Double, b: Double, op: Char) = when (op) {
        '+' -> a + b
        '-' -> a - b
        else -> a + b
    }

    private fun formatNum(v: Double): String {
        val asLong = v.toLong()
        return if (asLong.toDouble() == v) asLong.toString()
        else v.toBigDecimal().stripTrailingZeros().toPlainString()
    }

    fun save() {
        val s = _uiState.value
        val amount = s.displayAmount.toDoubleOrNull()?.takeIf { it > 0 } ?: return
        _uiState.value = s.copy(isSaving = true)
        viewModelScope.launch {
            if (s.existingTransactionId > 0L) {
                val existing = transactionRepository.getById(s.existingTransactionId) ?: return@launch
                transactionRepository.update(
                    existing.copy(
                        amount = amount,
                        categoryId = s.selectedCategoryId,
                        merchant = s.merchant.trim(),
                        note = s.note.trim(),
                        transactionType = s.transactionType,
                        transactionDate = s.transactionDate,
                    )
                )
            } else {
                transactionRepository.insert(
                    Transaction(
                        amount = amount,
                        categoryId = s.selectedCategoryId,
                        merchant = s.merchant.trim(),
                        note = s.note.trim(),
                        source = TransactionSource.MANUAL,
                        eventSource = EventSource.MANUAL_INPUT,
                        transactionType = s.transactionType,
                        transactionDate = s.transactionDate,
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }
}
