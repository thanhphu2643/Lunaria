package org.lunaria.app.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** In-app language choice. Reading it inside composition makes the UI recompose when it changes. */
enum class AppLanguage { SYSTEM, EN, VI;
    val label: String get() = when (this) { SYSTEM -> L("System"); EN -> "English"; VI -> "Tiếng Việt" }
}

object I18n {
    private const val KEY = "appLanguage"
    private var prefs: SharedPreferences? = null

    var language by mutableStateOf(AppLanguage.SYSTEM)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        language = prefs?.getString(KEY, null)?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM
    }

    fun select(lang: AppLanguage) {
        language = lang
        prefs?.edit()?.putString(KEY, lang.name)?.apply()
    }

    /** "vi" or "en". */
    val resolved: String
        get() = when (language) {
            AppLanguage.EN -> "en"
            AppLanguage.VI -> "vi"
            AppLanguage.SYSTEM -> if (Locale.getDefault().language == "vi") "vi" else "en"
        }

    val isVietnamese: Boolean get() = resolved == "vi"

    /** Locale for dates and numbers. */
    val locale: Locale
        get() = when {
            isVietnamese -> Locale.forLanguageTag("vi-VN")
            language == AppLanguage.SYSTEM -> Locale.getDefault()
            else -> Locale.forLanguageTag("en-" + Locale.getDefault().country.ifEmpty { "US" })
        }
}

/** Localized string for the selected app language. English text is the key; shares translations with iOS. */
fun L(key: String, vararg args: Any): String {
    val format = if (I18n.isVietnamese) VI[key] ?: key else key
    return if (args.isEmpty()) format else String.format(I18n.locale, format, *args)
}

/** "1 day" / "3 days" in English, "3 ngày" in Vietnamese. */
fun Ldays(n: Int): String = if (n == 1) L("%d day", n) else L("%d days", n)

