package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.NotificationType
import com.example.data.model.RecurringFrequency
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { enumValueOf<TransactionType>(it) }
    }

    @TypeConverter
    fun fromRecurringFrequency(value: RecurringFrequency?): String? {
        return value?.name
    }

    @TypeConverter
    fun toRecurringFrequency(value: String?): RecurringFrequency? {
        return value?.let { enumValueOf<RecurringFrequency>(it) }
    }

    @TypeConverter
    fun fromNotificationType(value: NotificationType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toNotificationType(value: String?): NotificationType? {
        return value?.let { enumValueOf<NotificationType>(it) }
    }
}
