import axios from 'axios';
import { MedDispatchRec, GeoJsonFeatureCollection, Drone, RouteComparisonResponse } from '../types';

// Use proxy if available (for Create React App), otherwise use full URL
const API_BASE_URL = process.env.REACT_APP_API_URL || '/api/v1';

/**
 * Calculate delivery paths and return as GeoJSON
 * @param dispatches Array of medical dispatch records
 * @returns Promise with GeoJSON FeatureCollection
 */
export async function calculateDeliveryPathAsGeoJson(
  dispatches: MedDispatchRec[]
): Promise<GeoJsonFeatureCollection> {
  try {
    const response = await axios.post<GeoJsonFeatureCollection>(
      `${API_BASE_URL}/calcDeliveryPathAsGeoJson`,
      dispatches,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      if (error.response) {
        throw new Error(`API Error: ${error.response.status} - ${error.response.statusText}`);
      } else if (error.request) {
        throw new Error('No response from server. Is the backend running?');
      } else {
        throw new Error(`Request error: ${error.message}`);
      }
    }
    throw error;
  }
}

/**
 * Query available drones that can handle the given dispatches
 * @param dispatches Array of medical dispatch records
 * @returns Promise with array of drone IDs
 */
export async function queryAvailableDrones(
  dispatches: MedDispatchRec[]
): Promise<string[]> {
  try {
    const response = await axios.post<string[]>(
      `${API_BASE_URL}/queryAvailableDrones`,
      dispatches,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    return response.data;
  } catch (error) {
    console.error('Error querying available drones:', error);
    return [];
  }
}

/**
 * Get details of a specific drone by ID
 * @param droneId The drone ID
 * @returns Promise with Drone object or null if not found
 */
export async function getDroneDetails(droneId: string): Promise<Drone | null> {
  try {
    const response = await axios.get<Drone>(
      `${API_BASE_URL}/droneDetails/${droneId}`,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    return response.data;
  } catch (error) {
    console.error('Error fetching drone details:', error);
    return null;
  }
}

/**
 * Get multiple drone details at once
 * @param droneIds Array of drone IDs
 * @returns Promise with array of Drone objects
 */
export async function getMultipleDroneDetails(droneIds: string[]): Promise<Drone[]> {
  const dronePromises = droneIds.map(id => getDroneDetails(id));
  const drones = await Promise.all(dronePromises);
  return drones.filter((d): d is Drone => d !== null);
}

/**
 * Compare single-drone vs multi-drone solutions
 * @param dispatches Array of medical dispatch records
 * @returns Promise with RouteComparisonResponse containing both solutions and stats
 */
export async function compareRoutes(
  dispatches: MedDispatchRec[]
): Promise<RouteComparisonResponse> {
  try {
    const response = await axios.post<RouteComparisonResponse>(
      `${API_BASE_URL}/compareRoutes`,
      dispatches,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      if (error.response) {
        throw new Error(`API Error: ${error.response.status} - ${error.response.statusText}`);
      } else if (error.request) {
        throw new Error('No response from server. Is the backend running?');
      } else {
        throw new Error(`Request error: ${error.message}`);
      }
    }
    throw error;
  }
}

