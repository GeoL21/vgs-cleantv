package zone.vgs.tvsach.ui

import zone.vgs.tvsach.dulieu.KeHoach
import zone.vgs.tvsach.dulieu.XaPhuong
import androidx.compose.foundation.layout.Row
import zone.vgs.tvsach.AnhTivi
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import zone.vgs.tvsach.BuildConfig
import zone.vgs.tvsach.NgonNgu
import zone.vgs.tvsach.R
import zone.vgs.tvsach.dulieu.LoaiMuc
import zone.vgs.tvsach.dulieu.MucGoi
import zone.vgs.tvsach.dulieu.THEMES
import zone.vgs.tvsach.dulieu.HUONG_DAN
import zone.vgs.tvsach.dulieu.THANH_PHO
import zone.vgs.tvsach.dulieu.Theme
import zone.vgs.tvsach.dulieu.tenDep
import zone.vgs.tvsach.t
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------- Colores TuneTV: estilo plano y claro, azul como color principal ----------------

private val Tim = Color(0xFF2563EB)      // azul principal
private val Hong = Color(0xFF1D4ED8)     // azul más oscuro para íconos y acentos
private val Xanh = Color(0xFF16A34A)     // verde: listo
private val Cam = Color(0xFFB45309)      // ámbar: avisos
private val Nen = Color(0xFFF8FAFC)      // fondo
private val The1 = Color(0xFFFFFFFF)     // tarjetas
private val The2 = Color(0xFFF1F5FD)     // superficies suaves
private val ChuPhu = Color(0xFF475569)   // texto secundario
private val Vien = Color(0xFFE2E8F0)     // bordes
private val Chu = Color(0xFF0F172A)      // texto principal
private val NenCanhBao = Color(0xFFFEF3C7)
private val ChuCanhBao = Color(0xFF78350F)
private val NenLoi = Color(0xFFFEE2E2)
private val Gradient = Brush.linearGradient(listOf(Tim, Tim))
private val NenXam = Brush.linearGradient(listOf(The2, The2))

private val OpenSans = FontFamily(
    Font(R.font.open_sans_400, FontWeight.Normal), Font(R.font.open_sans_600, FontWeight.Medium),
    Font(R.font.open_sans_600, FontWeight.SemiBold), Font(R.font.open_sans_700, FontWeight.Bold),
    Font(R.font.open_sans_800, FontWeight.ExtraBold), Font(R.font.open_sans_800, FontWeight.Black),
)
private val Poppins = FontFamily(
    Font(R.font.poppins_500, FontWeight.Medium), Font(R.font.poppins_600, FontWeight.SemiBold),
    Font(R.font.poppins_700, FontWeight.Bold), Font(R.font.poppins_700, FontWeight.Black),
)
private val Chu_ = androidx.compose.material3.Typography().let { t ->
    fun androidx.compose.ui.text.TextStyle.os() = copy(fontFamily = OpenSans)
    fun androidx.compose.ui.text.TextStyle.pp() = copy(fontFamily = Poppins)
    t.copy(
        displayLarge = t.displayLarge.pp(), displayMedium = t.displayMedium.pp(), displaySmall = t.displaySmall.pp(),
        headlineLarge = t.headlineLarge.pp(), headlineMedium = t.headlineMedium.pp(), headlineSmall = t.headlineSmall.pp(),
        titleLarge = t.titleLarge.pp(), titleMedium = t.titleMedium.os(), titleSmall = t.titleSmall.os(),
        bodyLarge = t.bodyLarge.os(), bodyMedium = t.bodyMedium.os(), bodySmall = t.bodySmall.os(),
        labelLarge = t.labelLarge.os(), labelMedium = t.labelMedium.os(), labelSmall = t.labelSmall.os(),
    )
}

private val BangMau = lightColorScheme(
    primary = Tim, onPrimary = Color.White, secondary = Hong, background = Nen, onBackground = Chu, surface = The1,
    onSurface = Chu, surfaceVariant = The2, onSurfaceVariant = ChuPhu, outline = Vien,
    surfaceContainerHigh = The1, surfaceContainerHighest = The2,
)

// ---------------- Khung ----------------

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UngDung(vm: PhienLamViec = viewModel()) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { NgonNgu.nap(ctx) }
    MaterialTheme(colorScheme = BangMau, typography = Chu_) {
        // Chế độ tivi: đi bằng remote nên ô đang chọn phải thấy rõ — lớp sáng khi focus đậm hơn hẳn mặc định.
        val gon = if (zone.vgs.tvsach.ManHinh.laTv)
            androidx.compose.material3.RippleConfiguration(Tim, androidx.compose.material.ripple.RippleAlpha(0.25f, 0.3f, 0.25f, 0.2f))
        else androidx.compose.material3.LocalRippleConfiguration.current
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalRippleConfiguration provides gon) {
        Surface(Modifier.fillMaxSize(), color = Nen) {
            BackHandler(enabled = vm.man != Man.CHINH && !vm.dangBan) { quayLai(vm) }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.fillMaxHeight().systemBarsPadding().then(
                if (zone.vgs.tvsach.ManHinh.laTv) Modifier.widthIn(max = 820.dp).padding(top = 12.dp) else Modifier.fillMaxWidth())) {
                ThanhTren(vm)
                buoc(vm.man)?.let { ThanhBuoc(it) }
                Box(Modifier.weight(1f)) {
                    when (vm.man) {
                        Man.CHINH -> ManChinh(vm)
                        Man.KET_NOI -> ManKetNoi(vm)
                        Man.THONG_TIN -> ManThongTin(vm)
                        Man.CHON_GOI -> ManChonGoi(vm)
                        Man.DANG_TAT -> ManDangTat(vm)
                        Man.HOME_SACH -> ManHomeSach(vm)
                        Man.XONG -> ManXong(vm)
                        Man.KHOI_PHUC -> ManKhoiPhuc(vm)
                        Man.HUONG_DAN -> ManHuongDan(vm)
                        Man.GIAY_PHEP -> ManGiayPhep()
                        Man.KHOA_TRE -> ManKhoaTre(vm)
                    }
                }
                if (vm.loi.isNotEmpty() || (vm.dangBan && vm.man != Man.DANG_TAT)) ThanhTrangThai(vm)
            }
            }
            vm.banMoi?.takeIf { vm.hoiCapNhat }?.let { b ->
                AlertDialog(
                    onDismissRequest = { vm.hoiCapNhat = false },
                    icon = { Icon(Icons.Rounded.SystemUpdate, null, tint = Hong) },
                    title = { Text("Ya salió TuneTV ${b.versionName}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (b.ghiChu.isNotBlank()) Text(b.ghiChu)
                            Phu("Tienes la versión ${BuildConfig.VERSION_NAME}. Actualizar toma un minuto; tus TV limpias se quedan como están.")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { vm.hoiCapNhat = false; vm.capNhatApp() }) { Text("Actualizar ahora", fontWeight = FontWeight.SemiBold) }
                    },
                    dismissButton = { TextButton(onClick = { vm.hoiCapNhat = false }) { Text("Más tarde") } },
                    containerColor = The1,
                )
            }
            vm.anhTv?.let { anh ->
                val ctxAnh = LocalContext.current
                AlertDialog(
                    onDismissRequest = { vm.anhTv = null },
                    confirmButton = {
                        Row {
                            if (AnhTivi.luuDuoc) TextButton(onClick = {
                                val ok = AnhTivi.luu(ctxAnh, anh)
                                Toast.makeText(ctxAnh, if (ok) "Guardada en Imágenes › TuneTV" else "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show()
                            }) { Text("Guardar", fontWeight = FontWeight.SemiBold) }
                            TextButton(onClick = { AnhTivi.chiaSe(ctxAnh, anh) }) { Text("Compartir") }
                            TextButton(onClick = { vm.anhTv = null }) { Text("Cerrar") }
                        }
                    },
                    title = { Text("Pantalla de la TV") },
                    text = { Image(anh.asImageBitmap(), null, Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))) },
                    containerColor = The1,
                )
            }
        }
    }
    }
}

private fun buoc(m: Man): Int? = when (m) {
    Man.KET_NOI -> 0
    Man.THONG_TIN, Man.CHON_GOI -> 1
    Man.DANG_TAT -> 2
    Man.HOME_SACH, Man.XONG -> 3
    else -> null
}

private fun quayLai(vm: PhienLamViec) {
    vm.loi = ""
    vm.man = when (vm.man) {
        Man.KET_NOI -> { vm.veManChinh(); Man.CHINH }
        Man.THONG_TIN, Man.KHOI_PHUC -> Man.KET_NOI
        Man.CHON_GOI -> Man.THONG_TIN
        Man.DANG_TAT -> Man.CHON_GOI
        Man.HOME_SACH -> if (vm.moNangCao) { vm.moNangCao = false; Man.HOME_SACH } else vm.manTruocHome
        Man.XONG -> Man.HOME_SACH
        Man.HUONG_DAN, Man.GIAY_PHEP -> vm.manTruoc
        Man.KHOA_TRE -> Man.THONG_TIN
        Man.CHINH -> Man.CHINH
    }
}

@Composable
private fun ThanhTren(vm: PhienLamViec) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (vm.man != Man.CHINH) {
            IconButton(onClick = { if (!vm.dangBan) quayLai(vm) }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás") }
        } else Spacer(Modifier.width(12.dp))
        Image(painterResource(R.drawable.logo_cleantv), "TuneTV", Modifier.size(32.dp))
        Spacer(Modifier.width(8.dp))
        Text("Tune", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, fontFamily = Poppins)
        Text("TV", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Tim, fontFamily = Poppins)
        Text(" by GeoL", fontSize = 13.sp, color = ChuPhu, modifier = Modifier.padding(top = 3.dp))
        Spacer(Modifier.weight(1f))
        val ten = vm.thongTin?.tenHienThi
        if (vm.man != Man.CHINH && ten != null) {
            Text(ten, fontSize = 12.sp, color = ChuPhu, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp))
            Spacer(Modifier.width(12.dp))
        }
    }
}

/** Thanh 4 bước: Kết nối · Chọn · Dọn · Home. */
@Composable
private fun ThanhBuoc(dangO: Int) {
    val ten = listOf("Conectar", "Elegir", "Limpiar", "Inicio")
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ten.indices.forEach { i ->
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= dangO) Tim else Vien))
            }
        }
        Text("Paso ${dangO + 1} de 4 · ${ten[dangO]}", fontSize = 12.sp, color = ChuPhu, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ThanhTrangThai(vm: PhienLamViec) {
    val laLoi = vm.loi.isNotEmpty()
    Row(Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(14.dp))
        .background(if (laLoi) NenLoi else The2).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (laLoi) Icon(Icons.Rounded.ErrorOutline, null, tint = Color(0xFFB91C1C))
        else CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(if (laLoi) vm.loi else vm.thongBao, Modifier.weight(1f), fontSize = 14.sp)
        if (laLoi && vm.man == Man.KET_NOI) TextButton(onClick = { vm.loi = ""; vm.moHuongDan() }) { Text("Guía") }
        if (laLoi) TextButton(onClick = { vm.loi = "" }) { Text("Ocultar") }
    }
}

