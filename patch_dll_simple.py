#!/usr/bin/env python3
"""
Simplified DLL JAR Patcher for Vape421Native.dll

Based on the official patch_dll_product_jar.py script.
This version skips obfuscated name patching since we're using the same obfuscation.

Usage:
    python patch_dll_simple.py

Requires:
    pip install pefile
"""

import shutil
import struct
import sys
import zipfile
from pathlib import Path

try:
    import pefile
except ImportError:
    print("[!] Error: pefile module not found")
    print("[*] Installing pefile...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pefile"])
    import pefile

# Configuration
DLL_PATH = Path(r"D:\windows-x64-injection-bundle\Vape421Native.dll")
JAR_PATH = Path(r"D:\VapeV4.21-main\build\libs\vape421-product-recovery-4.21-recovered-injection.jar")
BACKUP_SUFFIX = ".bak-original"
RCDATA_ID = 421  # Resource ID for the embedded JAR

def compress_jar(src: Path, dst: Path) -> bytes:
    """Compress JAR with maximum compression"""
    print(f"[*] Compressing JAR: {src.name}")
    with zipfile.ZipFile(src, "r") as zin:
        with zipfile.ZipFile(dst, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zout:
            for info in zin.infolist():
                data = zin.read(info.filename)
                zi = zipfile.ZipInfo(filename=info.filename, date_time=info.date_time)
                zi.compress_type = zipfile.ZIP_DEFLATED
                zi.external_attr = info.external_attr
                zout.writestr(zi, data, compresslevel=9)

    compressed = dst.read_bytes()
    print(f"[+] Compressed: {len(compressed):,} bytes ({len(compressed) / 1024 / 1024:.2f} MB)")
    return compressed

def find_rcdata(pe: pefile.PE, resource_id: int = RCDATA_ID) -> tuple[int, int, int]:
    """
    Find RCDATA resource by ID.
    Returns: (data_file_offset, capacity_size, size_field_file_offset)
    """
    print(f"[*] Searching for RCDATA resource ID {resource_id}...")

    RT_RCDATA = 10

    for entry in pe.DIRECTORY_ENTRY_RESOURCE.entries:
        if entry.id != RT_RCDATA:
            continue

        for e2 in entry.directory.entries:
            if e2.id != resource_id:
                continue

            for e3 in e2.directory.entries:
                rva = e3.data.struct.OffsetToData
                size = e3.data.struct.Size
                data_off = pe.get_offset_from_rva(rva)

                # Get file offset of the Size field (4 bytes after struct start)
                size_field_off = e3.data.struct.get_file_offset() + 4

                print(f"[+] Found RCDATA at offset: 0x{data_off:X}")
                print(f"[+] Current capacity: {size:,} bytes ({size / 1024 / 1024:.2f} MB)")
                print(f"[+] Size field offset: 0x{size_field_off:X}")

                return data_off, size, size_field_off

    raise SystemExit(f"[!] ERROR: RCDATA resource ID {resource_id} not found in DLL")

def patch_dll(dll_path: Path, jar_path: Path, backup_suffix: str = BACKUP_SUFFIX) -> bool:
    """Patch the DLL with new JAR"""

    print(f"\n{'='*70}")
    print("Vape421Native.dll JAR Patcher (Official Method)")
    print(f"{'='*70}\n")

    # Validate inputs
    if not dll_path.is_file():
        print(f"[!] ERROR: DLL not found: {dll_path}")
        return False

    if not jar_path.is_file():
        print(f"[!] ERROR: JAR not found: {jar_path}")
        print("[*] Run: .\\gradlew.bat build")
        return False

    # Create backup
    backup_path = dll_path.with_name(dll_path.name + backup_suffix)
    if not backup_path.is_file():
        print(f"[*] Creating backup: {backup_path.name}")
        shutil.copy2(dll_path, backup_path)
        print(f"[+] Backup created")
    else:
        print(f"[*] Using existing backup: {backup_path.name}")

    # Compress JAR
    compressed_jar = jar_path.with_name(jar_path.stem + "-maxcomp.jar")
    jar_bytes = compress_jar(jar_path, compressed_jar)

    # Read DLL from backup (idempotent)
    print(f"\n[*] Loading DLL from backup...")
    data = bytearray(backup_path.read_bytes())
    print(f"[+] DLL size: {len(data):,} bytes ({len(data) / 1024 / 1024:.2f} MB)")

    # Parse PE structure
    print(f"[*] Parsing PE structure...")
    pe = pefile.PE(data=bytes(data))

    # Find RCDATA resource
    rc_offset, rc_capacity, size_field_offset = find_rcdata(pe)

    # Check if compressed JAR fits
    if len(jar_bytes) > rc_capacity:
        print(f"\n[!] ERROR: Compressed JAR ({len(jar_bytes):,} bytes) is LARGER than RCDATA capacity ({rc_capacity:,} bytes)")
        print(f"[!] Overflow: {len(jar_bytes) - rc_capacity:,} bytes")
        print(f"[!] Cannot patch without rebuilding native DLL with larger capacity")
        return False

    padding = rc_capacity - len(jar_bytes)
    print(f"\n[*] JAR fits! Padding needed: {padding:,} bytes")

    # Replace JAR data
    print(f"[*] Replacing RCDATA with new JAR...")
    data[rc_offset : rc_offset + len(jar_bytes)] = jar_bytes

    # Zero-pad the rest
    if padding > 0:
        print(f"[*] Zero-padding remaining space...")
        data[rc_offset + len(jar_bytes) : rc_offset + rc_capacity] = b"\x00" * padding

    # Update Size field (CRITICAL: must match actual JAR size, not capacity)
    print(f"[*] Updating resource Size field to {len(jar_bytes):,} bytes...")
    struct.pack_into("<I", data, size_field_offset, len(jar_bytes))

    # Write patched DLL
    print(f"[*] Writing patched DLL to: {dll_path}")
    dll_path.write_bytes(data)

    print(f"\n[+] SUCCESS! DLL patched successfully")
    print(f"[+] JAR size: {len(jar_bytes):,} bytes")
    print(f"[+] Padding: {padding:,} bytes")

    # Verify ZIP is readable
    print(f"\n[*] Verifying patched DLL...")
    try:
        pe2 = pefile.PE(str(dll_path))
        for entry in pe2.DIRECTORY_ENTRY_RESOURCE.entries:
            if entry.id != 10:  # RT_RCDATA
                continue
            for e2 in entry.directory.entries:
                if e2.id != RCDATA_ID:
                    continue
                for e3 in e2.directory.entries:
                    size = e3.data.struct.Size
                    off = pe2.get_offset_from_rva(e3.data.struct.OffsetToData)
                    blob = dll_path.read_bytes()[off : off + size]

                    # Try to open as ZIP
                    import io
                    with zipfile.ZipFile(io.BytesIO(blob)) as zf:
                        entries = len(zf.namelist())
                        print(f"[+] Verification PASSED: {entries} entries in JAR")

                        # Check for LunarUnlocker
                        lunar_unlocker = "gg/vape/module/utility/LunarUnlocker.class"
                        if lunar_unlocker in zf.namelist():
                            print(f"[+] LunarUnlocker module found in JAR")
                        else:
                            print(f"[!] WARNING: LunarUnlocker not found in JAR")

                        return True

        print(f"[!] WARNING: Could not verify RCDATA")
        return True

    except Exception as e:
        print(f"[!] ERROR during verification: {e}")
        return False

def main():
    try:
        success = patch_dll(DLL_PATH, JAR_PATH)

        if success:
            print(f"\n{'='*70}")
            print("Next Steps:")
            print("1. Launch Lunar Client 1.8.9")
            print("2. Join a world")
            print("3. Inject: .\\Vape421Injector.exe <pid> .\\Vape421Native.dll")
            print("4. Open Vape GUI")
            print("5. Go to Utility category")
            print("6. Click LunarUnlocker")
            print("7. Check vape421-native.log for debug output")
            print(f"{'='*70}\n")
            return 0
        else:
            return 1

    except Exception as e:
        print(f"\n[!] FATAL ERROR: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    sys.exit(main())
