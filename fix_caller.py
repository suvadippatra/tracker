import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

# Replace any HotelDetailsBottomSheet caller missing viewModel = viewModel
lines = content.split('\n')
for i, line in enumerate(lines):
    if "HotelDetailsBottomSheet(" in line and "fun HotelDetailsBottomSheet" not in line:
        # Check next few lines if viewModel is passed
        has_vm = False
        for j in range(i, min(i+10, len(lines))):
            if "viewModel =" in lines[j]:
                has_vm = True
                break
        if not has_vm:
            # We need to add it
            for j in range(i, min(i+10, len(lines))):
                if "isBengali =" in lines[j]:
                    lines[j] = lines[j] + "\n            viewModel = viewModel,"
                    break

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write("\n".join(lines))
    
print("Fixed missing viewModel parameters")
