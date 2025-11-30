import React, { useState, useCallback, useEffect, useRef } from 'react';
import DeliveryForm from './components/DeliveryForm';
import FlightPathMap from './components/FlightPathMap';
import ResultsPanel from './components/ResultsPanel';
import DroneInfoPanel from './components/DroneInfoPanel';
import FlightAnimationControls from './components/FlightAnimationControls';
import RouteComparisonPanel from './components/RouteComparisonPanel';
import { DeliveryFormData, MedDispatchRec, GeoJsonFeatureCollection, Drone, LngLat, RouteComparisonResponse } from './types';
import { calculateDeliveryPathAsGeoJson, getMultipleDroneDetails, compareRoutes } from './services/apiService';
import { sendFlightStatsEmail, FlightStats, isValidEmail } from './services/emailService';

function App() {
  const [deliveries, setDeliveries] = useState<DeliveryFormData[]>([
    {
      id: 1,
      address: '',
      capacity: 0.5,
      cooling: false,
      heating: false,
      geocodingStatus: 'idle',
    },
  ]);
  const [geoJsonData, setGeoJsonData] = useState<GeoJsonFeatureCollection | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [email, setEmail] = useState('');
  const [sendEmail, setSendEmail] = useState(false);
  const [emailStatus, setEmailStatus] = useState<{ type: 'success' | 'error' | null; message: string }>({ type: null, message: '' });
  const [usedDrones, setUsedDrones] = useState<Drone[]>([]);
  const [dronesLoading, setDronesLoading] = useState(false);

  // Animation state
  const [isAnimating, setIsAnimating] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [animationSpeed, setAnimationSpeed] = useState(1);
  const [currentMoveIndex, setCurrentMoveIndex] = useState(0);
  const [flightPath, setFlightPath] = useState<LngLat[]>([]);
  const [dronePosition, setDronePosition] = useState<LngLat | null>(null);
  const [traveledPath, setTraveledPath] = useState<LngLat[]>([]);
  const animationRef = useRef<number | null>(null);

  // Route comparison state
  const [comparisonData, setComparisonData] = useState<RouteComparisonResponse | null>(null);
  const [comparisonLoading, setComparisonLoading] = useState(false);
  const [selectedRoute, setSelectedRoute] = useState<'single' | 'multi'>('single');

  // Extract flight path coordinates from GeoJSON
  const extractFlightPath = useCallback((geoJson: GeoJsonFeatureCollection): LngLat[] => {
    const path: LngLat[] = [];
    geoJson.features.forEach((feature) => {
      const props = feature.properties as any;
      if (props?.droneId !== undefined && feature.geometry.type === 'LineString') {
        const coords = feature.geometry.coordinates as number[][];
        coords.forEach((coord) => {
          path.push({ lng: coord[0], lat: coord[1] });
        });
      }
    });
    return path;
  }, []);

  // Animation effect
  useEffect(() => {
    if (isPlaying && flightPath.length > 0 && currentMoveIndex < flightPath.length) {
      const interval = 100 / animationSpeed; // Base interval adjusted by speed
      
      animationRef.current = window.setTimeout(() => {
        setCurrentMoveIndex((prev) => {
          const next = prev + 1;
          if (next >= flightPath.length) {
            setIsPlaying(false);
            return prev;
          }
          return next;
        });
      }, interval);

      return () => {
        if (animationRef.current) {
          clearTimeout(animationRef.current);
        }
      };
    }
  }, [isPlaying, currentMoveIndex, flightPath.length, animationSpeed]);

  // Update drone position and traveled path when move index changes
  useEffect(() => {
    if (flightPath.length > 0 && currentMoveIndex < flightPath.length) {
      setDronePosition(flightPath[currentMoveIndex]);
      setTraveledPath(flightPath.slice(0, currentMoveIndex + 1));
    }
  }, [currentMoveIndex, flightPath]);

  // Initialize animation when GeoJSON data changes
  useEffect(() => {
    if (geoJsonData && geoJsonData.features.length > 0) {
      const path = extractFlightPath(geoJsonData);
      setFlightPath(path);
      if (path.length > 0) {
        setCurrentMoveIndex(0);
        setDronePosition(path[0]);
        setTraveledPath([path[0]]);
        setIsAnimating(true);
      }
    } else {
      setFlightPath([]);
      setIsAnimating(false);
      setDronePosition(null);
      setTraveledPath([]);
    }
  }, [geoJsonData, extractFlightPath]);

  const handlePlayPause = () => {
    if (currentMoveIndex >= flightPath.length - 1) {
      // Reset if at end
      setCurrentMoveIndex(0);
    }
    setIsPlaying(!isPlaying);
  };

  const handleReset = () => {
    setIsPlaying(false);
    setCurrentMoveIndex(0);
    if (flightPath.length > 0) {
      setDronePosition(flightPath[0]);
      setTraveledPath([flightPath[0]]);
    }
  };

  const handleStepForward = () => {
    if (currentMoveIndex < flightPath.length - 1) {
      setCurrentMoveIndex((prev) => prev + 1);
    }
  };

  const handleStepBackward = () => {
    if (currentMoveIndex > 0) {
      setCurrentMoveIndex((prev) => prev - 1);
    }
  };

  const handleSeek = (progress: number) => {
    const index = Math.round((progress / 100) * (flightPath.length - 1));
    setCurrentMoveIndex(index);
  };

  const extractFlightStats = (geoJson: GeoJsonFeatureCollection): FlightStats => {
    let totalMoves = 0;
    let totalCost = 0;
    const droneIds: number[] = [];
    const deliveryIds: number[] = [];

    geoJson.features.forEach((feature) => {
      const props = feature.properties as any;
      if (props?.droneId !== undefined) {
        const droneId = Number(props.droneId);
        if (!droneIds.includes(droneId)) {
          droneIds.push(droneId);
        }
        totalMoves += Number(props.totalMoves) || 0;
        totalCost += Number(props.totalCost) || 0;
        if (props.deliveryIds && Array.isArray(props.deliveryIds)) {
          props.deliveryIds.forEach((id: any) => {
            const deliveryId = Number(id);
            if (!deliveryIds.includes(deliveryId)) {
              deliveryIds.push(deliveryId);
            }
          });
        }
      }
    });

    return {
      totalMoves,
      totalCost,
      numDrones: droneIds.length,
      droneIds,
      deliveryIds,
    };
  };

  const handleRouteSelect = async (route: 'single' | 'multi') => {
    setSelectedRoute(route);
    if (comparisonData) {
      const newGeoJson = route === 'single' 
        ? comparisonData.singleDroneGeoJson 
        : comparisonData.multiDroneGeoJson;
      if (newGeoJson) {
        setGeoJsonData(newGeoJson);
        
        // Update drone details for the selected route
        const stats = extractFlightStats(newGeoJson);
        const droneIdStrings = stats.droneIds.map(id => String(id));
        
        if (droneIdStrings.length > 0) {
          setDronesLoading(true);
          try {
            const droneDetails = await getMultipleDroneDetails(droneIdStrings);
            setUsedDrones(droneDetails);
          } catch (err) {
            console.error('Failed to fetch drone details:', err);
          } finally {
            setDronesLoading(false);
          }
        }
      }
    }
  };

  const handleCalculatePath = useCallback(async () => {
    const invalidDeliveries = deliveries.filter(d => !d.coordinates);
    if (invalidDeliveries.length > 0) {
      setError('Please geocode all addresses before calculating path');
      return;
    }

    setIsLoading(true);
    setError(null);
    setEmailStatus({ type: null, message: '' });
    setIsPlaying(false);
    setComparisonLoading(true);

    try {
      const dispatches: MedDispatchRec[] = deliveries.map((delivery) => {
        const dispatch: MedDispatchRec = {
          id: delivery.id,
          requirements: {
            capacity: delivery.capacity,
          },
          delivery: delivery.coordinates!,
        };

        if (delivery.cooling) {
          dispatch.requirements.cooling = true;
        }
        if (delivery.heating) {
          dispatch.requirements.heating = true;
        }
        if (delivery.maxCost !== undefined && delivery.maxCost > 0) {
          dispatch.requirements.maxCost = delivery.maxCost;
        }
        if (delivery.date) {
          dispatch.date = delivery.date;
        }
        if (delivery.time) {
          dispatch.time = delivery.time;
        }

        return dispatch;
      });

      // Fetch both regular path and comparison data
      const [result, comparison] = await Promise.all([
        calculateDeliveryPathAsGeoJson(dispatches),
        compareRoutes(dispatches),
      ]);

      setComparisonData(comparison);
      setComparisonLoading(false);
      
      // Determine which GeoJSON to use (recommended route from comparison)
      let recommendedGeoJson = result;
      let recommendedRoute: 'single' | 'multi' = 'single';
      
      if (comparison?.comparison) {
        const rec = comparison.comparison.recommendation;
        if (rec === 'SINGLE_DRONE' && comparison.singleDroneGeoJson) {
          recommendedGeoJson = comparison.singleDroneGeoJson;
          recommendedRoute = 'single';
        } else if (rec === 'MULTI_DRONE' && comparison.multiDroneGeoJson) {
          recommendedGeoJson = comparison.multiDroneGeoJson;
          recommendedRoute = 'multi';
        }
      }
      
      // Set the recommended route as default
      setSelectedRoute(recommendedRoute);
      setGeoJsonData(recommendedGeoJson);

      if (!recommendedGeoJson.features || recommendedGeoJson.features.length === 0) {
        setError('No solution found. Please check your delivery requirements.');
        setUsedDrones([]);
      } else {
        const stats = extractFlightStats(recommendedGeoJson);
        const droneIdStrings = stats.droneIds.map(id => String(id));
        
        let actualDrones: Drone[] = [];
        if (droneIdStrings.length > 0) {
          setDronesLoading(true);
          try {
            const droneDetails = await getMultipleDroneDetails(droneIdStrings);
            setUsedDrones(droneDetails);
            actualDrones = droneDetails;
          } catch (err) {
            console.error('Failed to fetch drone details:', err);
          } finally {
            setDronesLoading(false);
          }
        }
        
        // Email sends the RECOMMENDED route's stats from comparison data
        if (sendEmail && isValidEmail(email)) {
          try {
            // Use stats from comparison data (accurate) instead of GeoJSON extraction
            const compStats = comparison?.comparison;
            const emailStats: FlightStats = {
              totalMoves: recommendedRoute === 'single' 
                ? (compStats?.singleDroneMoves || stats.totalMoves)
                : (compStats?.multiDroneMoves || stats.totalMoves),
              totalCost: recommendedRoute === 'single'
                ? (compStats?.singleDroneCost || stats.totalCost)
                : (compStats?.multiDroneCost || stats.totalCost),
              numDrones: actualDrones.length || (recommendedRoute === 'multi' ? (compStats?.multiDroneCount || 1) : 1),
              droneIds: actualDrones.map(d => Number(d.id) || 0).filter(id => id > 0),
              deliveryIds: stats.deliveryIds,
            };
            
            // Add recommendation info to email
            const recommendationNote = compStats 
              ? ` (${compStats.recommendation === 'SINGLE_DRONE' ? 'Single Drone' : 'Multi Drone'} recommended: ${compStats.reason})`
              : '';
            
            const emailResult = await sendFlightStatsEmail({
              toEmail: email,
              stats: emailStats,
              recommendationNote,
            });
            
            setEmailStatus({
              type: emailResult.success ? 'success' : 'error',
              message: emailResult.message,
            });
          } catch (emailErr) {
            setEmailStatus({
              type: 'error',
              message: 'Failed to send email',
            });
          }
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to calculate path');
      setGeoJsonData(null);
      setComparisonData(null);
    } finally {
      setIsLoading(false);
      setComparisonLoading(false);
    }
  }, [deliveries, sendEmail, email]);

  const animationProgress = flightPath.length > 0 
    ? (currentMoveIndex / (flightPath.length - 1)) * 100 
    : 0;

  return (
    <div className="h-screen flex flex-col bg-gray-100">
      {/* Header */}
      <header className="bg-blue-600 text-white p-4 shadow-md">
        <h1 className="text-2xl font-bold">ILP Flight Path Visualizer</h1>
        <p className="text-sm text-blue-100 mt-1">Visualize optimal drone delivery routes with animation</p>
      </header>

      {/* Main Content */}
      <div className="flex-1 flex flex-col lg:flex-row overflow-hidden">
        {/* Left Sidebar - Delivery Form */}
        <aside className="w-full lg:w-[700px] bg-gray-50 border-r border-gray-300 overflow-y-auto">
          <DeliveryForm
            deliveries={deliveries}
            onDeliveriesChange={setDeliveries}
            onCalculatePath={handleCalculatePath}
            isCalculating={isLoading}
            email={email}
            onEmailChange={setEmail}
            sendEmail={sendEmail}
            onSendEmailChange={setSendEmail}
          />
        </aside>

        {/* Main Area - Map */}
        <main className="flex-1 relative" id="flight-map-container">
          <FlightPathMap 
            geoJsonData={geoJsonData}
            dronePosition={dronePosition}
            traveledPath={traveledPath}
            isAnimating={isAnimating && isPlaying}
          />
          
          {/* Animation Controls - Bottom of map */}
          {isAnimating && flightPath.length > 0 && (
            <div className="absolute bottom-4 left-4 right-4 z-[1000] max-w-lg mx-auto">
              <FlightAnimationControls
                isPlaying={isPlaying}
                onPlayPause={handlePlayPause}
                onReset={handleReset}
                onStepForward={handleStepForward}
                onStepBackward={handleStepBackward}
                speed={animationSpeed}
                onSpeedChange={setAnimationSpeed}
                currentMove={currentMoveIndex}
                totalMoves={flightPath.length}
                progress={animationProgress}
                onSeek={handleSeek}
                disabled={flightPath.length === 0}
              />
            </div>
          )}

          {/* Route Comparison Panel - Top right of map */}
          {(comparisonData || comparisonLoading) && (
            <div className="absolute top-4 right-4 z-[1000] w-80">
              <RouteComparisonPanel
                comparison={comparisonData}
                selectedRoute={selectedRoute}
                onRouteSelect={handleRouteSelect}
                isLoading={comparisonLoading}
              />
            </div>
          )}
          
          {/* Email Status Notification */}
          {emailStatus.type && (
            <div className={`absolute top-4 left-4 z-[1001] px-4 py-3 rounded-lg shadow-lg ${
              emailStatus.type === 'success' 
                ? 'bg-green-100 border border-green-400 text-green-700' 
                : 'bg-red-100 border border-red-400 text-red-700'
            }`}>
              <div className="flex items-center gap-2">
                <span>{emailStatus.type === 'success' ? '✓' : '✗'}</span>
                <span>{emailStatus.message}</span>
                <button 
                  onClick={() => setEmailStatus({ type: null, message: '' })}
                  className="ml-2 font-bold hover:opacity-70"
                >
                  ×
                </button>
              </div>
            </div>
          )}
        </main>
      </div>

      {/* Bottom Panel - Results */}
      <footer className="bg-white border-t border-gray-300 p-4 shadow-md">
        <div className="flex flex-col lg:flex-row gap-4">
          <div className="flex-1">
            <ResultsPanel
              geoJsonData={geoJsonData}
              isLoading={isLoading}
              error={error}
              deliveryCount={deliveries.length}
              actualDroneCount={usedDrones.length > 0 ? usedDrones.length : undefined}
              overrideCost={
                comparisonData?.comparison
                  ? (selectedRoute === 'single' 
                      ? comparisonData.comparison.singleDroneCost 
                      : comparisonData.comparison.multiDroneCost)
                  : undefined
              }
              overrideMoves={
                comparisonData?.comparison
                  ? (selectedRoute === 'single'
                      ? comparisonData.comparison.singleDroneMoves
                      : comparisonData.comparison.multiDroneMoves)
                  : undefined
              }
            />
          </div>
          {(usedDrones.length > 0 || dronesLoading) && (
            <div className="lg:w-80">
              <DroneInfoPanel drones={usedDrones} isLoading={dronesLoading} />
            </div>
          )}
        </div>
      </footer>
    </div>
  );
}

export default App;
