# Auth Service (port 8080)

## First page
Open `http://localhost:8080/login.html`. After successful login or OTP verification, the browser redirects to the Inventory dashboard at `http://localhost:8081/`.

## Setup
1. Create/use your existing Auth Service MySQL database and preserve its existing data.
2. Copy `.env.example` to `.env` in this project folder.
3. Fill in DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET_KEY, ADMIN_EMAIL and mail settings. OAuth client values are required by the current application configuration even if you do not use OAuth; use valid provider credentials if you use those login options.
4. Ensure `JWT_SECRET_KEY` is exactly the same as the value in Inventory Service `.env`.
5. Start from this folder with `mvnw.cmd spring-boot:run` on Windows or `bash mvnw spring-boot:run` on Linux/macOS.

The Auth Service remains the owner of login, signup, OTP, JWT, refresh token, OAuth and account management. Admin account creation through the management API is intentionally restricted; create/promote the first admin through your existing trusted setup.
