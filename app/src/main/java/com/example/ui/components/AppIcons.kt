package androidx.compose.material.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun buildCustomIcon(name: String, pathDataList: List<String>): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    for (d in pathDataList) {
        builder.addPath(
            pathData = addPathNodes(d),
            fill = SolidColor(Color(0xFF000000))
        )
    }
    return builder.build()
}

public val Icons.Filled.AccountBalance: ImageVector
    get() = _accountBalance ?: buildCustomIcon("Filled.AccountBalance", listOf("""M4 10v7h3v-7H4zm6.5 0v7h3v-7h-3zM2 22h20v-3H2v3zm15-12v7h3v-7h-3zM11.5 1 2 6v2h20V6l-10.5-5z""")).also { _accountBalance = it }
private var _accountBalance: ImageVector? = null

public val Icons.Filled.AccountCircle: ImageVector
    get() = _accountCircle ?: buildCustomIcon("Filled.AccountCircle", listOf("""M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 4c1.93 0 3.5 1.57 3.5 3.5S13.93 13 12 13s-3.5-1.57-3.5-3.5S10.07 6 12 6zm0 14c-2.03 0-4.43-.82-6.14-2.88C7.55 15.8 9.68 15 12 15s4.45.8 6.14 2.12C16.43 19.18 14.03 20 12 20z""")).also { _accountCircle = it }
private var _accountCircle: ImageVector? = null

public val Icons.Filled.AccountBalanceWallet: ImageVector
    get() = _accountBalanceWallet ?: buildCustomIcon("Filled.AccountBalanceWallet", listOf("""M21 18v1c0 1.1-.9 2-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14c1.1 0 2 .9 2 2v1h-9a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h9zm-9-2h10V8H12v8zm4-2.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z""")).also { _accountBalanceWallet = it }
private var _accountBalanceWallet: ImageVector? = null

public val Icons.Filled.AddBusiness: ImageVector
    get() = _addBusiness ?: buildCustomIcon("Filled.AddBusiness", listOf("""M15 17h2v-3h1v-2l-1-5H2l-1 5v2h1v6h9v-6h4v3zm-6 1H4v-4h5v4zM2 4h15v2H2z""", """M20 18v-3h-2v3h-3v2h3v3h2v-3h3v-2z""")).also { _addBusiness = it }
private var _addBusiness: ImageVector? = null

public val Icons.Filled.AddChart: ImageVector
    get() = _addChart ?: buildCustomIcon("Filled.AddChart", listOf("""M6 9.99h2v7H6zm8 3h2v4h-2zm-4-6h2v10h-2zM20 7V4h-2v3h-3v2h3v3h2V9h3V7zm-2 12H4V5h12V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-5h-2v5z""")).also { _addChart = it }
private var _addChart: ImageVector? = null

public val Icons.Filled.AddCircle: ImageVector
    get() = _addCircle ?: buildCustomIcon("Filled.AddCircle", listOf("""M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11h-4v4h-2v-4H7v-2h4V7h2v4h4v2z""")).also { _addCircle = it }
private var _addCircle: ImageVector? = null

public val Icons.Filled.Apartment: ImageVector
    get() = _apartment ?: buildCustomIcon("Filled.Apartment", listOf("""M17 11V3H7v4H3v14h8v-4h2v4h8V11h-4zM7 19H5v-2h2v2zm0-4H5v-2h2v2zm0-4H5V9h2v2zm4 4H9v-2h2v2zm0-4H9V9h2v2zm0-4H9V5h2v2zm4 8h-2v-2h2v2zm0-4h-2V9h2v2zm0-4h-2V5h2v2zm4 12h-2v-2h2v2zm0-4h-2v-2h2v2z""")).also { _apartment = it }
private var _apartment: ImageVector? = null

public val Icons.Filled.ArrowDownward: ImageVector
    get() = _arrowDownward ?: buildCustomIcon("Filled.ArrowDownward", listOf("""m20 12-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z""")).also { _arrowDownward = it }
private var _arrowDownward: ImageVector? = null

public val Icons.Filled.ArrowUpward: ImageVector
    get() = _arrowUpward ?: buildCustomIcon("Filled.ArrowUpward", listOf("""m4 12 1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z""")).also { _arrowUpward = it }
private var _arrowUpward: ImageVector? = null

public val Icons.Filled.AssignmentReturn: ImageVector
    get() = _assignmentReturn ?: buildCustomIcon("Filled.AssignmentReturn", listOf("""M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm4 12h-4v3l-5-5 5-5v3h4v4z""")).also { _assignmentReturn = it }
private var _assignmentReturn: ImageVector? = null

public val Icons.Filled.AttachFile: ImageVector
    get() = _attachFile ?: buildCustomIcon("Filled.AttachFile", listOf("""M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5a2.5 2.5 0 0 1 5 0v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5a2.5 2.5 0 0 0 5 0V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z""")).also { _attachFile = it }
private var _attachFile: ImageVector? = null

public val Icons.Filled.AutoAwesome: ImageVector
    get() = _autoAwesome ?: buildCustomIcon("Filled.AutoAwesome", listOf("""m19 9 1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z""")).also { _autoAwesome = it }
private var _autoAwesome: ImageVector? = null

public val Icons.Filled.Bolt: ImageVector
    get() = _bolt ?: buildCustomIcon("Filled.Bolt", listOf("""M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z""")).also { _bolt = it }
private var _bolt: ImageVector? = null

