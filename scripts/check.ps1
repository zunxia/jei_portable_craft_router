$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$build = "$root\build"
$cp = "$build\classes;" + (Get-Content -Raw -Encoding UTF8 "$build\classpath.txt")
New-Item -ItemType Directory -Force -Path "$build\checks" | Out-Null
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d "$build\checks" "$root\tests\java\RouterChecks.java"
if ($LASTEXITCODE -ne 0) { throw 'Check compilation failed' }
& java -cp "$build\checks;$cp" RouterChecks $build
if ($LASTEXITCODE -ne 0) { throw 'Router checks failed' }
$langDir = Join-Path $root 'src\main\resources\assets\jei_portable_craft_router\lang'
$english = Get-Content -LiteralPath "$langDir\en_us.json" -Raw -Encoding UTF8 | ConvertFrom-Json
$chinese = Get-Content -LiteralPath "$langDir\zh_cn.json" -Raw -Encoding UTF8 | ConvertFrom-Json
$keys = @('router.apply_failed','router.jei_cannot_apply','router.material_ready','router.material_missing','router.material_check_on_open','router.ctrl_craft','router.write_pattern','router.pattern_hint','router.pattern_server','router.no_pattern_module','router.pattern_open_failed')
foreach ($key in $keys) {
    if (-not $english.$key -or -not $chinese.$key) { throw "Missing bilingual key: $key" }
}
foreach ($file in @('en_us.json','zh_cn.json')) {
    $bytes = [IO.File]::ReadAllBytes("$langDir\$file")
    if ($bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191) { throw "Language resource has UTF-8 BOM: $file" }
}
Write-Host 'PASS: material status and failure translations exist in both UTF-8 languages'
