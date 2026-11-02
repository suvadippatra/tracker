import re

with open("app/src/main/java/com/example/data/DataTransferManager.kt", "r") as f:
    content = f.read()

importJson = re.search(r'suspend fun importJson\(.*?\).*?\{.*', content, re.DOTALL)
if importJson:
    print(importJson.group(0)[5000:6000])
