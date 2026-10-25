package zone.vgs.tvsach.dulieu

/**
 * Kịch bản khôi phục dùng bằng máy tính (24/10/2026, học cleanyourtv-app "Save a copy"): mất điện thoại, gỡ CleanTV
 * hay app hỏng vẫn đưa được tivi về như cũ chỉ với adb. Mỗi dòng là một lệnh adb thuần, không dùng tính năng riêng
 * của shell nào, nên cùng một file chạy được trên Windows (đuôi .bat) lẫn Mac / Linux (`sh file.bat`). Dòng chú thích
 * bắt đầu bằng ": " — Windows coi là nhãn, sh coi là lệnh rỗng — nên cả hai đều bỏ qua.
 */
object KichBanKhoiPhuc {
    private val HOME_SACH = listOf("com.gathin.tvhome", "com.ts.launcher")
    private const val PKG_KHOA = "zone.vgs.cleantv.khoa"
    private val TEN_GOI = Regex("^[A-Za-z0-9_.]+$")
    private val THANH_PHAN = Regex("^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$")

    fun tao(sl: BanSaoLuu, vi: Boolean): String {
        fun t(v: String, e: String) = if (vi) v else e
        val dong = mutableListOf<String>()
        fun chu(s: String) { dong += ": $s" }
        chu("TuneTV: script de restauración para ${sl.ten}")
        chu("Necesita adb (Android platform-tools). Windows: doble clic en este archivo. Mac / Linux: sh nombre-del-archivo.bat")
        chu("La depuración por USB debe estar activada. Si la IP cambió, edita abajo la línea adb connect.")
        dong += "adb connect ${sl.ip}:5555"
        val goi = sl.daTatBoiApp.filter { TEN_GOI.matches(it) }
        if (goi.isNotEmpty()) chu("Reactivar los ${goi.size} paquetes que TuneTV desactivó")
        goi.forEach { dong += "adb shell pm enable --user 0 $it" }
        if (sl.launcherDaCai && THANH_PHAN.matches(sl.homeTruoc) && HOME_SACH.none { sl.homeTruoc.startsWith("$it/") }) {
            chu("Restaurar el inicio de fábrica")
            dong += "adb shell cmd package set-home-activity --user 0 ${sl.homeTruoc}"
        }
        if (sl.launcherDaCai) {
            chu("Dejar los servicios de accesibilidad como estaban")
            val tc = sl.truyCapTruoc
            dong += if (tc.isBlank() || tc == "null" || !Regex("^[A-Za-z0-9_.:/$]+$").matches(tc)) "adb shell settings delete secure enabled_accessibility_services"
            else "adb shell settings put secure enabled_accessibility_services $tc"
            chu("Para desinstalar también el inicio limpio, quita los \": \" del comienzo de la siguiente línea")
            HOME_SACH.forEach { chu("adb uninstall $it") }
        }
        if (sl.dnsDaDoi) {
            chu("Restaurar DNS")
            val mode = sl.dnsModeTruoc.takeIf { it.isNotBlank() && it != "null" && TEN_GOI.matches(it) } ?: "opportunistic"
            dong += "adb shell settings put global private_dns_mode $mode"
            dong += if (sl.dnsHostTruoc.isBlank() || sl.dnsHostTruoc == "null" || !TEN_GOI.matches(sl.dnsHostTruoc)) "adb shell settings delete global private_dns_specifier"
            else "adb shell settings put global private_dns_specifier ${sl.dnsHostTruoc}"
        }
        chu("Quitar el bloqueo infantil si está instalado (si aquí sale un error, no pasa nada)")
        dong += "adb uninstall $PKG_KHOA"
        dong += "adb shell input keyevent KEYCODE_HOME"
        chu("Listo. Apaga la depuración por USB en la TV.")
        return dong.joinToString("\r\n", postfix = "\r\n")
    }
}
