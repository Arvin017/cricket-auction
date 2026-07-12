# StrikeZone Auctions — Live IPL-Style Cricket Auction Platform

A full-stack, real-time cricket player auction platform. Teams (with a purse and
squad limits) bid on players one at a time, live, with all connected clients
watching the same state update instantly over WebSockets. Bid amounts must
follow IPL-style increment slabs, and the core technical piece is a **Redis
Lua script that atomically validates and applies every bid**, so two teams
bidding in the same instant can never both "win" the same bid.

```
cricket-auction/
├── backend/    Spring Boot 3.2 / Java 17, MySQL, Redis, JWT, STOMP WebSockets
└── frontend/   React + Vite
```

---

## 1. What you need installed

You'll install four things. Do them in this order and test each one before
moving to the next — that way if something goes wrong later, you already
know these four pieces work.

### 1a. Java 17 (JDK)

The backend is written in Java 17.

- **Windows/Mac/Linux**: download "Eclipse Temurin 17 (LTS)" from
  https://adoptium.net — pick the installer for your OS, run it, accept the
  defaults.
- **Check it worked**: open a terminal (Command Prompt/PowerShell on
  Windows, Terminal on Mac) and run:
  ```
  java -version
  ```
  You should see something mentioning `17.x.x`. If you see "command not
  found", the installer didn't add Java to your PATH — reinstall and make
  sure the "Add to PATH" checkbox is ticked.

### 1b. Maven

Maven builds and runs the Spring Boot backend. Most Java IDEs (like
IntelliJ) actually bundle their own copy of Maven, so if you're using
IntelliJ you can skip this and let IntelliJ handle it (see Step 3). If you
want to run things from the terminal instead:

- Download from https://maven.apache.org/download.cgi (the "Binary zip
  archive"), unzip it somewhere like `C:\maven` or `~/maven`, and add its
  `bin` folder to your PATH.
- Check it worked: `mvn -version` should print a Maven version and confirm
  it's using Java 17.

### 1c. MySQL

Stores teams, players, users, and bid history permanently.

- Download **MySQL Community Server** from
  https://dev.mysql.com/downloads/mysql/ and run the installer.
- During setup it will ask you to set a **root password** — pick something
  simple for local dev (e.g. `root`) and remember it.
- **Check it worked**: open "MySQL Command Line Client" (installed
  alongside MySQL) or run in a terminal:
  ```
  mysql -u root -p
  ```
  Enter your password. If you get a `mysql>` prompt, it's working. Type
  `exit` to leave.
- You do **not** need to manually create the `cricket_auction` database —
  the backend config creates it automatically on first run.
- If your root password isn't `root`, open
  `backend/src/main/resources/application.yml` and update the `username`/
  `password` under `spring.datasource`.

### 1d. Redis

Holds the live, fast-changing auction state (current bid, current player)
and runs the atomic bid-validation script.

- **Windows**: Redis doesn't officially support Windows. The easiest path
  is installing it via WSL (Windows Subsystem for Linux) — or, simpler
  still, download the community Windows build from
  https://github.com/tporadowski/redis/releases (get the `.msi`), install
  it, and it'll run as a background service automatically.
- **Mac**: install [Homebrew](https://brew.sh) if you don't have it, then:
  ```
  brew install redis
  brew services start redis
  ```
- **Linux**: `sudo apt install redis-server` (Ubuntu/Debian), then
  `sudo systemctl start redis-server`.
- **Check it worked**: run `redis-cli ping` in a terminal. It should
  print `PONG`.

### 1e. Node.js (for the frontend)

- Download the **LTS** version from https://nodejs.org and install it.
- Check it worked: `node -v` should print something like `v20.x.x`, and
  `npm -v` should print a version too.

---

## 2. Get the code into a project folder

Unzip/copy the `cricket-auction` folder (containing `backend/` and
`frontend/`) anywhere on your machine, e.g. `Documents/cricket-auction`.

---

## 3. Run the backend

### Option A — IntelliJ IDEA (recommended if you're not comfortable with
terminal commands)

