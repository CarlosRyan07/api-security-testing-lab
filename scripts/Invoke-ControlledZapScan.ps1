[CmdletBinding(DefaultParameterSetName = 'Planejar')]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^ghcr\.io/zaproxy/zaproxy@sha256:[0-9a-f]{64}$')]
    [string] $ImagemZap,

    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 50)]
    [int] $OrcamentoRequests,

    [ValidateRange(1, 2)]
    [int] $LimiteMinutos = 2,

    [string] $BaseUrl = $(if ($env:BASE_URL) { $env:BASE_URL } else { 'http://localhost:8888' }),

    [string[]] $ContainersSaude = @('api.mypremiumdealership.com', 'crapi-identity'),

    [ValidateRange(1024, 65535)]
    [int] $PortaGateway = 18080,

    [ValidateRange(1024, 65535)]
    [int] $PortaControle = 18081,

    [Parameter(Mandatory = $true, ParameterSetName = 'Executar')]
    [switch] $Executar,

    [Parameter(Mandatory = $true, ParameterSetName = 'Executar')]
    [ValidateSet('AUTORIZO_DAST_PASSIVO')]
    [string] $Confirmacao
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$nomeContainerZap = $null
$processoGateway = $null
$processoZap = $null

function Assert-HttpUrl {
    param([Parameter(Mandatory = $true)][string] $Url)

    $uri = $null
    if (-not [Uri]::TryCreate($Url, [UriKind]::Absolute, [ref] $uri) -or
        @('http', 'https') -notcontains $uri.Scheme -or
        [string]::IsNullOrWhiteSpace($uri.Host) -or
        -not [string]::IsNullOrWhiteSpace($uri.UserInfo) -or
        -not [string]::IsNullOrWhiteSpace($uri.Query) -or
        -not [string]::IsNullOrWhiteSpace($uri.Fragment) -or
        @('', '/') -notcontains $uri.AbsolutePath) {
        throw 'BASE_URL deve ser uma URL HTTP ou HTTPS de raiz, sem credenciais, query ou fragmento.'
    }
}

function Assert-ScopedOpenApi {
    param([Parameter(Mandatory = $true)][string] $Path)

    try {
        $spec = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
    } catch {
        throw 'A OpenAPI reduzida não contém JSON válido.'
    }

    $allowed = [ordered]@{
        '/identity/api/auth/signup' = 'post'
        '/identity/api/auth/login' = 'post'
        '/identity/api/v2/user/dashboard' = 'get'
    }
    $actualPaths = @($spec.paths.PSObject.Properties.Name)
    if ($actualPaths.Count -ne $allowed.Count) {
        throw 'A OpenAPI reduzida deve conter exatamente três paths.'
    }

    foreach ($entry in $allowed.GetEnumerator()) {
        $pathProperty = $spec.paths.PSObject.Properties[$entry.Key]
        if ($null -eq $pathProperty) {
            throw "Path obrigatório ausente na OpenAPI reduzida: $($entry.Key)"
        }
        $operations = @($pathProperty.Value.PSObject.Properties.Name)
        if ($operations.Count -ne 1 -or $operations[0] -ne $entry.Value) {
            throw "Operação inválida na OpenAPI reduzida: $($entry.Key)"
        }
    }
}

function Get-ContainerState {
    param([Parameter(Mandatory = $true)][string] $Name)

    $json = & docker inspect --format '{{json .State}}' $Name 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($json)) {
        throw "Container obrigatório não encontrado: $Name"
    }

    $state = $json | ConvertFrom-Json
    if (-not $state.Running) {
        throw "Container obrigatório não está em execução: $Name"
    }
    $healthProperty = $state.PSObject.Properties['Health']
    if ($null -ne $healthProperty -and $healthProperty.Value.Status -ne 'healthy') {
        throw "Container obrigatório não está saudável: $Name ($($healthProperty.Value.Status))"
    }
}

function Stop-ZapContainer {
    if ([string]::IsNullOrWhiteSpace($script:nomeContainerZap)) {
        return
    }

    & docker inspect $script:nomeContainerZap *> $null
    if ($LASTEXITCODE -eq 0) {
        & docker stop --time 1 $script:nomeContainerZap *> $null
    }
}

