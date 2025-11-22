#!/bin/bash

echo "=== Testing Endpoint 5a: calcDeliveryPath ==="
echo ""

# Test 1: Single delivery with basic requirements
echo "Test 1: Single delivery (capacity only)"
curl -s -X POST http://localhost:8080/api/v1/calcDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"requirements":{"capacity":4.0},"delivery":{"lng":-3.1865,"lat":55.9445}}]' | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('dronePaths'):
    path = data['dronePaths'][0]['deliveries'][0]['flightPath']
    print(f\"  ✅ Path: {len(path)} points, Moves: {data.get('totalMoves')}, Cost: {data.get('totalCost'):.2f}\")
    print(f\"  Expected moves: {len(path)-1}, Match: {data.get('totalMoves')==len(path)-1}\")
else:
    print(f\"  ⚠️  No path returned\")
"
echo ""

# Test 2: Single delivery with cooling requirement
echo "Test 2: Single delivery (capacity + cooling)"
curl -s -X POST http://localhost:8080/api/v1/calcDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"date":"2025-12-22","time":"14:30","requirements":{"capacity":8.0,"cooling":true},"delivery":{"lng":-3.188,"lat":55.944}}]' | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('dronePaths'):
    path = data['dronePaths'][0]['deliveries'][0]['flightPath']
    print(f\"  ✅ Path: {len(path)} points, Moves: {data.get('totalMoves')}, Cost: {data.get('totalCost'):.2f}\")
else:
    print(f\"  ⚠️  No path returned\")
"
echo ""

# Test 3: Multiple deliveries
echo "Test 3: Multiple deliveries (2 dispatches)"
curl -s -X POST http://localhost:8080/api/v1/calcDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"date":"2025-12-22","time":"14:30","requirements":{"capacity":8.0,"cooling":true},"delivery":{"lng":-3.188,"lat":55.944}},{"id":2,"date":"2025-12-22","time":"15:00","requirements":{"capacity":4.0,"heating":true},"delivery":{"lng":-3.189,"lat":55.945}}]' | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('dronePaths'):
    dp = data['dronePaths'][0]
    print(f\"  ✅ Drone: {dp.get('droneId')}, Deliveries: {len(dp.get('deliveries', []))}\")
    total_moves = 0
    for d in dp.get('deliveries', []):
        total_moves += len(d.get('flightPath', [])) - 1
    print(f\"  Total moves: {data.get('totalMoves')}, Expected: {total_moves}, Match: {data.get('totalMoves')==total_moves}\")
else:
    print(f\"  ⚠️  No path returned\")
"
echo ""

# Test 4: Empty array
echo "Test 4: Empty array"
curl -s -X POST http://localhost:8080/api/v1/calcDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[]' | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('totalCost') == 0.0 and data.get('totalMoves') == 0 and data.get('dronePaths') == []:
    print(f\"  ✅ Correct empty response\")
else:
    print(f\"  ❌ Unexpected response\")
"
echo ""

# Test 5: Delivery at service point (should be very short path)
echo "Test 5: Delivery at service point"
curl -s -X POST http://localhost:8080/api/v1/calcDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"requirements":{"capacity":4.0},"delivery":{"lng":-3.1863580788986368,"lat":55.94468066708487}}]' | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('dronePaths'):
    path = data['dronePaths'][0]['deliveries'][0]['flightPath']
    print(f\"  ✅ Path: {len(path)} points, Moves: {data.get('totalMoves')}\")
    # Check if first and last are close (return path)
    first = path[0]
    last = path[-1]
    dist = ((first['lng']-last['lng'])**2 + (first['lat']-last['lat'])**2)**0.5
    print(f\"  Return check: First to last distance = {dist:.10f} (should be < 0.00015)\")
else:
    print(f\"  ⚠️  No path returned\")
"
echo ""

# Test 6: Delivery with heating requirement
echo "Test 6: Delivery with heating requirement"
curl -s -X POST http://localhost:8080/api/v1/calcDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"requirements":{"capacity":4.0,"heating":true},"delivery":{"lng":-3.187,"lat":55.945}}]' | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('dronePaths'):
    path = data['dronePaths'][0]['deliveries'][0]['flightPath']
    print(f\"  ✅ Path: {len(path)} points, Moves: {data.get('totalMoves')}, Cost: {data.get('totalCost'):.2f}\")
else:
    print(f\"  ⚠️  No path returned\")
"
echo ""

# Test 7: Check hover implementation
echo "Test 7: Verify hover (duplicate coordinates)"
curl -s -X POST http://localhost:8080/api/v1/calcDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"requirements":{"capacity":4.0},"delivery":{"lng":-3.1865,"lat":55.9445}}]' | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('dronePaths'):
    path = data['dronePaths'][0]['deliveries'][0]['flightPath']
    hover_found = False
    for i in range(len(path)-1):
        if abs(path[i]['lng']-path[i+1]['lng'])<1e-10 and abs(path[i]['lat']-path[i+1]['lat'])<1e-10:
            hover_found = True
            print(f\"  ✅ Hover found at points {i} and {i+1}\")
            break
    if not hover_found:
        print(f\"  ❌ No hover found (should have duplicate coordinates)\")
else:
    print(f\"  ⚠️  No path returned\")
"
echo ""

echo "=== All Tests Complete ==="
