package org.lunaria.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lunaria.app.LocalCycle

enum class Tab(private val titleKey: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    Calendar("Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Log("Log", Icons.Outlined.Sms, Icons.Filled.Sms),
    Learn("Learn", Icons.AutoMirrored.Outlined.Article, Icons.AutoMirrored.Filled.Article),
    Profile("Profile", Icons.Outlined.Person, Icons.Filled.Person);

    val title: String get() = L(titleKey)
}

@Composable
fun RootScreen() {
    val cycle = LocalCycle.current
    var tab by rememberSaveable { mutableStateOf(Tab.Calendar) }

    Scaffold(
        containerColor = Color.White,
        bottomBar = { TabBar(tab) { tab = it } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.Home -> HomeScreen(selectTab = { tab = it })
                Tab.Calendar -> CalendarScreen()
                Tab.Log -> DailyLogScreen()
                Tab.Learn -> LearnScreen()
                Tab.Profile -> ProfileScreen()
            }
        }
    }

    cycle.error?.let { message ->
        AlertDialog(
            onDismissRequest = { cycle.error = null },
            confirmButton = { TextButton(onClick = { cycle.error = null }) { Text(L("OK")) } },
            title = { Text(L("Something went wrong")) },
            text = { Text(message) },
        )
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.background(Color.White).navigationBarsPadding()) {
        HorizontalDivider(color = Palette.text.copy(alpha = 0.06f))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val isSelected = tab == selected
                val bg by animateColorAsState(if (isSelected) Palette.accent else Color.Transparent, label = "tabBg")
                Row(
                    Modifier
                        .height(50.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClickLabel = tab.title,
                        ) { onSelect(tab) }
                        .padding(horizontal = if (isSelected) 20.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (isSelected) tab.selectedIcon else tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) Color.White else Palette.text.copy(alpha = 0.75f),
                    )
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally(),
                    ) {
                        Row {
                            Spacer(Modifier.width(8.dp))
                            Text(tab.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
