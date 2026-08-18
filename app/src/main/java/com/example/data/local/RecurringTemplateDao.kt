package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RecurringTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTemplateDao {
    @Query("SELECT * FROM recurring_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<RecurringTemplate>>

    @Query("SELECT * FROM recurring_templates WHERE isActive = 1")
    fun getActiveTemplates(): Flow<List<RecurringTemplate>>

    @Query("SELECT * FROM recurring_templates WHERE isActive = 1")
    suspend fun getActiveTemplatesSnapshot(): List<RecurringTemplate>

    @Query("SELECT * FROM recurring_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): RecurringTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: RecurringTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<RecurringTemplate>): List<Long>

    @Update
    suspend fun update(template: RecurringTemplate)

    @Delete
    suspend fun delete(template: RecurringTemplate)

    @Query("DELETE FROM recurring_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE recurring_templates SET lastGeneratedDate = :date WHERE id = :id")
    suspend fun updateLastGenerated(id: Long, date: Long)

    @Query("UPDATE recurring_templates SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: Long, isActive: Boolean)

    @Query("DELETE FROM recurring_templates")
    suspend fun deleteAll()
}
