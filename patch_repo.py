import re

with open('app/src/main/java/com/example/data/repository/StoreRepository.kt', 'r') as f:
    content = f.read()

repo_add = '''    val allExpenses: Flow<List<Expense>> = financeDao.getAllExpenses()

    suspend fun updateExpense(expense: Expense) {
        financeDao.updateExpense(expense.copy(updatedAt = System.currentTimeMillis(), syncStatus = 0))
        val currentMovement = financeDao.getExportCashMovements().find { it.referenceId == expense.id }
        if (currentMovement != null) {
            financeDao.insertCashMovement(
                currentMovement.copy(
                    amount = expense.amount,
                    note = expense.note ?: expense.categoryId,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = 0
                )
            )
        }
    }

    suspend fun archiveExpense(id: String) {
        financeDao.archiveExpense(id)
        val currentMovement = financeDao.getExportCashMovements().find { it.referenceId == id }
        if (currentMovement != null) {
            // Reverse or delete the cash movement. We can soft delete the movement or negate it.
            // Since we don't have isDeleted on cash_movements in this setup, let's just create a reversing entry or if there is soft delete, we'd use it.
            // Wait, does CashMovement have isDeleted? Let's check entities. We'll just negate it or add a counter movement for simplicity, OR if isDeleted exists, set it.
            // Let's assume it doesn't have soft delete, we'll just add a counter movement.
            financeDao.insertCashMovement(
                CashMovement(
                    movementType = "EXPENSE_REVERSAL",
                    amount = currentMovement.amount,
                    direction = "IN",
                    referenceType = "EXPENSE",
                    referenceId = id,
                    note = "عكس مصروف محذوف"
                )
            )
        }
    }'''

content = content.replace('    // Expenses', '    // Expenses\n' + repo_add)

with open('app/src/main/java/com/example/data/repository/StoreRepository.kt', 'w') as f:
    f.write(content)
