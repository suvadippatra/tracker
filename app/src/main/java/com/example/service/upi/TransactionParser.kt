package com.example.service.upi

import com.example.data.model.UpiTransactionType
import java.util.Locale

data class ParsedUpiTransaction(
    val amount: Double,
    val transactionType: UpiTransactionType,
    val counterpartyOrReceiverId: String,
    val sourceAppOrBank: String,
    val upiRefNumber: String? = null,
    val vpa: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val rawMessage: String = ""
)

/**
 * 100% On-Device Multi-Pattern Regex & NLP Parsing Engine for Indian UPI apps & bank alerts.
 * Strictly zero network calls; parses notifications and transactional SMS locally.
 */
object TransactionParser {

    val SUPPORTED_UPI_PACKAGES: Map<String, String> = mapOf(
        "com.phonepe.app" to "PhonePe",
        "com.phonepe.app.business" to "PhonePe Business",
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.google.android.apps.nbu.paisa.merchant" to "Google Pay Business",
        "net.one97.paytm" to "Paytm",
        "com.paytm.business" to "Paytm Business",
        "com.bharatpe.app" to "BharatPe",
        "com.postpe.app" to "PostPe",
        "com.jio.myjio" to "JioFinance",
        "com.jio.jiofinance" to "JioFinance",
        "com.mobikwik_new" to "MobiKwik",
        "in.org.npci.upiapp" to "BHIM UPI",
        "in.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.dreamplug.androidapp" to "CRED",
        "com.whatsapp" to "WhatsApp Pay",
        "com.freecharge.android" to "Freecharge",
        "com.naviapp" to "Navi UPI",
        "money.super.payments" to "super.money",
        "indwin.c3.shareapp" to "Slice UPI",
        "money.jupiter" to "Jupiter",
        "com.epifi.paisa" to "Fi Money",
        "com.myairtelapp" to "Airtel Thanks UPI"
    )

    val SUPPORTED_BANK_PACKAGES: Map<String, String> = mapOf(
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.hdfcbank.payzapp" to "HDFC PayZapp",
        "com.sbi.lotusintouch" to "YONO SBI",
        "com.sbi.SBIFreedomPlus" to "YONO Lite SBI",
        "com.csam.icici.bank.imobile" to "ICICI iMobile",
        "com.axis.mobile" to "Axis Mobile",
        "com.msf.kbank.mobile" to "Kotak 811",
        "com.kotak811mobilebankingapp.instantsavingsupiprogram" to "Kotak 811",
        "com.Version1" to "PNB One",
        "com.canarabank.mobility" to "Canara ai1",
        "com.bob.mconnect" to "bob World",
        "com.idfcfirstbank.optimus" to "IDFC FIRST Bank",
        "com.fss.indus" to "IndusMobile",
        "in.co.bankofbaroda.mpassbook" to "Bank of Baroda",
        "com.yesbank.yesmobile" to "YES Bank",
        "com.fedmobile" to "Federal Bank",
        "com.unionbank.ecommerce.mobile.android" to "Union Bank"
    )

