#!/bin/bash
python3 -c '
import re

with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "r") as f:
    content = f.read()

new_delete = """    suspend fun softDeleteExpenseAndRevert(expense: ExtraExpenseEntity, accounts: List<BankAccountEntity>) {
        extraExpenseDao.insertExpense(expense.copy(isDeleted = true))
        accounts.find { it.id == expense.bankAccountId }?.let { account ->
            val change = if (expense.type == "INCOME") -expense.amount else expense.amount
            val updatedAccount = account.copy(balance = account.balance + change)
            bankAccountDao.updateBankAccount(updatedAccount)
        }
    }

    suspend fun restoreExpenseAndApply(expense: ExtraExpenseEntity, accounts: List<BankAccountEntity>) {
        extraExpenseDao.insertExpense(expense.copy(isDeleted = false))
        accounts.find { it.id == expense.bankAccountId }?.let { account ->
            val change = if (expense.type == "INCOME") expense.amount else -expense.amount
            val updatedAccount = account.copy(balance = account.balance + change)
            bankAccountDao.updateBankAccount(updatedAccount)
        }
    }

    suspend fun deleteExpensePermanently(expense: ExtraExpenseEntity) {
        extraExpenseDao.deleteExpense(expense)
    }"""

content = re.sub(r"    suspend fun deleteExpenseAndRevert\(expense: ExtraExpenseEntity, accounts: List<BankAccountEntity>\) \{[\s\S]*?\}", new_delete, content)

with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "w") as f:
    f.write(content)
'
