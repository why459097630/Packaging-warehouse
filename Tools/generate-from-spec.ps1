<# 
  generate-from-spec.ps1
  读取一个简单的接口规范 JSON，生成最小可跑通的 Kotlin 代码：
  - data model（根据 responseModel.fields 生成）
  - ApiService（Retrofit 接口定义）
  - RetrofitClient（Retrofit 构建器）
#>

[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string]$SpecPath,

  [ValidateSet("simple-template","core-template","form-template")]
  [string]$Template = "simple-template"
)

$ErrorActionPreference = "Stop"

function Ensure-Dir([string]$path) {
  if (-not (Test-Path $path)) {
    New-Item -ItemType Directory -Force -Path $path | Out-Null
  }
}

# 读取规范
if (-not (Test-Path $SpecPath)) {
  throw "Spec file not found: $SpecPath"
}
$spec = Get-Content $SpecPath -Raw | ConvertFrom-Json

# 解析包名和基础路径
$pkg = if ($spec.package) { $spec.package } else { "com.ndjc.app" }

# 工程根目录（模板所在目录）
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..") | Select-Object -ExpandProperty Path
$projRoot = Resolve-Path (Join-Path $repoRoot "templates\$Template") | Select-Object -ExpandProperty Path

# Kotlin 源码输出根
$javaPkgPath = ($pkg -replace '\.', '/')
$srcRoot = Join-Path $projRoot "app/src/main/java/$javaPkgPath"
Ensure-Dir $srcRoot

# 子目录
$modelDir   = Join-Path $srcRoot "data/model"
$remoteDir  = Join-Path $srcRoot "data/remote"
Ensure-Dir $modelDir
Ensure-Dir $remoteDir

# ========== 1) 生成 Model ==========
if ($spec.endpoints -isnot [System.Collections.IEnumerable]) {
  throw "spec.endpoints 不是数组，请检查 JSON 结构。"
}

# 收集所有响应模型（去重）
$models = @{}
foreach ($ep in $spec.endpoints) {
  if ($ep.responseModel -and $ep.responseModel.name) {
    $models[$ep.responseModel.name] = $ep.responseModel
  }
}

foreach ($modelName in $models.Keys) {
  $model = $models[$modelName]

  # 用 psobject.Properties 枚举字段（修复 .GetEnumerator 报错）
  $fieldLines = @()
  if ($model.fields) {
    foreach ($prop in $model.fields.psobject.Properties) {
      # 生成 Kotlin 字段行，例如:  val id: Int
      $fieldLines += "    val $($prop.Name): $($prop.Value)"
    }
  }
  $fieldsBlock = ($fieldLines -join "`r`n")

  $modelKt = @"
package $pkg.data.model

data class $modelName(
$fieldsBlock
)
"@

  $modelPath = Join-Path $modelDir "$modelName.kt"
  Set-Content -Path $modelPath -Value $modelKt -Encoding UTF8
  Write-Host "✔ Model generated: $($modelPath -replace [regex]::Escape($repoRoot),'.')" -ForegroundColor Green
}

# ========== 2) 生成 ApiService ==========
# 最简单可用的 Retrofit 接口定义：只处理 GET + pathParams（够你当前验证）
$apiFunLines = @()
foreach ($ep in $spec.endpoints) {
  $name = if ($ep.name) { $ep.name } else { "api${([Math]::Abs([Guid]::NewGuid().GetHashCode()))}" }
  $method = ($ep.method ?? "GET").ToUpper()
  $path   = if ($ep.path) { $ep.path } else { "/" }
  $respName = if ($ep.responseModel -and $ep.responseModel.name) { $ep.responseModel.name } else { "Unit" }

  # 只处理 GET，其他方法简单落入 GET（你后面可以按需拓展）
  $httpAnno = "@GET(`"$path`")"

  # 处理 path 参数，如果是单个字符串也兼容
  $paramList = @()
  if ($ep.pathParams) {
    foreach ($p in @($ep.pathParams)) {
      # 类型先默认 Int（需要可在 spec 里加类型之后再拓展）
      $paramList += "@Path(`"$p`") $p: Int"
    }
  }
  $paramsSig = $paramList -join ", "

  if ([string]::IsNullOrWhiteSpace($paramsSig)) {
    $apiFunLines += @"
    $httpAnno
    suspend fun $name(): $respName
"@
  } else {
    $apiFunLines += @"
    $httpAnno
    suspend fun $name($paramsSig): $respName
"@
  }
}

$apiServiceKt = @"
package $pkg.data.remote

import $pkg.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

$($apiFunLines -join "`r`n")
}
"@

$apiServicePath = Join-Path $remoteDir "ApiService.kt"
Set-Content -Path $apiServicePath -Value $apiServiceKt -Encoding UTF8
Write-Host "✔ ApiService generated: $($apiServicePath -replace [regex]::Escape($repoRoot),'.')" -ForegroundColor Green


# ========== 3) 生成 RetrofitClient ==========
$baseUrl = if ($spec.baseUrl) { $spec.baseUrl.TrimEnd('/') + "/" } else { "https://example.com/" }

$retrofitClientKt = @"
package $pkg.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("$baseUrl")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
"@

$retrofitClientPath = Join-Path $remoteDir "RetrofitClient.kt"
Set-Content -Path $retrofitClientPath -Value $retrofitClientKt -Encoding UTF8
Write-Host "✔ RetrofitClient generated: $($retrofitClientPath -replace [regex]::Escape($repoRoot),'.')" -ForegroundColor Green

Write-Host "`nAll generated from spec: $($SpecPath) for template '$Template' ✅" -ForegroundColor Cyan
