package com.zirakusa.tienda.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.tienda.R
import com.zirakusa.tienda.ui.CatalogError
import com.zirakusa.tienda.ui.StoreState
import com.zirakusa.tienda.ui.theme.StoreColors

/** «Apps»: the header with «Actualizar todo» and «Recargar», the notices, and one row per app (pull down to reload). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppsScreen(state: StoreState) {
    Column(Modifier.fillMaxSize().background(StoreColors.Background).safeDrawingPadding()) {
        Header(state)
        if (!state.canInstall) AllowCard(state::allowInstalls)
        state.catalogError?.let {
            Notice(stringResource(if (it == CatalogError.NETWORK) R.string.catalog_network else R.string.catalog_invalid))
        }
        state.failure?.let { Notice(failureText(it), onClose = { state.failure = null }) }
        PullToRefreshBox(isRefreshing = state.loading, onRefresh = state::refresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.rows.isEmpty()) item {
                    Text(stringResource(if (state.loading) R.string.loading else R.string.empty), color = StoreColors.Muted,
                        modifier = Modifier.padding(vertical = 24.dp))
                }
                items(state.rows, key = { it.app.packageName }) { row -> AppRowView(state, row) }
            }
        }
    }
}

@Composable private fun Header(state: StoreState) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.subtitle), color = StoreColors.Muted, fontSize = 14.sp)
        }
        IconButton(onClick = state::refresh, enabled = !state.loading) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.reload))
        }
    }
    val count = state.updates
    Box(Modifier.padding(horizontal = 16.dp)) {
        Button(onClick = state::updateAll, enabled = count > 0 && !state.busy, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StoreColors.Accent, contentColor = StoreColors.OnAccent,
                disabledContainerColor = StoreColors.Chip, disabledContentColor = StoreColors.Muted)) {
            Text(if (count > 0) stringResource(R.string.update_all, count) else stringResource(R.string.all_up_to_date),
                fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(4.dp))
}
