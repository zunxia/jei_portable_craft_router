param([string]$TargetMods)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$source = Join-Path $root 'build\jei_portable_craft_router-1.3.2.jar'
if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw 'Build the mod before installing.' }
if (-not $TargetMods) {
    $instanceFile = Join-Path $root 'build\instance.txt'
    if (-not (Test-Path -LiteralPath $instanceFile)) { throw 'Pass -TargetMods or build with a local instance first.' }
    $instance = (Get-Content -LiteralPath $instanceFile -Raw -Encoding UTF8).Trim()
    $TargetMods = Join-Path $instance 'mods'
}
$target = (Resolve-Path -LiteralPath $TargetMods).Path.TrimEnd('\', '/')
if (-not (Test-Path -LiteralPath $target -PathType Container) -or (Split-Path $target -Leaf) -ne 'mods') {
    throw 'TargetMods must be an existing mods directory.'
}
$destination = Join-Path $target (Split-Path $source -Leaf)
$oldMods = @(Get-ChildItem -LiteralPath $target -Filter 'jei_portable_craft_router-*.jar' -File)
if ($oldMods.Count) {
    $backup = Join-Path $root ('build\install-backups\' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
    New-Item -ItemType Directory -Path $backup -Force | Out-Null
    foreach ($old in $oldMods) { Copy-Item -LiteralPath $old.FullName -Destination $backup }
    Write-Host "Previous router jars backed up to $backup"
}
Copy-Item -LiteralPath $source -Destination $destination -Force
$sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne $sourceHash) {
    throw 'Installed jar hash differs; previous versions are preserved in the backup.'
}
foreach ($old in $oldMods) {
    if ($old.FullName -ne $destination) { Remove-Item -LiteralPath $old.FullName }
}
Write-Host "Installed: $destination"
Write-Host "SHA256: $sourceHash"
Write-Host 'Restart the game/server to load the mod.'
