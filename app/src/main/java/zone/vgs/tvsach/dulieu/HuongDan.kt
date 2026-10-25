package zone.vgs.tvsach.dulieu

/**
 * Hướng dẫn bật chế độ nhà phát triển + gỡ lỗi trên tivi. Ảnh chụp thật từ Android TV 14 (máy ảo, tiếng Việt và
 * English) ở assets/huongdan/{vi,en}/<anh>.webp. Chữ trong ngoặc kép là đúng tên mục trên tivi.
 */
data class BuocHuongDan(val anh: String, val tieuDeVi: String, val tieuDeEn: String, val chuVi: String, val chuEn: String)

val HUONG_DAN = listOf(
    BuocHuongDan(
        "1-caidat",
        "Mở Cài đặt của tivi", "Abre los Ajustes de la TV",
        "Bấm nút bánh răng trên remote, hoặc chọn biểu tượng Cài đặt ở góc trên bên phải màn Home. Chọn \"Lựa chọn ưu tiên về thiết bị\". Google TV: chọn \"Hệ thống\".",
        "Pulsa el botón del engranaje en el control, o elige el ícono de Ajustes arriba a la derecha del inicio. Elige \"Preferencias del dispositivo\". En Google TV: elige \"Sistema\".",
    ),
    BuocHuongDan(
        "2-tuychon",
        "Vào Giới thiệu", "Entra a Acerca de",
        "Chọn \"Giới thiệu\" (có hãng ghi \"Thông tin\" hoặc \"About\").",
        "Elige \"Acerca de\" (algunas marcas lo llaman \"Información\").",
    ),
    BuocHuongDan(
        "3-bandung",
        "Bấm OK vào Bản dựng 7 lần", "Pulsa OK 7 veces sobre Compilación",
        "Kéo xuống cuối, đứng ở \"Bản dựng hệ điều hành Android TV\" (hoặc \"Bản dựng\") rồi bấm OK liên tục 7 lần, tới khi hiện \"Bạn đã là nhà phát triển!\". Tivi hỏi mã PIN thì nhập mã của tivi.",
        "Baja hasta el final, quédate en \"Compilación del SO de Android TV\" (o \"Compilación\") y pulsa OK 7 veces hasta que salga \"¡Ya eres desarrollador!\". Si pide el PIN de la TV, escríbelo.",
    ),
    BuocHuongDan(
        "4-tuychonnpt",
        "Mở Tùy chọn nhà phát triển", "Abre las Opciones para desarrolladores",
        "Bấm Back một lần. Cuối danh sách \"Lựa chọn ưu tiên về thiết bị\" (Google TV: trong \"Hệ thống\") có thêm \"Tùy chọn nhà phát triển\" — chọn nó.",
        "Pulsa Atrás una vez. Al final de \"Preferencias del dispositivo\" (Google TV: dentro de \"Sistema\") aparece \"Opciones para desarrolladores\": ábrela.",
    ),
    BuocHuongDan(
        "5-goloi",
        "Bật Gỡ lỗi qua USB", "Activa la depuración por USB",
        "Bật \"Gỡ lỗi qua USB\". Tivi nào có thêm \"Gỡ lỗi qua mạng\" / \"ADB debugging\" thì bật luôn. Không cần cắm dây USB nào cả — app kết nối qua Wi-Fi.",
        "Activa \"Depuración por USB\". Si la TV también tiene \"Depuración de red\" / \"Depuración ADB\", actívala también. No necesitas ningún cable USB: la app se conecta por Wi‑Fi.",
    ),
    BuocHuongDan(
        "6-goloiwifi",
        "Google TV đời mới: Gỡ lỗi qua Wi-Fi", "Google TV nuevas: Depuración inalámbrica",
        "Nếu app quét không thấy tivi: bật \"Gỡ lỗi qua Wi-Fi\", vào trong chọn \"Ghép nối thiết bị bằng mã ghép nối\". Nhập IP:cổng và mã 6 số hiện trên tivi vào phần \"Ghép đôi bằng mã\" trong app.",
        "Si la app no encuentra la TV: activa \"Depuración inalámbrica\", entra y elige \"Vincular dispositivo con un código de vinculación\". Escribe la IP:puerto y el código de 6 dígitos que muestra la TV en \"Vincular con código\" dentro de la app.",
    ),
    BuocHuongDan(
        "7-chophep",
        "Bấm Cho phép trên tivi", "Pulsa Permitir en la TV",
        "Lần đầu app kết nối, tivi hiện \"Cho phép gỡ lỗi qua USB?\". Tích \"Luôn cho phép từ máy tính này\" rồi bấm \"Cho phép\". Không thấy hộp này thì bấm Kết nối lại.",
        "La primera vez que la app se conecta, la TV muestra \"¿Permitir depuración por USB?\". Marca \"Permitir siempre desde esta computadora\" y pulsa \"Permitir\". ¿No aparece? Pulsa Conectar otra vez.",
    ),
)