// ---------------- Mảnh dùng chung ----------------

@Composable
private fun Cuon(noiDung: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = noiDung)
}

@Composable
private fun The(tieuDe: String? = null, icon: ImageVector? = null, noiDung: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(The1).border(1.dp, Vien, RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (tieuDe != null) Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(The2), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(18.dp), tint = Tim)
                }
                Spacer(Modifier.width(10.dp))
            }
            Text(tieuDe, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontFamily = Poppins)
        }
        noiDung()
    }
}

@Composable
private fun NutChinh(chu: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    val mo = if (enabled) 1f else 0.4f
    Box(modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp)).background(Tim.copy(alpha = mo))
        .clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = Color.White); Spacer(Modifier.width(8.dp)) }
            Text(chu, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
    }
}

@Composable
private fun NutPhu(chu: String, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Vien),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = The1, contentColor = Chu)) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = Tim); Spacer(Modifier.width(8.dp)) }
        Text(chu, color = if (enabled) Chu else ChuPhu, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Phu(chu: String, mau: Color = ChuPhu) = Text(chu, fontSize = 13.sp, color = mau, lineHeight = 19.sp)

@Composable
private fun OIcon(icon: ImageVector, size: Dp = 44.dp, nen: Brush = Gradient) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size / 3.2f)).background(nen), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (nen === NenXam) Tim else Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun Chip(chu: String, mau: Color = ChuPhu) {
    Text(chu, fontSize = 12.sp, color = mau, modifier = Modifier.clip(RoundedCornerShape(50)).background(The2).padding(horizontal = 10.dp, vertical = 4.dp))
}

/** Mục gập/mở. */
@Composable
private fun Gap(tieuDe: String, icon: ImageVector, moSan: Boolean = false, noiDung: @Composable ColumnScope.() -> Unit) {
    var mo by rememberSaveable(icon.name) { mutableStateOf(moSan) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(The1).border(1.dp, Vien, RoundedCornerShape(16.dp))) {
        Row(Modifier.fillMaxWidth().clickable { mo = !mo }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = Tim); Spacer(Modifier.width(10.dp))
            Text(tieuDe, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(if (mo) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = ChuPhu)
        }
        AnimatedVisibility(mo) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = noiDung)
        }
    }
}

@Composable
private fun BuocSo(so: Int, chu: String) {
    Row {
        Box(Modifier.size(24.dp).clip(CircleShape).background(Tim), contentAlignment = Alignment.Center) {
            Text("$so", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(chu, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}

/** Màn kết quả lớn ở giữa: icon + tiêu đề + dòng phụ. */
@Composable
private fun DauTrang(icon: @Composable () -> Unit, tieuDe: String, phu: String?) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        icon()
        Text(tieuDe, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, textAlign = TextAlign.Center, fontFamily = Poppins)
        if (phu != null) Phu(phu)
    }
}

/** Mở hộp góp ý chung của vgs.zone (mục App CleanTV tự chọn theo chữ "CleanTV"), kèm bản app + model tivi. */
@Composable
private fun NutGopY(vm: PhienLamViec) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    NutPhu("Comentarios / reportar un error", Icons.Rounded.Feedback) {
        val tu = "TuneTV ${BuildConfig.VERSION_NAME}" + (vm.thongTin?.let { " · ${it.tenHienThi} (${it.device}, Android ${it.android})" } ?: "")
        // TuneTV: los comentarios van a los issues del repositorio de TuneTV.
        uri.openUri("https://github.com/GeoL21/tunetv/issues/new?title=" + java.net.URLEncoder.encode(tu, "UTF-8"))
    }
}

private fun ngay(ms: Long) = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ms))

// ---------------- Màn chính ----------------

@Composable
private fun ManChinh(vm: PhienLamViec) = Cuon {
    if (vm.daTuTatAdb) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tim.copy(alpha = 0.12f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Lock, null, tint = Hong); Spacer(Modifier.width(10.dp))
        Text("La depuración ADB se apagó en la TV cuando saliste de la app. La próxima vez, vuelve a activar la depuración por USB en la TV.", fontSize = 13.sp)
    }
    vm.banMoi?.let { b ->
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tim.copy(alpha = 0.18f)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = Hong); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Actualización ${b.versionName} disponible", fontWeight = FontWeight.SemiBold)
                if (b.ghiChu.isNotBlank()) Phu(b.ghiChu)
            }
            TextButton(onClick = { vm.capNhatApp() }, enabled = !vm.dangCapNhat) {
                Text(if (vm.dangCapNhat) "Descargando…" else "Actualizar")
            }
        }
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(The1).border(1.dp, Vien, RoundedCornerShape(20.dp)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tu TV sin publicidad, en 4 pasos", fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 33.sp, fontFamily = Poppins)
        Phu("Desactivamos lo que no usas. Nada se borra y todo se puede deshacer con Restaurar.")
        if (vm.dsSaoLuu.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("${vm.dsSaoLuu.size}" to "TV limpias", "${vm.dsSaoLuu.sumOf { it.daTatBoiApp.size }}" to "apps desactivadas").forEach { (so, chu) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(The2).padding(12.dp)) {
                    Text(so, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Hong, fontFamily = Poppins)
                    Text(chu, fontSize = 12.sp, color = ChuPhu)
                }
            }
        }
        NutChinh("Limpiar mi TV", icon = Icons.Rounded.AutoAwesome) { vm.datLai(); vm.man = Man.KET_NOI }
        NutPhu("Cómo preparar la TV", Icons.Rounded.PhotoLibrary) { vm.moHuongDan() }
    }
    TheNhacNpt(vm)
    if (vm.dsSaoLuu.isNotEmpty()) {
        Text("TV limpias", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        vm.dsSaoLuu.forEach { b ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(The1).border(1.dp, Vien, RoundedCornerShape(18.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OIcon(Icons.Rounded.Tv, 42.dp, NenXam)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(b.ten, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Phu("${b.ip} · ${ngay(b.luc)}")
                    Phu("${b.daTatBoiApp.size} desactivadas" + if (b.launcherDaCai) " · Inicio limpio" else "")
                }
                TextButton(onClick = { vm.moKhoiPhuc(b) }) {
                    Icon(Icons.Rounded.Restore, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Restaurar")
                }
            }
        }
    }
    NutGopY(vm)
    TextButton(onClick = { vm.moGiayPhep() }) { Icon(Icons.Rounded.Gavel, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Licencias de código abierto") }
    Phu("Basada en CleanYourTV (github.com/Gathin23/cleanyourtv, MIT) de Gathin y colaboradores: las 213 listas de TV y el inicio limpio vienen de ese proyecto." +
        "\nTuneTV ${BuildConfig.VERSION_NAME} · GeoL (basada en CleanTV de AlexVGS)")
}

// ---------------- Kết nối ----------------

@Composable
private fun Radar(dangQuet: Boolean) {
    val tr = rememberInfiniteTransition(label = "radar")
    val p by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "p")
    Box(Modifier.size(170.dp).drawBehind {
        val r = size.minDimension / 2
        if (dangQuet) for (k in 0..2) {
            val f = (p + k / 3f) % 1f
            drawCircle(Hong.copy(alpha = (1f - f) * 0.5f), radius = r * (0.35f + 0.65f * f), style = Stroke(2.dp.toPx()))
        }
        drawCircle(Vien, radius = r * 0.98f, style = Stroke(1.dp.toPx()))
        drawCircle(Vien, radius = r * 0.66f, style = Stroke(1.dp.toPx()))
    }, contentAlignment = Alignment.Center) {
        OIcon(Icons.Rounded.Tv, 64.dp)
    }
}

@Composable
private fun ManKetNoi(vm: PhienLamViec) = Cuon {
    if (zone.vgs.tvsach.ManHinh.laTv) { KetNoiTiviNay(vm); return@Cuon }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NenCanhBao).clickable { vm.moHuongDan() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Warning, null, tint = Cam); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("¿Está activada la depuración por USB?", fontWeight = FontWeight.SemiBold)
            Phu("Sin ella la app no se puede conectar. Toca para ver la guía con imágenes.", ChuCanhBao)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Cam)
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Radar(vm.dangQuet || vm.dangBan)
        Text(
            when {
                vm.dangKhoiPhuc -> "Vuelve a conectar la TV para restaurar"
                vm.dangQuet -> "Buscando TV…"
                else -> "Buscar la TV"
            }, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
        Phu("Teléfono y TV en el mismo Wi‑Fi, con la depuración por USB activada.")
    }
    NutPhu(if (vm.dangQuet) "Buscando…" else "Buscar en la red", Icons.Rounded.WifiFind, enabled = !vm.dangQuet) { vm.quetMang() }
    vm.timThay.forEach { dc ->
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(The1).border(1.dp, Vien, RoundedCornerShape(18.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OIcon(Icons.Rounded.Tv, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(dc, fontWeight = FontWeight.SemiBold)
                Phu("Android TV · puerto 5555 abierto")
            }
            Button(onClick = { vm.ip = dc; vm.cong = "5555"; vm.ketNoi() }, enabled = !vm.dangBan, shape = RoundedCornerShape(12.dp)) {
                Text("Conectar")
            }
        }
    }
    Gap("Escribir la IP a mano", Icons.Rounded.Keyboard, moSan = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(vm.ip, { vm.ip = it.trim() }, label = { Text("IP de la TV") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(vm.cong, { vm.cong = it.filter(Char::isDigit) }, label = { Text("Puerto") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(100.dp), shape = RoundedCornerShape(12.dp))
        }
        NutChinh("Conectar", enabled = !vm.dangBan) { vm.ketNoi() }
        Phu("IP de la TV: Ajustes → Red → red actual. La primera vez la TV pregunta \"¿Permitir la depuración?\": marca Permitir siempre y pulsa Permitir.")
    }
    Gap("Vincular con código (Depuración inalámbrica)", Icons.Rounded.Link) {
        Phu("En la TV: Opciones para desarrolladores → Depuración inalámbrica → Vincular dispositivo con un código de vinculación.")
        OutlinedTextField(vm.ghepIpCong, { vm.ghepIpCong = it.trim() }, label = { Text("IP:puerto de vinculación") }, placeholder = { Text("192.168.1.20:37123") },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(vm.ghepMa, { vm.ghepMa = it.filter(Char::isDigit).take(6) }, label = { Text("Código de 6 dígitos") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        NutPhu("Vincular y conectar", Icons.Rounded.Link, enabled = !vm.dangBan) { vm.ghepDoi() }
    }
}

/** Chế độ tivi: app chạy ngay trên tivi, kết nối ADB vào chính nó. */
@Composable
private fun ColumnScope.KetNoiTiviNay(vm: PhienLamViec) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OIcon(Icons.Rounded.Tv, 64.dp)
        Text(if (vm.dangKhoiPhuc) "Vuelve a conectar para restaurar esta TV" else "Limpiar esta TV",
            fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center)
        Phu("TuneTV está funcionando en la misma TV, sin necesidad de teléfono.")
    }
    The("Antes de empezar", Icons.Rounded.Warning) {
        BuocSo(1, "La depuración por USB está activada (Opciones para desarrolladores).")
        BuocSo(2, "Pulsa Conectar. Cuando la TV pregunte \"¿Permitir depuración por USB?\", marca Permitir siempre y pulsa Permitir.")
        BuocSo(3, "Mientras trabaja, la pantalla puede saltar al inicio unos segundos y luego volver.")
    }
    NutChinh("Conectar esta TV", enabled = !vm.dangBan, icon = Icons.Rounded.Link) { vm.ketNoiTiviNay() }
    NutPhu("Guía con imágenes para la depuración por USB", Icons.Rounded.Warning) { vm.moHuongDan() }
    val ctxTv = androidx.compose.ui.platform.LocalContext.current
    NutPhu("Abrir Opciones para desarrolladores", Icons.Rounded.Tune) {
        runCatching { ctxTv.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { ctxTv.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
}

// ---------------- Thông tin + chọn mẫu ----------------

@Composable
private fun ManThongTin(vm: PhienLamViec) = Cuon {
    val info = vm.thongTin ?: return@Cuon
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Tim.copy(alpha = 0.3f), The1)))
        .border(1.dp, Vien, RoundedCornerShape(24.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        OIcon(Icons.Rounded.Tv, 56.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp), tint = Xanh); Spacer(Modifier.width(4.dp))
                Text("Conectada", fontSize = 12.sp, color = Xanh)
            }
            Text(info.tenHienThi, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("Android ${info.android}")
                vm.trangThai?.let { Chip("${it.caiDat.size} paquetes") }
            }
        }
    }
    if (vm.laAndroidCu && !vm.laFireTv) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NenCanhBao).padding(12.dp)) {
        Icon(Icons.Rounded.Warning, null, tint = Cam); Spacer(Modifier.width(8.dp))
        Phu("Este equipo tiene Android ${vm.thongTin?.android}: TuneTV puede quitar anuncios, rastreo y apps basura, pero NO puede instalar el inicio limpio (necesita Android 8 o más nuevo).")
    }
    if (vm.laFireTv) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NenCanhBao).padding(12.dp)) {
        Icon(Icons.Rounded.Warning, null, tint = Cam); Spacer(Modifier.width(8.dp))
        Phu("Fire TV: TuneTV puede quitar anuncios, rastreo y apps basura, pero NO puede reemplazar el inicio. Amazon bloquea el inicio con su propio launcher; ninguna app lo puede cambiar.")
    }
    if (info.adbSecure == "0") Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NenCanhBao).padding(12.dp)) {
        Icon(Icons.Rounded.Warning, null, tint = Cam); Spacer(Modifier.width(8.dp))
        Phu("Esta TV acepta ADB sin preguntar: cualquiera en la red puede controlarla. Apaga ADB cuando termines.", ChuCanhBao)
    }
    vm.saoLuu?.let {
        Phu("Hay un respaldo del ${ngay(it.luc)}" +
            (if (vm.saoLuuTuTv) " (leído de la copia en la TV)" else "") +
            ". Si continúas, se conserva ese punto de restauración.")
    }
    The("Lista del modelo", Icons.Rounded.FactCheck) {
        if (vm.ungVien.isEmpty()) Phu("Ninguna lista probada coincide con esta TV. La app usa reglas generales: solo marca los paquetes que se desactivaron en muchas otras TV y que nadie marcó como necesarios.")
        vm.ungVien.forEach { (m, diem) ->
            LuaChon(vm.mauChon == m, m.ten.take(90), "${m.goi.size} " + "paquetes" + " · " + "coincide" + " $diem") { vm.mauChon = m }
        }
        if (vm.ganGiong.isNotEmpty()) {
            Phu("Coincidencias cercanas (misma marca, nombre de modelo parecido). Elige una si es la línea de tu TV:")
            vm.ganGiong.forEach { m -> LuaChon(vm.mauChon == m, m.ten.take(90), "${m.goi.size} " + "paquetes" + " · " + "coincidencia cercana") { vm.mauChon = m } }
        }
        // Mẫu chọn tay từ hộp "Chọn mẫu khác" (không nằm trong danh sách trên) vẫn hiện để thấy đang dùng mẫu nào.
        vm.mauChon?.takeIf { c -> vm.ungVien.none { it.first == c } && c !in vm.ganGiong }?.let { m ->
            LuaChon(true, m.ten.take(90), "${m.goi.size} " + "paquetes" + " · " + "tu elección") {}
        }
        LuaChon(vm.mauChon == null, "Sin lista, solo reglas generales", null) { vm.mauChon = null }
        NutPhu("Elegir otra lista (${vm.tatCaMau.size})", Icons.Rounded.Search) { vm.moChonMau = true }
    }
    if (vm.moChonMau) HopChonMau(vm)
    TheMayChieu(vm)
    NutChinh("Ver paquetes", enabled = !vm.dangBan, icon = Icons.AutoMirrored.Rounded.ArrowForward) { vm.lapKeHoach() }
    NutPhu("Ver la pantalla de la TV", Icons.Rounded.Screenshot) { vm.chupManHinh() }
    if (vm.daCoHomeSach) NutPhu("Cambiar el tema del inicio limpio", Icons.Rounded.Palette) { vm.moDoiTheme() }
    NutPhu("Bloqueo infantil (horario + PIN)", Icons.Rounded.Lock) { vm.moKhoaTre() }
    if (vm.saoLuu != null) NutPhu("Restaurar esta TV", Icons.Rounded.Restore) { vm.man = Man.KHOI_PHUC }
    else if (vm.coDauCleanTv) NutPhu("Restaurar (sin respaldo)", Icons.Rounded.Restore) { vm.man = Man.KHOI_PHUC }
    else NutPhu("Reactivar apps desactivadas", Icons.Rounded.Restore) { vm.man = Man.KHOI_PHUC }
}

