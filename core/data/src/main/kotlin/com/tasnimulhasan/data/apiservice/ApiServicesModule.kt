package com.tasnimulhasan.data.apiservice

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiServicesModule {

    /*@Provides
    @Singleton
    fun provideCredentialApiService(
        @AppBaseUrl retrofit: Retrofit,
        authRefreshServiceHolder: AuthRefreshServiceHolder
    ): CredentialApiService {
        authRefreshServiceHolder.setAuthRefreshApi(retrofit.create(AuthRefreshApiService::class.java))
        return retrofit.create(CredentialApiService::class.java)
    }*/
}