package com.example.responderapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.responderapp.data.local.dao.DistressEventDao
import com.example.responderapp.data.local.dao.PregnancyCaseDao
import com.example.responderapp.data.local.dao.UserDao
import com.example.responderapp.data.local.entity.CaseUpdateEntity
import com.example.responderapp.data.local.entity.DistressEventEntity
import com.example.responderapp.data.local.entity.PregnancyCaseEntity
import com.example.responderapp.data.local.entity.UserAccountEntity

@Database(
    entities = [
        PregnancyCaseEntity::class, 
        UserAccountEntity::class,
        CaseUpdateEntity::class,
        DistressEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pregnancyCaseDao(): PregnancyCaseDao
    abstract fun userDao(): UserDao
    abstract fun distressEventDao(): DistressEventDao
}