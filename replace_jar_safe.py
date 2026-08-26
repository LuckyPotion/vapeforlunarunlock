#!/usr/bin/env python3
"""
Safe DLL JAR Replacer - Uses PE resource section editing

This version properly handles PE resource sections to avoid corrupting the DLL.
"""

import struct
import os
import sys
import shutil
from pathlib import Path
import pefile

# Paths
PROJECT_ROOT = Path(__file__).parent
DLL_PATH = Path(r"D:\windows-x64-injection-bundle\Vape421Native.dll.backup")
NEW_JAR_PATH = PROJECT_ROOT / "build" / "libs" / "vape421-product-recovery-4.21-recovered-injection.jar"
OUTPUT_DLL_PATH = Path(r"D:\windows-x64-injection-bundle\Vape421Native.dll")

def replace_jar_safe(dll_path, new_jar_path, output_path):
    """Replace JAR using pefile library for safe PE manipulation"""
    print(f"\n{'='*60}")
    print("Safe DLL JAR Replacer (PE Resource Editor)")
    print(f"{'='*60}\n")

    try:
        import pefile
    except ImportError:
        print("[!] pefile library not found")
        print("[*] Installing pefile...")
        import subprocess
        subprocess.check_call([sys.executable, "-m", "pip", "install", "pefile"])
        import pefile

    # Check files
    if not dll_path.exists():
        print(f"[!] ERROR: DLL not found at: {dll_path}")
        print("[*] Looking for backup...")
        dll_path = dll_path.with_suffix('.dll.backup')
        if not dll_path.exists():
            print(f"[!] ERROR: Backup not found either")
            return False

    if not new_jar_path.exists():
        print(f"[!] ERROR: New JAR not found at: {new_jar_path}")
        return False

    # Read new JAR
    print(f"[*] Reading new JAR: {new_jar_path}")
    with open(new_jar_path, 'rb') as f:
        new_jar_data = f.read()
    print(f"[+] New JAR size: {len(new_jar_data):,} bytes")

    # Load PE
    print(f"[*] Loading PE file: {dll_path}")
    pe = pefile.PE(str(dll_path))

    # Find RCDATA resource
    print("[*] Searching for RCDATA resources...")

    # Resource types
    RT_RCDATA = 10

    if not hasattr(pe, 'DIRECTORY_ENTRY_RESOURCE'):
        print("[!] ERROR: No resource directory found in DLL")
        return False

    # Navigate resource tree
    found_resource = False
    for resource_type in pe.DIRECTORY_ENTRY_RESOURCE.entries:
        if resource_type.id == RT_RCDATA:
            print("[+] Found RCDATA resource type")

            for resource_id in resource_type.directory.entries:
                print(f"[*] Checking RCDATA resource ID: {resource_id.id}")

                for resource_lang in resource_id.directory.entries:
                    # Get resource data
                    data_rva = resource_lang.data.struct.OffsetToData
                    size = resource_lang.data.struct.Size
                    offset = pe.get_offset_from_rva(data_rva)

                    # Check if it's a JAR (starts with PK)
                    pe_data = pe.get_data(data_rva, 4)
                    if pe_data[:4] == b'PK\x03\x04':
                        print(f"[+] Found JAR resource at RVA: 0x{data_rva:X}, Size: {size:,} bytes")
                        found_resource = True

                        # Replace the resource data
                        print("[*] Replacing resource data...")

                        # Calculate new size
                        new_size = len(new_jar_data)
                        size_diff = new_size - size

                        print(f"[*] Old size: {size:,} bytes")
                        print(f"[*] New size: {new_size:,} bytes")
                        print(f"[*] Difference: {size_diff:+,} bytes")

                        # Set new data
                        pe.set_bytes_at_offset(offset, new_jar_data)

                        # Update resource directory size
                        resource_lang.data.struct.Size = new_size

                        print("[+] Resource data replaced")
                        break

                if found_resource:
                    break

        if found_resource:
            break

    if not found_resource:
        print("[!] ERROR: Could not find JAR resource in DLL")
        return False

    # Write modified PE
    print(f"[*] Writing modified DLL to: {output_path}")
    pe.write(str(output_path))

    print(f"\n[+] SUCCESS! DLL updated with new JAR")
    return True

def simple_replace(dll_path, new_jar_path, output_path):
    """Simple byte replacement method (original)"""
    print(f"\n{'='*60}")
    print("Simple Byte Replacement Method")
    print(f"{'='*60}\n")

    # Read files
    with open(dll_path, 'rb') as f:
        dll_data = bytearray(f.read())

    with open(new_jar_path, 'rb') as f:
        new_jar = f.read()

    # Find JAR
    jar_sig = b'PK\x03\x04'
    jar_start = dll_data.find(jar_sig)

    if jar_start == -1:
        print("[!] ERROR: JAR not found in DLL")
        return False

    print(f"[+] Found JAR at offset: 0x{jar_start:X}")

    # Find JAR end
    jar_end_sig = b'PK\x05\x06'
    jar_end = dll_data.find(jar_end_sig, jar_start)

    if jar_end != -1:
        # Read end of central directory
        if jar_end + 22 <= len(dll_data):
            comment_len = struct.unpack('<H', dll_data[jar_end + 20:jar_end + 22])[0]
            jar_end = jar_end + 22 + comment_len
    else:
        jar_end = len(dll_data)

    old_size = jar_end - jar_start
    new_size = len(new_jar)

    print(f"[*] Old JAR size: {old_size:,} bytes")
    print(f"[*] New JAR size: {new_size:,} bytes")
    print(f"[*] Size diff: {new_size - old_size:+,} bytes")

    # Replace
    new_dll = dll_data[:jar_start] + new_jar + dll_data[jar_end:]

    # Write
    with open(output_path, 'wb') as f:
        f.write(new_dll)

    print(f"[+] DLL written to: {output_path}")
    return True

def main():
    print("Vape421 DLL JAR Replacer\n")

    # Try pefile method first
    print("[*] Attempting safe PE resource replacement...")
    try:
        success = replace_jar_safe(DLL_PATH, NEW_JAR_PATH, OUTPUT_DLL_PATH)
        if success:
            print("\n[+] Replacement successful using PE method")
            return 0
    except Exception as e:
        print(f"[!] PE method failed: {e}")
        print("[*] Falling back to simple replacement...")

    # Fallback to simple method
    try:
        # Use backup if available
        dll_source = DLL_PATH
        backup = OUTPUT_DLL_PATH.with_suffix('.dll.backup')
        if backup.exists():
            dll_source = backup
            print(f"[*] Using backup: {backup}")

        success = simple_replace(dll_source, NEW_JAR_PATH, OUTPUT_DLL_PATH)
        if success:
            print("\n[+] Replacement successful using simple method")
            return 0
        else:
            return 1
    except Exception as e:
        print(f"[!] ERROR: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == '__main__':
    sys.exit(main())