public val Icons.Filled.Brightness4: ImageVector
    get() = _brightness4 ?: buildCustomIcon("Filled.Brightness4", listOf("""M20 8.69V4h-4.69L12 .69 8.69 4H4v4.69L.69 12 4 15.31V20h4.69L12 23.31 15.31 20H20v-4.69L23.31 12 20 8.69zM12 18c-.89 0-1.74-.2-2.5-.55C11.56 16.5 13 14.42 13 12s-1.44-4.5-3.5-5.45C10.26 6.2 11.11 6 12 6c3.31 0 6 2.69 6 6s-2.69 6-6 6z""")).also { _brightness4 = it }
private var _brightness4: ImageVector? = null

public val Icons.Filled.BrightnessAuto: ImageVector
    get() = _brightnessAuto ?: buildCustomIcon("Filled.BrightnessAuto", listOf("""M10.85 12.65h2.3L12 9l-1.15 3.65zM20 8.69V4h-4.69L12 .69 8.69 4H4v4.69L.69 12 4 15.31V20h4.69L12 23.31 15.31 20H20v-4.69L23.31 12 20 8.69zM14.3 16l-.7-2h-3.2l-.7 2H7.8L11 7h2l3.2 9h-1.9z""")).also { _brightnessAuto = it }
private var _brightnessAuto: ImageVector? = null

public val Icons.Filled.Calculate: ImageVector
    get() = _calculate ?: buildCustomIcon("Filled.Calculate", listOf("""M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5.97 4.06L14.09 6l1.41 1.41L16.91 6l1.06 1.06-1.41 1.41 1.41 1.41-1.06 1.06-1.41-1.4-1.41 1.41-1.06-1.06 1.41-1.41-1.41-1.42zm-6.78.66h5v1.5h-5v-1.5zM11.5 16h-2v2H8v-2H6v-1.5h2v-2h1.5v2h2V16zm6.5 1.25h-5v-1.5h5v1.5zm0-2.5h-5v-1.5h5v1.5z""")).also { _calculate = it }
private var _calculate: ImageVector? = null

public val Icons.Filled.CalendarToday: ImageVector
    get() = _calendarToday ?: buildCustomIcon("Filled.CalendarToday", listOf("""M20 3h-1V1h-2v2H7V1H5v2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H4V8h16v13z""")).also { _calendarToday = it }
private var _calendarToday: ImageVector? = null

public val Icons.Filled.Call: ImageVector
    get() = _call ?: buildCustomIcon("Filled.Call", listOf("""M20.01 15.38c-1.23 0-2.42-.2-3.53-.56a.977.977 0 0 0-1.01.24l-1.57 1.97c-2.83-1.35-5.48-3.9-6.89-6.83l1.95-1.66c.27-.28.35-.67.24-1.02-.37-1.11-.56-2.3-.56-3.53 0-.54-.45-.99-.99-.99H4.19C3.65 3 3 3.24 3 3.99 3 13.28 10.73 21 20.01 21c.71 0 .99-.63.99-1.18v-3.45c0-.54-.45-.99-.99-.99z""")).also { _call = it }
private var _call: ImageVector? = null

public val Icons.Filled.CameraAlt: ImageVector
    get() = _cameraAlt ?: buildCustomIcon("Filled.CameraAlt", listOf("""M9 2 7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z""")).also { _cameraAlt = it }
private var _cameraAlt: ImageVector? = null

public val Icons.Filled.CardGiftcard: ImageVector
    get() = _cardGiftcard ?: buildCustomIcon("Filled.CardGiftcard", listOf("""M20 6h-2.18c.11-.31.18-.65.18-1a2.996 2.996 0 0 0-5.5-1.65l-.5.67-.5-.68C10.96 2.54 10.05 2 9 2 7.34 2 6 3.34 6 5c0 .35.07.69.18 1H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-5-2c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zM9 4c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm11 15H4v-2h16v2zm0-5H4V8h5.08L7 10.83 8.62 12 11 8.76l1-1.36 1 1.36L15.38 12 17 10.83 14.92 8H20v6z""")).also { _cardGiftcard = it }
private var _cardGiftcard: ImageVector? = null

public val Icons.Filled.ChevronLeft: ImageVector
    get() = _chevronLeft ?: buildCustomIcon("Filled.ChevronLeft", listOf("""M15.41 7.41 14 6l-6 6 6 6 1.41-1.41L10.83 12z""")).also { _chevronLeft = it }
private var _chevronLeft: ImageVector? = null

public val Icons.Filled.ChevronRight: ImageVector
    get() = _chevronRight ?: buildCustomIcon("Filled.ChevronRight", listOf("""M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z""")).also { _chevronRight = it }
private var _chevronRight: ImageVector? = null

public val Icons.Filled.CloudOff: ImageVector
    get() = _cloudOff ?: buildCustomIcon("Filled.CloudOff", listOf("""M19.35 10.04A7.49 7.49 0 0 0 12 4c-1.48 0-2.85.43-4.01 1.17l1.46 1.46a5.497 5.497 0 0 1 8.05 4.87v.5H19c1.66 0 3 1.34 3 3 0 1.13-.64 2.11-1.56 2.62l1.45 1.45C23.16 18.16 24 16.68 24 15c0-2.64-2.05-4.78-4.65-4.96zM3 5.27l2.75 2.74C2.56 8.15 0 10.77 0 14c0 3.31 2.69 6 6 6h11.73l2 2L21 20.73 4.27 4 3 5.27zM7.73 10l8 8H6c-2.21 0-4-1.79-4-4s1.79-4 4-4h1.73z""")).also { _cloudOff = it }
