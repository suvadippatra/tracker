package com.example.service.upi

import com.example.data.model.UpiTransactionType
import com.example.utils.GitHubReleaseAsset
import com.example.utils.GitHubUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionParserTest {

    @Test
    fun parsePhonePeDebitNotification() {
        val parsed = TransactionParser.parseNotification(
            packageName = "com.phonepe.app",
            title = "Paid ₹450 to Swiggy Instamart",
            text = "Debited from HDFC Bank **1234. UPI Ref No 429812345678",
            bigText = null,
            timestamp = 1_700_000_000_000L
        )
        assertNotNull(parsed)
        assertEquals(450.0, parsed!!.amount, 0.001)
        assertEquals(UpiTransactionType.DEBIT, parsed.transactionType)
        assertEquals("PhonePe", parsed.sourceAppOrBank)
        assertEquals("429812345678", parsed.upiRefNumber)
        assertTrue(parsed.counterpartyOrReceiverId.contains("Swiggy", ignoreCase = true))
    }

    @Test
    fun parseGooglePayCreditNotification() {
        val parsed = TransactionParser.parseNotification(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Rahul Sharma paid you ₹1,250.50",
            text = "Credited to your State Bank of India account. VPA rahul.sharma@okaxis",
            bigText = null
        )
        assertNotNull(parsed)
        assertEquals(1250.50, parsed!!.amount, 0.001)
        assertEquals(UpiTransactionType.CREDIT, parsed.transactionType)
        assertEquals("Google Pay", parsed.sourceAppOrBank)
        assertEquals("rahul.sharma@okaxis", parsed.vpa)
    }

    @Test
    fun parsePaytmDebitWithIndianCommaFormat() {
        val parsed = TransactionParser.parseNotification(
            packageName = "net.one97.paytm",
            title = "Payment Successful",
            text = "Rs. 1,25,450.75 paid to rent.owner@paytm via Paytm UPI. Ref 501234567890",
            bigText = null
        )
        assertNotNull(parsed)
        assertEquals(125450.75, parsed!!.amount, 0.001)
        assertEquals(UpiTransactionType.DEBIT, parsed.transactionType)
        assertEquals("Paytm", parsed.sourceAppOrBank)
        assertEquals("rent.owner@paytm", parsed.vpa)
        assertEquals("501234567890", parsed.upiRefNumber)
    }

    @Test
    fun parseBharatPeAndCredAndJioAndMobiKwikNotifications() {
        val bharatPe = TransactionParser.parseNotification(
            packageName = "com.bharatpe.app",
            title = "Payment Received",
            text = "Received ₹320 from arjun@ybl on BharatPe QR",
            bigText = null
        )
        assertNotNull(bharatPe)
        assertEquals(320.0, bharatPe!!.amount, 0.001)
        assertEquals(UpiTransactionType.CREDIT, bharatPe.transactionType)
        assertEquals("BharatPe", bharatPe.sourceAppOrBank)
        assertEquals("arjun@ybl", bharatPe.vpa)

        val cred = TransactionParser.parseNotification(
            packageName = "com.dreamplug.androidapp",
            title = "CRED UPI",
            text = "Paid INR 1999 to Zomato Gold via CRED UPI UTR 432198765432",
            bigText = null
        )
        assertNotNull(cred)
        assertEquals(1999.0, cred!!.amount, 0.001)
        assertEquals(UpiTransactionType.DEBIT, cred.transactionType)
        assertEquals("CRED", cred.sourceAppOrBank)
        assertEquals("432198765432", cred.upiRefNumber)

        val jio = TransactionParser.parseNotification(
            packageName = "com.jio.myjio",
            title = "JioFinance UPI",
            text = "Sent Rs.299.00 to jio.recharge@icici",
            bigText = null
        )
        assertNotNull(jio)
        assertEquals(299.0, jio!!.amount, 0.001)
        assertEquals("JioFinance", jio.sourceAppOrBank)
    }

    @Test
    fun parseHdfcBankSmsDebitWithVpaAndRrn() {
        val parsed = TransactionParser.parseBankSms(
            sender = "VM-HDFCBK",
            body = "Sent Rs.780.00 From HDFC Bank A/C *4512 To merchant.cafe@okhdfcbank On 18/11/26 Ref 432109876543 Not You? Call 18002586161"
        )
        assertNotNull(parsed)
        assertEquals(780.0, parsed!!.amount, 0.001)
        assertEquals(UpiTransactionType.DEBIT, parsed.transactionType)
        assertEquals("HDFC Bank", parsed.sourceAppOrBank)
        assertEquals("merchant.cafe@okhdfcbank", parsed.vpa)
        assertEquals("432109876543", parsed.upiRefNumber)
    }

    @Test
    fun parseSbiUpiSmsDebitAndCredit() {
        val debit = TransactionParser.parseBankSms(
            sender = "AD-SBIUPI",
            body = "Dear UPI user A/C X8921 debited by 645.0 on date 19Nov26 trf to SHARMA MEDICALS Refno 612345678901. If not u? call 1800111109 -SBI"
        )
        assertNotNull(debit)
        assertEquals(645.0, debit!!.amount, 0.001)
        assertEquals(UpiTransactionType.DEBIT, debit.transactionType)
        assertEquals("SBI", debit.sourceAppOrBank)
        assertEquals("612345678901", debit.upiRefNumber)
        assertTrue(debit.counterpartyOrReceiverId.contains("SHARMA MEDICALS", ignoreCase = true))

        val credit = TransactionParser.parseBankSms(
            sender = "AX-ICICIB",
            body = "Acct XX304 credited with Rs 15,000.00 on 19-Nov-26; Info: UPI/512345678912/salary.desk@icici. Avl Bal Rs 48,200.00"
        )
        assertNotNull(credit)
        assertEquals(15000.0, credit!!.amount, 0.001)
        assertEquals(UpiTransactionType.CREDIT, credit.transactionType)
        assertEquals("ICICI Bank", credit.sourceAppOrBank)
        assertEquals("salary.desk@icici", credit.vpa)
        assertEquals("512345678912", credit.upiRefNumber)
    }

    @Test
    fun ignoresOtpPromotionalFailedAndRequestMessages() {
        val otp = TransactionParser.parseBankSms(
            sender = "VM-HDFCBK",
            body = "482910 is your OTP for Rs.500.00 UPI payment. Do not share OTP with anyone."
        )
        assertNull(otp)

        val promo = TransactionParser.parseNotification(
            packageName = "com.phonepe.app",
            title = "Win ₹500 Cashback!",
            text = "Pay electricity bill and get flat ₹100 cashback offer today",
            bigText = null
        )
        assertNull(promo)

        val collectRequest = TransactionParser.parseNotification(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Payment request",
            text = "Rohan has requested ₹800 from you. Tap to pay.",
            bigText = null
        )
        assertNull(collectRequest)

        val failed = TransactionParser.parseBankSms(
            sender = "AD-SBIUPI",
            body = "Your UPI transaction of Rs. 950.00 has failed and was declined by bank."
        )
        assertNull(failed)

        val nonAllowlistedApp = TransactionParser.parseNotification(
            packageName = "com.instagram.android",
            title = "Paid ₹500 to friend",
            text = "Sent ₹500 via UPI",
            bigText = null
        )
        assertNull(nonAllowlistedApp)
    }

    @Test
    fun gitHubUpdaterSelectsLatestCompiledCompatibleApkAndComparesSemver() {
        val assets = listOf(
            GitHubReleaseAsset(
                name = "traker-v1.5-armeabi-v7a.apk",
                downloadUrl = "https://example.com/v7a.apk",
                sizeBytes = 12_000_000L,
                updatedAt = "2026-11-18T10:00:00Z"
            ),
            GitHubReleaseAsset(
                name = "traker-v1.5-arm64-v8a-old.apk",
                downloadUrl = "https://example.com/arm64-old.apk",
                sizeBytes = 14_000_000L,
                updatedAt = "2026-11-18T11:00:00Z"
            ),
            GitHubReleaseAsset(
                name = "traker-v1.5-arm64-v8a-latest.apk",
                downloadUrl = "https://example.com/arm64-latest.apk",
                sizeBytes = 14_500_000L,
                updatedAt = "2026-11-19T08:30:00Z"
            )
        )
        val picked = GitHubUpdateManager.selectBestCompiledApk(
            assets = assets,
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a")
        )
        assertEquals("traker-v1.5-arm64-v8a-latest.apk", picked.name)

        assertTrue(GitHubUpdateManager.isRemoteVersionNewer("2.1.0", "v2.2.0"))
        assertTrue(GitHubUpdateManager.isRemoteVersionNewer("2.1.0", "2.1.1"))
        assertFalse(GitHubUpdateManager.isRemoteVersionNewer("2.2.0", "v2.2.0"))
        assertFalse(GitHubUpdateManager.isRemoteVersionNewer("2.3.0", "v2.2.9"))
    }
}