@Composable
private fun LuaChon(chon: Boolean, chu: String, phu: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (chon) Tim.copy(alpha = 0.18f) else The2)
        .border(1.dp, if (chon) Tim else Color.Transparent, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(chu, fontSize = 14.sp)
            if (phu != null) Phu(phu)
        }
        RadioButton(chon, onClick)
    }
}

// ---------------- Chọn gói ----------------

@Composable
private fun ManChonGoi(vm: PhienLamViec) {
    val nhom = vm.muc.groupBy { it.loai }
    val so = vm.soDaChon()
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(The1).border(1.dp, Vien, RoundedCornerShape(20.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$so", fontWeight = FontWeight.SemiBold, fontSize = 40.sp, color = Tim, fontFamily = Poppins)
                        Spacer(Modifier.width(8.dp))
                        Text("para desactivar" + " / ${vm.muc.size}", color = ChuPhu, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    LinearProgressIndicator(progress = { if (vm.muc.isEmpty()) 0f else so.toFloat() / vm.muc.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Hong, trackColor = The2)
                    Phu("Los paquetes seguros ya vienen marcados. Pregunta al dueño por las apps con ícono. Los paquetes que la TV necesita (HDMI, control, teclado, Play Store) se conservan y no aparecen.")
                }
            }
            LoaiMuc.entries.forEach { loai ->
                val ds = nhom[loai].orEmpty()
                if (ds.isEmpty()) return@forEach
                item(key = "h-$loai") {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(loai.tieuDe, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, fontFamily = Poppins)
                            Phu(loai.moTa)
                        }
                        val tatCa = ds.all { vm.chon[it.pkg] == true }
                        TextButton(onClick = { ds.forEach { vm.chon[it.pkg] = !tatCa } }) {
                            Text(if (tatCa) "Ninguna" else "Todas")
                        }
                    }
                }
                items(ds, key = { it.pkg }) { m -> DongGoi(m, vm.chon[m.pkg] == true) { vm.chon[m.pkg] = it } }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(14.dp)).clickable { vm.epSetupwraith = !vm.epSetupwraith }.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(vm.epSetupwraith, { vm.epSetupwraith = it })
                    Phu("La TV está por cable de red (permitir desactivar setupwraith aunque sea dueño del Wi‑Fi)")
                }
            }
        }
        var hoi by remember { mutableStateOf(false) }
        Column(Modifier.background(Nen).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NutChinh("Respaldar y limpiar $so paquetes", enabled = !vm.dangBan && so > 0, icon = Icons.Rounded.CleaningServices) { hoi = true }
            NutXemLenh { vm.lenhDon() }
            if (!vm.khongCaiHome) NutPhu(if (vm.daCoHomeSach) "Omitir, solo cambiar el tema" else "Omitir, solo instalar el inicio limpio",
                Icons.Rounded.Home, enabled = !vm.dangBan) { vm.boQuaDon() }
        }
        val nenGiu = vm.muc.filter { vm.chon[it.pkg] == true && KeHoach.nenGiu(it) }
        if (hoi && nenGiu.isNotEmpty()) AlertDialog(
            onDismissRequest = { hoi = false }, containerColor = The1,
            icon = { Icon(Icons.Rounded.Warning, null, tint = Cam) },
            title = { Text("Espera: ${nenGiu.size} paquetes que la mayoría de TV conservan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Marcaste estas, pero muchas listas probadas dicen CONSERVARLAS (desactivarlas puede dañar la tecla Entrada, el HDMI, el control, Ajustes…):")
                    nenGiu.take(8).forEach { Text("• ${tenDep(it.pkg)} (${it.pkg}): " + "conservado en ${it.soMauGiu} listas", fontSize = 13.sp, color = ChuPhu) }
                    if (nenGiu.size > 8) Text("… y ${nenGiu.size - 8} más", fontSize = 13.sp, color = ChuPhu)
                }
            },
            confirmButton = { TextButton(onClick = { nenGiu.forEach { vm.chon[it.pkg] = false }; hoi = false; vm.batDauTat() }) { Text("Desmárcalas y limpiar", fontWeight = FontWeight.SemiBold) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { hoi = false; vm.batDauTat() }) { Text("Desactivar todas de todos modos", color = Cam) }
                    TextButton(onClick = { hoi = false }) { Text("Cancelar") }
                }
            },
        )
        else if (hoi) AlertDialog(
            onDismissRequest = { hoi = false }, containerColor = The1,
            icon = { Icon(Icons.Rounded.Shield, null, tint = Hong) },
            title = { Text("¿Limpiar $so paquetes?") },
            text = { Text("Primero la app guarda en este teléfono el estado de la TV y después desactiva cada paquete (no se desinstala nada). Puedes restaurar cuando quieras.") },
            confirmButton = { TextButton(onClick = { hoi = false; vm.batDauTat() }) { Text("Limpiar") } },
            dismissButton = { TextButton(onClick = { hoi = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DongGoi(m: MucGoi, chon: Boolean, doi: (Boolean) -> Unit) {
    val ten = tenDep(m.pkg)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(The1)
        .border(1.dp, if (chon) Tim.copy(alpha = 0.45f) else Vien, RoundedCornerShape(14.dp)).clickable { doi(!chon) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(The2), contentAlignment = Alignment.Center) {
            Text(ten.take(1).uppercase(), fontWeight = FontWeight.Bold, color = if (chon) Tim else ChuPhu)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ten, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Text(m.pkg, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChuPhu.copy(alpha = 0.8f), maxLines = 1)
            val ghi = m.ghiChu.ifBlank { m.nhom }
            if (ghi.isNotBlank()) Text(ghi, fontSize = 12.sp, color = ChuPhu, maxLines = 2, lineHeight = 16.sp)
            if (m.soMauTat > 0 || m.soMauGiu > 0) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (m.soMauTat > 0) Chip("apagado en ${m.soMauTat} modelos")
                if (m.soMauGiu > 0) Chip("⚠ conservado en ${m.soMauGiu}", Cam)
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(chon, doi)
    }
}

// ---------------- Đang dọn / kết quả ----------------

@Composable
private fun ManDangTat(vm: PhienLamViec) = Cuon {
    val kq = vm.ketQua
    if (kq == null) {
        Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                val p = vm.tienDo
                if (p <= 0f) CircularProgressIndicator(Modifier.fillMaxSize(), strokeWidth = 10.dp, color = Hong, trackColor = The2)
                else CircularProgressIndicator(progress = { p }, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp, color = Hong, trackColor = The2)
                Text(if (p <= 0f) "…" else "${(p * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 34.sp)
            }
            Text("Limpiando la TV", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(vm.thongBao, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ChuPhu, textAlign = TextAlign.Center, maxLines = 2)
            Phu("No apagues la TV ni cambies de Wi‑Fi ahora.")
        }
    } else {
        DauTrang({
            Box(Modifier.size(84.dp).clip(CircleShape).background(Xanh.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(52.dp), tint = Xanh)
            }
        }, "Se limpiaron ${kq.daTat.size} paquetes", null)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Chip("${kq.boQua.size} conservados", Cam)
            Spacer(Modifier.width(8.dp))
            Chip("${kq.loi.size} fallaron", if (kq.loi.isEmpty()) ChuPhu else Hong)
        }
        if (kq.boQua.isNotEmpty()) Gap("Conservados por seguridad (${kq.boQua.size})", Icons.Rounded.Shield, moSan = true) {
            kq.boQua.forEach { (p, ly) -> Column { Text(tenDep(p), fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Phu(ly) } }
        }
        if (kq.loi.isNotEmpty()) Gap("No se pudieron desactivar (${kq.loi.size})", Icons.Rounded.ErrorOutline) {
            kq.loi.forEach { (p, ly) -> Text("$p: $ly", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = ChuPhu) }
            Phu("Suelen ser paquetes protegidos del sistema; puedes ignorarlos.")
        }
        The("Revisa en la TV", Icons.Rounded.Tv) {
            Phu("Pulsa Inicio, abre YouTube/Netflix, cambia de HDMI, abre Ajustes, prueba el control y la voz.")
            NutPhu("Ver la pantalla de la TV", Icons.Rounded.Screenshot) { vm.chupManHinh() }
        }
        if (vm.khongCaiHome) NutChinh(if (vm.laFireTv) "Listo (Fire TV no permite cambiar el inicio)" else "Listo (sin inicio limpio en este equipo)", icon = Icons.Rounded.CheckCircle) { vm.man = Man.XONG }
        else {
            NutChinh("Siguiente: inicio limpio", icon = Icons.Rounded.Home) { vm.man = Man.HOME_SACH }
            NutPhu("Omitir inicio limpio") { vm.man = Man.XONG }
        }
        NutGopY(vm)
    }
}

// ---------------- Home sạch ----------------

/**
 * Màn Home sạch / Đổi giao diện (làm lại 23/10/2026 theo bản phác anh Alex duyệt): ảnh xem trước ở đầu, ba thẻ
 * Giao diện · Home · Tuỳ chỉnh, nút Áp dụng dính đáy; Nâng cao là màn riêng (ManNangCao).
 */
@Composable
private fun ManHomeSach(vm: PhienLamViec) {
    if (vm.moNangCao) { ManNangCao(vm); return }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            XemTruocHome(vm)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Aspecto", "Inicio", "Opciones").forEachIndexed { i, ten ->
                    val chon = vm.theHome == i
                    Text(ten, fontSize = 14.sp, fontWeight = if (chon) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (chon) Tim.copy(alpha = 0.3f) else The2)
                            .border(1.dp, if (chon) Tim else Vien, RoundedCornerShape(50)).clickable { vm.theHome = i }.padding(vertical = 10.dp))
                }
            }
            when (vm.theHome) {
                0 -> TheGiaoDien(vm)
                1 -> TheHomeNoiDung(vm)
                else -> TheTuyChinh(vm)
            }
            vm.nhatKy.takeLast(4).forEach { Text(it, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = ChuPhu) }
        }
        // Nút dính đáy: đổi thẻ nào cũng bấm được, không phải cuộn hết trang.
        Column(Modifier.fillMaxWidth().background(The1).padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (vm.daCoHomeSach) NutChinh(if (vm.dangBan) "Aplicando…" else "Aplicar a la TV",
                enabled = !vm.dangBan, icon = Icons.Rounded.Palette) { vm.apDungTheme() }
            else NutChinh(if (vm.dangBan) "Instalando…" else "Instalar inicio limpio", enabled = !vm.dangBan, icon = Icons.Rounded.Home) { vm.caiHomeSach() }
            NutXemLenh { vm.lenhHome() }
        }
    }
}