private var _cloudOff: ImageVector? = null

public val Icons.Filled.Computer: ImageVector
    get() = _computer ?: buildCustomIcon("Filled.Computer", listOf("""M20 18c1.1 0 1.99-.9 1.99-2L22 6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2H0v2h24v-2h-4zM4 6h16v10H4V6z""")).also { _computer = it }
private var _computer: ImageVector? = null

public val Icons.Filled.ContentCopy: ImageVector
    get() = _contentCopy ?: buildCustomIcon("Filled.ContentCopy", listOf("""M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z""")).also { _contentCopy = it }
private var _contentCopy: ImageVector? = null

public val Icons.Filled.ContentPaste: ImageVector
    get() = _contentPaste ?: buildCustomIcon("Filled.ContentPaste", listOf("""M19 2h-4.18C14.4.84 13.3 0 12 0c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm7 18H5V4h2v3h10V4h2v16z""")).also { _contentPaste = it }
private var _contentPaste: ImageVector? = null

public val Icons.Filled.DarkMode: ImageVector
    get() = _darkMode ?: buildCustomIcon("Filled.DarkMode", listOf("""M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 0 1-4.4 2.26 5.403 5.403 0 0 1-3.14-9.8c-.44-.06-.9-.1-1.36-.1z""")).also { _darkMode = it }
private var _darkMode: ImageVector? = null

public val Icons.Filled.Dashboard: ImageVector
    get() = _dashboard ?: buildCustomIcon("Filled.Dashboard", listOf("""M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z""")).also { _dashboard = it }
private var _dashboard: ImageVector? = null

public val Icons.Filled.DateRange: ImageVector
    get() = _dateRange ?: buildCustomIcon("Filled.DateRange", listOf("""M9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm2-7h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11z""")).also { _dateRange = it }
private var _dateRange: ImageVector? = null

public val Icons.Filled.DeleteForever: ImageVector
    get() = _deleteForever ?: buildCustomIcon("Filled.DeleteForever", listOf("""M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zm2.46-7.12 1.41-1.41L12 12.59l2.12-2.12 1.41 1.41L13.41 14l2.12 2.12-1.41 1.41L12 15.41l-2.12 2.12-1.41-1.41L10.59 14l-2.13-2.12zM15.5 4l-1-1h-5l-1 1H5v2h14V4z""")).also { _deleteForever = it }
private var _deleteForever: ImageVector? = null

public val Icons.Filled.DeleteOutline: ImageVector
    get() = _deleteOutline ?: buildCustomIcon("Filled.DeleteOutline", listOf("""M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5-1-1h-5l-1 1H5v2h14V4z""")).also { _deleteOutline = it }
private var _deleteOutline: ImageVector? = null

public val Icons.Filled.DeleteSweep: ImageVector
    get() = _deleteSweep ?: buildCustomIcon("Filled.DeleteSweep", listOf("""M15 16h4v2h-4zm0-8h7v2h-7zm0 4h6v2h-6zM3 18c0 1.1.9 2 2 2h6c1.1 0 2-.9 2-2V8H3v10zM14 5h-3l-1-1H6L5 5H2v2h12z""")).also { _deleteSweep = it }
private var _deleteSweep: ImageVector? = null

public val Icons.Filled.Download: ImageVector
    get() = _download ?: buildCustomIcon("Filled.Download", listOf("""M5 20h14v-2H5v2zM19 9h-4V3H9v6H5l7 7 7-7z""")).also { _download = it }
private var _download: ImageVector? = null

public val Icons.Filled.ElectricBolt: ImageVector
    get() = _electricBolt ?: buildCustomIcon("Filled.ElectricBolt", listOf("""M14.69 2.21 4.33 11.49c-.64.58-.28 1.65.58 1.73L13 14l-4.85 6.76c-.22.31-.19.74.08 1.01.3.3.77.31 1.08.02l10.36-9.28c.64-.58.28-1.65-.58-1.73L11 10l4.85-6.76c.22-.31.19-.74-.08-1.01a.77.77 0 0 0-1.08-.02z""")).also { _electricBolt = it }
private var _electricBolt: ImageVector? = null

public val Icons.Filled.ElectricMeter: ImageVector
    get() = _electricMeter ?: buildCustomIcon("Filled.ElectricMeter", listOf("""M12 2c-4.96 0-9 4.04-9 9 0 3.91 2.51 7.24 6 8.47V22h2v-2.06a8.262 8.262 0 0 0 2 0V22h2v-2.53c3.49-1.24 6-4.57 6-8.47 0-4.96-4.04-9-9-9zm2.25 12-3 3-1.5-1.5L11 14.25 9.75 13l3-3 1.5 1.5L13 12.75 14.25 14zM16 9H8V7h8v2z""")).also { _electricMeter = it }
private var _electricMeter: ImageVector? = null

public val Icons.Filled.Fastfood: ImageVector
    get() = _fastfood ?: buildCustomIcon("Filled.Fastfood", listOf("""M18.06 22.99h1.66c.84 0 1.53-.64 1.63-1.46L23 5.05h-5V1h-1.97v4.05h-4.97l.3 2.34c1.71.47 3.31 1.32 4.27 2.26 1.44 1.42 2.43 2.89 2.43 5.29v8.05zM1 21.99V21h15.03v.99c0 .55-.45 1-1.01 1H2.01c-.56 0-1.01-.45-1.01-1zm15.03-7c0-8-15.03-8-15.03 0h15.03zM1.02 17h15v2h-15z""")).also { _fastfood = it }
