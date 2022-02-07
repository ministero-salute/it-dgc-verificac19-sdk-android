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
 *  Created by climent on 6/14/21 1:13 PM
 */

package it.ministerodellasalute.verificaC19sdk.di

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import it.ministerodellasalute.verificaC19sdk.data.local.prefs.PreferencesImpl
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DataModuleTest {

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    lateinit var context: Context

    private lateinit var module: DataModule

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        module = DataModule
    }

    @Test
    fun `test PreferencesImpl dependency`() {
        MatcherAssert.assertThat(
            "PreferencesImpl",
            module.providePreferences(context),
            CoreMatchers.instanceOf(PreferencesImpl::class.java)
        )
    }
}