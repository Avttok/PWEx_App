package com.pwexpress.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ícones Lucide (os mesmos do site), gerados a partir do pacote lucide-react.
 * Licença ISC · https://lucide.dev · Para adicionar: inclua o nome no gerador e rode de novo.
 */
object PwIcones {
    private fun icone(nome: String, vararg caminhos: String): ImageVector {
        val b = ImageVector.Builder(name = nome, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        caminhos.forEach {
            b.addPath(
                pathData = addPathNodes(it),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val MapPin: ImageVector by lazy { icone("map-pin", "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0", "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val MapPinned: ImageVector by lazy { icone("map-pinned", "M18 8c0 3.613-3.869 7.429-5.393 8.795a1 1 0 01-1.214 0C9.87 15.429 6 11.613 6 8a6 6 0 0112 0", "M4.474 15h-.197a1 1 0 00-.969.753l-1.097 4.35a1.5 1.5 0 001.444 1.898L20.344 22a1.5 1.5 0 001.446-1.897l-1.098-4.35a1 1 0 00-.969-.753h-.197", "M10 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Navigation: ImageVector by lazy { icone("navigation", "M3 11L22 2L13 21L11 13L3 11Z") }
    val Phone: ImageVector by lazy { icone("phone", "M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384") }
    val Check: ImageVector by lazy { icone("check", "M20 6 9 17l-5-5") }
    val CircleCheck: ImageVector by lazy { icone("circle-check", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "m16 9-5.5 5.5L8 12") }
    val X: ImageVector by lazy { icone("x", "M18 6 6 18", "m6 6 12 12") }
    val FlaskConical: ImageVector by lazy { icone("flask-conical", "M14 2v6a2 2 0 0 0 .245.96l5.51 10.08A2 2 0 0 1 18 22H6a2 2 0 0 1-1.755-2.96l5.51-10.08A2 2 0 0 0 10 8V2", "M6.453 15h11.094", "M8.5 2h7") }
    val UserRound: ImageVector by lazy { icone("user-round", "M7 8a5 5 0 1 0 10 0a5 5 0 1 0 -10 0", "M20 21a8 8 0 0 0-16 0") }
    val Camera: ImageVector by lazy { icone("camera", "M13.997 4a2 2 0 0 1 1.76 1.05l.486.9A2 2 0 0 0 18.003 7H20a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.997a2 2 0 0 0 1.759-1.048l.489-.904A2 2 0 0 1 10.004 4z", "M9 13a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val Image: ImageVector by lazy { icone("image", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2Z", "M7 9a2 2 0 1 0 4 0a2 2 0 1 0 -4 0", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21") }
    val LogOut: ImageVector by lazy { icone("log-out", "m16 17 5-5-5-5", "M21 12H9", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4") }
    val Sun: ImageVector by lazy { icone("sun", "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0", "M12 2v2", "M12 20v2", "m4.93 4.93 1.41 1.41", "m17.66 17.66 1.41 1.41", "M2 12h2", "M20 12h2", "m6.34 17.66-1.41 1.41", "m19.07 4.93-1.41 1.41") }
    val Moon: ImageVector by lazy { icone("moon", "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401") }
    val Monitor: ImageVector by lazy { icone("monitor", "M4 3h16a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2Z", "M8 21L16 21", "M12 17L12 21") }
    val Bike: ImageVector by lazy { icone("bike", "M15 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0", "M2 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0", "M14 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0", "M12 17.5V14l-3-3 4-3 2 3h2") }
    val Wallet: ImageVector by lazy { icone("wallet", "M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1", "M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4") }
    val Timer: ImageVector by lazy { icone("timer", "M10 2L14 2", "M12 14L15 11", "M4 14a8 8 0 1 0 16 0a8 8 0 1 0 -16 0") }
    val Crosshair: ImageVector by lazy { icone("crosshair", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M22 12L18 12", "M6 12L2 12", "M12 6L12 2", "M12 22L12 18") }
    val BatteryMedium: ImageVector by lazy { icone("battery-medium", "M10 14v-4", "M22 14v-4", "M6 14v-4", "M4 6h12a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2Z") }
    val BatteryLow: ImageVector by lazy { icone("battery-low", "M22 14v-4", "M6 14v-4", "M4 6h12a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2Z") }
    val Power: ImageVector by lazy { icone("power", "M12 2v10", "M18.4 6.6a9 9 0 1 1-12.77.04") }
    val Radio: ImageVector by lazy { icone("radio", "M16.247 7.761a6 6 0 0 1 0 8.478", "M19.075 4.933a10 10 0 0 1 0 14.134", "M4.925 19.067a10 10 0 0 1 0-14.134", "M7.753 16.239a6 6 0 0 1 0-8.478", "M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Package: ImageVector by lazy { icone("package", "M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z", "M12 22V12", "M3.29 7L12 12L20.71 7", "m7.5 4.27 9 5.15") }
    val PackageCheck: ImageVector by lazy { icone("package-check", "M12 22V12", "m16 17 2 2 4-4", "M21 11.127V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.729l7 4a2 2 0 0 0 2 .001l1.32-.753", "M3.29 7 12 12l8.71-5", "m7.5 4.27 8.997 5.148") }
    val Route: ImageVector by lazy { icone("route", "M3 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15", "M15 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val ChevronsRight: ImageVector by lazy { icone("chevrons-right", "m6 17 5-5-5-5", "m13 17 5-5-5-5") }
    val Settings2: ImageVector by lazy { icone("settings-2", "M14 17H5", "M19 7h-9", "M14 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M4 7a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val Bug: ImageVector by lazy { icone("bug", "M12 20v-9", "M14 7a4 4 0 0 1 4 4v3a6 6 0 0 1-12 0v-3a4 4 0 0 1 4-4z", "M14.12 3.88 16 2", "M21 21a4 4 0 0 0-3.81-4", "M21 5a4 4 0 0 1-3.55 3.97", "M22 13h-4", "M3 21a4 4 0 0 1 3.81-4", "M3 5a4 4 0 0 0 3.55 3.97", "M6 13H2", "m8 2 1.88 1.88", "M9 7.13V6a3 3 0 1 1 6 0v1.13") }
    val ShieldCheck: ImageVector by lazy { icone("shield-check", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z", "m9 12 2 2 4-4") }
    val PenLine: ImageVector by lazy { icone("pen-line", "M13 21h8", "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z") }
    val KeyRound: ImageVector by lazy { icone("key-round", "M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z", "M16 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0") }
    val Clock: ImageVector by lazy { icone("clock", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 6v6l4 2") }
    val ArrowUpFromLine: ImageVector by lazy { icone("arrow-up-from-line", "m18 9-6-6-6 6", "M12 3v14", "M5 21h14") }
    val ArrowDownToLine: ImageVector by lazy { icone("arrow-down-to-line", "M12 17V3", "m6 11 6 6 6-6", "M19 21H5") }
    val LocateFixed: ImageVector by lazy { icone("locate-fixed", "M2 12L5 12", "M19 12L22 12", "M12 2L12 5", "M12 19L12 22", "M5 12a7 7 0 1 0 14 0a7 7 0 1 0 -14 0", "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val BellRing: ImageVector by lazy { icone("bell-ring", "M10.268 21a2 2 0 0 0 3.464 0", "M22 8c0-2.3-.8-4.3-2-6", "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326", "M4 2C2.8 3.7 2 5.7 2 8") }
    val Volume2: ImageVector by lazy { icone("volume-2", "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z", "M16 9a5 5 0 0 1 0 6", "M19.364 18.364a9 9 0 0 0 0-12.728") }
    val RefreshCw: ImageVector by lazy { icone("refresh-cw", "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5") }
    val ChevronLeft: ImageVector by lazy { icone("chevron-left", "m15 18-6-6 6-6") }
    val Alerta: ImageVector by lazy { icone("triangle-exclamation-point", "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3", "M12 9v4", "M12 17h.01") }
    val Sparkles: ImageVector by lazy { icone("sparkles", "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z", "M20 2v4", "M22 4h-4", "M2 20a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Pencil: ImageVector by lazy { icone("pencil", "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z", "m15 5 4 4") }
    val Send: ImageVector by lazy { icone("send", "M14.536 21.686a.5.5 0 0 0 .937-.024l6.5-19a.496.496 0 0 0-.635-.635l-19 6.5a.5.5 0 0 0-.024.937l7.93 3.18a2 2 0 0 1 1.112 1.11z", "m21.854 2.147-10.94 10.939") }
    val EyeOff: ImageVector by lazy { icone("eye-off", "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49", "M14.084 14.158a3 3 0 0 1-4.242-4.242", "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143", "m2 2 20 20") }
    val Maximize2: ImageVector by lazy { icone("maximize-2", "M15 3h6v6", "m21 3-7 7", "m3 21 7-7", "M9 21H3v-6") }
}
