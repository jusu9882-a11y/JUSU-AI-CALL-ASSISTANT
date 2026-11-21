package com.jusu.aicallassistant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InstructionDao {
    @Query("SELECT * FROM call_instructions ORDER BY priority DESC, title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<InstructionEntity>>

    @Query("SELECT * FROM call_instructions")
    suspend fun getAll(): List<InstructionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: InstructionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<InstructionEntity>)

    @Query("DELETE FROM call_instructions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM call_instructions")
    suspend fun clear()
}
