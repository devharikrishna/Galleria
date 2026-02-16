package com.irah.galleria.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.irah.galleria.ui.theme.GlassSurface

@Composable
fun SelectionActionMenu(
    visible: Boolean,
    modifier: Modifier = Modifier,
    zIndex: Float = 10f,
    isTrash: Boolean = false,
    hasFavorites: Boolean = false, // To toggle heart icon if needed, though usually we just show "Favorite"
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit = {},
    onRestore: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + androidx.compose.animation.fadeIn(),
        exit = slideOutVertically { it } + androidx.compose.animation.fadeOut(),
        modifier = modifier.zIndex(zIndex)
    ) {
        GlassSurface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTrash) {
                    ActionButton(
                        icon = Icons.Default.Refresh,
                        label = "Restore",
                        onClick = onRestore
                    )
                    ActionButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        onClick = onDelete
                    )
                } else {
                    ActionButton(
                        icon = Icons.Outlined.Share,
                        label = "Share",
                        onClick = onShare
                    )
                    ActionButton(
                        icon = Icons.Outlined.FavoriteBorder,
                        label = "Favorite",
                        onClick = onFavorite
                    )
                    ActionButton(
                        icon = Icons.Outlined.ContentCopy,
                        label = "Copy",
                        onClick = onCopy
                    )
                    ActionButton(
                        icon = Icons.Outlined.ContentCut,
                        label = "Move",
                        onClick = onMove
                    )
                    ActionButton(
                        icon = Icons.Outlined.Delete,
                        label = "Delete",
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
