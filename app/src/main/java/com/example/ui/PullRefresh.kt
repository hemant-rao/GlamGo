package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * §708 — wrap any scrollable screen body in pull-to-refresh.
 *
 * [onRefresh] kicks off the screen's (fire-and-forget) reloads; the screen's
 * StateFlows update the content independently, so the spinner just needs to
 * auto-dismiss after a short settle window. This keeps every screen's existing
 * structure untouched — we only add the gesture + indicator around it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NikhatPullRefresh(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            onRefresh()
            scope.launch {
                delay(1100)
                refreshing = false
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        content()
    }
}
