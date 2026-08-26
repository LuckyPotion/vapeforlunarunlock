#!/usr/bin/env python3
"""
Resource Hacker Method - Replace JAR using Windows Resource Tools

This uses ResourceHacker or similar tools to properly update PE resources.
For manual method, we'll provide instructions.
"""

import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent
DLL_PATH = Path(r"D:\windows-x64-injection-bundle\Vape421Native.dll")
NEW_JAR_PATH = PROJECT_ROOT / "build" / "libs" / "vape421-product-recovery-4.21-recovered-injection.jar"

def main():
    print("""
╔══════════════════════════════════════════════════════════════════════╗
║          Vape421 JAR Update Instructions                             ║
╚══════════════════════════════════════════════════════════════════════╝

The DLL contains an embedded JAR as an RCDATA resource.
Simple byte replacement breaks the PE structure.

╔══════════════════════════════════════════════════════════════════════╗
║  Method 1: Rebuild Native (Recommended)                              ║
╚══════════════════════════════════════════════════════════════════════╝

1. Install CMake: https://cmake.org/download/
   Add to PATH during installation

2. Install Visual Studio 2022:
   - Download "Build Tools for Visual Studio 2022"
   - Install "Desktop development with C++"

3. Set JDK path:
   set JAVA_HOME=C:\\Program Files\\Java\\jdk1.8.0_XXX

4. Build native bundle:
   .\\gradlew.bat prepareInjectionBundle -PtargetRelease=8 -PnativeJavaHome="%JAVA_HOME%"

5. Output will be in: build\\injection\\Vape421Native.dll

╔══════════════════════════════════════════════════════════════════════╗
║  Method 2: Use Resource Hacker                                       ║
╚══════════════════════════════════════════════════════════════════════╝

1. Download Resource Hacker:
   http://www.angusj.com/resourcehacker/

2. Open Vape421Native.dll in Resource Hacker

3. Navigate to: RCData -> 1 -> 0

4. Action -> Replace Resource

5. Select: {new_jar}

6. Save DLL

╔══════════════════════════════════════════════════════════════════════╗
║  Method 3: Automated Build Script (Creates working DLL)             ║
╚══════════════════════════════════════════════════════════════════════╝

I'll create a build script that properly embeds the JAR...
""".format(new_jar=NEW_JAR_PATH))

    # Create automated build script
    create_build_script()

def create_build_script():
    """Create a PowerShell script to automate the build"""
    script_content = r"""# Vape421 Quick Build Script
# This script rebuilds just the injection JAR and provides instructions

Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Vape421 Quick Build                                         ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build injection JAR
Write-Host "[1/3] Building injection JAR..." -ForegroundColor Yellow
.\gradlew.bat clean injectionJar

if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "[+] JAR built successfully" -ForegroundColor Green

# Step 2: Check if we can build native
$cmakeExists = Get-Command cmake -ErrorAction SilentlyContinue
$vsExists = Test-Path "C:\Program Files\Microsoft Visual Studio\2022\*\VC\Auxiliary\Build\vcvars64.bat"

Write-Host ""
Write-Host "[2/3] Checking native build tools..." -ForegroundColor Yellow

if ($cmakeExists -and $vsExists) {
    Write-Host "[+] CMake and Visual Studio found!" -ForegroundColor Green
    Write-Host "[*] Attempting native build..." -ForegroundColor Yellow

    # Find JDK
    $jdkPaths = @(
        "C:\Program Files\Java\jdk1.8.*",
        "C:\Program Files\Java\jdk-8*",
        "C:\Program Files\Eclipse Adoptium\jdk-8*"
    )

    $jdkPath = $null
    foreach ($path in $jdkPaths) {
        $found = Get-Item $path -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $jdkPath = $found.FullName
            break
        }
    }

    if ($jdkPath) {
        Write-Host "[+] Found JDK: $jdkPath" -ForegroundColor Green
        Write-Host "[*] Building native bundle..." -ForegroundColor Yellow

        .\gradlew.bat prepareInjectionBundle -PtargetRelease=8 "-PnativeJavaHome=$jdkPath"

        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Green
            Write-Host "║  BUILD SUCCESSFUL!                                           ║" -ForegroundColor Green
            Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Green
            Write-Host ""
            Write-Host "Output: build\injection\Vape421Native.dll" -ForegroundColor Cyan
            Write-Host ""
            Write-Host "Copy to injection bundle:" -ForegroundColor Yellow
            Write-Host "  copy build\injection\Vape421Native.dll D:\windows-x64-injection-bundle\" -ForegroundColor White
            exit 0
        }
    } else {
        Write-Host "[!] JDK 8 not found" -ForegroundColor Red
    }
}

# Native build not possible, show manual instructions
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Yellow
Write-Host "║  Native build tools not available                            ║" -ForegroundColor Yellow
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Yellow
Write-Host ""
Write-Host "[3/3] Manual update required:" -ForegroundColor Yellow
Write-Host ""
Write-Host "New JAR location:" -ForegroundColor Cyan
Write-Host "  build\libs\vape421-product-recovery-4.21-recovered-injection.jar" -ForegroundColor White
Write-Host ""
Write-Host "Option A: Use Resource Hacker (Recommended)" -ForegroundColor Cyan
Write-Host "  1. Download: http://www.angusj.com/resourcehacker/" -ForegroundColor White
Write-Host "  2. Open Vape421Native.dll" -ForegroundColor White
Write-Host "  3. RCData -> 1 -> 0" -ForegroundColor White
Write-Host "  4. Action -> Replace Resource -> Select new JAR" -ForegroundColor White
Write-Host "  5. Save" -ForegroundColor White
Write-Host ""
Write-Host "Option B: Install build tools (for future)" -ForegroundColor Cyan
Write-Host "  1. CMake: https://cmake.org/download/" -ForegroundColor White
Write-Host "  2. Visual Studio 2022 Build Tools" -ForegroundColor White
Write-Host "  3. JDK 8" -ForegroundColor White
Write-Host ""
"""

    script_path = PROJECT_ROOT / "quick_build.ps1"
    with open(script_path, 'w', encoding='utf-8') as f:
        f.write(script_content)

    print(f"\n[+] Created build script: {script_path}")
    print(f"\nRun it with: .\\quick_build.ps1\n")

if __name__ == '__main__':
    main()
