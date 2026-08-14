with open('app/src/main/java/com/example/ui/components/ZXingAnalyzer.kt', 'r') as f:
    content = f.read()

target = 'if (image.format == ImageFormat.YUV_420_888 || image.format == ImageFormat.YUV_CAMERA) {'
repl = 'if (image.format == ImageFormat.YUV_420_888) {'
content = content.replace(target, repl)

with open('app/src/main/java/com/example/ui/components/ZXingAnalyzer.kt', 'w') as f:
    f.write(content)
