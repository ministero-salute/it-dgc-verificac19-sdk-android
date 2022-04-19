/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ---license-end
 *
 *  Created by mykhailo.nester on 4/24/21 1:41 PM
 */

package it.ministerodellasalute.verificaC19sdk.di

import android.content.Context
import com.google.gson.Gson
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.ministerodellasalute.verificaC19sdk.BuildConfig
import it.ministerodellasalute.verificaC19sdk.data.remote.ApiService
import it.ministerodellasalute.verificaC19sdk.network.HeaderInterceptor
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val CONNECT_TIMEOUT = 30L

/**
 *
 * This object acts as a data module for the Network's services.
 *
 */
@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    /**
     *
     * This method provides the [Cache] instance for the passing [Context].
     *
     */
    @Singleton
    @Provides
    internal fun provideCache(@ApplicationContext context: Context): Cache {
        val cacheSize = (10 * 1024 * 1024).toLong() // 10 MB
        return Cache(context.cacheDir, cacheSize)
    }

    /**
     *
     * This method provides the [OkHttpClient] instance for the passing [Cache].
     *
     */
    @Singleton
    @Provides
    internal fun provideOkhttpClient(cache: Cache): OkHttpClient {
        val httpClient = getHttpClient(cache).apply {
            addInterceptor(HeaderInterceptor())
        }
        addCertificateSHA(httpClient)


        return httpClient.build()
    }

    /**
     *
     * This method provides the [Retrofit] instance for the passing [Lazy] of [OkHttpClient] type.
     *
     */
    @Singleton
    @Provides
    internal fun provideRetrofit(okHttpClient: Lazy<OkHttpClient>): Retrofit {
        return createRetrofit(okHttpClient)
    }

    /**
     *
     * This method provides the [ApiService] instance for the passing [Retrofit].
     *
     */
    @Singleton
    @Provides
    internal fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    /**
     *
     * This method gets the [OkHttpClient.Builder] instance for the passing [Cache].
     *
     */
    private fun getHttpClient(cache: Cache): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
    }

    /**
     *
     * This method adds the [CertificatePinner.Builder] to the passing [OkHttpClient.Builder].
     *
     */
    private fun addCertificateSHA(httpClient: OkHttpClient.Builder) {
        val certificatePinner = CertificatePinner.Builder()
            .add(BuildConfig.SERVER_HOST, BuildConfig.LEAF_CERTIFICATE)
            .add(BuildConfig.SERVER_HOST, BuildConfig.BACKUP_CERTIFICATE)
        httpClient.certificatePinner(certificatePinner.build())
    }

    /**
     *
     * This method creates the [Retrofit] instance for the passing [Lazy] of [OkHttpClient] type.
     *
     */
    private fun createRetrofit(okHttpClient: Lazy<OkHttpClient>): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .baseUrl(BuildConfig.BASE_URL)
            .callFactory { okHttpClient.get().newCall(it) }
            .build()
    }
}

@PublishedApi
internal inline fun Retrofit.Builder.callFactory(
    crossinline body: (Request) -> Call
) = callFactory(object : Call.Factory {
    override fun newCall(request: Request): Call = body(request)
})