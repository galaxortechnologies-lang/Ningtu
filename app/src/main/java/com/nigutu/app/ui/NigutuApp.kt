package com.nigutu.app.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.nigutu.app.R
import com.nigutu.app.designsystem.MiniPlayer
import com.nigutu.app.designsystem.NigutuTheme
import com.nigutu.app.navigation.Destination
import com.nigutu.app.navigation.NigutuNavHost

/**
 * Root composable: installs the theme, asks for the minimum permissions the
 * music player needs, and wires the bottom bar plus mini player.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NigutuApp(appViewModel: AppViewModel = hiltViewModel()) {
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val playbackState by appViewModel.playbackState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissions = buildList {
        add(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)
    val hasAudioAccess = permissionState.permissions.firstOrNull()?.status?.isGranted == true

    LaunchedEffect(Unit) {
        if (permissionState.permissions.none { it.status.isGranted }) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    NigutuTheme(themeMode = settings.themeMode) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showChrome = currentRoute !in setOf(Destination.Player.route, Destination.Lyrics.route)

        val destinations = listOf(
            Destination.Home,
            Destination.Explore,
            Destination.Search,
            Destination.Library,
            Destination.Settings,
        )
        val icons = mapOf<Destination, ImageVector>(
            Destination.Home to Icons.Rounded.Home,
            Destination.Explore to Icons.Rounded.Explore,
            Destination.Search to Icons.Rounded.Search,
            Destination.Library to Icons.Rounded.LibraryMusic,
            Destination.Settings to Icons.Rounded.Person,
        )
        val labels = mapOf(
            Destination.Home to stringResource(R.string.nav_home),
            Destination.Explore to stringResource(R.string.nav_explore),
            Destination.Search to stringResource(R.string.nav_search),
            Destination.Library to stringResource(R.string.nav_library),
            Destination.Settings to stringResource(R.string.nav_settings),
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showChrome) {
                    Column {
                        playbackState.currentSong?.let { song ->
                            MiniPlayer(
                                song = song,
                                isPlaying = playbackState.isPlaying,
                                progress = playbackState.progress,
                                onOpen = { navController.navigate(Destination.Player.route) },
                                onTogglePlay = appViewModel::togglePlayPause,
                            )
                        }
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                            destinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = icons.getValue(destination),
                                            contentDescription = labels[destination],
                                        )
                                    },
                                    label = { Text(labels[destination].orEmpty()) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            NigutuNavHost(
                navController = navController,
                hasAudioAccess = hasAudioAccess,
                onRequestAudioAccess = { permissionState.launchMultiplePermissionRequest() },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
