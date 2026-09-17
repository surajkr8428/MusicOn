package com.example.musicon.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicon.R
import com.example.musicon.data.LibraryViewMode
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.ui.components.StellarBackground
import com.example.musicon.ui.components.TrackOptionsBottomSheet
import com.example.musicon.ui.theme.LavenderTitle
import com.example.musicon.ui.viewmodel.MainViewModel
import com.example.musicon.logic.formatSleepTime
import com.example.musicon.logic.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import com.example.musicon.ui.components.PlaylistOptionsBottomSheet
import com.example.musicon.ui.components.RenameDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenCutter: (TrackEntity) -> Unit,
    onOpenPlayer: () -> Unit
) {
    val tracks by viewModel.filteredTracks.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val customFolders by viewModel.customFolders.collectAsState()
    val searchQuery by viewModel.searchQuery
    val sortOrder by viewModel.songSortOrder.collectAsState()
    val viewMode by viewModel.libraryViewMode.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isWifi by viewModel.isWifi.collectAsState()
    val syncStatus by com.example.musicon.data.remote.CloudSyncManager.status.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val backgroundMode by viewModel.backgroundMode.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTrackOptions by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToEdit by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToDeleteConfirm by remember { mutableStateOf<TrackEntity?>(null) }
    var tracksToBulkDeleteConfirm by remember { mutableStateOf<List<TrackEntity>?>(null) }
    
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var currentPlaylistDetail by remember { mutableStateOf<com.example.musicon.data.local.Playlist?>(null) }
    var selectedPlaylistOptions by remember { mutableStateOf<com.example.musicon.data.local.Playlist?>(null) }
    var playlistToRename by remember { mutableStateOf<com.example.musicon.data.local.Playlist?>(null) }
    var playlistToRemoveConfirm by remember { mutableStateOf<com.example.musicon.data.local.Playlist?>(null) }
    var playlistToDeleteConfirm by remember { mutableStateOf<com.example.musicon.data.local.Playlist?>(null) }
    var showTrackInfoDialog by remember { mutableStateOf<TrackEntity?>(null) }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showBulkPlaylistDialog by remember { mutableStateOf(false) }

    val baseTabs = listOf("All Songs", "Playlists", "Albums", "Artists", "Genres", "Recent", "Popular")
    val tabs = baseTabs + customFolders.map { it.substringAfterLast("/").ifBlank { "Folder" } }
    val pagerState = rememberPagerState { tabs.size }

    val songListState = rememberLazyListState()
    val songGridState = rememberLazyGridState()
    val playlistListState = rememberLazyListState()
    val playlistGridState = rememberLazyGridState()
    val groupedListStates = remember { mutableStateMapOf<String, androidx.compose.foundation.lazy.LazyListState>() }
    val groupedGridStates = remember { mutableStateMapOf<String, androidx.compose.foundation.lazy.grid.LazyGridState>() }
    val folderListStates = remember { mutableStateMapOf<String, androidx.compose.foundation.lazy.LazyListState>() }
    val folderGridStates = remember { mutableStateMapOf<String, androidx.compose.foundation.lazy.grid.LazyGridState>() }
    
    var showSignInRequiredDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(syncStatus) {
        val currentStatus = syncStatus
        if (currentStatus is com.example.musicon.data.remote.SyncStatus.Success) {
            android.widget.Toast.makeText(context, currentStatus.message, android.widget.Toast.LENGTH_SHORT).show()
        } else if (currentStatus is com.example.musicon.data.remote.SyncStatus.Error) {
            android.widget.Toast.makeText(context, currentStatus.message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(isSelectionMode || searchQuery.isNotEmpty() || currentPlaylistDetail != null || isSearchActive) {
        if (isSelectionMode) selectedIds = emptySet()
        else if (isSearchActive) { isSearchActive = false; viewModel.updateSearchQuery("") }
        else if (currentPlaylistDetail != null) currentPlaylistDetail = null
    }

    if (currentPlaylistDetail != null) {
        PlaylistDetailScreen(playlist = currentPlaylistDetail!!, viewModel = viewModel, onBack = { currentPlaylistDetail = null }, viewMode = viewMode, isLandscape = isLandscape)
    } else {
        StellarBackground(themeMode = themeMode, backgroundMode = backgroundMode) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    if (isSelectionMode) {
                        SelectionTopBar(count = selectedIds.size, onClose = { selectedIds = emptySet() }, onSelectAll = { val all = tracks.map { it.id }.toSet(); selectedIds = if (selectedIds.size == all.size) emptySet() else all })
                    } else {
                        LibraryTopBar(searchQuery, isSearchActive, { isSearchActive = !isSearchActive }, { viewModel.updateSearchQuery(it) }, onOpenDrawer, onOpenSettings, { showSortMenu = true }, { viewModel.updateLibraryViewMode(if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST) }, viewMode, sleepTimerRemaining, isLandscape, viewModel, isOnline, isWifi, syncStatus)
                    }
                },
                bottomBar = {
                    AnimatedVisibility(visible = isSelectionMode, enter = expandVertically(), exit = shrinkVertically()) {
                        SelectionBottomBar(
                            onPlay = { viewModel.playSelected(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() },
                            onNext = { viewModel.addToQueueNext(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() },
                            onPlaylist = { showBulkPlaylistDialog = true },
                            onShare = { 
                                val toShare = allTracks.filter { it.id in selectedIds }
                                if (toShare.isNotEmpty()) viewModel.shareTracks(toShare)
                                selectedIds = emptySet()
                            },
                            onDelete = { tracksToBulkDeleteConfirm = allTracks.filter { it.id in selectedIds } },
                            onUpload = { 
                                if (!viewModel.isUserSignedIn.value) {
                                    showSignInRequiredDialog = true 
                                } else { 
                                    val toUpload = allTracks.filter { it.id in selectedIds }
                                    if (toUpload.isNotEmpty()) {
                                        android.widget.Toast.makeText(context, "Starting GDrive sync for ${toUpload.size} songs", android.widget.Toast.LENGTH_SHORT).show()
                                        viewModel.bulkUpload(toUpload)
                                    }
                                    selectedIds = emptySet() 
                                } 
                            },
                            onRemove = { viewModel.removeFromLibrary(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() }
                        )
                    }
                }
            ) { innerPadding ->
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { scope.launch { isRefreshing = true; viewModel.scanLocalStorage(); delay(1000); isRefreshing = false } }, modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        if (!isSelectionMode) {
                            ScrollableTabRow(selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent, edgePadding = 16.dp, divider = {}, indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(it[pagerState.currentPage]), color = MaterialTheme.colorScheme.primary) }) {
                                tabs.forEachIndexed { index, title -> 
                                    val isSelected = pagerState.currentPage == index
                                    val isBright = com.example.musicon.ui.components.LocalIsBackgroundBright.current
                                    val tabColor = if (isSelected) (if (isBright) Color.Black else Color.White) else Color.Gray
                                    Tab(
                                        selected = isSelected, 
                                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } }, 
                                        text = { Text(title, color = tabColor) }
                                    ) 
                                }
                            }
                        }
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            when (page) {
                                0 -> SongsTab(tracks, selectedIds, viewMode, isLandscape, songListState, songGridState, { if (isSelectionMode) selectedIds = if (it.id in selectedIds) selectedIds - it.id else selectedIds + it.id else viewModel.playTrackList(tracks, it) }, { selectedIds = selectedIds + it.id }, { selectedTrackOptions = it }, { viewModel.playSelected(tracks.shuffled()) }, { viewModel.playSelected(tracks) }, { viewModel.toggleFavorite(it) })
                                1 -> PlaylistsTab(playlists, viewMode, playlistListState, playlistGridState, { currentPlaylistDetail = it }, { showCreatePlaylistDialog = true }, { selectedPlaylistOptions = it })
                                2 -> GroupedTab(allTracks, "Album", viewMode, groupedListStates.getOrPut("Album"){rememberLazyListState()}, groupedGridStates.getOrPut("Album"){rememberLazyGridState()}) { viewModel.playSelected(it) }
                                3 -> GroupedTab(allTracks, "Artist", viewMode, groupedListStates.getOrPut("Artist"){rememberLazyListState()}, groupedGridStates.getOrPut("Artist"){rememberLazyGridState()}) { viewModel.playSelected(it) }
                                4 -> GroupedTab(allTracks, "Genre", viewMode, groupedListStates.getOrPut("Genre"){rememberLazyListState()}, groupedGridStates.getOrPut("Genre"){rememberLazyGridState()}) { viewModel.playSelected(it) }
                                5 -> { val recent by viewModel.recentlyPlayed.collectAsState(); SongsTab(recent, selectedIds, viewMode, isLandscape, rememberLazyListState(), rememberLazyGridState(), { viewModel.playTrackList(recent, it) }, { selectedIds = selectedIds + it.id }, { selectedTrackOptions = it }, { viewModel.playSelected(recent.shuffled()) }, { viewModel.playSelected(recent) }, { viewModel.toggleFavorite(it) }) }
                                6 -> { val popular by viewModel.mostPlayed.collectAsState(); SongsTab(popular, selectedIds, viewMode, isLandscape, rememberLazyListState(), rememberLazyGridState(), { viewModel.playTrackList(popular, it) }, { selectedIds = selectedIds + it.id }, { selectedTrackOptions = it }, { viewModel.playSelected(popular.shuffled()) }, { viewModel.playSelected(popular) }, { viewModel.toggleFavorite(it) }) }
                                else -> {
                                    val path = customFolders[page - 7]
                                    val fTracks = allTracks.filter { it.localPath?.startsWith(path) == true }
                                    SongsTab(fTracks, selectedIds, viewMode, isLandscape, folderListStates.getOrPut(path){rememberLazyListState()}, folderGridStates.getOrPut(path){rememberLazyGridState()}, { viewModel.playTrackList(fTracks, it) }, { selectedIds = selectedIds + it.id }, { selectedTrackOptions = it }, { viewModel.playSelected(fTracks.shuffled()) }, { viewModel.playSelected(fTracks) }, { viewModel.toggleFavorite(it) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) com.example.musicon.ui.components.CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false }, onConfirm = { viewModel.createPlaylist(it, selectedIds.toList()); showCreatePlaylistDialog = false; selectedIds = emptySet() })
    if (showSortMenu) SortMenu(currentOrder = sortOrder, onDismiss = { showSortMenu = false }, onSortSelected = { viewModel.updateSongSortOrder(it); showSortMenu = false })
    if (showBulkPlaylistDialog) com.example.musicon.ui.components.AddToPlaylistDialog(playlists = playlists, onDismiss = { showBulkPlaylistDialog = false }, onPlaylistSelected = { viewModel.bulkAddTracksToPlaylist(it, selectedIds.toList()); showBulkPlaylistDialog = false; selectedIds = emptySet() }, onCreateNew = { showBulkPlaylistDialog = false; showCreatePlaylistDialog = true })
    if (showSignInRequiredDialog) AlertDialog(onDismissRequest = { showSignInRequiredDialog = false }, title = { Text("Sign in Required") }, text = { Text("Please sign in with Google to use cloud features.") }, confirmButton = { Button(onClick = { showSignInRequiredDialog = false; onOpenDrawer() }) { Text("Sign In") } }, dismissButton = { TextButton(onClick = { showSignInRequiredDialog = false }) { Text("Cancel") } })
    if (selectedTrackOptions != null) {
        TrackOptionsBottomSheet(track = selectedTrackOptions!!, onDismiss = { selectedTrackOptions = null }, onAction = { action ->
            when (action) {
                "favorite" -> viewModel.toggleFavorite(selectedTrackOptions!!)
                "play" -> { viewModel.playTrack(selectedTrackOptions!!) }
                "play_next" -> viewModel.addToQueueNext(listOf(selectedTrackOptions!!))
                "download" -> viewModel.downloadTrack(selectedTrackOptions!!)
                "add_to_playlist" -> { showBulkPlaylistDialog = false; selectedIds = setOf(selectedTrackOptions!!.id); showBulkPlaylistDialog = true }
                "cut" -> onOpenCutter(selectedTrackOptions!!)
                "edit" -> trackToEdit = selectedTrackOptions
                "location" -> viewModel.openFileLocation(selectedTrackOptions!!)
                "remove" -> trackToDeleteConfirm = selectedTrackOptions
                "upload" -> { if (!viewModel.isUserSignedIn.value) showSignInRequiredDialog = true else viewModel.uploadTrack(selectedTrackOptions!!) }
                "info" -> { showTrackInfoDialog = selectedTrackOptions }
                "share" -> { viewModel.shareTrack(selectedTrackOptions!!) }
                "delete" -> { tracksToBulkDeleteConfirm = listOf(selectedTrackOptions!!) }
            }
            selectedTrackOptions = null
        })
    }
    
    if (selectedPlaylistOptions != null) {
        PlaylistOptionsBottomSheet(
            playlist = selectedPlaylistOptions!!,
            onDismiss = { selectedPlaylistOptions = null },
            onAction = { action ->
                when (action) {
                    "play" -> {
                        scope.launch {
                            val pid = selectedPlaylistOptions?.id ?: return@launch
                            val pTracks = viewModel.getTracksForPlaylist(pid).first()
                            if (pTracks.isNotEmpty()) viewModel.playTrackList(pTracks, pTracks.first())
                        }
                    }
                    "rename" -> { playlistToRename = selectedPlaylistOptions }
                    "share" -> { selectedPlaylistOptions?.let { viewModel.sharePlaylist(it) } }
                    "remove" -> { playlistToRemoveConfirm = selectedPlaylistOptions }
                    "delete" -> { playlistToDeleteConfirm = selectedPlaylistOptions }
                }
                selectedPlaylistOptions = null
            }
        )
    }

    if (playlistToRename != null) {
        RenameDialog(initialName = playlistToRename!!.name, onDismiss = { playlistToRename = null }, onConfirm = { 
            viewModel.renamePlaylist(playlistToRename!!.id, it)
            playlistToRename = null 
        })
    }

    if (playlistToRemoveConfirm != null) AlertDialog(onDismissRequest = { playlistToRemoveConfirm = null }, title = { Text("Remove from list?") }, text = { Text("Song entries will remain but playlist will be removed.") }, confirmButton = { Button(onClick = { viewModel.deletePlaylist(playlistToRemoveConfirm!!); playlistToRemoveConfirm = null }) { Text("Remove") } }, dismissButton = { TextButton(onClick = { playlistToRemoveConfirm = null }) { Text("Cancel") } })
    if (playlistToDeleteConfirm != null) AlertDialog(onDismissRequest = { playlistToDeleteConfirm = null }, title = { Text("Delete Playlist?", color = Color.White) }, text = { Text("This will permanently delete the playlist '${playlistToDeleteConfirm!!.name}'. Tracks will not be deleted.", color = Color.Gray) }, confirmButton = { Button(onClick = { viewModel.deletePlaylist(playlistToDeleteConfirm!!); playlistToDeleteConfirm = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") } }, dismissButton = { TextButton(onClick = { playlistToDeleteConfirm = null }) { Text("Cancel") } })

    if (showTrackInfoDialog != null) {
        val t = showTrackInfoDialog!!
        AlertDialog(
            onDismissRequest = { showTrackInfoDialog = null },
            title = { Text("Song Info", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Title: ${t.displayName}", fontSize = 14.sp)
                    Text("Artist: ${t.displayArtist}", fontSize = 14.sp)
                    Text("Album: ${t.displayAlbum}", fontSize = 14.sp)
                    Text("Duration: ${formatDuration(t.duration)}", fontSize = 14.sp)
                    Text("Path: ${t.localPath ?: "Cloud"}", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = { Button(onClick = { showTrackInfoDialog = null }) { Text("Close") } }
        )
    }
    if (trackToDeleteConfirm != null) AlertDialog(onDismissRequest = { trackToDeleteConfirm = null }, title = { Text("Remove Song?") }, text = { Text("Are you sure you want to remove '${trackToDeleteConfirm!!.displayName}' from your library?") }, confirmButton = { Button(onClick = { viewModel.removeFromLibrary(listOf(trackToDeleteConfirm!!)); trackToDeleteConfirm = null }) { Text("Remove") } }, dismissButton = { TextButton(onClick = { trackToDeleteConfirm = null }) { Text("Cancel") } })
    if (tracksToBulkDeleteConfirm != null) AlertDialog(onDismissRequest = { tracksToBulkDeleteConfirm = null }, title = { Text("Delete Songs?", color = Color.White) }, text = { Text("Are you sure you want to delete ${tracksToBulkDeleteConfirm!!.size} songs? This cannot be undone.", color = Color.Gray) }, confirmButton = { Button(onClick = { viewModel.bulkDelete(tracksToBulkDeleteConfirm!!); tracksToBulkDeleteConfirm = null; selectedIds = emptySet() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") } }, dismissButton = { TextButton(onClick = { tracksToBulkDeleteConfirm = null }) { Text("Cancel") } })
    if (trackToEdit != null) com.example.musicon.ui.components.EditTrackDialog(track = trackToEdit!!, onDismiss = { trackToEdit = null }, onConfirm = { t, ar, al, c, l -> viewModel.updateTrackMetadata(trackToEdit!!.id, t, ar, al, c, l); trackToEdit = null })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(playlist: com.example.musicon.data.local.Playlist, viewModel: MainViewModel, onBack: () -> Unit, viewMode: LibraryViewMode, isLandscape: Boolean) {
    val tracks by viewModel.getTracksForPlaylist(playlist.id).collectAsState(emptyList())
    StellarBackground {
        Scaffold(containerColor = Color.Transparent, topBar = { CenterAlignedTopAppBar(title = { Text(playlist.name, color = Color.White, fontFamily = FontFamily.Cursive, fontSize = if (isLandscape) 18.sp else 24.sp) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, actions = { IconButton(onClick = { }) { Icon(Icons.Default.Add, null, tint = Color.White) }; IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (viewMode == LibraryViewMode.GRID) LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 100.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) { items(tracks) { StellarGridItem(it, false, { viewModel.playTrackList(tracks, it) }, {}, { viewModel.toggleFavorite(it) }) } }
                else LazyColumn(Modifier.fillMaxSize()) { items(tracks) { StellarTrackItem(it, false, { viewModel.playTrackList(tracks, it) }, {}, { }, { viewModel.toggleFavorite(it) }) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
    searchQuery: String, 
    isSearchActive: Boolean,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit, 
    onOpenDrawer: () -> Unit, 
    onOpenSettings: () -> Unit,
    onSortClick: () -> Unit,
    onViewModeToggle: () -> Unit,
    viewMode: LibraryViewMode,
    timerRemaining: Long?,
    isLandscape: Boolean,
    viewModel: MainViewModel,
    isOnline: Boolean,
    isWifi: Boolean,
    syncStatus: com.example.musicon.data.remote.SyncStatus
) {
    TopAppBar(
        modifier = if (isLandscape) Modifier.height(IntrinsicSize.Min) else Modifier, windowInsets = WindowInsets(0),
        title = {
            if (isSearchActive) TextField(value = searchQuery, onValueChange = onSearchQueryChange, placeholder = { Text("Search songs...", color = Color.Gray, fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth().padding(end = 8.dp).then(if (isLandscape) Modifier.height(40.dp) else Modifier), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true, trailingIcon = { IconButton(onClick = onSearchToggle) { Icon(Icons.Default.Close, null, tint = Color.Gray) } })
            else Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = if (isLandscape) 0.dp else 4.dp)) {
                val isBright = com.example.musicon.ui.components.LocalIsBackgroundBright.current
                val headerTextColor = if (isBright) Color.Black else Color.White

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Raag", color = headerTextColor, fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 18.sp else 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )

                    // Stretched Progress Bar from Icon to Header Actions (Settings)
                    SyncProgressBar(syncStatus, Modifier.weight(1f).padding(horizontal = 12.dp))
                }
                
                if (timerRemaining != null) {
                    val isPaused by viewModel.isSleepTimerPaused.collectAsState()
                    Surface(color = MaterialTheme.colorScheme.primary.copy(0.2f), shape = RoundedCornerShape(22.dp)) { 
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { 
                            Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(formatSleepTime(timerRemaining), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            
                            // Pause/Resume Button
                            IconButton(onClick = { viewModel.toggleSleepTimerPause() }, modifier = Modifier.size(24.dp)) {
                                Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                            
                            // Reset Button
                            IconButton(onClick = { viewModel.resetSleepTimer() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        } 
                    }
                }
            }
        },
        navigationIcon = { 
            val isBright = com.example.musicon.ui.components.LocalIsBackgroundBright.current
            val headerTextColor = if (isBright) Color.Black else Color.White
            IconButton(onClick = onOpenDrawer, modifier = if (isLandscape) Modifier.size(36.dp).padding(start = 4.dp) else Modifier) { Icon(Icons.Default.Menu, null, tint = headerTextColor, modifier = if (isLandscape) Modifier.size(20.dp) else Modifier) } 
        },
        actions = {
            val isBright = com.example.musicon.ui.components.LocalIsBackgroundBright.current
            val headerTextColor = if (isBright) Color.Black else Color.White
            if (!isSearchActive) {
                HeaderStatusPill(isOnline, isWifi)
                val iconSize = if (isLandscape) 36.dp else 48.dp
                val innerSize = if (isLandscape) 20.dp else 24.dp
                IconButton(onClick = onSearchToggle, modifier = Modifier.size(iconSize)) { Icon(Icons.Default.Search, null, tint = headerTextColor, modifier = Modifier.size(innerSize)) }
                IconButton(onClick = onViewModeToggle, modifier = Modifier.size(iconSize)) { Icon(if (viewMode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List, null, tint = headerTextColor, modifier = Modifier.size(innerSize)) }
                IconButton(onClick = onSortClick, modifier = Modifier.size(iconSize)) { Icon(Icons.AutoMirrored.Filled.Sort, null, tint = headerTextColor, modifier = Modifier.size(innerSize)) }
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(iconSize)) { Icon(Icons.Default.Settings, null, tint = headerTextColor, modifier = Modifier.size(innerSize)) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun HeaderStatusPill(isOnline: Boolean, isWifi: Boolean) {
    Surface(
        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOnline && isWifi) Icon(Icons.Default.Wifi, null, tint = Color.White, modifier = Modifier.size(14.dp).padding(end = 6.dp))
            Text(text = if (isOnline) "ONLINE" else "OFFLINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color.White)
        }
    }
}

@Composable
fun SyncProgressBar(syncStatus: com.example.musicon.data.remote.SyncStatus, modifier: Modifier = Modifier) {
    if (syncStatus is com.example.musicon.data.remote.SyncStatus.Idle) {
        Box(modifier)
        return
    }

    val barColor = when (syncStatus) {
        is com.example.musicon.data.remote.SyncStatus.Loading -> Color.Yellow
        is com.example.musicon.data.remote.SyncStatus.Success -> Color.Green
        is com.example.musicon.data.remote.SyncStatus.Error -> Color.Red
        else -> MaterialTheme.colorScheme.primary
    }

    val progress = when (syncStatus) {
        is com.example.musicon.data.remote.SyncStatus.Loading -> syncStatus.progress
        is com.example.musicon.data.remote.SyncStatus.Success -> 1f
        else -> -1f
    }

    Box(modifier = modifier.height(36.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        val sweepProgress = if (progress >= 0f) progress else 0.5f
        
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(15.dp).clip(RoundedCornerShape(8.dp))) {
            val width = size.width
            val height = size.height
            
            // Track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2)
            )
            
            // Progress line
            drawRoundRect(
                color = barColor,
                size = androidx.compose.ui.geometry.Size(width * sweepProgress, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2)
            )
            
            // Tip Dot - Circular indicator at the tip
            if (syncStatus is com.example.musicon.data.remote.SyncStatus.Loading) {
                drawCircle(
                    color = Color.White,
                    radius = height * 0.7f, // Slightly larger than bar
                    center = Offset(width * sweepProgress, height / 2)
                )
                drawCircle(
                    color = barColor,
                    radius = height * 0.4f,
                    center = Offset(width * sweepProgress, height / 2)
                )
            }
        }

        if (syncStatus is com.example.musicon.data.remote.SyncStatus.Success) {
            val uploaded = syncStatus.uploaded
            val failed = syncStatus.failed
            val text = if (uploaded > 0 || failed > 0) {
                "Upload finished! $uploaded successful, $failed failed"
            } else {
                "Sync Complete"
            }
            Text(
                text = text,
                color = Color.Black,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        } else if (syncStatus is com.example.musicon.data.remote.SyncStatus.Loading && syncStatus.total > 0) {
            Text(
                text = "Uploading: ${syncStatus.current} / ${syncStatus.total}",
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        } else if (progress >= 0f) {
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable fun SortMenu(currentOrder: String, onDismiss: () -> Unit, onSortSelected: (String) -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Sort Songs By", fontSize = 16.sp, fontWeight = FontWeight.Bold) }, text = { Column { SortCategory("Name", "NAME", currentOrder, onSortSelected); SortCategory("Artist", "ARTIST", currentOrder, onSortSelected); SortCategory("Recent", "RECENT", currentOrder, onSortSelected); SortCategory("Duration", "DURATION", currentOrder, onSortSelected) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }, shape = RoundedCornerShape(12.dp)) }
@Composable fun SortCategory(label: String, baseValue: String, current: String, onSelect: (String) -> Unit) { Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { SortMiniButton(if (baseValue == "RECENT") "Newest" else if (baseValue == "DURATION") "Longest" else "A-Z", "${baseValue}_ASC", current, onSelect, Modifier.weight(1f)); SortMiniButton(if (baseValue == "RECENT") "Oldest" else if (baseValue == "DURATION") "Shortest" else "Z-A", "${baseValue}_DESC", current, onSelect, Modifier.weight(1f)) } } }
@Composable fun SortMiniButton(label: String, value: String, current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) { val isSelected = value == current; Button(onClick = { onSelect(value) }, modifier = modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(0.05f), contentColor = if (isSelected) Color.Black else Color.White), contentPadding = PaddingValues(horizontal = 8.dp), shape = RoundedCornerShape(8.dp)) { Text(label, fontSize = 11.sp, maxLines = 1) } }
@OptIn(ExperimentalMaterial3Api::class) @Composable fun SelectionTopBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit) { TopAppBar(title = { Text("$count selected", color = Color.White) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) } }, windowInsets = WindowInsets(0), actions = { IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Select All", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.1f))) }
@Composable fun SelectionBottomBar(onPlay: () -> Unit, onNext: () -> Unit, onPlaylist: () -> Unit, onShare: () -> Unit, onUpload: () -> Unit, onRemove: () -> Unit, onDelete: () -> Unit) { Surface(color = Color(0xFF1E1B36).copy(alpha = 0.95f), modifier = Modifier.fillMaxWidth().height(70.dp)) { Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { SelectionActionItem(Icons.Default.PlayArrow, "Play", onPlay); SelectionActionItem(Icons.AutoMirrored.Filled.PlaylistPlay, "Next", onNext); SelectionActionItem(Icons.AutoMirrored.Filled.PlaylistAdd, "Playlist", onPlaylist); SelectionActionItem(Icons.Default.Share, "Share", onShare); SelectionActionItem(Icons.Default.CloudUpload, "GDrive", onUpload); SelectionActionItem(Icons.Default.RemoveCircleOutline, "Remove", onRemove); SelectionActionItem(Icons.Default.Delete, "Delete", onDelete) } } }
@Composable fun SelectionActionItem(icon: ImageVector, label: String, onClick: () -> Unit) { IconButton(onClick = onClick) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)); Text(label, fontSize = 9.sp, color = Color.White) } } }

@Composable fun SongsTab(tracks: List<TrackEntity>, selectedIds: Set<String>, viewMode: LibraryViewMode, isLandscape: Boolean, listState: androidx.compose.foundation.lazy.LazyListState, gridState: androidx.compose.foundation.lazy.grid.LazyGridState, onTrackClick: (TrackEntity) -> Unit, onTrackLongClick: (TrackEntity) -> Unit, onOptions: (TrackEntity) -> Unit, onShuffleAll: () -> Unit, onPlayAll: () -> Unit, onToggleFavorite: (TrackEntity) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (viewMode == LibraryViewMode.GRID) LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(minSize = 100.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) { items(tracks) { StellarGridItem(it, it.id in selectedIds, onTrackClick, onTrackLongClick, { onToggleFavorite(it) }) } }
        else LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) { items(tracks) { StellarTrackItem(it, it.id in selectedIds, { onTrackClick(it) }, { onTrackLongClick(it) }, { onOptions(it) }, { onToggleFavorite(it) }) } }
    }
}

@Composable fun PlaylistsTab(playlists: List<com.example.musicon.data.local.Playlist>, viewMode: LibraryViewMode, listState: androidx.compose.foundation.lazy.LazyListState, gridState: androidx.compose.foundation.lazy.grid.LazyGridState, onPlaylistClick: (com.example.musicon.data.local.Playlist) -> Unit, onCreatePlaylist: () -> Unit, onOptions: (com.example.musicon.data.local.Playlist) -> Unit) {
    if (viewMode == LibraryViewMode.GRID) LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(minSize = 100.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) { items(playlists) { PlaylistGridItem(it, onPlaylistClick, onOptions) }; item { Column(modifier = Modifier.padding(6.dp).clickable { onCreatePlaylist() }, horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(32.dp)) }; Spacer(Modifier.height(6.dp)); Text("Add Playlist", color = Color.White, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center) } } }
    else LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) { items(playlists) { ListItem(headlineContent = { Text(it.name, color = Color.White) }, leadingContent = { val icon = if (it.name == "Favorite") Icons.Default.Favorite else Icons.AutoMirrored.Filled.PlaylistAdd; Icon(icon, null, tint = if (it.name == "Favorite") Color.Red else Color.White) }, trailingContent = { IconButton(onClick = { onOptions(it) }) { Icon(Icons.Default.MoreVert, null, tint = Color.Gray) } }, modifier = Modifier.clickable { onPlaylistClick(it) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }; item { ListItem(headlineContent = { Text("Add Playlist", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }, leadingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }, modifier = Modifier.clickable { onCreatePlaylist() }, colors = ListItemDefaults.colors(containerColor = Color.Transparent) ) } }
}

@Composable fun PlaylistGridItem(playlist: com.example.musicon.data.local.Playlist, onClick: (com.example.musicon.data.local.Playlist) -> Unit, onOptions: (com.example.musicon.data.local.Playlist) -> Unit) { 
    Column(modifier = Modifier.padding(6.dp).clickable { onClick(playlist) }, horizontalAlignment = Alignment.CenterHorizontally) { 
        Box(Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) { 
            Icon(if (playlist.name == "Favorite") Icons.Default.Favorite else Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = if (playlist.name == "Favorite") Color.Red else Color.Gray, modifier = Modifier.size(32.dp))
            IconButton(onClick = { onOptions(playlist) }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) { 
                Icon(Icons.Default.MoreVert, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)) 
            } 
        }
        Spacer(Modifier.height(6.dp))
        Text(playlist.name, color = Color.White, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center) 
    } 
}

@Composable fun GroupedTab(tracks: List<TrackEntity>, groupType: String, viewMode: LibraryViewMode, listState: androidx.compose.foundation.lazy.LazyListState, gridState: androidx.compose.foundation.lazy.grid.LazyGridState, onPlayGroup: (List<TrackEntity>) -> Unit) {
    val grouped = remember(tracks) { when (groupType) { "Album" -> tracks.groupBy { it.displayAlbum }; "Artist" -> tracks.groupBy { it.displayArtist }; "Genre" -> tracks.groupBy { it.genre ?: "Unknown" }; else -> emptyMap() } }
    if (grouped.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No $groupType found", color = Color.Gray) }
    else { if (viewMode == LibraryViewMode.GRID) LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(minSize = 100.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) { grouped.forEach { (name, gTracks) -> item { GroupGridItem(name, gTracks, onPlayGroup) } } } else LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) { grouped.forEach { (name, gTracks) -> item { ListItem(headlineContent = { Text(name, color = Color.White, fontWeight = FontWeight.Bold) }, supportingContent = { Text("${gTracks.size} songs", color = Color.Gray) }, leadingContent = { Box(Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, null, tint = Color.Gray) } }, modifier = Modifier.clickable { onPlayGroup(gTracks) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) } } } }
}

@OptIn(ExperimentalFoundationApi::class) @Composable fun StellarTrackItem(track: TrackEntity, isSelected: Boolean, onPlay: () -> Unit, onLongClick: () -> Unit, onOptions: () -> Unit, onToggleFavorite: () -> Unit) {
    val isBright = com.example.musicon.ui.components.LocalIsBackgroundBright.current
    val primaryTextColor = if (isBright) Color.Black else LavenderTitle
    val secondaryTextColor = if (isBright) Color.DarkGray else Color.LightGray

    Row(modifier = Modifier.fillMaxWidth().background(if (isSelected) (if (isBright) Color.Black.copy(0.05f) else Color.White.copy(0.1f)) else Color.Transparent).combinedClickable(onClick = onPlay, onLongClick = onLongClick, onDoubleClick = onToggleFavorite).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { 
        Box { 
            val imageModel = remember(track.customCoverPath, track.localPath) {
                if (track.customCoverPath?.startsWith("http") == true) track.customCoverPath
                else track.customCoverPath?.let { File(it) }?.takeIf { it.exists() }
                    ?: track.localPath?.let { if (it.startsWith("content://")) it else File(it) }
                    ?: R.drawable.ic_launcher_foreground
            }
            AsyncImage(model = imageModel, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            if (isSelected) Box(modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.White) } 
            
            // Cloud Icon Overlay - Solid indicator
            if (track.gDriveId != null && !track.isDownloaded) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Cloud, 
                            null, 
                            tint = Color(0xFF1976D2), // Standard cloud blue
                            modifier = Modifier.padding(3.dp)
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) { 
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Text(text = track.displayName, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Cursive, color = primaryTextColor, fontSize = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) { Icon(if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, modifier = Modifier.size(16.dp), tint = if (track.isFavorite) Color.Red else secondaryTextColor) } 
            }
            Text(text = "${track.displayArtist} | ${formatDuration(track.duration)}", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor, maxLines = 1) 
        }
        IconButton(onClick = onOptions) { Icon(Icons.Default.MoreVert, null, tint = secondaryTextColor, modifier = Modifier.size(20.dp)) } 
    }
}
@OptIn(ExperimentalFoundationApi::class) @Composable fun StellarGridItem(track: TrackEntity, isSelected: Boolean, onPlay: (TrackEntity) -> Unit, onLongClick: (TrackEntity) -> Unit, onToggleFavorite: () -> Unit) {
    val isBright = com.example.musicon.ui.components.LocalIsBackgroundBright.current
    val primaryTextColor = if (isBright) Color.Black else LavenderTitle
    val secondaryTextColor = if (isBright) Color.DarkGray else Color.Gray

    Column(modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = { onPlay(track) }, onLongClick = { onLongClick(track) }, onDoubleClick = onToggleFavorite).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) { 
        Box { 
            val imageModel = remember(track.customCoverPath, track.localPath) {
                if (track.customCoverPath?.startsWith("http") == true) track.customCoverPath
                else track.customCoverPath?.let { File(it) }?.takeIf { it.exists() }
                    ?: track.localPath?.let { if (it.startsWith("content://")) it else File(it) }
                    ?: R.drawable.ic_launcher_foreground
            }
            AsyncImage(model = imageModel, contentDescription = null, modifier = Modifier.aspectRatio(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            if (isSelected) Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.White) }
            
            // Cloud Icon Overlay - Solid indicator
            if (track.gDriveId != null && !track.isDownloaded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Cloud, 
                            null, 
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
            
            if (track.isFavorite) Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(text = track.displayName, color = primaryTextColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(text = track.displayArtist, color = secondaryTextColor, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) 
    }
}

@Composable fun GroupGridItem(name: String, tracks: List<TrackEntity>, onClick: (List<TrackEntity>) -> Unit) { Column(modifier = Modifier.padding(6.dp).clickable { onClick(tracks) }, horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, null, tint = Color.Gray, modifier = Modifier.size(32.dp)) }; Spacer(Modifier.height(8.dp)); Text(name, color = Color.White, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center); Text("${tracks.size} songs", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center) } }
@Composable
fun StellarActionButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) { Button(onClick = onClick, modifier = modifier.height(38.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), contentColor = Color.Black), shape = RoundedCornerShape(22.dp)) { Icon(icon, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit, onSet: (Int, Int, Int) -> Unit) {
    var h by remember { mutableStateOf("") }
    var m by remember { mutableStateOf("") }
    var s by remember { mutableStateOf("") }
    val isBright = com.example.musicon.ui.components.LocalIsBackgroundBright.current
    val contentColor = if (isBright) Color.Black else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer", color = contentColor, fontWeight = FontWeight.Bold) },
        containerColor = if (isBright) Color.White else Color(0xFF1E1B36),
        text = {
            Column {
                val times = listOf(
                    "Off" to Triple(0, 0, 0), 
                    "15 min" to Triple(0, 15, 0), 
                    "30 min" to Triple(0, 30, 0), 
                    "1 hour" to Triple(1, 0, 0)
                )
                times.forEach { (label, hms) ->
                    TextButton(onClick = { onSet(hms.first, hms.second, hms.third) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, color = contentColor)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = contentColor.copy(alpha = 0.1f))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = h, onValueChange = { if (it.length <= 2) h = it },
                        label = { Text("HH", fontSize = 10.sp) }, modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = contentColor, unfocusedTextColor = contentColor),
                        singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = m, onValueChange = { if (it.length <= 2) m = it },
                        label = { Text("MM", fontSize = 10.sp) }, modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = contentColor, unfocusedTextColor = contentColor),
                        singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = s, onValueChange = { if (it.length <= 2) s = it },
                        label = { Text("SS", fontSize = 10.sp) }, modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = contentColor, unfocusedTextColor = contentColor),
                        singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
                Text("Enter hours, minutes and seconds", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = { 
            Button(onClick = { onSet(h.toIntOrNull() ?: 0, m.toIntOrNull() ?: 0, s.toIntOrNull() ?: 0) }) { Text("Set") } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}
