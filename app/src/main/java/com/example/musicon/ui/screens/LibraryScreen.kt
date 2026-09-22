package com.example.musicon.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicon.R
import com.example.musicon.data.LibraryViewMode
import com.example.musicon.data.local.Playlist
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.data.remote.CloudSyncManager
import com.example.musicon.data.remote.SyncStatus
import com.example.musicon.ui.components.StellarBackground
import com.example.musicon.ui.components.TrackOptionsBottomSheet
import com.example.musicon.ui.theme.LavenderTitle
import com.example.musicon.ui.viewmodel.MainViewModel
import com.example.musicon.logic.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.musicon.ui.components.TechnicalInfoPopup
import com.example.musicon.ui.components.HeaderStatusPill
import com.example.musicon.ui.components.SyncProgressBar
import com.example.musicon.ui.components.RenameDialog
import com.example.musicon.ui.components.CreatePlaylistDialog
import com.example.musicon.ui.components.AddToPlaylistDialog
import com.example.musicon.ui.components.EditTrackDialog

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
    val isOnline by viewModel.isOnline.collectAsState()
    val syncStatus by CloudSyncManager.status.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val backgroundMode by viewModel.backgroundMode.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTrackOptions by remember { mutableStateOf<TrackEntity?>(null) }
    var currentPlaylistDetail by remember { mutableStateOf<Playlist?>(null) }
    var showTrackInfoDialog by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToRename by remember { mutableStateOf<TrackEntity?>(null) }
    var showEditTrackDialog by remember { mutableStateOf<TrackEntity?>(null) }

    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    var selectedPlaylistIds by remember { mutableStateOf(setOf<String>()) }
    
    val isTrackSelectionMode = selectedTrackIds.isNotEmpty()
    val isPlaylistSelectionMode = selectedPlaylistIds.isNotEmpty()
    
    var addingToPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showBulkPlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }

    val tabs = remember(customFolders) { 
        listOf("Recents", "All Songs", "Playlists", "Albums", "Artists", "Genres") + 
        customFolders.map { it.substringAfterLast("/").ifBlank { "Folder" } }
    }
    
    val pagerState = rememberPagerState(initialPage = 1) { tabs.size }
    
    BackHandler(isTrackSelectionMode || isPlaylistSelectionMode || searchQuery.isNotEmpty() || currentPlaylistDetail != null || isSearchActive) {
        if (isTrackSelectionMode) { selectedTrackIds = emptySet(); addingToPlaylistId = null }
        else if (isPlaylistSelectionMode) { selectedPlaylistIds = emptySet() }
        else if (isSearchActive) { isSearchActive = false; viewModel.updateSearchQuery("") }
        else if (currentPlaylistDetail != null) currentPlaylistDetail = null
    }

    if (currentPlaylistDetail != null && addingToPlaylistId == null) {
        PlaylistDetailScreen(
            playlist = currentPlaylistDetail!!, 
            viewModel = viewModel, 
            onBack = { currentPlaylistDetail = null }, 
            viewMode = viewMode, 
            isLandscape = isLandscape,
            onAddSongs = { addingToPlaylistId = currentPlaylistDetail!!.id; scope.launch { pagerState.animateScrollToPage(1) } },
            onOpenCutter = onOpenCutter
        )
    } else {
        StellarBackground(themeMode = themeMode, backgroundMode = backgroundMode) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    if (isTrackSelectionMode || addingToPlaylistId != null) {
                        SelectionTopBar(count = selectedTrackIds.size, onClose = { selectedTrackIds = emptySet(); addingToPlaylistId = null }, onSelectAll = { val all = tracks.map { it.id }.toSet(); selectedTrackIds = if (selectedTrackIds.size == all.size) emptySet() else all }, title = if (addingToPlaylistId != null) "Add to Playlist" else "selected")
                    } else if (isPlaylistSelectionMode) {
                        SelectionTopBar(count = selectedPlaylistIds.size, onClose = { selectedPlaylistIds = emptySet() }, onSelectAll = { val all = playlists.map { it.id }.toSet(); selectedPlaylistIds = if (selectedPlaylistIds.size == all.size) emptySet() else all }, title = "selected")
                    } else {
                        LibraryTopBar(searchQuery, isSearchActive, { isSearchActive = !isSearchActive }, { viewModel.updateSearchQuery(it) }, onOpenDrawer, onOpenSettings, { showSortMenu = true }, { viewModel.updateLibraryViewMode(if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST) }, viewMode, viewModel, isOnline, syncStatus)
                    }
                },
                bottomBar = {
                    if (addingToPlaylistId != null) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth().height(70.dp)) {
                            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { viewModel.bulkAddTracksToPlaylist(addingToPlaylistId!!, selectedTrackIds.toList()); selectedTrackIds = emptySet(); addingToPlaylistId = null }, enabled = selectedTrackIds.isNotEmpty()) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Add ${selectedTrackIds.size} songs") }
                            }
                        }
                    } else if (isTrackSelectionMode) {
                        AnimatedVisibility(visible = true, enter = expandVertically(), exit = shrinkVertically()) {
                            SelectionBottomBar(onPlay = { viewModel.playSelected(allTracks.filter { it.id in selectedTrackIds }); selectedTrackIds = emptySet() }, onNext = { viewModel.addToQueueNext(allTracks.filter { it.id in selectedTrackIds }); selectedTrackIds = emptySet() }, onShare = { viewModel.shareTracks(allTracks.filter { it.id in selectedTrackIds }); selectedTrackIds = emptySet() }, onDelete = { viewModel.bulkDelete(allTracks.filter { it.id in selectedTrackIds }); selectedTrackIds = emptySet() })
                        }
                    } else if (isPlaylistSelectionMode) {
                        Surface(color = Color(0xFF1E1B36), modifier = Modifier.fillMaxWidth().height(70.dp)) {
                            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                if (selectedPlaylistIds.size == 1) {
                                    IconButton(onClick = { playlistToRename = playlists.find { it.id == selectedPlaylistIds.first() } }) { Icon(Icons.Default.Edit, null, tint = Color.White) }
                                }
                                IconButton(onClick = { viewModel.bulkDeletePlaylists(playlists.filter { it.id in selectedPlaylistIds }); selectedPlaylistIds = emptySet() }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { scope.launch { isRefreshing = true; viewModel.scanLocalStorage(); delay(1000); isRefreshing = false } }, modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        if (!isTrackSelectionMode && !isPlaylistSelectionMode && addingToPlaylistId == null) {
                            ScrollableTabRow(selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent, edgePadding = 16.dp, divider = {}, indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(it[pagerState.currentPage]), color = MaterialTheme.colorScheme.primary) }) {
                                tabs.forEachIndexed { index, title -> val isSelected = pagerState.currentPage == index; Tab(selected = isSelected, onClick = { scope.launch { pagerState.animateScrollToPage(index) } }, text = { Text(title, color = if (isSelected) Color.White else Color.Gray) }) }
                            }
                        }
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            when (page) {
                                0 -> SongsTab(viewModel.sessionRecentlyPlayed.collectAsState().value, selectedTrackIds, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isTrackSelectionMode || addingToPlaylistId != null) selectedTrackIds = if (it.id in selectedTrackIds) selectedTrackIds - it.id else selectedTrackIds + it.id else viewModel.playTrackList(viewModel.sessionRecentlyPlayed.value, it) }, { if (!isTrackSelectionMode) selectedTrackIds = setOf(it.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) }, { showTrackInfoDialog = it })
                                1 -> SongsTab(tracks, selectedTrackIds, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isTrackSelectionMode || addingToPlaylistId != null) selectedTrackIds = if (it.id in selectedTrackIds) selectedTrackIds - it.id else selectedTrackIds + it.id else viewModel.playTrackList(tracks, it) }, { if (!isTrackSelectionMode) selectedTrackIds = setOf(it.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) }, { showTrackInfoDialog = it })
                                2 -> PlaylistsTab(playlists, selectedPlaylistIds, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isPlaylistSelectionMode) selectedPlaylistIds = if (it.id in selectedPlaylistIds) selectedPlaylistIds - it.id else selectedPlaylistIds + it.id else currentPlaylistDetail = it }, { showCreatePlaylistDialog = true }, { selectedPlaylistIds = setOf(it.id) }, { pid -> addingToPlaylistId = pid; scope.launch { pagerState.animateScrollToPage(1) } }, viewModel)
                                3 -> GroupedTab(allTracks, "Album", viewMode, rememberLazyListState(), { viewModel.playTrackList(allTracks, it.first()) }, viewModel)
                                4 -> GroupedTab(allTracks, "Artist", viewMode, rememberLazyListState(), { viewModel.playTrackList(allTracks, it.first()) }, viewModel)
                                5 -> GroupedTab(allTracks, "Genre", viewMode, rememberLazyListState(), { viewModel.playTrackList(allTracks, it.first()) }, viewModel)
                                else -> {
                                    val path = customFolders[page - 6]
                                    val fTracks = allTracks.filter { it.localPath?.startsWith(path) == true }
                                    SongsTab(fTracks, selectedTrackIds, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isTrackSelectionMode || addingToPlaylistId != null) selectedTrackIds = if (it.id in selectedTrackIds) selectedTrackIds - it.id else selectedTrackIds + it.id else viewModel.playTrackList(fTracks, it) }, { if (!isTrackSelectionMode) selectedTrackIds = setOf(it.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) }, { showTrackInfoDialog = it })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortMenu) SortMenu(currentOrder = sortOrder, onDismiss = { showSortMenu = false }, onSortSelected = { viewModel.updateSongSortOrder(it) })
    if (selectedTrackOptions != null) {
        TrackOptionsBottomSheet(track = selectedTrackOptions!!, onDismiss = { selectedTrackOptions = null }, onAction = { action ->
            when (action) {
                "favorite" -> viewModel.toggleFavorite(selectedTrackOptions!!)
                "play" -> viewModel.playTrack(selectedTrackOptions!!)
                "play_next" -> viewModel.addToQueueNext(listOf(selectedTrackOptions!!))
                "add_to_queue" -> viewModel.addToQueue(selectedTrackOptions!!)
                "info" -> showTrackInfoDialog = selectedTrackOptions
                "share" -> viewModel.shareTrack(selectedTrackOptions!!)
                "delete" -> viewModel.bulkDelete(listOf(selectedTrackOptions!!))
                "upload" -> viewModel.uploadTrack(selectedTrackOptions!!)
                "remove" -> viewModel.removeFromLibrary(listOf(selectedTrackOptions!!))
                "rename" -> trackToRename = selectedTrackOptions
                "ringtone" -> viewModel.setAsRingtone(selectedTrackOptions!!)
                "cut" -> onOpenCutter(selectedTrackOptions!!)
                "add_to_playlist" -> showBulkPlaylistDialog = true
                "edit" -> showEditTrackDialog = selectedTrackOptions
                "location" -> viewModel.openFileLocation(selectedTrackOptions!!)
                "cloud_delete" -> viewModel.deleteTrackFromCloud(selectedTrackOptions!!)
            }
            if (action != "add_to_playlist") selectedTrackOptions = null
        })
    }
    if (showBulkPlaylistDialog && selectedTrackOptions != null) {
        AddToPlaylistDialog(playlists = playlists, onDismiss = { showBulkPlaylistDialog = false; selectedTrackOptions = null }, onPlaylistSelected = { pid -> viewModel.bulkAddTracksToPlaylist(pid, listOf(selectedTrackOptions!!.id)); showBulkPlaylistDialog = false; selectedTrackOptions = null }, onCreateNew = { showBulkPlaylistDialog = false; showCreatePlaylistDialog = true })
    }
    if (showTrackInfoDialog != null) TechnicalInfoPopup(track = showTrackInfoDialog!!, onDismiss = { showTrackInfoDialog = null })
    if (trackToRename != null) RenameDialog(initialName = trackToRename!!.displayName, onDismiss = { trackToRename = null }, onConfirm = { viewModel.updateTrackMetadata(trackToRename!!.id, it, null, null, null, null); trackToRename = null })
    if (showCreatePlaylistDialog) CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false }, onConfirm = { viewModel.createPlaylist(it); showCreatePlaylistDialog = false })
    if (playlistToRename != null) RenameDialog(initialName = playlistToRename!!.name, onDismiss = { playlistToRename = null }, onConfirm = { viewModel.renamePlaylist(playlistToRename!!.id, it); playlistToRename = null; selectedPlaylistIds = emptySet() })
    if (showEditTrackDialog != null) EditTrackDialog(track = showEditTrackDialog!!, onDismiss = { showEditTrackDialog = null }, onConfirm = { t, ar, al, c, l -> viewModel.updateTrackMetadata(showEditTrackDialog!!.id, t, ar, al, c, l); showEditTrackDialog = null })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(playlist: Playlist, viewModel: MainViewModel, onBack: () -> Unit, viewMode: LibraryViewMode, isLandscape: Boolean, onAddSongs: () -> Unit, onOpenCutter: (TrackEntity) -> Unit) {
    val tracks by viewModel.getTracksForPlaylist(playlist.id).collectAsState(emptyList())
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedTrackOptions by remember { mutableStateOf<TrackEntity?>(null) }
    var subViewMode by rememberSaveable { mutableStateOf(LibraryViewMode.GRID) }
    var showTrackInfoDialog by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToRename by remember { mutableStateOf<TrackEntity?>(null) }
    val isSelectionMode = selectedIds.isNotEmpty()
    BackHandler(isSelectionMode) { selectedIds = emptySet() }
    StellarBackground {
        Scaffold(
            containerColor = Color.Transparent, 
            topBar = { 
                if (isSelectionMode) SelectionTopBar(count = selectedIds.size, onClose = { selectedIds = emptySet() }, onSelectAll = { val all = tracks.map { it.id }.toSet(); selectedIds = if (selectedIds.size == all.size) emptySet() else all }, title = "selected" )
                else CenterAlignedTopAppBar(title = { Text(playlist.name, color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, actions = { IconButton(onClick = { subViewMode = if (subViewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST }) { Icon(if (subViewMode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List, null, tint = Color.White) }; IconButton(onClick = onAddSongs) { Icon(Icons.Default.Add, null, tint = Color.White) } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent))
            },
            bottomBar = {
                AnimatedVisibility(visible = isSelectionMode, enter = expandVertically(), exit = shrinkVertically()) {
                    SelectionBottomBar(onPlay = { viewModel.playSelected(tracks.filter { it.id in selectedIds }); selectedIds = emptySet() }, onNext = { viewModel.addToQueueNext(tracks.filter { it.id in selectedIds }); selectedIds = emptySet() }, onShare = { viewModel.shareTracks(tracks.filter { it.id in selectedIds }); selectedIds = emptySet() }, onDelete = { viewModel.bulkDelete(tracks.filter { it.id in selectedIds }); selectedIds = emptySet() })
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (tracks.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Button(onClick = onAddSongs) { Text("Add Songs") } }
                else {
                    if (subViewMode == LibraryViewMode.GRID) LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 130.dp), modifier = Modifier.fillMaxSize()) { items(tracks) { track -> StellarGridItem(track, track.id in selectedIds, { if (isSelectionMode) selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id else viewModel.playTrackList(tracks, track, playlist.id) }, { if (!isSelectionMode) selectedIds = setOf(track.id) }, { selectedTrackOptions = track }, { viewModel.toggleFavorite(track) }, { showTrackInfoDialog = it } ) } }
                    else LazyColumn(Modifier.fillMaxSize()) { items(tracks) { track -> StellarTrackItem(track, track.id in selectedIds, { if (isSelectionMode) selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id else viewModel.playTrackList(tracks, track, playlist.id) }, { if (!isSelectionMode) selectedIds = setOf(track.id) }, { selectedTrackOptions = track }, { viewModel.toggleFavorite(track) }, { showTrackInfoDialog = it } ) } }
                }
            }
        }
        if (showTrackInfoDialog != null) TechnicalInfoPopup(track = showTrackInfoDialog!!, onDismiss = { showTrackInfoDialog = null })
        if (selectedTrackOptions != null) {
            TrackOptionsBottomSheet(track = selectedTrackOptions!!, onDismiss = { selectedTrackOptions = null }, onAction = { action ->
                when (action) {
                    "favorite" -> viewModel.toggleFavorite(selectedTrackOptions!!)
                    "play" -> viewModel.playTrack(selectedTrackOptions!!)
                    "play_next" -> viewModel.addToQueueNext(listOf(selectedTrackOptions!!))
                    "add_to_queue" -> viewModel.addToQueue(selectedTrackOptions!!)
                    "info" -> showTrackInfoDialog = selectedTrackOptions
                    "share" -> viewModel.shareTrack(selectedTrackOptions!!)
                    "delete" -> viewModel.bulkDelete(listOf(selectedTrackOptions!!))
                    "upload" -> viewModel.uploadTrack(selectedTrackOptions!!)
                    "remove" -> viewModel.removeTrackFromPlaylist(playlist.id, selectedTrackOptions!!.id)
                    "rename" -> trackToRename = selectedTrackOptions
                    "ringtone" -> viewModel.setAsRingtone(selectedTrackOptions!!)
                    "cut" -> onOpenCutter(selectedTrackOptions!!)
                    "location" -> viewModel.openFileLocation(selectedTrackOptions!!)
                    "cloud_delete" -> viewModel.deleteTrackFromCloud(selectedTrackOptions!!)
                }
                selectedTrackOptions = null
            })
        }
        if (trackToRename != null) RenameDialog(initialName = trackToRename!!.displayName, onDismiss = { trackToRename = null }, onConfirm = { viewModel.updateTrackMetadata(trackToRename!!.id, it, null, null, null, null); trackToRename = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(q: String, active: Boolean, onToggle: () -> Unit, onChange: (String) -> Unit, onDrawer: () -> Unit, onSettings: () -> Unit, onSort: () -> Unit, onMode: () -> Unit, mode: LibraryViewMode, viewModel: MainViewModel, online: Boolean, sync: SyncStatus) {
    TopAppBar(
        title = { if (active) TextField(value = q, onValueChange = onChange, placeholder = { Text("Search...") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)) else Text("Nirvaana", color = Color.White, fontWeight = FontWeight.Bold) }, 
        navigationIcon = { IconButton(onClick = onDrawer) { Icon(Icons.Default.Menu, null, tint = Color.White) } }, 
        actions = { 
            IconButton(onClick = onToggle) { Icon(if (active) Icons.Default.Close else Icons.Default.Search, null, tint = Color.White) }
            IconButton(onClick = onMode) { Icon(if (mode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List, null, tint = Color.White) }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, null, tint = Color.White) } 
        }, 
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        // Removed custom WindowInsets to fix overlap
    )
}

@Composable fun SongsTab(tracks: List<TrackEntity>, selected: Set<String>, mode: LibraryViewMode, listState: LazyListState, gridState: LazyGridState, onClick: (TrackEntity) -> Unit, onLong: (TrackEntity) -> Unit, onOptions: (TrackEntity) -> Unit, onFav: (TrackEntity) -> Unit, onInfo: (TrackEntity) -> Unit) {
    if (mode == LibraryViewMode.GRID) LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(minSize = 110.dp), modifier = Modifier.fillMaxSize()) { items(tracks) { StellarGridItem(it, it.id in selected, { onClick(it) }, { onLong(it) }, onOptions, onFav, onInfo) } }
    else LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) { items(tracks) { StellarTrackItem(it, it.id in selected, { onClick(it) }, { onLong(it) }, onOptions, onFav, onInfo) } }
}

@Composable fun PlaylistsTab(playlists: List<Playlist>, selected: Set<String>, mode: LibraryViewMode, listState: LazyListState, gridState: LazyGridState, onClick: (Playlist) -> Unit, onCreate: () -> Unit, onLong: (Playlist) -> Unit, onAdd: (String) -> Unit, viewModel: MainViewModel) {
    var expandedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        item { 
            ListItem(
                headlineContent = { Text("Create New Playlist", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }, 
                leadingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }, 
                modifier = Modifier.clickable { onCreate() }, 
                colors = ListItemDefaults.colors(containerColor = Color.White.copy(alpha = 0.05f))
            ) 
        }
        items(playlists) { playlist -> 
            val isSelected = playlist.id in selected
            val isExpanded = expandedPlaylistId == playlist.id
            
            Column {
                ListItem(
                    headlineContent = { Text(playlist.name, color = Color.White) }, 
                    leadingContent = { Icon(getPlaylistIcon(playlist.name), null, tint = Color.White) },
                    trailingContent = {
                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = { expandedPlaylistId = if (isExpanded) null else playlist.id }, 
                        onLongClick = { onLong(playlist) }
                    ),
                    colors = ListItemDefaults.colors(containerColor = if (isSelected) Color.White.copy(0.1f) else Color.Transparent)
                )
                
                AnimatedVisibility(visible = isExpanded) {
                    val tracks by viewModel.getTracksForPlaylist(playlist.id).collectAsState(emptyList())
                    Column(Modifier.padding(start = 16.dp).background(Color.White.copy(0.03f))) {
                        if (tracks.isEmpty()) {
                            Text("No songs in this playlist", color = Color.Gray, modifier = Modifier.padding(16.dp), fontSize = 12.sp)
                        } else {
                            tracks.forEach { track ->
                                StellarTrackItem(
                                    track = track,
                                    isSelected = false,
                                    onPlay = { viewModel.playTrackList(tracks, track, playlist.id) },
                                    onLongClick = { },
                                    onOptions = { },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onInfo = { }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable fun GroupedTab(allTracks: List<TrackEntity>, type: String, mode: LibraryViewMode, listState: LazyListState, onPlay: (List<TrackEntity>) -> Unit, viewModel: MainViewModel) {
    val grouped = remember(allTracks, type) { 
        when(type) { 
            "Album" -> allTracks.groupBy { it.displayAlbum }
            "Artist" -> allTracks.groupBy { it.displayArtist }
            else -> allTracks.groupBy { it.genre ?: "Unknown" } 
        } 
    }
    var expandedGroupName by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) { 
        grouped.forEach { (name, gTracks) -> 
            val isExpanded = expandedGroupName == name
            item { 
                Column {
                    ListItem(
                        headlineContent = { Text(name ?: "Unknown", color = Color.White) }, 
                        supportingContent = { Text("${gTracks.size} songs") },
                        trailingContent = {
                            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
                        },
                        modifier = Modifier.clickable { expandedGroupName = if (isExpanded) null else name }
                    )
                    
                    AnimatedVisibility(visible = isExpanded) {
                        Column(Modifier.padding(start = 16.dp).background(Color.White.copy(0.03f))) {
                            gTracks.forEach { track ->
                                StellarTrackItem(
                                    track = track,
                                    isSelected = false,
                                    onPlay = { viewModel.playTrackList(gTracks, track) },
                                    onLongClick = { },
                                    onOptions = { },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onInfo = { }
                                )
                            }
                        }
                    }
                }
            } 
        } 
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun StellarTrackItem(track: TrackEntity, isSelected: Boolean, onPlay: () -> Unit, onLongClick: () -> Unit, onOptions: (TrackEntity) -> Unit, onToggleFavorite: (TrackEntity) -> Unit, onInfo: (TrackEntity) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color.White.copy(0.1f) else Color.Transparent).combinedClickable(onClick = onPlay, onLongClick = onLongClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) { 
        Box {
            AsyncImage(model = track.customCoverPath ?: track.localPath, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            if (track.gDriveId != null) Icon(Icons.Default.Cloud, null, tint = Color.Cyan, modifier = Modifier.size(16.dp).align(Alignment.TopStart).background(Color.Black.copy(0.4f), CircleShape).padding(2.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) { 
            Text(track.displayName, color = LavenderTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.displayArtist, color = Color.Gray, style = MaterialTheme.typography.labelSmall) 
        }
        
        IconButton(onClick = { onToggleFavorite(track) }) {
            Icon(
                if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (track.isFavorite) Color.Red else Color.White.copy(alpha = 0.5f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { onOptions(track) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
            Spacer(Modifier.height(8.dp))
            IconButton(onClick = { onInfo(track) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun StellarGridItem(track: TrackEntity, isSelected: Boolean, onPlay: (TrackEntity) -> Unit, onLongClick: (TrackEntity) -> Unit, onOptions: (TrackEntity) -> Unit = {}, onFav: (TrackEntity) -> Unit = {}, onInfo: (TrackEntity) -> Unit = {}) {
    Column(modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color.White.copy(0.05f) else Color.Transparent).combinedClickable(onClick = { onPlay(track) }, onLongClick = { onLongClick(track) }).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) { 
        Box {
            AsyncImage(model = track.customCoverPath ?: track.localPath, contentDescription = null, modifier = Modifier.aspectRatio(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            if (track.gDriveId != null) Icon(Icons.Default.Cloud, null, tint = Color.Cyan, modifier = Modifier.size(20.dp).align(Alignment.TopStart).background(Color.Black.copy(0.4f), CircleShape).padding(4.dp))
            
            IconButton(onClick = { onFav(track) }, modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp).size(28.dp)) {
                Icon(if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (track.isFavorite) Color.Red else Color.White, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = { onOptions(track) }, modifier = Modifier.align(Alignment.TopEnd).size(32.dp).padding(4.dp).background(Color.Black.copy(0.3f), CircleShape)) { Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = { onInfo(track) }, modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).padding(4.dp).background(Color.Black.copy(0.3f), CircleShape)) { Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(track.displayName, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun PlaylistGridItem(p: Playlist, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color.White.copy(0.1f) else Color.Transparent).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) { Icon(getPlaylistIcon(p.name), null, tint = Color.Gray, modifier = Modifier.size(48.dp)) }
        Text(p.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SelectionTopBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit, title: String) {
    TopAppBar(title = { Text(if (title == "selected") "$count Selected" else title) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) } }, actions = { IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, null) } })
}

@Composable fun SelectionBottomBar(onPlay: () -> Unit, onNext: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    Surface(color = Color(0xFF1E1B36), modifier = Modifier.fillMaxWidth().height(70.dp)) { Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, null, tint = Color.White) }; IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = Color.White) }; IconButton(onClick = onShare) { Icon(Icons.Default.Share, null, tint = Color.White) }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) } } }
}

@Composable fun SortMenu(currentOrder: String, onDismiss: () -> Unit, onSortSelected: (String) -> Unit) { 
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Sort By") }, text = { Column { listOf("Name A-Z" to "NAME_ASC", "Name Z-A" to "NAME_DESC").forEach { (label, value) -> TextButton(onClick = { onSortSelected(value); onDismiss() }) { Text(label, color = if (currentOrder == value) Color.Cyan else Color.White) } } } }, confirmButton = { Button(onClick = onDismiss) { Text("Close") } })
}

private fun getPlaylistIcon(name: String): ImageVector {
    val icons = listOf(
        Icons.Default.QueueMusic, Icons.Default.MusicNote, Icons.Default.Album, 
        Icons.Default.Person, Icons.Default.History, Icons.Default.Audiotrack,
        Icons.Default.Headphones, Icons.Default.LibraryMusic, Icons.Default.Radio,
        Icons.Default.Piano, Icons.Default.Mic, Icons.Default.GraphicEq,
        Icons.Default.Speaker, Icons.AutoMirrored.Filled.VolumeUp, Icons.Default.MusicVideo
    )
    if (name.lowercase().contains("fav")) return Icons.Default.Favorite
    if (name.lowercase().contains("recent")) return Icons.Default.History
    if (name.lowercase().contains("cloud")) return Icons.Default.Cloud
    return icons[Math.abs(name.hashCode()) % icons.size]
}
