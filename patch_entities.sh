#!/bin/bash
python3 -c '
import re

with open("app/src/main/java/com/example/data/model/Entities.kt", "r") as f:
    content = f.read()

# Add isDeleted to ExtraExpenseEntity
content = re.sub(r"(val notes: String = \"\")\n\)", r"\1,\n    val isDeleted: Boolean = false\n)", content)

# Add isDeleted to HotelTransactionEntity
content = re.sub(r"(val totalAmount: Double)\n\)", r"\1,\n    val isDeleted: Boolean = false\n)", content)

# Add isDeleted to TransactionCategoryEntity
content = re.sub(r"(val isSystem: Boolean = false)\n\)", r"\1,\n    val isDeleted: Boolean = false\n)", content)

# Add isDeleted to AttachmentEntity
content = re.sub(r"(val timestamp: Long = System.currentTimeMillis\(\))\n\)", r"\1,\n    val isDeleted: Boolean = false\n)", content)

# Add isDeleted to OwnerAccountEntity
content = re.sub(r"(val qrCodeUri: String = \"\")\n\)", r"\1,\n    val isDeleted: Boolean = false\n)", content)

# Add isDeleted to HotelPaymentEntity
content = re.sub(r"(val note: String = \"\")\n\)", r"\1,\n    val isDeleted: Boolean = false\n)", content)

# Add isDeleted to UtilityBill
content = re.sub(r"(val transactionId: String\? = null)\n\)", r"\1,\n    val isDeleted: Boolean = false\n)", content)

# Add isDeleted to ElectricityRate
content = re.sub(r"(val ratePerUnit: Double)\n\)", r"\1,\n    val isDeleted: Boolean = false\n)", content)

with open("app/src/main/java/com/example/data/model/Entities.kt", "w") as f:
    f.write(content)
'
