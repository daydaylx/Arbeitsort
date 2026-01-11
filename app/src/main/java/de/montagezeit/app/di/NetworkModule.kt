package de.montagezeit.app.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.data.network.DistanceService
import de.montagezeit.app.data.network.OpenRouteServiceDistanceService
import de.montagezeit.app.data.preferences.RoutingSettingsManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideDistanceService(
        routingSettingsManager: RoutingSettingsManager,
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): DistanceService {
        return OpenRouteServiceDistanceService(
            routingSettingsManager = routingSettingsManager,
            okHttpClient = okHttpClient,
            moshi = moshi
        )
    }
}
