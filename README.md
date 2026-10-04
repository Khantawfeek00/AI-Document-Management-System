[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/y1GmoBve)

# A Microservices-based file management system
## Lab05 ?" Event-Driven Document Enrichment and AI Integration

### Group 07 - Members
- Emiliana Marziano - s337448
- Alessia Priotti - s343737
- Lorenzo Ronco - s343512
- Omar Simone - s343432

### Project Overview
This project implements a microservices-based file management system that allows users to upload, store, and manage files. 
The system is designed to be scalable, resilient, and easy to maintain by leveraging microservices architecture principles.
The main improvement introduced by this lab is the integration of a more intelligent platform, capable of enriching documents using AI services. 
A new microservice called AIProcessingService has been added to the existing architecture.
This introduces asynchronous processing, communication with LLMs, machine-to-machine communications.
Another element introduced is the use of an event-driven architecture, which allows services to communicate through events, improving decoupling and scalability.
This is achieved using a message broker (Kafka) to handle event publishing and subscribing.

### Project Structure
The project is organized into several microservices, each responsible for a specific functionality:
- **UserDetailService**: 
  - It manages user authentication and authorization details. 
  - It handles user registration, login, and profile management. 
  - Only authenticated users can access the file management features. 
  - Only authorized users can perform specific actions based on their roles. 
  - Users can be managed only by admin users.
  - It laverages JWT tokens for secure communication between services.
  - It uses a relational database (PostgreSQL) to store user information securely.
  - It exposes RESTful APIs for user management operations.
  - It integrates with the API Gateway for routing requests.
  - It uses Keycloak as an external identity provider for enhanced security and user management capabilities.
- **FileService**: 
  - It handles file uploads, downloads, and storage.
  - It provides APIs for file management operations.
  - It stores metadata in a database (PostgreSQL) and files in a file storage system (MinIO).
  - It communicates with other services via the message broker (Kafka) for event-driven processing.
  - It integrates with the API Gateway for routing requests.
  - It implements security measures to ensure only authorized users can access files, by enforcing RBAC and ACL rules.
  - It laverages JWT tokens for secure communication between services.
- **AIProcessingService**: 
  - New service that processes files using AI to extract insights, summarize content, and generate tags.
  - It listens for file upload events from the FileService via Kafka.
  - It communicates with external AI services to perform document enrichment.
  - It updates the FileService with enriched data once processing is complete.
- **API Gateway**: 
  - It acts as a single entry point for all client requests, routing them to the appropriate microservices.
  - It handles request authentication and authorization using JWT tokens.
- **Message Broker (Kafka)**: 
  - It facilitates event-driven communication between microservices.
  - It allows services to publish and subscribe to events asynchronously.
  - It improves decoupling and scalability of the system.
  - It ensures reliable message delivery and processing.
- **Database**: 
  - The UserDetailService and the FileService microservices have their own database to ensure data isolation and independence.
  - UserDetailService uses PostgreSQL to store user information.
  - FileService uses PostgreSQL to store file metadata.
  - MinIO is used for file storage.
  - Each database is managed independently by its respective microservice.
- **Frontend**: 
  - A web-based user interface for interacting with the file management system.
  - It allows users to upload, download, and manage files.
  - It communicates with the API Gateway to access backend services.
  - It provides a user-friendly experience for file management operations.
  - It implements authentication and authorization flows to ensure secure access to user data.
  - It displays enriched document information provided by the AIProcessingService.
  - It is built using modern web technologies (React + Vite).
  - It integrates with Keycloak for user authentication and management.
- **Docker Compose**: 
  - It is used to orchestrate the deployment of all microservices and their dependencies.
  - It simplifies the setup and management of the development and testing environment.
  - It ensures consistent configurations across different environments.
  - It contains services for Kafka, PostgreSQL, MinIO, and Keycloak.

### Event-Driven Architecture

### AI Integration

### Testing strategy
To ensure the reliability and correctness of the microservices-based file management system, a comprehensive testing strategy has been implemented. 
This includes unit tests, integration tests, and end-to-end tests.
Using mocks and stubs, we simulate interactions between microservices to verify their behavior in isolation.
Kafka topics are tested to ensure proper event publishing and subscribing.
The FileService and AIProcessingService communicate through JWT tokens, which are also validated during testing.

### Frontend features

### Limitations and Future Work