if ($PortaGateway -eq $PortaControle) {
    throw 'As portas do gateway e de controle devem ser diferentes.'
}
if ($null -eq $ContainersSaude -or $ContainersSaude.Count -lt 1 -or
    @($ContainersSaude | Where-Object { [string]::IsNullOrWhiteSpace($_) }).Count -gt 0) {
    throw 'Informe ao menos um container obrigatório em ContainersSaude.'
}

Assert-HttpUrl -Url $BaseUrl

$raizProjeto = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$diretorioZap = [IO.Path]::GetFullPath((Join-Path $raizProjeto 'target\zap'))
$openApiPath = [IO.Path]::GetFullPath((Join-Path $diretorioZap 'scoped-openapi.json'))
$classesTeste = [IO.Path]::GetFullPath((Join-Path $raizProjeto 'target\test-classes'))
$classeGateway = Join-Path $classesTeste 'io\github\apisecurity\tooling\DastTrafficGate.class'
$statusGateway = [IO.Path]::GetFullPath((Join-Path $diretorioZap 'gate-status.json'))
$limiteSegundos = $LimiteMinutos * 60

if (-not (Test-Path -LiteralPath $openApiPath -PathType Leaf)) {
    throw 'OpenAPI reduzida ausente. Execute primeiro o gerador opt-in documentado.'
}
Assert-ScopedOpenApi -Path $openApiPath
if (-not (Test-Path -LiteralPath $classeGateway -PathType Leaf)) {
    throw 'Gateway DAST não compilado. Execute mvn test-compile com Java 17.'
}

$plano = [ordered]@{
    modo = $(if ($Executar) { 'executar' } else { 'planejar' })
    imagem = $ImagemZap
    orcamentoRequests = $OrcamentoRequests
    limiteSegundos = $limiteSegundos
    baseUrl = $BaseUrl.TrimEnd('/')
    containersSaude = $ContainersSaude
    portaGateway = $PortaGateway
    portaControle = $PortaControle
    openApi = $openApiPath
    activeScan = $false
    alphaRules = $false
    autenticacaoAutomatizada = $false
}

if (-not $Executar) {
    $plano | ConvertTo-Json -Depth 3
    Write-Host 'Planejamento concluído. Nenhum container foi iniciado e nenhuma chamada foi enviada à crAPI.'
    exit 0
}

if ($Confirmacao -ne 'AUTORIZO_DAST_PASSIVO') {
    throw 'Confirmação explícita inválida.'
}

