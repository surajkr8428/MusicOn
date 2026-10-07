package com.example.musicon.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import com.example.musicon.ui.components.MultiSelectSongDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import com.example.musicon.ui.components.PlaylistOptionsBottomSheet

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
    val currentPlayingTrack by viewModel.currentPlayingTrack.collectAsState()
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
    var selectedPlaylistOptions by remember { mutableStateOf<Playlist?>(null) }
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
                                Button(
                                    onClick = { 
                                        val targetPid = addingToPlaylistId!!
                                        viewModel.bulkAddTracksToPlaylist(targetPid, selectedTrackIds.toList())
                                        selectedTrackIds = emptySet()
                                        addingToPlaylistId = null 
                                        playlists.find { it.id == targetPid }?.let { currentPlaylistDetail = it }
                                    }, 
                                    enabled = selectedTrackIds.isNotEmpty()
                                ) { 
                                    Icon(Icons.Default.Check, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add ${selectedTrackIds.size} songs") 
                                }
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
                            val currentTrackId = currentPlayingTrack?.id
                            when (page) {
                                0 -> Column(Modifier.fillMaxSize()) {
                                    val recents = viewModel.recentlyPlayed.collectAsState().value
                                    if (recents.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${recents.size} Recently Played", 
                                                style = MaterialTheme.typography.labelMedium, 
                                                color = Color.Gray
                                            )
                                            TextButton(onClick = { viewModel.clearRecentlyPlayed() }) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Reset Recents", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Reset Recents", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    SongsTab(recents, selectedTrackIds, currentTrackId, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isTrackSelectionMode || addingToPlaylistId != null) selectedTrackIds = if (it.id in selectedTrackIds) selectedTrackIds - it.id else selectedTrackIds + it.id else viewModel.playTrackList(recents, it) }, { if (!isTrackSelectionMode) selectedTrackIds = setOf(it.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) }, { showTrackInfoDialog = it }, { viewModel.uploadTrack(it) })
                                }
                                1 -> SongsTab(tracks, selectedTrackIds, currentTrackId, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isTrackSelectionMode || addingToPlaylistId != null) selectedTrackIds = if (it.id in selectedTrackIds) selectedTrackIds - it.id else selectedTrackIds + it.id else viewModel.playTrackList(tracks, it) }, { if (!isTrackSelectionMode) selectedTrackIds = setOf(it.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) }, { showTrackInfoDialog = it }, { viewModel.uploadTrack(it) })
                                2 -> PlaylistsTab(playlists, selectedPlaylistIds, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isPlaylistSelectionMode) selectedPlaylistIds = if (it.id in selectedPlaylistIds) selectedPlaylistIds - it.id else selectedPlaylistIds + it.id else { /* Expansion handles click */ } }, { showCreatePlaylistDialog = true }, { selectedPlaylistIds = setOf(it.id) }, { pid -> addingToPlaylistId = pid; scope.launch { pagerState.animateScrollToPage(1) } }, viewModel, selectedTrackIds, { id -> selectedTrackIds = if (id in selectedTrackIds) selectedTrackIds - id else selectedTrackIds + id }, { selectedPlaylistOptions = it }, { selectedTrackOptions = it }, { showTrackInfoDialog = it }, { pid -> addingToPlaylistId = pid; scope.launch { pagerState.animateScrollToPage(1) } })
                                3 -> GroupedTab(allTracks, "Album", viewMode, rememberLazyListState(), { viewModel.playTrackList(allTracks, it.first()) }, viewModel, selectedTrackIds, { id -> selectedTrackIds = if (id in selectedTrackIds) selectedTrackIds - id else selectedTrackIds + id }, { selectedTrackOptions = it }, { showTrackInfoDialog = it })
                                4 -> GroupedTab(allTracks, "Artist", viewMode, rememberLazyListState(), { viewModel.playTrackList(allTracks, it.first()) }, viewModel, selectedTrackIds, { id -> selectedTrackIds = if (id in selectedTrackIds) selectedTrackIds - id else selectedTrackIds + id }, { selectedTrackOptions = it }, { showTrackInfoDialog = it })
                                5 -> GroupedTab(allTracks, "Genre", viewMode, rememberLazyListState(), { viewModel.playTrackList(allTracks, it.first()) }, viewModel, selectedTrackIds, { id -> selectedTrackIds = if (id in selectedTrackIds) selectedTrackIds - id else selectedTrackIds + id }, { selectedTrackOptions = it }, { showTrackInfoDialog = it })
                                else -> {
                                    val path = customFolders[page - 6]
                                    val fTracks = allTracks.filter { it.localPath?.startsWith(path) == true }
                                    SongsTab(fTracks, selectedTrackIds, currentTrackId, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isTrackSelectionMode || addingToPlaylistId != null) selectedTrackIds = if (it.id in selectedTrackIds) selectedTrackIds - it.id else selectedTrackIds + it.id else viewModel.playTrackList(fTracks, it) }, { if (!isTrackSelectionMode) selectedTrackIds = setOf(it.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) }, { showTrackInfoDialog = it }, { viewModel.uploadTrack(it) })
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
                "info" -> showTrackInfoDialog = selectedTrackOptions
                "share" -> viewModel.shareTrack(selectedTrackOptions!!)
                "delete" -> viewModel.bulkDelete(listOf(selectedTrackOptions!!))
                "upload" -> viewModel.uploadTrack(selectedTrackOptions!!)
                "remove" -> { /* Handled contextually */ }
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
    if (selectedPlaylistOptions != null) {
        PlaylistOptionsBottomSheet(playlist = selectedPlaylistOptions!!, onDismiss = { selectedPlaylistOptions = null }, onAction = { action ->
            when (action) {
                "play" -> {
                    val p = selectedPlaylistOptions!!
                    scope.launch {
                        val pt = viewModel.getTracksForPlaylist(p.id).first()
                        if (pt.isNotEmpty()) viewModel.playTrackList(pt, pt.first(), p.id)
                    }
                }
                "rename" -> playlistToRename = selectedPlaylistOptions
                "delete" -> viewModel.bulkDeletePlaylists(listOf(selectedPlaylistOptions!!))
            }
            selectedPlaylistOptions = null
        })
    }
    if (showBulkPlaylistDialog && selectedTrackOptions != null) {
        AddToPlaylistDialog(
            playlists = playlists, 
            onDismiss = { showBulkPlaylistDialog = false; selectedTrackOptions = null }, 
            onPlaylistSelected = { pid -> 
                viewModel.bulkAddTracksToPlaylist(pid, listOf(selectedTrackOptions!!.id))
                playlists.find { it.id == pid }?.let { currentPlaylistDetail = it }
                showBulkPlaylistDialog = false
                selectedTrackOptions = null 
            }, 
            onCreateNew = { showBulkPlaylistDialog = false; showCreatePlaylistDialog = true } 
        )
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
    val currentPlayingTrack by viewModel.currentPlayingTrack.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedTrackOptions by remember { mutableStateOf<TrackEntity?>(null) }
    var subViewMode by rememberSaveable { mutableStateOf(LibraryViewMode.GRID) }
    var showTrackInfoDialog by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToRename by remember { mutableStateOf<TrackEntity?>(null) }
    val isSelectionMode = selectedIds.isNotEmpty()

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var hasAutoScrolled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress, gridState.isScrollInProgress) {
        if (listState.isScrollInProgress || gridState.isScrollInProgress) {
            hasAutoScrolled = true
        }
    }

    LaunchedEffect(Unit) {
        val targetId = currentPlayingTrack?.id
        if (!hasAutoScrolled && targetId != null && tracks.isNotEmpty()) {
            val targetIndex = tracks.indexOfFirst { it.id == targetId }
            if (targetIndex >= 0) {
                try {
                    if (subViewMode == LibraryViewMode.GRID) {
                        gridState.animateScrollToItem(targetIndex)
                    } else {
                        listState.animateScrollToItem(targetIndex)
                    }
                    hasAutoScrolled = true
                } catch (_: Exception) {}
            }
        }
    }

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
            val detailConfig = LocalConfiguration.current
            val isDetailTablet = detailConfig.screenWidthDp >= 600 || detailConfig.smallestScreenWidthDp >= 600
            val isDetailLandscape = detailConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            val detailGridColumns = when {
                isDetailTablet && isDetailLandscape -> GridCells.Fixed(12)
                isDetailTablet -> GridCells.Fixed(8)
                else -> GridCells.Fixed(4)
            }

            Box(Modifier.fillMaxSize().padding(padding)) {
                if (tracks.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Button(onClick = onAddSongs) { Text("Add Songs") } }
                else {
                    if (subViewMode == LibraryViewMode.GRID) LazyVerticalGrid(state = gridState, columns = detailGridColumns, modifier = Modifier.fillMaxSize()) { items(tracks) { track -> StellarGridItem(track, track.id in selectedIds, track.id == currentPlayingTrack?.id, { if (isSelectionMode) selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id else viewModel.playTrackList(tracks, track, playlist.id) }, { if (!isSelectionMode) selectedIds = setOf(track.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) }, { showTrackInfoDialog = it }, { viewModel.uploadTrack(it) } ) } }
                    else LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) { items(tracks) { track -> StellarTrackItem(track, track.id in selectedIds, track.id == currentPlayingTrack?.id, { if (isSelectionMode) selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id else viewModel.playTrackList(tracks, track, playlist.id) }, { if (!isSelectionMode) selectedIds = setOf(track.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) }, { showTrackInfoDialog = it }, { viewModel.uploadTrack(it) } ) } }
                }
            }
        }
        if (showTrackInfoDialog != null) TechnicalInfoPopup(track = showTrackInfoDialog!!, onDismiss = { showTrackInfoDialog = null })
        if (selectedTrackOptions != null) {
            TrackOptionsBottomSheet(track = selectedTrackOptions!!, onDismiss = { selectedTrackOptions = null }, onAction = { action ->
                when (action) {
                    "favorite" -> viewModel.toggleFavorite(selectedTrackOptions!!)
                    "play" -> viewModel.playTrackList(tracks, selectedTrackOptions!!, playlist.id)
                    "play_next" -> viewModel.addToQueueNext(listOf(selectedTrackOptions!!))
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        windowInsets = WindowInsets.statusBars
    )
}

@Composable fun SongsTab(
    tracks: List<TrackEntity>, 
    selected: Set<String>, 
    currentPlayingTrackId: String? = null,
    mode: LibraryViewMode = LibraryViewMode.LIST, 
    listState: LazyListState = rememberLazyListState(), 
    gridState: LazyGridState = rememberLazyGridState(), 
    onClick: (TrackEntity) -> Unit = {}, 
    onLong: (TrackEntity) -> Unit = {}, 
    onOptions: (TrackEntity) -> Unit = {}, 
    onFav: (TrackEntity) -> Unit = {}, 
    onInfo: (TrackEntity) -> Unit = {},
    onUpload: (TrackEntity) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var hasAutoScrolled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress, gridState.isScrollInProgress) {
        if (listState.isScrollInProgress || gridState.isScrollInProgress) {
            hasAutoScrolled = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAutoScrolled && currentPlayingTrackId != null && tracks.isNotEmpty()) {
            val targetIndex = tracks.indexOfFirst { it.id == currentPlayingTrackId }
            if (targetIndex >= 0) {
                try {
                    if (mode == LibraryViewMode.GRID) {
                        gridState.animateScrollToItem(targetIndex)
                    } else {
                        listState.animateScrollToItem(targetIndex)
                    }
                    hasAutoScrolled = true
                } catch (_: Exception) {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val tabConfig = LocalConfiguration.current
        val isTabTablet = tabConfig.screenWidthDp >= 600 || tabConfig.smallestScreenWidthDp >= 600
        val isTabLandscape = tabConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        val tabGridColumns = when {
            isTabTablet && isTabLandscape -> GridCells.Fixed(12)
            isTabTablet -> GridCells.Fixed(8)
            else -> GridCells.Fixed(4)
        }

        if (mode == LibraryViewMode.GRID) {
            LazyVerticalGrid(
                state = gridState, 
                columns = tabGridColumns, 
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, end = 24.dp)
            ) { 
                items(tracks) { track -> 
                    StellarGridItem(track, track.id in selected, track.id == currentPlayingTrackId, { onClick(track) }, { onLong(track) }, onOptions, onFav, onInfo, onUpload) 
                } 
            }
            AlphabetFastScroller(
                tracks = tracks,
                onScrollTo = { targetIndex -> scope.launch { gridState.scrollToItem(targetIndex) } },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        } else {
            LazyColumn(
                state = listState, 
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, end = 24.dp)
            ) { 
                items(tracks) { track -> 
                    StellarTrackItem(track, track.id in selected, track.id == currentPlayingTrackId, { onClick(track) }, { onLong(track) }, onOptions, onFav, onInfo, onUpload) 
                } 
            }
            AlphabetFastScroller(
                tracks = tracks,
                onScrollTo = { targetIndex -> scope.launch { listState.scrollToItem(targetIndex) } },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable fun PlaylistsTab(playlists: List<Playlist>, selected: Set<String>, mode: LibraryViewMode, listState: LazyListState, gridState: LazyGridState, onClick: (Playlist) -> Unit, onCreate: () -> Unit, onLong: (Playlist) -> Unit, onAdd: (String) -> Unit, viewModel: MainViewModel, selectedTrackIds: Set<String>, onTrackClick: (String) -> Unit, onOptions: (Playlist) -> Unit, onTrackOptions: (TrackEntity) -> Unit, onTrackInfo: (TrackEntity) -> Unit, onAddSongsToPlaylist: (String) -> Unit) {
    var expandedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var subViewMode by rememberSaveable { mutableStateOf(LibraryViewMode.LIST) }

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
            val pTracks by viewModel.getTracksForPlaylist(playlist.id).collectAsState(emptyList())
            val currentPlayingTrack by viewModel.currentPlayingTrack.collectAsState()
            
            Column {
                ListItem(
                    headlineContent = { Text(playlist.name, color = Color.White) }, 
                    leadingContent = {
                        val firstTrack = pTracks.firstOrNull()
                        val coverPath = firstTrack?.customCoverPath ?: firstTrack?.localPath
                        if (coverPath != null) {
                            AsyncImage(
                                model = coverPath,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(getPlaylistIcon(playlist.name), null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onAddSongsToPlaylist(playlist.id) }) {
                                Icon(Icons.Default.Add, "Add Songs", tint = MaterialTheme.colorScheme.primary)
                            }
                            if (isExpanded) {
                                IconButton(onClick = { viewModel.shareTracks(pTracks) }) {
                                    Icon(Icons.Default.Share, "Share All", tint = Color.Gray)
                                }
                                IconButton(onClick = { subViewMode = if (subViewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST }) {
                                    Icon(if (subViewMode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List, null, tint = Color.Gray)
                                }
                            }
                            IconButton(onClick = { onOptions(playlist) }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                            IconButton(onClick = { expandedPlaylistId = if (isExpanded) null else playlist.id }) {
                                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = { expandedPlaylistId = if (isExpanded) null else playlist.id }, 
                        onLongClick = { onLong(playlist) }
                    ),
                    colors = ListItemDefaults.colors(containerColor = if (isSelected) Color.White.copy(0.1f) else Color.Transparent)
                )
                
                AnimatedVisibility(visible = isExpanded) {
                    val tracks = pTracks
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            if (tracks.isEmpty()) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No songs in this playlist", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { onAddSongsToPlaylist(playlist.id) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.2f))) {
                                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Songs", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            } else {
                                val playlistConfig = LocalConfiguration.current
                                val isPlaylistTablet = playlistConfig.screenWidthDp >= 600 || playlistConfig.smallestScreenWidthDp >= 600
                                val isPlaylistLandscape = playlistConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                                val playlistGridCols = when {
                                    isPlaylistTablet && isPlaylistLandscape -> GridCells.Fixed(12)
                                    isPlaylistTablet -> GridCells.Fixed(8)
                                    else -> GridCells.Fixed(4)
                                }

                                if (subViewMode == LibraryViewMode.GRID) {
                                    LazyVerticalGrid(
                                        columns = playlistGridCols,
                                        modifier = Modifier.heightIn(max = 2000.dp)
                                    ) {
                                        items(tracks) { track ->
                                            StellarGridItem(
                                                track = track,
                                                isSelected = track.id in selectedTrackIds,
                                                isPlaying = track.id == currentPlayingTrack?.id,
                                                onPlay = { if (selectedTrackIds.isNotEmpty()) onTrackClick(track.id) else viewModel.playTrackList(tracks, track, playlist.id) },
                                                onLongClick = { onTrackClick(track.id) },
                                                onOptions = onTrackOptions,
                                                onFav = { viewModel.toggleFavorite(it) },
                                                onInfo = onTrackInfo,
                                                onUpload = { viewModel.uploadTrack(it) }
                                            )
                                        }
                                    }
                                } else {
                                    Column {
                                        tracks.forEach { track ->
                                            StellarTrackItem(
                                                track = track,
                                                isSelected = track.id in selectedTrackIds,
                                                isPlaying = track.id == currentPlayingTrack?.id,
                                                onPlay = { if (selectedTrackIds.isNotEmpty()) onTrackClick(track.id) else viewModel.playTrackList(tracks, track, playlist.id) },
                                                onLongClick = { onTrackClick(track.id) },
                                                onOptions = onTrackOptions,
                                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                onInfo = onTrackInfo,
                                                onUpload = { viewModel.uploadTrack(it) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable fun GroupedTab(allTracks: List<TrackEntity>, type: String, mode: LibraryViewMode, listState: LazyListState, onPlay: (List<TrackEntity>) -> Unit, viewModel: MainViewModel, selectedTrackIds: Set<String>, onTrackClick: (String) -> Unit, onTrackOptions: (TrackEntity) -> Unit, onTrackInfo: (TrackEntity) -> Unit) {
    val grouped = remember(allTracks, type) { 
        when(type) { 
            "Album" -> allTracks.groupBy { it.displayAlbum }
            "Artist" -> allTracks.groupBy { it.displayArtist }
            else -> allTracks.groupBy { it.genre ?: "Unknown" } 
        } 
    }
    var expandedGroupName by rememberSaveable { mutableStateOf<String?>(null) }
    var subViewMode by rememberSaveable { mutableStateOf(LibraryViewMode.LIST) }
    var addSongsGroupTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    val currentPlayingTrack by viewModel.currentPlayingTrack.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) { 
            grouped.forEach { (name, gTracks) -> 
                val groupName = name ?: "Unknown"
                val isExpanded = expandedGroupName == groupName
                item { 
                    Column {
                        ListItem(
                            headlineContent = { Text(groupName, color = Color.White) }, 
                            supportingContent = { Text("${gTracks.size} songs") },
                            leadingContent = {
                                val firstTrack = gTracks.firstOrNull()
                                val coverPath = firstTrack?.customCoverPath ?: firstTrack?.localPath
                                if (coverPath != null) {
                                    AsyncImage(
                                        model = coverPath,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        when (type) {
                                            "Album" -> Icons.Default.Album
                                            "Artist" -> Icons.Default.Person
                                            else -> Icons.Default.Category
                                        },
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { addSongsGroupTarget = Pair(type, groupName) }) {
                                        Icon(Icons.Default.Add, "Add Songs", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    if (isExpanded) {
                                        IconButton(onClick = { viewModel.shareTracks(gTracks) }) {
                                            Icon(Icons.Default.Share, "Share All", tint = Color.Gray)
                                        }
                                        IconButton(onClick = { subViewMode = if (subViewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST }) {
                                            Icon(if (subViewMode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List, null, tint = Color.Gray)
                                        }
                                    }
                                    IconButton(onClick = { expandedGroupName = if (isExpanded) null else groupName }) {
                                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
                                    }
                                }
                            },
                            modifier = Modifier.clickable { expandedGroupName = if (isExpanded) null else groupName },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        
                        AnimatedVisibility(visible = isExpanded) {
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    val groupConfig = LocalConfiguration.current
                                    val isGroupTablet = groupConfig.screenWidthDp >= 600 || groupConfig.smallestScreenWidthDp >= 600
                                    val isGroupLandscape = groupConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                                    val groupGridCols = when {
                                        isGroupTablet && isGroupLandscape -> GridCells.Fixed(12)
                                        isGroupTablet -> GridCells.Fixed(8)
                                        else -> GridCells.Fixed(4)
                                    }

                                    if (subViewMode == LibraryViewMode.GRID) {
                                        LazyVerticalGrid(
                                            columns = groupGridCols,
                                            modifier = Modifier.heightIn(max = 2000.dp)
                                        ) {
                                            items(gTracks) { track ->
                                                StellarGridItem(
                                                    track = track,
                                                    isSelected = track.id in selectedTrackIds,
                                                    isPlaying = track.id == currentPlayingTrack?.id,
                                                    onPlay = { if (selectedTrackIds.isNotEmpty()) onTrackClick(track.id) else viewModel.playTrackList(gTracks, track) },
                                                    onLongClick = { onTrackClick(track.id) },
                                                    onOptions = onTrackOptions,
                                                    onFav = { viewModel.toggleFavorite(it) },
                                                    onInfo = onTrackInfo,
                                                    onUpload = { viewModel.uploadTrack(it) }
                                                )
                                            }
                                        }
                                    } else {
                                        Column {
                                            gTracks.forEach { track ->
                                                StellarTrackItem(
                                                    track = track,
                                                    isSelected = track.id in selectedTrackIds,
                                                    isPlaying = track.id == currentPlayingTrack?.id,
                                                    onPlay = { if (selectedTrackIds.isNotEmpty()) onTrackClick(track.id) else viewModel.playTrackList(gTracks, track) },
                                                    onLongClick = { onTrackClick(track.id) },
                                                    onOptions = onTrackOptions,
                                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                    onInfo = onTrackInfo,
                                                    onUpload = { viewModel.uploadTrack(it) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (addSongsGroupTarget != null) {
            val (groupType, groupName) = addSongsGroupTarget!!
            val existingIds = remember(allTracks, groupType, groupName) {
                allTracks.filter { 
                    when (groupType) {
                        "Album" -> it.displayAlbum == groupName
                        "Artist" -> it.displayArtist == groupName
                        else -> (it.genre ?: "Unknown") == groupName
                    }
                }.map { it.id }.toSet()
            }
            MultiSelectSongDialog(
                title = "Add Songs to $groupType ($groupName)",
                allTracks = allTracks,
                existingTrackIds = existingIds,
                onDismiss = { addSongsGroupTarget = null },
                onConfirm = { selectedIds ->
                    when (groupType) {
                        "Album" -> viewModel.bulkAssignTracksToAlbum(groupName, selectedIds)
                        "Artist" -> viewModel.bulkAssignTracksToArtist(groupName, selectedIds)
                        else -> viewModel.bulkAssignTracksToGenre(groupName, selectedIds)
                    }
                    addSongsGroupTarget = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun StellarTrackItem(
    track: TrackEntity, 
    isSelected: Boolean, 
    isPlaying: Boolean = false,
    onPlay: () -> Unit, 
    onLongClick: () -> Unit, 
    onOptions: (TrackEntity) -> Unit, 
    onToggleFavorite: (TrackEntity) -> Unit, 
    onInfo: (TrackEntity) -> Unit,
    onUpload: (TrackEntity) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600 || configuration.smallestScreenWidthDp >= 600
    val thumbSize = if (isTablet) 10.dp else 48.dp

    val isUnplayed = track.playCount == 0
    val activeBg = when {
        isSelected -> Color.White.copy(0.15f)
        isPlaying -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(activeBg)
            .combinedClickable(onClick = onPlay, onLongClick = onLongClick)
            .padding(12.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) { 
        Box {
            AsyncImage(
                model = track.customCoverPath ?: track.localPath, 
                contentDescription = null, 
                modifier = Modifier.size(thumbSize).clip(RoundedCornerShape(if (isTablet) 2.dp else 8.dp)), 
                contentScale = ContentScale.Crop
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .clip(RoundedCornerShape(if (isTablet) 2.dp else 8.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq, 
                        contentDescription = "Playing", 
                        tint = Color.Cyan, 
                        modifier = Modifier.size(if (isTablet) 6.dp else 24.dp)
                    )
                }
            }
            if (track.isFavorite) {
                Icon(
                    Icons.Default.Favorite, 
                    null, 
                    tint = Color.Red, 
                    modifier = Modifier.size(if (isTablet) 4.dp else 14.dp).align(Alignment.BottomStart).background(Color.Black.copy(0.4f), CircleShape).padding(1.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.displayName, 
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else LavenderTitle, 
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isUnplayed) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, CircleShape)
                    )
                }
            }
            Text(track.displayArtist, color = Color.Gray, style = MaterialTheme.typography.labelSmall) 
        }
        
        // Cloud Icon (Solid if synced, Empty/Outline if unsynced -> Tapping uploads)
        IconButton(
            onClick = {
                if (track.gDriveId == null) onUpload(track)
                else onOptions(track)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (track.gDriveId != null) Icons.Default.Cloud else Icons.Default.CloudQueue,
                contentDescription = if (track.gDriveId != null) "Synced to Cloud" else "Upload to Cloud",
                tint = if (track.gDriveId != null) Color.Cyan else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = { onToggleFavorite(track) }, modifier = Modifier.size(32.dp)) {
            Icon(
                if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (track.isFavorite) Color.Red else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { onOptions(track) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
            Spacer(Modifier.height(4.dp))
            IconButton(onClick = { onInfo(track) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun StellarGridItem(
    track: TrackEntity, 
    isSelected: Boolean, 
    isPlaying: Boolean = false,
    onPlay: (TrackEntity) -> Unit, 
    onLongClick: (TrackEntity) -> Unit, 
    onOptions: (TrackEntity) -> Unit = {}, 
    onFav: (TrackEntity) -> Unit = {}, 
    onInfo: (TrackEntity) -> Unit = {},
    onUpload: (TrackEntity) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600 || configuration.smallestScreenWidthDp >= 600
    val btnSize = if (isTablet) 12.dp else 26.dp
    val iconSize = if (isTablet) 7.dp else 15.dp

    val isUnplayed = track.playCount == 0
    val cardBorder = if (isPlaying) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

    Column(
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White.copy(0.15f) else Color.Transparent)
            .then(if (cardBorder != null) Modifier.border(cardBorder, RoundedCornerShape(8.dp)) else Modifier)
            .combinedClickable(onClick = { onPlay(track) }, onLongClick = { onLongClick(track) })
            .padding(2.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) { 
        Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth()) {
            AsyncImage(
                model = track.customCoverPath ?: track.localPath, 
                contentDescription = null, 
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), 
                contentScale = ContentScale.Crop
            )

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq, 
                        contentDescription = "Playing", 
                        tint = Color.Cyan, 
                        modifier = Modifier.size(if (isTablet) 14.dp else 32.dp)
                    )
                }
            }

            // Unplayed Red Dot Badge on Top-Right overlay
            if (isUnplayed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = if (isTablet) 10.dp else 28.dp, end = 2.dp)
                        .size(if (isTablet) 4.dp else 8.dp)
                        .background(Color.Red, CircleShape)
                )
            }

            // Top-Left Corner: Heart Icon (Favorite)
            IconButton(
                onClick = { onFav(track) }, 
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(btnSize)
                    .background(Color.Black.copy(0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    contentDescription = "Favorite", 
                    tint = if (track.isFavorite) Color.Red else Color.White, 
                    modifier = Modifier.size(iconSize)
                )
            }

            // Top-Right Corner: 3 Dot Icon (Options)
            IconButton(
                onClick = { onOptions(track) }, 
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(btnSize)
                    .background(Color.Black.copy(0.45f), CircleShape)
            ) { 
                Icon(
                    imageVector = Icons.Default.MoreVert, 
                    contentDescription = "Options", 
                    tint = Color.White, 
                    modifier = Modifier.size(iconSize)
                ) 
            }

            // Bottom-Left Corner: Cloud Icon (Solid if synced, Empty if unsynced -> Tapping uploads)
            IconButton(
                onClick = { 
                    if (track.gDriveId == null) onUpload(track)
                    else onOptions(track)
                }, 
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(2.dp)
                    .size(btnSize)
                    .background(Color.Black.copy(0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = if (track.gDriveId != null) Icons.Default.Cloud else Icons.Default.CloudQueue, 
                    contentDescription = if (track.gDriveId != null) "Synced to Cloud" else "Upload to Cloud", 
                    tint = if (track.gDriveId != null) Color.Cyan else Color.White.copy(alpha = 0.6f), 
                    modifier = Modifier.size(iconSize)
                )
            }

            // Bottom-Right Corner: Info Icon ("i")
            IconButton(
                onClick = { onInfo(track) }, 
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(btnSize)
                    .background(Color.Black.copy(0.45f), CircleShape)
            ) { 
                Icon(
                    imageVector = Icons.Default.Info, 
                    contentDescription = "Info", 
                    tint = Color.White, 
                    modifier = Modifier.size(iconSize)
                ) 
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = track.displayName, 
                color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White, 
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (isTablet) 8.sp else 11.sp, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis, 
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isUnplayed) {
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 3.dp else 6.dp)
                        .background(Color.Red, CircleShape)
                )
            }
        }
    }
}

@Composable
fun AlphabetFastScroller(
    tracks: List<TrackEntity>,
    onScrollTo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return
    
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    
    val alphabetMap = remember(tracks) {
        val map = mutableMapOf<Char, Int>()
        tracks.forEachIndexed { index, track ->
            val char = track.displayName.trim().firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (char in 'A'..'Z') char else '#'
            if (!map.containsKey(key)) {
                map[key] = index
            }
        }
        map
    }
    
    val letters = remember(alphabetMap) {
        ('A'..'Z').filter { alphabetMap.containsKey(it) } + if (alphabetMap.containsKey('#')) listOf('#') else emptyList()
    }
    
    if (letters.isEmpty()) return

    Box(modifier = modifier.fillMaxHeight().padding(vertical = 12.dp, horizontal = 1.dp), contentAlignment = Alignment.CenterEnd) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(vertical = 2.dp)
                .pointerInput(letters) {
                    detectTapGestures { offset ->
                        val itemHeight = size.height / letters.size.toFloat()
                        val index = (offset.y / itemHeight).toInt().coerceIn(0, letters.size - 1)
                        val letter = letters[index]
                        activeLetter = letter
                        alphabetMap[letter]?.let { targetIndex ->
                            onScrollTo(targetIndex)
                        }
                    }
                }
        ) {
            letters.forEach { char ->
                Text(
                    text = char.toString(),
                    color = if (activeLetter == char) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        activeLetter = char
                        alphabetMap[char]?.let { targetIndex ->
                            onScrollTo(targetIndex)
                        }
                    }
                )
            }
        }

        activeLetter?.let { letter ->
            Box(
                modifier = Modifier
                    .padding(end = 32.dp)
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            LaunchedEffect(letter) {
                delay(1200)
                if (activeLetter == letter) activeLetter = null
            }
        }
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
    TopAppBar(
        title = { Text(if (title == "selected") "$count Selected" else title) }, 
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) } }, 
        actions = { IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, null) } },
        windowInsets = WindowInsets.statusBars // Align below status bar
    )
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
        Icons.Default.Speaker, Icons.AutoMirrored.Filled.VolumeUp, Icons.Default.MusicVideo,
        Icons.Default.DiscFull, Icons.AutoMirrored.Filled.LibraryBooks, Icons.Default.Collections,
        Icons.Default.FolderSpecial, Icons.Default.AutoAwesome, Icons.Default.Celebration,
        Icons.Default.Nightlife, Icons.Default.SelfImprovement, Icons.Default.Brush,
        Icons.Default.Explore, Icons.Default.Star, Icons.Default.Favorite,
        Icons.Default.ThumbUp, Icons.Default.Bolt, Icons.Default.Whatshot
    )
    val lowerName = name.lowercase()
    if (lowerName.contains("fav")) return Icons.Default.Favorite
    if (lowerName.contains("recent")) return Icons.Default.History
    if (lowerName.contains("cloud")) return Icons.Default.Cloud
    if (lowerName.contains("party")) return Icons.Default.Nightlife
    if (lowerName.contains("relax")) return Icons.Default.SelfImprovement
    if (lowerName.contains("gym") || lowerName.contains("work")) return Icons.Default.Bolt
    if (lowerName.contains("top") || lowerName.contains("best")) return Icons.Default.Whatshot
    
    // Hash-based unique assignment from a larger pool
    return icons[Math.abs(name.hashCode()) % icons.size]
}
