param([string[]]$Classes, [switch]$Bytecode)
$ErrorActionPreference = 'Stop'
$cp = Get-Content -Raw (Join-Path (Split-Path $PSScriptRoot -Parent) 'build\classpath.txt')
foreach ($cls in $Classes) {
    if ($Bytecode) { & java --add-modules jdk.jdeps -m jdk.jdeps/com.sun.tools.javap.Main -p -c -cp $cp $cls }
    else { & java --add-modules jdk.jdeps -m jdk.jdeps/com.sun.tools.javap.Main -p -cp $cp $cls }
}
