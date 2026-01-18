package com.irah.galleria.ui.gallery.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.irah.galleria.domain.model.Album

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumSelectionSheet(
    albums: List<Album>,
    onAlbumSelected: (Album) -> Unit,
    onCreateNewAlbum: () -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Move to Album",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("Create New Album") },
                leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                modifier = Modifier.clickable { onCreateNewAlbum() }
            )

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(albums) { album ->
                    ListItem(
                        headlineContent = { Text(album.name) },
                        supportingContent = { Text("${album.count} items") },
                        modifier = Modifier.clickable { onAlbumSelected(album) }
                    )
                }
            }
        }
    }
}
