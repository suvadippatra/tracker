with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

target = '  // implementation(libs.accompanist.permissions)'
repl = '  implementation(libs.accompanist.permissions)'
content = content.replace(target, repl)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