1. Open IntelliJ IDEA → **File → Open** → select the `backend` folder.
2. IntelliJ will detect the `pom.xml` and ask to import it as a Maven
   project — click **Yes/Trust Project**. It will download dependencies
   automatically (this can take a few minutes the first time — watch the
   progress bar at the bottom right).
3. Once it's done indexing, find `AuctionApplication.java` in the project
   tree (`src/main/java/com/auction/AuctionApplication.java`), right-click
   it, and choose **Run 'AuctionApplication'**.
4. Watch the console at the bottom. You're looking for a line like:
   ```
   Started AuctionApplication in X.XXX seconds
   ```
   and, just after that, a log line confirming demo data was seeded (teams,
   players, and login accounts).

If it fails immediately, the most common causes are:
- MySQL isn't running, or the password in `application.yml` doesn't match
  yours → **paste the exact error here** and we'll fix it together.
- Redis isn't running → same, paste the error.

### Option B — Terminal

```
cd backend
mvn spring-boot:run
```

Same success signal: `Started AuctionApplication`.

The backend now runs at **http://localhost:8080**.

---

## 4. Run the frontend

Open a **new** terminal window (leave the backend running in the first
one):

```
cd frontend
npm install
npm run dev
```

`npm install` only needs to be run once (or whenever `package.json`
changes) — it downloads the frontend's dependencies. `npm run dev` starts
the dev server; you'll see something like:

```
  VITE ready
  ➜  Local:   http://localhost:5173/
```

Open **http://localhost:5173** in your browser.

---

## 5. Try it out

Demo accounts are seeded automatically on first backend startup:

| Role | Username | Password |
|---|---|---|
| Auctioneer (admin) | `admin` | `admin123` |
| Team rep, Mumbai Monarchs | `mumbaimonarchs` | `team123` |
| Team rep, Chennai Chargers | `chennaichargers` | `team123` |
| Team rep, Bengaluru Blasters | `bengalurublasters` | `team123` |
| Team rep, Delhi Dominators | `delhidominators` | `team123` |
| Team rep, Kolkata Knightriders XI | `kolkataknightridersxi` | `team123` |
| Team rep, Hyderabad Hurricanes | `hyderabadhurricanes` | `team123` |

A good way to demo it for a portfolio video/interview:

1. Open two browser windows side by side (or one normal + one incognito).
2. Log into one as `admin` (Auctioneer) → go to the **Admin** tab → click
   **Start** next to a player.
3. Log into the other as a team rep (e.g. `mumbaimonarchs`) → go to
   **Live Room** → you'll see the player appear live, with a countdown
   timer, and a **Bid** button showing the next valid increment.
4. Click bid a few times from different logged-in team reps to see the
   increment slabs and the live bid feed update instantly via WebSocket.
5. Back in the Admin tab, click **Finalize Sale** to trigger the SOLD
   animation, and watch the team's dashboard purse update.

---

## 6. The technical highlight: atomic bid validation

The file `backend/src/main/resources/scripts/bid_validation.lua` is the
piece worth walking an interviewer through. In plain terms:

- Redis can execute a Lua script as **one single atomic operation** — no
  other command can run in the middle of it, even under heavy concurrent
  load.
- The script does two checks in that one atomic step: (1) is this bid
  amount exactly the next valid increment above the current highest bid,
  and (2) does it belong to the player currently on the block (guards
  against a stale bid landing after the block has already moved on).
- If both checks pass, it writes the new highest bid in the *same*
  operation.
- Because read-check-write all happens as one atomic unit inside Redis
  itself (not read-then-check-then-write across separate calls from the
  Spring Boot app, which would have a race window), two team reps clicking
  "bid" in the same millisecond can never both be accepted — Redis
  processes them one at a time and only the genuinely valid one wins.

