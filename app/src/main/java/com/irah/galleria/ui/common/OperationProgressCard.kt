package com.irah.galleria.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.irah.galleria.domain.model.MediaOperationState
import com.irah.galleria.domain.model.OperationType
import com.irah.galleria.ui.theme.GlassSurface

@Composable
fun OperationProgressCard(
    modifier: Modifier = Modifier,
    state: MediaOperationState.Running,
    onCancel: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    val title = when (state.operation) {
        OperationType.COPY -> "Copying"
        OperationType.MOVE -> "Moving"
        OperationType.DELETE -> "Deleting"
        OperationType.RESTORE -> "Restoring"
        OperationType.DELETE_FOREVER -> "Deleting Permanently"
    }

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                   Text(
                       text = "${(state.progress * 100) / state.total}%",
                       style = MaterialTheme.typography.labelSmall,
                       color = MaterialTheme.colorScheme.primary
                   )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.progress}/${state.total}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    LinearProgressIndicator(
                        progress = { state.progress.toFloat() / state.total.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    
                    if (state.currentFile.isNotEmpty()) {
                        Text(
                            text = state.currentFile,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
