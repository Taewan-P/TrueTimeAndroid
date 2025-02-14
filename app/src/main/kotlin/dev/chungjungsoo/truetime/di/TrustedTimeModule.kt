package dev.chungjungsoo.truetime.di

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.chungjungsoo.truetime.data.TrustedTimeClientAccessor

@Module
@InstallIn(SingletonComponent::class)
object TrustedTimeModule {

    @Provides
    fun provideTrustedTimeClientAccessor(
        @ApplicationContext context: Context
    ): TrustedTimeClientAccessor {
        return object : TrustedTimeClientAccessor {
            override fun createClient(): Task<TrustedTimeClient> {
                return TrustedTime.createClient(context)
            }
        }
    }
}