package com.bolobhai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.bolobhai.audio.waveformBars
import com.bolobhai.audio.wavData
import com.bolobhai.domain.formatDuration
import com.bolobhai.ui.theme.Bolo
import kotlinx.coroutines.delay
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * Inline audio player (BoloBhai.md §13.1): round play/pause, a 48-bar clickable waveform and a
 * mono `pos / len` readout. Auto-plays on appearance; releases the Clip when it leaves.
 */
@Composable
fun AudioPlayerBar(path: String, modifier: Modifier = Modifier) {
    val c = Bolo.colors
    val bars = remember(path) {
        runCatching { waveformBars(wavData(File(path).readBytes()), 48) }.getOrDefault(List(48) { 0f })
    }
    var clip by remember(path) { mutableStateOf<Clip?>(null) }
    var playing by remember(path) { mutableStateOf(false) }
    var fraction by remember(path) { mutableStateOf(0f) }

    DisposableEffect(path) {
        val c0 = runCatching {
            AudioSystem.getClip().apply { open(AudioSystem.getAudioInputStream(File(path))) }
        }.getOrNull()
        clip = c0
        c0?.start()
        playing = c0 != null
        onDispose { c0?.run { stop(); close() } }
    }

    // Drive the progress readout; stop at the end so the button flips back to Play.
    LaunchedEffect(playing, clip) {
        val cl = clip ?: return@LaunchedEffect
        while (playing) {
            val len = cl.microsecondLength.coerceAtLeast(1)
            fraction = (cl.microsecondPosition.toFloat() / len).coerceIn(0f, 1f)
            if (cl.microsecondPosition >= cl.microsecondLength) {
                cl.stop(); cl.microsecondPosition = 0; playing = false; fraction = 0f
            }
            delay(50)
        }
    }

    val lengthMs = (clip?.microsecondLength ?: 0) / 1000
    val posMs = (lengthMs * fraction).toLong()

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(c.blue)
                .pointerInput(clip) { detectTapGestures { toggle(clip, playing) { playing = it } } },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) BoloIcons.Pause else BoloIcons.Play,
                contentDescription = if (playing) "Pause" else "Play",
                tint = c.onAccent,
                modifier = Modifier.size(18.dp),
            )
        }
        Canvas(
            Modifier.weight(1f).height(48.dp).pointerInput(clip) {
                detectTapGestures { offset ->
                    val cl = clip ?: return@detectTapGestures
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    cl.microsecondPosition = (cl.microsecondLength * f).toLong()
                    fraction = f
                }
            },
        ) {
            val gap = 3.dp.toPx()
            val barW = (size.width - gap * (bars.size - 1)) / bars.size
            bars.forEachIndexed { i, amp ->
                val h = (amp * size.height).coerceAtLeast(2f)
                val x = i * (barW + gap)
                val played = (i + 0.5f) / bars.size <= fraction
                drawRect(
                    color = if (played) c.blue else c.border,
                    topLeft = Offset(x, (size.height - h) / 2),
                    size = Size(barW, h),
                )
            }
        }
        Text("${formatDuration(posMs)} / ${formatDuration(lengthMs)}", style = Bolo.type.monoSmall, color = c.muted)
    }
}

private fun toggle(clip: Clip?, playing: Boolean, set: (Boolean) -> Unit) {
    val cl = clip ?: return
    if (playing) {
        cl.stop(); set(false)
    } else {
        if (cl.microsecondPosition >= cl.microsecondLength) cl.microsecondPosition = 0
        cl.start(); set(true)
    }
}
