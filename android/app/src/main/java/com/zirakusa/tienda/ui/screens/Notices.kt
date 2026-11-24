package com.zirakusa.tienda.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.tienda.R
import com.zirakusa.tienda.data.Problem
import com.zirakusa.tienda.ui.FailedApp
import com.zirakusa.tienda.ui.theme.StoreColors

/** «Instalar apps desconocidas» is off: what it is and a button to Android's page for it. */
@Composable fun AllowCard(allow: () -> Unit) {
    Column(Modifier.padding(16.dp, 10.dp, 16.dp, 0.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(StoreColors.Card).padding(14.dp)) {
        Text(stringResource(R.string.allow_title), fontWeight = FontWeight.SemiBold, color = StoreColors.Warning.takeUnless {
            StoreColors.isLight } ?: StoreColors.Text)
        Text(stringResource(R.string.allow_text), fontSize = 14.sp, color = StoreColors.Muted, modifier = Modifier.padding(top = 4.dp))
        Button(onClick = allow, modifier = Modifier.padding(top = 8.dp), colors = ButtonDefaults.buttonColors(
            containerColor = StoreColors.Accent, contentColor = StoreColors.OnAccent)) { Text(stringResource(R.string.allow_button)) }
    }
}

/** A message on top of the list (catalog or install problem), with «Cerrar» when it can be dismissed. */
@Composable fun Notice(text: String, onClose: (() -> Unit)? = null) {
    Row(Modifier.padding(16.dp, 10.dp, 16.dp, 0.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(StoreColors.Card).padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 14.sp, color = StoreColors.Danger, modifier = Modifier.weight(1f))
        if (onClose != null) TextButton(onClick = onClose) { Text(stringResource(R.string.close), color = StoreColors.AccentText) }
    }
}

/** The clear message for each [Problem], with the app's name. */
@Composable fun failureText(failed: FailedApp): String {
    val name = failed.name
    return when (failed.failure.problem) {
        Problem.NETWORK -> stringResource(R.string.fail_network, name)
        Problem.BAD_HOST -> stringResource(R.string.fail_bad_host, name)
        Problem.TOO_BIG -> stringResource(R.string.fail_too_big, name)
        Problem.UNREADABLE -> stringResource(R.string.fail_unreadable, name)
        Problem.WRONG_PACKAGE -> stringResource(R.string.fail_wrong_package, name)
        Problem.WRONG_VERSION -> stringResource(R.string.fail_wrong_version, name)
        Problem.WRONG_SIGNATURE -> stringResource(R.string.fail_wrong_signature, name)
        Problem.NOT_ALLOWED -> stringResource(R.string.fail_not_allowed, name)
        Problem.CANCELLED -> stringResource(R.string.fail_cancelled, name)
        Problem.INSTALL_FAILED -> stringResource(R.string.fail_install, name,
            failed.failure.detail?.takeIf { it.isNotBlank() } ?: stringResource(R.string.fail_unknown))
    }
}
