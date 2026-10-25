# Inventory Service + static frontend (port 8081)

## Flow
1. Start Auth Service on port 8080.
2. Start this service on port 8081.
3. Open `http://localhost:8080/login.html` as the first page.
4. Successful login redirects to `http://localhost:8081/`. If a user opens the inventory URL without a valid login, the browser redirects back to the Auth Service login page.

## Setup
1. Create/use the existing inventory MySQL database.
2. Copy `.env.example` to `.env` in this project folder and set DB_URL, DB_USER, DB_PASS and JWT_SECRET_KEY.
3. JWT_SECRET_KEY must exactly match the Auth Service secret.
4. Start from this folder with `mvnw.cmd spring-boot:run` on Windows or `bash mvnw spring-boot:run` on Linux/macOS.

The plain HTML, CSS and JavaScript frontend is under `src/main/resources/static/`. React is not used. Backend role checks remain enforced by Spring Security; hiding UI controls is not the security boundary.

## Role behavior
- ADMIN: Employee Dashboard, Users Dashboard, Products Dashboard; can manage non-admin accounts and products.
- EMPLOYEE: Users Dashboard (read-only) and Products Dashboard (product CRUD).
- USER: Products Dashboard (read-only, including price range filter).

## Notes
- Java 21 is required by the Maven project.
- Preserve existing databases; do not drop tables to run the updated code.
- Configure your existing email credentials if low-stock email alerts are enabled.
