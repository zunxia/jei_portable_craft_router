param([Parameter(Mandatory=$true)][string]$Classes, [Parameter(Mandatory=$true)][string]$OutputJar)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$Classes = [IO.Path]::GetFullPath($Classes)
if (Test-Path -LiteralPath $OutputJar) { Remove-Item -LiteralPath $OutputJar -Force }
$zip = [IO.Compression.ZipFile]::Open($OutputJar, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in Get-ChildItem -LiteralPath $Classes -File -Recurse -Force) {
        $name = $file.FullName.Substring($Classes.Length + 1).Replace('\', '/')
        [void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $name, [IO.Compression.CompressionLevel]::Optimal)
    }
} finally { $zip.Dispose() }
