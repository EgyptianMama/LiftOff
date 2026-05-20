$ErrorActionPreference = 'Stop'

$projectId = "bec6a8ed-6aa8-463d-a4e8-7841ba7c4353"
$secret = "hTdZsBZkUxKL4oWFyProOw4Bsz350AcZpP92Poh-Yj4"

$payload = @"
{
  "ref": "refs/heads/main",
  "repository": {
    "name": "LiftOff",
    "full_name": "EgyptianMama/LiftOff"
  },
  "commits": [
    {
      "id": "1234567890abcdef",
      "message": "Initial commit"
    }
  ]
}
"@

# Compute HMAC SHA256
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($secret)
$hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($payload))
$signature = "sha256=" + [BitConverter]::ToString($hash).Replace("-", "").ToLower()

Write-Host "Sending webhook with signature: $signature"

$headers = @{
    "X-GitHub-Event" = "push"
    "X-Hub-Signature-256" = $signature
}

try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/webhooks/github/$projectId" -Method Post -Headers $headers -ContentType "application/json" -Body $payload
    Write-Host "Webhook response:"
    $resp | ConvertTo-Json -Depth 5
} catch {
    Write-Host "Error calling webhook : $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        Write-Host "Response Body: $errBody"
    }
    throw
}
