package com.example.musicon

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicon.data.SettingsRepository
import com.example.musicon.service.PlaybackService
import com.example.musicon.ui.components.MiniPlayer
import com.example.musicon.ui.screens.*
import com.example.musicon.ui.theme.MusicOnTheme
import com.example.musicon.ui.viewmodel.MainViewModel
import com.example.musicon.ui.viewmodel.PlaybackEvent
import com.example.musicon.logic.MediaMetadataUtils
import com.example.musicon.logic.ShakeDetector
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.common.util.concurrent.MoreExecutors
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? by mutableStateOf(null)
    private var shakeDetector: ShakeDetector? = null

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
            val accentColorInt by viewModel.accentColor.collectAsState()
            val autoTheme by viewModel.autoTheme.collectAsState()
            val extractedColor by viewModel.extractedAccentColor.collectAsState()
            
            val accentColor = remember(accentColorInt, autoTheme, extractedColor) {
                if (autoTheme && extractedColor != null) Color(extractedColor!!) else Color(accentColorInt)
            }
            val keepScreenOn by viewModel.keepScreenOn.collectAsState()
            val shakeToSkip by viewModel.shakeToSkip.collectAsState()
            val playbackQueue by viewModel.playbackQueue.collectAsState()
            val currentTrack by viewModel.currentPlayingTrack.collectAsState()
            val customBgUri by settingsRepository.customBgUriFlow.collectAsState(null)

            // Dynamic Theming: Extract color from artwork
            val context = LocalContext.current
            LaunchedEffect(currentTrack, autoTheme) {
                if (!autoTheme) {
                    viewModel.updateExtractedColor(null)
                    return@LaunchedEffect
                }
                
                val track = currentTrack ?: return@LaunchedEffect
                val path = track.customCoverPath ?: track.localPath
                if (path != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val bitmap = if (path.startsWith("content://")) {
                                context.contentResolver.openInputStream(Uri.parse(path))?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            } else {
                                BitmapFactory.decodeFile(path)
                            }
                            
                            bitmap?.let {
                                val palette = Palette.from(it).generate()
                                val color = palette.getVibrantColor(palette.getDominantColor(accentColorInt))
                                viewModel.updateExtractedColor(color)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MusicOn", "Palette extraction failed", e)
                        }
                    }
                } else {
                    viewModel.updateExtractedColor(null)
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    viewModel.scanLocalStorage()
                }
            }

            // Sync Service -> UI (ONE WAY)
            DisposableEffect(mediaController) {
                val controller = mediaController ?: return@DisposableEffect onDispose {}
                val listener = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.let { item ->
                            val track = playbackQueue.find { it.id == item.mediaId }
                            if (track != null && viewModel.currentPlayingTrack.value?.id != track.id) {
                                viewModel.updateCurrentTrack(track)
                            }
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("MusicOn", "Player Error: ${error.errorCodeName}", error)
                    }
                }
                controller.addListener(listener)
                onDispose { controller.removeListener(listener) }
            }

            // UI Actions -> Service (ONE WAY)
            LaunchedEffect(mediaController) {
                val controller = mediaController ?: return@LaunchedEffect
                viewModel.playbackEvents.collect { event ->
                    when (event) {
                        is PlaybackEvent.PlayTrackList -> {
                            val items = event.tracks.map { it.toMediaItem() }
                            controller.setMediaItems(items, event.startIndex, 0L)
                            controller.prepare()
                            controller.play()
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                viewModel.playbackCommand.collect { command ->
                    if (command == MainViewModel.PlaybackCommand.PAUSE) {
                        mediaController?.pause()
                    }
                }
            }

            LaunchedEffect(shakeToSkip, mediaController) {
                val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                if (shakeToSkip && mediaController != null) {
                    shakeDetector = ShakeDetector {
                        mediaController?.seekToNext()
                    }
                    sensorManager.registerListener(shakeDetector, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
                } else {
                    shakeDetector?.let { sensorManager.unregisterListener(it) }
                    shakeDetector = null
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

            // Initial Sync / State Restoration
            var isInitialSyncDone by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(mediaController, playbackQueue) {
                val controller = mediaController ?: return@LaunchedEffect
                if (isInitialSyncDone || playbackQueue.isEmpty()) return@LaunchedEffect

                val controllerTrackId = controller.currentMediaItem?.mediaId
                if (controllerTrackId != null) {
                    // Service is ALREADY active. UI must follow it.
                    val track = playbackQueue.find { it.id == controllerTrackId }
                    if (track != null) {
                        viewModel.updateCurrentTrack(track)
                    }
                } else {
                    // Service is IDLE. Attempt resume from storage.
                    val lastId = settingsRepository.lastTrackIdFlow.first()
                    if (lastId != null) {
                        val index = playbackQueue.indexOfFirst { it.id == lastId }
                        if (index != -1) {
                            val lastPos = settingsRepository.lastPositionFlow.first()
                            controller.setMediaItems(playbackQueue.map { it.toMediaItem() }, index, lastPos)
                            controller.prepare()
                        }
                    } else {
                        controller.setMediaItems(playbackQueue.map { it.toMediaItem() })
                        controller.prepare()
                    }
                }
                isInitialSyncDone = true
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
            
            val triggerSignOut = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                val client = GoogleSignIn.getClient(this, gso)
                client.signOut().addOnCompleteListener {
                    viewModel.updateSignInStatus(false)
                }
            }
            
            LaunchedEffect(Unit) {
                if (GoogleSignIn.getLastSignedInAccount(this@MainActivity) != null) {
                    viewModel.updateSignInStatus(true)
                }
            }

            CompositionLocalProvider(com.example.musicon.ui.components.LocalCustomBackground provides customBgUri) {
                MusicOnTheme(themeMode = themeMode, accentColor = accentColor) {
                    // Startup Animation
                    var showApp by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(200)
                        showApp = true
                    }

                    AnimatedVisibility(
                        visible = showApp,
                        enter = fadeIn(tween(1000, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.94f, animationSpec = tween(1000, easing = LinearOutSlowInEasing)),
                        exit = fadeOut()
                    ) {
                        MusicOnApp(
                            viewModel = viewModel,
                            mediaController = mediaController,
                            onSignInClick = triggerSignIn,
                            onSignOutClick = { triggerSignOut() },
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
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
            } catch (e: Exception) {
                android.util.Log.e("MusicOn", "Failed to connect to MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
        shakeDetector?.let { (getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(it) }
    }
}

private fun com.example.musicon.data.local.TrackEntity.toMediaItem(): MediaItem {
    val artworkUri = if (customCoverPath != null && File(customCoverPath).exists()) {
        Uri.fromFile(File(customCoverPath))
    } else if (localPath?.startsWith("content://") == true) {
        Uri.parse(localPath)
    } else if (localPath != null) {
        Uri.fromFile(File(localPath))
    } else {
        null
    }
    
    val contentUri = if (localPath != null) {
        if (localPath.startsWith("content://") || localPath.startsWith("file://")) {
            Uri.parse(localPath)
        } else {
            Uri.fromFile(File(localPath))
        }
    } else if (gDriveId != null) {
        Uri.parse("https://www.googleapis.com/drive/v3/files/$gDriveId?alt=media")
    } else {
        Uri.EMPTY
    }

    return MediaItem.Builder()
        .setUri(contentUri)
        .setMediaId(id)
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle(displayName)
            .setArtist(displayArtist)
            .setArtworkUri(artworkUri)
            .build())
        .build()
}

@Composable
fun MusicOnApp(
    viewModel: MainViewModel,
    mediaController: Player?,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isUserSignedIn by viewModel.isUserSignedIn
    val context = LocalContext.current
    
    var isPlayerVisible by rememberSaveable { mutableStateOf(false) }
    var isSettingsVisible by rememberSaveable { mutableStateOf(false) }
    var isEqualizerVisible by rememberSaveable { mutableStateOf(false) }
    var cutterTrack by remember { mutableStateOf<com.example.musicon.data.local.TrackEntity?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.addCustomFolder(it.toString()) }
    }

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
    } else if (isEqualizerVisible) {
        EqualizerScreen(
            viewModel = viewModel,
            onBack = { isEqualizerVisible = false }
        )
    } else if (cutterTrack != null) {
        Mp3CutterScreen(
            track = cutterTrack!!,
            onBack = { cutterTrack = null }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF0D0B1F),
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(48.dp))
                        
                        // Sign-in at the top
                        if (!isUserSignedIn) {
                            Button(
                                onClick = onSignInClick,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.CloudSync, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Sign in with Google", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        account?.email ?: "Signed in",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Cloud Sync Enabled",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                IconButton(onClick = onSignOutClick) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, "Sign Out", tint = Color.Red)
                                }
                            }
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        
                        // Branded Title
                        val primaryColor = MaterialTheme.colorScheme.primary
                        Text(
                            "MusicOn",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        
                        NavigationDrawerItem(
                            label = { Text("Import Hub (Local)") },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; filePicker.launch("audio/*") },
                            icon = { Icon(Icons.Default.FileDownload, null) },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                        )
                        NavigationDrawerItem(
                            label = { Text("Scan Local Music") },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; onScanClick() },
                            icon = { Icon(Icons.Default.Scanner, null) },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                        )
                        NavigationDrawerItem(
                            label = { Text("Add Folder Tab") },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; folderPicker.launch(null) },
                            icon = { Icon(Icons.Default.CreateNewFolder, null) },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                        )
                        NavigationDrawerItem(
                            label = { Text("Equalizer") },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; isEqualizerVisible = true },
                            icon = { Icon(Icons.Default.Tune, null) },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                        )
                        NavigationDrawerItem(
                            label = { Text("Settings") },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; isSettingsVisible = true },
                            icon = { Icon(Icons.Default.Settings, null) },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                        )
                        NavigationDrawerItem(
                            label = { Text("Share App (APK)") },
                            selected = false,
                            onClick = { 
                                scope.launch { 
                                    drawerState.close()
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val sourceFile = File(context.packageResourcePath)
                                            val shareDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                                            val destFile = File(shareDir, "MusicOn.apk")
                                            sourceFile.copyTo(destFile, overwrite = true)
                                            
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                destFile
                                            )
                                            
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/vnd.android.package-archive"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            val chooser = android.content.Intent.createChooser(intent, "Share MusicOn APK")
                                            chooser.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            context.startActivity(chooser)
                                        } catch (e: Exception) {
                                            android.util.Log.e("MusicOn", "Failed to share APK", e)
                                        }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Default.Share, null) },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White)
                        )
                        
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    MiniPlayer(
                        onNavigateToPlayer = { isPlayerVisible = true },
                        player = mediaController,
                        viewModel = viewModel
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    LibraryScreen(
                        viewModel = viewModel,
                        onOpenSettings = { isSettingsVisible = true },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenCutter = { cutterTrack = it },
                        onOpenPlayer = { isPlayerVisible = true }
                    )
                }
            }
        }
    }
}
