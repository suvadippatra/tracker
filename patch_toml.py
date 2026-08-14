with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

target = '[libraries]\n'
repl = '[libraries]\nzxing = { group = "com.google.zxing", name = "core", version.ref = "zxing" }\n'
content = content.replace(target, repl)

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(content)