private var _fastfood: ImageVector? = null

public val Icons.Filled.Fingerprint: ImageVector
    get() = _fingerprint ?: buildCustomIcon("Filled.Fingerprint", listOf("""M17.81 4.47c-.08 0-.16-.02-.23-.06C15.66 3.42 14 3 12.01 3c-1.98 0-3.86.47-5.57 1.41-.24.13-.54.04-.68-.2a.506.506 0 0 1 .2-.68C7.82 2.52 9.86 2 12.01 2c2.13 0 3.99.47 6.03 1.52.25.13.34.43.21.67a.49.49 0 0 1-.44.28zM3.5 9.72a.499.499 0 0 1-.41-.79c.99-1.4 2.25-2.5 3.75-3.27C9.98 4.04 14 4.03 17.15 5.65c1.5.77 2.76 1.86 3.75 3.25a.5.5 0 0 1-.12.7c-.23.16-.54.11-.7-.12a9.388 9.388 0 0 0-3.39-2.94c-2.87-1.47-6.54-1.47-9.4.01-1.36.7-2.5 1.7-3.4 2.96-.08.14-.23.21-.39.21zm6.25 12.07a.47.47 0 0 1-.35-.15c-.87-.87-1.34-1.43-2.01-2.64-.69-1.23-1.05-2.73-1.05-4.34 0-2.97 2.54-5.39 5.66-5.39s5.66 2.42 5.66 5.39c0 .28-.22.5-.5.5s-.5-.22-.5-.5c0-2.42-2.09-4.39-4.66-4.39-2.57 0-4.66 1.97-4.66 4.39 0 1.44.32 2.77.93 3.85.64 1.15 1.08 1.64 1.85 2.42.19.2.19.51 0 .71-.11.1-.24.15-.37.15zm7.17-1.85c-1.19 0-2.24-.3-3.1-.89-1.49-1.01-2.38-2.65-2.38-4.39 0-.28.22-.5.5-.5s.5.22.5.5c0 1.41.72 2.74 1.94 3.56.71.48 1.54.71 2.54.71.24 0 .64-.03 1.04-.1.27-.05.53.13.58.41.05.27-.13.53-.41.58-.57.11-1.07.12-1.21.12zM14.91 22c-.04 0-.09-.01-.13-.02-1.59-.44-2.63-1.03-3.72-2.1a7.297 7.297 0 0 1-2.17-5.22c0-1.62 1.38-2.94 3.08-2.94 1.7 0 3.08 1.32 3.08 2.94 0 1.07.93 1.94 2.08 1.94s2.08-.87 2.08-1.94c0-3.77-3.25-6.83-7.25-6.83-2.84 0-5.44 1.58-6.61 4.03-.39.81-.59 1.76-.59 2.8 0 .78.07 2.01.67 3.61.1.26-.03.55-.29.64-.26.1-.55-.04-.64-.29a11.14 11.14 0 0 1-.73-3.96c0-1.2.23-2.29.68-3.24 1.33-2.79 4.28-4.6 7.51-4.6 4.55 0 8.25 3.51 8.25 7.83 0 1.62-1.38 2.94-3.08 2.94s-3.08-1.32-3.08-2.94c0-1.07-.93-1.94-2.08-1.94s-2.08.87-2.08 1.94c0 1.71.66 3.31 1.87 4.51.95.94 1.86 1.46 3.27 1.85.27.07.42.35.35.61-.05.23-.26.38-.47.38z""")).also { _fingerprint = it }
private var _fingerprint: ImageVector? = null

public val Icons.Filled.FlashOn: ImageVector
    get() = _flashOn ?: buildCustomIcon("Filled.FlashOn", listOf("""M7 2v11h3v9l7-12h-4l4-8z""")).also { _flashOn = it }
private var _flashOn: ImageVector? = null

public val Icons.Filled.Flight: ImageVector
    get() = _flight ?: buildCustomIcon("Filled.Flight", listOf("""M21 16v-2l-8-5V3.5c0-.83-.67-1.5-1.5-1.5S10 2.67 10 3.5V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z""")).also { _flight = it }
private var _flight: ImageVector? = null

public val Icons.Filled.Gavel: ImageVector
    get() = _gavel ?: buildCustomIcon("Filled.Gavel", listOf("""m5.25 8.069 2.83-2.827 14.134 14.15-2.83 2.827zm4.236-4.242L12.314.998l5.657 5.656-2.828 2.83zM.999 12.315l2.828-2.829 5.657 5.657-2.828 2.828zM1 21h12v2H1z""")).also { _gavel = it }
private var _gavel: ImageVector? = null

public val Icons.Filled.HelpOutline: ImageVector
    get() = _helpOutline ?: buildCustomIcon("Filled.HelpOutline", listOf("""M11 18h2v-2h-2v2zm1-16C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-2.21 0-4 1.79-4 4h2c0-1.1.9-2 2-2s2 .9 2 2c0 2-3 1.75-3 5h2c0-2.25 3-2.5 3-5 0-2.21-1.79-4-4-4z""")).also { _helpOutline = it }
private var _helpOutline: ImageVector? = null

public val Icons.Filled.History: ImageVector
    get() = _history ?: buildCustomIcon("Filled.History", listOf("""M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.954 8.954 0 0 0 13 21a9 9 0 0 0 0-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z""")).also { _history = it }