/** Ảnh xem trước Home: ảnh riêng / ảnh theme / dải màu, đồng hồ, dòng âm lịch + thời tiết, dock giả. */
@Composable
private fun XemTruocHome(vm: PhienLamViec) {
    val ctx = LocalContext.current
    val th = vm.themeChon
    val anhRieng = vm.anhNenRieng
    val anh = remember(th.id, anhRieng) {
        runCatching {
            if (anhRieng != null) android.graphics.BitmapFactory.decodeByteArray(anhRieng, 0, anhRieng.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 })
            else (th.anhNen ?: th.anhXem)?.let { a -> ctx.assets.open(a).use { android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }) } }
        }.getOrNull()?.asImageBitmap()
    }
    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(th.mau.map { Color(it) })).border(1.dp, Vien, RoundedCornerShape(16.dp))) {
        if (anh != null) Image(anh, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        if (vm.lamToiNen > 0) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = vm.lamToiNen / 100f)))
        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            if (vm.tenChao.isNotBlank()) Text("HOLA, " + vm.tenChao.uppercase(), fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
            Text(java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date()), fontSize = 26.sp, fontWeight = FontWeight.Light, color = Color.White)
            val dong = listOfNotNull(if (vm.amLich) "fecha lunar" else null, vm.thanhPho?.ten).joinToString(" · ")
            if (dong.isNotEmpty()) Text(dong, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
        }
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(5) { i -> Box(Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(5.dp)).background(if (i == 0) Color.White else Color.White.copy(alpha = if (i < vm.dock.size) 0.6f else 0.25f))) }
        }
    }
    Phu(t(th.tenVi, th.tenEn) + " · " + t(th.moTaVi, th.moTaEn) + if (anhRieng != null) " · tu foto" else "")
}

@Composable
private fun TheGiaoDien(vm: PhienLamViec) {
    The {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            THEMES.forEach { th -> TheTheme(th, th == vm.themeChon) { vm.themeChon = th } }
        }
        // Ảnh của chính mình làm hình nền (góp ý 23/10/2026): thay ảnh / bộ ảnh của theme, giữ màu và hiệu ứng theme.
        val chonAnh = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) vm.chonAnhNen(uri)
        }
        // Chạy CleanTV ngay trên tivi: thường không có trình chọn ảnh (TCL báo "không có ứng dụng phù hợp", 24/10/2026)
        // → chọn ảnh có sẵn trên tivi / USB qua shell.
        val trenTv = zone.vgs.tvsach.ManHinh.laTv || vm.ip.trim() == "127.0.0.1"
        if (vm.anhNenRieng == null) {
            if (!trenTv) NutPhu("Usa tu propia foto como fondo", Icons.Rounded.Image) {
                runCatching { chonAnh.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    .onFailure { vm.moAnhTrenTv() }
            }
            NutPhu("Elige una foto de la TV / USB", Icons.Rounded.Image) { vm.moAnhTrenTv() }
        }
        vm.anhTrenTv?.let { ds ->
            AlertDialog(
                onDismissRequest = { vm.anhTrenTv = null }, containerColor = The1,
                title = { Text("Fotos en la TV / USB") },
                text = {
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        if (ds.isEmpty()) Phu("No hay fotos en Descargas, Imágenes, DCIM ni en USB. Copia un JPG a una memoria USB y conéctala a la TV, o descarga uno en la TV.")
                        ds.forEach { f ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { vm.chonAnhTrenTv(f) }.padding(vertical = 8.dp, horizontal = 4.dp)) {
                                Text(f.substringAfterLast('/'), fontSize = 14.sp, maxLines = 1)
                                Text(f.substringBeforeLast('/'), fontSize = 11.sp, color = ChuPhu, maxLines = 1)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { vm.anhTrenTv = null }) { Text("Cerrar") } },
            )
        }
        if (vm.anhNenRieng == null) Unit
        else NutPhu("Quitar tu foto (${vm.anhNenRieng!!.size / 1024} KB)", Icons.Rounded.Close) { vm.anhNenRieng = null }
        Text("Oscurecer el fondo", fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 20, 40).forEach { p -> ChonChip(if (p == 0) "Ninguna" else "$p%", vm.lamToiNen == p) { vm.lamToiNen = p } }
        }
    }
    if (vm.dsNangCao.isNotEmpty()) {
        val daDat = vm.nangCao.count { it.value.isNotBlank() }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(The1).border(1.dp, Vien, RoundedCornerShape(20.dp))
            .clickable { vm.moNangCao = true }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Tune, null, Modifier.size(20.dp), tint = Hong)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Avanzado · ${vm.dsNangCao.size} opciones", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Phu(if (daDat > 0) "$daDat cambiados" else "Para curiosos: efectos, cuadrícula, control…")
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = ChuPhu)
        }
    }
}

