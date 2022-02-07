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
 *  Created by osarapulov on 4/30/21 12:07 AM
 */

package it.ministerodellasalute.verificaC19sdk.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 *
 * This class defines the database configuration and serves as the app's main access point to the
 * persisted data.
 *
 */
@Database(entities = [Key::class, Blacklist::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao
    abstract fun blackListDao(): BlacklistDao

    companion object {
        @JvmField
        val MIGRATION_1_2 = Migration1To2()
    }

    class Migration1To2 : Migration(1,2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. Create new table
            database.execSQL("CREATE TABLE IF NOT EXISTS 'blacklist' ('bvalue' TEXT NOT NULL, PRIMARY KEY('bvalue'))")
        }
    }
}