private var _history: ImageVector? = null

public val Icons.Filled.Hotel: ImageVector
    get() = _hotel ?: buildCustomIcon("Filled.Hotel", listOf("""M7 13c1.66 0 3-1.34 3-3S8.66 7 7 7s-3 1.34-3 3 1.34 3 3 3zm12-6h-8v7H3V5H1v15h2v-3h18v3h2v-9c0-2.21-1.79-4-4-4z""")).also { _hotel = it }
private var _hotel: ImageVector? = null

public val Icons.Filled.Image: ImageVector
    get() = _image ?: buildCustomIcon("Filled.Image", listOf("""M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z""")).also { _image = it }
private var _image: ImageVector? = null

public val Icons.Filled.Inbox: ImageVector
    get() = _inbox ?: buildCustomIcon("Filled.Inbox", listOf("""M19 3H4.99c-1.11 0-1.98.89-1.98 2L3 19c0 1.1.88 2 1.99 2H19c1.1 0 2-.9 2-2V5a2 2 0 0 0-2-2zm0 12h-4c0 1.66-1.35 3-3 3s-3-1.34-3-3H4.99V5H19v10z""")).also { _inbox = it }
private var _inbox: ImageVector? = null

public val Icons.Filled.Language: ImageVector
    get() = _language ?: buildCustomIcon("Filled.Language", listOf("""M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95a15.65 15.65 0 0 0-1.38-3.56A8.03 8.03 0 0 1 18.92 8zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56A7.987 7.987 0 0 1 5.08 16zm2.95-8H5.08a7.987 7.987 0 0 1 4.33-3.56A15.65 15.65 0 0 0 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95a8.03 8.03 0 0 1-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z""")).also { _language = it }
private var _language: ImageVector? = null

public val Icons.Filled.LightMode: ImageVector
    get() = _lightMode ?: buildCustomIcon("Filled.LightMode", listOf("""M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0a.996.996 0 0 0 0-1.41l-1.06-1.06zm1.06-10.96a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z""")).also { _lightMode = it }
private var _lightMode: ImageVector? = null

public val Icons.Filled.List: ImageVector
    get() = _list ?: buildCustomIcon("Filled.List", listOf("""M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z""")).also { _list = it }
private var _list: ImageVector? = null

public val Icons.Filled.LockOpen: ImageVector
    get() = _lockOpen ?: buildCustomIcon("Filled.LockOpen", listOf("""M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm6-9h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm0 12H6V10h12v10z""")).also { _lockOpen = it }
private var _lockOpen: ImageVector? = null

public val Icons.Filled.Map: ImageVector
    get() = _map ?: buildCustomIcon("Filled.Map", listOf("""m20.5 3-.16.03L15 5.1 9 3 3.36 4.9c-.21.07-.36.25-.36.48V20.5c0 .28.22.5.5.5l.16-.03L9 18.9l6 2.1 5.64-1.9c.21-.07.36-.25.36-.48V3.5c0-.28-.22-.5-.5-.5zM15 19l-6-2.11V5l6 2.11V19z""")).also { _map = it }
private var _map: ImageVector? = null

public val Icons.Filled.Mic: ImageVector
    get() = _mic ?: buildCustomIcon("Filled.Mic", listOf("""M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z""")).also { _mic = it }
private var _mic: ImageVector? = null

public val Icons.Filled.MonetizationOn: ImageVector
    get() = _monetizationOn ?: buildCustomIcon("Filled.MonetizationOn", listOf("""M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1.41 16.09V20h-2.67v-1.93c-1.71-.36-3.16-1.46-3.27-3.4h1.96c.1 1.05.82 1.87 2.65 1.87 1.96 0 2.4-.98 2.4-1.59 0-.83-.44-1.61-2.67-2.14-2.48-.6-4.18-1.62-4.18-3.67 0-1.72 1.39-2.84 3.11-3.21V4h2.67v1.95c1.86.45 2.79 1.86 2.85 3.39H14.3c-.05-1.11-.64-1.87-2.22-1.87-1.5 0-2.4.68-2.4 1.64 0 .84.65 1.39 2.67 1.91s4.18 1.39 4.18 3.91c-.01 1.83-1.38 2.83-3.12 3.16z""")).also { _monetizationOn = it }
private var _monetizationOn: ImageVector? = null

public val Icons.Filled.Movie: ImageVector
    get() = _movie ?: buildCustomIcon("Filled.Movie", listOf("""m18 4 2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z""")).also { _movie = it }
private var _movie: ImageVector? = null

public val Icons.Filled.NightsStay: ImageVector
    get() = _nightsStay ?: buildCustomIcon("Filled.NightsStay", listOf("""M11.1 12.08c-2.33-4.51-.5-8.48.53-10.07C6.27 2.2 1.98 6.59 1.98 12c0 .14.02.28.02.42.62-.27 1.29-.42 2-.42 1.66 0 3.18.83 4.1 2.15A4.01 4.01 0 0 1 11 18c0 1.52-.87 2.83-2.12 3.51.98.32 2.03.5 3.11.5 3.5 0 6.58-1.8 8.37-4.52-2.36.23-6.98-.97-9.26-5.41z""", """M7 16h-.18C6.4 14.84 5.3 14 4 14c-1.66 0-3 1.34-3 3s1.34 3 3 3h3c1.1 0 2-.9 2-2s-.9-2-2-2z""")).also { _nightsStay = it }
private var _nightsStay: ImageVector? = null

