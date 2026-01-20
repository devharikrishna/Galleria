from PIL import Image, ImageDraw, ImageFilter, ImageFont
import os
import math

# Configuration
PROJECT_ROOT = r"d:\AntiGravityCodes\Galleria"
RES_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "res")

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Colors (HSL-like logic but manual RGB for simplicity)
BG_TOP = (103, 58, 183) # Deep Purple 500
BG_BOTTOM = (63, 81, 181) # Indigo 500

def create_base_icon(size=1024):
    """Generates the master icon (square)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1. Background (Rounded Rect for adaptive "feel", or full fill for legacy)
    # Since we are replacing PNGs, we usually want the shape built-in for legacy, 
    # but modern Android applies the mask content.
    # We will generate a "Full Bleed" version for background if needed, but for simple PNG replacement:
    # We'll create a shaped icon (Squircle/Rounded Rect) for standard, Circle for round.
    
    # Draw Gradient Background (Rounded)
    # Simulating a gradient by drawing processed lines or just a solid linear interpolation?
    # Simple linear interpolation:
    for y in range(size):
        r = int(BG_TOP[0] + (BG_BOTTOM[0] - BG_TOP[0]) * (y / size))
        g = int(BG_TOP[1] + (BG_BOTTOM[1] - BG_TOP[1]) * (y / size))
        b = int(BG_TOP[2] + (BG_BOTTOM[2] - BG_TOP[2]) * (y / size))
        draw.line([(0, y), (size, y)], fill=(r, g, b))

    # 2. Draw "Gallery Stack" (3 photos)
    margin = size * 0.2
    card_w = size * 0.45
    card_h = size * 0.4
    
    # Center calc
    cx, cy = size / 2, size / 2

    # Card 1 (Back, rotated left)
    draw_card(draw, cx - 40, cy - 60, card_w, card_h, -15, (255, 255, 255, 100))
    
    # Card 2 (Middle, rotated right)
    draw_card(draw, cx + 40, cy - 40, card_w, card_h, 15, (255, 255, 255, 180))
    
    # Card 3 (Front, center)
    draw_rect_rounded(draw, cx - card_w/2, cy - card_h/2, card_w, card_h, radius=40, fill=(255, 255, 255, 255))
    
    # Add a "Mountain/Sun" icon on the top card to signify "Image"
    # Sun
    sun_x = cx + card_w * 0.2
    sun_y = cy - card_h * 0.2
    draw.ellipse([sun_x - 20, sun_y - 20, sun_x + 20, sun_y + 20], fill=(255, 193, 7, 255)) # Amber sun

    # Mountains
    m_base = cy + card_h * 0.25
    m_left = cx - card_w * 0.35, m_base
    m_peak1 = cx - card_w * 0.1, m_base - 50
    m_mid = cx + card_w * 0.1, m_base
    m_peak2 = cx + card_w * 0.3, m_base - 80
    m_right = cx + card_w * 0.4, m_base
    
    draw.polygon([m_left, (cx - card_w * 0.35, m_base), m_peak1, m_mid], fill=(76, 175, 80, 255)) # Green
    draw.polygon([m_mid, m_peak2, (cx + card_w * 0.4, m_base), (cx - card_w * 0.4, m_base)], fill=(56, 142, 60, 255)) # Darker Green

    return img

def draw_card(draw, cx, cy, w, h, angle, fill):
    # This is a hacky rotation, proper way is to rotate a separate image and paste.
    # For this script complexity, let's just stick to straight stacks or implement rotation properly.
    # Let's do rotation properly using Image object.
    pass # See create_layered_icon for logic

def create_layered_icon(size=1024):
    """Composites layers properly."""
    base = Image.new("RGBA", (size, size), (0,0,0,0))
    
    # 1. Background
    # Create gradient
    grad = Image.new("RGBA", (size, size), (0,0,0,0))
    draw_g = ImageDraw.Draw(grad)
    for y in range(size):
        r = int(BG_TOP[0] + (BG_BOTTOM[0] - BG_TOP[0]) * (y / size))
        g = int(BG_TOP[1] + (BG_BOTTOM[1] - BG_TOP[1]) * (y / size))
        b = int(BG_TOP[2] + (BG_BOTTOM[2] - BG_TOP[2]) * (y / size))
        draw_g.line([(0, y), (size, y)], fill=(r, g, b))
    base.paste(grad, (0,0))
    
    # 2. Photos
    # Helper to make a card
    def make_card(w, h, color, icon=False):
        c = Image.new("RGBA", (int(w), int(h)), (0,0,0,0))
        d = ImageDraw.Draw(c)
        draw_rect_rounded(d, 0, 0, w, h, w*0.1, color)
        
        if icon:
            # Add simple gallery icon art
            cx, cy = w/2, h/2
            # Sun
            d.ellipse([w*0.7-30, h*0.3-30, w*0.7+30, h*0.3+30], fill=(255, 193, 7, 255))
            # Mountains
            d.polygon([(0, h), (w*0.3, h*0.4), (w*0.6, h)], fill=(76, 175, 80, 255))
            d.polygon([(w*0.4, h), (w*0.7, h*0.2), (w, h)], fill=(139, 195, 74, 255))
            
        return c

    cw, ch = size * 0.5, size * 0.45
    cx, cy = size/2, size/2
    
    # Back card
    c1 = make_card(cw, ch, (255,255,255, 128))
    c1 = c1.rotate(15, expand=True, resample=Image.BICUBIC)
    base.paste(c1, (int(cx - c1.width/2 - 40), int(cy - c1.height/2 - 40)), c1)
    
    # Mid card
    c2 = make_card(cw, ch, (255,255,255, 180))
    c2 = c2.rotate(-10, expand=True, resample=Image.BICUBIC)
    base.paste(c2, (int(cx - c2.width/2 + 30), int(cy - c2.height/2 - 20)), c2)
    
    # Front card
    c3 = make_card(cw, ch, (255,255,255, 255), icon=True)
    base.paste(c3, (int(cx - c3.width/2), int(cy - c3.height/2)), c3)
    
    return base

def draw_rect_rounded(draw, x, y, w, h, radius, fill):
    draw.rounded_rectangle((x, y, x+w, y+h), radius=radius, fill=fill)

def main():
    print("Generating Master Icon...")
    master = create_layered_icon(1024)
    
    # Ensure res dirs exist
    for folder_name, px in SIZES.items():
        folder_path = os.path.join(RES_DIR, folder_name)
        os.makedirs(folder_path, exist_ok=True)
        
        # 1. Standard (Square/Full)
        # Note: In a real adaptive setup, this would be masked. 
        # For simplicity in this script, we assume the base we generated IS the "full bleed" legacy icon.
        # But wait, standard icons shouldn't flood the box.
        # Let's scale it slightly down (80%) and put it on a transparent background?
        # Actually, for modern Android, users usually provide:
        # - ic_launcher (Legacy, usually generated shape manually)
        # - ic_launcher_round (Circle masked)
        
        # Resize
        resized = master.resize((px, px), Image.LANCZOS)
        
        # Save standard
        # For standard, a fully filled square is often too aggressive. Let's make it a Rounded Rect shape explicitly.
        # Or better, let's just mask the master to a squircle/circle.
        
        # --- ic_launcher_round (Circle) ---
        mask = Image.new("L", (px, px), 0)
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, px, px), fill=255)
        round_icon = Image.new("RGBA", (px, px), (0,0,0,0))
        round_icon.paste(resized, (0,0), mask)
        round_icon.save(os.path.join(folder_path, "ic_launcher_round.png"))
        
        # --- ic_launcher (Standard - Squircle-ish or just same) ---
        # We will save the full square as standard, assuming the device might mask it or it looks like a tile.
        # Actually, let's add a slight rounded corner mask to the standard one too so it's not a sharp box.
        mask_sq = Image.new("L", (px, px), 0)
        draw_sq = ImageDraw.Draw(mask_sq)
        draw_sq.rounded_rectangle((0, 0, px, px), radius=px*0.2, fill=255)
        std_icon = Image.new("RGBA", (px, px), (0,0,0,0))
        std_icon.paste(resized, (0,0), mask_sq)
        std_icon.save(os.path.join(folder_path, "ic_launcher.png"))
        
        print(f"Generated {folder_name}: {px}x{px}")

if __name__ == "__main__":
    main()
