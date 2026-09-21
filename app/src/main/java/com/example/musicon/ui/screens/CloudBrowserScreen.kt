package com.example.musicon.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicon.R
import com.example.musicon.data.LibraryViewMode
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.data.remote.CloudStorageManager
import com.example.musicon.data.remote.CloudSyncManager
import com.example.musicon.ui.components.HeaderStatusPill
import com.example.musicon.ui.components.RenameDialog
import com.example.musicon.ui.components.StellarBackground
import com.example.musicon.ui.components.SyncProgressBar
import com.example.musicon.ui.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.services.drive.model.File
import kotlinx.coroutines.launch

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
    val cloudManager = remember { CloudStorageManager(context) }
    
    var cloudFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var viewMode by rememberSaveable { mutableStateOf(LibraryViewMode.LIST) }
    var sortOrder by rememberSaveable { mutableStateOf("NAME_ASC") }
    
    var trackToRename by remember { mutableStateOf<File?>(null) }
    var trackToDownloadConfirm by remember { mutableStateOf<File?>(null) }
    var tracksToDeleteConfirm by remember { mutableStateOf<List<File>?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            isRefreshing = true
            val folderId = cloudManager.getOrCreateAppFolder()
            cloudFiles = cloudManager.listAudioFiles(folderId)
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

    StellarBackground(themeMode = themeMode, backgroundMode = backgroundMode) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (selectedIds.isNotEmpty()) {
                    TopAppBar(
                        title = { Text("${selectedIds.size} selected", color = Color.White) },
                        navigationIcon = { IconButton(onClick = { selectedIds = emptySet() }) { Icon(Icons.Default.Close, null, tint = Color.White) } },
                        actions = {
                            IconButton(onClick = { val allIds = cloudFiles.map { it.id }.toSet(); selectedIds = if (selectedIds.size == allIds.size) emptySet() else allIds }) { Icon(Icons.Default.SelectAll, null, tint = Color.White) }
                            IconButton(onClick = { val sel = cloudFiles.filter { it.id in selectedIds }; if (sel.isNotEmpty()) trackToDownloadConfirm = sel.first() }) { Icon(Icons.Default.Download, null, tint = Color.White) }
                            IconButton(onClick = { tracksToDeleteConfirm = cloudFiles.filter { it.id in selectedIds } }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(0.1f))
                    )
                } else {
                    TopAppBar(
                        title = { Row(verticalAlignment = Alignment.CenterVertically) { Text("Cloud Browser", color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); HeaderStatusPill(isOnline, isWifi); SyncProgressBar(syncStatus, Modifier.weight(1f)) } },
                        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                        actions = {
                            IconButton(onClick = { viewModel.deleteAllCloudTracks() }) { Icon(Icons.Default.DeleteSweep, "Delete All", tint = Color.Red) }
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
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
        ) { padding ->
            if (!isUserSignedIn) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, null, tint = Color.Gray, modifier = Modifier.size(64.dp)); Spacer(Modifier.height(16.dp)); Text("Sign in to view Cloud songs", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { refresh() }, modifier = Modifier.padding(padding).fillMaxSize()) {
                    if (isLoading && !isRefreshing) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
                    else if (viewMode == LibraryViewMode.GRID) {
                        LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                            items(sortedFiles) { file ->
                                val isSelected = file.id in selectedIds
                                Column(modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color.White.copy(0.15f) else Color.Transparent).combinedClickable(onClick = { if (selectedIds.isNotEmpty()) selectedIds = if (isSelected) selectedIds - file.id else selectedIds + file.id }, onLongClick = { selectedIds = setOf(file.id) }).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = file.thumbnailLink ?: R.drawable.ic_launcher_foreground,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                        error = ColorPainter(Color.White.copy(alpha = 0.1f))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(file.name ?: "Unknown", color = Color.White, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(sortedFiles) { file ->
                                val isSelected = file.id in selectedIds
                                ListItem(
                                    headlineContent = { Text(file.name ?: "Unknown", color = Color.White) },
                                    supportingContent = { Text("${(file.getSize() ?: 0L) / 1024} KB", color = Color.Gray) },
                                    leadingContent = { 
                                        if (selectedIds.isNotEmpty()) Checkbox(checked = isSelected, onCheckedChange = null) 
                                        else AsyncImage(model = file.thumbnailLink ?: R.drawable.ic_launcher_foreground, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                                    },
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
    if (trackToRename != null) RenameDialog(initialName = trackToRename!!.getName() ?: "", onDismiss = { trackToRename = null }, onConfirm = { newName -> scope.launch { cloudManager.renameFile(trackToRename!!.id, newName); trackToRename = null; refresh() } })
    if (trackToDownloadConfirm != null) AlertDialog(onDismissRequest = { trackToDownloadConfirm = null }, title = { Text("Download?") }, text = { Text("Download '${trackToDownloadConfirm!!.getName()}' to internal storage?") }, confirmButton = { Button(onClick = { viewModel.downloadTrack(TrackEntity(id = trackToDownloadConfirm!!.id, title = trackToDownloadConfirm!!.getName() ?: "Unknown", artist = "Cloud", album = "Cloud", duration = 0, gDriveId = trackToDownloadConfirm!!.id)); trackToDownloadConfirm = null }) { Text("Download") } }, dismissButton = { TextButton(onClick = { trackToDownloadConfirm = null }) { Text("Cancel") } })
    if (tracksToDeleteConfirm != null) AlertDialog(onDismissRequest = { tracksToDeleteConfirm = null }, title = { Text("Delete?") }, text = { Text("Delete ${tracksToDeleteConfirm!!.size} songs from Cloud?") }, confirmButton = { Button(onClick = { scope.launch { tracksToDeleteConfirm!!.forEach { cloudManager.deleteFile(it.id) }; tracksToDeleteConfirm = null; selectedIds = emptySet(); refresh() } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") } }, dismissButton = { TextButton(onClick = { tracksToDeleteConfirm = null }) { Text("Cancel") } })
}
