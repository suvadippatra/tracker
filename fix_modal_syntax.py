import re
with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'r') as f:
    content = f.read()

# Fix the syntax error at line 138
content = content.replace("""                                    }
                                )
                            }
                        },
            bottomBar = {""", """                                    }
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {""")

with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'w') as f:
    f.write(content)
print("done")
