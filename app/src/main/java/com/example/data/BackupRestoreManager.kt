package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRestoreManager(private val context: Context) {
    private val dbName = "traker_database"

    suspend fun createBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // Force checkpoint by closing database? 
            // In Android Room, calling query("PRAGMA wal_checkpoint(FULL)") is a way.
            val db = AppDatabase.getDatabase(context)
            db.query("PRAGMA wal_checkpoint(FULL)", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    // Checkpoint successful
                }
            }

            val dbFile = context.getDatabasePath(dbName)
            val shmFile = File(dbFile.path + "-shm")
            val walFile = File(dbFile.path + "-wal")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zos ->
                    val filesToBackup = listOf(dbFile, shmFile, walFile)
                    for (file in filesToBackup) {
                        if (file.exists()) {
                            FileInputStream(file).use { fis ->
                                val entry = ZipEntry(file.name)
                                zos.putNextEntry(entry)
                                fis.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            val shmFile = File(dbFile.path + "-shm")
            val walFile = File(dbFile.path + "-wal")

            // We must close the database before overwriting
            AppDatabase.getDatabase(context).close()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = when (entry.name) {
                            dbFile.name -> dbFile
                            shmFile.name -> shmFile
                            walFile.name -> walFile
                            else -> null
                        }

                        if (outFile != null) {
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            // Kill the process so it restarts with the new DB
            Runtime.getRuntime().exit(0)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
