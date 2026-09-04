package com.example.musicon

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicon.ui.viewmodel.PlaybackEvent
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.palette.graphics.Palette
import com.example.musicon.data.LibraryViewMode
import com.example.musicon.data.SettingsRepository
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.data.remote.CloudSyncManager
import com.example.musicon.data.remote.SyncStatus
import com.example.musicon.ui.components.MiniPlayer
import com.example.musicon.ui.screens.*
import com.example.musicon.ui.theme.MusicOnTheme
import com.example.musicon.ui.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? by mutableStateOf(null)
    
    private var onSignInResult: ((Boolean) -> Unit)? = null

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onSignInResult?.invoke(result.resultCode == RESULT_OK)
    }

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
            val currentTrack by viewModel.currentPlayingTrack.collectAsState()
            val customBgUri by settingsRepository.customBgUriFlow.collectAsState(null)
            val isOnline by viewModel.isOnline.collectAsState()

            // Connectivity Monitor
            DisposableEffect(Unit) {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        val capabilities = cm.getNetworkCapabilities(network)
                        val isWifi = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
                        viewModel.updateOnlineStatus(true, isWifi)
                    }
                    override fun onLost(network: Network) { viewModel.updateOnlineStatus(false) }
                }
                cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
                onDispose { cm.unregisterNetworkCallback(callback) }
            }

            // Playback Commands
            LaunchedEffect(Unit) {
                viewModel.playbackCommand.collect { command ->
                    when (command) {
                        MainViewModel.PlaybackCommand.CLOSE_APP -> {
                            mediaController?.pause()
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

            // Handle Playback Events (Fix for song not playing)
            LaunchedEffect(mediaController) {
                viewModel.playbackEvents.collect { event ->
                    when (event) {
                        is PlaybackEvent.PlayTrackList -> {
                            val mediaItems = event.tracks.map { it.toMediaItem() }
                            mediaController?.apply {
                                setMediaItems(mediaItems)
                                seekTo(event.startIndex, 0L)
                                prepare()
                                play()
                            }
                        }
                    }
                }
            }

            // Dynamic Theming
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
                                context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use {
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

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.syncCloudTracks()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            CompositionLocalProvider(com.example.musicon.ui.components.LocalCustomBackground provides customBgUri) {
                MusicOnTheme(themeMode = themeMode, accentColor = accentColor) {
                    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
                    var showApp by remember { mutableStateOf(!isFirstLaunch) }
                    LaunchedEffect(Unit) {
                        if (isFirstLaunch) {
                            kotlinx.coroutines.delay(200)
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
                                    onSignInResult = { success -> 
                                        if (success) viewModel.updateSignInStatus(true) 
                                    }
                                    triggerSignIn() 
                                },
                                onSignOutClick = { triggerSignOut(); viewModel.updateSignInStatus(false) }
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize().background(Color(0xFF0D0B1F)))
                    }
                }
            }
        }
    }

    private fun triggerSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"), com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.readonly"))
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
        val sessionToken = androidx.media3.session.SessionToken(this, android.content.ComponentName(this, com.example.musicon.service.PlaybackService::class.java))
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

