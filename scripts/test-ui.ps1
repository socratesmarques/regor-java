$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $repo 'backend'
$target = Join-Path $backend 'target'
$info = Join-Path $target 'ui-fixture.json'
$stop = Join-Path $target 'stop-ui-fixture'
$oldApi = $env:REGOR_API_URL
$fixtureProcess = $null
New-Item -ItemType Directory -Path $target -Force | Out-Null
foreach ($file in @($info, $stop)) {
    if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file }
}
try {
    $fixtureProcess = Start-Process -FilePath $env:ComSpec -WindowStyle Hidden -PassThru -WorkingDirectory $backend -ArgumentList '/d /c ..\mvnw.cmd -B -ntp test-compile org.codehaus.mojo:exec-maven-plugin:3.6.3:java -Dexec.mainClass=com.regor.backend.UiFixtureServer -Dexec.classpathScope=test' -RedirectStandardOutput (Join-Path $target 'ui-fixture.log') -RedirectStandardError (Join-Path $target 'ui-fixture-error.log')
    $deadline = (Get-Date).AddMinutes(2)
    while (!(Test-Path -LiteralPath $info)) {
        if ($fixtureProcess.HasExited) { throw "O backend de teste falhou. Consulte backend/target/ui-fixture.log." }
        if ((Get-Date) -gt $deadline) { throw 'Tempo limite ao iniciar o backend de teste.' }
        Start-Sleep -Milliseconds 300
    }
    $fixture = Get-Content -LiteralPath $info -Raw | ConvertFrom-Json
    $env:REGOR_API_URL = "http://localhost:$($fixture.port)"
    Push-Location $repo
    try {
        & .\mvnw.cmd -B -ntp -f desktop/pom.xml '-Dtest=ConnectedViewsIT' test
        if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes conectados do JavaFX.' }
    } finally { Pop-Location }
} finally {
    $env:REGOR_API_URL = $oldApi
    Set-Content -LiteralPath $stop -Value 'stop'
    if ($fixtureProcess -and !$fixtureProcess.HasExited -and !$fixtureProcess.WaitForExit(15000)) {
        $fixtureProcess.Kill($true)
    }
}
