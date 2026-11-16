# Zero-Cost & Free Resources Deployment Guide
## Running the Parental Control Ecosystem for Free

This ecosystem is designed from the ground up to operate completely on **free, open, and community tiers** without requiring paid cloud infrastructure or licenses.

---

### 1. Database Tier (100% Free)

#### Option A: Local Development (Default - Zero Setup, Zero Cost)
- **Engine**: SQLite via Prisma ORM
- **Cost**: $0.00
- **Configuration**: In `backend/.env`, `DATABASE_URL="file:./dev.db"`
- **Benefits**: Self-contained, instant startup, no PostgreSQL installation required, perfect for offline development.

#### Option B: Cloud Database (Free Production Tier)
- **Provider**: **Supabase** (supabase.com) or **Neon** (neon.tech)
- **Cost**: $0.00 (Free Tier includes 500MB storage, pooled connections, SSL)
- **Setup**:
  1. Create a free account at [Supabase](https://supabase.com).
  2. Create a new project and copy your connection string (`Transaction Pooler`).
  3. In `backend/.env`:
     ```env
     DATABASE_URL="postgresql://postgres:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:6543/postgres?pgbouncer=true"
     ```
  4. In `backend/prisma/schema.prisma`, change `provider = "sqlite"` to `provider = "postgresql"`.
  5. Run `npx prisma db push`.

---

### 2. Backend Hosting (100% Free)

#### Option A: Local Machine
- Run `npm run start:dev` on `http://localhost:3000`.
- Android Emulator connects directly via `http://10.0.2.2:3000`.
- Physical Android devices on the same Wi-Fi connect via your machine's LAN IP (e.g. `http://192.168.1.50:3000`).

#### Option B: Free Cloud Hosting
- **Render** (render.com): Free Web Service tier (512MB RAM, automatic HTTPS).
- **Railway** / **Fly.io**: Free hobby allowance.
- **Vercel**: Free Serverless Node.js deployment.

---

### 3. Android Development & Testing (100% Free)

- **IDE**: Android Studio (Ladybug / Koala) - Free & Open Source.
- **Compiler**: OpenJDK 17 / 21 - Free.
- **Emulators**: Built-in Android Virtual Devices (AVD) running Android 11 to Android 15 - Free.
- **Physical Devices**: Any Android phone with USB Debugging or Wireless Debugging enabled - Free.

---

### 4. Push Notifications & Messaging (100% Free)

- **Provider**: Google Firebase Cloud Messaging (FCM)
- **Tier**: **Spark Plan** ($0.00 / month forever)
- **Limits**: Unlimited free push notifications to both Parent and Child devices.
- **Fallback**: The backend includes built-in REST polling so pairing, status checks, and policy sync work seamlessly even without Firebase keys configured.

---

### 5. Maps & Geolocation (100% Free)

- **Google Maps SDK**: Google provides **$200 free credit every month** for Google Cloud / Maps API, covering ~28,000 map loads per month with zero bill.
- **Open-Source Alternative**: MapLibre / OpenStreetMap tiles (100% free open-source, no API keys or credit card needed).
