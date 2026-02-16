package com.irah.galleria.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.irah.galleria.R
import com.irah.galleria.domain.model.MediaOperationState
import com.irah.galleria.domain.model.OperationType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val channelId = "file_operations"
    private val notificationId = 1001

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "File Operations"
            val descriptionText = "Shows progress of file operations"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL_MS = 500L

    fun showProgress(state: MediaOperationState.Running) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val currentTime = System.currentTimeMillis()
        // Update immediately if it's the first update (progress 0/1) or explicitly finished (handled in showCompletion)
        // or if enough time has passed.
        if (state.progress > 0 && 
            state.progress < state.total && 
            currentTime - lastUpdateTime < UPDATE_INTERVAL_MS) {
            return
        }
        lastUpdateTime = currentTime

        val title = when (state.operation) {
            OperationType.COPY -> "Copying ${state.total} items"
            OperationType.MOVE -> "Moving ${state.total} items"
            OperationType.DELETE -> "Deleting ${state.total} items"
            OperationType.RESTORE -> "Restoring ${state.total} items"
            OperationType.DELETE_FOREVER -> "Deleting ${state.total} items permanently"
        }

        val icon = when (state.operation) {
            OperationType.COPY -> R.drawable.ic_notification_copy
            OperationType.MOVE -> R.drawable.ic_notification_move
            OperationType.DELETE -> R.drawable.ic_notification_delete
            OperationType.RESTORE -> R.drawable.ic_notification_restore
            OperationType.DELETE_FOREVER -> R.drawable.ic_notification_delete
        }

        val percentage = if (state.total > 0) (state.progress * 100) / state.total else 0
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon) 
            .setContentTitle(title)
            .setContentText("${state.progress} / ${state.total} • ${state.currentFile}")
            .setSubText("$percentage%") // Percentage in subtext
            .setPriority(NotificationCompat.PRIORITY_LOW) 
            .setOngoing(true)
            .setOnlyAlertOnce(true) 
            .setProgress(state.total, state.progress, false)

        notificationManager.notify(notificationId, builder.build())
    }

    fun showCompletion(state: MediaOperationState.Completed) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val title = when (state.operation) {
            OperationType.COPY -> "Copying file(s) completed"
            OperationType.MOVE -> "Moving file(s) completed"
            OperationType.DELETE -> "Delete completed"
            OperationType.RESTORE -> "Restore completed"
            OperationType.DELETE_FOREVER -> "Delete completed"
        }
        
        val icon = when (state.operation) {
            OperationType.COPY -> R.drawable.ic_notification_copy
            OperationType.MOVE -> R.drawable.ic_notification_move
            OperationType.DELETE -> R.drawable.ic_notification_delete
            OperationType.RESTORE -> R.drawable.ic_notification_restore
            OperationType.DELETE_FOREVER -> R.drawable.ic_notification_delete
        }

        lastUpdateTime = 0L // Reset for next operation

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText("Successfully processed ${state.count} files")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
    
    fun cancel() {
        notificationManager.cancel(notificationId)
    }
}
