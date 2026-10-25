package zone.vgs.tvsach.ui

import zone.vgs.tvsach.t
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.LinkProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntashirakon.adb.AdbPairingRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import zone.vgs.tvsach.adb.KetNoiAdb
import zone.vgs.tvsach.adb.ThaoTacTv
import zone.vgs.tvsach.dulieu.BanSaoLuu
import zone.vgs.tvsach.dulieu.DuLieu
import zone.vgs.tvsach.dulieu.KeHoach
import zone.vgs.tvsach.dulieu.KhoSaoLuu
import zone.vgs.tvsach.dulieu.THANH_PHO
import zone.vgs.tvsach.dulieu.MauTv
import zone.vgs.tvsach.dulieu.MucGoi
import zone.vgs.tvsach.dulieu.ThongTinTv
import zone.vgs.tvsach.dulieu.TrangThaiGoi
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket

enum class Man { CHINH, KET_NOI, THONG_TIN, CHON_GOI, DANG_TAT, HOME_SACH, XONG, KHOI_PHUC, HUONG_DAN, GIAY_PHEP, KHOA_TRE }

/** Số app tối đa trong dock: bằng số cột lớn nhất của Home sạch (columns 1–8); dock dài hơn số cột thì tràn xuống hàng sau (22/10/2026, góp ý muốn hàng đầu nhiều app hơn). */
const val DOCK_TOI_DA = 8

/**
 * Bảng phím chung cho TCL không có mẫu riêng: Netflix 4062, YouTube 4065, Prime Video 4099, bánh răng 83 / 4077 — cùng số
 * trên mọi mẫu TCL có bảng trong upstream (tcl-43c645, tcl-55c725, tcl-65c735, tcl-g10…).
 */
/** doiHang đặc biệt: hàng dock nằm giữa màn hình (rows_middle của bản vá launcher), góp ý 24/10/2026. */
const val HANG_GIUA = Int.MIN_VALUE

/**
 * Ô kênh VTV trên Home sạch (góp ý khoald92vn, trananhht9 23/10/2026: bố mẹ bấm thẳng vào kênh). Mỗi ô mở thẳng kênh
 * trong VTVgo bằng vtvgo://play?mediaType=CHANNEL&contentId=<mã> (mã kênh từ API công khai display/v21.0/catalogs/
 * with-channels của VTVgo, thử trên VTVgo TV 12.10.23, 25/10/2026). Launcher: link_tiles + gói giả vgs.kenh.* trong dock_apps.
 */
const val GOI_VTVGO = "vn.vtv.vtvgotv"
val KENH_VTV = listOf(
    Triple("VTV1", 1, "#D71920"), Triple("VTV2", 2, "#2E7D32"), Triple("VTV3", 3, "#E8590C"),
    Triple("VTV4", 4, "#1565C0"), Triple("VTV5", 5, "#6A1B9A"), Triple("VTV6", 13, "#00897B"),
    Triple("VTV7", 27, "#F9A825"), Triple("VTV8", 36, "#AD1457"), Triple("VTV9", 39, "#283593"),
)
private const val GOI_KENH = "vgs.kenh."

private const val PHIM_TCL_CHUNG = """{"app_keys":["4062:com.netflix.ninja","4065:com.google.android.youtube.tv","4099:com.amazon.amazonvideo.livingroom"],"settings_key_keycodes":[83,4077]}"""

/**
 * Mẫu mà phím remote mặc định TẮT: TCL G09 Android 12 — bật dịch vụ lọc phím là giữ nút âm lượng chỉ nhảy một nấc
 * (upstream docs/devices/tcl-55p71b-g09-key-repeat.md). Người dùng vẫn tự bật được.
 */
private val PHIM_TAT_SAN = setOf("tcl-g09-google-tv")

/** Trình duyệt cho nút Web của remote TCL, theo thứ tự ưu tiên; tivi không có cái nào thì nút Web để nguyên. */
private val TRINH_DUYET_TV = listOf("com.android.chrome", "com.tcl.browser", "com.phlox.tvwebbrowser", "com.cloudmosa.puffinTV", "org.mozilla.tv.firefox")

