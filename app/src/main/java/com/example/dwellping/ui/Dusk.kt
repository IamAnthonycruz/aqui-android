package com.example.dwellping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dwellping.ui.theme.DuskBottom
import com.example.dwellping.ui.theme.DuskTop
import com.example.dwellping.ui.theme.Mist

/** The Dusk gradient behind every screen. */
fun Modifier.duskBackground() = background(Brush.verticalGradient(listOf(DuskTop, DuskBottom)))

/**
 * A sub-screen on the Dusk gradient: back arrow, title, scrolling column of [DuskPanel]s.
 * [bottomBar] stays pinned at the bottom, above the navigation bar and the keyboard, while the column
 * scrolls. Use it for the screen's main action, so it's reachable on any screen size.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuskScaffold(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().duskBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            },
            bottomBar = {
                if (bottomBar != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            // union, not both paddings: the keyboard's inset already covers the nav bar.
                            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) { bottomBar() }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                content = content,
            )
        }
    }
}

/** The see-through rounded panel used on every screen (same as the Start screen's people list). */
@Composable
fun DuskPanel(
    modifier: Modifier = Modifier,
    padding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Mist.copy(alpha = 0.08f))
            .then(if (padding) Modifier.padding(16.dp) else Modifier.padding(vertical = 6.dp)),
        verticalArrangement = Arrangement.spacedBy(if (padding) 10.dp else 0.dp),
        content = content,
    )
}