@Composable
private fun TheHomeNoiDung(vm: PhienLamViec) {
    The("Dock (hasta $DOCK_TOI_DA)", Icons.Rounded.Apps) {
        // Thứ tự trên dock, dời bằng ↑ ↓ (góp ý daohoangthong1 23/10/2026: không biết cách đổi thứ tự).
        if (vm.dock.isNotEmpty()) {
            Text("Orden del dock", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            vm.dock.toList().forEachIndexed { i, p ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tim.copy(alpha = 0.15f)).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Hong, modifier = Modifier.width(22.dp))
                    Text(tenDep(p), fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = { vm.dichDock(p, -1) }, enabled = i > 0) { Icon(Icons.Rounded.KeyboardArrowUp, "Arriba") }
                    IconButton(onClick = { vm.dichDock(p, 1) }, enabled = i < vm.dock.size - 1) { Icon(Icons.Rounded.KeyboardArrowDown, "Abajo") }
                }
            }
            Text("Elegir apps del dock", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        }
        vm.appChoDock().forEach { p ->
            val viTri = vm.dock.indexOf(p)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (viTri >= 0) Tim.copy(alpha = 0.2f) else The2)
                .border(1.dp, if (viTri >= 0) Tim else Color.Transparent, RoundedCornerShape(14.dp)).clickable { vm.doiDock(p) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(if (viTri >= 0) Gradient else Brush.linearGradient(listOf(Vien, Vien))),
                    contentAlignment = Alignment.Center) {
                    Text(if (viTri >= 0) "${viTri + 1}" else "", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tenDep(p), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(p, fontSize = 11.sp, color = ChuPhu, fontFamily = FontFamily.Monospace, maxLines = 1)
                }
            }
        }
    }
    The("Canales VTV en el inicio", Icons.Rounded.LiveTv) {
        if (vm.coVtvGo) {
            Phu("Elige canales: cada uno se vuelve un mosaico al inicio del dock que abre el canal directo en VTVgo.")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KENH_VTV.forEach { (ten, _, _) ->
                    ChonChip(ten, ten in vm.kenhVtv) {
                        if (ten in vm.kenhVtv) vm.kenhVtv.remove(ten)
                        else { vm.kenhVtv.add(ten); vm.kenhVtv.sortBy { k -> KENH_VTV.indexOfFirst { it.first == k } } }
                    }
                }
            }
        } else Phu("Necesita la app VTVgo en la TV: instálala desde Google Play en la TV, ábrela una vez y vuelve a conectar TuneTV.")
    }
    The("Clima", Icons.Rounded.WbSunny) {
        OutlinedTextField(vm.timXa, { vm.doiTimXa(it) }, label = { Text("Busca tu barrio o ciudad") },
            placeholder = { Text("p. ej. Chapinero, Bocagrande, Medellín") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        vm.ketQuaXa.forEach { x ->
            Text("${x.ten} · ${x.tinh}", fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .clickable { vm.chonXa(x) }.padding(horizontal = 12.dp, vertical = 10.dp))
        }
        Phu("O elige una ciudad:")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChonChip("Apagado", vm.thanhPho == null) { vm.thanhPho = null }
            vm.thanhPho?.takeIf { it !in THANH_PHO }?.let { tp -> ChonChip(tp.ten, true) { } }
            THANH_PHO.forEach { tp -> ChonChip(tp.ten, vm.thanhPho == tp) { vm.thanhPho = tp } }
        }
        Phu("Datos del clima: Open-Meteo.com (CC BY 4.0), la TV los actualiza cada 30 minutos. Ubicaciones © OpenStreetMap.")
    }
    The("Reloj y saludo", Icons.Rounded.Schedule) {
        OutlinedTextField(vm.tenChao, { vm.tenChao = it }, label = { Text("Nombre para el saludo (p. ej. familia Pérez)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Hang("Ocultar el saludo", "Quita la línea \"Buenas noches\" sobre el reloj", vm.anLoiChao) { vm.anLoiChao = it }
        Hang("Fecha lunar bajo la fecha", "p. ej. Lunar 2/9 · Año del Caballo", vm.amLich) { vm.amLich = it }
        Text("Reloj cuando no se usa", fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 5, 15, 30).forEach { p -> ChonChip(if (p == 0) "Apagado" else "$p min", vm.dongHoKhiYen == p) { vm.dongHoKhiYen = p } }
        }
    }
    TheMayChieu(vm)
}

@Composable
private fun TheTuyChinh(vm: PhienLamViec) {
    The("Pantalla", Icons.Rounded.Visibility) {
        Hang("Nombre grande de la app sobre el dock", "Apágalo para un inicio más tranquilo, solo el brillo", vm.tenToTrenDock) { vm.tenToTrenDock = it }
        Hang("Nombres bajo cada app", "Apps más fáciles de reconocer", vm.tenDuoiApp) { vm.tenDuoiApp = it }
        Hang("Mosaicos de color para apps de teléfono", "Las apps sin banner de TV tienen un mosaico del color de su ícono", vm.toMauOApp) { vm.toMauOApp = it }
        Hang("Todas las apps en una fila", "Una fila que se desliza de lado, sin cuadrícula debajo", vm.motHang) { vm.motHang = it }
        Hang("Mosaicos de ícono uniformes", "Estilo Apple TV: cada mosaico es el ícono sobre su propio color, sin banners", vm.oDeuNhau) { vm.oDeuNhau = it }
        Hang("Botones compactos con ícono", "Un grupo ordenado, sin nombres", vm.gonNut) { vm.gonNut = it }
        Text("Tamaño del reloj, la fecha y el clima", fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100 to "Normal", 125 to "Grande", 150 to "Extra grande").forEach { (v, chu) ->
                ChonChip(chu, vm.coChuDau == v) { vm.coChuDau = v }
            }
        }
        Text("Apps por fila", fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Del tema", 4 to "4", 5 to "5", 6 to "6", 7 to "7").forEach { (v, chu) ->
                ChonChip(chu, vm.soCot == v) { vm.soCot = v }
            }
        }
        Text("Posición de las filas de apps", fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(HANG_GIUA to "En medio", -120 to "Mucho más arriba", -60 to "Más arriba", 0 to "Predeterminado", 60 to "Más abajo", 300 to "Lo más abajo").forEach { (v, chu) ->
                ChonChip(chu, vm.doiHang == v) { vm.doiHang = v }
            }
        }
        Hang("Abrir una app al encender", "Actívalo, aplícalo a la TV y luego usa el botón de abajo para elegir la app en la TV", vm.moKhiBat) { vm.moKhiBat = it }
        if (vm.moKhiBat) NutPhu("Abrir el selector de apps en la TV", Icons.Rounded.Tv) { vm.moChonAppKhiBat() }
        if (vm.moKhiBat && vm.baoChonApp.isNotEmpty()) Phu(vm.baoChonApp, Xanh)
        Hang("Botón de lo que se está reproduciendo", "Aparece mientras suenan apps: pausa cada una o detenlas todas", vm.nutDangPhat) { vm.nutDangPhat = it }
        Hang("Botón de protector de pantalla", "Abre el protector de pantalla de fotos / reloj, útil con música", vm.nutManCho) { vm.nutManCho = it }
        Hang("Biblioteca de apps", "Un botón con todas las apps; agrega o quita del inicio", vm.thuVienApp) { vm.thuVienApp = it }
        Hang("Mostrar CPU / memoria", "Una línea pequeña bajo el reloj", vm.hienThongSo) { vm.hienThongSo = it }
    }
    The("Control", Icons.Rounded.SettingsRemote) {
        Hang("El botón Entrada del control abre el selector de entradas", "Recomendado en Google TV", vm.nutInput) { vm.nutInput = it }
        Hang("El control envía una tecla Inicio rara", "Palitos Xiaomi, decodificadores de operador…", vm.nutHome) { vm.nutHome = it }
        if (vm.phimRemote() != null) Hang("Botones de apps del control",
            if (vm.mauChon?.id == "tcl-g09-google-tv" || vm.laTclGoogleTv) "YouTube, Netflix, Ajustes… funcionan con el inicio de fábrica apagado. En esta TV, mantener el volumen sube solo un paso"
            else "YouTube, Netflix, Ajustes… siguen funcionando con el inicio de fábrica apagado",
            vm.nutAppRemote) { vm.nutAppRemoteChon = it }
        Hang("Mantén el volumen para seguir subiendo / bajando",
            "Actívalo si al mantener el volumen solo sube un paso (común en Sony). A cambio, los botones Entrada / Apps / de apps del control funcionan como los de la TV",
            vm.giuAmLuong) { vm.giuAmLuong = it }
    }
    The("Archivo de ajustes", Icons.Rounded.Save) {
        Phu("Guarda todas tus elecciones (tema, dock, clima, opciones, Avanzado, tu fondo) en un archivo para conservarlas o usarlas en otra TV.")
        val luu = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) vm.xuatCauHinh(uri) }
        val mo = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.nhapCauHinh(uri) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { NutPhu("Guardar en archivo", Icons.Rounded.Save) { luu.launch("tunetv-" + (vm.thongTin?.model ?: "tivi").replace(Regex("[^A-Za-z0-9]+"), "-") + ".json") } }
            Box(Modifier.weight(1f)) { NutPhu("Abrir archivo", Icons.Rounded.FolderOpen) { mo.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) } }
        }
        if (vm.cauHinhBao.isNotEmpty()) Phu(vm.cauHinhBao, Xanh)
    }
    The("TV lentas y red", Icons.Rounded.Speed) {
        Hang("Modo TV lenta", "Movimiento más suave para TV viejas con poca memoria", vm.tiviYeu) { vm.tiviYeu = it }
        Hang("Efectos de vidrio esmerilado", "Dock esmerilado, brillo del color de la app", vm.hieuUngKinh) { vm.hieuUngKinh = it }
        Hang("DNS que bloquea anuncios (AdGuard)", "Un ajuste de DNS de la TV, no una app (Ajustes → Red → DNS privado). No bloquea los anuncios de YouTube. Si tu router ya filtra (Pi-hole, AdGuard Home, VPN en el router), déjalo apagado: la TV se saltaría el router", vm.dnsChanQc) { vm.dnsChanQc = it }
    }
}

/** Nhóm của một khoá Nâng cao, theo thứ tự hiện. */
private val NHOM_NANG_CAO = listOf(
    Triple("hinh-nen", "Hình nền & màu" to "Fondo y color", Regex("^(background_color|sky_intensity|wallpaper_.*|accent_color|festival.*|glow_style|ambient_glow)$")),
    Triple("dock", "Dock & lưới app" to "Dock y cuadrícula de apps", Regex("^(fill_dock|hidden_apps|columns|rows_below_dock|dock_.*|library_apps|app_library_selection|app_labels|app_banners|module_cards|reorder_button|tile_menu|fade_rows|focus_overshoot)$")),
    Triple("thanh-tren", "Thanh trên & lời chào" to "Barra superior y saludo", Regex("^(show_.*|icon_only_.*|top_menu|launcher_settings_button|wifi_.*|greeting|shamsi_date|vpn_app)$")),
    Triple("dien-anh", "Home điện ảnh" to "Inicio de cine", Regex("^cinematic_.*$")),
    Triple("remote", "Remote, HDMI & app hãng" to "Control, HDMI y apps del fabricante", Regex("^(live_tv_.*|source_panel_.*|coocaa_sources|launch_actions|input_.*|settings_key_.*|restore_key_services|app_keys|key_filter|stock_home_redirect|boot_to_live_tv)$")),
)

