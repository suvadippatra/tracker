package com.example.data

import com.example.data.dao.HotelDao
import com.example.data.dao.MenuDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BankAccountEntity
import com.example.data.model.HotelEntity
import com.example.data.model.OwnerAccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class QrExportManager(
    private val hotelDao: HotelDao,
    private val menuDao: MenuDao,
    private val transactionDao: TransactionDao,
    private val db: AppDatabase? = null
) {
    private val dataTransferManager: DataTransferManager? = db?.let { DataTransferManager(it) }

    suspend fun generateExportPayload(hotelId: Int): ExportResult = withContext(Dispatchers.IO) {
        exportWholeHotel(hotelId)
    }

    suspend fun exportWholeHotel(hotelId: Int): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportWholeHotelJson(hotelId, includeQrMedia = false)
            val hotel = hotelDao.getHotelByIdSync(hotelId)
            val paymentQrs = extractPaymentQrItems(hotels = listOfNotNull(hotel))
            packageJsonToExportResult(appendQrManifestToJson(json, paymentQrs), paymentQrs)
        } catch (e: Exception) {
            exportWholeHotelLegacy(hotelId)
        }
    }

    suspend fun exportHotelMenus(hotelId: Int): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportHotelMenusJson(hotelId)
            packageJsonToExportResult(json)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export menus via QR")
        }
    }

    suspend fun exportHotelHistory(hotelId: Int): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportHotelHistoryJson(hotelId)
            packageJsonToExportResult(json)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export hotel history via QR")
        }
    }

    suspend fun exportWalletWhole(bankAccountId: Int? = null): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportWalletWholeJson(bankAccountId, includeQrMedia = false)
            val bankAccounts = if (bankAccountId != null) {
                listOfNotNull(db?.bankAccountDao()?.getBankAccountByIdSync(bankAccountId))
            } else {
                db?.bankAccountDao()?.getAllBankAccountsSync() ?: emptyList()
            }
            val paymentQrs = extractPaymentQrItems(bankAccounts = bankAccounts)
            packageJsonToExportResult(appendQrManifestToJson(json, paymentQrs), paymentQrs)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export wallet data via QR")
        }
    }

    suspend fun exportBankAccounts(bankAccountId: Int? = null): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportBankAccountsJson(bankAccountId, includeQrMedia = false)
            val bankAccounts = if (bankAccountId != null) {
                listOfNotNull(db?.bankAccountDao()?.getBankAccountByIdSync(bankAccountId))
            } else {
                db?.bankAccountDao()?.getAllBankAccountsSync() ?: emptyList()
            }
            val paymentQrs = extractPaymentQrItems(bankAccounts = bankAccounts)
            packageJsonToExportResult(appendQrManifestToJson(json, paymentQrs), paymentQrs)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export bank accounts via QR")
        }
    }

    suspend fun exportSingleBankAccount(bankAccountId: Int): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportSingleBankAccountJson(bankAccountId, includeQrMedia = false)
            val bank = db?.bankAccountDao()?.getBankAccountByIdSync(bankAccountId)
            val paymentQrs = extractPaymentQrItems(bankAccounts = listOfNotNull(bank))
            packageJsonToExportResult(appendQrManifestToJson(json, paymentQrs), paymentQrs)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export single bank account via QR")
        }
    }

    suspend fun exportTransactionHistory(bankAccountId: Int? = null): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportTransactionHistoryJson(bankAccountId)
            packageJsonToExportResult(json)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export transaction history via QR")
        }
    }

    suspend fun exportElectricityWhole(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportElectricityWholeJson()
            packageJsonToExportResult(json)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export electricity data via QR")
        }
    }

    suspend fun exportMeterReadings(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportMeterReadingsJson()
            packageJsonToExportResult(json)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export meter readings via QR")
        }
    }

    suspend fun exportUtilityBills(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportUtilityBillsJson()
            packageJsonToExportResult(json)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export utility bills via QR")
        }
    }

    suspend fun exportHotelInfo(hotelId: Int): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportHotelInfoJson(hotelId, includeQrMedia = false)
            val hotel = hotelDao.getHotelByIdSync(hotelId)
            val paymentQrs = extractPaymentQrItems(hotels = listOfNotNull(hotel))
            packageJsonToExportResult(appendQrManifestToJson(json, paymentQrs), paymentQrs)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export hotel info via QR")
        }
    }

    suspend fun exportElectricityRates(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportElectricityRatesJson()
            packageJsonToExportResult(json)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export electricity rates via QR")
        }
    }

    suspend fun exportSettings(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportSettingsJson()
            packageJsonToExportResult(json)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export settings via QR")
        }
    }

    suspend fun exportFullBackup(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val dtm = dataTransferManager ?: throw IllegalStateException("Database required")
            val json = dtm.exportFullBackupJson(includeQrMedia = false)
            val hotels = hotelDao.getAllHotelsSync()
            val bankAccounts = db?.bankAccountDao()?.getAllBankAccountsSync() ?: emptyList()
            val ownerAccounts = db?.ownerAccountDao()?.getAllOwnerAccountsSync()?.filter { !it.isDeleted } ?: emptyList()
            val paymentQrs = extractPaymentQrItems(
                hotels = hotels,
                bankAccounts = bankAccounts,
                ownerAccounts = ownerAccounts
            )
            packageJsonToExportResult(appendQrManifestToJson(json, paymentQrs), paymentQrs)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to export full backup via QR")
        }
    }

    private fun extractPaymentQrItems(
        hotels: List<HotelEntity> = emptyList(),
        bankAccounts: List<BankAccountEntity> = emptyList(),
        ownerAccounts: List<OwnerAccountEntity> = emptyList()
    ): List<QrMediaCodec.PaymentQrTransferItem> {
        val ctx = AppDatabase.getAppContext() ?: return emptyList()
        val result = mutableListOf<QrMediaCodec.PaymentQrTransferItem>()

        hotels.forEach { hotel ->
            val decoded = QrMediaCodec.extractDecodedQrsFromUri(ctx, hotel.qrCodeUri)
            decoded.forEachIndexed { idx, payload ->
                result.add(
                    QrMediaCodec.PaymentQrTransferItem(
                        key = "HOTEL:${hotel.id}:$idx",
                        entityType = "HOTEL",
                        entityName = hotel.name.ifBlank { "Hotel #${hotel.id}" },
                        originalId = hotel.id,
                        slotIndex = idx,
                        totalSlots = decoded.size,
                        decodedQr = payload
                    )
                )
            }
        }

        bankAccounts.forEach { bank ->
            val decoded = QrMediaCodec.extractDecodedQrsFromUri(ctx, bank.qrCodeUri)
            decoded.forEachIndexed { idx, payload ->
                result.add(
                    QrMediaCodec.PaymentQrTransferItem(
                        key = "BANK:${bank.id}:$idx",
                        entityType = "BANK",
                        entityName = bank.name.ifBlank { "Bank #${bank.id}" },
                        originalId = bank.id,
                        slotIndex = idx,
                        totalSlots = decoded.size,
                        decodedQr = payload
                    )
                )
            }
        }

        ownerAccounts.forEach { owner ->
            val decoded = QrMediaCodec.extractDecodedQrsFromUri(ctx, owner.qrCodeUri)
            decoded.forEachIndexed { idx, payload ->
                val label = owner.ownerName.ifBlank { owner.accountName.ifBlank { "Owner #${owner.id}" } }
                result.add(
                    QrMediaCodec.PaymentQrTransferItem(
                        key = "OWNER:${owner.id}:$idx",
                        entityType = "OWNER",
                        entityName = label,
                        originalId = owner.id,
                        slotIndex = idx,
                        totalSlots = decoded.size,
                        decodedQr = payload
                    )
                )
            }
        }

        return result
    }

    private fun appendQrManifestToJson(
        jsonString: String,
        paymentQrs: List<QrMediaCodec.PaymentQrTransferItem>
    ): String {
        if (paymentQrs.isEmpty()) return jsonString
        return try {
            val root = JSONObject(jsonString)
            val arr = JSONArray()
            paymentQrs.forEach { item ->
                arr.put(JSONObject().apply {
                    put("key", item.key)
                    put("type", item.entityType)
                    put("id", item.originalId)
                    put("name", item.entityName)
                    put("slot", item.slotIndex)
                    put("total", item.totalSlots)
                })
            }
            root.put("qrManifest", arr)
            root.toString()
        } catch (_: Exception) {
            jsonString
        }
    }

    fun packageJsonToExportResult(
        jsonString: String,
        paymentQrs: List<QrMediaCodec.PaymentQrTransferItem> = emptyList()
    ): ExportResult {
        return try {
            val rawBytes = jsonString.toByteArray(Charsets.UTF_8)

            // Compress using GZIP
            val baos = ByteArrayOutputStream()
            GZIPOutputStream(baos).use { it.write(rawBytes) }
            val compressedBytes = baos.toByteArray()

            // Encrypt using AES-GCM
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            val secretKey = keyGen.generateKey()

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            val encryptedBytes = cipher.doFinal(compressedBytes)

            ExportResult.Success(
                payload = encryptedBytes,
                key = secretKey.encoded,
                iv = iv,
                originalSize = rawBytes.size,
                compressedSize = compressedBytes.size,
                encryptedSize = encryptedBytes.size,
                paymentQrs = paymentQrs
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult.Error(e.message ?: "Unknown error during QR packaging")
        }
    }

    private suspend fun exportWholeHotelLegacy(hotelId: Int): ExportResult = withContext(Dispatchers.IO) {
        try {
            val hotel = hotelDao.getHotelByIdSync(hotelId) ?: return@withContext ExportResult.Error("Hotel not found")
            val menus = menuDao.getMenusForHotelSync(hotelId)
            val transactions = transactionDao.getTransactionsForHotelSync(hotelId)

            val sanitizedHotel = hotel.copy(qrCodeUri = "", phoneNumber = "", upiId = "")
            val emojiRegex = Regex("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]")
            val sanitizedMenus = menus.map {
                it.copy(
                    banglaName = it.banglaName.replace(emojiRegex, "").trim(),
                    englishName = it.englishName.replace(emojiRegex, "").trim(),
                    emoji = ""
                )
            }

            val jsonRoot = JSONObject()
            val hotelJson = JSONObject().apply {
                put("id", sanitizedHotel.id)
                put("n", sanitizedHotel.name)
                put("loc", sanitizedHotel.location)
                put("adv", sanitizedHotel.advancedPayment)
            }
            jsonRoot.put("h", hotelJson)

            val menuArray = JSONArray()
            sanitizedMenus.forEach { m ->
                val mJson = JSONObject().apply {
                    put("id", m.id)
                    put("bn", m.banglaName)
                    put("en", m.englishName)
                    put("pr", m.price)
                    put("dy", if (m.isDayMenu) 1 else 0)
                    put("nt", if (m.isNightMenu) 1 else 0)
                    put("ad", if (m.isAddOn) 1 else 0)
                }
                menuArray.put(mJson)
            }
            jsonRoot.put("m", menuArray)

            val txArray = JSONArray()
            transactions.forEach { t ->
                val tJson = JSONObject().apply {
                    put("id", t.id)
                    put("ts", t.timestamp)
                    put("n", if (t.isNight) 1 else 0)
                    put("tot", t.totalAmount)
                    put("it", t.itemsSerialized)
                }
                txArray.put(tJson)
            }
            jsonRoot.put("t", txArray)

            packageJsonToExportResult(jsonRoot.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult.Error(e.message ?: "Unknown error during export")
        }
    }
}

sealed class ExportResult {
    data class Success(
        val payload: ByteArray,
        val key: ByteArray,
        val iv: ByteArray,
        val originalSize: Int,
        val compressedSize: Int,
        val encryptedSize: Int,
        val paymentQrs: List<QrMediaCodec.PaymentQrTransferItem> = emptyList()
    ) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
