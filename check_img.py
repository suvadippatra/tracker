import sys
from PIL import Image

try:
    img = Image.open('app_logo_512px.png')
    img = img.convert("RGBA")
    data = img.getdata()
    
    # Check if there are transparent pixels
    has_alpha = any(pixel[3] < 255 for pixel in data)
    print("Has transparency:", has_alpha)
    
    # Try to remove white/light background just in case, or just scale it
    # If the user says "keeping the background removed", it might mean it ALREADY has a removed background and they just want me to KEEP it removed during compression.
    # Let's compress and save it.
    img.thumbnail((300, 300), Image.Resampling.LANCZOS)
    img.save('app/src/main/res/drawable/app_logo_foreground.png', 'PNG', optimize=True)
    print("Saved as app_logo_foreground.png")
except Exception as e:
    print(e)
