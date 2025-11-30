import axios from 'axios';
import { LngLat, NominatimResponse } from '../types';

// Cache to avoid duplicate API calls
const geocodingCache = new Map<string, LngLat>();
const suggestionsCache = new Map<string, AddressSuggestion[]>();

export interface AddressSuggestion {
  displayName: string;
  coordinates: LngLat;
  type: string;
  importance: number;
}

/**
 * Search for address suggestions using OpenStreetMap Nominatim API
 * Returns multiple results for autocomplete
 */
export async function searchAddresses(query: string): Promise<AddressSuggestion[]> {
  if (!query || query.trim().length < 2) {
    return [];
  }

  const cacheKey = query.toLowerCase().trim();
  const cached = suggestionsCache.get(cacheKey);
  if (cached) {
    return cached;
  }

  try {
    const url = 'https://nominatim.openstreetmap.org/search';
    const params = {
      q: query,
      format: 'json',
      limit: 6,
      addressdetails: 1,
      // Bias towards UK/Edinburgh area
      viewbox: '-4.5,54.5,-2.0,56.5',
      bounded: 0,
    };

    const response = await axios.get<any[]>(url, {
      params,
      headers: {
        'User-Agent': 'ILP-CW3-FlightPathVisualizer/1.0',
      },
    });

    if (response.data && response.data.length > 0) {
      const suggestions: AddressSuggestion[] = response.data.map((result) => ({
        displayName: result.display_name,
        coordinates: {
          lng: parseFloat(result.lon),
          lat: parseFloat(result.lat),
        },
        type: result.type || 'place',
        importance: result.importance || 0,
      }));

      // Cache for 5 minutes
      suggestionsCache.set(cacheKey, suggestions);
      setTimeout(() => suggestionsCache.delete(cacheKey), 5 * 60 * 1000);

      return suggestions;
    }
    return [];
  } catch (error) {
    console.error('Address search failed:', error);
    return [];
  }
}

/**
 * Geocode an address using OpenStreetMap Nominatim API
 * @param address The address string to geocode
 * @returns Promise with coordinates {lng, lat}
 */
export async function geocodeAddress(address: string): Promise<LngLat> {
  // Check cache first
  const cached = geocodingCache.get(address.toLowerCase().trim());
  if (cached) {
    return cached;
  }

  try {
    // Nominatim API endpoint
    const url = 'https://nominatim.openstreetmap.org/search';
    const params = {
      q: address,
      format: 'json',
      limit: 1,
      addressdetails: 0,
    };

    // Add User-Agent header (required by Nominatim)
    const response = await axios.get<NominatimResponse[]>(url, {
      params,
      headers: {
        'User-Agent': 'ILP-CW3-FlightPathVisualizer/1.0',
      },
    });

    if (response.data && response.data.length > 0) {
      const result = response.data[0];
      const coordinates: LngLat = {
        lng: parseFloat(result.lon),
        lat: parseFloat(result.lat),
      };

      // Cache the result
      geocodingCache.set(address.toLowerCase().trim(), coordinates);
      return coordinates;
    } else {
      throw new Error('No results found for this address');
    }
  } catch (error) {
    if (axios.isAxiosError(error)) {
      throw new Error(`Geocoding failed: ${error.message}`);
    }
    throw error;
  }
}

/**
 * Clear the geocoding cache
 */
export function clearGeocodingCache(): void {
  geocodingCache.clear();
}

