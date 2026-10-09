param([string]$InstanceDir, [string]$NeoForgeVersion = '21.1.249', [switch]$PrepareOnly)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($PSScriptRoot)
$build = [IO.Path]::GetFullPath((Join-Path $root 'build'))
if ($build -ne ($root + '\build')) { throw 'Invalid build directory' }
if (-not $InstanceDir) {
    $hit = Get-ChildItem 'E:\BaiduNetdiskDownload' -Directory -Recurse -Depth 4 -ErrorAction SilentlyContinue |
        Where-Object Name -eq 'All the Mods 10 - 8.2' | Select-Object -First 1
    if (-not $hit) { throw 'Pass -InstanceDir pointing to ATM10 8.2' }
    $InstanceDir = $hit.FullName
}
$InstanceDir = (Resolve-Path -LiteralPath $InstanceDir).Path
$mc = Split-Path (Split-Path $InstanceDir -Parent) -Parent
$libraries = Join-Path $mc 'libraries'
$jsonFile = Join-Path $InstanceDir ((Split-Path $InstanceDir -Leaf) + '.json')
$json = Get-Content -Raw -Encoding UTF8 -LiteralPath $jsonFile | ConvertFrom-Json
$cp = [Collections.Generic.List[string]]::new()
$missing = 0
foreach ($lib in $json.libraries) {
    $p = $lib.name -split ':'
    if ($p.Count -lt 3 -or ($p[0] -eq 'net.neoforged' -and $p[1] -eq 'neoforge')) { continue }
    $suffix = if ($p.Count -gt 3) { '-' + $p[3] } else { '' }
    $path = Join-Path $libraries (($p[0] -replace '\.', '\') + '\' + $p[1] + '\' + $p[2] + '\' + $p[1] + '-' + $p[2] + $suffix + '.jar')
    if (Test-Path -LiteralPath $path) { $cp.Add($path) } else { $missing++ }
}
foreach ($path in @(
    (Join-Path $libraries 'net\minecraft\client\1.21.1-20240808.144430\client-1.21.1-20240808.144430-srg.jar'),
    (Join-Path $libraries "net\neoforged\neoforge\$NeoForgeVersion\neoforge-$NeoForgeVersion-client.jar"),
    (Join-Path $libraries "net\neoforged\neoforge\$NeoForgeVersion\neoforge-$NeoForgeVersion-universal.jar"),
    (Join-Path $libraries 'org\spongepowered\mixin\0.8.5\mixin-0.8.5.jar')
)) {
    if (-not (Test-Path -LiteralPath $path)) { throw "Required library missing: $path" }
    $cp.Add($path)
}
$mods = Get-ChildItem -LiteralPath (Join-Path $InstanceDir 'mods') -Filter '*.jar' |
    Where-Object Name -match '^(jei-|appliedenergistics|ae2wtlib|ae2jei|crafting_on|curios|cyclopscore|integrateddynamics|integratedtunnels|integratedcrafting|commoncapabilities)|integratedterminals-'
foreach ($mod in $mods) { $cp.Add($mod.FullName) }
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($mod in $mods | Where-Object { $_.Name -like 'ae2wtlib-*.jar' }) {
    $zip = [IO.Compression.ZipFile]::OpenRead($mod.FullName)
    try {
        $entry = $zip.Entries | Where-Object { $_.FullName -match 'ae2wtlib_api.*\.jar$' } | Select-Object -First 1
        if ($entry) {
            New-Item -ItemType Directory -Force -Path $build | Out-Null
            $apiJar = "$build\ae2wtlib-api.jar"
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $apiJar, $true)
            $cp.Add($apiJar)
        }
    } finally { $zip.Dispose() }
}
$ordered = @($cp | Select-Object -Unique | Sort-Object { if ($_ -like "*neoforge-$NeoForgeVersion-client.jar") { 0 } elseif ($_ -like "*neoforge-$NeoForgeVersion-universal.jar") { 1 } elseif ($_ -like '*-srg.jar') { 2 } else { 3 } })
New-Item -ItemType Directory -Force -Path "$build\cp", "$build\classes" | Out-Null
$stage = [Collections.Generic.List[string]]::new()
$index = 0
foreach ($path in $ordered) {
    $target = Join-Path "$build\cp" ('j{0:d3}.jar' -f ++$index)
    Copy-Item -LiteralPath $path -Destination $target -Force
    $stage.Add($target)
}
[IO.File]::WriteAllText("$build\classpath.txt", ($stage -join ';'))
[IO.File]::WriteAllText("$build\instance.txt", $InstanceDir)
Write-Host "Prepared $index jars; skipped $missing missing library variants; NeoForge $NeoForgeVersion"
if ($PrepareOnly) { return }
# Only remove this project's verified compilation directory; retain staged offline dependencies.
Remove-Item -LiteralPath "$build\classes" -Recurse -Force
New-Item -ItemType Directory -Path "$build\classes" | Out-Null
$sources = @(Get-ChildItem "$root\src\main\java" -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac --release 21 -encoding UTF-8 -proc:none -cp ($stage -join ';') -d "$build\classes" @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed: $LASTEXITCODE" }
Get-ChildItem "$root\src\main\resources" -Force | Copy-Item -Destination "$build\classes" -Recurse -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$dist = "$build\jei_portable_craft_router-1.3.2.jar"
if (Test-Path -LiteralPath $dist) { Remove-Item -LiteralPath $dist -Force }
& "$root\scripts\package.ps1" -Classes "$build\classes" -OutputJar $dist
Write-Host "OK: $dist ($((Get-Item -LiteralPath $dist).Length) bytes), $($sources.Count) Java files"
