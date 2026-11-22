package com.nigutu.app.core.di

import com.nigutu.app.data.local.FavoriteDao
import com.nigutu.app.data.local.HistoryDao
import com.nigutu.app.data.local.NigutuDatabase
import com.nigutu.app.data.local.PlaylistDao
import com.nigutu.app.data.local.SearchHistoryDao
import com.nigutu.app.data.local.SongDao
import com.nigutu.app.data.mediastore.LocalMusicSource
import com.nigutu.app.data.mediastore.MediaStoreScanner
import com.nigutu.app.data.remote.JsonHttpClient
import com.nigutu.app.data.repository.MusicRepositoryImpl
import com.nigutu.app.data.repository.PlaylistRepositoryImpl
import com.nigutu.app.data.repository.SettingsRepositoryImpl
import com.nigutu.app.data.remote.DeezerCatalogRepository
import com.nigutu.app.data.remote.LrcLibLyricsRepository
import com.nigutu.app.domain.repository.CatalogRepository
import com.nigutu.app.domain.repository.LyricsRepository
import com.nigutu.app.domain.repository.MusicRepository
import com.nigutu.app.domain.repository.PlaylistRepository
import com.nigutu.app.domain.repository.SettingsRepository
import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Provides the Room database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NigutuDatabase =
        Room.databaseBuilder(context, NigutuDatabase::class.java, NigutuDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSongDao(db: NigutuDatabase): SongDao = db.songDao()

    @Provides
    fun provideFavoriteDao(db: NigutuDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun providePlaylistDao(db: NigutuDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideHistoryDao(db: NigutuDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideSearchHistoryDao(db: NigutuDatabase): SearchHistoryDao = db.searchHistoryDao()
}

/** Provides networking primitives. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideJsonHttpClient(client: OkHttpClient, json: Json): JsonHttpClient =
        JsonHttpClient(client, json)
}

/** Binds repository interfaces to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: DeezerCatalogRepository): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(impl: LrcLibLyricsRepository): LyricsRepository

    @Binds
    @Singleton
    abstract fun bindLocalMusicSource(impl: MediaStoreScanner): LocalMusicSource
}
