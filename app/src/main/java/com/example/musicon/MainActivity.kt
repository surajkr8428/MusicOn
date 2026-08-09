package com.example.musicon

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicon.data.SettingsRepository
import com.example.musicon.service.PlaybackService
import com.example.musicon.ui.components.MiniPlayer
import com.example.musicon.ui.screens.LibraryScreen
import com.example.musicon.ui.screens.PlayerScreen
import com.example.musicon.ui.screens.SettingsScreen
import com.example.musicon.ui.theme.MusicOnTheme
import com.example.musicon.ui.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.common.util.concurrent.MoreExecutors
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settingsRepository = SettingsRepository(applicationContext)
        val database = com.example.musicon.data.local.MusicDatabase.getDatabase(applicationContext)
        val cloudManager = com.example.musicon.data.remote.CloudStorageManager(applicationContext)
        val musicRepository = com.example.musicon.data.MusicRepository(applicationContext, database.trackDao(), database.playlistDao(), cloudManager)
        
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return MainViewModel(settingsRepository, musicRepository) as T
                    }
                }
            )
            val themeMode by viewModel.themeMode.collectAsState()
            val keepScreenOn by viewModel.keepScreenOn.collectAsState()
            val currentTrack by viewModel.currentPlayingTrack
            val playbackQueue by viewModel.playbackQueue

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    viewModel.scanLocalStorage()
                }
            }

            SideEffect {
                val window = (this@MainActivity as android.app.Activity).window
                if (keepScreenOn) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            LaunchedEffect(playbackQueue) {
                val controller = mediaController ?: return@LaunchedEffect
                if (playbackQueue.isEmpty()) return@LaunchedEffect

                val mediaItems = playbackQueue.map { track ->
                    val metadata = MediaMetadata.Builder()
                        .setTitle(track.displayName)
                        .setArtist(track.displayArtist)
                        .setAlbumTitle(track.displayAlbum)
                        .setArtworkUri(track.customCoverPath?.let { Uri.parse("file://$it") })
                        .build()

                    val uri = if (track.localPath != null) {
                        Uri.parse("file://${track.localPath}")
                    } else if (track.gDriveId != null) {
                        Uri.parse("https://www.googleapis.com/drive/v3/files/${track.gDriveId}?alt=media")
                    } else {
                        Uri.EMPTY
                    }

                    MediaItem.Builder()
                        .setUri(uri)
                        .setMediaId(track.id)
                        .setMediaMetadata(metadata)
                        .build()
                }

                controller.setMediaItems(mediaItems)
                
                currentTrack?.let { target ->
                    val index = playbackQueue.indexOfFirst { it.id == target.id }
                    if (index != -1) {
                        controller.seekTo(index, 0L)
                    }
                }
                
                controller.prepare()
                controller.play()
            }

            val signInLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    viewModel.updateSignInStatus(true)
                }
            }

            val triggerSignIn = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_READONLY))
                    .build()
                val client = GoogleSignIn.getClient(this, gso)
                signInLauncher.launch(client.signInIntent)
            }
            
            LaunchedEffect(Unit) {
                if (GoogleSignIn.getLastSignedInAccount(this@MainActivity) != null) {
                    viewModel.updateSignInStatus(true)
                }
            }

            MusicOnTheme(themeMode = themeMode) {
                MusicOnApp(
                    viewModel = viewModel,
                    mediaController = mediaController,
                    onSignInClick = triggerSignIn,
                    onScanClick = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        
                        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.scanLocalStorage()
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
    }
}

@Composable
fun MusicOnApp(
    viewModel: MainViewModel,
    mediaController: Player?,
    onSignInClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var isPlayerVisible by rememberSaveable { mutableStateOf(false) }
    var isSettingsVisible by rememberSaveable { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.importLocalTracks(uris)
    }

    if (isPlayerVisible) {
        PlayerScreen(
            viewModel = viewModel,
            player = mediaController,
            onBack = { isPlayerVisible = false }
        )
    } else if (isSettingsVisible) {
        SettingsScreen(
            viewModel = viewModel,
            onSignInClick = onSignInClick,
            onScanClick = onScanClick,
            onBack = { isSettingsVisible = false }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF0D0B1F),
                    modifier = Modifier.width(300.dp)
                ) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        "MusicOn",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    NavigationDrawerItem(
                        label = { Text("Import Hub (Local)") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            filePicker.launch("audio/*")
                        },
                        icon = { Icon(Icons.Default.FileDownload, null) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                    )
                    NavigationDrawerItem(
                        label = { Text("Sync Google Drive") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSignInClick()
                        },
                        icon = { Icon(Icons.Default.CloudSync, null) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                    )
                    NavigationDrawerItem(
                        label = { Text("Scan Local Music") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onScanClick()
                        },
                        icon = { Icon(Icons.Default.Scanner, null) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                    )
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            isSettingsVisible = true
                        },
                        icon = { Icon(Icons.Default.Settings, null) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    MiniPlayer(
                        onNavigateToPlayer = { isPlayerVisible = true },
                        player = mediaController
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    LibraryScreen(
                        viewModel = viewModel,
                        onOpenSettings = { isSettingsVisible = true },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}
