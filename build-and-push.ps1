$sec = Read-Host "Enter GH Packages token" -AsSecureString
$plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
)
$Env:GH_PACKAGES_TOKEN = $plain

$services = @(
    "auth",
    "cart",
    "eureka",
    "gateway",
    "order",
    "product-service",
    "file-service"
)

foreach ($s in $services) {
    $tag = "ghcr.io/pavgos22/$($s):latest"
    Write-Host "Building $s..."
    docker build --build-arg GH_PACKAGES_TOKEN=$Env:GH_PACKAGES_TOKEN -t $tag .\$s
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

foreach ($s in $services) {
    $tag = "ghcr.io/pavgos22/$($s):latest"
    Write-Host "Pushing $s..."
    docker push $tag
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

Remove-Variable plain -ErrorAction SilentlyContinue
Remove-Variable sec -ErrorAction SilentlyContinue
Remove-Item Env:GH_PACKAGES_TOKEN -ErrorAction SilentlyContinue
Write-Host "Done."
