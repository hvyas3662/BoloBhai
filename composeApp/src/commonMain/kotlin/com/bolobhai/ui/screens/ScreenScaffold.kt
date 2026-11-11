package com.bolobhai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bolobhai.ui.theme.Bolo

/** Standard screen frame (BoloBhai.md §13.1): header (H1 + sub + optional trailing) over a body. */
@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    body: @Composable () -> Unit,
) {
    val c = Bolo.colors
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = Bolo.type.h1, color = c.text)
                if (subtitle != null) {
                    Text(subtitle, style = Bolo.type.body, color = c.muted)
                }
            }
            if (trailing != null) trailing()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
        Box(Modifier.fillMaxSize()) { body() }
    }
}

/** Placeholder body used by all M1 screens until their milestone fills them in. */
@Composable
fun ComingSoon() {
    Text(
        "Coming soon",
        style = Bolo.type.body,
        color = Bolo.colors.muted,
        modifier = Modifier.padding(32.dp),
    )
}
