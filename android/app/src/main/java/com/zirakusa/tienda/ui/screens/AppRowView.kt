package com.zirakusa.tienda.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.tienda.R
import com.zirakusa.tienda.data.AppRow
import com.zirakusa.tienda.data.RowState
import com.zirakusa.tienda.ui.Phase
import com.zirakusa.tienda.ui.StoreState
import com.zirakusa.tienda.ui.theme.StoreColors

/** One app: colour dot with its initial, name, versions and its button; a tap opens what changed. */
@Composable fun AppRowView(state: StoreState, row: AppRow) {
    val app = row.app
    val self = app.packageName == state.selfPackage
    val job = state.job?.takeIf { it.packageName == app.packageName }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(StoreColors.Card)
        .clickable { state.toggleNotes(row) }.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Dot(app.name, Color(0xFF000000 or app.color.toLong()))
            Column(Modifier.weight(1f)) {
                Text(app.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(versions(row, self), color = StoreColors.Muted, fontSize = 13.sp)
            }
            RowButton(state, row, self)
        }
        when {
            job != null -> Progress(job.phase, job.progress)
            app.packageName in state.queue -> Text(stringResource(R.string.queued), color = StoreColors.Muted,
                fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (state.expanded == app.packageName) {
            Text(stringResource(R.string.notes_title, app.versionName), fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                color = StoreColors.AccentText, modifier = Modifier.padding(top = 10.dp))
            Text(app.notes.ifBlank { stringResource(R.string.no_notes) }, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable private fun versions(row: AppRow, self: Boolean): String {
    val now = row.installed?.versionName.orEmpty()
    return when {
        row.state == RowState.NOT_INSTALLED -> stringResource(R.string.row_not_installed, row.app.versionName)
        row.state == RowState.UPDATE -> stringResource(R.string.row_update, now, row.app.versionName)
        self -> stringResource(R.string.row_self, now)
        else -> stringResource(R.string.row_up_to_date, now)
    }
}

@Composable private fun Dot(name: String, color: Color) {
    val light = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f) > 0.6f
    Box(Modifier.size(44.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = if (light) Color(0xFF111111) else Color.White, fontSize = 20.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable private fun RowButton(state: StoreState, row: AppRow, self: Boolean) {
    if (self && row.state == RowState.UP_TO_DATE) return
    val enabled = !state.busy
    when (row.state) {
        RowState.UP_TO_DATE -> OutlinedButton(onClick = { state.act(row) }, enabled = enabled) {
            Text(stringResource(R.string.open), color = StoreColors.AccentText)
        }
        else -> Button(onClick = { state.act(row) }, enabled = enabled, colors = ButtonDefaults.buttonColors(
            containerColor = StoreColors.Accent, contentColor = StoreColors.OnAccent,
            disabledContainerColor = StoreColors.Chip, disabledContentColor = StoreColors.Muted)) {
            Text(stringResource(if (row.state == RowState.UPDATE) R.string.update else R.string.install))
        }
    }
}

@Composable private fun Progress(phase: Phase, progress: Float) {
    Column(Modifier.padding(top = 10.dp)) {
        if (phase == Phase.DOWNLOADING) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(),
            color = StoreColors.Accent, trackColor = StoreColors.Chip)
        else LinearProgressIndicator(Modifier.fillMaxWidth(), color = StoreColors.Accent, trackColor = StoreColors.Chip)
        val text = when (phase) {
            Phase.DOWNLOADING -> stringResource(R.string.downloading, (progress * 100).toInt().coerceIn(0, 100))
            Phase.VERIFYING -> stringResource(R.string.verifying)
            Phase.INSTALLING -> stringResource(R.string.installing)
        }
        Text(text, color = StoreColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
