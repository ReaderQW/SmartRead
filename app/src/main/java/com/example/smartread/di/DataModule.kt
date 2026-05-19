package com.example.smartread.di

import android.content.Context
import androidx.room.Room
import com.example.smartread.data.local.SmartReadDatabase
import com.example.smartread.data.remote.AigcApi
import com.example.smartread.data.repository.BookRepositoryImpl
import com.example.smartread.data.repository.ChatRepositoryImpl
import com.example.smartread.data.repository.KnowledgeRepositoryImpl
import com.example.smartread.data.repository.NoteRepositoryImpl
import com.example.smartread.data.repository.ReaderRepositoryImpl
import com.example.smartread.data.repository.ReportRepositoryImpl
import com.example.smartread.data.vector.EmbeddingApi
import com.example.smartread.domain.repository.BookRepository
import com.example.smartread.domain.repository.ChatRepository
import com.example.smartread.domain.repository.KnowledgeRepository
import com.example.smartread.domain.repository.NoteRepository
import com.example.smartread.domain.repository.ReaderRepository
import com.example.smartread.domain.repository.ReportRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartReadDatabase =
        Room.databaseBuilder(context, SmartReadDatabase::class.java, "smartread.db").build()

    @Provides fun provideBookDao(database: SmartReadDatabase) = database.bookDao()
    @Provides fun provideHighlightDao(database: SmartReadDatabase) = database.highlightDao()
    @Provides fun provideNoteDao(database: SmartReadDatabase) = database.noteDao()
    @Provides fun provideChatDao(database: SmartReadDatabase) = database.chatDao()
    @Provides fun provideEmbeddingDao(database: SmartReadDatabase) = database.embeddingDao()
    @Provides fun provideKnowledgeGraphDao(database: SmartReadDatabase) = database.knowledgeGraphDao()
    @Provides fun provideReportDao(database: SmartReadDatabase) = database.reportDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder().addInterceptor(logging).build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides fun provideAigcApi(retrofit: Retrofit): AigcApi = retrofit.create(AigcApi::class.java)
    @Provides fun provideEmbeddingApi(retrofit: Retrofit): EmbeddingApi = retrofit.create(EmbeddingApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository
    @Binds abstract fun bindReaderRepository(impl: ReaderRepositoryImpl): ReaderRepository
    @Binds abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
    @Binds abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
    @Binds abstract fun bindKnowledgeRepository(impl: KnowledgeRepositoryImpl): KnowledgeRepository
    @Binds abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository
}
