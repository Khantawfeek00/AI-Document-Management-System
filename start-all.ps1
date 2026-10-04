$ErrorActionPreference = "Stop"

Write-Host "1. Starting Docker containers..." -ForegroundColor Green
docker-compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker is not running or compose failed! Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

Write-Host "2. Making sure Ollama model is downloaded (this runs in background)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit -Command `"Write-Host 'Downloading Ollama model...'; docker exec ollama_lab05 ollama pull nomic-embed-text; Write-Host 'Finished pulling model! You can close this window.'`""

Write-Host "3. Starting Java Microservices..." -ForegroundColor Green
# Start ApiGateway
Start-Process powershell -ArgumentList "-NoExit -Command `"Title ApiGateway; cd ApiGateway; mvn spring-boot:run`""

# Start UserDetailService
Start-Process powershell -ArgumentList "-NoExit -Command `"Title UserDetailService; cd UserDetailService; mvn spring-boot:run`""

# Start FileService
Start-Process powershell -ArgumentList "-NoExit -Command `"Title FileService; cd FileService; mvn spring-boot:run`""

# Start AIProcessingService with prompt inside the new window
Start-Process powershell -ArgumentList "-NoExit -Command `"Title AIProcessingService; `$key = Read-Host 'Please enter your GROQ_API_KEY (press Enter to skip if already set)'; if (`$key) { `$env:GROQ_API_KEY = `$key }; cd AIProcessingService; mvn spring-boot:run`""

Write-Host "4. Starting React Frontend..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit -Command `"Title Frontend; cd FilePortalFrontend; npm install; npm run dev`""

Write-Host "All services are starting up! Check the newly opened terminal windows for logs." -ForegroundColor Green
Write-Host "Frontend will be available at http://localhost:5173" -ForegroundColor Cyan
