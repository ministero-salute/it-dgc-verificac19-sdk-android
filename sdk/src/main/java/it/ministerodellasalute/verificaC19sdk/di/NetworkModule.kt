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
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException

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
        val httpClient = getUnsafeOkHttpClient(cache).apply {
            addInterceptor(HeaderInterceptor())
        }


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

    private fun getUnsafeOkHttpClient(cache: Cache): OkHttpClient.Builder {
        return try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?, authType: String?
                ) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?, authType: String?
                ) {
                }

                override fun getAcceptedIssuers(): Array<out java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory
            val trustManagerFactory: TrustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers: Array<TrustManager> = trustManagerFactory.trustManagers
            check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                "Unexpected default trust managers:" + trustManagers.contentToString()
            }

            val trustManager = trustManagers[0] as X509TrustManager


            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustManager)
            builder.cache(cache)
            builder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            builder.readTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            builder.hostnameVerifier { _, _ -> true }
            return builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     *
     * This method creates the [Retrofit] instance for the passing [Lazy] of [OkHttpClient] type.
     *
     */
    private fun createRetrofit(okHttpClient: Lazy<OkHttpClient>): Retrofit {
        return Retrofit.Builder().addConverterFactory(GsonConverterFactory.create(Gson()))
            .baseUrl(BuildConfig.BASE_URL).callFactory { okHttpClient.get().newCall(it) }.build()
    }
}

@PublishedApi
internal inline fun Retrofit.Builder.callFactory(
    crossinline body: (Request) -> Call
) = callFactory(object : Call.Factory {
    override fun newCall(request: Request): Call = body(request)
})