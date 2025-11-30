import React, { useEffect, useRef, useMemo } from 'react';
import { MapContainer, TileLayer, GeoJSON, useMap, Polyline, Marker } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { GeoJsonFeatureCollection, LngLat } from '../types';

// Fix for default marker icons in React-Leaflet
// Use CDN URLs to avoid TypeScript module resolution issues with image imports
const DefaultIcon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

L.Marker.prototype.options.icon = DefaultIcon;

interface FlightPathMapProps {
  geoJsonData: GeoJsonFeatureCollection | null;
  // Animation props
  dronePosition?: LngLat | null;
  traveledPath?: LngLat[];
  isAnimating?: boolean;
}

// Component to fit map bounds to features
const FitBounds: React.FC<{ geoJsonData: GeoJsonFeatureCollection | null }> = ({ geoJsonData }) => {
  const map = useMap();

  useEffect(() => {
    if (geoJsonData && geoJsonData.features && geoJsonData.features.length > 0) {
      const bounds = L.latLngBounds([]);
      let hasFlightPath = false;
      
      // ONLY focus on flight paths - detect by droneId property
      geoJsonData.features.forEach((feature) => {
        const props = feature.properties;
        const isFlightPath = props?.droneId !== undefined || props?.deliveryIds !== undefined;
        
        // Only include flight paths for zoom calculation
        if (isFlightPath && feature.geometry.type === 'LineString' && Array.isArray(feature.geometry.coordinates)) {
          hasFlightPath = true;
          feature.geometry.coordinates.forEach((coord: number[]) => {
            bounds.extend([coord[1], coord[0]]);
          });
        }
      });

      // Only zoom if we have flight paths
      if (hasFlightPath && bounds.isValid()) {
        // Use a longer timeout to ensure GeoJSON is fully rendered
        setTimeout(() => {
          map.fitBounds(bounds, { 
            padding: [50, 50], // Padding for better view
            maxZoom: 18 // Don't zoom in too much
          });
        }, 300);
      }
    }
  }, [geoJsonData, map]);

  return null;
};

// Create drone icon for animation
const droneIcon = L.divIcon({
  className: 'drone-marker',
  html: `<div style="
    font-size: 32px;
    text-shadow: 0 2px 4px rgba(0,0,0,0.5);
    animation: pulse 1s infinite;
  ">🚁</div>`,
  iconSize: [32, 32],
  iconAnchor: [16, 16],
});

