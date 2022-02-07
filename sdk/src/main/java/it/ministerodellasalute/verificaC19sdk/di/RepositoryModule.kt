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
 *  Created by mykhailo.nester on 4/24/21 2:19 PM
 */

package it.ministerodellasalute.verificaC19sdk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.ministerodellasalute.verificaC19sdk.data.repository.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.data.repository.VerifierRepositoryImpl
import javax.inject.Singleton

/**
 *
 * This object acts as a data module for the [VerifierRepository].
 *
 */
@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {

    /**
     *
     * This method provides the [VerifierRepository] instance for the passing [VerifierRepositoryImpl].
     *
     */
    @Singleton
    @Binds
    abstract fun bindVerifierRepository(verifierRepository: VerifierRepositoryImpl): VerifierRepository
}