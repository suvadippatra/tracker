import re
with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    text = f.read()

card_match = re.search(r'fun GlassCard.*?\n\}', text, re.DOTALL)
if card_match:
    print(card_match.group(0))
