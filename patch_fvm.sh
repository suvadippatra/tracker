#!/bin/bash
python3 -c '
import re

with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

# Transactions
content = content.replace("fun deleteTransaction(transaction: HotelTransactionEntity) {", "fun deleteTransaction(transaction: HotelTransactionEntity) {\n        viewModelScope.launch {\n            repository.updateTransaction(transaction.copy(isDeleted = true))\n            updateWidgets()\n        }\n    }\n\n    fun restoreTransaction(transaction: HotelTransactionEntity) {\n        viewModelScope.launch {\n            repository.updateTransaction(transaction.copy(isDeleted = false))\n            updateWidgets()\n        }\n    }\n\n    fun deleteTransactionPermanently(transaction: HotelTransactionEntity) {")

# ExtraExpenses
content = content.replace("fun deleteExtraExpense(expense: ExtraExpenseEntity) {\n        val accounts = allBankAccounts.value\n        viewModelScope.launch {\n            repository.deleteExpenseAndRevert(expense, accounts)\n            updateWidgets()\n        }\n    }", "fun deleteExtraExpense(expense: ExtraExpenseEntity) {\n        val accounts = allBankAccounts.value\n        viewModelScope.launch {\n            repository.softDeleteExpenseAndRevert(expense, accounts)\n            updateWidgets()\n        }\n    }\n\n    fun restoreExtraExpense(expense: ExtraExpenseEntity) {\n        val accounts = allBankAccounts.value\n        viewModelScope.launch {\n            repository.restoreExpenseAndApply(expense, accounts)\n            updateWidgets()\n        }\n    }\n\n    fun deleteExtraExpensePermanently(expense: ExtraExpenseEntity) {\n        viewModelScope.launch {\n            repository.deleteExpensePermanently(expense)\n            updateWidgets()\n        }\n    }")

# Expose Deleted items flow
deleted_flows = """
    val deletedHotels: StateFlow<List<HotelEntity>> = repository.getDeletedHotels()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedBankAccounts: StateFlow<List<BankAccountEntity>> = repository.getDeletedBankAccounts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedTransactions: StateFlow<List<HotelTransactionEntity>> = repository.getDeletedTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedExtraExpenses: StateFlow<List<ExtraExpenseEntity>> = repository.getDeletedExpenses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
"""
# insert before updateWidgets()
content = content.replace("    fun updateWidgets() {", deleted_flows + "\n    fun updateWidgets() {")


with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "w") as f:
    f.write(content)
'
