// Type definitions for the application

export interface LngLat {
  lng: number;
  lat: number;
}

export interface MedDispatchRequirements {
  capacity: number;
  cooling?: boolean;
  heating?: boolean;
  maxCost?: number;
}

export interface MedDispatchRec {
  id: number;
  date?: string;
  time?: string;
  requirements: MedDispatchRequirements;
  delivery: LngLat;
}

export interface DeliveryFormData {
  id: number;
  address: string;
  coordinates?: LngLat;
  capacity: number;
  cooling: boolean;
  heating: boolean;
  maxCost?: number;
  date?: string;
  time?: string;
  geocodingStatus: 'idle' | 'loading' | 'success' | 'error';
  geocodingError?: string;
}

export interface GeoJsonGeometry {
  type: string;
  coordinates: any;
}

export interface GeoJsonProperties {
  droneId?: string;
  deliveryIds?: number[];
  totalMoves?: number;
  totalCost?: number;
  name?: string;
  markerColor?: string;
  markerSize?: string;
  markerSymbol?: string;
  type?: string;
}

export interface GeoJsonFeature {
  type: string;
  geometry: GeoJsonGeometry;
  properties: GeoJsonProperties;
}

export interface GeoJsonFeatureCollection {
  type: string;
  features: GeoJsonFeature[];
}

export interface NominatimResponse {
  lat: string;
  lon: string;
  display_name: string;
}

// Drone types from CW2
export interface DroneCapability {
  capacity: number;
  cooling?: boolean;
  heating?: boolean;
  maxMoves?: number;
}

export interface DroneAvailability {
  days: string[];
  startTime: string;
  endTime: string;
}

export interface Drone {
  id: string;
  name?: string;
  capability?: DroneCapability;
  availability?: DroneAvailability[];
  costPerMove?: number;
}

// Route comparison types
export interface DeliveryPathResponse {
  totalCost: number;
  totalMoves: number;
  dronePaths: DronePath[];
}

export interface DronePath {
  droneId: string;
  deliveries: DeliveryPath[];
}

export interface DeliveryPath {
  deliveryId: number;
  flightPath: LngLat[];
}

export interface ComparisonStats {
  singleDronePossible: boolean;
  costDifferencePercent?: number;
  moveDifferencePercent?: number;
  recommendation: 'SINGLE_DRONE' | 'MULTI_DRONE' | 'NONE';
  reason: string;
  singleDroneCost?: number;
  multiDroneCost?: number;
  singleDroneMoves?: number;
  multiDroneMoves?: number;
  multiDroneCount?: number;
}

export interface RouteComparisonResponse {
  singleDroneSolution?: DeliveryPathResponse;
  multiDroneSolution?: DeliveryPathResponse;
  comparison: ComparisonStats;
  singleDroneGeoJson?: GeoJsonFeatureCollection;
  multiDroneGeoJson?: GeoJsonFeatureCollection;
}

