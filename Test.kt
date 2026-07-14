import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*

@Composable
fun Test() {
    var showAddHotelDialog by remember { mutableStateOf(false) }
    var editingHotel: Any? by remember { mutableStateOf(null) }
    
    Box {
        Column {
            // inside column
        }
        
        if (showAddHotelDialog || editingHotel != null) {
            // dialog
        }
    }
}
