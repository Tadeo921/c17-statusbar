# Compile the release Java and Kotlin sources only (fast; what the desktop checks run against).
. (Join-Path $PSScriptRoot 'c17-env.ps1')
Set-Location -LiteralPath $C17Root
$log = Join-Path $C17Out 'compile.log'
$code = 1
C17-WithSdk {
    & $C17Gradle ':app:compileReleaseJavaWithJavac' @C17GradleFlags > $log 2>&1
    $script:code = $LASTEXITCODE
}
Get-Content -LiteralPath $log -Tail 25
Write-Output "GRADLE EXIT $code"
exit $code
