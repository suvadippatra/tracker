package com.example.data

import com.example.data.dao.HotelDao
import com.example.data.dao.MenuDao
import com.example.data.dao.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class QrManifestItem(
    val key: String,
    val entityType: String,
    val originalId: Int,
    val entityName: String,
    val slotIndex: Int,
    val totalSlots: Int
)

class QrImportManager(
    private val hotelDao: HotelDao,
    private val menuDao: MenuDao,
    private val transactionDao: TransactionDao,
    private val db: AppDatabase? = null
) {
    companion object {
        fun decryptToJson(
            encryptedPayload: ByteArray,
            keyBytes: ByteArray,
            ivBytes: ByteArray
        ): String {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val compressedBytes = cipher.doFinal(encryptedPayload)
            val bais = ByteArrayInputStream(compressedBytes)
            val jsonBytes = GZIPInputStream(bais).readBytes()
            return String(jsonBytes, Charsets.UTF_8)
        }

        fun peekQrManifest(
            encryptedPayload: ByteArray,
            keyBytes: ByteArray,
            ivBytes: ByteArray
        ): List<QrManifestItem> {
            return runCatching {
                val jsonString = decryptToJson(encryptedPayload, keyBytes, ivBytes)
                val root = JSONObject(jsonString)
                val arr = root.optJSONArray("qrManifest") ?: return@runCatching emptyList()
                val items = mutableListOf<QrManifestItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val key = obj.optString("key", "")
                    if (key.isNotBlank()) {
                        items.add(
                            QrManifestItem(
                                key = key,
                                entityType = obj.optString("type", "PAYMENT"),
                                originalId = obj.optInt("id", -1),
                                entityName = obj.optString("name", "Payment QR"),
                                slotIndex = obj.optInt("slot", 0),
                                totalSlots = obj.optInt("total", 1)
                            )
                        )
                    }
                }
                items
            }.getOrDefault(emptyList())
        }
    }

    suspend fun importPayload(
        encryptedPayload: ByteArray,
        keyBytes: ByteArray,
        ivBytes: ByteArray,
        targetHotelId: Int = 1,
        targetBankAccountId: Int = 1,
        scannedPaymentQrUris: Map<String, String> = emptyMap()
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val rawJson = decryptToJson(encryptedPayload, keyBytes, ivBytes)

            if (db != null) {
                val dtm = DataTransferManager(db)
                val finalJson = if (scannedPaymentQrUris.isNotEmpty()) {
                    dtm.injectScannedPaymentQrsIntoJson(rawJson, scannedPaymentQrUris)
                } else {
                    rawJson
                }
                val outcome = dtm.importJson(
                    finalJson,
                    targetHotelId = targetHotelId,
                    targetBankAccountId = targetBankAccountId
                )
                return@withContext when (outcome) {
                    is ImportOutcome.Success -> {
                        val qrSuffix = if (scannedPaymentQrUris.isNotEmpty()) {
                            " (+${scannedPaymentQrUris.size} exact payment QR(s))"
                        } else ""
                        ImportResult.Success(
                            newHotelId = outcome.newHotelId ?: 0,
                            message = outcome.message + qrSuffix,
                            count = outcome.count
                        )
                    }
                    is ImportOutcome.Error -> ImportResult.Error(outcome.message)
                }
            }

            // Fallback manual parser for legacy calls where db wasn't supplied directly
            val root = JSONObject(rawJson)
            if (root.has("h") && root.has("m")) {
                val hJson = root.getJSONObject("h")
                val name = hJson.getString("n")
                val location = hJson.optString("loc", "")
                val advancedPayment = hJson.optDouble("adv", 0.0)

                val newHotel = com.example.data.model.HotelEntity(
                    name = name,
                    location = location,
                    advancedPayment = advancedPayment
                )
                val newHotelId = hotelDao.insertHotel(newHotel).toInt()

                val mArray = root.getJSONArray("m")
                val menusToInsert = mutableListOf<com.example.data.model.MenuEntity>()
                for (i in 0 until mArray.length()) {
                    val mJson = mArray.getJSONObject(i)
                    menusToInsert.add(
                        com.example.data.model.MenuEntity(
                            hotelId = newHotelId,
                            banglaName = mJson.getString("bn"),
                            englishName = mJson.getString("en"),
                            price = mJson.getDouble("pr"),
                            isDayMenu = mJson.getInt("dy") == 1,
                            isNightMenu = mJson.getInt("nt") == 1,
                            isAddOn = mJson.getInt("ad") == 1,
                            emoji = "🍽️",
                            type = "Common"
                        )
                    )
                }
                menuDao.insertMenuItems(menusToInsert)

                if (root.has("t")) {
                    val tArray = root.getJSONArray("t")
                    val txs = mutableListOf<com.example.data.model.HotelTransactionEntity>()
                    for (i in 0 until tArray.length()) {
                        val tJson = tArray.getJSONObject(i)
                        txs.add(
                            com.example.data.model.HotelTransactionEntity(
                                hotelId = newHotelId,
                                timestamp = tJson.getLong("ts"),
                                isNight = tJson.getInt("n") == 1,
                                totalAmount = tJson.getDouble("tot"),
                                itemsSerialized = tJson.getString("it")
                            )
                        )
                    }
                    transactionDao.insertTransactions(txs)
                }

                return@withContext ImportResult.Success(
                    newHotelId = newHotelId,
                    message = "Hotel '$name' imported successfully with ${menusToInsert.size} menus",
                    count = menusToInsert.size
                )
            }

            ImportResult.Error("Unsupported data format")
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Error(e.message ?: "Unknown error during import")
        }
    }
}

sealed class ImportResult {
    data class Success(
        val newHotelId: Int = 0,
        val message: String = "Imported successfully",
        val count: Int = 1
    ) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
