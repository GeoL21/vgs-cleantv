package zone.vgs.tvsach

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Hai ngôn ngữ: Tiếng Việt và English. Mặc định theo máy (máy tiếng Việt → vi, còn lại → en),
 * đổi tay ở màn chính. Mọi chữ hiện ra viết dạng "es" để hai bản luôn đi cùng nhau.
 */
object NgonNgu {
    var tiengViet by mutableStateOf(false)
        private set

    fun nap(ctx: Context) {
        // TuneTV: solo español. Los textos de datos usan la segunda opción (traducida al español).
        tiengViet = false
    }

    fun doi(ctx: Context) {
        // TuneTV: solo español, no hay cambio de idioma.
    }
}

fun t(vi: String, en: String): String = if (NgonNgu.tiengViet) vi else en
