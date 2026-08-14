import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

# Replace result.data with result.payload
content = content.replace("result.data", "result.payload")

# Fix the duplicate else block globally
bad_pattern = re.compile(r'\} else \{\s*android\.widget\.Toast\.makeText\(context, "Export failed", android\.widget\.Toast\.LENGTH_SHORT\)\.show\(\)\s*\}\s*\} else \{\s*android\.widget\.Toast\.makeText\(context, "Export failed", android\.widget\.Toast\.LENGTH_SHORT\)\.show\(\)\s*\}')
good_replacement = r"""} else {
    android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
}"""

# Wait, let's look at the literal text at 775
# The Toast text is identical. Let's just find `} else { ... Toast ... }` followed by `} else { ... Toast ... }` and replace it with just one.
# Wait, actually, the outer block is:
# viewModel.exportHotelViaQr(hotel.id) { result ->
#     if (result is com.example.data.ExportResult.Success) {
#         viewModel.setQrExportPayload(result.payload, java.util.UUID.randomUUID().toString())
#     } else {
#         android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
#     }
# } else {
#    android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
# }
# }

def fix_duplicate_else(text):
    import re
    # We find the specific pattern
    pattern = r'\}\s*else\s*\{\s*android\.widget\.Toast\.makeText\(context,\s*"Export failed",\s*android\.widget\.Toast\.LENGTH_SHORT\)\.show\(\)\s*\}\s*\}\s*else\s*\{\s*android\.widget\.Toast\.makeText\(context,\s*"Export failed",\s*android\.widget\.Toast\.LENGTH_SHORT\)\.show\(\)\s*\}'
    replacement = r'\} else \{\n                                                                            android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()\n                                                                        }\n                                                                    }'
    return re.sub(pattern, replacement, text)

content = fix_duplicate_else(content)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
