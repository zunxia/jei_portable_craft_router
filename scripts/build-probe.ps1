$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$out = "$root\build\probe-classes"
New-Item -ItemType Directory -Force -Path $out | Out-Null
$cp = "$root\build\classes;" + (Get-Content -Raw -Encoding UTF8 "$root\build\classpath.txt")
$sources = @(Get-ChildItem "$root\tests\probe\java" -Filter '*.java' -Recurse | ForEach-Object FullName)
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $out @sources
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed' }
Get-ChildItem "$root\tests\probe\resources" -Force | Copy-Item -Destination $out -Recurse -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = "$root\build\router_probe.jar"
if (Test-Path -LiteralPath $jar) { Remove-Item -LiteralPath $jar -Force }
& "$PSScriptRoot\package.ps1" -Classes $out -OutputJar $jar
Write-Host "Probe built: $jar"
