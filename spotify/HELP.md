# Spotify Track Service

A robust RESTful service that fetches music track metadata and cover art from the Spotify API using an ISRC, then stores and serves the data. The service is built with a focus on resilience, performance, and scalability.

---
## Overview

This application provides a simple API to enrich a track's ISRC (International Standard Recording Code) with comprehensive metadata. It communicates with the official Spotify API for data retrieval and is designed with a clean, decoupled architecture that allows for easy extension and deployment in a production environment.

### Key Features
* **Create Track by ISRC:** Fetches and stores a track's metadata and cover image.
* **Get Track Metadata:** Retrieves the stored metadata for a given track.
* **Download Cover Image:** Serves the stored cover image for a track.
* **Resilient API Client:** Features automatic, thread-safe token refreshing, caching for performance, and retries for transient network errors.
* **Scalable by Design:** Built with service interfaces (`StorageService`) that allow for seamless integration with cloud services like AWS S3.

---
## Architecture & Tech Stack

* **Framework:** Spring Boot 3.x
* **Language:** Java 17+
* **API Documentation:** SpringDoc OpenAPI (Swagger UI)
* **Database (Current):** In-Memory (H2)
* **File Storage (Current):** Local File System
* **Build Tool:** Maven

---
## Getting Started

Follow these steps to get the application running on your local machine.

### Prerequisites
* Java JDK 17 or later
* Apache Maven
* A **Spotify Developer Account** to get API credentials. You can create one [here](https://developer.spotify.com/dashboard/).

### Setup & Configuration
1.  **Unzip the project**
    ```bash
    unzip <your-repository-path>
    ```

2.  **Get Spotify Credentials**
    * Go to your Spotify Developer Dashboard and create a new application.
    * Note down the **Client ID** and **Client Secret**.

3.  **Update Configuration**
    * Open the `src/main/resources/application.properties` file.
    * Update the `spotify.api.client-id` and `spotify.api.client-secret` with your credentials.

    ```properties
    # src/main/resources/application.properties

    #== Spotify API Configuration ==#
    # Replace these with your actual credentials from the Spotify Developer Dashboard
    spotify.api.client-id=YOUR_CLIENT_ID_HERE
    spotify.api.client-secret=YOUR_CLIENT_SECRET_HERE

    #== Application Configuration ==#
    # Defines the base directory for storing cover images locally
    storage.location=./cover-images
    ```

4.  **Build the Project**
    ```bash
    mvn clean install
    ```

5.  **Run the Application**
    ```bash
    mvn spring-boot:run
    ```
    The application will start on `http://localhost:8080`.

---
## API Documentation

The API is fully documented using Swagger UI. Once the application is running, you can access the interactive API documentation at:

**[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

From this interface, you can view all available endpoints, see their request/response models, and execute API calls directly from your browser.

---
## Architectural Design for Scalability

This project was intentionally designed to be easily scalable for a production environment.

### Storage Abstraction
The application uses a `StorageService` interface for all file operations. The default implementation, `LocalFileStorageService`, saves images to the local disk. This design makes it trivial to switch to a cloud-based storage solution.

**To migrate to AWS S3:**
1.  Create a new class `S3StorageService` that implements the `StorageService` interface.
2.  Implement the `storeFile` and `loadFileAsResource` methods using the AWS S3 SDK.
3.  Use a Spring Profile (`@Profile("prod")`) to activate this implementation in your production environment. No other code changes would be required.

### Database Migration
The application currently uses an in-memory H2 database for simplicity. Because it uses Spring Data JPA, migrating to a persistent relational database is straightforward.

**To migrate to PostgreSQL:**
1.  Add the PostgreSQL JDBC driver dependency to your `pom.xml`.
2.  Update the `application.properties` file with the PostgreSQL connection URL, username, and password. Spring Boot will automatically detect and configure the new data source.