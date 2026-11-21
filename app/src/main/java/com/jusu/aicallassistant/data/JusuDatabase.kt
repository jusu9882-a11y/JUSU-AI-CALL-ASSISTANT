package com.jusu.aicallassistant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [InstructionEntity::class], version = 1, exportSchema = false)
abstract class JusuDatabase : RoomDatabase() {
    abstract fun instructionDao(): InstructionDao

    companion object {
        @Volatile private var instance: JusuDatabase? = null

        fun get(context: Context): JusuDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                JusuDatabase::class.java,
                "jusu_local.db"
            ).build().also { instance = it }
        }
    }
}
