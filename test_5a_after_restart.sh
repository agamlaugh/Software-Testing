#!/bin/bash

# Test script for endpoint 5a after restart
# Run this after restarting your Spring Boot application

echo "=== Testing Endpoint 5a After Restart ==="
echo ""

# Wait a moment for app to be ready
sleep 2

# Test 1: Check if app is running
echo "1. Checking if app is running..."
if curl -s http://localhost:8080/api/v1/uid | grep -q "s2490039"; then
    echo "   ✅ App is running"
else
    echo "   ❌ App is not running. Please start it first."
    exit 1
fi

echo ""
echo "2. Testing move count fix..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/calcDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"requirements":{"capacity":4.0},"delivery":{"lng":-3.1865,"lat":55.9445}}]')

echo "$RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data.get('dronePaths'):
        path = data['dronePaths'][0]['deliveries'][0]['flightPath']
        total_moves = data.get('totalMoves', 0)
        expected_moves = len(path) - 1
        
        print(f\"   Path points: {len(path)}\")
        print(f\"   Total moves: {total_moves}\")
        print(f\"   Expected (path.size()-1): {expected_moves}\")
        
        if total_moves == expected_moves:
            print(f\"   ✅ Move count is CORRECT\")
        else:
            print(f\"   ❌ Move count mismatch (expected {expected_moves}, got {total_moves})\")
        
        # Check hover
        hover_found = any(
            abs(path[i]['lng']-path[i+1]['lng'])<1e-10 and 
            abs(path[i]['lat']-path[i+1]['lat'])<1e-10 
            for i in range(len(path)-1)
        )
        if hover_found:
            print(f\"   ✅ Hover detected\")
        else:
            print(f\"   ❌ No hover found\")
        
        # Check return path
        last_point = path[-1]
        first_point = path[0]
        distance = ((last_point['lng']-first_point['lng'])**2 + (last_point['lat']-first_point['lat'])**2)**0.5
        if distance < 0.00015:
            print(f\"   ✅ Return path reaches service point\")
        else:
            print(f\"   ⚠️  Return path may not reach service point (distance: {distance:.10f})\")
    else:
        print(\"   ❌ No drone paths returned\")
except Exception as e:
    print(f\"   ❌ Error: {e}\")
    print(\"   Raw response:\")
    print(\"$RESPONSE\")
"

echo ""
echo "3. Testing empty array..."
EMPTY_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/calcDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[]')

echo "$EMPTY_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data.get('totalCost') == 0.0 and data.get('totalMoves') == 0 and data.get('dronePaths') == []:
        print(\"   ✅ Empty array returns correct structure\")
    else:
        print(\"   ❌ Empty array returns wrong structure\")
except:
    print(\"   ❌ Error parsing response\")
"

echo ""
echo "=== Test Complete ==="

