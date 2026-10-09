$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
& "$PSScriptRoot\smoke-client.ps1" -WithProbe -Session version-template -StartScenario 2 -PrepareOnly
$test = "$root\build\version-smoke"
New-Item -ItemType Directory -Force -Path "$test\mods" | Out-Null
foreach ($old in @(Get-ChildItem -LiteralPath "$test\mods" -Filter '*.jar' -File)) {
    if ($old.Name -match '^(jei-|jei_portable_craft_router-|appliedenergistics|ae2wtlib|ae2jei|crafting_on|curios|cyclopscore|integrateddynamics|integratedtunnels|integratedcrafting|commoncapabilities|guideme|router_probe)|integratedterminals-') {
        Remove-Item -LiteralPath $old.FullName
    }
}
Get-ChildItem "$root\build\version-template\mods" -Filter '*.jar' | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination "$test\mods" -Force }
$jei = Get-ChildItem "$test\mods" -Filter 'jei-*.jar' | Select-Object -First 1
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$sourceJar = Get-ChildItem "$root\build\version-template\mods" -Filter 'jei-*.jar' | Select-Object -First 1
$source = [IO.Compression.ZipFile]::OpenRead($sourceJar.FullName)
Remove-Item -LiteralPath $jei.FullName -Force
$zip = [IO.Compression.ZipFile]::Open($jei.FullName, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($old in $source.Entries) {
        $entry = $zip.CreateEntry($old.FullName, [IO.Compression.CompressionLevel]::Optimal)
        $input = $old.Open(); $output = $entry.Open()
        try {
            if ($old.FullName -eq 'META-INF/neoforge.mods.toml') {
                $reader = [IO.StreamReader]::new($input)
                $toml = $reader.ReadToEnd() -replace '(?m)^\s*version\s*=\s*"[^"\r\n]+"', 'version="19.58.0.0"'
                $writer = [IO.StreamWriter]::new($output, [Text.UTF8Encoding]::new($false)); $writer.Write($toml); $writer.Dispose()
            } else { $input.CopyTo($output) }
        } finally { $input.Dispose(); $output.Dispose() }
    }
} finally { $zip.Dispose(); $source.Dispose() }
# 上方模板已准备运行参数，版本替换仅发生在隔离目录。
foreach ($link in @('libraries','assets','natives')) {
    if (-not (Test-Path -LiteralPath "$test\$link")) {
        $target = @((Get-Item -LiteralPath "$root\build\version-template\$link").Target)[0]
        New-Item -ItemType Junction -Path "$test\$link" -Target $target | Out-Null
    }
}
Copy-Item "$root\build\version-template\vanilla.jar" "$test\vanilla.jar" -Force
Copy-Item "$root\build\version-template\options.txt" "$test\options.txt" -Force
$argsText = [IO.File]::ReadAllText("$root\build\version-template\launch.args").Replace('version-template','version-smoke')
[IO.File]::WriteAllText("$test\launch.args", '"-Drouter.probe.disabled=true"' + "`n" + $argsText, [Text.UTF8Encoding]::new($false))
$proc = Start-Process -FilePath 'E:\JAVA\bin\java.exe' -ArgumentList ('@"'+"$test\launch.args"+'"') -WorkingDirectory $test -WindowStyle Hidden -PassThru -RedirectStandardOutput "$test\stdout.log" -RedirectStandardError "$test\stderr.log"
[IO.File]::WriteAllText("$test\process.id", $proc.Id.ToString())
Write-Host "JEI version guard smoke started: PID $($proc.Id)"
