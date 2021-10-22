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
 *  Created by Mykhailo Nester on 4/23/21 9:48 AM
 */

package it.ministerodellasalute.verificaC19sdk.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dgca.verifier.app.decoder.base45.Base45Service
import dgca.verifier.app.decoder.base45.DefaultBase45Service
import dgca.verifier.app.decoder.cbor.CborService
import dgca.verifier.app.decoder.cbor.DefaultCborService
import dgca.verifier.app.decoder.compression.CompressorService
import dgca.verifier.app.decoder.compression.DefaultCompressorService
import dgca.verifier.app.decoder.cose.CoseService
import dgca.verifier.app.decoder.cose.CryptoService
import dgca.verifier.app.decoder.cose.DefaultCoseService
import dgca.verifier.app.decoder.cose.VerificationCryptoService
import dgca.verifier.app.decoder.prefixvalidation.DefaultPrefixValidationService
import dgca.verifier.app.decoder.prefixvalidation.PrefixValidationService
import dgca.verifier.app.decoder.schema.DefaultSchemaValidator
import dgca.verifier.app.decoder.schema.SchemaValidator
import dgca.verifier.app.decoder.services.X509
import javax.inject.Singleton

/**
 *
 * This object acts as a data module for the Decoder's services.
 *
 */
@InstallIn(SingletonComponent::class)
@Module
object DecoderModule {

    /**
     *
     * This method provides the [PrefixValidationService] instance.
     *
     */
    @Singleton
    @Provides
    fun providePrefixValidationService(): PrefixValidationService = DefaultPrefixValidationService()

    /**
     *
     * This method provides the [Base45Service] instance.
     *
     */
    @ExperimentalUnsignedTypes
    @Singleton
    @Provides
    fun provideBase45Decoder(): Base45Service = DefaultBase45Service()

    /**
     *
     * This method provides the [CompressorService] instance.
     *
     */
    @Singleton
    @Provides
    fun provideCompressorService(): CompressorService = DefaultCompressorService()

    /**
     *
     * This method provides the [CoseService] instance.
     *
     */
    @Singleton
    @Provides
    fun provideCoseService(): CoseService = DefaultCoseService()

    /**
     *
     * This method provides the [SchemaValidator] instance.
     *
     */
    @Singleton
    @Provides
    fun provideSchemaValidator(): SchemaValidator = DefaultSchemaValidator()

    /**
     *
     * This method provides the [CborService] instance.
     *
     */
    @Singleton
    @Provides
    fun provideCborService(): CborService = DefaultCborService()

    /**
     *
     * This method provides the [CryptoService] instance.
     *
     */
    @Singleton
    @Provides
    fun provideCryptoService(): CryptoService = VerificationCryptoService(X509())
}