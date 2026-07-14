import sys

def rewrite():
    with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
        lines = f.readlines()
        
    out = []
    
    # We will find HotelManagementTab and insert the early return style for LazyColumn
    
    in_hotel = False
    for i, line in enumerate(lines):
        if "fun HotelManagementTab(" in line:
            in_hotel = True
        
        if in_hotel and "Box(modifier = Modifier.fillMaxSize()) {" in line:
            # We found the Box. Let's see if we can just wrap the Box contents
            pass
            
        out.append(line)
        
rewrite()
