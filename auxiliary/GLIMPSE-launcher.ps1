# GLIMPSE-launcher.ps1
# PowerShell launcher for GLIMPSE-CE ScenarioBuilder (GCAM-USA 8.2 Windows)
#
# Run from the GLIMPSE-CE root directory:
#   powershell -ExecutionPolicy Bypass -File auxiliary\GLIMPSE-launcher.ps1
# Or, from within PowerShell already open in that directory:
#   .\auxiliary\GLIMPSE-launcher.ps1

# Resolve the GLIMPSE-CE root as the parent of this script's folder (auxiliary\)
$glimpseRoot = Split-Path -Parent $PSScriptRoot

# --- Java home -----------------------------------------------------------
$javaHome = Join-Path $glimpseRoot "amazon-corretto-8.442.06.1-windows-x64-jre"
$javaExe  = Join-Path $javaHome "bin\java.exe"

if (-not (Test-Path $javaExe)) {
    Write-Error "JAVA_HOME setting needs to be fixed.`nExpected java.exe at: $javaExe"
    Read-Host "Press Enter to exit"
    exit 1
}

# --- PATH additions -------------------------------------------------------
$jvmPath = Join-Path $javaHome "bin\server"
$env:PATH = ".$([IO.Path]::PathSeparator)$jvmPath$([IO.Path]::PathSeparator)$javaHome$([IO.Path]::PathSeparator)$(Join-Path $javaHome 'bin')$([IO.Path]::PathSeparator)$(Join-Path $glimpseRoot '..\..\ModelInterface')$([IO.Path]::PathSeparator)$env:PATH"

# --- Launch ---------------------------------------------------------------
$jar     = Join-Path $glimpseRoot "GLIMPSE-ScenarioBuilder\GLIMPSE-ScenarioBuilder.jar"
$options = Join-Path $glimpseRoot "options_GCAM-USA-8.2-windows.txt"

Write-Host "Launching GLIMPSE ScenarioBuilder..."
Write-Host "  Root   : $glimpseRoot"
Write-Host "  Java   : $javaExe"
Write-Host "  Jar    : $jar"
Write-Host "  Options: $options"

& $javaExe -Dprism.order=sw -jar $jar -options $options