/** Màn Nâng cao riêng: tìm, lọc Đã đổi / Cẩn thận, chia nhóm, chấm tím ở mục đã đổi. */
@Composable
private fun ManNangCao(vm: PhienLamViec) {
    var tim by remember { mutableStateOf("") }
    var loc by remember { mutableStateOf(0) } // 0 tất cả, 1 đã đổi, 2 cẩn thận
    val daDat = vm.nangCao.count { it.value.isNotBlank() }
    val q = XaPhuong.boDau(tim.trim())
    val ds = vm.dsNangCao.filter { tc ->
        (q.isEmpty() || tc.khoa.contains(q) || XaPhuong.boDau(tc.ten).contains(q) || XaPhuong.boDau(tc.moTaVi).contains(q) || tc.moTa.lowercase().contains(q)) &&
            when (loc) { 1 -> !vm.nangCao[tc.khoa].isNullOrBlank(); 2 -> tc.canThan; else -> true }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Avanzado", fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.weight(1f))
                if (daDat > 0) TextButton(onClick = { vm.nangCao.clear() }) { Text("Restablecer todo") }
            }
            Phu("\"Predeterminado\" deja que el inicio limpio decida. Lo marcado con \"Cuidado\" toca el control, el HDMI y las apps del fabricante: un valor equivocado puede dañarlos; Restaurar deja todo como estaba.")
            OutlinedTextField(tim, { tim = it }, label = { Text("Buscar: nombre de app, reloj, fondo…") }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChonChip("Todas", loc == 0) { loc = 0 }
                ChonChip("Cambiados ($daDat)", loc == 1) { loc = 1 }
                ChonChip("Cuidado", loc == 2) { loc = 2 }
            }
            val conLai = ds.toMutableList()
            (NHOM_NANG_CAO.map { (id, ten, re) -> id to ten to re } + listOf("khac" to ("Khác" to "Otros") to Regex(".*"))).forEach { (dau, re) ->
                val (_, ten) = dau
                val nhom = conLai.filter { re.matches(it.khoa) }
                conLai.removeAll(nhom)
                if (nhom.isEmpty()) return@forEach
                Text("${t(ten.first, ten.second)} · ${nhom.size}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChuPhu, modifier = Modifier.padding(top = 8.dp))
                nhom.forEach { tc -> DongNangCao(tc, vm.nangCao[tc.khoa] ?: "") { v -> if (v.isBlank()) vm.nangCao.remove(tc.khoa) else vm.nangCao[tc.khoa] = v } }
            }
            if (ds.isEmpty()) Phu("No hay opciones que coincidan.")
        }
        Column(Modifier.fillMaxWidth().background(The1).padding(horizontal = 16.dp, vertical = 10.dp)) {
            NutChinh("Listo", icon = Icons.Rounded.Check) { vm.moNangCao = false }
        }
    }
}

@Composable
private fun DongNangCao(tc: zone.vgs.tvsach.dulieu.TuyChon, gt: String, doi: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(The2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (gt.isNotBlank()) { Box(Modifier.size(8.dp).clip(CircleShape).background(Tim)); Spacer(Modifier.width(8.dp)) }
            Text(tc.tieuDe(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (gt.isNotBlank()) Hong else Chu, modifier = Modifier.weight(1f))
            if (tc.canThan) Text("Cuidado", fontSize = 11.sp, color = Cam, modifier = Modifier.clip(RoundedCornerShape(50)).background(Cam.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp))
        }
        // Khoá kỹ thuật vẫn hiện nhỏ (tra tài liệu CleanYourTV, hỏi trên nhóm) khi đã có tên tiếng Việt.
        if (tc.tieuDe() != tc.khoa) Text(tc.khoa, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ChuPhu)
        Phu(tc.moTaHien())
        when (tc.kieu) {
            "bool" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChonChip("Predeterminado", gt.isBlank()) { doi("") }
                ChonChip("Encendido", gt == "true") { doi("true") }
                ChonChip("Apagado", gt == "false") { doi("false") }
            }
            "chon" -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChonChip("Predeterminado", gt.isBlank()) { doi("") }
                tc.chon.forEach { c -> ChonChip(tc.nhanChon(c), gt == c) { doi(c) } }
            }
            else -> OutlinedTextField(gt, { doi(it) }, singleLine = tc.kieu == "so" || tc.kieu == "chu",
                placeholder = { Text(when (tc.kieu) {
                    "so" -> "${tc.min} a ${tc.max}"
                    "ds", "ds-so" -> "Separados por comas"
                    else -> "Vacío = predeterminado"
                }, fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = if (tc.kieu == "so") KeyboardType.Number else KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
        }
    }
}

@Composable
private fun ChonChip(chu: String, chon: Boolean, onClick: () -> Unit) {
    Text(chu, fontSize = 13.sp, fontWeight = if (chon) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (chon) Tim.copy(alpha = 0.3f) else The2)
            .border(1.dp, if (chon) Tim else Vien, RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp))
}

/** Thẻ xem trước theme: ảnh nền thật (nếu có) hoặc dải màu, kèm dock giả. */
@Composable
private fun TheTheme(th: Theme, chon: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val anh = remember(th.id) {
        (th.anhNen ?: th.anhXem)?.let { a -> runCatching {
            ctx.assets.open(a).use { android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 }) }
        }.getOrNull()?.asImageBitmap() }
    }
    Column(Modifier.width(132.dp).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(th.mau.map { Color(it) }))
            .border(if (chon) 2.dp else 1.dp, if (chon) Hong else Vien, RoundedCornerShape(12.dp))) {
            if (anh != null) Image(anh, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) { i -> Box(Modifier.size(width = 20.dp, height = 12.dp).clip(RoundedCornerShape(3.dp)).background(if (i == 0) Color.White else Color.White.copy(alpha = 0.55f))) }
            }
            Text("5:05", Modifier.align(Alignment.TopStart).padding(8.dp), fontSize = 13.sp, fontWeight = FontWeight.Light)
            if (chon) Icon(Icons.Rounded.CheckCircle, null, Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp), tint = Hong)
        }
        Text(t(th.tenVi, th.tenEn), fontSize = 13.sp, fontWeight = if (chon) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp))
    }
}

@Composable
private fun Hang(chu: String, phu: String, bat: Boolean, doi: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { doi(!bat) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(chu, fontSize = 14.sp); Phu(phu) }
        Switch(bat, doi, colors = SwitchDefaults.colors(checkedTrackColor = Tim))
    }
}

// ---------------- Xong ----------------

@Composable
private fun ManXong(vm: PhienLamViec) = Cuon {
    // Rời app khi đang ở màn này: hẹn tự tắt ADB sau 60 giây; quay lại trước đó thì huỷ.
    val vongDoi = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(vongDoi) {
        val nghe = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) vm.henTuTatAdb()
            if (e == androidx.lifecycle.Lifecycle.Event.ON_START) vm.huyTuTatAdb()
        }
        vongDoi.lifecycle.addObserver(nghe)
        onDispose { vongDoi.lifecycle.removeObserver(nghe) }
    }
    DauTrang({
        Box(Modifier.size(88.dp).clip(CircleShape).background(Color(0xFFDCFCE7)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, Modifier.size(48.dp), tint = Color(0xFF166534))
        }
    }, "¡Tu TV quedó limpia!",
        if (vm.homeXong && vm.phimHomeOk) "El inicio limpio está funcionando: pulsa Inicio en el control."
        else if (vm.homeXong) "El inicio limpio está instalado, pero el botón Inicio del control sigue abriendo el inicio anterior (mira la nota de abajo)."
        else null)
    The("Antes de salir", Icons.Rounded.Checklist) {
        BuocSo(1, "Prueba YouTube, Netflix, HDMI, Ajustes y el control.")
        BuocSo(2, "Reinicia una vez para ver si algo se vuelve a activar solo.")
        BuocSo(3, "Apaga ADB: si lo dejas activado, cualquiera en la red puede controlar la TV. Si sales de la app por más de un minuto, la TV apaga ADB sola.")
        Hang("Dejar ADB activado para la próxima vez", "La próxima vez que cambies el aspecto o restaures, no tendrás que volver a activar la depuración", vm.giuAdb) { vm.giuAdb = it }
        NutPhu("Ver la pantalla de la TV", Icons.Rounded.Screenshot) { vm.chupManHinh() }
        NutPhu("Reiniciar la TV", Icons.Rounded.RestartAlt) { vm.khoiDongLaiTv() }
    }
    // Mời vào cộng đồng VGS (22/10/2026, anh Alex duyệt): ?tu=cleantv-app để vgs.zone ghi nguồn đăng ký từ app.
    // TuneTV: créditos al proyecto original en lugar de la invitación a la comunidad VGS.
    The("Créditos", Icons.Rounded.Groups) {
        Phu("TuneTV es gratis. Es una versión en español de CleanTV, de AlexVGS (VGS), que a su vez se basa en CleanYourTV de Gathin. Todo el mérito es de ellos. Traducida por GeoL con ayuda de Claude.")
        val uri = androidx.compose.ui.platform.LocalUriHandler.current
        NutPhu("Ver el proyecto original (CleanTV)", Icons.Rounded.Groups) {
            runCatching { uri.openUri("https://github.com/vgscity/vgs-cleantv") }
        }
    }
    TheKichBan(vm)
    TheGuiDanhSach(vm)
    NutGopY(vm)
    NutChinh("Apagar ADB y terminar", enabled = !vm.dangBan, icon = Icons.Rounded.Lock) { vm.tatAdbVaKetThuc() }
    Phu("Después en la TV: Opciones para desarrolladores → apaga la depuración por USB y \"Revocar autorizaciones de depuración por USB\". El respaldo se queda en este teléfono para restaurar.")
}

// ---------------- Khôi phục ----------------

