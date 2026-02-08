# Build script for Spring Boot application (Windows)
# This script builds the application on the host and creates a Docker image

Write-Host "Building Spring Boot application..." -ForegroundColor Green

# Build using gradle
Write-Host "Running gradle build..." -ForegroundColor Yellow
& .\gradlew clean build -x test

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Application built successfully!" -ForegroundColor Green

# Get the JAR file path
$jarFile = Get-ChildItem -Path "build/libs" -Filter "*.jar" | Select-Object -First 1

if ($null -eq $jarFile) {
    Write-Host "Error: JAR file not found in build/libs directory" -ForegroundColor Red
    exit 1
}

Write-Host "Found JAR file: $($jarFile.FullName)" -ForegroundColor Cyan

# Build Docker image
Write-Host "Building Docker image 'orders-app:latest'..." -ForegroundColor Yellow
docker build -t orders-app:latest -f docker/Dockerfile.runtime .

if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Docker image built successfully!" -ForegroundColor Green
Write-Host "You can now run: docker-compose up -d" -ForegroundColor Cyan

