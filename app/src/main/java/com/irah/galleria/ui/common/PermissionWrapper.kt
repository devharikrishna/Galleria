package com.irah.galleria.ui.common
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.irah.galleria.R
import com.irah.galleria.ui.theme.GlassScaffold
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(
    content: @Composable () -> Unit
) {
    var currentStep by remember { mutableStateOf(PermissionStep.MEDIA) }

    // Media Permissions
    val mediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (Build.VERSION.SDK_INT >= 34) {
             listOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else {
             listOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        }
    } else {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }
    
    // Notification Permission (Android 13+)
    val notificationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyList()
    }

    val mediaPermissionState = rememberMultiplePermissionsState(mediaPermissions)
    val notificationPermissionState = rememberMultiplePermissionsState(notificationPermissions)

    val hasMediaAccess = mediaPermissionState.allPermissionsGranted || 
            (Build.VERSION.SDK_INT >= 34 && mediaPermissionState.permissions.any { it.permission == android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED && it.status.isGranted })
            
    val hasNotificationAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionState.allPermissionsGranted
    } else true

    LaunchedEffect(hasMediaAccess, hasNotificationAccess) {
        if (hasMediaAccess && !hasNotificationAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentStep = PermissionStep.NOTIFICATION
        } else if (hasMediaAccess && hasNotificationAccess) {
            currentStep = PermissionStep.GRANTED
        }
    }

    if (currentStep == PermissionStep.GRANTED) {
        content()
    } else {
        GlassScaffold(
            modifier = Modifier
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isMediaStep = currentStep == PermissionStep.MEDIA
                    
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(20.dp)
                    ) {
                         if (isMediaStep) {
                             Image(
                                painter = painterResource(id = R.mipmap.ic_launcher),
                                contentDescription = "App Icon",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                         } else {
                             Icon(
                                 imageVector = Icons.Default.Notifications,
                                 contentDescription = "Notification Icon",
                                 modifier = Modifier.fillMaxSize(),
                                 tint = MaterialTheme.colorScheme.primary
                             )
                         }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = if (isMediaStep) "Media Access Required" else "Stay Updated",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isMediaStep) 
                            "Galleria needs access to your photos and videos to display and manage them." 
                            else "Allow notifications to see the progress of file operations like Copy, Move, and Delete.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    if (isMediaStep && Build.VERSION.SDK_INT >= 34) {
                         Spacer(modifier = Modifier.height(8.dp))
                         Text(
                            text = "You can choose to grant access to only specific photos if you prefer.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = { 
                            if (isMediaStep) {
                                mediaPermissionState.launchMultiplePermissionRequest()
                            } else {
                                notificationPermissionState.launchMultiplePermissionRequest()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                         colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            if (isMediaStep) "Grant Media Access" else "Allow Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    if (!isMediaStep) {
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { currentStep = PermissionStep.GRANTED }
                        ) {
                             Text(
                                "Skip for now",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Your content stays private and secure on your device.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

enum class PermissionStep {
    MEDIA, NOTIFICATION, GRANTED
}