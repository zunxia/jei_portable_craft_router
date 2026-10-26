param([string]$ArtifactDir = '', [switch]$Download, [string]$MinVersion = '19.27.0.335', [string]$MaxVersion = '19.57.0.451', [int]$ExpectedCount = 83)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (-not $ArtifactDir) { $ArtifactDir = Join-Path $root 'build\jei-wide' }
if ($Download) {
    New-Item -ItemType Directory -Force -Path $ArtifactDir | Out-Null
    $base = 'https://maven.blamejared.com/mezz/jei/jei-1.21.1-neoforge'
    $metadata = [xml](Invoke-WebRequest "$base/maven-metadata.xml" -UseBasicParsing).Content
    $versions = @($metadata.metadata.versioning.versions.version | Where-Object {
        [version]$_ -ge [version]$MinVersion -and [version]$_ -le [version]$MaxVersion
    })
    foreach ($version in $versions) {
        $file = Join-Path $ArtifactDir "jei-1.21.1-neoforge-$version.jar"
        if (-not (Test-Path -LiteralPath $file)) {
            Invoke-WebRequest "$base/$version/jei-1.21.1-neoforge-$version.jar" -OutFile $file -UseBasicParsing
        }
    }
}
$cp = "$root\build\classes;" + (Get-Content -Raw -Encoding UTF8 "$root\build\classpath.txt").Trim()
New-Item -ItemType Directory -Force -Path "$root\build\matrix-checks" | Out-Null
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d "$root\build\matrix-checks" "$root\tests\java\JeiMatrixChecks.java"
if ($LASTEXITCODE -ne 0) { throw 'JEI matrix check compilation failed' }
& java -cp "$root\build\matrix-checks;$cp" JeiMatrixChecks "$root\build\classes" $ArtifactDir $MinVersion $MaxVersion $ExpectedCount
if ($LASTEXITCODE -ne 0) { throw 'JEI matrix checks failed' }