    private val SMS_MESSAGING_PACKAGES: Set<String> = setOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.oneplus.mms",
        "com.miui.mms"
    )

    val BANK_SMS_SENDER_MAP: Map<String, String> = mapOf(
        "HDFCBK" to "HDFC Bank",
        "HDFC" to "HDFC Bank",
        "SBIUPI" to "SBI",
        "SBIPSG" to "SBI",
        "SBIINB" to "SBI",
        "SBICRD" to "SBI",
        "CBSSBI" to "SBI",
        "ICICIB" to "ICICI Bank",
        "ICICI" to "ICICI Bank",
        "AXISBK" to "Axis Bank",
        "AXIS" to "Axis Bank",
        "KOTAKB" to "Kotak Bank",
        "KOTAK" to "Kotak Bank",
        "PNBSMS" to "PNB",
        "PUNBNK" to "PNB",
        "CANBNK" to "Canara Bank",
        "UNIONB" to "Union Bank",
        "UBOI" to "Union Bank",
        "IDFCFB" to "IDFC FIRST Bank",
        "INDUSB" to "IndusInd Bank",
        "YESBNK" to "YES Bank",
        "BOBTXN" to "Bank of Baroda",
        "BARODA" to "Bank of Baroda",
        "IOBCHN" to "Indian Overseas Bank",
        "FEDBNK" to "Federal Bank",
        "PAYTMB" to "Paytm Payments Bank",
        "AIRBNK" to "Airtel Payments Bank",
        "JIOPAY" to "Jio Payments Bank",
        "KBLBNK" to "Karnataka Bank",
        "SCBLIN" to "Standard Chartered",
        "HSBCIN" to "HSBC India",
        "BOIIND" to "Bank of India",
        "CBIIND" to "Central Bank of India",
        "UCOBNK" to "UCO Bank",
        "INDBNK" to "Indian Bank",
        "BANDHN" to "Bandhan Bank",
        "RBLBNK" to "RBL Bank",
        "AUFINB" to "AU Small Finance Bank",
        "EQUITB" to "Equitas Bank"
    )

    // Matches ₹1,23,456.75 | Rs. 450 | Rs 1,250.00 | INR 1999 | ₹500
    private val AMOUNT_REGEX = Regex(
        """(?i)(?:₹|rs\.?|inr)\s*([0-9]{1,3}(?:,[0-9]{2,3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
    )

    // Fallback for formats like "500.00 INR" or "1200 Rs"
    private val SUFFIX_AMOUNT_REGEX = Regex(
        """(?i)\b([0-9]{1,3}(?:,[0-9]{2,3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:rs\.?|inr|₹)\b"""
    )

    // Real-world SBI UPI SMS format omits currency symbol: "debited by 645.0 on date 19Nov26"
    private val VERB_AMOUNT_REGEX = Regex(
        """(?i)\b(?:debited\s+by|credited\s+by|credited\s+with|paid|sent|received)\s+([0-9]{1,3}(?:,[0-9]{2,3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\b"""
    )

    // Standard UPI VPA: name.123_abc@okhdfcbank / merchant@ybl / 9876543210@paytm
    private val VPA_REGEX = Regex(
        """\b([a-zA-Z0-9][a-zA-Z0-9.\-_]{1,63}@[a-zA-Z]{2,30})\b"""
    )

    // 12-digit UPI RRN / UTR reference numbers with common Indian bank labels (including SBI's 'Refno')
    private val UPI_REF_LABELED_REGEX = Regex(
        """(?i)(?:upi\s*ref(?:\s*no\.?|no\.?|\s*id)?|refno\.?|ref(?:\s*no\.?|\s*id)?|utr(?:\s*no\.?|no\.?)?|rrn(?:\s*no\.?|no\.?)?|txn\s*id|upi\/(?:dr|cr|p2p|p2m)?\/?)\s*[:#\-/\s]*([0-9]{12})\b"""
    )

    // Standalone 12-digit numeric token (used carefully when UPI context is present)
    private val UPI_REF_FALLBACK_REGEX = Regex("""\b([0-9]{12})\b""")

    private val DEBIT_PHRASES = listOf(
        "paid to", "sent to", "debited", "transferred to", "trf to",
        "payment of", "paid rs", "paid ₹", "paid inr", "sent rs", "sent ₹",
        "sent inr", "spent on", "spent at", "withdrawn", "deducted",
        "money sent", "payment successful to", "you paid", "paid via upi"
    )

    private val CREDIT_PHRASES = listOf(
        "received from", "credited", "added to your", "deposited",
        "money received", "received rs", "received ₹", "received inr",
        "you received", "refund of", "cashback credited", "has sent you",
        "paid you", "sent you ₹", "sent you rs", "sent you inr"
    )

    private val IGNORED_PHRASES = listOf(
        "otp", "one time password", "one-time password", "verification code",
        "do not share", "login code", "secret code", "pin is",
        "failed", "declined", "unsuccessful", "could not be completed",
        "payment request", "has requested", "requested ₹", "requested rs", "requested inr",
        "collect request", "approve request", "pending approval",
        "will be debited", "auto-pay reminder", "autopay reminder", "mandate created",
        "mandate paused", "due on", "bill due", "is due", "emi due",
        "win up to", "get up to", "flat ₹", "flat rs", "offer expires",
        "scratch card", "refer and earn", "pre-approved loan", "apply now",
        "kyc pending", "update kyc", "limit is", "available limit"
    )

    private val COUNTERPARTY_PATTERNS = listOf(
        Regex("""(?i)(?:paid|sent|transferred)\s+(?:₹|rs\.?|inr)\s*[0-9,]+(?:\.[0-9]{1,2})?\s+to\s+([A-Za-z0-9][A-Za-z0-9 .@&_'\-]{1,48})"""),
        Regex("""(?i)(?:received)\s+(?:₹|rs\.?|inr)\s*[0-9,]+(?:\.[0-9]{1,2})?\s+from\s+([A-Za-z0-9][A-Za-z0-9 .@&_'\-]{1,48})"""),
        Regex("""(?i)(?:paid\s+to|sent\s+to|transferred\s+to|trf\s+to|transfer\s+to|to\s+vpa|to\s+upi\s+id)\s+([A-Za-z0-9][A-Za-z0-9 .@&_'\-]{1,48})"""),
        Regex("""(?i)(?:received\s+from|credited\s+by|from\s+vpa|by\s+vpa|sent\s+by)\s+([A-Za-z0-9][A-Za-z0-9 .@&_'\-]{1,48})"""),
        Regex("""(?i)(?:at|to)\s+([A-Z][A-Za-z0-9 .&'\-]{2,36})\s+(?:via|on|using|ref|refno|utr|upi|avl)"""),
        Regex("""(?i)^([A-Za-z][A-Za-z0-9 .&'\-]{1,36})\s+(?:paid\s+you|has\s+sent\s+you|sent\s+you)""")
    )

    private val DLT_SENDER_REGEX = Regex("""^[A-Z]{2}[-]?([A-Z0-9]{4,9})$""")

    fun isAllowedNotificationPackage(packageName: String): Boolean {
        return packageName in SUPPORTED_UPI_PACKAGES ||
            packageName in SUPPORTED_BANK_PACKAGES ||
            packageName in SMS_MESSAGING_PACKAGES
    }

    /**
     * Checks whether an SMS sender header matches a transactional Indian bank DLT ID
     * such as `VM-HDFCBK`, `AD-SBIUPI`, `JD-ICICIB`, `AX-AXISBK`, `BK-KOTAKB`.
     */
    fun resolveBankFromSmsSender(sender: String): String? {
        val cleaned = sender.trim().uppercase(Locale.ROOT)
        if (cleaned.isEmpty()) return null
        // Reject personal 10-12 digit phone numbers
        val digitsOnly = cleaned.filter { it.isDigit() }
        if (digitsOnly.length >= 10) return null

        val headerCore = if (cleaned.contains("-")) {
            cleaned.substringAfter("-").trim()
        } else {
            val match = DLT_SENDER_REGEX.find(cleaned)
            match?.groupValues?.get(1) ?: cleaned
        }

        BANK_SMS_SENDER_MAP[headerCore]?.let { return it }
        BANK_SMS_SENDER_MAP.entries.firstOrNull { (code, _) ->
            headerCore.contains(code)
        }?.let { return it.value }

        return null
    }

    /**
     * Parses a notification from an allowlisted UPI app, bank app, or SMS notification.
     */
    fun parseNotification(
        packageName: String,
        title: String,
        text: String,
        bigText: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): ParsedUpiTransaction? {
        if (packageName == "com.tracker.suva" || packageName == "com.example") return null
        if (!isAllowedNotificationPackage(packageName)) return null

        val bodyText = listOfNotNull(
            bigText?.trim()?.takeIf { it.isNotEmpty() },
            text.trim().takeIf { it.isNotEmpty() }
        ).distinct().joinToString(" ")

        val combined = listOf(title.trim(), bodyText)
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (combined.length < 8) return null

        val sourceAppOrBank = when {
            packageName in SUPPORTED_UPI_PACKAGES -> SUPPORTED_UPI_PACKAGES[packageName]!!
            packageName in SUPPORTED_BANK_PACKAGES -> SUPPORTED_BANK_PACKAGES[packageName]!!
            packageName in SMS_MESSAGING_PACKAGES -> {
                resolveBankFromSmsSender(title) ?: inferBankFromText(combined) ?: return null
            }
            else -> return null
        }

        // For SMS messaging app notifications, require transactional UPI/bank wording
        if (packageName in SMS_MESSAGING_PACKAGES && !hasBankOrUpiContext(combined)) {
            return null
        }

        return parseCommon(
            title = title.trim(),
            combined = combined,
            sourceAppOrBank = sourceAppOrBank,
            timestamp = timestamp
        )
    }

    /**
     * Parses an incoming or inbox Bank SMS message.
     */
    fun parseBankSms(
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): ParsedUpiTransaction? {
        val bankName = resolveBankFromSmsSender(sender) ?: return null
        val cleanedBody = body.replace(Regex("""\s+"""), " ").trim()
        if (cleanedBody.length < 10) return null
        if (!hasBankOrUpiContext(cleanedBody)) return null

        return parseCommon(
            title = sender,
            combined = cleanedBody,
            sourceAppOrBank = bankName,
            timestamp = timestamp
        )
    }

    private fun parseCommon(
        title: String,
        combined: String,
        sourceAppOrBank: String,
        timestamp: Long
    ): ParsedUpiTransaction? {
        val lower = combined.lowercase(Locale.ROOT)

        // Filter out OTPs, promos, failed/declined, collect requests, and upcoming reminders
        if (IGNORED_PHRASES.any { lower.contains(it) }) return null

        // Determine transaction direction with careful bank SMS disambiguation
        val txType = classifyTransactionType(lower) ?: return null

        // Extract INR/₹/Rs amount
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0.0) return null

        // Extract VPA (ignore email addresses like support@bank.com by rejecting dot in domain TLD)
        val vpa = extractVpa(combined)

        // Extract 12-digit UPI RRN / UTR reference number
        val upiRef = extractUpiRefNumber(combined)

        // Extract counterparty: prefer VPA if present or clean merchant/person name
        val counterparty = extractCounterparty(title, combined, vpa, sourceAppOrBank)

        return ParsedUpiTransaction(
            amount = amount,
            transactionType = txType,
            counterpartyOrReceiverId = counterparty.take(64),
            sourceAppOrBank = sourceAppOrBank,
            upiRefNumber = upiRef,
            vpa = vpa,
            timestamp = timestamp,
            rawMessage = combined.take(320)
        )
    }

    private fun classifyTransactionType(lower: String): UpiTransactionType? {
        val hasAccountDebited = lower.contains("debited") ||
            lower.contains("withdrawn") ||
            lower.contains("deducted") ||
            lower.contains("spent")

        val hasAccountCredited = lower.contains("a/c") && lower.contains("credited") ||
            lower.contains("acct") && lower.contains("credited") ||
            lower.contains("account") && lower.contains("credited") ||
            lower.contains("credited to your") ||
            lower.contains("credited with")

        // Bank SMS like "Your A/c X1234 is debited by Rs.500 and credited to VPA merchant@okaxis"
        if (hasAccountDebited) {
            val debitIdx = listOf("debited", "withdrawn", "deducted", "spent")
                .map { lower.indexOf(it) }
                .filter { it >= 0 }
                .minOrNull() ?: Int.MAX_VALUE
            val creditIdx = listOf("credited to your", "credited with", "received from")
                .map { lower.indexOf(it) }
                .filter { it >= 0 }
                .minOrNull() ?: Int.MAX_VALUE
            if (debitIdx < creditIdx) return UpiTransactionType.DEBIT
        }

        if (hasAccountCredited && !hasAccountDebited) {
            return UpiTransactionType.CREDIT
        }

        val isDebit = DEBIT_PHRASES.any { lower.contains(it) }
        val isCredit = CREDIT_PHRASES.any { lower.contains(it) }

        return when {
            isCredit && !isDebit -> UpiTransactionType.CREDIT
            isDebit && !isCredit -> UpiTransactionType.DEBIT
            isDebit && isCredit -> {
                // Disambiguate e.g. "Paid ₹500 ... received confirmation" vs "Received ₹500 ... paid by"
                val firstDebit = DEBIT_PHRASES.mapNotNull { p -> lower.indexOf(p).takeIf { it >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                val firstCredit = CREDIT_PHRASES.mapNotNull { p -> lower.indexOf(p).takeIf { it >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                if (firstCredit < firstDebit) UpiTransactionType.CREDIT else UpiTransactionType.DEBIT
            }
            else -> null
        }
    }

    private fun extractAmount(text: String): Double? {
        // Prefer amount directly near transaction verb rather than "Avl Bal Rs 25,000"
        val cleanedForPrimary = text
            .substringBefore("Avl Bal", text)
            .substringBefore("Avl bal", text)
            .substringBefore("Avail Bal", text)
            .substringBefore("Available Balance", text)
            .substringBefore("Bal:", text)

        val primaryMatch = AMOUNT_REGEX.find(cleanedForPrimary)
            ?: SUFFIX_AMOUNT_REGEX.find(cleanedForPrimary)
            ?: VERB_AMOUNT_REGEX.find(cleanedForPrimary)
            ?: AMOUNT_REGEX.find(text)
            ?: SUFFIX_AMOUNT_REGEX.find(text)
            ?: VERB_AMOUNT_REGEX.find(text)
            ?: return null

        val numericStr = primaryMatch.groupValues[1].replace(",", "")
        return numericStr.toDoubleOrNull()
    }

    private fun extractVpa(text: String): String? {
        for (match in VPA_REGEX.findAll(text)) {
            val candidate = match.groupValues[1]
            val afterIndex = match.range.last + 1
            // Skip standard email addresses like care@hdfcbank.com ('.' followed by a TLD letter),
            // while still accepting a VPA at the end of a sentence like "salary.desk@icici. Avl Bal"
            if (afterIndex + 1 < text.length && text[afterIndex] == '.' && text[afterIndex + 1].isLetter()) continue
            val handle = candidate.substringAfter("@").lowercase(Locale.ROOT)
            if (handle in setOf("gmail", "yahoo", "outlook", "hotmail", "icloud", "proton")) continue
            return candidate
        }
        return null
    }

    private fun extractUpiRefNumber(text: String): String? {
        UPI_REF_LABELED_REGEX.find(text)?.let { return it.groupValues[1] }
        val lower = text.lowercase(Locale.ROOT)
        if (lower.contains("upi") || lower.contains("ref") || lower.contains("utr") || lower.contains("rrn")) {
            for (match in UPI_REF_FALLBACK_REGEX.findAll(text)) {
                val num = match.groupValues[1]
                // Avoid matching masked account or phone-like numbers starting with 91
                if (!num.startsWith("919") && !num.startsWith("918") && !num.startsWith("917") && !num.startsWith("916")) {
                    return num
                }
            }
        }
        return null
    }

    private fun extractCounterparty(
        title: String,
        combined: String,
        vpa: String?,
        sourceAppOrBank: String
    ): String {
        if (!vpa.isNullOrBlank()) return vpa

        for (pattern in COUNTERPARTY_PATTERNS) {
            val m = pattern.find(combined)
            if (m != null) {
                val raw = cleanCounterpartyCandidate(m.groupValues[1])
                if (raw.length >= 2 && !raw.equals("your", ignoreCase = true) && !raw.equals("a/c", ignoreCase = true)) {
                    return raw
                }
            }
        }

        // Clean title fallback (e.g. Google Pay title "Zomato" or "Paid to Rahul")
        val cleanedTitle = cleanCounterpartyCandidate(
            title
                .replace(AMOUNT_REGEX, "")
                .replace(Regex("""(?i)\b(paid|sent|received|payment|successful|completed|money|to|from|via|upi)\b"""), "")
        )
        if (cleanedTitle.length >= 2 && !cleanedTitle.equals(sourceAppOrBank, ignoreCase = true) && !cleanedTitle.contains("-")) {
            return cleanedTitle
        }

        return "$sourceAppOrBank UPI"
    }

    private fun cleanCounterpartyCandidate(input: String): String {
        return input
            .replace(Regex("""(?i)\b(debited|credited|on|via|using|ref|refno|utr|rrn|upi|avl|avail|bal|for|with|info|a/c|acct|account)\b.*$"""), "")
            .replace(Regex("""[.,:;()]+$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun hasBankOrUpiContext(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("upi") ||
            lower.contains("vpa") ||
            lower.contains("a/c") ||
            lower.contains("acct") ||
            lower.contains("account") ||
            lower.contains("bank") ||
            lower.contains("imps") ||
            lower.contains("neft") ||
            lower.contains("utr") ||
            lower.contains("rrn")
    }

    private fun inferBankFromText(text: String): String? {
        val upper = text.uppercase(Locale.ROOT)
        return when {
            upper.contains("HDFC") -> "HDFC Bank"
            upper.contains("SBI") -> "SBI"
            upper.contains("ICICI") -> "ICICI Bank"
            upper.contains("AXIS") -> "Axis Bank"
            upper.contains("KOTAK") -> "Kotak Bank"
            upper.contains("PNB") || upper.contains("PUNJAB NATIONAL") -> "PNB"
            upper.contains("CANARA") -> "Canara Bank"
            upper.contains("IDFC") -> "IDFC FIRST Bank"
            upper.contains("INDUSIND") -> "IndusInd Bank"
            upper.contains("YES BANK") -> "YES Bank"
            upper.contains("BANK OF BARODA") || upper.contains("BOB") -> "Bank of Baroda"
            upper.contains("UPI") -> "Bank SMS Alert"
            else -> null
        }
    }
}
