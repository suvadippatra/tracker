import re

with open("app/src/main/java/com/example/data/DataTransferManager.kt", "r") as f:
    content = f.read()
    
# we want to print importJson method
importJson = re.search(r'suspend fun importJson\(.*?\).*?\{.*', content, re.DOTALL)
if importJson:
    print(importJson.group(0)[:1000]) # Print first 1000 chars of importJson
