with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

# Remove the appended lines
if 'glance = "1.1.0"' in content:
    idx = content.find('glance = "1.1.0"')
    content = content[:idx]

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)

with open("app/build.gradle.kts", "r") as f:
    app_content = f.read()

if 'dependencies {\n    implementation(libs.glance.appwidget)' in app_content:
    idx = app_content.find('dependencies {\n    implementation(libs.glance.appwidget)')
    app_content = app_content[:idx]

with open("app/build.gradle.kts", "w") as f:
    f.write(app_content)
