import re
with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'r') as f:
    content = f.read()

content = content.replace("""                        }
                    }
                }
            }
                    }
                }
            }
        ) { innerPadding ->""", """                        }
                    }
                }
            }
        ) { innerPadding ->""")

with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'w') as f:
    f.write(content)
print("done")
