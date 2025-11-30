import React from 'react';
import { GeoJsonFeatureCollection } from '../types';

interface ResultsPanelProps {
  geoJsonData: GeoJsonFeatureCollection | null;
  isLoading: boolean;
  error: string | null;
  deliveryCount: number;
  actualDroneCount?: number;
  // Direct stats from comparison (takes priority over GeoJSON extraction)
  overrideCost?: number;
  overrideMoves?: number;
}

const ResultsPanel: React.FC<ResultsPanelProps> = ({
  geoJsonData,
  isLoading,
  error,
  deliveryCount,
  actualDroneCount,
  overrideCost,
  overrideMoves,
}) => {
  if (isLoading) {
    return (
      <div className="bg-white p-4 rounded-lg shadow-lg">
        <div className="flex items-center justify-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <span className="ml-3 text-gray-700">Calculating optimal path...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white p-4 rounded-lg shadow-lg">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <h3 className="text-red-800 font-semibold mb-2">Error</h3>
          <p className="text-red-600 text-sm">{error}</p>
        </div>
      </div>
    );
  }

  if (!geoJsonData || !geoJsonData.features || geoJsonData.features.length === 0) {
    return (
      <div className="bg-white p-4 rounded-lg shadow-lg">
        <div className="text-center py-8 text-gray-500">
          <p>No path calculated yet.</p>
          <p className="text-sm mt-2">Add deliveries and click "Calculate Path" to see results.</p>
        </div>
      </div>
    );
  }

  // Use override stats from comparison data if available, otherwise extract from GeoJSON
  let totalCost = overrideCost;
  let totalMoves = overrideMoves;
  let droneCount = actualDroneCount || 0;
  
  // Fall back to GeoJSON extraction if no override provided
  if (totalCost === undefined || totalMoves === undefined) {
    const flightPathFeatures = geoJsonData.features.filter(
      f => f.properties?.droneId !== undefined
    );
    
    if (totalCost === undefined) {
      totalCost = flightPathFeatures.reduce((sum, f) => {
        return sum + (Number(f.properties?.totalCost) || 0);
      }, 0);
    }

    if (totalMoves === undefined) {
      totalMoves = flightPathFeatures.reduce((sum, f) => {
        return sum + (Number(f.properties?.totalMoves) || 0);
      }, 0);
    }

    if (droneCount === 0) {
      const droneIds = new Set(
        flightPathFeatures
          .map(f => f.properties?.droneId)
          .filter(id => id !== undefined)
      );
      droneCount = droneIds.size;
    }
  }

  return (
    <div className="bg-white p-4 rounded-lg shadow-lg">
      <h3 className="text-xl font-bold mb-4 text-gray-800">Path Summary</h3>
      
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-blue-50 p-3 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Total Cost</p>
          <p className="text-2xl font-bold text-blue-700">£{totalCost.toFixed(2)}</p>
        </div>
        
        <div className="bg-green-50 p-3 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Total Moves</p>
          <p className="text-2xl font-bold text-green-700">{totalMoves.toLocaleString()}</p>
        </div>
        
        <div className="bg-purple-50 p-3 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Drones Used</p>
          <p className="text-2xl font-bold text-purple-700">{droneCount}</p>
        </div>
        
        <div className="bg-orange-50 p-3 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Deliveries</p>
          <p className="text-2xl font-bold text-orange-700">{deliveryCount}</p>
        </div>
      </div>

    </div>
  );
};

export default ResultsPanel;

