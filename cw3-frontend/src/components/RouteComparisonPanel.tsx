import React from 'react';
import { RouteComparisonResponse, ComparisonStats } from '../types';

interface RouteComparisonPanelProps {
  comparison: RouteComparisonResponse | null;
  selectedRoute: 'single' | 'multi';
  onRouteSelect: (route: 'single' | 'multi') => void;
  isLoading: boolean;
}

const RouteComparisonPanel: React.FC<RouteComparisonPanelProps> = ({
  comparison,
  selectedRoute,
  onRouteSelect,
  isLoading,
}) => {
  if (isLoading) {
    return (
      <div className="bg-white rounded-xl shadow-lg p-4 border border-gray-200">
        <h4 className="text-lg font-semibold text-gray-800 mb-4">Route Comparison</h4>
        <div className="flex items-center justify-center py-6">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <span className="ml-3 text-gray-600">Comparing routes...</span>
        </div>
      </div>
    );
  }

  if (!comparison || !comparison.comparison) {
    return null;
  }

  const stats = comparison.comparison;
  const hasSingleDrone = stats.singleDronePossible && comparison.singleDroneSolution;
  const hasMultiDrone = comparison.multiDroneSolution && 
    comparison.multiDroneSolution.dronePaths && 
    comparison.multiDroneSolution.dronePaths.length >= 2; // 2 or more drones = multi-drone

  // Don't show the panel if there's no meaningful comparison
  if (!hasSingleDrone && !hasMultiDrone) {
    return null;
  }
  
  // If only single drone solution exists (no multi-drone alternative), don't show comparison
  if (hasSingleDrone && !hasMultiDrone) {
    return null;
  }
  
  // If only multi-drone exists (single not possible), don't show comparison
  if (!hasSingleDrone && hasMultiDrone) {
    return null;
  }

  const getRecommendationBadge = () => {
    if (stats.recommendation === 'SINGLE_DRONE') {
      return (
        <span className="px-2 py-1 bg-green-100 text-green-800 text-xs font-medium rounded-full">
          ✓ Recommended
        </span>
      );
    } else if (stats.recommendation === 'MULTI_DRONE') {
      return (
        <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs font-medium rounded-full">
          ✓ Recommended
        </span>
      );
    }
    return null;
  };

  return (
    <div className="bg-white rounded-xl shadow-lg p-4 border border-gray-200">
      <h4 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
        📊 Route Comparison
      </h4>

      {/* Route Toggle Buttons */}
      <div className="grid grid-cols-2 gap-3 mb-4">
        {/* Single Drone Option */}
        <button
          onClick={() => hasSingleDrone && onRouteSelect('single')}
          disabled={!hasSingleDrone}
          className={`p-3 rounded-lg border-2 transition-all text-left ${
            selectedRoute === 'single' && hasSingleDrone
              ? 'border-green-500 bg-green-50'
              : hasSingleDrone
              ? 'border-gray-200 hover:border-green-300 hover:bg-green-50/50'
              : 'border-gray-200 bg-gray-50 opacity-60 cursor-not-allowed'
          }`}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="font-semibold text-gray-800">Single Drone</span>
            {stats.recommendation === 'SINGLE_DRONE' && getRecommendationBadge()}
          </div>
          {hasSingleDrone ? (
            <div className="text-sm text-gray-600 space-y-1">
              <p>Cost: <span className="font-medium text-gray-800">£{stats.singleDroneCost?.toFixed(2)}</span></p>
              <p>Moves: <span className="font-medium text-gray-800">{stats.singleDroneMoves}</span></p>
              <p>Drones: <span className="font-medium text-gray-800">1</span></p>
            </div>
          ) : (
            <p className="text-sm text-gray-500 italic">Not available</p>
          )}
        </button>

        {/* Multi Drone Option */}
        <button
          onClick={() => hasMultiDrone && onRouteSelect('multi')}
          disabled={!hasMultiDrone}
          className={`p-3 rounded-lg border-2 transition-all text-left ${
            selectedRoute === 'multi' && hasMultiDrone
              ? 'border-blue-500 bg-blue-50'
              : hasMultiDrone
              ? 'border-gray-200 hover:border-blue-300 hover:bg-blue-50/50'
              : 'border-gray-200 bg-gray-50 opacity-60 cursor-not-allowed'
          }`}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="font-semibold text-gray-800">Multi Drone</span>
            {stats.recommendation === 'MULTI_DRONE' && getRecommendationBadge()}
          </div>
          {hasMultiDrone ? (
            <div className="text-sm text-gray-600 space-y-1">
              <p>Cost: <span className="font-medium text-gray-800">£{stats.multiDroneCost?.toFixed(2)}</span></p>
              <p>Moves: <span className="font-medium text-gray-800">{stats.multiDroneMoves}</span></p>
              <p>Drones: <span className="font-medium text-gray-800">{stats.multiDroneCount}</span></p>
            </div>
          ) : (
            <p className="text-sm text-gray-500 italic">Not available</p>
          )}
        </button>
      </div>

      {/* Comparison Stats */}
      {hasSingleDrone && hasMultiDrone && (
        <div className="bg-gray-50 rounded-lg p-3 mb-3">
          <h5 className="text-sm font-medium text-gray-700 mb-2">Comparison</h5>
          <div className="grid grid-cols-2 gap-2 text-sm">
            {stats.costDifferencePercent !== undefined && stats.costDifferencePercent !== null && (
              <div className="flex items-center gap-1">
                <span className="text-gray-600">Cost Diff:</span>
                <span className={`font-medium ${stats.costDifferencePercent > 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {stats.costDifferencePercent > 0 ? '+' : ''}{stats.costDifferencePercent.toFixed(1)}%
                </span>
              </div>
            )}
            {stats.moveDifferencePercent !== undefined && stats.moveDifferencePercent !== null && (
              <div className="flex items-center gap-1">
                <span className="text-gray-600">Move Diff:</span>
                <span className={`font-medium ${stats.moveDifferencePercent > 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {stats.moveDifferencePercent > 0 ? '+' : ''}{stats.moveDifferencePercent.toFixed(1)}%
                </span>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Recommendation Reason */}
      <div className={`rounded-lg p-3 ${
        stats.recommendation === 'SINGLE_DRONE' ? 'bg-green-50 border border-green-200' :
        stats.recommendation === 'MULTI_DRONE' ? 'bg-blue-50 border border-blue-200' :
        'bg-gray-50 border border-gray-200'
      }`}>
        <p className="text-sm text-gray-700">
          <span className="font-medium">Analysis: </span>
          {stats.reason}
        </p>
      </div>
    </div>
  );
};

export default RouteComparisonPanel;

