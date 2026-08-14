with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

bad_str = "            updateWidgets()\n        }\n        private fun copyToInternalStorage("
good_str = "            updateWidgets()\n        }\n    }\n\n    private fun copyToInternalStorage("

if bad_str in content:
    content = content.replace(bad_str, good_str)
else:
    # Try one line version
    bad_str2 = "updateWidgets()        }        private fun copyToInternalStorage("
    good_str2 = "updateWidgets()\n        }\n    }\n\n    private fun copyToInternalStorage("
    content = content.replace(bad_str2, good_str2)

# Also fix the end of file which had missing brace due to type errors
# The errors showed: Type 'MutableState<ByteArray?>' has no method 'getValue'
# That means I need to import androidx.compose.runtime.getValue and setValue!
# Let's check imports.
imports = """
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
"""
if "import androidx.compose.runtime.getValue" not in content:
    content = content.replace("import androidx.lifecycle.ViewModel", imports + "import androidx.lifecycle.ViewModel")

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