private val VI: Map<String, String> = mapOf(
    "%d day" to "%d ngày",
    "%d days" to "%d ngày",
    "%d weeks %d days" to "%d tuần %d ngày",
    "%s late" to "Trễ %s",
    "Acne" to "Nổi mụn",
    "Add just this day" to "Chỉ thêm ngày này",
    "Anxious" to "Lo âu",
    "App" to "Ứng dụng",
    "Avg. cycle" to "Chu kỳ TB",
    "Avg. period" to "Hành kinh TB",
    "Back" to "Quay lại",
    "Back pain" to "Đau lưng",
    "Best day to test" to "Ngày thử thai tốt nhất",
    "Bloating" to "Đầy hơi",
    "Calculates estimated due date" to "Ước tính ngày dự sinh",
    "Calculates optimal date for pregnancy test." to "Tính ngày thử thai phù hợp nhất.",
    "Calendar" to "Lịch",
    "Calm" to "Bình yên",
    "Cancel" to "Hủy",
    "Cramps" to "Đau bụng kinh",
    "Cravings" to "Thèm ăn",
    "Cycle" to "Chu kỳ",
    "Cycle day %d" to "Ngày %d của chu kỳ",
    "Cycle length" to "Độ dài chu kỳ",
    "Cycle length: %s" to "Độ dài chu kỳ: %s",
    "Cycles logged" to "Chu kỳ đã ghi",
    "Daily Log" to "Nhật ký hằng ngày",
    "Day" to "Ngày",
    "Decrease %s" to "Giảm %s",
    "Delete period" to "Xóa kỳ kinh",
    "Done" to "Xong",
    "Due Date Calculator" to "Tính ngày dự sinh",
    "Due date" to "Ngày dự sinh",
    "Earliest (sensitive test)" to "Sớm nhất (que nhạy)",
    "Erase" to "Xóa",
    "Erase all data" to "Xóa toàn bộ dữ liệu",
    "Erase all data?" to "Xóa toàn bộ dữ liệu?",
    "Estimated due date" to "Ngày dự sinh dự kiến",
    "Estimated ovulation" to "Ngày rụng trứng dự kiến",
    "Estimates only, based on the calendar method. Always confirm with a healthcare professional." to "Chỉ là ước tính theo phương pháp tính lịch. Hãy luôn xác nhận lại với bác sĩ.",
    "Estimates pregnancy week." to "Ước tính tuần thai.",
    "Estrogen is rising — energy and mood often improve." to "Estrogen đang tăng, năng lượng và tâm trạng thường tốt lên.",
    "Export backup" to "Xuất bản sao lưu",
    "Fatigue" to "Mệt mỏi",
    "Fertile Days" to "Ngày dễ thụ thai",
    "Fertile window" to "Cửa sổ thụ thai",
    "First day of last period" to "Ngày đầu kỳ kinh gần nhất",
    "Flow" to "Lượng máu",
    "Follicular phase" to "Giai đoạn nang trứng",
    "General Tools:" to "Công cụ:",
    "Happy" to "Vui vẻ",
    "Headache" to "Đau đầu",
    "Heavy" to "Nhiều",
    "Hello" to "Xin chào",
    "Highest chance of pregnancy in this window." to "Khả năng mang thai cao nhất trong giai đoạn này.",
    "History" to "Lịch sử",
    "Home" to "Trang chủ",
    "How are you feeling?" to "Hôm nay bạn thấy thế nào?",
    "Import backup" to "Nhập bản sao lưu",
    "Increase %s" to "Tăng %s",
    "Insomnia" to "Mất ngủ",
    "Intimacy" to "Quan hệ",
    "Irritated" to "Cáu gắt",
    "Language" to "Ngôn ngữ",
    "Learn" to "Tìm hiểu",
    "Learn from my history" to "Học từ lịch sử của tôi",
    "Light" to "Ít",
    "Likely conception" to "Ngày thụ thai có thể",
    "Log" to "Nhật ký",
    "Log the first day of your last period and Lunaria will predict your next period, ovulation and fertile window. Everything stays on this device." to "Ghi lại ngày đầu tiên của kỳ kinh gần nhất, Lunaria sẽ dự đoán kỳ kinh tiếp theo, ngày rụng trứng và cửa sổ thụ thai. Mọi dữ liệu chỉ nằm trên máy này.",
    "Log today" to "Ghi hôm nay",
    "Longer" to "Dài hơn",
    "Longest cycle" to "Chu kỳ dài nhất",
    "Lunaria backup" to "Bản sao lưu Lunaria",
    "Lunaria has no account, no server and no analytics. Your data never leaves this device unless you export it." to "Lunaria không có tài khoản, không máy chủ, không thu thập phân tích. Dữ liệu của bạn không rời khỏi máy này trừ khi bạn tự xuất ra.",
    "Lunaria has no account, no server, no analytics and no internet permission. Your data never leaves this device unless you export it." to "Lunaria không có tài khoản, không máy chủ, không thu thập phân tích và không cần quyền truy cập internet. Dữ liệu của bạn không rời khỏi máy này trừ khi bạn tự xuất ra.",
    "Luteal phase" to "Giai đoạn hoàng thể",
    "Luteal phase: %s" to "Giai đoạn hoàng thể: %s",
    "Medium" to "Vừa",
    "Menstrual phase" to "Giai đoạn hành kinh",
    "Mood" to "Tâm trạng",
    "My period started today" to "Hôm nay tôi bắt đầu có kinh",
    "Nausea" to "Buồn nôn",
    "Next month" to "Tháng sau",
    "Next period" to "Kỳ kinh tới",
    "No" to "Không",
    "No periods logged yet" to "Chưa ghi kỳ kinh nào",
    "Note (only stored on this phone)" to "Ghi chú (chỉ lưu trên máy này)",
    "Notes" to "Ghi chú",
    "OK" to "OK",
    "Open source (MIT)" to "Mã nguồn mở (MIT)",
    "Ovulation" to "Rụng trứng",
    "Period" to "Hành kinh",
    "Period ended" to "Đã hết kinh",
    "Period ended this day" to "Hết kinh vào ngày này",
    "Period expected today" to "Dự kiến có kinh hôm nay",
    "Period in" to "Có kinh sau",
    "Period in %s, cycle day %d" to "Có kinh sau %s, ngày %d của chu kỳ",
    "Period length" to "Số ngày hành kinh",
    "Period length: %s" to "Số ngày hành kinh: %s",
    "Period started" to "Bắt đầu có kinh",
    "Periods" to "Các kỳ kinh",
    "Pick a past date" to "Chọn ngày trước đó",
    "Predicted" to "Dự đoán",
    "Pregnancy Calculator" to "Tính tuần thai",
    "Pregnancy Test Calculator" to "Tính ngày thử thai",
    "Previous month" to "Tháng trước",
    "Profile" to "Hồ sơ",
    "Progesterone peaks. PMS symptoms may appear near the end." to "Progesterone đạt đỉnh. Có thể xuất hiện triệu chứng tiền kinh nguyệt vào cuối giai đoạn.",
    "Protected" to "Có bảo vệ",
    "Remove this day" to "Bỏ ngày này",
    "Rest, hydrate and keep warm. Energy is usually lower." to "Nghỉ ngơi, uống đủ nước và giữ ấm. Năng lượng thường thấp hơn.",
    "Result" to "Kết quả",
    "Sad" to "Buồn",
    "Safe Days" to "Ngày an toàn",
    "Sensitive" to "Nhạy cảm",
    "Shorter" to "Ngắn hơn",
    "Shortest cycle" to "Chu kỳ ngắn nhất",
    "Something went wrong" to "Đã có lỗi xảy ra",
    "Spotting" to "Lấm tấm",
    "Symptoms" to "Triệu chứng",
    "System" to "Theo hệ thống",
    "Tap a day to log when your last period started — predictions appear instantly." to "Chạm vào một ngày để ghi ngày bắt đầu kỳ kinh gần nhất, dự đoán sẽ hiện ngay.",
    "Tender breasts" to "Căng ngực",
    "This permanently removes every period and log from this device." to "Thao tác này xóa vĩnh viễn mọi kỳ kinh và nhật ký trên máy.",
    "Trimester" to "Tam cá nguyệt",
    "Unprotected" to "Không bảo vệ",
    "Welcome to Lunaria" to "Chào mừng đến với Lunaria",
    "With “Learn from my history” on, averages of your last 6 cycles are used once you have logged data." to "Khi bật “Học từ lịch sử của tôi”, app dùng trung bình 6 chu kỳ gần nhất sau khi bạn đã ghi dữ liệu.",
    "You are" to "Bạn đang ở",
    "You can log periods up to today." to "Chỉ ghi được kỳ kinh đến hôm nay.",
    "Your data" to "Dữ liệu của bạn",
    "days" to "ngày",
    "intimacy logged" to "có quan hệ",
)
