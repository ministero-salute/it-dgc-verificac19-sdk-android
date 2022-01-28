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
 *  Created by nicolamcornelio on 1/27/22, 10:59 AM
 */

package it.ministerodellasalute.verificaC19sdk.data.local.room

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import it.ministerodellasalute.verificaC19sdk.data.local.AppDatabase
import it.ministerodellasalute.verificaC19sdk.data.local.Key
import it.ministerodellasalute.verificaC19sdk.data.local.KeyDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var keyDao: KeyDao
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        keyDao = database.keyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertTest() = runBlocking {
        val kid1 = Key("kid1", "key1")

        keyDao.insert(kid1)
        val result = keyDao.getById("kid1")

        assertThat(result).isEqualTo(kid1)
    }

    @Test
    fun deleteAllExceptTest() = runBlocking {
        val kid1 = Key("kid1", "key1")
        val kid2 = Key("kid2", "key2")
        val kid3 = Key("kid3", "key3")

        keyDao.insert(kid1)
        keyDao.insert(kid2)
        keyDao.insert(kid3)
        keyDao.deleteAllExcept(mutableListOf("kid3").toTypedArray())

        val result = keyDao.getById("kid3")
        val count = keyDao.getCount()

        assertThat(result).isEqualTo(kid3)
        assertThat(count).isEqualTo(1)
    }
}