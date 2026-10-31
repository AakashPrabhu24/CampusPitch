import os
from PIL import Image, ImageDraw, ImageFilter

def process_icons():
    src_path = "app_icon_preview.jpg"
    img = Image.open(src_path).convert("RGBA")
    w, h = img.size

    # Ensure square
    if w != h:
        dim = min(w, h)
        img = img.crop(((w - dim)//2, (h - dim)//2, (w + dim)//2, (h + dim)//2))
    
    # 1. Generate 512x512 Play Store icon
    playstore_icon = img.resize((512, 512), Image.Resampling.LANCZOS)
    playstore_icon.save("app/src/main/ic_launcher-web.png", "PNG")
    print("Saved app/src/main/ic_launcher-web.png")

    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    res_dir = os.path.join("app", "src", "main", "res")

    for folder, size in densities.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)

        # Standard icon (with smooth rounded squircle corners)
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Rounded corner mask (squircle radius ~ 20% of size)
        mask = Image.new("L", (size, size), 0)
        draw = ImageDraw.Draw(mask)
        corner_rad = int(size * 0.22)
        draw.rounded_rectangle([0, 0, size, size], radius=corner_rad, fill=255)
        
        squircle_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        squircle_icon.paste(resized, (0, 0), mask=mask)
        
        # Save ic_launcher.webp
        out_square = os.path.join(folder_path, "ic_launcher.webp")
        squircle_icon.save(out_square, "WEBP", quality=95)

        # Circular icon (round mask)
        circle_mask = Image.new("L", (size, size), 0)
        draw_circle = ImageDraw.Draw(circle_mask)
        draw_circle.ellipse([0, 0, size - 1, size - 1], fill=255)
        
        circle_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        circle_icon.paste(resized, (0, 0), mask=circle_mask)
        
        # Save ic_launcher_round.webp
        out_round = os.path.join(folder_path, "ic_launcher_round.webp")
        circle_icon.save(out_round, "WEBP", quality=95)

        print(f"Generated {size}x{size} icons for {folder}")

    # Adaptive Icon Foreground (432x432 px = 108dp @ 4x xxxhdpi)
    # The inner 72dp is 288px (66.6% of 432).
    # We place the 3D emblem comfortably inside this area on a transparent canvas
    # or center-crop with smooth alpha feathering so it adapts seamlessly.
    fg_size = 432
    emblem_size = int(fg_size * 0.72)
    emblem_resized = img.resize((emblem_size, emblem_size), Image.Resampling.LANCZOS)
    
    # Feather emblem edges so it blends cleanly into the adaptive background
    emblem_mask = Image.new("L", (emblem_size, emblem_size), 0)
    draw_em = ImageDraw.Draw(emblem_mask)
    draw_em.ellipse([0, 0, emblem_size - 1, emblem_size - 1], fill=255)
    emblem_mask = emblem_mask.filter(ImageFilter.GaussianBlur(radius=4))
    
    fg_canvas = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
    offset = (fg_size - emblem_size) // 2
    fg_canvas.paste(emblem_resized, (offset, offset), mask=emblem_mask)
    
    drawable_dir = os.path.join(res_dir, "drawable-xxxhdpi")
    os.makedirs(drawable_dir, exist_ok=True)
    fg_canvas.save(os.path.join(drawable_dir, "ic_launcher_foreground.png"), "PNG")
    print("Generated adaptive foreground PNG")

if __name__ == "__main__":
    process_icons()
