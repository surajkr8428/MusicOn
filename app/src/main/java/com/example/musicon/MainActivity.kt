package com.example.musicon

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.musicon.data.remote.CloudStorageManager
import com.example.musicon.ui.viewmodel.PlaybackEvent
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import coil.compose.LocalImageLoader
import com.example.musicon.data.LibraryViewMode
import com.example.musicon.data.MusicRepository
import com.example.musicon.data.SettingsRepository
import com.example.musicon.data.local.MusicDatabase
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.ui.theme.ThemeMode
import com.example.musicon.data.remote.CloudSyncManager
import com.example.musicon.ui.components.MiniPlayer
import com.example.musicon.ui.components.RenameDialog
import com.example.musicon.ui.screens.*
import com.example.musicon.ui.theme.MusicOnTheme
import com.example.musicon.ui.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.musicon.ui.components.HeaderStatusPill
import com.example.musicon.ui.components.SyncProgressBar
import com.example.musicon.data.remote.SyncStatus
import com.example.musicon.ui.components.StellarBackground
import com.example.musicon.ui.components.LocalIsBackgroundBright
import com.example.musicon.ui.screens.PlayerScreen
import com.example.musicon.ui.screens.EqualizerScreen
import com.example.musicon.ui.screens.Mp3CutterScreen
import com.example.musicon.ui.screens.SettingsScreen
import com.example.musicon.ui.screens.LibraryScreen
import com.example.musicon.ui.screens.CloudBrowserScreen
import com.example.musicon.service.PlaybackService
import com.example.musicon.ui.components.CircularSyncProgressBar
import com.example.musicon.ui.components.CreatePlaylistDialog
import com.example.musicon.ui.components.LocalCustomBackground
import com.example.musicon.ui.components.LocalIsBackgroundBright
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? by mutableStateOf(null)
    
    private var onSignInResult: ((GoogleSignInAccount?) -> Unit)? = null

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            onSignInResult?.invoke(account)
        } catch (e: Exception) {
            Log.e("MusicOn", "Sign-in failed", e)
            onSignInResult?.invoke(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settingsRepository = SettingsRepository(applicationContext)
        val database = MusicDatabase.getDatabase(applicationContext)
        val cloudManager = CloudStorageManager(applicationContext)
        val musicRepository = MusicRepository(applicationContext, database.trackDao(), database.playlistDao(), cloudManager)
        
        val account = GoogleSignIn.getLastSignedInAccount(this)

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return MainViewModel(settingsRepository, musicRepository) as T
                    }
                }
            )

            LaunchedEffect(Unit) {
                if (account != null) viewModel.updateSignInStatus(true, account.email)
                viewModel.scanLocalStorage()
                intent?.let { handleIntent(it, viewModel) }
            }

            val themeMode by viewModel.themeMode.collectAsState()
            val accentColorInt by viewModel.accentColor.collectAsState()
            val autoTheme by viewModel.autoTheme.collectAsState()
            val extractedColor by viewModel.extractedAccentColor.collectAsState()
            
            val accentColor = remember(accentColorInt, autoTheme, extractedColor) {
                if (autoTheme && extractedColor != null) Color(extractedColor!!) else Color(accentColorInt)
            }
            val currentTrack by viewModel.currentPlayingTrack.collectAsState()
            val customBgUri by settingsRepository.customBgUriFlow.collectAsState(null)
            val isOnline by viewModel.isOnline.collectAsState()

            DisposableEffect(Unit) {
                val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        val capabilities = cm.getNetworkCapabilities(network)
                        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                        viewModel.updateOnlineStatus(true, isWifi)
                    }
                    override fun onLost(network: Network) { viewModel.updateOnlineStatus(false) }
                }
                cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
                onDispose { cm.unregisterNetworkCallback(callback) }
            }

            LaunchedEffect(Unit) {
                viewModel.playbackCommand.collect { command ->
                    when (command) {
                        MainViewModel.PlaybackCommand.CLOSE_APP -> {
                            mediaController?.stop()
                            finishAffinity()
                        }
                        MainViewModel.PlaybackCommand.STOP_PLAYBACK -> {
                            mediaController?.pause()
                            mediaController?.stop()
                        }
                        else -> {}
                    }
                }
            }

            LaunchedEffect(mediaController) {
                val controller = mediaController ?: return@LaunchedEffect
                
                controller.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("MusicOn", "Player Error: ${error.errorCodeName}", error)
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val id = mediaItem?.mediaId
                        if (id != null) {
                            viewModel.updateCurrentTrackById(id)
                        }
                    }
                })

                if (controller.mediaItemCount == 0) {
                    launch {
                        val startupData = combine(
                            viewModel.playbackQueue,
                            viewModel.lastPosition,
                            viewModel.currentPlayingTrack
                        ) { queue, pos, track ->
                            if (queue.isNotEmpty() && track != null) Triple(queue, pos, track) else null
                        }.filterNotNull().first()
                        
                        val initialTracks = startupData.first
                        val lastPos = startupData.second
                        val currentTrack = startupData.third
                        
                        val mediaItems = initialTracks.map { it.toMediaItem() }
                        val startIndex = initialTracks.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0)
                        
                        controller.setMediaItems(mediaItems)
                        controller.seekTo(startIndex, lastPos)
                        controller.prepare()
                    }
                }

                viewModel.playbackEvents.collect { event ->
                    try {
                        when (event) {
                            is PlaybackEvent.PlayTrackList -> {
                                val mediaItems = event.tracks.map { it.toMediaItem() }
                                controller.setMediaItems(mediaItems)
                                controller.seekTo(event.startIndex, 0L)
                                controller.prepare()
                                controller.play()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicOn", "Playback failed", e)
                    }
                }
            }

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
                            } else if (path.startsWith("http")) {
                                null
                            } else {
                                BitmapFactory.decodeFile(path)
                            }
                            bitmap?.let {
                                val palette = Palette.from(it).generate()
                                val color = palette.getVibrantColor(palette.getDominantColor(accentColorInt))
                                viewModel.updateExtractedColor(color)
                            }
                        } catch (e: Exception) {
                            Log.e("MusicOn", "Palette extraction failed", e)
                        }
                    }
                } else {
                    viewModel.updateExtractedColor(null)
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) viewModel.startRealTimeSync()
            }
            
            LaunchedEffect(Unit) {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.startRealTimeSync()
                } else {
                    permissionLauncher.launch(permission)
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val lifecycle = lifecycleOwner.lifecycle
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.syncCloudTracks()
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            val imageLoader = remember {
                ImageLoader.Builder(applicationContext)
                    .crossfade(true)
                    .build()
            }

            CompositionLocalProvider(
                LocalCustomBackground provides customBgUri,
                LocalIsBackgroundBright provides (themeMode == ThemeMode.LIGHT),
                LocalImageLoader provides imageLoader
            ) {
                MusicOnTheme(themeMode = themeMode, accentColor = accentColor) {
                    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
                    var showApp by remember { mutableStateOf(!isFirstLaunch) }
                    LaunchedEffect(Unit) {
                        if (isFirstLaunch) {
                            delay(200)
                            showApp = true
                            isFirstLaunch = false
                        }
                    }

                    if (showApp) {
                        AnimatedVisibility(visible = true, enter = fadeIn(tween(1000)) + scaleIn(initialScale = 0.96f)) {
                            MusicOnApp(
                                viewModel = viewModel,
                                mediaController = mediaController,
                                isOnline = isOnline,
                                onSignInClick = { 
                                    onSignInResult = { acc -> if (acc != null) viewModel.updateSignInStatus(true, acc.email) }
                                    triggerSignIn() 
                                },
                                onSignOutClick = { triggerSignOut(); viewModel.updateSignInStatus(false, null) }
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize().background(Color(0xFF0D0B1F)), contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_nirvaana_logo),
                                contentDescription = null,
                                modifier = Modifier.size(140.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent, viewModel: MainViewModel) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri: Uri ->
                viewModel.playExternalFile(uri)
            }
        }
    }

    private fun triggerSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("https://www.googleapis.com/auth/drive.readonly")
            )
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        signInLauncher.launch(client.signInIntent)
    }

    private fun triggerSignOut() {
        val client = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
        client.signOut()
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
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onStop()
    }
}

