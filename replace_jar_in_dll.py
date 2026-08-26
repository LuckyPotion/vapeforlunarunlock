#!/usr/bin/env python3
"""
DLL JAR Replacer - Replace embedded JAR in Vape421Native.dll

This script finds and replaces the embedded JAR resource (RCDATA)
in the native DLL without needing to rebuild the entire project.

Usage:
    python replace_jar_in_dll.py

The script will:
1. Read the new injection JAR from build/libs/
2. Find the RCDATA resource in Vape421Native.dll
3. Replace the embedded JAR
4. Save to the injection bundle directory
"""

import struct
import os
import sys
import shutil
from pathlib import Path

# Paths
PROJECT_ROOT = Path(__file__).parent
DLL_PATH = Path(r"D:\windows-x64-injection-bundle\Vape421Native.dll")
NEW_JAR_PATH = PROJECT_ROOT / "build" / "libs" / "vape421-product-recovery-4.21-recovered-injection.jar"
OUTPUT_DLL_PATH = Path(r"D:\windows-x64-injection-bundle\Vape421Native.dll")

# JAR magic bytes (PK signature)
JAR_MAGIC = b'PK\x03\x04'

def find_jar_in_dll(dll_data):
    """Find the embedded JAR in DLL by searching for PK magic bytes"""
    print("[*] Searching for embedded JAR in DLL...")

    # Search for JAR signature
    jar_start = dll_data.find(JAR_MAGIC)
    if jar_start == -1:
        print("[!] ERROR: Could not find JAR signature in DLL")
        return None, None

    print(f"[+] Found JAR signature at offset: 0x{jar_start:X}")

    # Find the end of JAR (look for end of central directory signature)
    # ZIP/JAR end signature: PK\x05\x06
    jar_end_sig = b'PK\x05\x06'
    jar_end = dll_data.find(jar_end_sig, jar_start)

    if jar_end == -1:
        print("[!] WARNING: Could not find JAR end signature, using heuristic...")
        # Fallback: assume JAR is at the end of file or before next section
        jar_end = len(dll_data)
    else:
        # Read the end of central directory record to get exact size
        # Format: signature(4) + disk_number(2) + start_disk(2) + entries_disk(2) +
        #         total_entries(2) + central_dir_size(4) + central_dir_offset(4) + comment_length(2)
        if jar_end + 22 <= len(dll_data):
            comment_length = struct.unpack('<H', dll_data[jar_end + 20:jar_end + 22])[0]
            jar_end = jar_end + 22 + comment_length
            print(f"[+] Found JAR end at offset: 0x{jar_end:X}")

    jar_size = jar_end - jar_start
    print(f"[+] Embedded JAR size: {jar_size:,} bytes ({jar_size / 1024 / 1024:.2f} MB)")

    return jar_start, jar_end

def replace_jar_in_dll(dll_path, new_jar_path, output_path):
    """Replace the embedded JAR in DLL"""
    print(f"\n{'='*60}")
    print("DLL JAR Replacer for Vape421Native.dll")
    print(f"{'='*60}\n")

    # Check if files exist
    if not dll_path.exists():
        print(f"[!] ERROR: DLL not found at: {dll_path}")
        return False

    if not new_jar_path.exists():
        print(f"[!] ERROR: New JAR not found at: {new_jar_path}")
        print("[*] Please run: .\\gradlew.bat build")
        return False

    # Read the DLL
    print(f"[*] Reading DLL from: {dll_path}")
    with open(dll_path, 'rb') as f:
        dll_data = bytearray(f.read())

    dll_size = len(dll_data)
    print(f"[+] DLL size: {dll_size:,} bytes ({dll_size / 1024 / 1024:.2f} MB)")

    # Find embedded JAR
    jar_start, jar_end = find_jar_in_dll(dll_data)
    if jar_start is None:
        return False

    old_jar_size = jar_end - jar_start

    # Read the new JAR
    print(f"\n[*] Reading new JAR from: {new_jar_path}")
    with open(new_jar_path, 'rb') as f:
        new_jar_data = f.read()

    new_jar_size = len(new_jar_data)
    print(f"[+] New JAR size: {new_jar_size:,} bytes ({new_jar_size / 1024 / 1024:.2f} MB)")

    # Calculate size difference
    size_diff = new_jar_size - old_jar_size
    print(f"[*] Size difference: {size_diff:+,} bytes")

    if size_diff > 0:
        print(f"[*] New JAR is {size_diff:,} bytes LARGER")
    elif size_diff < 0:
        print(f"[*] New JAR is {-size_diff:,} bytes SMALLER")
    else:
        print("[*] JAR sizes are identical")

    # Replace the JAR
    print("\n[*] Replacing embedded JAR...")
    new_dll_data = dll_data[:jar_start] + new_jar_data + dll_data[jar_end:]

    new_dll_size = len(new_dll_data)
    print(f"[+] New DLL size: {new_dll_size:,} bytes ({new_dll_size / 1024 / 1024:.2f} MB)")

    # Backup original DLL
    backup_path = output_path.with_suffix('.dll.backup')
    if output_path.exists() and not backup_path.exists():
        print(f"[*] Creating backup: {backup_path}")
        shutil.copy2(output_path, backup_path)

    # Write the new DLL
    print(f"[*] Writing modified DLL to: {output_path}")
    with open(output_path, 'wb') as f:
        f.write(new_dll_data)

    print(f"\n[+] SUCCESS! DLL updated with new JAR payload")
    print(f"[+] Modified DLL: {output_path}")

    if backup_path.exists():
        print(f"[+] Backup saved: {backup_path}")

    return True

def verify_jar_signature(jar_data):
    """Verify that data starts with JAR signature"""
    return jar_data[:4] == JAR_MAGIC

def main():
    try:
        # Verify new JAR is valid
        if NEW_JAR_PATH.exists():
            with open(NEW_JAR_PATH, 'rb') as f:
                jar_header = f.read(4)
            if not verify_jar_signature(jar_header):
                print(f"[!] ERROR: {NEW_JAR_PATH} is not a valid JAR file")
                return 1

        # Perform replacement
        success = replace_jar_in_dll(DLL_PATH, NEW_JAR_PATH, OUTPUT_DLL_PATH)

        if success:
            print(f"\n{'='*60}")
            print("Next steps:")
            print("1. Launch Lunar Client 1.8.9")
            print("2. Join a world")
            print("3. Run: .\\Vape421Injector.exe <pid> .\\Vape421Native.dll")
            print("4. Open Vape GUI and test LunarUnlocker")
            print(f"{'='*60}\n")
            return 0
        else:
            return 1

    except Exception as e:
        print(f"\n[!] ERROR: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == '__main__':
    sys.exit(main())
