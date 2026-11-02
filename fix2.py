with open("app/src/main/java/com/example/ui/screens/meals/AddMealScreen.kt", "r") as f:
    text = f.read()

target = """                                                }
                                            }
                        }
                    ) { innerPadding ->"""
                    
replacement = """                                                }
                                            }
                                        }
                        }
                    ) { innerPadding ->"""
                    
if target in text:
    text = text.replace(target, replacement)
    print("Fixed bottomBar braces")
else:
    print("Target not found")
    
# now add 7 closing braces at the end
end_braces = """
                        }
                    }
                }
            }
        }
    }
}
"""
text = text.rstrip() + end_braces

with open("app/src/main/java/com/example/ui/screens/meals/AddMealScreen.kt", "w") as f:
    f.write(text)
