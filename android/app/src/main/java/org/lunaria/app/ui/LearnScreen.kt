package org.lunaria.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class Article(val icon: ImageVector, val title: String, val summary: String, val body: String)

private val articles = listOf(
    Article(
        Icons.Filled.WaterDrop, "The four phases of your cycle", "Menstrual, follicular, ovulation and luteal.",
        """
        **Menstrual (day 1–5):** the uterine lining sheds. Hormones are at their lowest.

        **Follicular (day 1–13):** estrogen rises as follicles mature. Energy usually goes up.

        **Ovulation (around day 14 in a 28-day cycle):** an egg is released. It survives about 24 hours.

        **Luteal (day 15–28):** progesterone rises. If no pregnancy happens, hormone levels drop and the next period starts.
        """.trimIndent(),
    ),
    Article(
        Icons.Filled.AutoAwesome, "How Lunaria predicts", "The calendar method, explained.",
        """
        Lunaria averages the length of your last six cycles (from period start to period start). Ovulation is estimated 14 days before the next expected period — the luteal phase is fairly constant — and the fertile window covers the 5 days before ovulation, ovulation day and the day after, because sperm can survive up to 5 days.

        Predictions improve as you log more cycles. They are estimates: **do not use them as contraception.**
        """.trimIndent(),
    ),
    Article(
        Icons.Filled.MedicalServices, "When to talk to a doctor", "Signs worth checking.",
        """
        Consider seeing a healthcare professional if:
        • your cycles are regularly shorter than 21 or longer than 35 days
        • a period lasts more than 7 days
        • bleeding is very heavy (changing protection every 1–2 hours)
        • you have severe pain that stops daily activities
        • you miss 3 or more periods in a row and are not pregnant
        """.trimIndent(),
    ),
    Article(
        Icons.Filled.Lock, "Your privacy", "Why everything stays on your phone.",
        """
        Menstrual data is sensitive health data. Lunaria stores it only in a local file on your device. The app does not even request internet permission. There is no account, no cloud sync, no ads and no analytics SDK. The source code is open so anyone can verify this.
        """.trimIndent(),
    ),
)

/** Renders `**bold**` spans; enough for the short built-in articles. */
private fun markdownBold(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) } else append(part)
    }
}

@Composable
fun LearnScreen() {
    var openIndex by rememberSaveable { mutableStateOf(-1) }
    val open = articles.getOrNull(openIndex)

    if (open != null) {
        BackHandler { openIndex = -1 }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = { openIndex = -1 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.accent) }
                Text(open.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Palette.text)
            }
            Spacer(Modifier.height(12.dp))
            Text(markdownBold(open.body), fontSize = 17.sp, lineHeight = 26.sp, color = Palette.text)
            Spacer(Modifier.height(24.dp))
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Learn")
        articles.forEachIndexed { i, a ->
            Row(
                Modifier.fillMaxWidth().card().clickable { openIndex = i },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(56.dp).background(Palette.iconBubble, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(a.icon, null, tint = Palette.accent)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Palette.text)
                    Text(a.summary, fontSize = 14.sp, color = Palette.secondaryText)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.secondaryText)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
