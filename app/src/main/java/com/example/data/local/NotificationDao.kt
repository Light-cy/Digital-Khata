package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotification): Long

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM app_notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM app_notifications WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query("SELECT * FROM app_notifications WHERE type = :type AND timestamp >= :sinceTimestamp")
    suspend fun getRecentByType(type: NotificationType, sinceTimestamp: Long): List<AppNotification>

    @Query("SELECT * FROM app_notifications WHERE type = :type AND relatedId = :relatedId AND timestamp >= :sinceTimestamp")
    suspend fun getRecentByTypeAndRelatedId(type: NotificationType, relatedId: Long, sinceTimestamp: Long): List<AppNotification>
}