private fun TrackEntity.toMediaItem(): androidx.media3.common.MediaItem {
    val metadata = androidx.media3.common.MediaMetadata.Builder()
        .setTitle(displayName)
        .setArtist(displayArtist)
        .setAlbumTitle(displayAlbum)
        .setArtworkUri(customCoverPath?.let { android.net.Uri.fromFile(File(it)) } ?: localPath?.let { android.net.Uri.parse(it) })
        .build()

    return androidx.media3.common.MediaItem.Builder()
        .setMediaId(id)
        .setUri(localPath?.let { android.net.Uri.parse(it) })
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isUserSignedIn by viewModel.isUserSignedIn
    val context = LocalContext.current
    
    var isPlayerVisible by rememberSaveable { mutableStateOf(false) }
    var isSettingsVisible by rememberSaveable { mutableStateOf(false) }
    var isEqualizerVisible by rememberSaveable { mutableStateOf(false) }
    var isCloudBrowserVisible by rememberSaveable { mutableStateOf(false) }
    var cutterTrack by remember { mutableStateOf<TrackEntity?>(null) }
    
    var showSignInPrompt by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.importLocalTracks(uris)
    }

    if (isPlayerVisible) {
        PlayerScreen(viewModel = viewModel, player = mediaController, onBack = { isPlayerVisible = false })
    } else if (isSettingsVisible) {
        SettingsScreen(viewModel = viewModel, onSignInClick = onSignInClick, onScanClick = { viewModel.scanLocalStorage() }, onBack = { isSettingsVisible = false })
    } else if (isEqualizerVisible) {
        EqualizerScreen(viewModel = viewModel, onBack = { isEqualizerVisible = false })
    } else if (isCloudBrowserVisible) {
        CloudBrowserScreen(viewModel = viewModel, onBack = { isCloudBrowserVisible = false })
    } else if (cutterTrack != null) {
        Mp3CutterScreen(
            track = cutterTrack!!,
            viewModel = viewModel,
            onBack = { cutterTrack = null }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background, modifier = Modifier.width(300.dp)) {
                    Column(Modifier.fillMaxHeight()) {
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
                                Column(Modifier.weight(1f)) {
                                    Text(account?.email ?: "Signed in", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Cloud Sync Enabled", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = onSignOutClick) { Icon(Icons.AutoMirrored.Filled.Logout, "Sign Out", tint = Color.Red) }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        val primaryColor = MaterialTheme.colorScheme.primary
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MusicOn", style = MaterialTheme.typography.headlineMedium.copy(color = primaryColor, fontWeight = FontWeight.Bold))
                            IconButton(onClick = { 
                                if (!isUserSignedIn) { showSignInPrompt = true }
                                else {
                                    scope.launch { drawerState.close(); viewModel.syncAllLocalToCloud() } 
                                }
                            }) {
                                Icon(Icons.Default.CloudSync, "Sync All to Cloud", tint = primaryColor)
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        NavigationDrawerItem(label = { Text("Import Hub (Local)") }, selected = false, onClick = { scope.launch { drawerState.close() }; filePicker.launch("audio/*") }, icon = { Icon(Icons.Default.FileDownload, null) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = MaterialTheme.colorScheme.onSurface))
                        NavigationDrawerItem(label = { Text("Cloud Browser") }, selected = false, onClick = { 
                            if (!isUserSignedIn) { showSignInPrompt = true }
                            else { scope.launch { drawerState.close() }; isCloudBrowserVisible = true }
                        }, icon = { Icon(Icons.Default.CloudQueue, null) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = MaterialTheme.colorScheme.onSurface))
                        NavigationDrawerItem(label = { Text("Equalizer") }, selected = false, onClick = { scope.launch { drawerState.close() }; isEqualizerVisible = true }, icon = { Icon(Icons.Default.Tune, null) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = MaterialTheme.colorScheme.onSurface))
                        NavigationDrawerItem(label = { Text("Settings") }, selected = false, onClick = { scope.launch { drawerState.close() }; isSettingsVisible = true }, icon = { Icon(Icons.Default.Settings, null) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = MaterialTheme.colorScheme.onSurface))
                        NavigationDrawerItem(label = { Text("Share App (APK)") }, selected = false, onClick = { scope.launch { drawerState.close() }; /* Share Logic */ }, icon = { Icon(Icons.Default.Share, null) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = MaterialTheme.colorScheme.onSurface))
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.statusBars,
                bottomBar = { MiniPlayer(onNavigateToPlayer = { isPlayerVisible = true }, player = mediaController, viewModel = viewModel) }
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

    if (showSignInPrompt) {
        AlertDialog(
            onDismissRequest = { showSignInPrompt = false },
            title = { Text("Sign in Required") },
            text = { Text("Please sign in with Google to use cloud features.") },
            confirmButton = {
                Button(onClick = { showSignInPrompt = false; onSignInClick() }) { Text("Sign In") }
            },
            dismissButton = { TextButton(onClick = { showSignInPrompt = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun RenameDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Gray)
            )
        },
        confirmButton = { Button(onClick = { onConfirm(name) }) { Text("Rename") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CloudBrowserScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val isUserSignedIn by viewModel.isUserSignedIn
    val isOnline by viewModel.isOnline.collectAsState()
    val isWifi by viewModel.isWifi.collectAsState()
    val syncStatus by CloudSyncManager.status.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val backgroundMode by viewModel.backgroundMode.collectAsState()
    val cloudManager = remember { com.example.musicon.data.remote.CloudStorageManager(context) }
    var cloudFiles by remember { mutableStateOf<List<com.google.api.services.drive.model.File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var viewMode by rememberSaveable { mutableStateOf(LibraryViewMode.LIST) }
    var sortOrder by rememberSaveable { mutableStateOf("NAME_ASC") }
    var trackToRename by remember { mutableStateOf<com.google.api.services.drive.model.File?>(null) }
    var trackToDownloadConfirm by remember { mutableStateOf<com.google.api.services.drive.model.File?>(null) }
    var tracksToDeleteConfirm by remember { mutableStateOf<List<com.google.api.services.drive.model.File>?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            isRefreshing = true
            cloudFiles = cloudManager.listAudioFiles()
            isRefreshing = false
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }
    BackHandler(onBack = onBack)

    val sortedFiles = remember(cloudFiles, sortOrder) {
        when (sortOrder) {
            "NAME_ASC" -> cloudFiles.sortedBy { it.getName()?.lowercase() }
            "NAME_DESC" -> cloudFiles.sortedByDescending { it.getName()?.lowercase() }
            "SIZE_ASC" -> cloudFiles.sortedBy { it.getSize() ?: 0L }
            "SIZE_DESC" -> cloudFiles.sortedByDescending { it.getSize() ?: 0L }
            else -> cloudFiles
        }
    }

    com.example.musicon.ui.components.StellarBackground(themeMode = themeMode, backgroundMode = backgroundMode) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (selectedIds.isNotEmpty()) {
                    TopAppBar(
                        title = { Text("${selectedIds.size} selected", color = Color.White) },
                        navigationIcon = { IconButton(onClick = { selectedIds = emptySet() }) { Icon(Icons.Default.Close, null, tint = Color.White) } },
                        actions = {
                            IconButton(onClick = { 
                                val allIds = cloudFiles.map { it.id }.toSet()
                                selectedIds = if (selectedIds.size == allIds.size) emptySet() else allIds
                            }) { Icon(Icons.Default.SelectAll, null, tint = Color.White) }
                            IconButton(onClick = { 
                                val selectedFiles = cloudFiles.filter { it.id in selectedIds }
                                // Simple download first of selected
                                if (selectedFiles.isNotEmpty()) trackToDownloadConfirm = selectedFiles.first() 
                            }) { Icon(Icons.Default.Download, null, tint = Color.White) }
                            IconButton(onClick = { tracksToDeleteConfirm = cloudFiles.filter { it.id in selectedIds } }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(0.1f))
                    )
                } else {
                    TopAppBar(
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Cloud Browser", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(12.dp))
                                HeaderStatusPill(isOnline = isOnline, isWifi = isWifi)
                                SyncProgressBar(syncStatus = syncStatus, modifier = Modifier.weight(1f))
                            }
                        },
                        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                        actions = {
                            IconButton(onClick = { viewMode = if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST }) { Icon(if (viewMode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List, null, tint = Color.White) }
                            var showSort by remember { mutableStateOf(false) }
                            IconButton(onClick = { showSort = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, null, tint = Color.White)
                                DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                                    DropdownMenuItem(text = { Text("Name A-Z") }, onClick = { sortOrder = "NAME_ASC"; showSort = false })
                                    DropdownMenuItem(text = { Text("Name Z-A") }, onClick = { sortOrder = "NAME_DESC"; showSort = false })
                                    DropdownMenuItem(text = { Text("Size Smallest") }, onClick = { sortOrder = "SIZE_ASC"; showSort = false })
                                    DropdownMenuItem(text = { Text("Size Largest") }, onClick = { sortOrder = "SIZE_DESC"; showSort = false })
                                }
                            }
                            IconButton(onClick = { viewModel.syncAllLocalToCloud() }) { Icon(Icons.Default.CloudUpload, null, tint = Color.White) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
        ) { padding ->
            if (!isUserSignedIn) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Sign in to view Cloud songs", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                androidx.compose.material3.pulltorefresh.PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { refresh() }, modifier = Modifier.padding(padding).fillMaxSize()) {
                    if (isLoading && !isRefreshing) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
                    } else if (viewMode == LibraryViewMode.GRID) {
                        LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                            items(sortedFiles) { file ->
                                val isSelected = file.id in selectedIds
                                Column(modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color.White.copy(0.15f) else Color.Transparent).combinedClickable(onClick = { if (selectedIds.isNotEmpty()) selectedIds = if (isSelected) selectedIds - file.id else selectedIds + file.id }, onLongClick = { selectedIds = setOf(file.id) }).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(64.dp))
                                    Text(file.getName() ?: "Unknown", color = Color.White, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(sortedFiles) { file ->
                                val isSelected = file.id in selectedIds
                                ListItem(
                                    headlineContent = { Text(file.getName() ?: "Unknown", color = Color.White) },
                                    supportingContent = { Text("${(file.getSize() ?: 0L) / 1024} KB", color = Color.Gray) },
                                    leadingContent = { if (selectedIds.isNotEmpty()) Checkbox(checked = isSelected, onCheckedChange = null) else Icon(Icons.Default.MusicNote, null, tint = Color.Gray) },
                                    trailingContent = {
                                        var showMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                                DropdownMenuItem(text = { Text("Download") }, leadingIcon = { Icon(Icons.Default.Download, null) }, onClick = { trackToDownloadConfirm = file; showMenu = false })
                                                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { trackToRename = file; showMenu = false })
                                                DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }, onClick = { tracksToDeleteConfirm = listOf(file); showMenu = false })
                                            }
                                        }
                                    },
                                    modifier = Modifier.combinedClickable(onClick = { if (selectedIds.isNotEmpty()) selectedIds = if (isSelected) selectedIds - file.id else selectedIds + file.id }, onLongClick = { selectedIds = setOf(file.id) }),
                                    colors = ListItemDefaults.colors(containerColor = if (isSelected) Color.White.copy(0.1f) else Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (trackToRename != null) {
        RenameDialog(initialName = trackToRename!!.getName() ?: "", onDismiss = { trackToRename = null }, onConfirm = { newName ->
            scope.launch { cloudManager.renameFile(trackToRename!!.id, newName); trackToRename = null; refresh() }
        })
    }
    if (trackToDownloadConfirm != null) {
        AlertDialog(onDismissRequest = { trackToDownloadConfirm = null }, title = { Text("Download?") }, text = { Text("Download '${trackToDownloadConfirm!!.getName()}' to internal storage?") }, confirmButton = { Button(onClick = { viewModel.downloadTrack(TrackEntity(id = trackToDownloadConfirm!!.id, title = trackToDownloadConfirm!!.getName() ?: "Unknown", artist = "Cloud", album = "Cloud", duration = 0, gDriveId = trackToDownloadConfirm!!.id)); trackToDownloadConfirm = null }) { Text("Download") } }, dismissButton = { TextButton(onClick = { trackToDownloadConfirm = null }) { Text("Cancel") } })
    }
    if (tracksToDeleteConfirm != null) {
        AlertDialog(onDismissRequest = { tracksToDeleteConfirm = null }, title = { Text("Delete?") }, text = { Text("Delete ${tracksToDeleteConfirm!!.size} songs from Cloud?") }, confirmButton = { Button(onClick = { scope.launch { tracksToDeleteConfirm!!.forEach { cloudManager.deleteFile(it.id) }; tracksToDeleteConfirm = null; selectedIds = emptySet(); refresh() } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") } }, dismissButton = { TextButton(onClick = { tracksToDeleteConfirm = null }) { Text("Cancel") } })
    }
}
