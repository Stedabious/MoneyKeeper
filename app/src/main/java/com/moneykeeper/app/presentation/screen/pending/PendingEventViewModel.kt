package com.moneykeeper.app.presentation.screen.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneykeeper.app.data.repository.CategoryRepository
import com.moneykeeper.app.data.repository.PendingEventRepository
import com.moneykeeper.app.data.repository.TransactionRepository
import com.moneykeeper.app.domain.model.Category
import com.moneykeeper.app.domain.model.PendingEvent
import com.moneykeeper.app.domain.model.Transaction
import com.moneykeeper.app.domain.model.TransactionSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingEventUiState(val events: List<PendingEvent> = emptyList())

@HiltViewModel
class PendingEventViewModel @Inject constructor(
    private val pendingEventRepository: PendingEventRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<PendingEventUiState> = pendingEventRepository.getPending()
        .map { PendingEventUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingEventUiState())

    val expenseCategories: StateFlow<List<Category>> = categoryRepository.getByType("EXPENSE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun confirm(event: PendingEvent, categoryId: Long) {
        val amount = event.amount ?: return
        viewModelScope.launch {
            transactionRepository.insert(
                Transaction(
                    amount = amount,
                    currency = event.currency,
                    categoryId = categoryId,
                    merchant = event.merchant ?: "",
                    source = TransactionSource.NOTIFICATION,
                    transactionDate = event.eventTime,
                )
            )
            pendingEventRepository.confirm(event.id)
        }
    }

    fun reject(event: PendingEvent) {
        viewModelScope.launch { pendingEventRepository.reject(event.id) }
    }
}