public val Icons.Filled.Numbers: ImageVector
    get() = _numbers ?: buildCustomIcon("Filled.Numbers", listOf("""m20.5 10 .5-2h-4l1-4h-2l-1 4h-4l1-4h-2L9 8H5l-.5 2h4l-1 4h-4L3 16h4l-1 4h2l1-4h4l-1 4h2l1-4h4l.5-2h-4l1-4h4zm-7 4h-4l1-4h4l-1 4z""")).also { _numbers = it }
private var _numbers: ImageVector? = null

public val Icons.Filled.OfflineBolt: ImageVector
    get() = _offlineBolt ?: buildCustomIcon("Filled.OfflineBolt", listOf("""M12 2.02c-5.51 0-9.98 4.47-9.98 9.98s4.47 9.98 9.98 9.98 9.98-4.47 9.98-9.98S17.51 2.02 12 2.02zM11.48 20v-6.26H8L13 4v6.26h3.35L11.48 20z""")).also { _offlineBolt = it }
private var _offlineBolt: ImageVector? = null

public val Icons.Filled.Palette: ImageVector
    get() = _palette ?: buildCustomIcon("Filled.Palette", listOf("""M12 2C6.49 2 2 6.49 2 12s4.49 10 10 10a2.5 2.5 0 0 0 2.5-2.5c0-.61-.23-1.2-.64-1.67a.528.528 0 0 1-.13-.33c0-.28.22-.5.5-.5H16c3.31 0 6-2.69 6-6 0-4.96-4.49-9-10-9zm5.5 11c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm-3-4c-.83 0-1.5-.67-1.5-1.5S13.67 6 14.5 6s1.5.67 1.5 1.5S15.33 9 14.5 9zM5 11.5c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5S7.33 13 6.5 13 5 12.33 5 11.5zm6-4c0 .83-.67 1.5-1.5 1.5S8 8.33 8 7.5 8.67 6 9.5 6s1.5.67 1.5 1.5z""")).also { _palette = it }
private var _palette: ImageVector? = null

public val Icons.Filled.Password: ImageVector
    get() = _password ?: buildCustomIcon("Filled.Password", listOf("""M2 17h20v2H2v-2zm1.15-4.05L4 11.47l.85 1.48 1.3-.75-.85-1.48H7v-1.5H5.3l.85-1.47L4.85 7 4 8.47 3.15 7l-1.3.75.85 1.47H1v1.5h1.7l-.85 1.48 1.3.75zm6.7-.75 1.3.75.85-1.48.85 1.48 1.3-.75-.85-1.48H15v-1.5h-1.7l.85-1.47-1.3-.75L12 8.47 11.15 7l-1.3.75.85 1.47H9v1.5h1.7l-.85 1.48zM23 9.22h-1.7l.85-1.47-1.3-.75L20 8.47 19.15 7l-1.3.75.85 1.47H17v1.5h1.7l-.85 1.48 1.3.75.85-1.48.85 1.48 1.3-.75-.85-1.48H23v-1.5z""")).also { _password = it }
private var _password: ImageVector? = null

public val Icons.Filled.Pause: ImageVector
    get() = _pause ?: buildCustomIcon("Filled.Pause", listOf("""M6 19h4V5H6v14zm8-14v14h4V5h-4z""")).also { _pause = it }
private var _pause: ImageVector? = null

public val Icons.Filled.Payment: ImageVector
    get() = _payment ?: buildCustomIcon("Filled.Payment", listOf("""M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z""")).also { _payment = it }
private var _payment: ImageVector? = null

public val Icons.Filled.PictureAsPdf: ImageVector
    get() = _pictureAsPdf ?: buildCustomIcon("Filled.PictureAsPdf", listOf("""M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8.5 7.5c0 .83-.67 1.5-1.5 1.5H9v2H7.5V7H10c.83 0 1.5.67 1.5 1.5v1zm5 2c0 .83-.67 1.5-1.5 1.5h-2.5V7H15c.83 0 1.5.67 1.5 1.5v3zm4-3H19v1h1.5V11H19v2h-1.5V7h3v1.5zM9 9.5h1v-1H9v1zM4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm10 5.5h1v-3h-1v3z""")).also { _pictureAsPdf = it }
private var _pictureAsPdf: ImageVector? = null

public val Icons.Filled.PrivacyTip: ImageVector
    get() = _privacyTip ?: buildCustomIcon("Filled.PrivacyTip", listOf("""M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-1 6h2v2h-2V7zm0 4h2v6h-2v-6z""")).also { _privacyTip = it }
private var _privacyTip: ImageVector? = null

public val Icons.Filled.QrCode: ImageVector
    get() = _qrCode ?: buildCustomIcon("Filled.QrCode", listOf("""M3 11h8V3H3v8zm2-6h4v4H5V5zM3 21h8v-8H3v8zm2-6h4v4H5v-4zm8-12v8h8V3h-8zm6 6h-4V5h4v4zm0 10h2v2h-2zm-6-6h2v2h-2zm2 2h2v2h-2zm-2 2h2v2h-2zm2 2h2v2h-2zm2-2h2v2h-2zm0-4h2v2h-2zm2 2h2v2h-2z""")).also { _qrCode = it }
private var _qrCode: ImageVector? = null

