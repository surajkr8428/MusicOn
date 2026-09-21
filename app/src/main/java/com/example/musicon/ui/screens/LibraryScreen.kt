package com.example.musicon.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
    var selectedPlaylistOptions by remember { mutableStateOf<Playlist?>(null) }
    var showTrackInfoDialog by remember { mutableStateOf<TrackEntity?>(null) }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var addingToPlaylistId by remember { mutableStateOf<String?>(null) }

    val tabs = remember(customFolders) { 
        listOf("Recents", "All Songs", "Playlists", "Albums", "Artists", "Genres") + 
        customFolders.map { it.substringAfterLast("/").ifBlank { "Folder" } }
    }
    
    // Simple PagerState
    val pagerState = rememberPagerState(initialPage = 1) { tabs.size }
    
    BackHandler(isSelectionMode || searchQuery.isNotEmpty() || currentPlaylistDetail != null || isSearchActive) {
        if (isSelectionMode) { selectedIds = emptySet(); addingToPlaylistId = null }
        else if (isSearchActive) { isSearchActive = false; viewModel.updateSearchQuery("") }
        else if (currentPlaylistDetail != null) currentPlaylistDetail = null
    }

    if (currentPlaylistDetail != null && addingToPlaylistId == null) {
        PlaylistDetailScreen(playlist = currentPlaylistDetail!!, viewModel = viewModel, onBack = { currentPlaylistDetail = null }, viewMode = viewMode, isLandscape = isLandscape, onAddSongs = { addingToPlaylistId = currentPlaylistDetail!!.id; scope.launch { pagerState.animateScrollToPage(1) } })
    } else {
        StellarBackground(themeMode = themeMode, backgroundMode = backgroundMode) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    if (addingToPlaylistId != null || isSelectionMode) {
                        SelectionTopBar(count = selectedIds.size, onClose = { selectedIds = emptySet(); addingToPlaylistId = null }, onSelectAll = { val all = tracks.map { it.id }.toSet(); selectedIds = if (selectedIds.size == all.size) emptySet() else all }, title = if (addingToPlaylistId != null) "Add to Playlist" else "selected")
                    } else {
                        LibraryTopBar(searchQuery, isSearchActive, { isSearchActive = !isSearchActive }, { viewModel.updateSearchQuery(it) }, onOpenDrawer, onOpenSettings, { showSortMenu = true }, { viewModel.updateLibraryViewMode(if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST) }, viewMode, viewModel, isOnline, syncStatus)
                    }
                },
                bottomBar = {
                    if (addingToPlaylistId != null) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth().height(70.dp)) {
                            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { viewModel.bulkAddTracksToPlaylist(addingToPlaylistId!!, selectedIds.toList()); selectedIds = emptySet(); addingToPlaylistId = null }, enabled = selectedIds.isNotEmpty()) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Add ${selectedIds.size} songs") }
                            }
                        }
                    } else {
                        AnimatedVisibility(visible = isSelectionMode, enter = expandVertically(), exit = shrinkVertically()) {
                            SelectionBottomBar(onPlay = { viewModel.playSelected(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() }, onNext = { viewModel.addToQueueNext(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() }, onShare = { viewModel.shareTracks(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() }, onDelete = { viewModel.bulkDelete(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() })
                        }
                    }
                }
            ) { innerPadding ->
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { scope.launch { isRefreshing = true; viewModel.scanLocalStorage(); delay(1000); isRefreshing = false } }, modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        if (!isSelectionMode && addingToPlaylistId == null) {
                            ScrollableTabRow(selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent, edgePadding = 16.dp, divider = {}, indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(it[pagerState.currentPage]), color = MaterialTheme.colorScheme.primary) }) {
                                tabs.forEachIndexed { index, title -> val isSelected = pagerState.currentPage == index; Tab(selected = isSelected, onClick = { scope.launch { pagerState.animateScrollToPage(index) } }, text = { Text(title, color = if (isSelected) Color.White else Color.Gray) }) }
                            }
                        }
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            when (page) {
                                0 -> SongsTab(viewModel.sessionRecentlyPlayed.collectAsState().value, selectedIds, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isSelectionMode || addingToPlaylistId != null) selectedIds = if (it.id in selectedIds) selectedIds - it.id else selectedIds + it.id else viewModel.playTrackList(viewModel.sessionRecentlyPlayed.value, it) }, { if (!isSelectionMode) selectedIds = setOf(it.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) })
                                1 -> SongsTab(tracks, selectedIds, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isSelectionMode || addingToPlaylistId != null) selectedIds = if (it.id in selectedIds) selectedIds - it.id else selectedIds + it.id else viewModel.playTrackList(tracks, it) }, { if (!isSelectionMode) selectedIds = setOf(it.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) })
                                2 -> PlaylistsTab(playlists, viewMode, rememberLazyListState(), rememberLazyGridState(), { currentPlaylistDetail = it }, { viewModel.createPlaylist("New Playlist") }, { selectedPlaylistOptions = it }, { tList, tStart, pId -> viewModel.playTrackList(tList, tStart, pId) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) }, { pid -> addingToPlaylistId = pid; scope.launch { pagerState.animateScrollToPage(1) } })
                                3 -> GroupedTab(allTracks, "Album", viewMode, rememberLazyListState(), { viewModel.playTrackList(allTracks, it.first()) })
                                4 -> GroupedTab(allTracks, "Artist", viewMode, rememberLazyListState(), { viewModel.playTrackList(allTracks, it.first()) })
                                5 -> GroupedTab(allTracks, "Genre", viewMode, rememberLazyListState(), { viewModel.playTrackList(allTracks, it.first()) })
                                else -> {
                                    val path = customFolders[page - 6]
                                    val fTracks = allTracks.filter { it.localPath?.startsWith(path) == true }
                                    SongsTab(fTracks, selectedIds, viewMode, rememberLazyListState(), rememberLazyGridState(), { if (isSelectionMode || addingToPlaylistId != null) selectedIds = if (it.id in selectedIds) selectedIds - it.id else selectedIds + it.id else viewModel.playTrackList(fTracks, it) }, { if (!isSelectionMode) selectedIds = setOf(it.id) }, { selectedTrackOptions = it }, { viewModel.toggleFavorite(it) })
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
            }
            selectedTrackOptions = null
        })
    }
    if (showTrackInfoDialog != null) TechnicalInfoPopup(track = showTrackInfoDialog!!, onDismiss = { showTrackInfoDialog = null })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(playlist: Playlist, viewModel: MainViewModel, onBack: () -> Unit, viewMode: LibraryViewMode, isLandscape: Boolean, onAddSongs: () -> Unit) {
    val tracks by viewModel.getTracksForPlaylist(playlist.id).collectAsState(emptyList())
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedTrackOptions by remember { mutableStateOf<TrackEntity?>(null) }
    var subViewMode by rememberSaveable { mutableStateOf(LibraryViewMode.GRID) }
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
                    if (subViewMode == LibraryViewMode.GRID) LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 130.dp), modifier = Modifier.fillMaxSize()) { items(tracks) { track -> StellarGridItem(track, track.id in selectedIds, { if (isSelectionMode) selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id else viewModel.playTrackList(tracks, track, playlist.id) }, { if (!isSelectionMode) selectedIds = setOf(track.id) }) } }
                    else LazyColumn(Modifier.fillMaxSize()) { items(tracks) { track -> StellarTrackItem(track, track.id in selectedIds, { if (isSelectionMode) selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id else viewModel.playTrackList(tracks, track, playlist.id) }, { if (!isSelectionMode) selectedIds = setOf(track.id) }, { selectedTrackOptions = track }, { viewModel.toggleFavorite(track) } ) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(q: String, active: Boolean, onToggle: () -> Unit, onChange: (String) -> Unit, onDrawer: () -> Unit, onSettings: () -> Unit, onSort: () -> Unit, onMode: () -> Unit, mode: LibraryViewMode, viewModel: MainViewModel, online: Boolean, sync: SyncStatus) {
    TopAppBar(title = { if (active) TextField(value = q, onValueChange = onChange, placeholder = { Text("Search...") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)) else Text("Nirvaana", color = Color.White, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onDrawer) { Icon(Icons.Default.Menu, null, tint = Color.White) } }, actions = { IconButton(onClick = onToggle) { Icon(if (active) Icons.Default.Close else Icons.Default.Search, null, tint = Color.White) }; IconButton(onClick = onMode) { Icon(if (mode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List, null, tint = Color.White) }; IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
}

@Composable fun SongsTab(tracks: List<TrackEntity>, selected: Set<String>, mode: LibraryViewMode, listState: LazyListState, gridState: LazyGridState, onClick: (TrackEntity) -> Unit, onLong: (TrackEntity) -> Unit, onOptions: (TrackEntity) -> Unit, onFav: (TrackEntity) -> Unit) {
    if (mode == LibraryViewMode.GRID) LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(minSize = 110.dp), modifier = Modifier.fillMaxSize()) { items(tracks) { StellarGridItem(it, it.id in selected, { onClick(it) }, { onLong(it) }) } }
    else LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) { items(tracks) { StellarTrackItem(it, it.id in selected, { onClick(it) }, { onLong(it) }, { onOptions(it) }, { onFav(it) }) } }
}

@Composable fun PlaylistsTab(playlists: List<Playlist>, mode: LibraryViewMode, listState: LazyListState, gridState: LazyGridState, onClick: (Playlist) -> Unit, onCreate: () -> Unit, onOptions: (Playlist) -> Unit, onPlay: (List<TrackEntity>, TrackEntity, String) -> Unit, onTrackOptions: (TrackEntity) -> Unit, onFav: (TrackEntity) -> Unit, onAdd: (String) -> Unit) {
    if (mode == LibraryViewMode.GRID) LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(minSize = 130.dp), modifier = Modifier.fillMaxSize()) { items(playlists) { playlist -> PlaylistGridItem(playlist, { onClick(playlist) }) } }
    else LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) { items(playlists) { playlist -> ListItem(headlineContent = { Text(playlist.name, color = Color.White) }, leadingContent = { Icon(Icons.Default.QueueMusic, null, tint = Color.White) }, modifier = Modifier.clickable { onClick(playlist) }) } }
}

@Composable fun GroupedTab(tracks: List<TrackEntity>, type: String, mode: LibraryViewMode, listState: LazyListState, onPlay: (List<TrackEntity>) -> Unit) {
    val grouped = remember(tracks, type) { when(type) { "Album" -> tracks.groupBy { it.displayAlbum }; "Artist" -> tracks.groupBy { it.displayArtist }; else -> tracks.groupBy { it.genre ?: "Unknown" } } }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) { grouped.forEach { (name, gTracks) -> item { ListItem(headlineContent = { Text(name ?: "Unknown", color = Color.White) }, supportingContent = { Text("${gTracks.size} songs") }, modifier = Modifier.clickable { onPlay(gTracks) }) } } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun StellarTrackItem(track: TrackEntity, isSelected: Boolean, onPlay: () -> Unit, onLongClick: () -> Unit, onOptions: () -> Unit, onToggleFavorite: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color.White.copy(0.1f) else Color.Transparent).combinedClickable(onClick = onPlay, onLongClick = onLongClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) { 
        AsyncImage(model = track.customCoverPath ?: track.localPath, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) { Text(track.displayName, color = LavenderTitle, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(track.displayArtist, color = Color.Gray, style = MaterialTheme.typography.labelSmall) }
        IconButton(onClick = onOptions) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun StellarGridItem(track: TrackEntity, isSelected: Boolean, onPlay: (TrackEntity) -> Unit, onLongClick: (TrackEntity) -> Unit) {
    Column(modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = { onPlay(track) }, onLongClick = { onLongClick(track) }).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) { AsyncImage(model = track.customCoverPath ?: track.localPath, contentDescription = null, modifier = Modifier.aspectRatio(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop); Spacer(Modifier.height(8.dp)); Text(track.displayName, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
}

@Composable fun PlaylistGridItem(p: Playlist, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.QueueMusic, null, tint = Color.Gray, modifier = Modifier.size(48.dp)) }; Text(p.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
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

@Composable fun SleepTimerDialog(onDismiss: () -> Unit, onSet: (Int, Int, Int) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Sleep Timer") }, text = { Text("Set timer") }, confirmButton = { Button(onClick = { onSet(0, 30, 0) }) { Text("30m") } })
}