class PhienLamViec(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()
    private val adb by lazy { KetNoiAdb.lay(ctx) }
    val kho by lazy { KhoSaoLuu(ctx) }
    private val tv by lazy { ThaoTacTv(adb, kho) }
    private val prefs by lazy { ctx.getSharedPreferences("tvsach", Context.MODE_PRIVATE) }

    var man by mutableStateOf(Man.CHINH)
    /** Màn để quay về sau khi xem Hướng dẫn / Giấy phép. */
    var manTruoc by mutableStateOf(Man.CHINH)

    fun moHuongDan() { if (man != Man.HUONG_DAN) manTruoc = man; man = Man.HUONG_DAN }
    fun moGiayPhep() { if (man != Man.GIAY_PHEP) manTruoc = man; man = Man.GIAY_PHEP }
    var dangBan by mutableStateOf(false)
    var thongBao by mutableStateOf("")
    var loi by mutableStateOf("")

    // Kết nối
    var ip by mutableStateOf("")
    var cong by mutableStateOf("5555")
    val timThay = mutableStateListOf<String>()
    var dangQuet by mutableStateOf(false)
    var ghepIpCong by mutableStateOf("")
    var ghepMa by mutableStateOf("")

    // Tivi
    var thongTin by mutableStateOf<ThongTinTv?>(null)
    private var getprop = ""
    var trangThai by mutableStateOf<TrangThaiGoi?>(null)
    var ungVien by mutableStateOf<List<Pair<MauTv, Int>>>(emptyList())
    /** Mẫu cùng hãng tên máy gần giống (DuLieu.ganGiong): chỉ gợi ý, không tự chọn. */
    var ganGiong by mutableStateOf<List<MauTv>>(emptyList())
    /** Toàn bộ mẫu, cho hộp "Chọn mẫu khác". */
    var tatCaMau by mutableStateOf<List<MauTv>>(emptyList())
    var moChonMau by mutableStateOf(false)
    /** Màn Home sạch: thẻ đang mở (0 Giao diện, 1 Home, 2 Tuỳ chỉnh) và màn Nâng cao riêng. */
    var theHome by mutableStateOf(0)
    var moNangCao by mutableStateOf(false)
    var mauChon by mutableStateOf<MauTv?>(null)
    var saoLuu by mutableStateOf<BanSaoLuu?>(null)
    /** Bản sao lưu vừa lấy từ tivi (điện thoại này không có). */
    var saoLuuTuTv by mutableStateOf(false)

    // Chọn gói
    var muc by mutableStateOf<List<MucGoi>>(emptyList())
    val chon = mutableStateMapOf<String, Boolean>()
    var epSetupwraith by mutableStateOf(false)

    // Kết quả
    val nhatKy = mutableStateListOf<String>()
    var ketQua by mutableStateOf<ThaoTacTv.KetQuaTat?>(null)
    var anhTv by mutableStateOf<Bitmap?>(null)

    // Home sạch
    val dock = mutableStateListOf<String>()
    var tenChao by mutableStateOf("")
    var nutInput by mutableStateOf(false)
    var nutHome by mutableStateOf(false)
    var dnsChanQc by mutableStateOf(false)
    var nutManCho by mutableStateOf(true)
    var gonNut by mutableStateOf(true)
    /** Dời khối dock + hàng app lên (âm) / xuống (dương), dp — rows_offset_dp của bản vá launcher (góp ý 24/10/2026). */
    var doiHang by mutableStateOf(0)
    /** Nút "Đang phát" trên Home sạch (now_playing của bản vá launcher), mặc định bật. */
    var nutDangPhat by mutableStateOf(true)
    /** Cỡ chữ cụm đồng hồ / ngày / âm lịch / thời tiết (header_scale của bản vá launcher, %), góp ý 24/10/2026. */
    var coChuDau by mutableStateOf(100)
    /** Mọi app trên một hàng cuộn ngang, không lưới bên dưới (single_row của launcher 2.8, cần dock_row). */
    var motHang by mutableStateOf(false)
    /** Ẩn lời chào trên đồng hồ (show_greeting=false). */
    var anLoiChao by mutableStateOf(false)
    /**
     * Giữ nút âm lượng để tăng/giảm liên tục (key_filter=false): dịch vụ bắt phím của Home sạch làm Android thôi tự lặp
     * phím giữ trên Sony, Xiaomi, Hisense (upstream #1680, #944, #1326; góp ý meorudo, Sony, 25/10/2026). Bật thì dịch vụ
     * vẫn chạy để mở đè Home cũ, chỉ thôi lọc phím: nút app / Input / Apps trên remote không còn do Home sạch lo.
     */
    var giuAmLuong by mutableStateOf(false)
    /** Ô icon đều nhau kiểu Apple TV: icon trên nền màu của chính nó, không dùng ảnh banner (uniform_tiles). */
    var oDeuNhau by mutableStateOf(false)
    /** Số app mỗi hàng (columns của launcher, 0 = theo theme), góp ý 24/10/2026: Sony chỉ có 4 app một hàng. */
    var soCot by mutableStateOf(0)
    /** Cho chọn app tự mở khi bật tivi (open_on_start_choice của launcher; góp ý dmenh568, quan1sd1998 24/10/2026). */
    var moKhiBat by mutableStateOf(false)
    /** Kênh VTV đặt ở đầu dock (tên VTV1…), theo thứ tự KENH_VTV. */
    val kenhVtv = mutableStateListOf<String>()
    val coVtvGo get() = trangThai?.caiDat?.contains(GOI_VTVGO) == true
    /**
     * Nút app / Cài đặt trên remote (24/10/2026, góp ý TCL G09): tắt Home gốc của TCL là nút YouTube, Netflix… chết vì
     * hãng chỉ gửi chúng cho launcherx. null = tự chọn: bật khi mẫu có bảng phím, trừ mẫu trong PHIM_TAT_SAN.
     */
    var nutAppRemoteChon by mutableStateOf<Boolean?>(null)
    val nutAppRemote get() = nutAppRemoteChon ?: (mauChon?.id !in PHIM_TAT_SAN && !laTclGoogleTv)

    /**
     * TCL Google TV (có Home Google launcherx): dịch vụ trợ năng nào lọc phím cũng làm giữ nút âm lượng chỉ nhảy một nấc
     * (upstream docs/devices/tcl-55p71b-g09-key-repeat.md; góp ý nda2192 24/10/2026). Mặc định ưu tiên âm lượng: tắt nút
     * app remote và ghi key_filter=false — dịch vụ vẫn bật để mở đè Home (theo cửa sổ, không cần lọc phím).
     */
    // Chỉ khi launcherx CÒN BẬT (đường mở đè): nút app remote tự chạy qua nó, thôi lọc phím không mất gì. Đã tắt
    // launcherx thì nút app chỉ chạy nhờ app_keys → giữ như 2.6.7 (heavensky95, TCL G10 đã tắt launcherx).
    val laTclGoogleTv get() = thongTin?.brand?.equals("TCL", ignoreCase = true) == true &&
        trangThai?.caiDat?.contains("com.google.android.apps.tv.launcherx") == true &&
        trangThai?.daTat?.contains("com.google.android.apps.tv.launcherx") != true

    /** Bảng phím remote dùng cho tivi này: của mẫu đang chọn, không có thì bảng chung của TCL; null nếu không có. */
    fun phimRemote(): JSONObject? = mauChon?.phim
        ?: if (thongTin?.brand?.equals("TCL", ignoreCase = true) == true) JSONObject(PHIM_TCL_CHUNG) else null

    /** Có ghép bảng phím vào cấu hình lần này không (cần HomeKeyService). */
    val dungPhimRemote get() = nutAppRemote && phimRemote() != null
    // Tuỳ chỉnh thêm (1.7, các "switch" của launcher — upstream themes/README.md).
    var amLich by mutableStateOf(true)
    var tenDuoiApp by mutableStateOf(false)
    /** Tên app to phía trên dock (dock_title, launcher mặc định bật); góp ý 22/10/2026 muốn tắt cho đỡ rối. */
    var tenToTrenDock by mutableStateOf(true)
    /** Ô app điện thoại (không có ảnh tivi) tô màu theo icon (launcher icon_tile_color, 23/10/2026). */
    var toMauOApp by mutableStateOf(true)
    var thuVienApp by mutableStateOf(false)
    var dongHoKhiYen by mutableStateOf(0)
    var tiviYeu by mutableStateOf(false)
    var hieuUngKinh by mutableStateOf(false)
    var lamToiNen by mutableStateOf(0)
    var hienThongSo by mutableStateOf(false)
    /** Mục Nâng cao: danh sách tuỳ chọn + giá trị người dùng đặt (khoá → chữ; thiếu = mặc định). */
    val dsNangCao: List<zone.vgs.tvsach.dulieu.TuyChon> by lazy { zone.vgs.tvsach.dulieu.NangCao.nap(ctx) }
    val nangCao = androidx.compose.runtime.mutableStateMapOf<String, String>()
    var homeXong by mutableStateOf(false)
    /** Lần thử nút Home thật gần nhất ra Home sạch (thuPhimHome). */
    var phimHomeOk by mutableStateOf(true)
    /** Kết quả bấm thử nút Home lần gần nhất, gửi kèm danh sách gói để chẩn đoán. */
    var phimHomeGhi = ""
    var themeChon by mutableStateOf(zone.vgs.tvsach.dulieu.THEMES.first())

    // Máy chiếu: màn của hãng dò được + những màn đã ghim vào nút "Máy chiếu" trên Home sạch (comp → nhãn).
    var laMayChieu by mutableStateOf(false)
    var mayChieu by mutableStateOf<List<String>?>(null)
    val ghimMayChieu = mutableStateMapOf<String, String>()
    var dangDoMayChieu by mutableStateOf(false)

    fun doMayChieu() {
        if (dangDoMayChieu) return
        dangDoMayChieu = true; loi = ""
        viewModelScope.launch {
            try {
                val ds = io { tv.doMayChieu() }
                mayChieu = ds
                if (ds.isEmpty()) loi = "No se encontraron pantallas de enfoque / keystone."
            } catch (e: Throwable) { loi = moTaLoi(e) } finally { dangDoMayChieu = false }
        }
    }

    fun moThuMayChieu(comp: String) = chay("Abriendo en el proyector…") {
        io { tv.moThu(comp) }?.let { error("El proyector rechazó esta pantalla: $it") }
    }

    fun doiGhim(comp: String) {
        if (comp in ghimMayChieu) ghimMayChieu.remove(comp) else if (ghimMayChieu.size < 6) ghimMayChieu[comp] = nhanMayChieu(comp)
    }

    /** Đoán nhãn theo tên màn: focus → Lấy nét, keystone/trapez… → Chỉnh hình, zoom/scale → Thu phóng. */
    fun nhanMayChieu(comp: String): String {
        val c = comp.substringAfter('/').lowercase()
        return when {
            Regex("focus|duijiao").containsMatchIn(c) -> "Enfoque"
            Regex("keystone|trapez|tixing|jiaozheng|correct|geometr|ladder").containsMatchIn(c) -> "Keystone"
            Regex("zoom|scale|size").containsMatchIn(c) -> "Zoom"
            Regex("calibr").containsMatchIn(c) -> "Calibrar"
            else -> c.substringAfterLast('.').removeSuffix("activity").replaceFirstChar { it.uppercase() }
        }
    }

    /** Danh sách màn dò được, để dán vào hộp góp ý vgs.zone. */
    fun chuDanhSachMayChieu(): String = buildString {
        thongTin?.let { append("${it.tenHienThi} · ${it.device} · Android ${it.android}\n") }
        (mayChieu ?: emptyList()).forEach { append(it).append('\n') }
    }
    var thanhPho by mutableStateOf<zone.vgs.tvsach.dulieu.ThanhPho?>(null)
    /** Ô tìm phường/xã cho thời tiết (góp ý 23/10/2026). */
    var timXa by mutableStateOf("")
        private set
    /**
     * Kết quả tìm phường/xã: lọc 3.321 xã sau khi ngừng gõ 0,35 giây, ở luồng phụ (25/10/2026, TCL chạy CleanTV ngay
     * trên tivi: lọc lại mỗi lần vẽ màn hình làm ô gõ giật, không chọn được).
     */
    var ketQuaXa by mutableStateOf<List<zone.vgs.tvsach.dulieu.XaPhuong.Xa>>(emptyList())
        private set
    private var viecTimXa: kotlinx.coroutines.Job? = null
    /** Mở thẳng màn chọn app "Mở khi khởi động" trên tivi (25/10/2026: dmenh568 không tìm thấy chỗ chọn trên Xiaomi). */
    var baoChonApp by mutableStateOf("")
    fun moChonAppKhiBat() = chay("Abriendo en la TV…") {
        io { tv.moCaiDatLauncher() }
        baoChonApp = "Los ajustes del launcher están abiertos en la TV: elige \"Abrir al encender\" y luego la app."
    }

    fun doiTimXa(q: String) {
        timXa = q
        viecTimXa?.cancel()
        if (q.trim().length < 2) { ketQuaXa = emptyList(); return }
        viecTimXa = viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            val vn = withContext(Dispatchers.Default) { zone.vgs.tvsach.dulieu.XaPhuong.timXa(zone.vgs.tvsach.dulieu.XaPhuong.nap(ctx), q) }
            ketQuaXa = vn
            // Ít kết quả trong nước thì tìm thêm thành phố khắp thế giới (người dùng ở Bali, Osaka…).
            if (vn.size < 3 && q.trim().length >= 3) {
                val theGioi = withContext(Dispatchers.IO) { zone.vgs.tvsach.dulieu.XaPhuong.timTheGioi(q, zone.vgs.tvsach.NgonNgu.tiengViet) }
                if (timXa == q) ketQuaXa = vn + theGioi.take(8 - vn.size)
            }
        }
    }
    fun chonXa(x: zone.vgs.tvsach.dulieu.XaPhuong.Xa) { thanhPho = zone.vgs.tvsach.dulieu.ThanhPho(x.tenNgan, x.lat, x.lon); doiTimXa("") }

    /** Tivi đã có Home sạch: chỉ cần đẩy theme, không cài lại. */
    /** Tên gói Home sạch trên tivi đang nối (Xiaomi bản Trung Quốc: com.ts.launcher), dò lúc kết nối. */
    private var goiHome = ThaoTacTv.PKG_HOME
    val daCoHomeSach: Boolean get() = trangThai?.let { tt -> goiHome in tt.caiDat && goiHome !in tt.daTat } == true

    var dsSaoLuu by mutableStateOf<List<BanSaoLuu>>(emptyList())

    var banMoi by mutableStateOf<zone.vgs.tvsach.CapNhat.BanMoi?>(null)
    var dangCapNhat by mutableStateOf(false)
    /** Hộp mời nâng cấp: hiện một lần cho mỗi bản mới lúc mở app. */
    var hoiCapNhat by mutableStateOf(false)

    init {
        viewModelScope.launch {
            val b = runCatching { io { zone.vgs.tvsach.CapNhat.kiemTra(ctx) } }.getOrNull()
            banMoi = b
            if (b != null) xuLyBanMoi(b)
        }
        // Chạm thông báo khi app đang mở sẵn: kiểm tra lại cờ mỗi khi quay về.
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1500)
                if (!zone.vgs.tvsach.CapNhat.moTuThongBao) continue
                // Lúc mở app có thể đang mất mạng nên chưa biết bản mới → hỏi lại máy chủ.
                val b = banMoi ?: runCatching { io { zone.vgs.tvsach.CapNhat.kiemTraApp() } }.getOrNull().also { banMoi = it }
                if (b != null) xuLyBanMoi(b) else zone.vgs.tvsach.CapNhat.moTuThongBao = false
            }
        }
        ip = prefs.getString("ip", "") ?: ""
        cong = prefs.getString("cong", "5555") ?: "5555"
        taiLaiSaoLuu()
    }

    private fun xuLyBanMoi(b: zone.vgs.tvsach.CapNhat.BanMoi) {
        val p = ctx.getSharedPreferences("ota", Context.MODE_PRIVATE)
        if (zone.vgs.tvsach.CapNhat.moTuThongBao) {
            zone.vgs.tvsach.CapNhat.moTuThongBao = false
            hoiCapNhat = false
            capNhatApp()
        } else if (p.getInt("da_hoi", 0) < b.versionCode) {
            p.edit().putInt("da_hoi", b.versionCode).putInt("da_bao", maxOf(p.getInt("da_bao", 0), b.versionCode)).apply()
            hoiCapNhat = true
        }
    }

    fun capNhatApp() {
        val b = banMoi ?: return
        if (dangCapNhat) return
        dangCapNhat = true
        viewModelScope.launch {
            try { io { zone.vgs.tvsach.CapNhat.taiVaCai(ctx, b) } }
            catch (e: Throwable) { loi = "No se pudo descargar la actualización: ${e.message}" }
            finally { dangCapNhat = false }
        }
    }

    fun taiLaiSaoLuu() { dsSaoLuu = kho.tatCa() }

    private fun chay(viec: String, khoi: suspend () -> Unit) {
        if (dangBan) return
        dangBan = true; loi = ""; thongBao = viec
        viewModelScope.launch {
            try { khoi() } catch (e: Throwable) {
                loi = moTaLoi(e)
            } finally { dangBan = false; thongBao = "" }
        }
    }

    private fun laLoiTuChoi(e: Throwable) = listOfNotNull(e.message, e.cause?.message).any { it.contains("ECONNREFUSED") || it.contains("refused") }

    private fun moTaLoi(e: Throwable): String = when {
        e is ThaoTacTv.TiviKhoiDongLaiException ->
            "La TV se reinició justo después de desactivar ${e.goi}. La app dejó de limpiar por seguridad (${e.daTat} paquetes desactivados, todos en el respaldo). Espera a la TV y vuelve a conectar: si funciona, limpia otra vez sin ${e.goi}; si algo anda mal, pulsa Restaurar esta TV."
        e is ThaoTacTv.MatKetNoiTvException ->
            "Se perdió la conexión con la TV mientras se limpiaba y no se pudo reconectar (${e.daTat} paquetes desactivados, todos en el respaldo). Revisa que la TV esté encendida y en el mismo Wi‑Fi, y vuelve a conectar para seguir o pulsa Restaurar esta TV. Si nada funciona, desconecta la TV de la corriente 10 segundos."
        ThaoTacTv.laMatKetNoi(e) && !laLoiTuChoi(e) ->
            "Se perdió la conexión con la TV. Todo lo hecho está en el respaldo: vuelve a conectar para seguir o pulsa Restaurar esta TV."
        e is AdbPairingRequiredException -> "La TV necesita vinculación (Depuración inalámbrica). Usa \"Vincular con código\" abajo."
        // Chế độ chạy trên tivi: nhiều tivi (TCL…) bật Gỡ lỗi USB chỉ mở ADB qua dây, cổng 5555 chưa nghe (22/10/2026).
        laLoiTuChoi(e) && ip.trim() == "127.0.0.1" ->
            "La TV no ha abierto ADB por red (puerto 5555), así que la app en la TV no se puede conectar a sí misma. Solución: en Opciones de desarrollador activa también \"Depuración de red\" / \"ADB por red\" (si existe) y vuelve a Conectar. Si la TV solo tiene \"Depuración inalámbrica\", usa TuneTV en un teléfono con Vincular con código. O conecta la TV a un computador y ejecuta adb tcpip 5555 una vez."
        laLoiTuChoi(e) ->
            "La TV rechazó la conexión en este puerto. Revisa que la depuración USB / de red esté activada."
        e.message?.contains("EHOSTUNREACH") == true || e.message?.contains("No route") == true ->
            "No se encuentra la TV. El teléfono y la TV deben estar en el mismo Wi‑Fi."
        e is java.net.SocketTimeoutException || e.message?.contains("timed out") == true ->
            "Se acabó el tiempo. Puede que la TV esté mostrando \"¿Permitir depuración?\": pulsa Permitir e inténtalo de nuevo."
        else -> listOfNotNull(e.message, e.cause?.message).firstOrNull()
            ?: "Error de conexión (${e.javaClass.simpleName}). Revisa la IP, el puerto y que la TV esté encendida."
    }

    private suspend fun <T> io(khoi: () -> T): T = withContext(Dispatchers.IO) { khoi() }

    // ---------- Kết nối ----------

    /** Quét cả mạng /24 của điện thoại tìm máy mở cổng 5555. */
    fun quetMang() {
        if (dangQuet) return
        dangQuet = true; timThay.clear(); loi = ""
        viewModelScope.launch {
            try {
                val goc = ipDienThoai() ?: run { loi = "El teléfono no está conectado a Wi‑Fi."; return@launch }
                val dau = goc.substringBeforeLast('.')
                val sem = Semaphore(48)
                (1..254).map { i ->
                    async(Dispatchers.IO) {
                        sem.withPermit {
                            val dc = "$dau.$i"
                            if (dc != goc && moCong(dc, 5555, 400)) withContext(Dispatchers.Main) { timThay += dc }
                        }
                    }
                }.awaitAll()
                if (timThay.isEmpty()) loi = "No hay ninguna TV con el puerto 5555 abierto. Las Google TV nuevas a veces solo ofrecen Depuración inalámbrica: usa la vinculación con código."
            } finally { dangQuet = false }
        }
    }

    /** Các cổng TCP đang mở trên [host] trong [dai], quét song song (256 luồng, chờ 300 ms mỗi cổng). */
    private suspend fun quetCongMo(host: String, dai: IntRange, boQua: Set<Int> = emptySet()): List<Int> = withContext(Dispatchers.IO) {
        val sem = Semaphore(256)
        dai.filter { it !in boQua }.map { c -> async { sem.withPermit { if (moCong(host, c, 300)) c else null } } }.awaitAll().filterNotNull()
    }

    private fun moCong(host: String, port: Int, ms: Int): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress(host, port), ms) }; true
    }.getOrDefault(false)

    private fun ipDienThoai(): String? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val lp: LinkProperties = cm.getLinkProperties(cm.activeNetwork) ?: return null
        return lp.linkAddresses.map { it.address }.filterIsInstance<Inet4Address>().firstOrNull()?.hostAddress
    }

    fun ketNoi() = chay("Conectando… Si la TV muestra \"¿Permitir depuración USB?\", marca \"Permitir siempre\" y pulsa Permitir.") {
        val host = ip.trim(); val port = cong.trim().toIntOrNull() ?: 5555
        daTuTatAdb = false
        if (host.isEmpty()) error("Escribe la dirección IP de la TV.")
        io {
            runCatching { adb.disconnect() }
            if (!adb.connect(host, port)) error("No se pudo conectar. Inténtalo de nuevo y fíjate en el aviso de la TV.")
        }
        prefs.edit().putString("ip", host).putString("cong", port.toString()).apply()
        sauKhiKetNoi(host)
    }

    /** Chế độ tivi: kết nối ADB vào chính máy này. */
    fun ketNoiTiviNay() { ip = "127.0.0.1"; cong = "5555"; ketNoi() }

    /** Chế độ tivi: các bước bấm Home / khởi động lại Home đẩy app xuống nền — mở lại app cho người dùng thấy kết quả. */
    private suspend fun veApp() {
        if (zone.vgs.tvsach.ManHinh.laTv) io { runCatching { adb.shell("am start -n ${ctx.packageName}/zone.vgs.tvsach.MainActivity") } }
    }

    /** Ghép đôi Gỡ lỗi không dây (Android 11+), rồi tìm cổng kết nối qua mDNS. */
    fun ghepDoi() = chay("Vinculando…") {
        val (h, p) = tachIpCong(ghepIpCong) ?: error("Escríbelo como 192.168.1.20:37123 (IP y puerto del aviso de vinculación).")
        val ma = ghepMa.trim()
        if (ma.length != 6) error("El código de vinculación tiene 6 dígitos.")
        io { if (!adb.pair(h, p, ma)) error("Falló la vinculación; puede que el código haya cambiado. Vuelve a abrir el aviso de vinculación en la TV.") }
        thongBao = "Vinculada. Buscando el puerto de conexión de la TV…"
        val ok = io {
            runCatching { adb.disconnect() }
            runCatching { adb.connectTls(ctx, 15_000) }.getOrDefault(false)
        }
        // mDNS không thấy (mạng chặn multicast, Chromecast Android 14 — góp ý buivansaobg 24/10/2026): quét cổng
        // ngẫu nhiên của Gỡ lỗi không dây (dải cổng tạm 32768–60999 của Linux) rồi thử kết nối từng cổng mở.
        val ok2 = ok || run {
            thongBao = "El puerto no se anunció; probando los puertos de la TV (unos 30 segundos)…"
            val mo = quetCongMo(h, 32768..60999, boQua = setOf(p))
            mo.any { c -> io { runCatching { adb.disconnect() }; runCatching { adb.connect(h, c) }.getOrDefault(false) } }
        }
        if (!ok2) {
            ip = h
            error("Vinculada, pero el puerto no se encontró solo. Escribe la IP y el puerto de la línea \"Dirección IP y puerto\" " +
                "en la pantalla de Depuración inalámbrica de la TV (no el puerto de vinculación) y pulsa Conectar.")
        }
        ip = h
        prefs.edit().putString("ip", h).apply()
        sauKhiKetNoi(h)
    }

    private fun tachIpCong(s: String): Pair<String, Int>? {
        val t = s.trim(); val i = t.lastIndexOf(':')
        if (i <= 0) return null
        val p = t.substring(i + 1).toIntOrNull() ?: return null
        return t.substring(0, i) to p
    }

    private suspend fun sauKhiKetNoi(host: String) {
        daNapCu = false
        tv.quenTivi()
        thongBao = "Leyendo la TV…"
        val (info, gp) = io { tv.docThongTin() }
        goiHome = io { tv.pkgHome }
        thongTin = info; getprop = gp
        val tt = io { tv.docTrangThai() }
        trangThai = tt
        // Công tắc DNS AdGuard theo đúng tivi (24/10/2026, góp ý kimhuenguyen1964: kết nối lại thì công tắc luôn tắt).
        dnsChanQc = io { tv.dnsDangChan() }
        val du = io { DuLieu.nap(ctx) }
        ungVien = du.khopMau(info)
        mauChon = ungVien.firstOrNull()?.first
        ganGiong = du.ganGiong(info, ungVien.map { it.first.id }.toSet())
        tatCaMau = du.mau.sortedBy { it.ten.lowercase() }
        // Điện thoại không có bản sao lưu (gỡ app, đổi máy, người khác dọn) thì lấy bản trên tivi.
        val slMay = kho.doc(info.khoaLuu)
        saoLuu = (slMay ?: io { tv.docSaoLuuTrenTv(info.khoaLuu) })?.also { b ->
            b.ip = host; io { tv.luuSaoLuu(b) }
        }
        saoLuuTuTv = slMay == null && saoLuu != null
        // Xiaomi: sau khi đổi Home, nút INPUT của remote không mở bảng nguồn nữa (upstream #1368) — bật sẵn để Home sạch nhận.
        nutInput = tt.homeHienTai == "com.google.android.apps.tv.launcherx" || info.brand.equals("Xiaomi", ignoreCase = true)
        // Sony 2015 (bo SVP-DTV15): Cài đặt dễ mở ra trống mà muốn bật lại Gỡ lỗi USB lại phải qua Cài đặt — tự tắt ADB là
        // khoá chủ nhà ở ngoài, không sửa được nữa (25/10/2026, 2 máy phải reset cứng). Mặc định giữ ADB, vẫn tắt được tay.
        giuAdb = info.device == "SVP-DTV15"
        laMayChieu = tv.coVeMayChieu(tt, info)
        man = if (dangKhoiPhuc) Man.KHOI_PHUC else Man.THONG_TIN
    }

    var dangKhoiPhuc by mutableStateOf(false)
    var khoiPhucXong by mutableStateOf(false)

    // ---------- Lập kế hoạch ----------

    fun lapKeHoach() = chay("Comparando con ${tatCaMau.size.takeIf { it > 0 } ?: "los"} modelos de TV…") {
        val tt = trangThai ?: return@chay
        val du = io { DuLieu.nap(ctx) }
        val ds = io { KeHoach.lap(du, mauChon, tt) }
        muc = ds
        chon.clear(); ds.forEach { chon[it.pkg] = it.chonSan }
        man = Man.CHON_GOI
    }

    fun soDaChon() = chon.count { it.value }

    // ---------- Tắt gói ----------

    fun batDauTat() = chay("Respaldando…") {
        val info = thongTin ?: return@chay
        nhatKy.clear(); ketQua = null; tienDo = 0f
        manTruocHome = Man.DANG_TAT
        man = Man.DANG_TAT
        val sl = io { tv.damBaoSaoLuu(info, getprop, ip) }
        saoLuu = sl
        ghi("Respaldo guardado (${sl.goiTruoc.size} paquetes, ${sl.daTatTruoc.size} ya estaban desactivados).")
        val ds = muc.filter { chon[it.pkg] == true }.map { it.pkg }
        val kq = try {
            io { tv.tatGoi(ds, sl, epSetupwraith) { s -> ghiTuIo(s) } }
        } catch (e: ThaoTacTv.TiviKhoiDongLaiException) {
            zone.vgs.tvsach.Dem.gui(ctx, "don-loi", tenTiviDem(), e.daTat, "khoi-dong-lai:${e.goi}")
            saoLuu = kho.doc(info.khoaLuu); taiLaiSaoLuu(); throw e
        } catch (e: ThaoTacTv.MatKetNoiTvException) {
            zone.vgs.tvsach.Dem.gui(ctx, "don-loi", tenTiviDem(), e.daTat, "mat-ket-noi:${e.goi}")
            saoLuu = kho.doc(info.khoaLuu); taiLaiSaoLuu(); throw e
        }
        ketQua = kq
        zone.vgs.tvsach.Dem.gui(ctx, "don", tenTiviDem(), kq.daTat.size, if (mauChon != null) "mau:${mauChon!!.id}" else "luat-chung")
        ghi("Listo: ${kq.daTat.size} desactivados, ${kq.boQua.size} conservados, ${kq.loi.size} fallaron.")
        trangThai = io { tv.docTrangThai() }
        saoLuu = kho.doc(info.khoaLuu)
        taiLaiSaoLuu()
        chuanBiDock()
    }

    /**
     * Bỏ qua bước dọn, chỉ cài / đổi Home sạch (góp ý github.com/vgscity/vgs-cleantv/issues/1, 22/10/2026). Sao lưu VẪN
     * chạy (vài giây): đó là đường lui duy nhất để Khôi phục.
     */
    fun boQuaDon() = chay("Respaldando…") {
        val info = thongTin ?: return@chay
        saoLuu = io { tv.damBaoSaoLuu(info, getprop, ip) }
        taiLaiSaoLuu()
        chuanBiDock()
        manTruocHome = Man.CHON_GOI
        man = Man.HOME_SACH
        if (daCoHomeSach) napCauHinhCu()
    }

    // Đi qua hàng đợi Main như ghiTuIo, để dòng tổng kết không chen lên trước dòng tiến độ.
    /** Tên tivi gửi kèm số liệu ẩn danh: hãng + model + device + Android. */
    private fun tenTiviDem(): String = thongTin?.let { "${it.tenHienThi} (${it.device}) Android ${it.android}" } ?: ""

    private fun ghi(s: String) { viewModelScope.launch(Dispatchers.Main) { nhatKy += s } }
    /** 0..1 khi đang tắt/bật lại từng gói, -1 khi không đếm được. */
    var tienDo by mutableStateOf(-1f)

    private fun ghiTuIo(s: String) {
        viewModelScope.launch(Dispatchers.Main) {
            nhatKy += s; thongBao = s
            Regex("""(\d+)/(\d+)""").find(s)?.let { m ->
                val (a, b) = m.destructured
                if (b.toInt() > 0) tienDo = a.toFloat() / b.toFloat()
            }
        }
    }

    // ---------- Khoá trẻ em (23/10/2026) ----------

    /** Một khung giờ khoá: "HH:MM", ngay 1 = Thứ Hai … 7 = Chủ Nhật. */
    data class KhungKhoa(val tu: String, val den: String, val ngay: Set<Int>)
    var khoaTrenTv by mutableStateOf<org.json.JSONObject?>(null)
    var khoaBat by mutableStateOf(true)
    var khoaPin by mutableStateOf("")
    var khoaPin2 by mutableStateOf("")
    val khoaKhung = androidx.compose.runtime.mutableStateListOf<KhungKhoa>()
    var khoaBao by mutableStateOf("")
    /** Đúng PIN trên tivi thì mở bao lâu (anh Alex chốt 60 phút). */
    val KHOA_MO_TAM = 60

    fun moKhoaTre() = chay("Leyendo los ajustes del bloqueo…") {
        val j = io { tv.docKhoa() }
        khoaTrenTv = j; khoaBao = ""; khoaPin = ""; khoaPin2 = ""
        khoaKhung.clear()
        if (j != null) {
            khoaBat = j.optBoolean("bat")
            val ks = j.optJSONArray("khung") ?: org.json.JSONArray()
            for (i in 0 until ks.length()) {
                val k = ks.getJSONObject(i); val ng = k.optJSONArray("ngay") ?: org.json.JSONArray()
                khoaKhung += KhungKhoa(k.optString("tu"), k.optString("den"), (0 until ng.length()).map { ng.getInt(it) }.toSet())
            }
        }
        if (khoaKhung.isEmpty()) { khoaBat = true; khoaKhung += KhungKhoa("21:00", "06:00", (1..7).toSet()) }
        man = Man.KHOA_TRE
    }

    fun luuKhoaTre() = chay("Configurando el bloqueo en la TV…") {
        val coPin = khoaTrenTv?.optBoolean("coPin") == true
        if (!coPin || khoaPin.isNotEmpty()) {
            if (!Regex("^[0-9]{4}$").matches(khoaPin)) error("El PIN debe tener exactamente 4 dígitos.")
            if (khoaPin != khoaPin2) error("Los dos PIN no coinciden.")
        }
        if (khoaKhung.isEmpty()) error("Agrega al menos un horario de bloqueo.")
        val gio = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")
        khoaKhung.forEach { if (!gio.matches(it.tu) || !gio.matches(it.den) || it.ngay.isEmpty()) error("El horario ${it.tu}–${it.den} no es válido.") }
        val j = org.json.JSONObject().put("bat", khoaBat).put("moTamPhut", KHOA_MO_TAM)
            .put("khung", org.json.JSONArray(khoaKhung.map { org.json.JSONObject().put("tu", it.tu).put("den", it.den).put("ngay", org.json.JSONArray(it.ngay.sorted())) }))
        if (khoaPin.isNotEmpty()) {
            val muoi = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
            val bam = java.security.MessageDigest.getInstance("SHA-256").digest((muoi + khoaPin).toByteArray()).joinToString("") { "%02x".format(it) }
            j.put("muoi", muoi).put("pinBam", bam)
        }
        val apk = io { zone.vgs.tvsach.CapNhat.moAsset(ctx, "khoa.apk").readBytes() }
        io { tv.caiKhoa(apk, j.toString()) { thongBao = it } }
        kotlinx.coroutines.delay(2500)
        khoaTrenTv = io { tv.docKhoa() }
        khoaPin = ""; khoaPin2 = ""
        val chay = khoaTrenTv?.optBoolean("dichVuChay") == true
        khoaBao = if (!khoaBat) "Guardado. El bloqueo está apagado."
            else if (chay) "El bloqueo está activado. Durante los horarios la TV muestra la pantalla de bloqueo; el PIN la desbloquea por $KHOA_MO_TAM minutos."
            else "Guardado, pero el servicio de bloqueo aún no funciona. Reinicia la TV y vuelve a revisar."
        zone.vgs.tvsach.Dem.gui(ctx, "khoa", tenTiviDem(), khoaKhung.size, if (khoaBat) "bat" else "tat")
    }

    fun moTamKhoaTre(phut: Int) = chay("Enviando…") {
        io { tv.moTamKhoa(phut) }
        kotlinx.coroutines.delay(800)
        khoaTrenTv = io { tv.docKhoa() }
        khoaBao = if (phut > 0) "Desbloqueada por $phut minutos." else "Bloqueada de nuevo (si está dentro de un horario)."
    }

    fun tatKhoaTre() = chay("Apagando el bloqueo…") {
        io { tv.tatKhoa(go = true) }
        khoaTrenTv = null; khoaBao = "El bloqueo infantil está apagado y se quitó de la TV."
    }

    // ---------- Gửi danh sách gói cho VGS (màn Xong, 23/10/2026) ----------
    var dsDanhGia by mutableStateOf("on")
    var dsGhiChu by mutableStateOf("")
    var dsLienHe by mutableStateOf("")
    var dsDaGui by mutableStateOf(false)

    /** Đọc lại gói đang tắt trên tivi rồi gửi kèm model + đánh giá (người dùng tự bấm). */
    fun guiDanhSach() = chay("Enviando la lista…") {
        val tt = io { tv.docTrangThai() }
        val tv0 = thongTin
        val du = org.json.JSONObject()
            .put("hang", tv0?.manufacturer?.ifBlank { tv0.brand } ?: "").put("model", tv0?.model ?: "").put("device", tv0?.device ?: "")
            .put("android", tv0?.android ?: "").put("mau", mauChon?.id ?: "")
            .put("danhGia", dsDanhGia).put("ghiChu", dsGhiChu.trim()).put("lienHe", dsLienHe.trim())
            .put("goi", org.json.JSONArray(tt.daTat.sorted()))
            .put("giu", org.json.JSONArray((ketQua?.boQua ?: emptyList()).map { it.first }))
        du.put("chanDoan", (io { tv.chanDoanHome() } + "\n# phim-home " + phimHomeGhi).take(3000))
        val ok = io { zone.vgs.tvsach.Dem.guiDanhSach(ctx, du) }
        if (!ok) error("No se pudo enviar (¿sin internet?). Inténtalo más tarde.")
        dsDaGui = true
    }

    fun chupManHinh() = chay("Tomando una captura de la TV…") {
        val bytes = io { tv.chupManHinh() }
        anhTv = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("No se pudo capturar (algunas TV lo bloquean en una entrada HDMI; pulsa Inicio en el control e inténtalo de nuevo).")
    }

    // ---------- Home sạch ----------

    /** App có biểu tượng còn bật, để chọn vào dock. */
    fun appChoDock(): List<String> {
        val tt = trangThai ?: return emptyList()
        val khongDua = ThaoTacTv.HOME_SACH + setOf("com.android.tv.settings", "com.android.settings") + ThaoTacTv.HOME_GOC_BIET
        return tt.coBieuTuong.filter { it in tt.caiDat && it !in tt.daTat && it !in khongDua }
            .sortedWith(compareBy({ p -> UU_TIEN_DOCK.indexOf(p).let { if (it < 0) 99 else it } }, { it }))
    }

    /** Từ màn thông tin: đổi theme cho tivi đã có Home sạch. */
    /** Màn mà nút Back ở Home sạch quay về: vừa dọn → màn dọn; Đổi giao diện → thông tin tivi; Bỏ qua → chọn gói. */
    var manTruocHome by mutableStateOf(Man.DANG_TAT)

    fun moDoiTheme() { chuanBiDock(); manTruocHome = Man.THONG_TIN; man = Man.HOME_SACH; viewModelScope.launch { napCauHinhCu() } }

    /** "Đổi giao diện": mở ra đúng các lựa chọn đang có trên tivi thay vì mặc định. */
    /** Đã nạp config.json của tivi vào các lựa chọn chưa (mỗi lần kết nối một lần — quay lại màn không xoá chỉnh sửa). */
    private var daNapCu = false

    private suspend fun napCauHinhCu() {
        if (daNapCu) return
        daNapCu = true
        val j = runCatching { io { tv.docCauHinhTv() } }.getOrNull() ?: return
        napTuJson(j)
        // Ảnh nền riêng đã đặt từ trước: đọc lại từ tivi để Áp dụng lần sau không thay bằng ảnh theme (23/10/2026).
        // Nhận ra bằng cờ vgs_anh_rieng (từ 2.5.6) hoặc ảnh trên tivi không trùng ảnh của theme nào (bản cũ hơn).
        if (anhNenRieng == null && j.optBoolean("wallpaper")) {
            val anh = runCatching { io { tv.docAnhNenTv() } }.getOrNull()
            if (anh != null) {
                val laAnhTheme = !j.optBoolean("vgs_anh_rieng") && io {
                    zone.vgs.tvsach.dulieu.THEMES.mapNotNull { it.anhNen }.any { a -> runCatching { ctx.assets.open(a).readBytes().contentEquals(anh) }.getOrDefault(false) }
                }
                if (!laAnhTheme) anhNenRieng = anh
            }
        }
    }

    /** Đưa một config.json (của tivi hoặc file cấu hình đã lưu) vào các lựa chọn trên màn Home sạch. */
    private fun napTuJson(j: JSONObject) {
        // Khoá do theme đang dùng ghi (vgs_theme, từ 2.9) không phải lựa chọn của người dùng: đọc như mặc định, không thì
        // đổi sang theme khác vẫn dính (Kiểu Apple TV bật uniform_tiles, dock_glass, ẩn lời chào, dời hàng app).
        val theme = zone.vgs.tvsach.dulieu.THEMES.firstOrNull { it.id == j.optString("vgs_theme") }
        theme?.let { themeChon = it }
        fun cuaTheme(k: String) = theme?.cauHinh?.containsKey(k) == true
        amLich = j.optBoolean("lunar_date", amLich)
        tenDuoiApp = j.optBoolean("always_show_names", false)
        tenToTrenDock = j.optBoolean("dock_title", true)
        toMauOApp = j.optBoolean("icon_tile_color", true)
        thuVienApp = j.optBoolean("app_library", false)
        dongHoKhiYen = j.optInt("home_idle_clock_minutes", 0)
        tiviYeu = j.optBoolean("reduced_motion", false)
        hieuUngKinh = !cuaTheme("dock_glass") && j.optBoolean("dock_glass", false)
        lamToiNen = j.optInt("wallpaper_dim", 0)
        hienThongSo = j.optBoolean("show_stats", false)
        doiHang = if (j.optBoolean("rows_middle", false)) HANG_GIUA else if (cuaTheme("rows_offset_dp")) 0 else j.optInt("rows_offset_dp", 0)
        nutDangPhat = j.optBoolean("now_playing", true)
        coChuDau = j.optInt("header_scale", 100)
        motHang = j.optBoolean("single_row", false)
        anLoiChao = !cuaTheme("show_greeting") && !j.optBoolean("show_greeting", true)
        giuAmLuong = !cuaTheme("key_filter") && !j.optBoolean("key_filter", true) && !laTclGoogleTv
        oDeuNhau = !cuaTheme("uniform_tiles") && j.optBoolean("uniform_tiles", false)
        moKhiBat = j.optBoolean("open_on_start_choice", false)
        // Chỉ nạp số cột do người dùng chọn ở đây (vgs_so_cot), không lấy columns của theme.
        soCot = if (j.optBoolean("vgs_so_cot", false)) j.optInt("columns", 0) else 0
        if (j.has("handle_view_inputs")) nutInput = j.optBoolean("handle_view_inputs", nutInput)
        // Chỉ nạp lại khoá NGƯỜI DÙNG đặt ở Nâng cao (ghi trong vgs_nang_cao), không lấy khoá do theme ghi
        // (background_color, sky_intensity…) — nếu không, đổi theme sau đó sẽ bị kẹt giá trị của theme cũ.
        nangCao.clear()
        val daDat = j.optJSONArray("vgs_nang_cao")?.let { a -> (0 until a.length()).map { a.optString(it) }.toSet() } ?: emptySet()
        dsNangCao.filter { it.khoa in daDat }.forEach { tc -> zone.vgs.tvsach.dulieu.NangCao.docChu(tc, j)?.let { nangCao[tc.khoa] = it } }
        nutManCho = j.optBoolean("dream_button", nutManCho)
        gonNut = j.optBoolean("top_compact", gonNut)
        j.optJSONArray("dock_apps")?.let { a -> val ds = (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() && !it.startsWith(GOI_KENH) }; if (ds.isNotEmpty()) { dock.clear(); dock.addAll(ds.take(DOCK_TOI_DA)) } }
        kenhVtv.clear()
        j.optJSONArray("link_tiles")?.let { a -> (0 until a.length()).map { a.optString(it).substringBefore('|') }.filter { k -> KENH_VTV.any { it.first == k } }.forEach { kenhVtv.add(it) } }
        j.optString("greeting_name").takeIf { it.isNotBlank() }?.let { tenChao = it }
        j.optString("weather_place").takeIf { it.isNotBlank() }?.let { ten ->
            thanhPho = THANH_PHO.firstOrNull { it.ten == ten }
                ?: zone.vgs.tvsach.dulieu.ThanhPho(ten, j.optDouble("weather_lat"), j.optDouble("weather_lon")).takeIf { !it.lat.isNaN() && !it.lon.isNaN() }
        }
    }

    private fun chuanBiDock() {
        if (dock.isNotEmpty()) return
        dock.addAll(appChoDock().filter { it in UU_TIEN_DOCK }.take(4))
    }

    /** Dời app trong dock lên (-1) / xuống (+1). */
    fun dichDock(p: String, buoc: Int) {
        val i = dock.indexOf(p); val j = i + buoc
        if (i < 0 || j !in dock.indices) return
        dock[i] = dock[j]; dock[j] = p
    }

    fun doiDock(p: String) {
        if (p in dock) dock.remove(p) else if (dock.size < DOCK_TOI_DA) dock.add(p)
    }

    fun caiHomeSach() = chay("Instalando el inicio limpio…") {
        if (laAndroidCu) error("El inicio limpio necesita Android 8 o más nuevo; esta TV / TV box tiene Android ${thongTin?.android}. Aquí TuneTV solo puede quitar anuncios y apps basura.")
        if (laFireTv) error("Fire TV no deja que ninguna app reemplace el inicio (Amazon lo bloquea). En Fire TV, TuneTV solo puede quitar anuncios y apps basura.")
        val sl = saoLuu ?: io { tv.damBaoSaoLuu(thongTin!!, getprop, ip) }.also { saoLuu = it }
        nhatKy.clear()
        val apk = io { zone.vgs.tvsach.CapNhat.moAsset(ctx, tv.apkHome).readBytes() }
        val cauHinh = cauHinhHome()
        val anh = anhNenTheme()
        val bo = boAnhTheme()
        napAnhKenh()
        val loiCai = io { tv.caiLauncher(apk, sl, cauHinh, anh, bo, nutInput, nutHome || dungPhimRemote) { s -> ghiTuIo(s) } }
        // caiLauncher báo lỗi là đã tự trả Home gốc: huỷ canh gác trên tivi.
        if (loiCai != null) { io { tv.boGac() }; error(loiCai) }
        if (nutDangPhat) io { tv.choPhepDangPhat() }
        if (dnsChanQc) io { tv.batDnsChanQc(sl) }
        if (nutManCho) io { tv.chuanBiManCho(sl) }
        thuPhimHome(sl)
        zone.vgs.tvsach.Dem.gui(ctx, "home", tenTiviDem(), dock.size, "theme:${themeChon.id}")
        if (ghimMayChieu.isNotEmpty()) zone.vgs.tvsach.Dem.gui(ctx, "may-chieu", tenTiviDem(), ghimMayChieu.size, ghimMayChieu.keys.joinToString(",").take(110))
        homeXong = true
        ghi("El inicio limpio está funcionando. Pulsa Inicio en el control para verlo.")
        taiLaiSaoLuu()
        man = Man.XONG
    }

    /**
     * Bấm Home thật trên tivi, tắt gói còn giành nút Home (ThaoTacTv.damBaoPhimHome). Không được thì báo rõ, không
     * báo "xong" suông; gửi tên gói giành Home về bộ đếm để biết hãng nào bị (22/10/2026).
     */
    private suspend fun thuPhimHome(sl: zone.vgs.tvsach.dulieu.BanSaoLuu?) {
        val kq = io { tv.damBaoPhimHome(sl) { s -> ghiTuIo(s) } }
        // Đã thử xong (được, hoặc đã tự bật lại Home gốc): tivi thôi canh gác. Rớt kết nối trước dòng này thì
        // canh gác trên tivi tự trả Home gốc sau ThaoTacTv.GAC_GIAY giây.
        io { tv.boGac() }
        veApp()
        phimHomeOk = kq.ok
        phimHomeGhi = (if (kq.ok) "ok" else if (kq.gianh.isNotEmpty()) "lui" else "loi") + " gianh=" + kq.gianh.joinToString(",") + " conLai=" + kq.conLai + " lyDo=" + kq.lyDo
        if (kq.lyDo == "chuyen") { nutHome = true; ghi("La TV no deja desactivar el inicio de fábrica, así que sigue activo, pero cada vez que aparece se abre encima el inicio limpio (\"El control envía una tecla Inicio rara\" está activado). Restaurar deja todo como estaba.") }
        else if (kq.daBatBatPhim) { nutHome = true; ghi("Se activó \"El control envía una tecla Inicio rara\" porque otra app tenía el botón Inicio.") }
        if (kq.gianh.isNotEmpty() || !kq.ok || kq.lyDo.isNotBlank()) {
            // ok: đã tắt và chạy; lui: đã tắt nhưng không được nên bật lại; loi: không tắt gì. lyDo (chuyen, khong-tat,
            // setupwraith, cvte) gắn sau dấu # để biết nhánh nào.
            val dau = if (kq.ok) "ok:" else if (kq.gianh.isNotEmpty()) "lui:" else "loi:"
            zone.vgs.tvsach.Dem.gui(ctx, "home-phim", tenTiviDem(), kq.gianh.size,
                dau + (kq.gianh + kq.conLai).filter { it.isNotBlank() }.distinct().joinToString(",").take(90) + (if (kq.lyDo.isNotBlank()) "#" + kq.lyDo else ""))
        }
        if (kq.ok && kq.gianh.isNotEmpty()) {
            ghi("Se desactivó ${kq.gianh.joinToString()} porque se quedaba con el botón Inicio. Llega a los menús de imagen y sonido del fabricante con el botón Ajustes del inicio limpio; Restaurar lo devuelve.")
            taiLaiSaoLuu()
        }
        if (!kq.ok) {
            val goi = kq.conLai.ifBlank { "desconocido" }
            loi = if (kq.lyDo == "cvte")
                "El botón Inicio de la TV sigue abriendo el inicio de fábrica ($goi). Placa CVTE: la app nunca lo desactiva, porque una TV se quedó trabada en la animación de arranque por eso. Reinicia la TV una vez y pulsa Inicio otra vez; si sigue abriendo el inicio anterior, abre el inicio limpio desde la lista de apps."
            else if (kq.lyDo == "setupwraith")
                "El botón Inicio de la TV sigue abriendo el inicio de Google ($goi). En Google TV también hay que desactivar setupwraith para cambiar el inicio, y el Wi‑Fi guardado durante la configuración le pertenece, así que la app lo deja (desactivarlo quitaría el Wi‑Fi). La TV funciona como antes. Para cambiar el inicio por completo: en la TV, Ajustes > Red, olvida el Wi‑Fi y vuelve a conectarte (o conecta un cable de red), reconecta la app, marca setupwraith (grupo Google Home) en la lista de paquetes y limpia otra vez."
            else if (kq.lyDo == "khong-tat")
                "La TV no deja desactivado el inicio de fábrica ($goi): se vuelve a activar sola, y abrir el inicio limpio encima tampoco funcionó. La app dejó la TV como estaba. Abre el inicio limpio desde la lista de apps."
            else
                "El botón Inicio de la TV sigue abriendo '$goi' en vez del inicio limpio. La app dejó la TV como estaba."
        }
    }

    private fun cauHinhHome(): String = JSONObject().apply {
        // Ô kênh VTV đứng đầu dock, rồi tới app người dùng chọn.
        val kenh = if (coVtvGo) KENH_VTV.filter { it.first in kenhVtv } else emptyList()
        if (kenh.isNotEmpty()) put("link_tiles", JSONArray(kenh.map { (ten, ma, mau) -> "$ten|$GOI_VTVGO|vtvgo://play?mediaType=CHANNEL&contentId=$ma|$mau" }))
        put("dock_apps", JSONArray(kenh.map { GOI_KENH + it.first.lowercase() } + dock.toList()))
        if (tenChao.isNotBlank()) put("greeting_name", tenChao.trim())
        themeChon.ghiVao(this)
        if (anhNenRieng != null) { put("wallpaper", true); put("vgs_anh_rieng", true) }
        // Tivi Xiaomi bản Trung Quốc chỉ chuyển nguồn qua trình phát Xiaomi (upstream examples/xiaomi-mitv-astp0): Home sạch
        // (bản vá CleanTV) đổi tên dòng HDMI1-3 / AV ra số cổng 23-25 / 2 khi input_action là EXTSRC_PLAY (23/10/2026).
        if (trangThai?.caiDat?.contains("com.xiaomi.mitv.tvplayer") == true) put("input_action", "com.xiaomi.mitv.tvplayer.EXTSRC_PLAY")
        put("dream_button", nutManCho)
        put("top_compact", gonNut)
        thanhPho?.let { put("weather_lat", it.lat); put("weather_lon", it.lon); put("weather_place", it.ten) }
        if (ghimMayChieu.isNotEmpty()) put("projector_actions", JSONArray(ghimMayChieu.map { (comp, nhan) -> "$nhan=$comp" }))
        put("lunar_date", amLich)
        // Âm lịch tính theo múi giờ: launcher tự chọn theo ngôn ngữ tivi, tivi để tiếng Anh ở VN thì lệch một ngày quanh
        // ngày sóc (Formovie L206, 22/10/2026: 12/9 thay vì 13/9). Ghi múi giờ của điện thoại khi là +7 / +8.
        if (amLich) (java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3_600_000).takeIf { it == 7 || it == 8 }?.let { put("lunar_utc_offset", it) }
        // Launcher 2.6: nút Input trên remote (VIEW_INPUTS) mở bảng chọn nguồn của Home sạch — đi cùng công tắc nút Input.
        if (nutInput) put("handle_view_inputs", true)
        if (tenDuoiApp) put("always_show_names", true)
        if (!tenToTrenDock) put("dock_title", false)
        if (!toMauOApp) put("icon_tile_color", false)
        if (thuVienApp) { put("app_library", true); put("library_button", true) }
        if (dongHoKhiYen > 0) put("home_idle_clock_minutes", dongHoKhiYen)
        if (tiviYeu) put("reduced_motion", true)
        if (hieuUngKinh) { put("dock_glass", true); put("app_glow", true) }
        if (lamToiNen > 0) put("wallpaper_dim", lamToiNen)
        if (hienThongSo) put("show_stats", true)
        // Giữ OK trên ô app: menu Di chuyển / Thêm vào dock / Bỏ khỏi Home / Thông tin (upstream #464, #746; tắt mặc định
        // ở upstream). CleanTV bật sẵn vì người dùng xin đổi chỗ dock ngay trên tivi (22/10/2026); Nâng cao vẫn tắt được.
        put("tile_menu", true)
        // Giữa màn hình bỏ khoảng dời của theme (Kiểu Apple TV hạ dock 90dp) để hàng app nằm đúng giữa.
        if (doiHang == HANG_GIUA) { put("rows_middle", true); put("rows_offset_dp", 0) } else if (doiHang != 0) put("rows_offset_dp", doiHang)
        if (!nutDangPhat) put("now_playing", false)
        if (coChuDau != 100) put("header_scale", coChuDau)
        if (motHang) { put("dock_row", true); put("single_row", true) }
        // Một hàng + thư viện app: viên Thư viện chen vào hàng app, đã có nút trên thanh trên cùng (góp ý Giun2911 25/10/2026).
        if (motHang && thuVienApp) put("show_library_pill", false)
        if (anLoiChao) put("show_greeting", false)
        if (oDeuNhau) put("uniform_tiles", true)
        if (moKhiBat) put("open_on_start_choice", true)
        if (soCot > 0) { put("columns", soCot); put("vgs_so_cot", true) }
        // Phím APPS (KEYCODE_ALL_APPS) chỉ là nút Home với que / tivi Xiaomi hoặc khi bật "Remote gửi phím Home lạ";
        // tivi khác (Sony VH2: nút Apps mở Home sạch như Back, abc999roses 24/10/2026) để phím cho tivi.
        put("all_apps_home", nutHome || thongTin?.brand?.equals("Xiaomi", ignoreCase = true) == true)
        // Ảnh riêng làm nền: tắt ánh sáng sau dock theo màu app (ambient_glow), không thì ảnh gia đình bị ám đỏ / vàng /
        // xanh mỗi lần đổi ô (góp ý dangngoctri2010, 23/10/2026). Nâng cao ghi sau nên người dùng vẫn bật lại được.
        if (anhNenRieng != null) put("ambient_glow", false)
        // Ảnh riêng hiện đúng như ảnh (25/10/2026, góp ý ảnh bị phủ mờ, loang màu): bỏ lớp phủ bầu trời theo giờ của theme,
        // giải mã màu 32-bit thay vì RGB_565. Nâng cao ghi sau nên vẫn đổi lại được.
        if (anhNenRieng != null) { put("sky_intensity", 0); put("wallpaper_true_color", true) }
        // Tivi Xiaomi bản Trung Quốc không có tiếng Việt: Home sạch hiện chữ Trung (25/10/2026). Tivi đang để tiếng Trung mà
        // app dùng tiếng Việt thì ép Home sạch sang tiếng Việt (locale của bản vá launcher).
        if (zone.vgs.tvsach.NgonNgu.tiengViet && Regex("""\[(persist\.sys\.locale|ro\.product\.locale)\]: \[zh""").containsMatchIn(getprop)) put("locale", "vi")
        // Phím remote của mẫu (app_keys, settings_key_keycodes…), HomeKeyService đọc. LUÔN ghi app_keys, kể cả khi bật
        // nút Input: 2.6.5 bỏ app_keys lúc đó (InputKeyService cũng đọc, sợ mở app hai lần — upstream tcl-55c725) mà nút
        // Input tự bật trên mọi Google TV, nên TCL Google TV mất hẳn nút YouTube (góp ý heavensky95, TCL G10, 24/10/2026).
        // Mở cùng một app hai lần chỉ là đưa nó lên trước.
        // TCL Google TV không dùng nút app remote: thôi lọc phím để giữ âm lượng lặp được (Nâng cao đặt key_filter thì đè sau).
        val tclGoogleTv = thongTin?.brand?.equals("TCL", ignoreCase = true) == true && trangThai?.caiDat?.contains("com.google.android.apps.tv.launcherx") == true
        if ((tclGoogleTv && !dungPhimRemote) || giuAmLuong) put("key_filter", false)
        if (dungPhimRemote) phimRemote()?.let { ph ->
            ph.keys().forEach { k -> if (!has(k)) put(k, ph.get(k)) }
            // Nút Web của remote TCL (4159, upstream tcl-g10-google-tv): mở trình duyệt nếu tivi có.
            if (thongTin?.brand?.equals("TCL", ignoreCase = true) == true) {
                val web = TRINH_DUYET_TV.firstOrNull { trangThai?.caiDat?.contains(it) == true }
                val ds = optJSONArray("app_keys")
                if (web != null && ds != null && (0 until ds.length()).none { ds.optString(it).startsWith("4159:") }) ds.put("4159:$web")
            }
        }
        // Nâng cao ghi sau cùng: người dùng đã chủ động đặt thì đè lên giá trị theme.
        val daDat = dsNangCao.mapNotNull { tc -> nangCao[tc.khoa]?.let { chu -> zone.vgs.tvsach.dulieu.NangCao.giaTri(tc, chu)?.let { put(tc.khoa, it); tc.khoa } } }
        if (daDat.isNotEmpty()) put("vgs_nang_cao", JSONArray(daDat))
    }.toString()

    // ---------- Lưu / mở file cấu hình (góp ý 9000000, 23/10/2026) ----------
    var cauHinhBao by mutableStateOf("")

    /** Ghi mọi lựa chọn Home sạch (config.json + theme, nút Home, DNS, ảnh nền riêng) ra một file JSON người dùng chọn chỗ lưu. */
    fun xuatCauHinh(uri: android.net.Uri) = chay("Guardando el archivo de ajustes…") {
        val j = JSONObject(cauHinhHome())
        j.put("vgs_file", 1).put("vgs_theme", themeChon.id).put("vgs_nut_home", nutHome).put("vgs_dns", dnsChanQc)
        thongTin?.let { j.put("vgs_tivi", it.tenHienThi) }
        anhNenRieng?.let { j.put("vgs_anh_nen", android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)) }
        io { ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(j.toString(1).toByteArray()) } ?: error("no se pudo abrir el archivo") }
        cauHinhBao = "Archivo de ajustes guardado."
    }

    /**
     * Xem trước lệnh (24/10/2026, học cleanyourtv-app "Customise shows every command"): các lệnh chính app sẽ gửi lên
     * tivi, theo thứ tự, trước khi bấm. Dòng "#" là chú thích. Gói lõi Android / Home đang dùng app vẫn tự bỏ qua lúc chạy.
     */
    fun lenhDon(): List<String> = buildList {
        add("# 1. Respaldo (solo lectura, no cambia nada)")
        add("pm list packages -d")
        add("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME")
        add("settings get secure enabled_accessibility_services")
        add("settings get global private_dns_mode")
        val ds = muc.filter { chon[it.pkg] == true }.map { it.pkg }
        add("# 2. Desactivar ${ds.size} paquetes (solo desactivar, nunca desinstalar; Restaurar los vuelve a activar)")
        ds.forEach { add("pm disable-user --user 0 $it") }
    }

    fun lenhHome(): List<String> = buildList {
        val pk = tv.pkgHome
        val goc = trangThai?.homeHienTai?.substringBefore('/').orEmpty()
        if (!daCoHomeSach) {
            add("# Instalar el inicio limpio (APK incluido en la app)")
            add("pm install -r ${tv.apkHome}")
            add("cmd package compile -m speed -f $pk")
        }
        add("# Guardar el aspecto")
        add("content write --uri content://$pk.theme/config.json")
        if (!daCoHomeSach && goc.isNotBlank() && !goc.startsWith(pk)) {
            add("# Desactivar el inicio de fábrica y dejar el inicio limpio como predeterminado (la TV vigila: si se cae la conexión, vuelve el inicio de fábrica)")
            add("pm disable-user --user 0 $goc")
        }
        add("cmd package set-home-activity --user 0 ${tv.cmpHome}")
        if (nutHome || dungPhimRemote) add("settings put secure enabled_accessibility_services $pk/com.gathin.tvhome.HomeKeyService")
        if (nutInput) add("settings put secure enabled_accessibility_services $pk/com.gathin.tvhome.InputKeyService")
        if (dnsChanQc) add("settings put global private_dns_specifier dns.adguard-dns.com")
        add("# Probar el botón Inicio real")
        add("input keyevent KEYCODE_HOME")
    }

    /** Lưu kịch bản khôi phục cho máy tính (dulieu/KichBanKhoiPhuc) từ bản sao lưu của tivi đang mở. */
    fun xuatKichBan(uri: android.net.Uri) = chay("Guardando el script de restauración…") {
        val sl = saoLuu ?: error("Todavía no hay respaldo de esta TV.")
        val chu = zone.vgs.tvsach.dulieu.KichBanKhoiPhuc.tao(sl, zone.vgs.tvsach.NgonNgu.tiengViet)
        io { ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(chu.toByteArray()) } ?: error("no se pudo abrir el archivo") }
        cauHinhBao = "Script de restauración guardado."
    }

    /** Mở file cấu hình đã lưu: điền lại mọi lựa chọn, bấm Áp dụng mới đưa lên tivi. */
    fun nhapCauHinh(uri: android.net.Uri) = chay("Abriendo el archivo de ajustes…") {
        val chu = io { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } } ?: error("No se pudo leer el archivo.")
        val j = runCatching { JSONObject(chu) }.getOrNull()?.takeIf { it.has("vgs_file") || it.has("dock_apps") }
            ?: error("Este no es un archivo de ajustes de TuneTV.")
        napTuJson(j)
        zone.vgs.tvsach.dulieu.THEMES.firstOrNull { it.id == j.optString("vgs_theme") }?.let { themeChon = it }
        if (j.has("vgs_nut_home")) nutHome = j.optBoolean("vgs_nut_home")
        if (j.has("vgs_dns")) dnsChanQc = j.optBoolean("vgs_dns")
        anhNenRieng = j.optString("vgs_anh_nen").takeIf { it.isNotBlank() }?.let { runCatching { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }.getOrNull() }
        cauHinhBao = "Archivo de ajustes cargado" + (j.optString("vgs_tivi").takeIf { it.isNotBlank() }?.let { " (de $it)" } ?: "") +
            ". Pulsa \"Aplicar a la TV\" para enviarlo."
    }

    /** Ảnh logo kênh (assets/kenh, ảnh nhỏ của VTVgo) cho các kênh đang chọn; launcher không có mạng nên gửi kèm. */
    private suspend fun napAnhKenh() {
        tv.anhKenh = if (!coVtvGo) emptyList() else kenhVtv.mapNotNull { ten ->
            val f = ten.lowercase() + ".jpg"
            runCatching { f to io { ctx.assets.open("kenh/$f").readBytes() } }.getOrNull()
        }
    }

    private suspend fun anhNenTheme(): ByteArray? = anhNenRieng ?: themeChon.anhNen?.let { a -> io { ctx.assets.open(a).readBytes() } }

    /** Ảnh nền người dùng chọn từ điện thoại (góp ý 23/10/2026), đã thu về 1920×1080 JPEG; null = theo theme. */
    var anhNenRieng by mutableStateOf<ByteArray?>(null)

    /** Danh sách ảnh trên tivi / USB (null = chưa mở hộp chọn). */
    var anhTrenTv by mutableStateOf<List<String>?>(null)

    fun moAnhTrenTv() = chay("Buscando fotos en la TV…") {
        anhTrenTv = io { tv.timAnhTrenTv() }
    }

    fun chonAnhTrenTv(duongDan: String) = chay("Leyendo la foto desde la TV…") {
        anhTrenTv = null
        val du = io { tv.docFileTv(duongDan) } ?: error("No se pudo leer esta foto (muy grande o dañada); prueba con otra.")
        anhNenRieng = io { zone.vgs.tvsach.dulieu.AnhNen.chuanBiByte(du) } ?: error("No se pudo abrir esta foto (convierte HEIC a JPG); prueba con otra.")
    }

    fun chonAnhNen(uri: android.net.Uri) = chay("Preparando la foto…") {
        anhNenRieng = io { zone.vgs.tvsach.dulieu.AnhNen.chuanBi(ctx, uri) } ?: error("No se pudo leer esta foto; prueba con otra.")
    }

    /** Bộ ảnh xoay vòng của theme: mọi file ảnh trong thư mục assets boAnh. */
    private suspend fun boAnhTheme(): List<Pair<String, ByteArray>>? = if (anhNenRieng != null) null else themeChon.boAnh?.let { thu ->
        io { (ctx.assets.list(thu) ?: emptyArray()).sorted().map { it to ctx.assets.open("$thu/$it").readBytes() } }
    }

    /** Đổi theme / dock cho tivi đã có Home sạch. */
    fun apDungTheme() = chay("Aplicando el tema…") {
        val json = cauHinhHome()
        val anh = anhNenTheme()
        napAnhKenh()
        // Cài đè launcher đi kèm app (bản mới có dòng thời tiết…), giữ dữ liệu và dịch vụ trợ năng.
        thongBao = "Actualizando el inicio limpio…"
        if (!io { tv.capNhatLauncher(zone.vgs.tvsach.CapNhat.moAsset(ctx, tv.apkHome).readBytes()) }) {
            loi = "No se pudo actualizar el inicio limpio en la TV, así que el aspecto nuevo no se verá completo. Inténtalo de nuevo."
            return@chay
        }
        if (anh != null) thongBao = "Enviando el fondo (${anh.size / 1024} KB)…"
        val bo = boAnhTheme()
        if (bo != null) thongBao = "Enviando ${bo.size} fotos horizontales…"
        io { tv.dayCauHinh(json, anh, bo) }
        if (dungPhimRemote) io { tv.batBatPhimHome() }
        // Đổi giao diện cũng bật / tắt DNS AdGuard theo công tắc (trước 2.7.3 chỉ lúc cài Home sạch mới đặt).
        val dangChan = io { tv.dnsDangChan() }
        if (dnsChanQc && !dangChan) saoLuu?.let { sl -> io { tv.batDnsChanQc(sl) } }
        if (!dnsChanQc && dangChan) io { tv.tatDnsChanQc(saoLuu) }
        if (nutDangPhat) io { tv.choPhepDangPhat() }
        if (nutManCho) io { tv.chuanBiManCho(saoLuu) }
        thongBao = "Probando el botón Inicio de la TV…"
        val sl = saoLuu ?: thongTin?.let { tt -> runCatching { io { tv.damBaoSaoLuu(tt, getprop, ip) } }.getOrNull() }?.also { saoLuu = it }
        thuPhimHome(sl)
        zone.vgs.tvsach.Dem.gui(ctx, "home", tenTiviDem(), dock.size, "doi:${themeChon.id}")
        if (ghimMayChieu.isNotEmpty()) zone.vgs.tvsach.Dem.gui(ctx, "may-chieu", tenTiviDem(), ghimMayChieu.size, ghimMayChieu.keys.joinToString(",").take(110))
        homeXong = true
        man = Man.XONG
    }

    fun khoiDongLaiTv() = chay("Reiniciando la TV…") {
        io { runCatching { adb.shell("reboot") } }
        runCatching { io { adb.disconnect() } }
        thongBao = ""
        loi = "La TV se está reiniciando. Cuando vuelva, pulsa Conectar otra vez para revisar (aparecerán los paquetes que se reactiven al arrancar)."
        man = Man.KET_NOI
    }

    /**
     * Tự tắt ADB khi xong (24/10/2026, học cleanyourtv-app): đang ở màn Xong mà người dùng rời app thì hẹn TIVI tự tắt
     * ADB sau 60 giây (hẹn giờ phải nằm trên tivi: Android 14 đóng băng app nằm nền). Quay lại trước đó thì huỷ; quá
     * hạn thì về màn chính, báo đã tự tắt. Bật "Giữ ADB bật cho lần sau" thì không hẹn.
     */
    var giuAdb by mutableStateOf(false)
    var daTuTatAdb by mutableStateOf(false)
    private var daHenTatAdb = false

    fun henTuTatAdb() {
        if (giuAdb || man != Man.XONG || dangBan) return
        daHenTatAdb = true
        viewModelScope.launch { runCatching { io { tv.henTatAdbTrenTv(60) } }.onFailure { daHenTatAdb = false } }
    }

    fun huyTuTatAdb() {
        if (!daHenTatAdb) return
        daHenTatAdb = false
        viewModelScope.launch {
            // Huỷ không được (tivi đã tắt ADB, kết nối mất): coi như đã tự tắt.
            if (runCatching { io { tv.huyTatAdbTrenTv() } }.isFailure) {
                runCatching { io { adb.disconnect() } }
                datLai()
                daTuTatAdb = true
                man = Man.CHINH
            }
        }
    }

    fun tatAdbVaKetThuc() = chay("Apagando ADB en la TV…") {
        io { tv.tatAdb() }
        runCatching { io { adb.disconnect() } }
        val host = ip
        val conMo = io { Thread.sleep(2500); moCong(host, 5555, 1500) }
        loi = if (conMo) "El puerto 5555 sigue abierto. En Opciones de desarrollador de la TV, apaga la depuración USB / de red y reinicia."
        else ""
        thongBao = ""
        datLai()
        man = Man.CHINH
    }

    // ---------- Khôi phục ----------

    fun moKhoiPhuc(b: BanSaoLuu) {
        dangKhoiPhuc = true
        ip = b.ip
        man = Man.KET_NOI
    }

    fun chayKhoiPhuc(goLauncher: Boolean) = chay("Restaurando…") {
        val info = thongTin ?: return@chay
        val sl = kho.doc(info.khoaLuu) ?: error("Este teléfono no tiene respaldo de esta TV.")
        nhatKy.clear()
        val loiKp = io { tv.khoiPhuc(sl, goLauncher) { s -> ghiTuIo(s) } }
        veApp()
        zone.vgs.tvsach.Dem.gui(ctx, "khoi-phuc", tenTiviDem(), sl.daTatBoiApp.size)
        loiKp.forEach { ghi("Error: $it") }
        if (loiKp.isEmpty() && sl.daTatBoiApp.isEmpty() && !sl.launcherDaCai && !sl.dnsDaDoi) {
            kho.xoa(sl.khoa)
            io { tv.xoaSaoLuuTrenTv() }
            khoiPhucXong = true
            ghi("Restauración completa; respaldo eliminado.")
        } else ghi("Restaurado en parte; el respaldo se conserva para que puedas volver a intentarlo.")
        saoLuu = kho.doc(info.khoaLuu)
        taiLaiSaoLuu()
    }

    /**
     * Fire TV / Fire OS (23/10/2026, Fire TV Cube 2 AFTR): Amazon giữ Home cho launcher của họ — đặt Home bị bỏ qua,
     * launcher là gói được bảo vệ không tắt được, dịch vụ trợ năng không nhận phím Home (upstream TROUBLESHOOTING
     * "Fire TV"). Chỉ dọn được, không thay Home.
     */
    val laFireTv: Boolean get() = (thongTin?.model?.startsWith("AFT") == true) ||
        trangThai?.caiDat?.contains("com.amazon.tv.launcher") == true

    /** Android 7 trở xuống (SDK < 26): Home sạch cần Android 8+ (23/10/2026, hộp MXQ Pro Android 7.1). Chỉ dọn được. */
    val laAndroidCu: Boolean get() = thongTin?.sdk?.let { it in 1..25 } == true

    /** Không cài được Home sạch trên tivi này: chỉ dọn. */
    val khongCaiHome: Boolean get() = laFireTv || laAndroidCu

    /** Tivi có Home sạch (bản nào cũng được): đã từng dọn bằng CleanTV / CleanYourTV. */
    val coDauCleanTv: Boolean get() = trangThai?.let { tt -> ThaoTacTv.HOME_SACH.any { it in tt.caiDat } } == true

    /** Gói đang tắt trên tivi khi không có bản sao lưu (null = chưa đọc). */
    var goiTatKhongSaoLuu by mutableStateOf<List<String>?>(null)

    fun docGoiTatKhongSaoLuu() = chay("Leyendo los paquetes desactivados…") {
        goiTatKhongSaoLuu = io { tv.goiTatBoiNguoiDung() }
    }

    // ---------- Bật lại từng app (issue vgs-cleantv#3, 23/10/2026) ----------
    /** Gói đang tắt kiểu disable-user, đọc thẳng từ tivi (không cần bản sao lưu); null = chưa đọc. */
    var dsTatTv by mutableStateOf<List<String>?>(null)
    val chonBatLai = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    var batLaiBao by mutableStateOf("")

    fun docDsTat() = chay("Leyendo las apps desactivadas en la TV…") {
        dsTatTv = io { tv.goiTatBoiNguoiDung() }; chonBatLai.clear(); batLaiBao = ""
    }

    fun batLaiDaChon() = chay("Reactivando…") {
        val ds = chonBatLai.filterValues { it }.keys.sorted()
        val sl = saoLuu
        var ok = 0
        ds.forEachIndexed { i, p ->
            thongBao = "Reactivando ${i + 1}/${ds.size}: $p"
            if (io { tv.batLaiGoi(p, sl) }) ok++
        }
        dsTatTv = io { tv.goiTatBoiNguoiDung() }; chonBatLai.clear()
        batLaiBao = if (ok == ds.size) "Se reactivaron $ok apps." else "Se reactivaron $ok/${ds.size}; la TV rechazó el resto."
        zone.vgs.tvsach.Dem.gui(ctx, "khoi-phuc", tenTiviDem(), ok, "tung-app")
        if (sl != null) saoLuu = sl
    }

    fun chayKhoiPhucKhongSaoLuu(goLauncher: Boolean) = chay("Restaurando…") {
        nhatKy.clear()
        val loiKp = io { tv.khoiPhucKhongSaoLuu(goLauncher) { s -> ghiTuIo(s) } }
        veApp()
        zone.vgs.tvsach.Dem.gui(ctx, "khoi-phuc", tenTiviDem(), goiTatKhongSaoLuu?.size ?: 0, "khong-sao-luu")
        loiKp.forEach { ghi("Error: $it") }
        if (loiKp.isEmpty()) khoiPhucXong = true
        else ghi("Restaurado en parte; vuelve a ejecutarlo para reintentar.")
        goiTatKhongSaoLuu = null
    }

    fun datLai() {
        thongTin = null; trangThai = null; muc = emptyList(); chon.clear(); ketQua = null
        dock.clear(); tenChao = ""; homeXong = false; dnsChanQc = false; dangKhoiPhuc = false; khoiPhucXong = false; anhTv = null
        laMayChieu = false; mayChieu = null; ghimMayChieu.clear()
        nhatKy.clear(); saoLuu = null; saoLuuTuTv = false; goiTatKhongSaoLuu = null; epSetupwraith = false
        dsTatTv = null; chonBatLai.clear(); batLaiBao = ""
        taiLaiSaoLuu()
    }

    fun veManChinh() {
        viewModelScope.launch { runCatching { io { adb.disconnect() } } }
        datLai(); loi = ""; man = Man.CHINH
    }

    companion object {
        val UU_TIEN_DOCK = listOf(
            "com.google.android.youtube.tv", "com.netflix.ninja", "com.fptplay.tv", "net.fptplay.ottbox",
            "com.vieon.tv", "vn.vieon.tv", "com.viettel.tv360", "com.vnpt.mytv", "com.galaxyplay.tv",
            "com.amazon.amazonvideo.livingroom", "com.disney.disneyplus", "com.spotify.tv.android",
            "com.android.vending",
        )
    }
}
