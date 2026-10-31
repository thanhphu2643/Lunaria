import SwiftUI

struct Article: Identifiable {
    let id = UUID()
    let icon: String
    let title: String
    let summary: String
    let body: String
}

private var articles: [Article] {
    AppLanguage.current.resolved == "vi" ? viArticles : enArticles
}

private let enArticles: [Article] = [
    Article(icon: "drop.fill", title: "The four phases of your cycle",
            summary: "Menstrual, follicular, ovulation and luteal.",
            body: """
            **Menstrual (day 1–5):** the uterine lining sheds. Hormones are at their lowest.

            **Follicular (day 1–13):** estrogen rises as follicles mature. Energy usually goes up.

            **Ovulation (around day 14 in a 28-day cycle):** an egg is released. It survives about 24 hours.

            **Luteal (day 15–28):** progesterone rises. If no pregnancy happens, hormone levels drop and the next period starts.
            """),
    Article(icon: "sparkles", title: "How Lunaria predicts",
            summary: "The calendar method, explained.",
            body: """
            Lunaria averages the length of your last six cycles (from period start to period start). \
            Ovulation is estimated 14 days before the next expected period — the luteal phase is fairly constant — \
            and the fertile window covers the 5 days before ovulation, ovulation day and the day after, because sperm can survive up to 5 days.

            Predictions improve as you log more cycles. They are estimates: **do not use them as contraception.**
            """),
    Article(icon: "stethoscope", title: "When to talk to a doctor",
            summary: "Signs worth checking.",
            body: """
            Consider seeing a healthcare professional if:
            • your cycles are regularly shorter than 21 or longer than 35 days
            • a period lasts more than 7 days
            • bleeding is very heavy (changing protection every 1–2 hours)
            • you have severe pain that stops daily activities
            • you miss 3 or more periods in a row and are not pregnant
            """),
    Article(icon: "lock.fill", title: "Your privacy",
            summary: "Why everything stays on your phone.",
            body: """
            Menstrual data is sensitive health data. Lunaria stores it only in a local file on your device. \
            There is no account, no cloud sync, no ads and no analytics SDK. The source code is open so anyone can verify this.
            """),
]

private let viArticles: [Article] = [
    Article(icon: "drop.fill", title: "Bốn giai đoạn của chu kỳ",
            summary: "Hành kinh, nang trứng, rụng trứng và hoàng thể.",
            body: """
            **Hành kinh (ngày 1–5):** niêm mạc tử cung bong ra. Nội tiết tố ở mức thấp nhất.

            **Nang trứng (ngày 1–13):** estrogen tăng khi các nang trứng phát triển. Năng lượng thường tăng lên.

            **Rụng trứng (khoảng ngày 14 với chu kỳ 28 ngày):** trứng được phóng ra và sống được khoảng 24 giờ.

            **Hoàng thể (ngày 15–28):** progesterone tăng. Nếu không có thai, nội tiết tố giảm xuống và kỳ kinh tiếp theo bắt đầu.
            """),
    Article(icon: "sparkles", title: "Lunaria dự đoán thế nào",
            summary: "Giải thích phương pháp tính theo lịch.",
            body: """
            Lunaria lấy trung bình độ dài 6 chu kỳ gần nhất của bạn (từ ngày bắt đầu kỳ này đến ngày bắt đầu kỳ sau). \
            Ngày rụng trứng được ước tính là 14 ngày trước kỳ kinh dự kiến, vì giai đoạn hoàng thể khá ổn định. \
            Cửa sổ thụ thai gồm 5 ngày trước rụng trứng, ngày rụng trứng và 1 ngày sau đó, vì tinh trùng có thể sống tới 5 ngày.

            Dự đoán sẽ chính xác hơn khi bạn ghi lại nhiều chu kỳ hơn. Đây chỉ là ước tính: **đừng dùng làm biện pháp tránh thai.**
            """),
    Article(icon: "stethoscope", title: "Khi nào nên gặp bác sĩ",
            summary: "Những dấu hiệu cần kiểm tra.",
            body: """
            Bạn nên đi khám nếu:
            • chu kỳ thường ngắn hơn 21 ngày hoặc dài hơn 35 ngày
            • kỳ kinh kéo dài hơn 7 ngày
            • ra máu rất nhiều (phải thay băng mỗi 1–2 giờ)
            • đau dữ dội đến mức không sinh hoạt bình thường được
            • trễ kinh từ 3 kỳ liên tiếp trở lên mà không mang thai
            """),
    Article(icon: "lock.fill", title: "Quyền riêng tư của bạn",
            summary: "Vì sao mọi dữ liệu chỉ nằm trên điện thoại.",
            body: """
            Dữ liệu kinh nguyệt là dữ liệu sức khỏe nhạy cảm. Lunaria chỉ lưu nó trong một file trên máy của bạn. \
            Không tài khoản, không đồng bộ đám mây, không quảng cáo, không SDK phân tích. Mã nguồn mở nên ai cũng có thể kiểm chứng điều này.
            """),
]

struct LearnView: View {
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(L("Learn"))
                        .font(.system(size: 34, weight: .bold))
                        .foregroundStyle(Theme.text)
                        .padding(.top, 8)
                    ForEach(articles) { a in
                        NavigationLink {
                            ScrollView {
                                Text(.init(a.body))
                                    .font(.system(size: 17))
                                    .lineSpacing(5)
                                    .padding(24)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .navigationTitle(a.title)
                            .navigationBarTitleDisplayMode(.inline)
                        } label: {
                            HStack(spacing: 16) {
                                Image(systemName: a.icon)
                                    .font(.system(size: 22))
                                    .foregroundStyle(Theme.accent)
                                    .frame(width: 56, height: 56)
                                    .background(Theme.iconBubble, in: Circle())
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(a.title).font(.system(size: 17, weight: .semibold)).foregroundStyle(Theme.text)
                                    Text(a.summary).font(.system(size: 14)).foregroundStyle(Theme.secondaryText)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").foregroundStyle(Theme.secondaryText)
                            }
                            .card()
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
            .toolbar(.hidden, for: .navigationBar)
        }
    }
}
