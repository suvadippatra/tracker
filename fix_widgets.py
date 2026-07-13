import os

for file in os.listdir("app/src/main/java/com/example/widget/"):
    if not file.endswith(".kt"): continue
    filepath = "app/src/main/java/com/example/widget/" + file
    with open(filepath, "r") as f:
        code = f.read()
    code = code.replace("getUserProfile()", "getUserProfileSync()")
    code = code.replace("getHotelById(hotelId)", "getHotelByIdSync(hotelId)")
    code = code.replace("getAllBankAccountsList()", "getAllBankAccountsSync()")
    # Fix properties of Entity:
    # HotelEntity name is `name` (already correct)
    # BankAccountEntity name is `name` (already correct)
    with open(filepath, "w") as f:
        f.write(code)

print("Updated widgets")
