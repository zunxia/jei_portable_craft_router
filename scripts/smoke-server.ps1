param([string]$ServerRoot = 'E:\DfyWork\ATM10-Server', [switch]$WithoutJei, [switch]$Ae2Curios, [switch]$EmptyMaterials, [switch]$Pattern)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$test = if ($WithoutJei) { "$root\build\server-no-jei" } else { "$root\build\smoke-server" }
New-Item -ItemType Directory -Force -Path "$test\mods" | Out-Null
if (-not (Test-Path -LiteralPath "$test\libraries")) {
    New-Item -ItemType Junction -Path "$test\libraries" -Target "$ServerRoot\libraries" | Out-Null
}
Copy-Item -LiteralPath "$ServerRoot\libraries\net\neoforged\neoforge\21.1.251\win_args.txt" -Destination "$test\server.args" -Force
$instance = (Get-Content -LiteralPath "$root\build\instance.txt" -Raw -Encoding UTF8).Trim()
$mods = @(Get-ChildItem -LiteralPath "$instance\mods" -Filter '*.jar' |
    Where-Object { $_.Name -match '^(jei-|appliedenergistics|ae2wtlib|ae2jei|crafting_on|curios|cyclopscore|integrateddynamics|integratedtunnels|integratedcrafting|commoncapabilities|guideme)|integratedterminals-' })
if ($WithoutJei) {
    foreach ($old in @(Get-ChildItem -LiteralPath "$test\mods" -Filter '*.jar' |
        Where-Object { $_.Name -match '^(jei-|ae2jeiintegration)' })) {
        Remove-Item -LiteralPath $old.FullName
    }
}
foreach ($mod in $mods) {
    if ($WithoutJei -and $mod.Name -match '^(jei-|ae2jeiintegration)') { continue }
    Copy-Item -LiteralPath $mod.FullName -Destination "$test\mods" -Force
}
Copy-Item "$root\build\router_probe.jar" "$test\mods" -Force
Get-ChildItem -LiteralPath "$test\mods" -Filter "jei_portable_craft_router-*.jar" | Remove-Item -Force
Copy-Item "$root\build\jei_portable_craft_router-1.3.2.jar" "$test\mods" -Force
# Dedicated smoke server binds only localhost; it never touches the existing server world.
[IO.File]::WriteAllText("$test\server.properties", "server-ip=127.0.0.1`nserver-port=25576`nonline-mode=false`nview-distance=2`nsimulation-distance=3`nspawn-protection=0`nlevel-type=minecraft:flat`nmax-players=4`n", [Text.UTF8Encoding]::new($false))
Copy-Item -LiteralPath "$ServerRoot\eula.txt" -Destination "$test\eula.txt" -Force
$flags = ""
if ($Ae2Curios) { $flags += " -Drouter.probe.remoteAe2=true" }
if ($EmptyMaterials) { $flags += " -Drouter.probe.empty=true" }
if ($Pattern) { $flags += " -Drouter.probe.pattern=true" }
$proc = Start-Process -FilePath 'E:\JAVA\bin\java.exe' -ArgumentList ("-Xmx1G -Xms256M" + $flags + " @server.args nogui") -WorkingDirectory $test -WindowStyle Hidden -PassThru -RedirectStandardOutput "$test\stdout.log" -RedirectStandardError "$test\stderr.log"
[IO.File]::WriteAllText("$test\process.id", $proc.Id.ToString())
Write-Host "Isolated dedicated server: PID $($proc.Id), 127.0.0.1:25576"
