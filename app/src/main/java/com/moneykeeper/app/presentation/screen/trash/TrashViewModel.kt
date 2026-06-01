package com.moneykeeper.app.presentation.screen.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneykeeper.app.data.repository.TransactionRepository
import com.moneykeeper.app.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    val trashedItems: StateFlow<List<Transaction>> = transactionRepository.getTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(transaction: Transaction) {
        viewModelScope.launch { transactionRepository.restore(transaction.id) }
    }

    fun emptyTrash() {
        viewModelScope.launch { transactionRepository.emptyTrash() }
    }
}