try {
    & docker image inspect $ImagemZap *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'A imagem ZAP fixada por digest não está disponível localmente. O script não executa docker pull.'
    }

    foreach ($container in $ContainersSaude) {
        Get-ContainerState -Name $container
    }

    if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        throw 'JAVA_HOME deve apontar para um JDK 17 válido.'
    }
    $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
        throw 'JAVA_HOME deve apontar para um JDK 17 válido.'
    }
    $javaVersion = & $javaExe -version 2>&1
    if ($LASTEXITCODE -ne 0 -or ($javaVersion -join "`n") -notmatch 'version "17\.') {
        throw 'JAVA_HOME deve apontar especificamente para um JDK 17.'
    }

    New-Item -ItemType Directory -Force -Path $diretorioZap | Out-Null
    $artefatosExecucao = @(
        $statusGateway,
        (Join-Path $diretorioZap 'gate-stdout.log'),
        (Join-Path $diretorioZap 'gate-stderr.log'),
        (Join-Path $diretorioZap 'zap-stdout.log'),
        (Join-Path $diretorioZap 'zap-stderr.log'),
        (Join-Path $diretorioZap 'zap-report.json'),
        (Join-Path $diretorioZap 'zap-report.md')
    )
    foreach ($artefato in $artefatosExecucao) {
        if (Test-Path -LiteralPath $artefato -PathType Leaf) {
            Remove-Item -LiteralPath $artefato -Force
        }
    }

    $stdoutGateway = Join-Path $diretorioZap 'gate-stdout.log'
    $stderrGateway = Join-Path $diretorioZap 'gate-stderr.log'
    $argumentosGateway = @(
        '--add-modules', 'jdk.httpserver',
        '-cp', $classesTeste,
        'io.github.apisecurity.tooling.DastTrafficGate',
        "--upstream=$($BaseUrl.TrimEnd('/'))",
        '--listen-address=0.0.0.0',
        "--listen-port=$PortaGateway",
        "--control-port=$PortaControle",
        "--max-requests=$OrcamentoRequests",
        "--max-seconds=$limiteSegundos",
        "--status-file=$statusGateway"
    )
    $processoGateway = Start-Process `
        -FilePath $javaExe `
        -ArgumentList $argumentosGateway `
        -PassThru `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutGateway `
        -RedirectStandardError $stderrGateway

    $controleUri = "http://127.0.0.1:$PortaControle"
    $prazoInicializacao = [DateTimeOffset]::UtcNow.AddSeconds(10)
    $controleDisponivel = $false
    while ([DateTimeOffset]::UtcNow -lt $prazoInicializacao -and -not $processoGateway.HasExited) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri "$controleUri/status" -TimeoutSec 1
            if ($response.StatusCode -eq 200) {
                $controleDisponivel = $true
                break
            }
        } catch {
            Start-Sleep -Milliseconds 200
        }
    }
    if (-not $controleDisponivel) {
        throw 'O gateway de controle não iniciou dentro de dez segundos.'
    }

    $nomeContainerZap = "crapi-zap-safe-$([Guid]::NewGuid().ToString('N'))"
    $stdoutZap = Join-Path $diretorioZap 'zap-stdout.log'
    $stderrZap = Join-Path $diretorioZap 'zap-stderr.log'
    $volumeZap = '"{0}:/zap/wrk:rw"' -f $diretorioZap
    $argumentosDocker = @(
        'run', '--rm', '--pull=never',
        '--name', $nomeContainerZap,
        '-v', $volumeZap,
        $ImagemZap,
        'zap-api-scan.py',
        '-t', 'scoped-openapi.json',
        '-f', 'openapi',
        '-S',
        '-T', $LimiteMinutos,
        '-s',
        '-l', 'WARN',
        '-J', 'zap-report.json',
        '-w', 'zap-report.md',
        '-O', "http://host.docker.internal:$PortaGateway"
    )
    $processoZap = Start-Process `
        -FilePath 'docker' `
        -ArgumentList $argumentosDocker `
        -PassThru `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutZap `
        -RedirectStandardError $stderrZap

    $prazoExecucao = [DateTimeOffset]::UtcNow.AddSeconds($limiteSegundos)
    while (-not $processoZap.HasExited) {
        if ($processoGateway.HasExited) {
            Stop-ZapContainer
            throw "Gateway encerrou a execução por um controle de segurança. Consulte $statusGateway."
        }
        if ([DateTimeOffset]::UtcNow -ge $prazoExecucao) {
            Stop-ZapContainer
            throw 'Tempo máximo da execução DAST excedido.'
        }
        foreach ($container in $ContainersSaude) {
            Get-ContainerState -Name $container
        }
        Start-Sleep -Seconds 1
        $processoZap.Refresh()
        $processoGateway.Refresh()
    }

    $codigoZap = $processoZap.ExitCode
    Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$controleUri/complete" -TimeoutSec 2 | Out-Null
    if (-not $processoGateway.WaitForExit(5000)) {
        throw 'Gateway não encerrou após a conclusão do API Scan.'
    }
    if ($processoGateway.ExitCode -ne 0) {
        throw "Gateway encerrou com código $($processoGateway.ExitCode). Consulte $statusGateway."
    }

    Write-Host "API Scan encerrado com código oficial do ZAP: $codigoZap"
    Write-Host "Contagem do gateway registrada em: $statusGateway"
    exit $codigoZap
} finally {
    Stop-ZapContainer
    if ($null -ne $processoGateway -and -not $processoGateway.HasExited) {
        Stop-Process -Id $processoGateway.Id -Force
    }
}
