/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2022 T-Systems International GmbH and all other contributors
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
 *  Created by mykhailo.nester on 4/26/21 1:53 PM
 */

package it.ministerodellasalute.verificaC19sdk.data.repository

import android.util.Log
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.methodName
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseRepository(private val dispatcherProvider: DispatcherProvider) : Repository {

    suspend fun <P> execute(doOnAsyncBlock: suspend () -> P): P? {
        return withContext(dispatcherProvider.getIO()) {
            return@withContext try {
                Log.v(methodName(), "doing network coroutine work")
                doOnAsyncBlock.invoke()
            } catch (e: UnknownHostException) {
                Log.e(methodName(), "UnknownHostException", e)
                null
            } catch (e: SocketTimeoutException) {
                Log.e(methodName(), "SocketTimeoutException", e)
                null
            } catch (throwable: Throwable) {
                Log.e(methodName(), "Throwable", throwable)
                null
            }
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun ResponseBody.stringSuspending(dispatcherProvider: DispatcherProvider) =
    withContext(dispatcherProvider.getIO()) { string() }