@Composable
private fun ManKhoiPhuc(vm: PhienLamViec) = Cuon {
    val sl = vm.saoLuu
    if (vm.khoiPhucXong) {
        DauTrang({ Icon(Icons.Rounded.CheckCircle, null, Modifier.size(72.dp), tint = Xanh) },
            "Restaurado", "La TV volvió a como estaba.")
        NutChinh("Siguiente: apagar ADB", icon = Icons.Rounded.Lock) { vm.man = Man.XONG }
        return@Cuon
    }
    if (sl == null) {
        // Không có bản sao lưu cả trên điện thoại lẫn trên tivi: bật lại mọi gói đang tắt, hỏi xác nhận rõ (22/10/2026).
        DauTrang({ OIcon(Icons.Rounded.Restore, 72.dp) }, "Restaurar sin respaldo",
            "No hay respaldo de ${vm.thongTin?.tenHienThi} ni en este teléfono ni en la TV.")
        val ds = vm.goiTatKhongSaoLuu
        if (ds == null) {
            The {
                Text("La app puede reactivar TODOS los paquetes desactivados en la TV, quitar el inicio limpio y devolver el inicio del fabricante. También vuelven los paquetes que hayas desactivado tú antes.")
            }
            NutChinh(if (vm.dangBan) "Leyendo…" else "Ver paquetes desactivados", enabled = !vm.dangBan, icon = Icons.Rounded.Search) { vm.docGoiTatKhongSaoLuu() }
            return@Cuon
        }
        var dongY by remember { mutableStateOf(false) }
        var goHomeKsl by remember { mutableStateOf(true) }
        The("${ds.size} paquetes desactivados", Icons.Rounded.Restore) {
            if (ds.isEmpty()) Phu("No hay paquetes desactivados.")
            ds.take(12).forEach { Text(it, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = ChuPhu) }
            if (ds.size > 12) Phu("…y ${ds.size - 12} más.")
        }
        The {
            Hang("Desinstalar también el inicio limpio", "Desmárcala para dejarla en la TV", goHomeKsl) { goHomeKsl = it }
            Hang("Entiendo: reactivar todas", "Incluidas las que desactivé antes de usar TuneTV", dongY) { dongY = it }
        }
        NutChinh(if (vm.dangBan) "Restaurando…" else "Restaurar", enabled = dongY && !vm.dangBan, icon = Icons.Rounded.Restore) {
            vm.chayKhoiPhucKhongSaoLuu(goHomeKsl)
        }
        vm.nhatKy.takeLast(4).forEach { Text(it, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = ChuPhu) }
        TheBatTungApp(vm)
        return@Cuon
    }
    DauTrang({ OIcon(Icons.Rounded.Restore, 72.dp) }, "Restaurar ${sl.ten}",
        "Reactivar ${sl.daTatBoiApp.size} paquetes" +
            (if (sl.launcherDaCai) ", restaurar el inicio de fábrica" else "") +
            (if (sl.dnsDaDoi) ", restaurar DNS" else "") + ".")
    var goHome by remember { mutableStateOf(true) }
    if (sl.launcherDaCai) The {
        Hang("Desinstalar también el inicio limpio", "Desmárcala para dejarla en la TV", goHome) { goHome = it }
    }
    NutChinh(if (vm.dangBan) "Restaurando…" else "Restaurar", enabled = !vm.dangBan, icon = Icons.Rounded.Restore) {
        vm.chayKhoiPhuc(sl.launcherDaCai && goHome)
    }
    vm.nhatKy.takeLast(4).forEach { Text(it, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = ChuPhu) }
    TheKichBan(vm)
    TheBatTungApp(vm)
}

/** Chữ nhỏ "Xem lệnh sẽ chạy": mở hộp liệt kê các lệnh chính app sắp gửi lên tivi (24/10/2026). */
@Composable
private fun NutXemLenh(lenh: () -> List<String>) {
    var mo by remember { mutableStateOf(false) }
    Text("Ver los comandos que va a ejecutar", fontSize = 13.sp, color = ChuPhu,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { mo = true }.padding(6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    if (mo) AlertDialog(
        onDismissRequest = { mo = false }, containerColor = The1,
        title = { Text("Comandos para la TV") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Phu("Los comandos principales, en orden. Al ejecutarse se siguen saltando los paquetes esenciales de Android y el inicio en uso.")
                lenh().forEach { d ->
                    Text(d, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = if (d.startsWith("#")) Hong else ChuPhu, modifier = Modifier.padding(top = if (d.startsWith("#")) 8.dp else 1.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { mo = false }) { Text("Cerrar") } },
    )
}

/** Lưu kịch bản khôi phục cho máy tính (24/10/2026): mất điện thoại hay gỡ CleanTV vẫn trả tivi về như cũ bằng adb. */
@Composable
private fun TheKichBan(vm: PhienLamViec) {
    val sl = vm.saoLuu ?: return
    The("Script de restauración para computador", Icons.Rounded.Save) {
        Phu("Guarda un archivo de comandos adb que deja la TV como estaba desde un computador, incluso sin este teléfono ni TuneTV. Windows: doble clic; Mac / Linux: sh nombre-del-archivo.bat.")
        val luu = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> uri?.let { vm.xuatKichBan(it) } }
        NutPhu("Guardar script de restauración", Icons.Rounded.Save) {
            luu.launch("tunetv-restaurar-" + sl.ten.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(30) + ".bat")
        }
        if (vm.cauHinhBao.isNotEmpty()) Phu(vm.cauHinhBao, Xanh)
    }
}

/** Bật lại từng app (issue vgs-cleantv#3): danh sách gói đang tắt đọc thẳng từ tivi, tích cái nào bật cái đó. */
@Composable
private fun TheBatTungApp(vm: PhienLamViec) {
    The("Reactivar apps sueltas", Icons.Rounded.Apps) {
        Phu("¿Solo necesitas recuperar unas pocas apps? La app lee de la TV todas las apps desactivadas (incluso si reinstalaste TuneTV); marca las que quieres activar.")
        val ds = vm.dsTatTv
        if (ds == null) {
            NutPhu("Ver apps desactivadas", Icons.Rounded.Search) { vm.docDsTat() }
            return@The
        }
        if (ds.isEmpty()) Phu("No hay apps desactivadas en la TV.")
        val cuaCleanTv = vm.saoLuu?.daTatBoiApp.orEmpty()
        ds.forEach { p ->
            val chon = vm.chonBatLai[p] == true
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { vm.chonBatLai[p] = !chon }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(chon, { vm.chonBatLai[p] = it })
                Column(Modifier.weight(1f)) {
                    Text(tenDep(p), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(p + if (p in cuaCleanTv) " · desactivada por TuneTV" else "", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ChuPhu, maxLines = 1)
                }
            }
        }
        if (vm.batLaiBao.isNotEmpty()) Phu(vm.batLaiBao, Xanh)
        val so = vm.chonBatLai.count { it.value }
        if (ds.isNotEmpty()) NutPhu("Reactivar $so seleccionadas", Icons.Rounded.Restore, enabled = so > 0 && !vm.dangBan) { vm.batLaiDaChon() }
    }
}

// ---------------- Nhắc + hướng dẫn chế độ nhà phát triển ----------------

/** Thẻ nổi bật ở màn chính: việc BẮT BUỘC làm trên tivi trước khi dùng app. */
@Composable
private fun TheNhacNpt(vm: PhienLamViec) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NenCanhBao)
        .border(1.5.dp, Cam.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = Cam); Spacer(Modifier.width(8.dp))
            Text("Primero: activa el modo desarrollador en la TV", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Phu("Sin esto, la app no puede llegar a la TV. Se hace una sola vez, en un minuto: Ajustes → Acerca de → pulsa Compilación 7 veces → activa la depuración por USB.", ChuCanhBao)
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Cam).clickable { vm.moHuongDan() }, contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MenuBook, null, tint = Color.Black); Spacer(Modifier.width(8.dp))
                Text("Guía con imágenes (7 pasos)", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
        Phu("Solo Android TV / Google TV. Samsung (Tizen) y LG (webOS) no son compatibles.")
    }
}

