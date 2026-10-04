Write-Host "Waiting for Keycloak to be fully ready..."

while ($true) {
    try {
        $res = Invoke-WebRequest -Uri "http://localhost:9090/realms/file-management/.well-known/openid-configuration" -UseBasicParsing
        if ($res.StatusCode -eq 200) {
            Write-Host "Keycloak is UP!"
            break
        }
    } catch {
        Write-Host "Keycloak not ready yet, retrying in 5 seconds..."
    }
    Start-Sleep -Seconds 5
}

Write-Host "Starting UserDetailService..."
Start-Process powershell -WindowStyle Hidden -ArgumentList "-NoExit -Command `"cd UserDetailService; mvn spring-boot:run '-Dmaven.test.skip=true'`""

Write-Host "Starting FileService..."
Start-Process powershell -WindowStyle Hidden -ArgumentList "-NoExit -Command `"cd FileService; mvn spring-boot:run '-Dmaven.test.skip=true'`""

Write-Host "Starting AIProcessingService..."
Start-Process powershell -WindowStyle Hidden -ArgumentList "-NoExit -Command `"cd AIProcessingService; mvn spring-boot:run '-Dmaven.test.skip=true'`""

# Give them 10 seconds to initialize before the Gateway
Start-Sleep -Seconds 10

Write-Host "Starting ApiGateway..."
Start-Process powershell -WindowStyle Hidden -ArgumentList "-NoExit -Command `"cd ApiGateway; mvn spring-boot:run '-Dmaven.test.skip=true'`""

Write-Host "Starting Frontend..."
Start-Process powershell -WindowStyle Hidden -ArgumentList "-NoExit -Command `"cd FilePortalFrontend; npm install; npm run dev`""

Write-Host "All services have been started in the background!"
