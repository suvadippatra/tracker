#!/bin/bash
python3 -c '
import re

with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "r") as f:
    content = f.read()

getters = """
    fun getDeletedHotels(): Flow<List<HotelEntity>> = hotelDao.getDeletedHotels()
    fun getDeletedBankAccounts(): Flow<List<BankAccountEntity>> = bankAccountDao.getDeletedBankAccounts()
    fun getDeletedTransactions(): Flow<List<HotelTransactionEntity>> = transactionDao.getDeletedTransactions()
    fun getDeletedExpenses(): Flow<List<ExtraExpenseEntity>> = extraExpenseDao.getDeletedExpenses()
    fun getDeletedUtilityBills(): Flow<List<com.example.data.model.UtilityBill>> = meterDao.getDeletedUtilityBills()
    fun getDeletedReadings(): Flow<List<com.example.data.model.MeterReading>> = meterDao.getDeletedReadings()
"""

content = content.replace("    fun getAllHotels(): Flow<List<HotelEntity>> = hotelDao.getAllHotels()", "    fun getAllHotels(): Flow<List<HotelEntity>> = hotelDao.getAllHotels()" + getters)

# add updateTransaction
content = content.replace("suspend fun deleteTransaction(transaction: HotelTransactionEntity) {", "suspend fun updateTransaction(transaction: HotelTransactionEntity) {\n        transactionDao.updateTransaction(transaction)\n    }\n\n    suspend fun deleteTransaction(transaction: HotelTransactionEntity) {")


with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "w") as f:
    f.write(content)
'
