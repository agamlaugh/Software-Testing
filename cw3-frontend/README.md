# ILP Flight Path Visualizer

A React-based web application for visualizing optimal drone delivery flight paths. This application allows users to input delivery addresses, convert them to coordinates using OpenStreetMap Nominatim, calculate optimal routes via the backend API, and display results on an interactive Leaflet map.

## Features

- **Address Geocoding**: Convert addresses to coordinates using OpenStreetMap Nominatim API
- **Delivery Configuration**: Configure multiple deliveries with capacity, cooling, heating, max cost, date, and time requirements
- **Path Visualization**: Interactive Leaflet map displaying:
  - Flight paths (blue lines)
  - Service points (blue circles)
  - Delivery points (red stars)
  - Restricted areas (red polygons)
- **Results Summary**: Display total cost, total moves, drone count, and delivery information

## Prerequisites

- Node.js (v14 or higher)
- npm or yarn
- Backend API running on `http://localhost:8080` (or configure via environment variable)

## Installation

1. Navigate to the frontend directory:
```bash
cd cw3-frontend
```

2. Install dependencies:
```bash
npm install
```

## Configuration

The application connects to the backend API at `http://localhost:8080/api/v1` by default.

To change the API URL, create a `.env` file in the `cw3-frontend` directory:

```
REACT_APP_API_URL=http://your-backend-url:port/api/v1
```

## Running the Application

Start the development server:

```bash
npm start
```

The application will open in your browser at `http://localhost:3000`.

## Building for Production

Build the production bundle:

```bash
npm run build
```

The optimized build will be in the `build` directory.

## Usage

1. **Add Deliveries**: Click "Add Delivery" to add multiple delivery locations
2. **Enter Address**: Type an address in the address field (e.g., "Edinburgh, UK")
3. **Geocode**: Click "Find" or blur the address field to geocode the address
4. **Configure Requirements**: Set capacity (required), cooling, heating, max cost, date, and time
5. **Calculate Path**: Click "Calculate Path" to send requests to the backend API
6. **View Results**: The map will display the optimal flight paths, and the results panel will show summary information

## Project Structure

```
cw3-frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── DeliveryForm.tsx      # Form for inputting deliveries
│   │   ├── FlightPathMap.tsx     # Leaflet map component
│   │   └── ResultsPanel.tsx       # Results summary display
│   ├── services/
│   │   ├── apiService.ts         # Backend API client
│   │   └── geocodingService.ts   # Nominatim geocoding service
│   ├── types.ts                  # TypeScript type definitions
│   ├── App.tsx                   # Main application component
│   ├── index.tsx                 # Application entry point
│   └── index.css                 # Global styles
├── package.json
├── tsconfig.json
├── tailwind.config.js
└── postcss.config.js
```

## Technologies Used

- **React 18**: UI framework
- **TypeScript**: Type safety
- **Leaflet & React-Leaflet**: Interactive maps
- **Tailwind CSS**: Styling
- **Axios**: HTTP client
- **OpenStreetMap Nominatim**: Geocoding service

## Notes

- The geocoding service uses OpenStreetMap Nominatim, which has usage limits. Results are cached to minimize API calls.
- Ensure the backend API is running and accessible before calculating paths.
- The application requires all addresses to be geocoded before calculating paths.

## Troubleshooting

**Map not displaying**: Ensure Leaflet CSS is imported. Check browser console for errors.

**Geocoding fails**: Verify the address is valid and check Nominatim API status.

**API connection fails**: Ensure the backend is running on the configured port and CORS is properly configured.

**Build errors**: Clear `node_modules` and reinstall dependencies:
```bash
rm -rf node_modules package-lock.json
npm install
```