@Composable
private fun ManHuongDan(vm: PhienLamViec) {
    val ctx = LocalContext.current
    val thuMuc = "es" // TuneTV: capturas de la guía redibujadas en español
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Activa el modo desarrollador", fontWeight = FontWeight.Black, fontSize = 24.sp)
                Phu("Toma el control y sigue los 7 pasos. Las imágenes son de Android TV; los nombres pueden cambiar un poco según la marca (Sistema / Acerca de / Información…).")
            }
        }
        itemsIndexed(HUONG_DAN) { i, b ->
            val anh = remember(thuMuc, b.anh) {
                runCatching { ctx.assets.open("huongdan/$thuMuc/${b.anh}.webp").use { android.graphics.BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(The1).border(1.dp, Vien, RoundedCornerShape(20.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Gradient), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(t(b.tieuDeVi, b.tieuDeEn), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (anh != null) Image(anh, null, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Vien, RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth)
                Text(t(b.chuVi, b.chuEn), fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        item {
            The("Cambiar el aspecto después", Icons.Rounded.Palette) {
                BuocSo(1, "En la TV vuelve a activar \"Depuración por USB\" (las Opciones para desarrolladores siguen ahí, no hace falta pulsar Compilación otra vez).")
                BuocSo(2, "Abre la app → Limpiar una TV → conéctate de nuevo a esa TV. Si marcaste \"Permitir siempre\", la TV no volverá a preguntar.")
                BuocSo(3, "Toca \"Cambiar el tema del inicio limpio\", elige tema, dock, clima y saludo → Aplicar tema. Los paquetes desactivados se quedan como están.")
                BuocSo(4, "Cuando termines, pulsa Apagar ADB como antes.")
                Phu("Ajustes rápidos sin el teléfono: en la TV mantén OK sobre el botón Ajustes del inicio limpio para cambiar columnas, movimiento y orden de apps (los temas y fondos siguen necesitando la app).")
            }
        }
        item {
            The("Apágala cuando termines", Icons.Rounded.Lock) {
                Phu("El último paso de la app apaga ADB. Para estar seguro, vuelve a \"Opciones para desarrolladores\": apaga \"Depuración por USB\" y elige \"Revocar autorizaciones de depuración por USB\".")
            }
        }
        item {
            NutChinh("Listo: conecta la TV", icon = Icons.Rounded.WifiFind) { vm.man = Man.KET_NOI }
        }
    }
}

@Composable
private fun ManGiayPhep() {
    val ctx = LocalContext.current
    val chu = remember { runCatching { ctx.assets.open("giay-phep.txt").bufferedReader().readText() }.getOrDefault("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Licencias de código abierto", fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text(chu, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 16.sp, color = ChuPhu)
    }
}

// ---------------- Máy chiếu ----------------

/**
 * Dò màn lấy nét / chỉnh hình của hãng, mở thử trên máy chiếu, ghim vào nút "Máy chiếu" trên Home sạch.
 * Tivi thường không hiện thẻ này, trừ khi bấm dòng "Máy chiếu?" nhỏ.
 */
@Composable
private fun TheMayChieu(vm: PhienLamViec) {
    var moTay by rememberSaveable { mutableStateOf(false) }
    if (!vm.laMayChieu && vm.mayChieu == null && !moTay) {
        TextButton(onClick = { moTay = true }) {
            Icon(Icons.Rounded.Videocam, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text("¿Es un proyector? Agrega botones de enfoque / keystone")
        }
        return
    }
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val clip = androidx.compose.ui.platform.LocalClipboardManager.current
    The("Proyector: enfoque y keystone", Icons.Rounded.Videocam) {
        Phu("La app busca las pantallas de ajuste del fabricante. Toca Probar para revisar una y luego Fijar: el inicio limpio tendrá un botón \"Proyector\" que las abre. Los paquetes de enfoque / keystone nunca se desactivan.")
        val ds = vm.mayChieu
        if (ds == null) {
            NutPhu(if (vm.dangDoMayChieu) "Buscando… (unos 30 segundos)" else "Buscar pantallas de enfoque / keystone",
                Icons.Rounded.Search, enabled = !vm.dangDoMayChieu) { vm.doMayChieu() }
        } else {
            ds.forEach { comp ->
                val ghim = comp in vm.ghimMayChieu
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (ghim) Tim.copy(alpha = 0.2f) else The2)
                    .border(1.dp, if (ghim) Tim else Color.Transparent, RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text(vm.ghimMayChieu[comp] ?: vm.nhanMayChieu(comp), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(comp, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChuPhu, maxLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        TextButton(onClick = { vm.moThuMayChieu(comp) }, enabled = !vm.dangBan) { Text("Probar") }
                        TextButton(onClick = { vm.doiGhim(comp) }) { Text(if (ghim) "Quitar fijación" else "Fijar") }
                    }
                }
            }
            if (vm.ghimMayChieu.isNotEmpty()) Phu("${vm.ghimMayChieu.size} fijadas. Instala el inicio limpio / Aplica el tema para enviarlas al proyector.", Xanh)
        }
        NutPhu("Enviar la lista a VGS", Icons.Rounded.Feedback) {
            clip.setText(androidx.compose.ui.text.AnnotatedString(vm.chuDanhSachMayChieu()))
            val tu = "TuneTV proyector · " + (vm.thongTin?.tenHienThi ?: "")
            uri.openUri("https://vgs.zone/gop-y?tu=" + java.net.URLEncoder.encode(tu, "UTF-8"))
        }
        Phu("La lista se copió: pégala en el cuadro de comentarios.")
    }
}

/** Hộp chọn tay một mẫu trong toàn bộ danh sách, có ô tìm (23/10/2026). */
@Composable
private fun HopChonMau(vm: PhienLamViec) {
    var tim by remember { mutableStateOf("") }
    val khoa = XaPhuong.boDau(tim.trim().lowercase())
    val ds = remember(khoa, vm.tatCaMau) {
        if (khoa.isEmpty()) vm.tatCaMau else vm.tatCaMau.filter { m -> khoa.split(" ").all { it in XaPhuong.boDau(m.ten.lowercase()) || it in m.id } }
    }
    AlertDialog(
        onDismissRequest = { vm.moChonMau = false },
        confirmButton = { TextButton(onClick = { vm.moChonMau = false }) { Text("Cerrar") } },
        title = { Text("Elige una lista de TV") },
        text = {
            Column {
                OutlinedTextField(tim, { tim = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Busca marca o modelo (p. ej. sony vu3)") })
                Text("${ds.size} listas", fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(ds.take(120), key = { it.id }) { m ->
                        Text(m.ten.take(110), fontSize = 14.sp, modifier = Modifier.fillMaxWidth()
                            .clickable { vm.mauChon = m; vm.moChonMau = false }.padding(vertical = 10.dp))
                    }
                }
            }
        },
        containerColor = The1,
    )
}

/** Gửi danh sách gói cho VGS (23/10/2026): người dùng tự bấm, để làm mẫu cho tivi chưa có trong danh sách. */
@Composable
private fun TheGuiDanhSach(vm: PhienLamViec) {
    The("Ayuda a otros con la misma TV", Icons.Rounded.Share) {
        if (vm.dsDaGui) {
            Phu("¡Enviado, gracias! VGS lo revisará y agregará esta TV a las listas.", Xanh)
            return@The
        }
        Phu("Envía a VGS el modelo de esta TV y los paquetes apagados, para armar una lista para quienes tienen la misma TV. Sin nombre, correo ni lista de apps, a menos que agregues un contacto.")
        Text("Después de probarla, la TV:", fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChonChip("Funciona bien", vm.dsDanhGia == "on") { vm.dsDanhGia = "on" }
            ChonChip("Tiene problemas", vm.dsDanhGia == "chua-on") { vm.dsDanhGia = "chua-on" }
        }
        OutlinedTextField(vm.dsGhiChu, { if (it.length <= 1000) vm.dsGhiChu = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            label = { Text(if (vm.dsDanhGia == "on") "Nota (opcional)" else "¿Qué falla? (HDMI, control, voz…)") })
        // Chỉ nhận email (anh Alex 25/10/2026): VGS trả lời bằng thư từ cleantv@, không nhắn số điện thoại.
        OutlinedTextField(vm.dsLienHe, { if (it.length <= 120) vm.dsLienHe = it.trim() }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
            isError = vm.dsLienHe.isNotEmpty() && !laEmail(vm.dsLienHe),
            label = { Text("Correo para contactarte (opcional)") })
        NutPhu("Enviar la lista de paquetes a VGS", Icons.Rounded.Share, enabled = vm.dsLienHe.isEmpty() || laEmail(vm.dsLienHe)) { if (!vm.dangBan) vm.guiDanhSach() }
    }
}

// ---------------- Khoá trẻ em ----------------

private val TEN_THU = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
private val TEN_THU_EN = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do")

@Composable
private fun ManKhoaTre(vm: PhienLamViec) = Cuon {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Bloqueo infantil", fontWeight = FontWeight.Black, fontSize = 22.sp)
        Phu("Durante los horarios de bloqueo, la TV muestra una pantalla de bloqueo sobre todo (también HDMI y YouTube) y bloquea el control. El PIN correcto la desbloquea por ${vm.KHOA_MO_TAM} minutos. Sigue bloqueada después de reiniciar.")
    }
    vm.khoaTrenTv?.let { j ->
        The("En la TV", Icons.Rounded.Lock) {
            val bat = j.optBoolean("bat"); val chay = j.optBoolean("dichVuChay"); val dangKhoa = j.optBoolean("dangKhoa")
            val moDen = j.optLong("moTamDen")
            Phu(if (!bat) "El bloqueo está apagado."
                else if (!chay) "Activado, pero el servicio no está funcionando (reinicia la TV)."
                else if (dangKhoa) "La TV está bloqueada ahora."
                else if (moDen > System.currentTimeMillis()) "Desbloqueada hasta las ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(moDen))}."
                else "Activado, fuera de los horarios de bloqueo.", if (bat && chay) Xanh else ChuPhu)
            if (bat) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChonChip("Desbloquear ${vm.KHOA_MO_TAM} min", false) { vm.moTamKhoaTre(vm.KHOA_MO_TAM) }
                ChonChip("Bloquear ahora", false) { vm.moTamKhoaTre(0) }
            }
        }
    }
    The("Horarios de bloqueo", Icons.Rounded.Schedule) {
        Hang("Bloquear por horario", "Apagado: la TV funciona normal; el horario y el PIN se guardan", vm.khoaBat) { vm.khoaBat = it }
        vm.khoaKhung.forEachIndexed { i, k ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(The2).padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${k.tu} – ${k.den}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(if (k.ngay.size == 7) "Todos los días" else k.ngay.sorted().joinToString(" ") { t(TEN_THU[it - 1], TEN_THU_EN[it - 1]) }, fontSize = 12.sp, color = ChuPhu)
                }
                IconButton(onClick = { vm.khoaKhung.removeAt(i) }) { Icon(Icons.Rounded.Close, "Quitar") }
            }
        }
        Phu("Agregar rápido:")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChonChip("Noche 21:00–06:00", false) { vm.khoaKhung += PhienLamViec.KhungKhoa("21:00", "06:00", (1..7).toSet()) }
            ChonChip("Estudio 14:00–16:30 (lun–vie)", false) { vm.khoaKhung += PhienLamViec.KhungKhoa("14:00", "16:30", (1..5).toSet()) }
            ChonChip("Siesta 12:00–13:30", false) { vm.khoaKhung += PhienLamViec.KhungKhoa("12:00", "13:30", (1..7).toSet()) }
        }
        var tu by remember { mutableStateOf("") }
        var den by remember { mutableStateOf("") }
        var ngay by remember { mutableStateOf((1..7).toSet()) }
        Text("O pon la tuya:", fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(tu, { tu = it.take(5) }, label = { Text("Desde") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(den, { den = it.take(5) }, label = { Text("Hasta") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..7).forEach { d -> ChonChip(t(TEN_THU[d - 1], TEN_THU_EN[d - 1]), d in ngay) { ngay = if (d in ngay) ngay - d else ngay + d } }
        }
        NutPhu("Agregar este horario", Icons.Rounded.Add) {
            fun chuan(s: String) = s.trim().replace('.', ':').replace('h', ':').let { if (Regex("^[0-9]:").containsMatchIn(it)) "0$it" else it }.let { if (Regex("^[0-9]{2}$").matches(it)) "$it:00" else it }
            vm.khoaKhung += PhienLamViec.KhungKhoa(chuan(tu), chuan(den), ngay); tu = ""; den = ""
        }
        Phu("Los horarios que pasan la medianoche cuentan desde el día en que empiezan.")
    }
    The("PIN", Icons.Rounded.Key) {
        if (vm.khoaTrenTv?.optBoolean("coPin") == true) Phu("La TV ya tiene un PIN. Déjalo vacío para conservarlo.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(vm.khoaPin, { vm.khoaPin = it.filter(Char::isDigit).take(4) }, label = { Text("PIN de 4 dígitos") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            OutlinedTextField(vm.khoaPin2, { vm.khoaPin2 = it.filter(Char::isDigit).take(4) }, label = { Text("Repetir") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
        }
        Phu("5 intentos fallidos: espera 1 minuto. Si lo olvidas, vuelve a conectar aquí y pon uno nuevo. La TV solo guarda un hash.")
    }
    if (vm.khoaBao.isNotEmpty()) Phu(vm.khoaBao, Xanh)
    NutChinh("Guardar y aplicar a la TV", enabled = !vm.dangBan, icon = Icons.Rounded.Lock) { vm.luuKhoaTre() }
    if (vm.khoaTrenTv != null) NutPhu("Apagar y quitar", Icons.Rounded.LockOpen) { vm.tatKhoaTre() }
    Phu("Nota: no impide un restablecimiento de fábrica ni otro equipo por HDMI; con una TV box conectada a una TV, el selector de entradas de la TV sigue funcionando. Restaurar esta TV también quita el bloqueo.")
}

/** Ô liên hệ chỉ nhận email (góp ý trả lời qua thư). */
private fun laEmail(s: String) = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]{2,}$""").matches(s.trim())
