import os
import math
from PIL import Image, ImageDraw, ImageFilter

def create_campus_pitch_icon(size=512):
    # Create canvas with RGBA
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # 1. Background Rounded Hexagon / Squircle or Circle
    # Draw background on a high-res surface
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_bg = ImageDraw.Draw(bg)
    
    center = size / 2.0
    radius = size * 0.44  # Outer circular radius
    
    # Deep Midnight Navy background circle with glowing gradient
    steps = 60
    for i in range(steps):
        r = radius * (1.0 - i / float(steps) * 0.95)
        factor = i / float(steps)
        # Deep space navy to rich cyan-tinted navy
        red = int(10 + (22 - 10) * (1 - factor))
        green = int(15 + (35 - 15) * (1 - factor))
        blue = int(29 + (70 - 29) * (1 - factor))
        draw_bg.ellipse(
            [center - r, center - r, center + r, center + r],
            fill=(red, green, blue, 255)
        )
        
    # Subtle ambient inner glow
    glow_steps = 30
    for i in range(glow_steps):
        r = radius * 0.7 * (1.0 - i / float(glow_steps))
        alpha = int(45 * (1.0 - i / float(glow_steps)))
        draw_bg.ellipse(
            [center - r, center - r, center + r, center + r],
            fill=(6, 182, 212, alpha)
        )

    # High-tech concentric orbital rings
    draw_bg.ellipse(
        [center - radius * 0.88, center - radius * 0.88, center + radius * 0.88, center + radius * 0.88],
        outline=(16, 185, 129, 70), width=max(1, int(size * 0.006))
    )
    draw_bg.ellipse(
        [center - radius * 0.62, center - radius * 0.62, center + radius * 0.62, center + radius * 0.62],
        outline=(6, 182, 212, 60), width=max(1, int(size * 0.004))
    )

    # 2. Glowing Thrust / Rocket Flame at the bottom
    thrust = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_thrust = ImageDraw.Draw(thrust)
    
    # Flame outer glow (Cyan to Emerald)
    flame_pts_outer = [
        (center - size * 0.09, center + size * 0.16),
        (center, center + size * 0.36),
        (center + size * 0.09, center + size * 0.16)
    ]
    draw_thrust.polygon(flame_pts_outer, fill=(6, 182, 212, 180))
    
    # Flame inner core (Bright Emerald to White)
    flame_pts_inner = [
        (center - size * 0.045, center + size * 0.16),
        (center, center + size * 0.30),
        (center + size * 0.045, center + size * 0.16)
    ]
    draw_thrust.polygon(flame_pts_inner, fill=(255, 255, 255, 230))
    
    # Flame blur for neon plume effect
    thrust_blur = thrust.filter(ImageFilter.GaussianBlur(radius=int(size * 0.015)))
    bg.alpha_composite(thrust_blur)
    bg.alpha_composite(thrust)

    # 3. Child Chiller "W" Wings (Left wing, Right wing)
    wings = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_wings = ImageDraw.Draw(wings)
    
    # Left Wing (Stylized chevron sweeping out to create the left peak of 'W')
    # Wing tip at (center - 0.26, center + 0.12)
    left_wing = [
        (center - size * 0.05, center - size * 0.05),   # Top inner connection
        (center - size * 0.28, center + size * 0.15),   # Outer left sweep tip
        (center - size * 0.24, center + size * 0.22),   # Bottom wing outer
        (center - size * 0.14, center + size * 0.15),   # Inner elbow
        (center - size * 0.05, center + size * 0.18),   # Base at rocket fuselage
    ]
    draw_wings.polygon(left_wing, fill=(16, 185, 129, 255)) # Emerald
    
    # Left Wing highlight / facet
    left_wing_facet = [
        (center - size * 0.05, center - size * 0.05),
        (center - size * 0.28, center + size * 0.15),
        (center - size * 0.16, center + size * 0.08)
    ]
    draw_wings.polygon(left_wing_facet, fill=(52, 211, 153, 220)) # Light emerald

    # Right Wing (Stylized chevron sweeping out to create the right peak of 'W')
    right_wing = [
        (center + size * 0.05, center - size * 0.05),   # Top inner connection
        (center + size * 0.28, center + size * 0.15),   # Outer right sweep tip
        (center + size * 0.24, center + size * 0.22),   # Bottom wing outer
        (center + size * 0.14, center + size * 0.15),   # Inner elbow
        (center + size * 0.05, center + size * 0.18),   # Base at rocket fuselage
    ]
    draw_wings.polygon(right_wing, fill=(6, 182, 212, 255)) # Cyan
    
    right_wing_facet = [
        (center + size * 0.05, center - size * 0.05),
        (center + size * 0.28, center + size * 0.15),
        (center + size * 0.16, center + size * 0.08)
    ]
    draw_wings.polygon(right_wing_facet, fill=(34, 211, 238, 220)) # Light cyan

    # Soft shadow behind fuselage
    wings_shadow = wings.filter(ImageFilter.GaussianBlur(radius=int(size * 0.02)))
    bg.alpha_composite(wings)

    # 4. Center Rocket Fuselage (Venture Rocket / Arrow of Growth)
    fuse = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_fuse = ImageDraw.Draw(fuse)
    
    # Rocket body: Nose at (center, center - 0.28)
    nose_y = center - size * 0.28
    body_pts = [
        (center, nose_y),                                # Nose apex
        (center + size * 0.10, center - size * 0.05),    # Upper right shoulder
        (center + size * 0.08, center + size * 0.18),    # Lower right base
        (center, center + size * 0.15),                  # Bottom center notch
        (center - size * 0.08, center + size * 0.18),    # Lower left base
        (center - size * 0.10, center - size * 0.05),    # Upper left shoulder
    ]
    draw_fuse.polygon(body_pts, fill=(240, 246, 252, 255)) # Bright pearl white/silver
    
    # Left half specular shade (Light emerald gradient)
    body_left = [
        (center, nose_y),
        (center - size * 0.10, center - size * 0.05),
        (center - size * 0.08, center + size * 0.18),
        (center, center + size * 0.15)
    ]
    draw_fuse.polygon(body_left, fill=(209, 250, 229, 255)) # Emerald tinted white
    
    # Right half specular shade (Cyan tinted white)
    body_right = [
        (center, nose_y),
        (center + size * 0.10, center - size * 0.05),
        (center + size * 0.08, center + size * 0.18),
        (center, center + size * 0.15)
    ]
    draw_fuse.polygon(body_right, fill=(224, 242, 254, 255)) # Cyan tinted white

    # Central Spine Line
    draw_fuse.line([(center, nose_y), (center, center + size * 0.15)], fill=(16, 185, 129, 180), width=max(2, int(size * 0.008)))

    # 5. Glowing Central Cockpit Diamond (Pitch Core)
    core_cy = center + size * 0.01
    core_w = size * 0.05
    core_h = size * 0.075
    core_diamond = [
        (center, core_cy - core_h),
        (center + core_w, core_cy),
        (center, core_cy + core_h),
        (center - core_w, core_cy)
    ]
    draw_fuse.polygon(core_diamond, fill=(16, 185, 129, 255)) # Emerald primary
    
    # Core inner glow
    core_inner = [
        (center, core_cy - core_h * 0.5),
        (center + core_w * 0.5, core_cy),
        (center, core_cy + core_h * 0.5),
        (center - core_w * 0.5, core_cy)
    ]
    draw_fuse.polygon(core_inner, fill=(52, 211, 153, 255)) # Light emerald
    draw_fuse.ellipse([center - size*0.012, core_cy - size*0.012, center + size*0.012, core_cy + size*0.012], fill=(255, 255, 255, 240))

    # 6. Venture Spark / Star at Apex (Nose tip)
    spark_cy = nose_y - size * 0.01
    spark_rad = size * 0.05
    spark_pts = [
        (center, spark_cy - spark_rad),
        (center + spark_rad * 0.28, spark_cy - spark_rad * 0.28),
        (center + spark_rad, spark_cy),
        (center + spark_rad * 0.28, spark_cy + spark_rad * 0.28),
        (center, spark_cy + spark_rad),
        (center - spark_rad * 0.28, spark_cy + spark_rad * 0.28),
        (center - spark_rad, spark_cy),
        (center - spark_rad * 0.28, spark_cy - spark_rad * 0.28),
    ]
    draw_fuse.polygon(spark_pts, fill=(251, 191, 36, 255)) # Golden star
    draw_fuse.ellipse([center - size*0.008, spark_cy - size*0.008, center + size*0.008, spark_cy + size*0.008], fill=(255, 255, 255, 255))

    bg.alpha_composite(fuse)

    # 7. Sleek Outer Border Rim
    draw_rim = ImageDraw.Draw(bg)
    draw_rim.ellipse(
        [center - radius, center - radius, center + radius, center + radius],
        outline=(52, 211, 153, 200),
        width=max(2, int(size * 0.012))
    )

    return bg

if __name__ == "__main__":
    icon = create_campus_pitch_icon(512)
    icon.save("test_icon.png")
    print("Generated test_icon.png successfully")
