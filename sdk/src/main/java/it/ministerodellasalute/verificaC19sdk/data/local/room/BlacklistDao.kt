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
 *  Created by osarapulov on 4/29/21 11:51 PM
 */

package it.ministerodellasalute.verificaC19sdk.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist")
    fun getAll(): List<Blacklist>

    @Query("SELECT * FROM blacklist WHERE bvalue LIKE :bvalue LIMIT 1")
    fun getById(bvalue: String): Blacklist

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bvalue: Blacklist)

    @Delete
    fun delete(bvalue: Blacklist)

    @Query("DELETE FROM blacklist")
    fun deleteAll()
}