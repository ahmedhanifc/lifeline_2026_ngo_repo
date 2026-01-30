package com.example.responderapp.di

import android.content.Context
import androidx.room.Room
import com.example.responderapp.data.local.AppDatabase
import com.example.responderapp.data.local.dao.DistressEventDao
import com.example.responderapp.data.local.dao.PregnancyCaseDao
import com.example.responderapp.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifelines_db"
        ).fallbackToDestructiveMigration() // FLAG Useful for dev, remove for prod!
         .build()
    }

    @Provides
    fun providePregnancyCaseDao(database: AppDatabase): PregnancyCaseDao {
        return database.pregnancyCaseDao()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideDistressEventDao(database: AppDatabase): DistressEventDao {
        return database.distressEventDao()
    }
}