The Java side (`AuctionService.placeBid`) does an application-level purse
check *before* calling the script (since that only depends on the bidding
team's own state, not a cross-team race), then calls the script via
`RedisTemplate.execute(...)`, and broadcasts the result to everyone in the
room over STOMP/WebSocket.

---

## 7. Project scope notes

To keep this buildable in one pass, a few things are intentionally simple
and would be the first things to extend:

- The countdown timer auto-finalizes a sale server-side, but there's no
  UI warning as it gets close to expiring beyond the ring turning red in
  the last 5 seconds.
- `AuctionSession` (the DB entity for auction "runs") exists in the data
  model but isn't wired into history reporting yet — the live state
  currently lives in Redis; adding a wrap-up screen or exportable auction
  log would be a natural next step.
- CORS/WebSocket origins are hardcoded to `localhost` for local dev — for
  a real deployment you'd set `app.allowed-origin` to your real frontend
  URL.
- There's no password-reset flow.

If you want to trim scope further for a deadline, the safest cuts (in
order) are: seed data variety (fewer demo players), the countdown-ring
animation (a plain text timer works fine), and the `stats` free-text field
on players.

---

## 8. Deploying it (so you have a live link, not just localhost)

The simplest reliable combo for this stack: **Railway** for the backend +
MySQL + Redis (it hosts all three in one project, with a free trial
tier), and **Vercel** for the frontend (built specifically for exactly
this kind of static React app, free tier is generous).

You do **not** need Docker for this — Railway runs your Spring Boot app
directly from your GitHub repo.

### 8a. Push your project to GitHub

If you haven't already, create a new GitHub repo and push the whole
`cricket-auction` folder to it. (In IntelliJ: **Git → GitHub → Share
Project on GitHub**. Or from a terminal: `git init`, `git add .`,
`git commit -m "initial commit"`, then follow GitHub's instructions to
push.)

### 8b. Deploy the backend + databases on Railway

1. Go to https://railway.app and sign up (GitHub login is easiest).
2. Click **New Project → Deploy from GitHub repo** → pick your
   `cricket-auction` repo.
3. Railway will try to auto-detect a service — when it asks, point the
   **root directory** to `backend` (since that's where the `pom.xml`
   lives).
4. In the same project, click **+ New → Database → Add MySQL**, and
   separately **+ New → Database → Add Redis**. Railway spins both up
   automatically and gives you connection details.
5. Click on your backend service → **Variables** tab → add these
   (Railway shows you the MySQL/Redis values in their own service's
   "Variables" tab — copy them over):
   ```
   SPRING_DATASOURCE_URL      = jdbc:mysql://<railway-mysql-host>:<port>/railway
   SPRING_DATASOURCE_USERNAME = <from Railway MySQL variables>
   SPRING_DATASOURCE_PASSWORD = <from Railway MySQL variables>
   REDIS_HOST                 = <from Railway Redis variables>
   REDIS_PORT                 = <from Railway Redis variables>
   REDIS_PASSWORD              = <from Railway Redis variables>
   JWT_SECRET                 = <make up a long random string>
   ALLOWED_ORIGINS             = (fill this in after step 8c — your Vercel URL)
   ```
6. Railway will build and deploy automatically. Once it's live, click the
   service → **Settings → Networking → Generate Domain** to get a public
   URL like `https://cricket-auction-production.up.railway.app`.

### 8c. Deploy the frontend on Vercel

1. Go to https://vercel.com and sign up with GitHub.
2. **Add New → Project** → pick the same `cricket-auction` repo.
3. When it asks for the **root directory**, set it to `frontend`.
4. Under **Environment Variables**, add:
   ```
   VITE_API_BASE_URL = https://<your-railway-backend-url>/api
   VITE_WS_URL        = https://<your-railway-backend-url>/ws-sockjs
   ```
5. Click **Deploy**. You'll get a URL like
   `https://strikezone-auctions.vercel.app`.

### 8d. Connect the two

Go back to Railway → your backend service → **Variables**, and set:
```
ALLOWED_ORIGINS = https://strikezone-auctions.vercel.app
```
(This is the property the backend uses to decide which frontend origin is
allowed to call it and open a WebSocket — without this, the browser will
block requests with a CORS error.) Save — Railway will redeploy
automatically.

Give it a minute, then open your Vercel URL. That's your live, shareable
link.

**If something doesn't work at this stage**, the two most common issues
are (1) a typo in one of the env var values, or (2) forgetting to update
`ALLOWED_ORIGINS` after getting your final Vercel URL. Paste me the exact
error from the browser console or Railway's deploy logs and we'll sort it
out.
