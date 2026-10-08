param(
    [string]$Java8Home = "$PSScriptRoot/build/toolchains/jdk8u504-b01",
    [string]$Java16Home = "$PSScriptRoot/build/toolchains/jdk-16.0.2+7",
    [string]$Java21Home = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot',
    [string]$Java25Home = 'C:/Program Files/Java/jdk-25.0.2'
)

$ErrorActionPreference = 'Stop'
foreach ($jdkPath in @($Java8Home, $Java16Home, $Java21Home, $Java25Home)) {
    if (-not (Test-Path -LiteralPath "$jdkPath/bin/javac.exe")) {
        throw "JDK missing at $jdkPath. Pass its path using the corresponding Java Home parameter."
    }
}

$env:JAVA_HOME = $Java21Home
$env:Path = "$Java21Home/bin;$env:Path"
Push-Location $PSScriptRoot
try {
    & ./gradlew.bat :ReplayStudio:test :26.3:build --console=plain --max-workers=4 "-Porg.gradle.java.installations.paths=$Java8Home,$Java16Home,$Java25Home"
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
    Get-ChildItem versions/26.3/build/libs -Filter *.jar | Where-Object Name -NotMatch '-(raw|sources)\.jar$' | Select-Object FullName, Length
} finally {
    Pop-Location
}
