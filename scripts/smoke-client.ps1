param([switch]$WithProbe, [int]$StartScenario = 0, [int]$EndScenario = 54, [string]$Session = 'smoke-client', [string]$Remote = '', [string]$PlayerName = 'RouterSmoke', [switch]$UnsupportedCheck, [switch]$Ae2Curios, [switch]$EmptyMaterials, [string]$RouterJar = '', [string]$JeiJar = '', [string]$Language = 'en_us', [switch]$Pattern, [string]$NeoForgeVersion = '21.1.249', [string]$InstanceDir = '', [switch]$PrepareOnly)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$instance = if ($InstanceDir) { $InstanceDir } else { (Get-Content -Raw -Encoding UTF8 "$root\build\instance.txt").Trim() }
$instance = (Resolve-Path -LiteralPath $instance).Path
if ($JeiJar) { $JeiJar = (Resolve-Path -LiteralPath $JeiJar).Path }
$instanceName = Split-Path $instance -Leaf
$mc = Split-Path (Split-Path $instance -Parent) -Parent
$libraries = Join-Path $mc 'libraries'
foreach ($suffix in @('client', 'universal')) {
    $library = Join-Path $libraries "net\neoforged\neoforge\$NeoForgeVersion\neoforge-$NeoForgeVersion-$suffix.jar"
    if (-not (Test-Path -LiteralPath $library)) { throw "Required library missing: $library" }
}
$json = Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $instance ($instanceName + '.json')) | ConvertFrom-Json
if ($Session -notmatch '^[a-z0-9-]+$') { throw 'Invalid session name' }
$smoke = Join-Path $root ('build\' + $Session)
New-Item -ItemType Directory -Force -Path "$smoke\mods", "$smoke\config" | Out-Null
foreach ($link in @(
    @('libraries', $libraries), @('assets', "$mc\assets"),
    @('natives', (Join-Path $instance ($instanceName + '-natives')))
)) {
    $linkPath = Join-Path $smoke $link[0]
    if (-not (Test-Path -LiteralPath $linkPath)) { New-Item -ItemType Junction -Path $linkPath -Target $link[1] | Out-Null }
}
# Remove only the dependency and probe jars managed by this isolated launcher.
foreach ($old in @(Get-ChildItem -LiteralPath "$smoke\mods" -Filter '*.jar' -File)) {
    if ($old.Name -match '^(jei-|appliedenergistics|ae2wtlib|ae2jei|crafting_on|curios|cyclopscore|integrateddynamics|integratedtunnels|integratedcrafting|commoncapabilities|guideme|router_probe)|integratedterminals-') {
        Remove-Item -LiteralPath $old.FullName
    }
}
# 使用独立目录与离线测试身份，完全不读取玩家令牌，不写入用户存档。
$testMods = @(Get-ChildItem -LiteralPath "$instance\mods" -Filter '*.jar' |
    Where-Object { $_.Name -match '^(jei-|appliedenergistics|ae2wtlib|ae2jei|crafting_on|curios|cyclopscore|integrateddynamics|integratedtunnels|integratedcrafting|commoncapabilities|guideme)|integratedterminals-' })
foreach ($mod in $testMods) { Copy-Item -LiteralPath $mod.FullName -Destination "$smoke\mods" -Force }
if ($JeiJar) {
    Get-ChildItem -LiteralPath "$smoke\mods" -Filter 'jei-*.jar' | Remove-Item -Force
    Copy-Item -LiteralPath $JeiJar -Destination "$smoke\mods" -Force
}
Write-Host "Copied $($testMods.Count) dependency mods"
Get-ChildItem -LiteralPath "$smoke\mods" -Filter "jei_portable_craft_router-*.jar" | Remove-Item -Force
if (-not $RouterJar) { $RouterJar = "$root\build\jei_portable_craft_router-1.3.5.jar" }
Copy-Item -LiteralPath $RouterJar -Destination "$smoke\mods\jei_portable_craft_router-test.jar" -Force
if ($WithProbe) { Copy-Item "$root\build\router_probe.jar" "$smoke\mods" -Force }
$cp = [Collections.Generic.List[string]]::new()
foreach ($lib in $json.libraries) {
    $p = $lib.name -split ':'
    if ($p.Count -lt 3) { continue }
    $suffix = if ($p.Count -gt 3) { '-' + $p[3] } else { '' }
    $path = Join-Path $libraries (($p[0] -replace '\.', '\') + '\' + $p[1] + '\' + $p[2] + '\' + $p[1] + '-' + $p[2] + $suffix + '.jar')
    if (Test-Path -LiteralPath $path) { $cp.Add($path.Replace($libraries, "$smoke\libraries")) }
}
Copy-Item -LiteralPath (Join-Path $instance ($instanceName + '.jar')) -Destination "$smoke\vanilla.jar" -Force
$cp.Add("$smoke\vanilla.jar")
$values = @{
    natives_directory = "$smoke\natives"
    launcher_name = 'RouterSmoke'; launcher_version = '1'; classpath = (($cp | Select-Object -Unique) -join ';')
    version_name = 'vanilla'; library_directory = "$smoke\libraries"; classpath_separator = ';'
    auth_player_name = $PlayerName; game_directory = $smoke; assets_root = "$smoke\assets"
    assets_index_name = $json.assetIndex.id; auth_uuid = 'b0b0b0b0b0b030008000b0b0b0b0b0b0'
    auth_access_token = '0'; clientid = '0'; auth_xuid = '0'; user_type = 'legacy'; version_type = 'RouterSmoke'
}
$argsList = [Collections.Generic.List[string]]::new()
foreach ($v in @('-Xmx2G', '-Xms512M', '-Dfml.earlyprogresswindow=false', '-Dmixin.debug.export=true')) { $argsList.Add($v) }
$argsList.Add("-Drouter.probe.start=$StartScenario")
$argsList.Add("-Drouter.probe.end=$EndScenario")
if ($Ae2Curios) { $argsList.Add("-Drouter.probe.remoteAe2=true") }
if ($EmptyMaterials) { $argsList.Add("-Drouter.probe.empty=true") }
if ($Pattern) { $argsList.Add("-Drouter.probe.pattern=true") }
if ($Remote) { $argsList.Add("-Drouter.probe.remote=$Remote") }
if ($UnsupportedCheck) { $argsList.Add('-Drouter.probe.unsupported=true') }
function Expand-Argument([string]$value) {
    foreach ($key in $values.Keys) { $value = $value.Replace('${' + $key + '}', $values[$key]) }
    return $value
}
foreach ($arg in $json.arguments.jvm) {
    if ($arg -is [string]) { $argsList.Add((Expand-Argument $arg)) }
}
$argsList.Add($json.mainClass)
foreach ($arg in $json.arguments.game) {
    if ($arg -is [string]) { $argsList.Add((Expand-Argument $arg)) }
}
$neoForgeArgument = $argsList.IndexOf('--fml.neoForgeVersion')
if ($neoForgeArgument -lt 0) { throw 'Instance JSON has no NeoForge version argument' }
$argsList[$neoForgeArgument + 1] = $NeoForgeVersion
$argsList.Add('--width'); $argsList.Add('640'); $argsList.Add('--height'); $argsList.Add('480')
$argFile = "$smoke\launch.args"
# Java argument files treat backslash as escape, so use forward slashes for paths.
$quoted = $argsList | ForEach-Object { '"' + $_.Replace('\', '/').Replace('"', '\"') + '"' }
[IO.File]::WriteAllLines($argFile, $quoted, [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText("$smoke\options.txt", "lang:$Language`nrenderDistance:2`nmaxFps:30`nsimulationDistance:5`nsoundCategory_master:0.0`n", [Text.UTF8Encoding]::new($false))
if ($PrepareOnly) { Write-Host "Isolated client prepared: $smoke"; return }
$proc = Start-Process -FilePath 'E:\JAVA\bin\java.exe' -ArgumentList ('@"' + $argFile + '"') -WorkingDirectory $smoke -WindowStyle Hidden -PassThru -RedirectStandardOutput "$smoke\stdout.log" -RedirectStandardError "$smoke\stderr.log"
[IO.File]::WriteAllText("$smoke\process.id", $proc.Id.ToString())
Write-Host "Isolated smoke client started: PID $($proc.Id); logs in $smoke"
