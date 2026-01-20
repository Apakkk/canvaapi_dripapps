# Canva Design Importer MVP

A minimal proof-of-concept application demonstrating the **Canva → PNG → Our System** flow.

## Overview

This MVP allows users to:
1. Authenticate with Canva via OAuth 2.0
2. View a list of their existing Canva designs
3. Import any design as a flattened PNG image
4. View the imported images in our system

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌───────────────┐
│    Frontend     │────▶│     Backend      │────▶│   Canva API   │
│  (React + TS)   │     │  (Spring Boot)   │     │               │
│  localhost:5173 │     │  localhost:8080  │     │ api.canva.com │
└─────────────────┘     └──────────────────┘     └───────────────┘
                               │
                               ▼
                        ┌──────────────┐
                        │   /uploads   │
                        │  (PNG files) │
                        └──────────────┘
```

## Prerequisites

- **Java 17+** (tested up to Java 24)
- **Node.js 18+**
- **Canva Developer Account** with an integration configured
- **Maven 3.8+** (or use included Maven wrapper)

## Canva Developer Setup

1. Go to [Canva Developers Portal](https://www.canva.com/developers/)
2. Create a new integration
3. Configure the following:
   - **Integration Name**: Your app name
   - **Redirect URL**: `http://localhost:8080/api/canva/callback`
   - **Scopes**: Select `design:meta:read` and `design:content:read`
4. Generate and save your **Client ID** and **Client Secret**

## Environment Variables

Set these environment variables before running the backend:

```bash
export CANVA_CLIENT_ID=your_canva_client_id
export CANVA_CLIENT_SECRET=your_canva_client_secret
```

## Running the Application

### Option 1: Quick Start Script

```bash
# Set environment variables first
export CANVA_CLIENT_ID=your_client_id
export CANVA_CLIENT_SECRET=your_client_secret

# Run both services
./start-dev.sh
```

### Option 2: Manual Start

#### 1. Start the Backend

```bash
cd backend

# Set environment variables
export CANVA_CLIENT_ID=your_client_id
export CANVA_CLIENT_SECRET=your_client_secret

# Run with Maven
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

#### 2. Start the Frontend

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:5173`

### 3. Use the Application

1. Open `http://localhost:5173` in your browser
2. Click "Login with Canva"
3. Authorize the application in Canva
4. You'll be redirected back with your designs listed
5. Click "Import" on any design to download it as PNG
6. View the imported PNG in the preview panel

## Project Structure

```
canvainteg_dripapps/
├── backend/                      # Spring Boot application
│   ├── pom.xml                   # Maven dependencies
│   └── src/main/
│       ├── java/com/dripapps/canva/
│       │   ├── CanvaIntegrationApplication.java
│       │   ├── config/
│       │   │   ├── CanvaConfig.java      # API configuration
│       │   │   └── WebConfig.java        # CORS & WebClient
│       │   ├── controller/
│       │   │   ├── CanvaAuthController.java   # OAuth endpoints
│       │   │   ├── DesignController.java      # Design CRUD
│       │   │   └── ImageController.java       # Serve PNGs
│       │   ├── dto/
│       │   │   ├── AuthStatusDto.java
│       │   │   ├── DesignDto.java
│       │   │   └── ImportResultDto.java
│       │   └── service/
│       │       ├── CanvaApiService.java   # Canva API calls
│       │       └── CanvaAuthService.java  # OAuth flow
│       └── resources/
│           └── application.yml
│
├── frontend/                     # React + TypeScript application
│   ├── package.json
│   ├── src/
│   │   ├── App.tsx               # Main application component
│   │   ├── App.css               # Component styles
│   │   ├── api.ts                # Backend API client
│   │   ├── types.ts              # TypeScript interfaces
│   │   ├── main.tsx              # Application entry point
│   │   └── index.css             # Global styles
│   └── vite.config.ts
│
├── start-dev.sh                  # Quick start script
└── README.md
```

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/canva/auth/status` | Check if user is authenticated |
| GET | `/api/canva/auth/login` | Get OAuth authorization URL |
| GET | `/api/canva/callback` | OAuth callback (internal use) |
| POST | `/api/canva/logout` | Clear session |

### Designs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/designs` | List user's Canva designs |
| GET | `/api/designs/{id}` | Get specific design details |
| POST | `/api/designs/{id}/import` | Import design as PNG |

### Images

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/images/{filename}` | Serve imported PNG image |

## Canva API Limitations

1. **OAuth with PKCE Required**: Canva requires Proof Key for Code Exchange (PKCE) with SHA-256 for all OAuth flows.

2. **Async Exports**: Design exports are asynchronous. The API returns a job ID, and you must poll for completion.

3. **Rate Limits**: All Canva API endpoints have rate limits. Production applications should implement retry logic with exponential backoff.

4. **Temporary URLs**: 
   - Thumbnail URLs from the API are temporary
   - Export download URLs are valid for only 24 hours

5. **Multi-page Designs**: When exporting multi-page designs to PNG, Canva returns a ZIP file containing individual PNGs.

6. **Pro Features**: Some export options (transparent background, compression settings) require Canva Pro.

7. **Scopes Required**:
   - `design:meta:read` - To list designs
   - `design:content:read` - To export designs

## What This MVP Does NOT Include

Per requirements, this is a minimal MVP and does NOT implement:

- ❌ Canva editor embed
- ❌ SVG or layer parsing
- ❌ Design editing capabilities
- ❌ Database storage (uses filesystem)
- ❌ User authentication system (only Canva OAuth)
- ❌ Advanced UI/styling
- ❌ Token refresh logic
- ❌ Production-ready error handling
- ❌ Pagination for design lists

## Troubleshooting

### "Not authenticated" error
- Check that your Canva credentials are correct
- Ensure the redirect URI in Canva Developer Portal matches exactly: `http://localhost:8080/api/canva/callback`

### Import fails
- Check backend logs for detailed error messages
- Verify you have access to the design
- Some designs with premium elements may fail to export

### CORS errors
- Backend must be running on port 8080
- Frontend must be running on port 5173
- Both must be accessed via localhost, not IP address

### Backend won't start
- Ensure `CANVA_CLIENT_ID` and `CANVA_CLIENT_SECRET` environment variables are set
- Check that port 8080 is not in use

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.1, WebFlux (WebClient)
- **Frontend**: React 18, TypeScript, Vite
- **No external dependencies**: No database, no styling libraries, minimal complexity

## License

MIT
