import re

with open('app/src/main/java/com/example/ui/viewmodels/StoreViewModel.kt', 'r') as f:
    content = f.read()

vm_add = '''    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateExpense(expense: Expense, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            onSuccess()
        }
    }

    fun archiveExpense(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.archiveExpense(id)
            onSuccess()
        }
    }'''

content = content.replace('    fun addExpense(', vm_add + '\n\n    fun addExpense(')

with open('app/src/main/java/com/example/ui/viewmodels/StoreViewModel.kt', 'w') as f:
    f.write(content)
