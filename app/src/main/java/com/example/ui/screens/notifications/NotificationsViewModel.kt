package com.example.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.repository.NotificationRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationGroup(
    val title: String,
    val items: List<AppNotification>
)

class NotificationsViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    val unreadCount: StateFlow<Int> = repository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notificationGroups: StateFlow<List<NotificationGroup>> = repository.allNotifications
        .map { notifications ->
            val now = System.currentTimeMillis()
            val startOfToday = DateUtils.getStartOfDay(now)
            val startOfWeek = DateUtils.getStartOfWeek(now)

            val todayList = mutableListOf<AppNotification>()
            val thisWeekList = mutableListOf<AppNotification>()
            val earlierList = mutableListOf<AppNotification>()

            for (item in notifications) {
                when {
                    item.timestamp >= startOfToday -> todayList.add(item)
                    item.timestamp >= startOfWeek -> thisWeekList.add(item)
                    else -> earlierList.add(item)
                }
            }

            val groups = mutableListOf<NotificationGroup>()
            if (todayList.isNotEmpty()) {
                groups.add(NotificationGroup("Today", todayList))
            }
            if (thisWeekList.isNotEmpty()) {
                groups.add(NotificationGroup("This Week", thisWeekList))
            }
            if (earlierList.isNotEmpty()) {
                groups.add(NotificationGroup("Earlier", earlierList))
            }
            groups
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.allNotifications
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }
}