const FlightPathMap: React.FC<FlightPathMapProps> = ({ 
  geoJsonData,
  dronePosition,
  traveledPath,
  isAnimating = false,
}) => {
  const geoJsonRef = useRef<L.GeoJSON>(null);
  
  // Convert traveled path to leaflet format
  const traveledPositions = useMemo(() => {
    if (!traveledPath || traveledPath.length === 0) return [];
    return traveledPath.map(p => [p.lat, p.lng] as [number, number]);
  }, [traveledPath]);

  const getStyle = (feature: any) => {
    const props = feature?.properties;
    const geometryType = feature?.geometry?.type;
    
    // Detect flight path by checking for droneId property (flight paths have droneId, others don't)
    const isFlightPath = props?.droneId !== undefined || (geometryType === 'LineString' && props?.deliveryIds !== undefined);
    
    if (isFlightPath) {
      return {
        color: '#00008B', // DARK BLUE
        weight: 15, // VERY THICK
        opacity: 1.0, // Fully opaque
        fillOpacity: 0,
      };
    } else if (geometryType === 'Polygon') {
      // Restricted areas are polygons - RED with shaded red fill
      return {
        color: '#FF0000', // RED border
        weight: 3,
        opacity: 1,
        fillColor: '#FF0000', // RED fill
        fillOpacity: 0.4, // Visible shading
      };
    } else {
      return {
        color: '#6b7280',
        weight: 1,
        opacity: 0.5,
      };
    }
  };

  const onEachFeature = (feature: any, layer: L.Layer) => {
    const props = feature.properties;
    let popupContent = '';

    // Detect flight path by droneId property
    const isFlightPath = props?.droneId !== undefined || props?.deliveryIds !== undefined;
    const geometryType = feature?.geometry?.type;
    
    // Apply style directly to the layer for flight paths to ensure it's visible
    if (isFlightPath && layer instanceof L.Polyline) {
      layer.setStyle({
        color: '#00008B', // DARK BLUE
        weight: 15, // VERY THICK
        opacity: 1.0,
        fillOpacity: 0,
        lineCap: 'round',
        lineJoin: 'round',
      });
    }
    
    // Apply style directly to polygons (restricted areas) - FORCE RED FILL
    if (geometryType === 'Polygon' && layer instanceof L.Polygon) {
      layer.setStyle({
        color: '#FF0000', // RED border
        weight: 3,
        opacity: 1,
        fillColor: '#FF0000', // RED fill
        fillOpacity: 0.4, // Visible shading
      });
    }

    if (isFlightPath) {
      popupContent = `
        <div style="font-family: Arial, sans-serif;">
          <h3 style="margin: 0 0 8px 0; font-size: 16px; font-weight: bold;">Flight Path</h3>
          <p style="margin: 4px 0;"><strong>Drone ID:</strong> ${props.droneId || 'N/A'}</p>
          <p style="margin: 4px 0;"><strong>Total Moves:</strong> ${props.totalMoves || 'N/A'}</p>
          <p style="margin: 4px 0;"><strong>Total Cost:</strong> ${props.totalCost ? props.totalCost.toFixed(2) : 'N/A'}</p>
          <p style="margin: 4px 0;"><strong>Delivery IDs:</strong> ${props.deliveryIds ? props.deliveryIds.join(', ') : 'N/A'}</p>
        </div>
      `;
    } else if (props.type === 'servicePoint') {
      popupContent = `
        <div style="font-family: Arial, sans-serif;">
          <h3 style="margin: 0 0 8px 0; font-size: 16px; font-weight: bold;">Service Point</h3>
          <p style="margin: 4px 0;"><strong>Name:</strong> ${props.name || 'N/A'}</p>
        </div>
      `;
    } else if (props.type === 'deliveryPoint') {
      popupContent = `
        <div style="font-family: Arial, sans-serif;">
          <h3 style="margin: 0 0 8px 0; font-size: 16px; font-weight: bold;">Delivery Point</h3>
          <p style="margin: 4px 0;"><strong>Name:</strong> ${props.name || 'N/A'}</p>
        </div>
      `;
    } else if (props.type === 'restrictedArea' || !props.type) {
      popupContent = `
        <div style="font-family: Arial, sans-serif;">
          <h3 style="margin: 0 0 8px 0; font-size: 16px; font-weight: bold;">Restricted Area</h3>
          <p style="margin: 4px 0;"><strong>Name:</strong> ${props.name || 'No-Fly Zone'}</p>
        </div>
      `;
    }

    if (popupContent) {
      layer.bindPopup(popupContent);
    }

    // Add custom markers for points
    if (feature.geometry.type === 'Point') {
      const coordinates = feature.geometry.coordinates;
      const latlng = [coordinates[1], coordinates[0]] as [number, number];
      
      // Create custom icon based on type
      let markerIcon: L.Icon | L.DivIcon;
      if (props.type === 'servicePoint') {
        markerIcon = L.divIcon({
          className: 'custom-marker',
          html: `<div style="background-color: #0066FF; width: 20px; height: 20px; border-radius: 50%; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"></div>`,
          iconSize: [20, 20],
          iconAnchor: [10, 10],
        });
      } else if (props.type === 'deliveryPoint') {
        markerIcon = L.divIcon({
          className: 'custom-marker',
          html: `<div style="color: #FF0000; font-size: 24px; text-shadow: 0 0 3px white;">★</div>`,
          iconSize: [24, 24],
          iconAnchor: [12, 12],
        });
      } else {
        markerIcon = DefaultIcon;
      }

      // Remove existing layer and add new marker
      if (layer instanceof L.Marker) {
        layer.setIcon(markerIcon);
      }
    }
  };

  const pointToLayer = (feature: any, latlng: L.LatLng) => {
    const props = feature.properties;
    
    if (props.type === 'servicePoint') {
      return L.marker(latlng, {
        icon: L.divIcon({
          className: 'custom-marker',
          html: `<div style="background-color: #0066FF; width: 20px; height: 20px; border-radius: 50%; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"></div>`,
          iconSize: [20, 20],
          iconAnchor: [10, 10],
        }),
      });
    } else if (props.type === 'deliveryPoint') {
      return L.marker(latlng, {
        icon: L.divIcon({
          className: 'custom-marker',
          html: `<div style="color: #FF0000; font-size: 24px; text-shadow: 0 0 3px white;">★</div>`,
          iconSize: [24, 24],
          iconAnchor: [12, 12],
        }),
      });
    }
    
    return L.marker(latlng);
  };

  return (
    <div className="w-full h-full relative">
      <MapContainer
        center={[55.9533, -3.1883]} // Edinburgh default
        zoom={13}
        style={{ height: '100%', width: '100%' }}
        scrollWheelZoom={true}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        
        <FitBounds geoJsonData={geoJsonData} />
        
        {geoJsonData && geoJsonData.features && geoJsonData.features.length > 0 && (
          <GeoJSON
            key={JSON.stringify(geoJsonData)} // Force re-render when data changes
            ref={geoJsonRef}
            data={geoJsonData as any}
            style={getStyle}
            onEachFeature={onEachFeature}
            pointToLayer={pointToLayer}
          />
        )}

        {/* Animated traveled path */}
        {isAnimating && traveledPositions.length > 1 && (
          <Polyline
            positions={traveledPositions}
            pathOptions={{
              color: '#22c55e', // Green for traveled path
              weight: 8,
              opacity: 0.8,
              dashArray: undefined,
            }}
          />
        )}

        {/* Animated drone marker */}
        {isAnimating && dronePosition && (
          <Marker
            position={[dronePosition.lat, dronePosition.lng]}
            icon={droneIcon}
          />
        )}

        {/* Legend */}
        <div className="absolute bottom-4 right-4 bg-white p-4 rounded-lg shadow-lg z-[1000] border border-gray-300">
          <h4 className="font-bold text-sm mb-2 text-gray-800">Legend</h4>
          <div className="space-y-2 text-xs">
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-blue-500 rounded-full border border-white"></div>
              <span>Service Point</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-red-500 text-lg">★</span>
              <span>Delivery Point</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-8 h-3 bg-blue-800"></div>
              <span>Flight Path</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-red-500 opacity-30 border border-red-500"></div>
              <span>Restricted Area</span>
            </div>
          </div>
        </div>
      </MapContainer>
    </div>
  );
};

export default FlightPathMap;

