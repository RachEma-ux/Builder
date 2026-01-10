#!/usr/bin/env python3
"""
Create simple launcher icons for Builder app
Generates PNG files without requiring PIL/Pillow
"""

import struct
import zlib
import os

def create_png(width, height, rgb_color, output_path):
    """Create a simple solid-color PNG file."""

    def png_chunk(chunk_type, data):
        """Create a PNG chunk."""
        chunk_data = chunk_type + data
        crc = zlib.crc32(chunk_data) & 0xffffffff
        return struct.pack('>I', len(data)) + chunk_data + struct.pack('>I', crc)

    # PNG signature
    png_signature = b'\x89PNG\r\n\x1a\n'

    # IHDR chunk (image header)
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    ihdr_chunk = png_chunk(b'IHDR', ihdr_data)

    # IDAT chunk (image data)
    # Create raw image data (RGB pixels)
    r, g, b = rgb_color
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)  # Filter type (0 = None)
        for x in range(width):
            raw_data.extend([r, g, b])

    # Compress the raw data
    compressed_data = zlib.compress(bytes(raw_data), 9)
    idat_chunk = png_chunk(b'IDAT', compressed_data)

    # IEND chunk (image trailer)
    iend_chunk = png_chunk(b'IEND', b'')

    # Write PNG file
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'wb') as f:
        f.write(png_signature + ihdr_chunk + idat_chunk + iend_chunk)

    print(f"Created: {output_path} ({width}x{height})")

def create_builder_icon(width, height, output_path):
    """Create a Builder-themed launcher icon."""

    # Create a simple gradient/two-tone icon
    # We'll create a purple background with accent
    def png_chunk(chunk_type, data):
        """Create a PNG chunk."""
        chunk_data = chunk_type + data
        crc = zlib.crc32(chunk_data) & 0xffffffff
        return struct.pack('>I', len(data)) + chunk_data + struct.pack('>I', crc)

    # PNG signature
    png_signature = b'\x89PNG\r\n\x1a\n'

    # IHDR chunk
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    ihdr_chunk = png_chunk(b'IHDR', ihdr_data)

    # Create gradient from primary to primary_dark
    primary = (98, 0, 238)  # #6200EE
    primary_dark = (55, 0, 179)  # #3700B3
    accent = (3, 218, 198)  # #03DAC6

    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)  # Filter type
        ratio = y / height

        # Interpolate color
        r = int(primary[0] * (1 - ratio) + primary_dark[0] * ratio)
        g = int(primary[1] * (1 - ratio) + primary_dark[1] * ratio)
        b_val = int(primary[2] * (1 - ratio) + primary_dark[2] * ratio)

        for x in range(width):
            # Add accent color blocks in corner
            if x > width * 0.75 and y > height * 0.75:
                raw_data.extend(accent)
            else:
                raw_data.extend([r, g, b_val])

    # Compress
    compressed_data = zlib.compress(bytes(raw_data), 9)
    idat_chunk = png_chunk(b'IDAT', compressed_data)

    # IEND chunk
    iend_chunk = png_chunk(b'IEND', b'')

    # Write PNG file
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'wb') as f:
        f.write(png_signature + ihdr_chunk + idat_chunk + iend_chunk)

    print(f"Created: {output_path} ({width}x{height})")

def main():
    base_path = "/home/user/Builder/app/src/main/res"

    # Icon sizes for different densities
    densities = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192
    }

    for density, size in densities.items():
        mipmap_dir = f"{base_path}/mipmap-{density}"

        # Create ic_launcher.png
        create_builder_icon(size, size, f"{mipmap_dir}/ic_launcher.png")

        # Create ic_launcher_round.png (same for now)
        create_builder_icon(size, size, f"{mipmap_dir}/ic_launcher_round.png")

    print("\n✅ All launcher icons created successfully!")
    print("   Sizes: mdpi(48), hdpi(72), xhdpi(96), xxhdpi(144), xxxhdpi(192)")

if __name__ == "__main__":
    main()