public val Icons.Filled.QrCodeScanner: ImageVector
    get() = _qrCodeScanner ?: buildCustomIcon("Filled.QrCodeScanner", listOf("""M9.5 6.5v3h-3v-3h3M11 5H5v6h6V5zm-1.5 9.5v3h-3v-3h3M11 13H5v6h6v-6zm6.5-6.5v3h-3v-3h3M19 5h-6v6h6V5zm-6 8h1.5v1.5H13V13zm1.5 1.5H16V16h-1.5v-1.5zM16 13h1.5v1.5H16V13zm-3 3h1.5v1.5H13V16zm1.5 1.5H16V19h-1.5v-1.5zM16 16h1.5v1.5H16V16zm1.5-1.5H19V16h-1.5v-1.5zm0 3H19V19h-1.5v-1.5zM22 7h-2V4h-3V2h5v5zm0 15v-5h-2v3h-3v2h5zM2 22h5v-2H4v-3H2v5zM2 2v5h2V4h3V2H2z""")).also { _qrCodeScanner = it }
private var _qrCodeScanner: ImageVector? = null

public val Icons.Filled.Receipt: ImageVector
    get() = _receipt ?: buildCustomIcon("Filled.Receipt", listOf("""M18 17H6v-2h12v2zm0-4H6v-2h12v2zm0-4H6V7h12v2zM3 22l1.5-1.5L6 22l1.5-1.5L9 22l1.5-1.5L12 22l1.5-1.5L15 22l1.5-1.5L18 22l1.5-1.5L21 22V2l-1.5 1.5L18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2 4.5 3.5 3 2v20z""")).also { _receipt = it }
private var _receipt: ImageVector? = null

public val Icons.Filled.ReceiptLong: ImageVector
    get() = _receiptLong ?: buildCustomIcon("Filled.ReceiptLong", listOf("""M19.5 3.5 18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2v14H3v3c0 1.66 1.34 3 3 3h12c1.66 0 3-1.34 3-3V2l-1.5 1.5zM19 19c0 .55-.45 1-1 1s-1-.45-1-1v-3H8V5h11v14z""", """M9 7h6v2H9zm7 0h2v2h-2zm-7 3h6v2H9zm7 0h2v2h-2z""")).also { _receiptLong = it }
private var _receiptLong: ImageVector? = null

public val Icons.Filled.Remove: ImageVector
    get() = _remove ?: buildCustomIcon("Filled.Remove", listOf("""M19 13H5v-2h14v2z""")).also { _remove = it }
private var _remove: ImageVector? = null

public val Icons.Filled.Restaurant: ImageVector
    get() = _restaurant ?: buildCustomIcon("Filled.Restaurant", listOf("""M11 9H9V2H7v7H5V2H3v7c0 2.12 1.66 3.84 3.75 3.97V22h2.5v-9.03C11.34 12.84 13 11.12 13 9V2h-2v7zm5-3v8h2.5v8H21V2c-2.76 0-5 2.24-5 4z""")).also { _restaurant = it }
private var _restaurant: ImageVector? = null

public val Icons.Filled.RestaurantMenu: ImageVector
    get() = _restaurantMenu ?: buildCustomIcon("Filled.RestaurantMenu", listOf("""m8.1 13.34 2.83-2.83L3.91 3.5a4.008 4.008 0 0 0 0 5.66l4.19 4.18zm6.78-1.81c1.53.71 3.68.21 5.27-1.38 1.91-1.91 2.28-4.65.81-6.12-1.46-1.46-4.2-1.1-6.12.81-1.59 1.59-2.09 3.74-1.38 5.27L3.7 19.87l1.41 1.41L12 14.41l6.88 6.88 1.41-1.41L13.41 13l1.47-1.47z""")).also { _restaurantMenu = it }
private var _restaurantMenu: ImageVector? = null

public val Icons.Filled.Restore: ImageVector
    get() = _restore ?: buildCustomIcon("Filled.Restore", listOf("""M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.954 8.954 0 0 0 13 21a9 9 0 0 0 0-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z""")).also { _restore = it }
private var _restore: ImageVector? = null

public val Icons.Filled.Save: ImageVector
    get() = _save ?: buildCustomIcon("Filled.Save", listOf("""M17 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z""")).also { _save = it }
private var _save: ImageVector? = null

public val Icons.Filled.Schedule: ImageVector
    get() = _schedule ?: buildCustomIcon("Filled.Schedule", listOf("""M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z""", """M12.5 7H11v6l5.25 3.15.75-1.23-4.5-2.67z""")).also { _schedule = it }
private var _schedule: ImageVector? = null

public val Icons.Filled.School: ImageVector
    get() = _school ?: buildCustomIcon("Filled.School", listOf("""M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3 1 9l11 6 9-4.91V17h2V9L12 3z""")).also { _school = it }
private var _school: ImageVector? = null

public val Icons.Filled.SearchOff: ImageVector
    get() = _searchOff ?: buildCustomIcon("Filled.SearchOff", listOf("""M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 0 0 9.5 3C6.08 3 3.28 5.64 3.03 9h2.02C5.3 6.75 7.18 5 9.5 5 11.99 5 14 7.01 14 9.5S11.99 14 9.5 14c-.17 0-.33-.03-.5-.05v2.02c.17.02.33.03.5.03 1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5z""", """M6.47 10.82 4 13.29l-2.47-2.47-.71.71L3.29 14 .82 16.47l.71.71L4 14.71l2.47 2.47.71-.71L4.71 14l2.47-2.47z""")).also { _searchOff = it }
private var _searchOff: ImageVector? = null

