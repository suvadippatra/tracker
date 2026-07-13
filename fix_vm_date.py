with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

setter = """
    fun setAddMealDate(date: Long) {
        _addMealDate.value = date
    }
"""

if "fun setAddMealDate" not in content:
    content = content.replace("val addMealDate = _addMealDate.asStateFlow()", "val addMealDate = _addMealDate.asStateFlow()" + setter)
    with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "w") as f:
        f.write(content)
