import re

with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "r") as f:
    content = f.read()

old_logic = r"""                                if \(hotel == null\) \{
                                    viewModel\.addHotel\(HotelEntity\(
                                        name = editHotelName,
                                        advancedPayment = adv,
                                        phoneNumber = editPhone,
                                        location = editLocation,
                                        upiId = editUpiId,
                                        qrCodeUri = editQrCodeUris\.joinToString\(","\)
                                    \)\)
                                \} else \{
                                    viewModel\.updateHotel\(hotel\.copy\(
                                        name = editHotelName,
                                        advancedPayment = adv,
                                        phoneNumber = editPhone,
                                        location = editLocation,
                                        upiId = editUpiId,
                                        qrCodeUri = editQrCodeUris\.joinToString\(","\)
                                    \)\)
                                \}"""

new_logic = """                                if (hotel == null) {
                                    viewModel.addHotel(
                                        name = editHotelName,
                                        advancedPayment = adv,
                                        phoneNumber = editPhone,
                                        location = editLocation,
                                        upiId = editUpiId,
                                        qrCodeUri = editQrCodeUris.joinToString(",")
                                    )
                                } else {
                                    viewModel.editHotel(hotel.copy(
                                        name = editHotelName,
                                        advancedPayment = adv,
                                        phoneNumber = editPhone,
                                        location = editLocation,
                                        upiId = editUpiId,
                                        qrCodeUri = editQrCodeUris.joinToString(",")
                                    ))
                                }"""

content = re.sub(old_logic, new_logic, content)

with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "w") as f:
    f.write(content)
print("Fixed successfully!")
