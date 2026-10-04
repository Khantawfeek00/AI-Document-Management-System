import subprocess
import sys
import os
import time

def check_docker():
    print("="*60)
    print(" NOTE: This complete service requires Docker to be running!")
    print(" If Docker Desktop is not running, please start it now.")
    print("="*60)
    print("Checking Docker status...")
    
    try:
        # Run docker info to verify the daemon is reachable
        result = subprocess.run(['docker', 'info'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            print("\n ERROR: Docker is not running or not accessible.")
            print("Please open 'Docker Desktop', wait for the engine to start, and run this script again.")
            sys.exit(1)
        print(" Docker is running.")
    except FileNotFoundError:
        print("\n ERROR: Docker command not found. Please install Docker Desktop.")
        sys.exit(1)

def start_infrastructure():
    print("Starting infrastructure via docker compose...")
    result = subprocess.run(['docker', 'compose', 'up', '-d'])
    if result.returncode != 0:
        print("\n ERROR: Failed to start docker compose.")
        sys.exit(1)
    print(" Infrastructure is up and running.")

def run_service(name, cmd, cwd, log_file):
    print(f"Starting {name}... (Logs: logs/{log_file})")
    os.makedirs('logs', exist_ok=True)
    with open(f"logs/{log_file}", "w") as f:
        # shell=True allows us to run mvn and npm cleanly on Windows
        proc = subprocess.Popen(cmd, cwd=cwd, shell=True, stdout=f, stderr=subprocess.STDOUT)
    return proc

def main():
    # 1. Check Docker
    check_docker()
    
    # 2. Start Compose containers
    start_infrastructure()
    
    # 3. Wait for infrastructure to stabilize
    print("Waiting 15 seconds for Postgres, Kafka, and Keycloak to initialize...")
    time.sleep(15)
    
    processes = []
    
    try:
        # 4. Launch all Spring Boot backend services
        mvn_cmd = "mvn spring-boot:run -Dmaven.test.skip=true"
        
        processes.append(("ApiGateway", run_service("ApiGateway", mvn_cmd, "ApiGateway", "api_gateway.log")))
        processes.append(("UserDetailService", run_service("UserDetailService", mvn_cmd, "UserDetailService", "user_detail.log")))
        processes.append(("FileService", run_service("FileService", mvn_cmd, "FileService", "file_service.log")))
        processes.append(("AIProcessingService", run_service("AIProcessingService", mvn_cmd, "AIProcessingService", "ai_processing.log")))
        
        # 5. Launch the React/Vite Frontend
        processes.append(("Frontend", run_service("Frontend", "npm run dev", "FilePortalFrontend", "frontend.log")))
        
        print("\n" + "="*60)
        print(" ALL SERVICES STARTED IN BACKGROUND")
        print("="*60)
        print(" Frontend App:      http://localhost:5173")
        print(" API Gateway:       http://localhost:8090")
        print(" Keycloak Admin:    http://localhost:9090")
        print(" MinIO Console:     http://localhost:9001")
        print(" Kafka UI:          http://localhost:8085")
        print("="*60)
        print("Logs are being written to the 'logs/' directory.")
        print("Press Ctrl+C to cleanly stop all background services.")
        
        # Keep the main Python thread alive so it can catch Ctrl+C
        while True:
            time.sleep(1)
            
    except KeyboardInterrupt:
        print("\n\nStopping all background services...")
        for name, p in processes:
            print(f"Terminating {name}...")
            # On Windows, we must use taskkill /T to kill the child processes (java.exe, node.exe)
            subprocess.run(f"taskkill /F /T /PID {p.pid}", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        
        print("All background services stopped.")
        print("Note: Docker containers are still running. Run 'docker compose down' to stop them if needed.")

if __name__ == "__main__":
    main()
