# ILP CW3 - Flight Path Visualizer

A full-stack web application for visualizing drone delivery flight paths in Edinburgh. This project extends the ILP coursework by providing an interactive frontend for the drone delivery path calculation API.

## Project Structure

```
ilp_submission_3/
├── ilp_backend_image.tar      # Backend Docker image
├── ilp_frontend_image.tar     # Frontend Docker image
├── src/                       # Spring Boot backend source
├── cw3-frontend/              # React frontend source
├── Dockerfile                 # Backend Dockerfile
├── pom.xml                    # Maven configuration
└── cw3_explanation.pdf        # CW3 explanation document
```

## Prerequisites

- Docker Desktop installed and running
- Ports 8080 and 3000 (or 3001) available

## Quick Start (Using Pre-built Images)

### 1. Load the Docker Images

```bash
docker load -i ilp_backend_image.tar
docker load -i ilp_frontend_image.tar
```

### 2. Run the Backend

```bash
docker run -p 8080:8080 \
  -e ILP_ENDPOINT=https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net \
  --name ilp-backend \
  ilp-backend:latest
```

### 3. Run the Frontend

```bash
# Use port 3000 (default)
docker run -p 3000:80 \
  --name ilp-frontend \
  ilp-frontend:latest

# Or use port 3001 if 3000 is busy
docker run -p 3001:80 \
  --name ilp-frontend \
  ilp-frontend:latest
```

### 4. Access the Application

- **Frontend**: http://localhost:3000 (or http://localhost:3001)
- **Backend API**: http://localhost:8080/api/v1

## Building from Source

### Backend

```bash
# Build the Docker image
docker build -t ilp-backend:latest .

# Run the container
docker run -p 8080:8080 \
  -e ILP_ENDPOINT=https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net \
  --name ilp-backend \
  ilp-backend:latest
```

### Frontend

```bash
cd cw3-frontend

# Build the Docker image
docker build -t ilp-frontend:latest .

# Run the container
docker run -p 3000:80 \
  --name ilp-frontend \
  ilp-frontend:latest
```

## Stopping the Containers

```bash
docker stop ilp-backend ilp-frontend
docker rm ilp-backend ilp-frontend
```

## CW3 New Features

This coursework extends the ILP backend with a React frontend for visualizing drone delivery operations:

- **Interactive Map**: Visualize drone flight paths on an Edinburgh map using Leaflet
- **Delivery Planning**: Add multiple delivery locations with geocoding support (address autocomplete)
- **Drone Availability**: Real-time preview of available drones for delivery configurations
- **Route Comparison**: Compare single-drone vs multi-drone routing solutions with cost analysis
- **GeoJSON Visualization**: Display restricted areas (polygons), service points, delivery points, and flight paths
- **Flight Animation**: Animated playback of calculated flight paths
- **Cost Analysis**: View total costs, moves, drone count, and efficiency recommendations
- **Email Results**: Option to email delivery results using EmailJS integration

## CW3 New API Endpoint (New Frontend Visualization Features)
- `POST /api/v1/compareRoutes` - Compare single-drone vs multi-drone routing solutions with statistics, recommendations, and GeoJSON for both approaches

## Technology Stack

**Backend:**
- Java 21
- Spring Boot 3.4.3
- Maven

**Frontend:**
- React 18
- TypeScript
- Tailwind CSS
- Leaflet (maps)
- Axios (HTTP client)

**Infrastructure:**
- Docker
- Nginx (frontend serving & API proxy)

## Troubleshooting

### Port Already in Use

```bash
# Find and kill process using the port
lsof -ti :3000 | xargs kill -9
lsof -ti :8080 | xargs kill -9
```

### Container Already Exists

```bash
docker rm -f ilp-backend ilp-frontend
```

### CORS Issues

The backend allows requests from both http://localhost:3000 and http://localhost:3001.

## Author

Student ID: s2490039

