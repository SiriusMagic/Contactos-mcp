package com.zirakusa.tienda.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.tienda.R
import com.zirakusa.tienda.data.AppRow
import com.zirakusa.tienda.data.Formats
import com.zirakusa.tienda.data.RowState
import com.zirakusa.tienda.ui.Job
import com.zirakusa.tienda.ui.Phase
import com.zirakusa.tienda.ui.StoreState
import com.zirakusa.tienda.ui.theme.StoreColors

/**
 * One app, like a store: its icon (a ring around it while it downloads), name, category and version, and small chips
 * (size, «Verificada»); a rounded button on the right. A tap opens its details: what is new, «Desinstalar» and the button.
 */
@Composable fun AppRowView(state: StoreState, row: AppRow) {
    val app = row.app
    val self = app.packageName == state.selfPackage
    val job = state.job?.takeIf { it.packageName == app.packageName }
    val queued = app.packageName in state.queue
    val open = state.expanded == app.packageName
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(if (open) StoreColors.Card else Color.Transparent)
        .clickable { state.toggleNotes(row) }.padding(horizontal = 6.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(state, row, 56.dp, job)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(app.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle(row, self), color = StoreColors.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    when {
                        job != null -> Chip(progressText(job, app.size))
                        queued -> Chip(stringResource(R.string.queued))
                        else -> {
                            Formats.size(app.size).takeIf { it.isNotEmpty() }?.let { Chip(it) }
                            Chip(stringResource(R.string.verified), verified = true)
                        }
                    }
                }
            }
            when {
                job?.phase == Phase.DOWNLOADING || queued -> Pill(R.string.cancel, outlined = true, onClick = state::cancel)
                job != null -> Unit
                else -> MainButton(state, row, self)
            }
        }
        if (open) Details(state, row, self)
    }
}

/** Details: what changed in the new version, and «Desinstalar» next to the main button (like a store's app page). */
@Composable private fun Details(state: StoreState, row: AppRow, self: Boolean) {
    Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.notes_title, row.app.versionName), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = StoreColors.AccentText)
        Text(row.app.notes.ifBlank { stringResource(R.string.no_notes) }, fontSize = 14.sp, lineHeight = 20.sp)
        if (row.installed != null && !self) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(R.string.uninstall, outlined = true, modifier = Modifier.weight(1f), enabled = !state.busy) { state.uninstall(row) }
            Pill(if (row.state == RowState.UPDATE) R.string.update else R.string.open, modifier = Modifier.weight(1f), enabled = !state.busy) { state.act(row) }
        }
    }
}

/** The real icon (installed, or the catalog's picture), or the initial on the app's colour; a ring shows the download. */
@Composable fun AppIcon(state: StoreState, row: AppRow, size: Dp, job: Job?) {
    val icon by produceState<ImageBitmap?>(null, row.app.id, row.app.versionCode, row.installed) { value = state.icon(row.app)?.asImageBitmap() }
    Box(Modifier.size(size + 12.dp), contentAlignment = Alignment.Center) {
        if (job != null) {
            if (job.phase == Phase.DOWNLOADING && job.progress > 0f) CircularProgressIndicator(progress = { job.progress }, Modifier.fillMaxSize(),
                color = StoreColors.Accent, trackColor = StoreColors.Chip, strokeWidth = 3.dp)
            else CircularProgressIndicator(Modifier.fillMaxSize(), color = StoreColors.Accent, trackColor = StoreColors.Chip, strokeWidth = 3.dp)
        }
        val inner = if (job != null) size - 8.dp else size
        val shape = RoundedCornerShape(inner * 0.24f)
        icon?.let { Image(it, row.app.name, Modifier.size(inner).clip(shape)) } ?: Box(
            Modifier.size(inner).clip(shape).background(Color(0xFF000000 or row.app.color.toLong())), contentAlignment = Alignment.Center) {
            Text(row.app.name.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun MainButton(state: StoreState, row: AppRow, self: Boolean) {
    if (self && row.state == RowState.UP_TO_DATE) return
    when (row.state) {
        RowState.NOT_INSTALLED -> Pill(R.string.install, enabled = !state.busy) { state.act(row) }
        RowState.UPDATE -> Pill(R.string.update, outlined = true, enabled = !state.busy) { state.act(row) }
        RowState.UP_TO_DATE -> Pill(R.string.open, outlined = true, enabled = !state.busy) { state.act(row) }
    }
}

/** A rounded button: filled (the main action) or outlined. */
@Composable private fun Pill(label: Int, modifier: Modifier = Modifier, outlined: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    if (outlined) OutlinedButton(onClick, modifier, enabled = enabled, shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) StoreColors.Muted else StoreColors.Chip)) {
        Text(stringResource(label), color = if (enabled) StoreColors.AccentText else StoreColors.Muted, fontWeight = FontWeight.SemiBold)
    }
    else Button(onClick, modifier, enabled = enabled, shape = CircleShape, colors = ButtonDefaults.buttonColors(
        containerColor = StoreColors.Accent, contentColor = StoreColors.OnAccent, disabledContainerColor = StoreColors.Chip,
        disabledContentColor = StoreColors.Muted)) { Text(stringResource(label), fontWeight = FontWeight.SemiBold) }
}

/** A small grey label (size, «Verificada», progress). */
@Composable private fun Chip(text: String, verified: Boolean = false) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(StoreColors.Chip).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (verified) Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(14.dp).padding(end = 2.dp), tint = StoreColors.AccentText)
        Text(text, fontSize = 12.sp, color = if (verified) StoreColors.AccentText else StoreColors.Text, maxLines = 1, softWrap = false)
    }
}

@Composable private fun subtitle(row: AppRow, self: Boolean): String {
    val app = row.app
    val version = when {
        row.state == RowState.UPDATE -> stringResource(R.string.row_update, row.installed?.versionName.orEmpty(), app.versionName)
        row.state == RowState.NOT_INSTALLED -> stringResource(R.string.row_not_installed, app.versionName)
        self -> stringResource(R.string.row_self, row.installed?.versionName.orEmpty())
        else -> stringResource(R.string.row_up_to_date, row.installed?.versionName.orEmpty())
    }
    return listOf(app.category, version).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable private fun progressText(job: Job, size: Long): String = when (job.phase) {
    Phase.DOWNLOADING -> Formats.size(size).let { total ->
        if (total.isEmpty()) stringResource(R.string.downloading, Formats.percent(job.progress))
        else stringResource(R.string.downloading_of, Formats.percent(job.progress), total)
    }
    Phase.VERIFYING -> stringResource(R.string.verifying)
    Phase.INSTALLING -> stringResource(R.string.installing)
}
