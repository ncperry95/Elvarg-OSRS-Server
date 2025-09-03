# scripts/fix-client.ps1
# Cleans Elvarg CLIENT sources that accidentally include server-side package/import lines.
# - Keeps ONLY the first 'package ...;' line in each file
# - Deletes any 'package com.elvarg...' (server) lines
# - Deletes any 'import com.elvarg....' lines
# - Normalizes line endings and trims weird whitespace

param(
  [string]$ClientDir = "Elvarg - Client",
  [string]$SrcRel = "src"
)

$clientPath = Join-Path -Path (Get-Location) -ChildPath $ClientDir
$srcPath    = Join-Path $clientPath $SrcRel

if (-not (Test-Path $srcPath)) {
  Write-Error "Source directory not found: $srcPath"
  exit 1
}

# Backup once
$backupDir = Join-Path $clientPath "backup-before-fix"
if (-not (Test-Path $backupDir)) {
  New-Item -ItemType Directory -Path $backupDir | Out-Null
  Copy-Item -Recurse -Force $srcPath $backupDir
  Write-Host "Backup created at $backupDir"
}

# Helper to keep only first package line and remove any server refs
Get-ChildItem -Recurse -Filter *.java -Path $srcPath | ForEach-Object {
  $file = $_.FullName
  $content = Get-Content $file -Raw

  # Normalize CRLF
  $content = $content -replace "`r`n","`n"
  $lines   = $content -split "`n"

  $out = New-Object System.Collections.Generic.List[string]
  $seenPackage = $false

  foreach ($line in $lines) {
    $l = $line

    # Remove any server package/import lines entirely
    if ($l -match '^\s*package\s+com\.elvarg\.') { continue }
    if ($l -match '^\s*import\s+com\.elvarg\.')  { continue }

    # Keep only the first package statement of ANY kind
    if ($l -match '^\s*package\s+') {
      if ($seenPackage) {
        # drop subsequent package lines
        continue
      } else {
        $seenPackage = $true
        $out.Add($l)
        continue
      }
    }

    $out.Add($l)
  }

  # Write back (ASCII is fine for RS clients)
  [System.IO.File]::WriteAllText($file, ($out -join "`r`n"))
}

# Show any remaining references to server packages/imports
$leftovers = Select-String -Path (Join-Path $srcPath "**\*.java") -Pattern "com\.elvarg\." -CaseSensitive -SimpleMatch | Select-Object -ExpandProperty Path -Unique
if ($leftovers) {
  Write-Warning "Some files still reference com.elvarg types in code (not just imports):"
  $leftovers | ForEach-Object { Write-Host " - $_" }
  Write-Warning "You may need to manually swap those types to their client equivalents or remove those blocks."
} else {
  Write-Host "No remaining com.elvarg references found in imports/packages."
}

Write-Host "Client source cleanup complete."
