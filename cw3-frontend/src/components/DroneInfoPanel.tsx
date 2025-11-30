import React from 'react';
import { Drone } from '../types';

interface DroneInfoPanelProps {
  drones: Drone[];
  isLoading: boolean;
}

const DroneInfoPanel: React.FC<DroneInfoPanelProps> = ({ drones, isLoading }) => {
  if (isLoading) {
    return (
      <div className="bg-white p-4 rounded-lg shadow-lg border border-gray-200">
        <h3 className="text-lg font-bold mb-3 text-gray-800">Drone Details</h3>
        <div className="flex items-center justify-center py-4">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
          <span className="ml-2 text-gray-600">Loading drone info...</span>
        </div>
      </div>
    );
  }

  if (!drones || drones.length === 0) {
    return null;
  }

  return (
    <div className="bg-white p-4 rounded-lg shadow-lg border border-gray-200">
      <h3 className="text-lg font-bold mb-3 text-gray-800">
        🚁 Drones Used ({drones.length})
      </h3>
      
      <div className="space-y-3">
        {drones.map((drone) => (
          <div 
            key={drone.id} 
            className="bg-gradient-to-r from-blue-50 to-indigo-50 p-3 rounded-lg border border-blue-100"
          >
            <div className="flex justify-between items-start mb-2">
              <span className="font-semibold text-blue-800">
                Drone #{drone.id}
              </span>
              {drone.costPerMove && (
                <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                  £{drone.costPerMove.toFixed(3)}/move
                </span>
              )}
            </div>
            
            {drone.capability && (
              <div className="grid grid-cols-2 gap-2 text-sm">
                <div className="flex items-center gap-1">
                  <span className="text-gray-500">Capacity:</span>
                  <span className="font-medium text-gray-800">
                    {drone.capability.capacity}
                  </span>
                </div>
                
                <div className="flex items-center gap-1">
                  <span className="text-gray-500">Max Moves:</span>
                  <span className="font-medium text-gray-800">
                    {drone.capability.maxMoves || 'N/A'}
                  </span>
                </div>
                
                <div className="flex items-center gap-2">
                  {drone.capability.cooling && (
                    <span className="text-xs bg-cyan-100 text-cyan-700 px-2 py-0.5 rounded">
                      ❄️ Cooling
                    </span>
                  )}
                  {drone.capability.heating && (
                    <span className="text-xs bg-orange-100 text-orange-700 px-2 py-0.5 rounded">
                      🔥 Heating
                    </span>
                  )}
                  {!drone.capability.cooling && !drone.capability.heating && (
                    <span className="text-xs text-gray-400">No temp control</span>
                  )}
                </div>
              </div>
            )}
            
            {drone.availability && drone.availability.length > 0 && (
              <div className="mt-2 pt-2 border-t border-blue-100">
                <span className="text-xs text-gray-500">Available: </span>
                <span className="text-xs text-gray-700">
                  {drone.availability[0].days?.join(', ') || 'All days'} 
                  {drone.availability[0].startTime && drone.availability[0].endTime && (
                    <> ({drone.availability[0].startTime} - {drone.availability[0].endTime})</>
                  )}
                </span>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default DroneInfoPanel;

