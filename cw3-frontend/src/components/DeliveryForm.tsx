import React, { useState, useCallback } from 'react';
import { DeliveryFormData, LngLat } from '../types';
import { geocodeAddress } from '../services/geocodingService';
import { isValidEmail } from '../services/emailService';
import AvailableDronesPreview from './AvailableDronesPreview';
import AddressAutocomplete from './AddressAutocomplete';

interface DeliveryFormProps {
  deliveries: DeliveryFormData[];
  onDeliveriesChange: (deliveries: DeliveryFormData[]) => void;
  onCalculatePath: () => void;
  isCalculating: boolean;
  email: string;
  onEmailChange: (email: string) => void;
  sendEmail: boolean;
  onSendEmailChange: (send: boolean) => void;
}

const DeliveryForm: React.FC<DeliveryFormProps> = ({
  deliveries,
  onDeliveriesChange,
  onCalculatePath,
  isCalculating,
  email,
  onEmailChange,
  sendEmail,
  onSendEmailChange,
}) => {
  const [geocodingIndex, setGeocodingIndex] = useState<number | null>(null);

  // Handle selection from autocomplete dropdown
  const handleAddressSelect = useCallback((index: number, address: string, coordinates: LngLat) => {
    const updatedDeliveries = [...deliveries];
    updatedDeliveries[index].address = address;
    updatedDeliveries[index].coordinates = coordinates;
    updatedDeliveries[index].geocodingStatus = 'success';
    updatedDeliveries[index].geocodingError = undefined;
    onDeliveriesChange(updatedDeliveries);
  }, [deliveries, onDeliveriesChange]);

  const handleGeocode = useCallback(async (index: number, address: string) => {
    if (!address.trim()) {
      return;
    }

    setGeocodingIndex(index);
    const updatedDeliveries = [...deliveries];
    updatedDeliveries[index].geocodingStatus = 'loading';
    updatedDeliveries[index].geocodingError = undefined;
    onDeliveriesChange(updatedDeliveries);

    try {
      const coordinates = await geocodeAddress(address);
      updatedDeliveries[index].coordinates = coordinates;
      updatedDeliveries[index].geocodingStatus = 'success';
      updatedDeliveries[index].geocodingError = undefined;
    } catch (error) {
      updatedDeliveries[index].geocodingStatus = 'error';
      updatedDeliveries[index].geocodingError = error instanceof Error ? error.message : 'Geocoding failed';
      updatedDeliveries[index].coordinates = undefined;
    } finally {
      setGeocodingIndex(null);
      onDeliveriesChange(updatedDeliveries);
    }
  }, [deliveries, onDeliveriesChange]);

  const handleAddDelivery = () => {
    const newDelivery: DeliveryFormData = {
      id: deliveries.length > 0 ? Math.max(...deliveries.map(d => d.id)) + 1 : 1,
      address: '',
      capacity: 0.5,
      cooling: false,
      heating: false,
      geocodingStatus: 'idle',
    };
    onDeliveriesChange([...deliveries, newDelivery]);
  };

  const handleRemoveDelivery = (index: number) => {
    onDeliveriesChange(deliveries.filter((_, i) => i !== index));
  };

  const handleFieldChange = (index: number, field: keyof DeliveryFormData, value: any) => {
    const updatedDeliveries = [...deliveries];
    (updatedDeliveries[index] as any)[field] = value;
    // Reset geocoding status if address changes
    if (field === 'address') {
      updatedDeliveries[index].geocodingStatus = 'idle';
      updatedDeliveries[index].coordinates = undefined;
      updatedDeliveries[index].geocodingError = undefined;
    }
    onDeliveriesChange(updatedDeliveries);
  };

  const allGeocoded = deliveries.length > 0 && deliveries.every(d => d.coordinates !== undefined);
  const canCalculate = allGeocoded && !isCalculating && deliveries.length > 0;

  return (
    <div className="bg-white p-10 rounded-lg shadow-lg h-full overflow-y-auto">
      <h2 className="text-5xl font-bold mb-10 text-gray-800">Delivery Configuration</h2>
      
      <div className="space-y-8 mb-10">
        {deliveries.map((delivery, index) => (
          <div key={delivery.id} className="border border-gray-300 rounded-xl p-6 bg-gray-50">
            <div className="flex justify-between items-center mb-5">
              <h3 className="text-3xl font-semibold text-gray-700">Delivery #{delivery.id}</h3>
              {deliveries.length > 1 && (
                <button
                  onClick={() => handleRemoveDelivery(index)}
                  className="text-red-600 hover:text-red-800 font-medium"
                  type="button"
                >
                  Remove
                </button>
              )}
            </div>

            <div className="space-y-6">
              {/* Address Field with Autocomplete */}
              <div>
                <label className="block text-xl font-medium text-gray-700 mb-3">
                  Delivery Address *
                </label>
                <AddressAutocomplete
                  value={delivery.address}
                  onChange={(address) => handleFieldChange(index, 'address', address)}
                  onSelect={(address, coordinates) => handleAddressSelect(index, address, coordinates)}
                  placeholder="Start typing an address..."
                  className="px-5 py-4 text-xl border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                {delivery.geocodingStatus === 'success' && delivery.coordinates && (
                  <p className="text-xl text-green-600 mt-3">
                    ✓ {delivery.coordinates.lat.toFixed(6)}, {delivery.coordinates.lng.toFixed(6)}
                  </p>
                )}
                {delivery.geocodingStatus === 'error' && delivery.geocodingError && (
                  <p className="text-xl text-red-600 mt-3">✗ {delivery.geocodingError}</p>
                )}
              </div>

              {/* Capacity */}
              <div>
                <label className="block text-xl font-medium text-gray-700 mb-3">
                  Capacity (required) *
                </label>
                <input
                  type="number"
                  inputMode="decimal"
                  value={delivery.capacity || ''}
                  onChange={(e) => {
                    const val = e.target.value;
                    handleFieldChange(index, 'capacity', val === '' ? 0 : parseFloat(val) || 0);
                  }}
                  step="any"
                  min="0"
                  placeholder="e.g. 5"
                  className="w-full px-5 py-4 text-xl border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Cooling and Heating */}
              <div className="flex gap-10">
                <label className="flex items-center">
                  <input
                    type="checkbox"
                    checked={delivery.cooling}
                    onChange={(e) => handleFieldChange(index, 'cooling', e.target.checked)}
                    className="mr-3 w-7 h-7"
                  />
                  <span className="text-xl text-gray-700">Cooling</span>
                </label>
                <label className="flex items-center">
                  <input
                    type="checkbox"
                    checked={delivery.heating}
                    onChange={(e) => handleFieldChange(index, 'heating', e.target.checked)}
                    className="mr-3 w-7 h-7"
                  />
                  <span className="text-xl text-gray-700">Heating</span>
                </label>
              </div>

              {/* Max Cost */}
              <div>
                <label className="block text-xl font-medium text-gray-700 mb-3">
                  Max Cost (optional)
                </label>
                <input
                  type="number"
                  value={delivery.maxCost || ''}
                  onChange={(e) => handleFieldChange(index, 'maxCost', e.target.value ? parseFloat(e.target.value) : undefined)}
                  min="0"
                  step="0.1"
                  placeholder="Optional"
                  className="w-full px-5 py-4 text-xl border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Date and Time */}
              <div className="grid grid-cols-2 gap-5">
                <div>
                  <label className="block text-xl font-medium text-gray-700 mb-3">
                    Date (optional)
                  </label>
                  <input
                    type="date"
                    value={delivery.date || ''}
                    onChange={(e) => handleFieldChange(index, 'date', e.target.value || undefined)}
                    className="w-full px-5 py-4 text-xl border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xl font-medium text-gray-700 mb-3">
                    Time (optional)
                  </label>
                  <input
                    type="time"
                    value={delivery.time || ''}
                    onChange={(e) => handleFieldChange(index, 'time', e.target.value || undefined)}
                    className="w-full px-5 py-4 text-xl border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Available Drones Preview */}
      <AvailableDronesPreview deliveries={deliveries} />

      {/* Email Section */}
      <div className="border border-gray-300 rounded-xl p-6 bg-blue-50 mb-10">
        <h3 className="text-3xl font-semibold text-gray-700 mb-5">Email Results</h3>
        
        <label className="flex items-center mb-5 cursor-pointer">
          <input
            type="checkbox"
            checked={sendEmail}
            onChange={(e) => onSendEmailChange(e.target.checked)}
            className="mr-3 w-7 h-7"
          />
          <span className="text-xl text-gray-700">Send results to email</span>
        </label>

        {sendEmail && (
          <div>
            <label className="block text-xl font-medium text-gray-700 mb-3">
              Email Address
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => onEmailChange(e.target.value)}
              placeholder="your@email.com"
              className={`w-full px-5 py-4 text-xl border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                email && !isValidEmail(email) ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {email && !isValidEmail(email) && (
              <p className="text-xl text-red-600 mt-3">Please enter a valid email address</p>
            )}
          </div>
        )}
      </div>

      <div className="space-y-5">
        <button
          onClick={handleAddDelivery}
          className="w-full px-6 py-5 bg-green-600 text-white rounded-xl hover:bg-green-700 font-semibold text-2xl"
          type="button"
        >
          + Add Delivery
        </button>

        <button
          onClick={onCalculatePath}
          disabled={!canCalculate || (sendEmail && !isValidEmail(email))}
          className="w-full px-6 py-6 bg-blue-600 text-white rounded-xl hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed font-bold text-3xl"
          type="button"
        >
          {isCalculating ? 'Calculating...' : 'Calculate Path'}
        </button>

        {!allGeocoded && deliveries.length > 0 && (
          <p className="text-xl text-amber-600 text-center">
            Please geocode all addresses before calculating path
          </p>
        )}
        
        {sendEmail && !isValidEmail(email) && email && (
          <p className="text-xl text-red-600 text-center">
            Please enter a valid email address
          </p>
        )}
      </div>
    </div>
  );
};

export default DeliveryForm;

