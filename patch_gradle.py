with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

target = """  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)"""

repl = """  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)"""

content = content.replace(target, repl)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