public val Icons.Filled.Security: ImageVector
    get() = _security ?: buildCustomIcon("Filled.Security", listOf("""M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z""")).also { _security = it }
private var _security: ImageVector? = null

public val Icons.Filled.ShoppingBag: ImageVector
    get() = _shoppingBag ?: buildCustomIcon("Filled.ShoppingBag", listOf("""M18 6h-2c0-2.21-1.79-4-4-4S8 3.79 8 6H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-8 4c0 .55-.45 1-1 1s-1-.45-1-1V8h2v2zm2-6c1.1 0 2 .9 2 2h-4c0-1.1.9-2 2-2zm4 6c0 .55-.45 1-1 1s-1-.45-1-1V8h2v2z""")).also { _shoppingBag = it }
private var _shoppingBag: ImageVector? = null

public val Icons.Filled.Stop: ImageVector
    get() = _stop ?: buildCustomIcon("Filled.Stop", listOf("""M6 6h12v12H6z""")).also { _stop = it }
private var _stop: ImageVector? = null

public val Icons.Filled.Storage: ImageVector
    get() = _storage ?: buildCustomIcon("Filled.Storage", listOf("""M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z""")).also { _storage = it }
private var _storage: ImageVector? = null

public val Icons.Filled.SwapVert: ImageVector
    get() = _swapVert ?: buildCustomIcon("Filled.SwapVert", listOf("""M16 17.01V10h-2v7.01h-3L15 21l4-3.99h-3zM9 3 5 6.99h3V14h2V6.99h3L9 3z""")).also { _swapVert = it }
private var _swapVert: ImageVector? = null

public val Icons.Filled.TravelExplore: ImageVector
    get() = _travelExplore ?: buildCustomIcon("Filled.TravelExplore", listOf("""M19.3 16.9c.4-.7.7-1.5.7-2.4 0-2.5-2-4.5-4.5-4.5S11 12 11 14.5s2 4.5 4.5 4.5c.9 0 1.7-.3 2.4-.7l3.2 3.2 1.4-1.4-3.2-3.2zm-3.8.1c-1.4 0-2.5-1.1-2.5-2.5s1.1-2.5 2.5-2.5 2.5 1.1 2.5 2.5-1.1 2.5-2.5 2.5zM12 20v2C6.48 22 2 17.52 2 12S6.48 2 12 2c4.84 0 8.87 3.44 9.8 8h-2.07A8 8 0 0 0 15 4.59V5c0 1.1-.9 2-2 2h-2v2c0 .55-.45 1-1 1H8v2h2v3H9l-4.79-4.79C4.08 10.79 4 11.38 4 12c0 4.41 3.59 8 8 8z""")).also { _travelExplore = it }
private var _travelExplore: ImageVector? = null

public val Icons.Filled.TrendingUp: ImageVector
    get() = _trendingUp ?: buildCustomIcon("Filled.TrendingUp", listOf("""m16 6 2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z""")).also { _trendingUp = it }
private var _trendingUp: ImageVector? = null

public val Icons.Filled.UploadFile: ImageVector
    get() = _uploadFile ?: buildCustomIcon("Filled.UploadFile", listOf("""M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15.01l1.41 1.41L11 14.84V19h2v-4.16l1.59 1.59L16 15.01 12.01 11z""")).also { _uploadFile = it }
private var _uploadFile: ImageVector? = null

public val Icons.Filled.VerifiedUser: ImageVector
    get() = _verifiedUser ?: buildCustomIcon("Filled.VerifiedUser", listOf("""M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z""")).also { _verifiedUser = it }
private var _verifiedUser: ImageVector? = null

public val Icons.Filled.Visibility: ImageVector
    get() = _visibility ?: buildCustomIcon("Filled.Visibility", listOf("""M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z""")).also { _visibility = it }
private var _visibility: ImageVector? = null

public val Icons.Filled.VisibilityOff: ImageVector
    get() = _visibilityOff ?: buildCustomIcon("Filled.VisibilityOff", listOf("""M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 0 0 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78 3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z""")).also { _visibilityOff = it }
private var _visibilityOff: ImageVector? = null

public val Icons.Filled.WbSunny: ImageVector
    get() = _wbSunny ?: buildCustomIcon("Filled.WbSunny", listOf("""m6.76 4.84-1.8-1.79-1.41 1.41 1.79 1.79 1.42-1.41zM4 10.5H1v2h3v-2zm9-9.95h-2V3.5h2V.55zm7.45 3.91-1.41-1.41-1.79 1.79 1.41 1.41 1.79-1.79zm-3.21 13.7 1.79 1.8 1.41-1.41-1.8-1.79-1.4 1.4zM20 10.5v2h3v-2h-3zm-8-5c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm-1 16.95h2V19.5h-2v2.95zm-7.45-3.91 1.41 1.41 1.79-1.8-1.41-1.41-1.79 1.8z""")).also { _wbSunny = it }
private var _wbSunny: ImageVector? = null

public val Icons.Filled.BlurOn: ImageVector
    get() = _blurOn ?: buildCustomIcon("Filled.BlurOn", listOf("""M14 10c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm-4 0c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm4 4c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm-4 0c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm-4-4c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm0 4c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm12 0c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm0-4c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm-6-4c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm0 12c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm-4-8c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm8 0c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm-4 4c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm0-8c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm0 16c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm-8-8c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1zm16 0c0-.55-.45-1-1-1s-1 .45-1 1 .45 1 1 1 1-.45 1-1z""")).also { _blurOn = it }
private var _blurOn: ImageVector? = null
