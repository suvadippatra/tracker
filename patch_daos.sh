#!/bin/bash
python3 -c '
import re

with open("app/src/main/java/com/example/data/dao/Daos.kt", "r") as f:
    content = f.read()

# TransactionDao
content = content.replace("FROM hotel_transactions WHERE hotelId = :hotelId ORDER BY timestamp DESC", "FROM hotel_transactions WHERE hotelId = :hotelId AND isDeleted = 0 ORDER BY timestamp DESC")
content = content.replace("FROM hotel_transactions WHERE hotelId = :hotelId AND timestamp >= :start AND timestamp <= :end", "FROM hotel_transactions WHERE hotelId = :hotelId AND isDeleted = 0 AND timestamp >= :start AND timestamp <= :end")
content = content.replace("FROM hotel_transactions ORDER BY timestamp DESC", "FROM hotel_transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
content = re.sub(r"fun getAllTransactions\(\): Flow<List<HotelTransactionEntity>>", "fun getAllTransactions(): Flow<List<HotelTransactionEntity>>\n    @Query(\"SELECT * FROM hotel_transactions WHERE isDeleted = 1 ORDER BY timestamp DESC\")\n    fun getDeletedTransactions(): Flow<List<HotelTransactionEntity>>", content)

# ExtraExpenseDao
content = content.replace("FROM extra_expenses ORDER BY timestamp DESC", "FROM extra_expenses WHERE isDeleted = 0 ORDER BY timestamp DESC")
content = content.replace("FROM extra_expenses WHERE timestamp >= :start AND timestamp <= :end", "FROM extra_expenses WHERE isDeleted = 0 AND timestamp >= :start AND timestamp <= :end")
content = re.sub(r"fun getAllExpenses\(\): Flow<List<ExtraExpenseEntity>>", "fun getAllExpenses(): Flow<List<ExtraExpenseEntity>>\n    @Query(\"SELECT * FROM extra_expenses WHERE isDeleted = 1 ORDER BY timestamp DESC\")\n    fun getDeletedExpenses(): Flow<List<ExtraExpenseEntity>>", content)

# MeterDao
content = re.sub(r"fun getAllUtilityBills\(\): Flow<List<UtilityBill>>", "fun getAllUtilityBills(): Flow<List<UtilityBill>>\n    @Query(\"SELECT * FROM utility_bills WHERE isDeleted = 1 ORDER BY startDate DESC\")\n    fun getDeletedUtilityBills(): Flow<List<UtilityBill>>", content)
content = content.replace("FROM utility_bills ORDER BY startDate DESC", "FROM utility_bills WHERE isDeleted = 0 ORDER BY startDate DESC")
content = content.replace("FROM meter_readings WHERE isDeleted = 1 ORDER BY readingDate ASC", "FROM meter_readings WHERE isDeleted = 1 ORDER BY readingDate DESC") # Wait, actually we can just add a getter for deleted meter readings
content = re.sub(r"fun softDeleteReading\(id: String\)", "fun softDeleteReading(id: String)\n    @Query(\"SELECT * FROM meter_readings WHERE isDeleted = 1 ORDER BY readingDate DESC\")\n    fun getDeletedReadings(): Flow<List<MeterReading>>", content)

with open("app/src/main/java/com/example/data/dao/Daos.kt", "w") as f:
    f.write(content)
'
