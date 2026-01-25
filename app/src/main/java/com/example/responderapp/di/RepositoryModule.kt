package com.example.responderapp.di

import com.example.responderapp.data.repository.PregnancyCaseRepository
import com.example.responderapp.data.repository.PregnancyCaseRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPregnancyCaseRepository(
        impl: PregnancyCaseRepositoryImpl
    ): PregnancyCaseRepository
}