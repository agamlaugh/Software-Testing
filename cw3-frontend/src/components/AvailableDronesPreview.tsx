import React, { useEffect, useState } from 'react';
import { DeliveryFormData, MedDispatchRec, Drone } from '../types';
import { queryAvailableDrones, getMultipleDroneDetails } from '../services/apiService';

interface AvailableDronesPreviewProps {
  deliveries: DeliveryFormData[];
}

const AvailableDronesPreview: React.FC<AvailableDronesPreviewProps> = ({ deliveries }) => {
  const [availableDroneIds, setAvailableDroneIds] = useState<string[]>([]);
  const [drones, setDrones] = useState<Drone[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Check if all deliveries are geocoded
  const allGeocoded = deliveries.length > 0 && deliveries.every(d => d.coordinates);

  useEffect(() => {
    const fetchAvailableDrones = async () => {
      if (!allGeocoded) {
        setAvailableDroneIds([]);
        setDrones([]);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        // Query each delivery individually to find all drones that can handle at least one
        const allDroneIds = new Set<string>();
        
        for (const delivery of deliveries) {
          const dispatch: MedDispatchRec = {
            id: delivery.id,
            requirements: {
              capacity: delivery.capacity,
              cooling: delivery.cooling || undefined,
              heating: delivery.heating || undefined,
              maxCost: delivery.maxCost,
            },
            delivery: delivery.coordinates!,
            date: delivery.date,
            time: delivery.time,
          };

          try {
            const droneIds = await queryAvailableDrones([dispatch]);
            droneIds.forEach(id => allDroneIds.add(id));
          } catch (err) {
            console.warn(`Failed to query drones for delivery ${delivery.id}`, err);
          }
        }

        const uniqueDroneIds = Array.from(allDroneIds);
        setAvailableDroneIds(uniqueDroneIds);

        // Fetch details for available drones (limit to first 5 to avoid too many requests)
        if (uniqueDroneIds.length > 0) {
          const limitedIds = uniqueDroneIds.slice(0, 5);
          const droneDetails = await getMultipleDroneDetails(limitedIds);
          setDrones(droneDetails);
        } else {
          setDrones([]);
        }
      } catch (err) {
        setError('Failed to check available drones');
        console.error(err);
      } finally {
        setIsLoading(false);
      }
    };

    // Debounce the API call
    const timeoutId = setTimeout(fetchAvailableDrones, 500);
    return () => clearTimeout(timeoutId);
  }, [deliveries, allGeocoded]);

  if (!allGeocoded) {
    return null;
  }

  return (
    <div className="border border-gray-300 rounded-xl p-6 bg-gradient-to-r from-green-50 to-emerald-50 mb-8">
      <h3 className="text-3xl font-semibold text-gray-700 mb-4 flex items-center gap-3">
        🚁 Available Drones
        {isLoading && (
          <span className="animate-spin rounded-full h-7 w-7 border-b-2 border-green-600"></span>
        )}
      </h3>

      {error && (
        <p className="text-xl text-red-600">{error}</p>
      )}

      {!isLoading && !error && (
        <>
          {availableDroneIds.length === 0 ? (
            <p className="text-xl text-amber-600">
              ⚠️ No drones available for this delivery configuration
            </p>
          ) : (
            <div className="space-y-4">
              <p className="text-xl text-green-700">
                ✓ {availableDroneIds.length} drone{availableDroneIds.length !== 1 ? 's' : ''} available for {deliveries.length === 1 ? 'this delivery' : 'these deliveries'}
              </p>
              
              <div className="flex flex-wrap gap-4">
                {drones.map((drone) => (
                  <div 
                    key={drone.id}
                    className="bg-white px-5 py-4 rounded-xl border border-green-200 shadow-sm"
                  >
                    <div className="font-semibold text-gray-800 text-2xl">#{drone.id}</div>
                    {drone.capability && (
                      <div className="text-lg text-gray-500 flex gap-2 mt-2">
                        <span>Cap: {drone.capability.capacity}</span>
                        {drone.capability.cooling && <span>❄️</span>}
                        {drone.capability.heating && <span>🔥</span>}
                      </div>
                    )}
                  </div>
                ))}
                {availableDroneIds.length > 5 && (
                  <div className="bg-gray-100 px-5 py-4 rounded-xl text-xl text-gray-600">
                    +{availableDroneIds.length - 5} more
                  </div>
                )}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default AvailableDronesPreview;

