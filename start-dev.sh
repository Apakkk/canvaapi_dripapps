#!/bin/bash

# Canva Integration MVP - Quick Start Script
# This script starts both backend and frontend for development

echo "==================================="
echo "Canva Integration MVP - Quick Start"
echo "==================================="
echo ""

# Check for environment variables
if [ -z "$CANVA_CLIENT_ID" ] || [ -z "$CANVA_CLIENT_SECRET" ]; then
    echo "⚠️  WARNING: Canva API credentials not set!"
    echo ""
    echo "Please set the following environment variables:"
    echo "  export CANVA_CLIENT_ID=your_client_id"
    echo "  export CANVA_CLIENT_SECRET=your_client_secret"
    echo ""
    echo "Get these from https://www.canva.com/developers/"
    echo ""
    read -p "Press Enter to continue anyway (for testing)..."
fi

# Start backend in background
echo "📦 Starting backend on http://localhost:8080..."
cd backend
./mvnw spring-boot:run &
BACKEND_PID=$!
cd ..

# Wait for backend to start
echo "⏳ Waiting for backend to start..."
sleep 10

# Start frontend
echo "🎨 Starting frontend on http://localhost:5173..."
cd frontend
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "==================================="
echo "✅ Both services are starting!"
echo ""
echo "Frontend: http://localhost:5173"
echo "Backend:  http://localhost:8080"
echo ""
echo "Press Ctrl+C to stop both services"
echo "==================================="

# Wait for interrupt
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT
wait
