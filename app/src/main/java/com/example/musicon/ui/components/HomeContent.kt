package com.example.musicon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun HomeContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    ),
                    startY = 0f,
                    endY = 500f
                )
            ),
        contentPadding = PaddingValues(bottom = 80.dp) // Space for Mini Player
    ) {
        item {
            Text(
                text = "Good evening",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                modifier = Modifier.padding(16.dp)
            )
        }

        item {
            RecentGrids()
        }

        item {
            SectionHeader("Made For You")
            TrackHorizontalList()
        }

        item {
            SectionHeader("Recently Played")
            TrackHorizontalList()
        }
    }
}

@Composable
fun RecentGrids() {
    val items = listOf("Daily Mix 1", "Liked Songs", "Discover Weekly", "Release Radar")
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        for (i in 0 until 2) {
            Row(modifier = Modifier.fillMaxWidth()) {
                RecentCard(items[i*2], Modifier.weight(1f))
                RecentCard(items[i*2+1], Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RecentCard(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.Gray)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp),
            maxLines = 2
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp)
    )
}

@Composable
fun TrackHorizontalList() {
    LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
        items(5) {
            TrackCard()
        }
    }
}

@Composable
fun TrackCard() {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .width(150.dp)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )
        Text(
            text = "Track Title",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 1
        )
        Text(
            text = "Artist Name",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
