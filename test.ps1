$ErrorActionPreference = 'Stop'

function Invoke-Api {
    param (
        [string]$Method,
        [string]$Url,
        [string]$Body,
        [string]$Token
    )
    
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    
    try {
        if ($Body) {
            $resp = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -ContentType "application/json" -Body $Body
        } else {
            $resp = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers
        }
        return $resp
    } catch {
        Write-Host "Error calling $Url : $_"
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errBody = $reader.ReadToEnd()
            Write-Host "Response Body: $errBody"
        }
        throw
    }
}

Write-Host "1. Registering..."
$regBody = '{"username":"e2euser2","email":"e2e2@example.com","password":"password123"}'
try {
    $reg = Invoke-Api -Method POST -Url "http://localhost:8080/api/auth/register" -Body $regBody
    Write-Host "Registered user."
} catch {
    Write-Host "Register failed (probably already exists). Continuing..."
}

Write-Host "2. Logging in..."
$loginBody = '{"email":"e2e2@example.com","password":"password123"}'
$auth = Invoke-Api -Method POST -Url "http://localhost:8080/api/auth/login" -Body $loginBody
$token = $auth.accessToken
Write-Host "Logged in. Token: $token"

Write-Host "3. Creating Project..."
$projBody = '{"name":"liftoff-demo2","subdomain":"liftoff-demo2","githubRepoUrl":"https://github.com/EgyptianMama/LiftOff.git","branch":"main"}'
$proj = Invoke-Api -Method POST -Url "http://localhost:8080/api/projects" -Body $projBody -Token $token
$secret = $proj.githubWebhookSecret
$projectId = $proj.id
Write-Host "Project Created. ID: $projectId, Secret: $secret"