private fun TrackEntity.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(displayName)
        .setArtist(displayArtist)
        .setAlbumTitle(displayAlbum)
        .setArtworkUri(customCoverPath?.let { Uri.parse(it) } ?: localPath?.let { Uri.parse(it) })
        .build()

    val uri = if (localPath != null) {
        if (localPath.startsWith("content://")) Uri.parse(localPath)
        else Uri.fromFile(File(localPath))
    } else if (gDriveId != null) {
        Uri.parse("https://www.googleapis.com/drive/v3/files/$gDriveId?alt=media")
    } else null

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

@Composable
fun MusicOnApp(
    viewModel: MainViewModel,
    mediaController: Player?,
    isOnline: Boolean,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val backgroundMode by viewModel.backgroundMode.collectAsState()

    StellarBackground(themeMode = themeMode, backgroundMode = backgroundMode) {
        val scope = rememberCoroutineScope()
        val isUserSignedIn by viewModel.isUserSignedIn
        val context = LocalContext.current
        val syncStatus by CloudSyncManager.status.collectAsState()
        
        var isPlayerVisible by rememberSaveable { mutableStateOf(false) }
        var isEqualizerVisible by rememberSaveable { mutableStateOf(false) }
        var isCloudBrowserVisible by rememberSaveable { mutableStateOf(false) }
        var cutterTrack by remember { mutableStateOf<TrackEntity?>(null) }
        var showSignInPrompt by remember { mutableStateOf(false) }
        var showCreatePlaylistDialog by remember { mutableStateOf(false) }

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            viewModel.importLocalTracks(uris)
        }

        if (isPlayerVisible) {
            PlayerScreen(viewModel = viewModel, player = mediaController, onBack = { isPlayerVisible = false })
        } else if (isEqualizerVisible) {
            EqualizerScreen(viewModel = viewModel, onBack = { isEqualizerVisible = false })
        } else if (isCloudBrowserVisible) {
            CloudBrowserScreen(viewModel = viewModel, onBack = { isCloudBrowserVisible = false })
        } else if (cutterTrack != null) {
            Mp3CutterScreen(track = cutterTrack!!, viewModel = viewModel, onBack = { cutterTrack = null })
        } else {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val adaptiveWidth = if (screenWidth > 600.dp) 400.dp else screenWidth * 0.85f

            val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            ModalNavigationDrawer(
                drawerState = leftDrawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        modifier = Modifier.width(adaptiveWidth)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.height(48.dp))
                            if (!isUserSignedIn) {
                                Button(onClick = onSignInClick, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Icon(Icons.Default.CloudSync, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sign in with Google", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                val account = GoogleSignIn.getLastSignedInAccount(context)
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    val isBright = LocalIsBackgroundBright.current
                                    val emailColor = if (isBright) Color.Black else Color.White

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            account?.email ?: "Signed in", 
                                            color = emailColor, 
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold, 
                                                fontSize = 14.sp
                                            ), 
                                            maxLines = 1, 
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("Cloud Sync Enabled", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = onSignOutClick) { Icon(Icons.AutoMirrored.Filled.Logout, "Sign Out", tint = Color.Red, modifier = Modifier.size(20.dp)) }
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val localCount by viewModel.localTracksCount.collectAsState()
                            val cloudCount by viewModel.cloudTracksCount.collectAsState()
                            
                            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)) {
                                Text(
                                    "NIRVAANA STORAGE", 
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = primaryColor, 
                                        fontWeight = FontWeight.Black,
                                        fontSize = 26.sp
                                    ),
                                    lineHeight = 30.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Smartphone, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Local Songs: $localCount", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = Color.White)
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudQueue, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Cloud Songs: $cloudCount", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = Color.White)
                                    Spacer(Modifier.weight(1f))
                                    
                                    val isSyncing = syncStatus is SyncStatus.Loading
                                    val isPaused = syncStatus is SyncStatus.Paused
                                    
                                    IconButton(
                                        onClick = { 
                                            if (!isUserSignedIn) showSignInPrompt = true 
                                            else if (isSyncing) viewModel.pauseSync()
                                            else if (isPaused) viewModel.resumeSync()
                                            else viewModel.syncAllLocalToCloud() 
                                        },
                                        modifier = Modifier.size(32.dp).background(primaryColor.copy(alpha = 0.1f), CircleShape)
                                    ) { 
                                        Icon(
                                            imageVector = if (isSyncing) Icons.Default.Pause else if (isPaused) Icons.Default.PlayArrow else Icons.Default.Sync, 
                                            contentDescription = "Sync Toggle", 
                                            tint = primaryColor, 
                                            modifier = Modifier.size(18.dp)
                                        ) 
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            
                            // Compact Sidebar Items
                            val sidePadding = Modifier.padding(horizontal = 12.dp)
                            
                            NavigationDrawerItem(
                                label = { Text("Import Hub (Local)", fontSize = 14.sp) }, 
                                selected = false, 
                                onClick = { scope.launch { leftDrawerState.close() }; filePicker.launch("audio/*") }, 
                                icon = { Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(20.dp)) }, 
                                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                                modifier = sidePadding.height(40.dp)
                            )
                            NavigationDrawerItem(
                                label = { Text("Cloud Browser", fontSize = 14.sp) }, 
                                selected = false, 
                                onClick = { if (!isUserSignedIn) showSignInPrompt = true else { scope.launch { leftDrawerState.close() }; isCloudBrowserVisible = true } }, 
                                icon = { Icon(Icons.Default.CloudQueue, null, modifier = Modifier.size(20.dp)) }, 
                                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                                modifier = sidePadding.height(40.dp)
                            )
                            NavigationDrawerItem(
                                label = { Text("Equalizer", fontSize = 14.sp) }, 
                                selected = false, 
                                onClick = { scope.launch { leftDrawerState.close() }; isEqualizerVisible = true }, 
                                icon = { Icon(Icons.Default.Tune, null, modifier = Modifier.size(20.dp)) }, 
                                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                                modifier = sidePadding.height(40.dp)
                            )
                            NavigationDrawerItem(
                                label = { Text("Share App (APK)", fontSize = 14.sp) }, 
                                selected = false, 
                                onClick = { scope.launch { leftDrawerState.close() }; viewModel.shareAppApk() }, 
                                icon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp)) }, 
                                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                                modifier = sidePadding.height(40.dp)
                            )
                            
                            Spacer(Modifier.height(32.dp))

                            CircularSyncProgressBar(
                                syncStatus = syncStatus,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                onSyncClick = {
                                    val isSyncingNow = syncStatus is SyncStatus.Loading
                                    val isPausedNow = syncStatus is SyncStatus.Paused
                                    
                                    if (!isUserSignedIn) showSignInPrompt = true 
                                    else if (isSyncingNow) viewModel.pauseSync()
                                    else if (isPausedNow) viewModel.resumeSync()
                                    else viewModel.syncAllLocalToCloud() 
                                }
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                },
                content = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        ModalNavigationDrawer(
                            drawerState = rightDrawerState,
                            drawerContent = {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f), modifier = Modifier.width(adaptiveWidth).fillMaxHeight()) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { scope.launch { rightDrawerState.close() } }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface) }
                                            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        SettingsScreen(viewModel = viewModel, onSignInClick = onSignInClick, onScanClick = { viewModel.scanLocalStorage() }, onBack = { scope.launch { rightDrawerState.close() } })
                                    }
                                }
                            },
                            content = {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Scaffold(
                                        modifier = Modifier.fillMaxSize(),
                                        containerColor = Color.Transparent,
                                        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Let TopAppBars handle insets
                                        bottomBar = { 
                                            MiniPlayer(
                                                onNavigateToPlayer = { isPlayerVisible = true }, 
                                                player = mediaController, 
                                                viewModel = viewModel,
                                                isLeftMenuOpen = leftDrawerState.isOpen,
                                                isRightSidebarOpen = rightDrawerState.isOpen
                                            ) 
                                        }
                                    ) { innerPadding ->
                                        Box(modifier = Modifier.padding(innerPadding)) {
                                            LibraryScreen(
                                                viewModel = viewModel,
                                                onOpenSettings = { scope.launch { rightDrawerState.open() } },
                                                onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                onOpenCutter = { cutterTrack = it },
                                                onOpenPlayer = { isPlayerVisible = true }
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
        if (showSignInPrompt) AlertDialog(onDismissRequest = { showSignInPrompt = false }, title = { Text("Sign in Required") }, text = { Text("Please sign in with Google to use cloud features.") }, confirmButton = { Button(onClick = { showSignInPrompt = false; onSignInClick() }) { Text("Sign In") } }, dismissButton = { TextButton(onClick = { showSignInPrompt = false }) { Text("Cancel") } })
        if (showCreatePlaylistDialog) CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false }, onConfirm = { viewModel.createPlaylist(it); showCreatePlaylistDialog = false })
    }
}
