import re

with open('app/src/main/java/com/example/data/local/Daos.kt', 'r') as f:
    content = f.read()

new_expense_methods = '''    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("UPDATE expenses SET isDeleted = 1, updatedAt = :timestamp, syncStatus = 0 WHERE id = :id")
    suspend fun archiveExpense(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY expenseDate DESC")
    fun getAllExpenses(): Flow<List<Expense>>'''

content = content.replace('    @Insert(onConflict = OnConflictStrategy.REPLACE)\n    suspend fun insertExpense(expense: Expense)', new_expense_methods)

with open('app/src/main/java/com/example/data/local/Daos.kt', 'w') as f:
    f.write(content)
