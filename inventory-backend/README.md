# Inventory Management — Spring Boot Backend

Migrated from the Core Java CLI project. This is a backend-only REST API; no frontend or login/signup implementation is included yet because the separate authentication app will be integrated later with `RestClient`.

## Architecture

- `entity`: JPA entities (`Product`)
- `repository`: Spring Data JPA database access
- `service`: business logic, CSV reports, email, scheduled stock alerts
- `controller`: REST endpoints
- `dto`: request/response objects and validation
- `advice`: consistent API error handling
- `exception`: application-specific exceptions

## Requirements

- JDK 21
- MySQL
- Maven (or `mvnw`)

## Database setup

Create a MySQL database named `inventory_management`, or set `DB_URL` to your existing database. The legacy `products` table must use these column names: `product_id`, `product_name`, `product_quantity`, `product_price`, `product_category`, and `threshold`. For a new database, use `src/main/resources/schema-mysql.sql`.

**Existing database note:** Hibernate `ddl-auto=update` does not reliably convert an old manually assigned `product_id` column to `AUTO_INCREMENT`. Back up your database and verify the table definition before running POST requests. If the old table has a primary key on `product_id`, review `src/main/resources/migrate-legacy-products-table.sql` and run it manually only after confirming it matches your schema. Do not drop an existing table to resolve schema errors.

## Environment variables

- `DB_URL` (default `jdbc:mysql://localhost:3306/inventory_management`)
- `DB_USER` (default `root`)
- `DB_PASS` (default empty)
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASS`
- `STOCK_ALERT_RECIPIENT` — email recipient for scheduled stock alerts
- `STOCK_ALERT_ENABLED` (default `true`)
- `SERVER_PORT` (default `8081`; uses 8081 to avoid colliding with a typical auth service on 8080)

Do not put real passwords or SMTP app passwords into source control.

## Run

On Windows: `mvnw.cmd spring-boot:run`

On macOS/Linux: `./mvnw spring-boot:run`

Or open the project in IntelliJ IDEA and run `InventoryManagementApplication`.

## API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/products` | Create a product |
| GET | `/api/products` | List products |
| GET | `/api/products/{id}` | Get one product |
| PUT | `/api/products/{id}` | Replace/update a product |
| DELETE | `/api/products/{id}` | Delete a product |
| GET | `/api/products/filter/price?minPrice=10&maxPrice=100` | Filter by price range |
| GET | `/api/products/low-stock` | List products where quantity is below threshold |
| GET | `/api/reports/inventory.csv` | Download a CSV inventory report |
| POST | `/api/reports/email` | Email the CSV report; JSON body: `{"recipientEmail":"recipient@example.com"}` |

Create product example:

```json
{
  "productName": "Keyboard",
  "quantity": 20,
  "price": 799.00,
  "category": "Electronics",
  "threshold": 5
}
```

## Scheduled stock alerts

`@EnableScheduling` is in the main application class. `StockAlertScheduler` uses `@Scheduled` to check low-stock products every five minutes (configurable via `STOCK_ALERT_FIXED_RATE_MS`). It sends a combined alert only when low-stock products exist and `STOCK_ALERT_RECIPIENT` is configured. This avoids a thread/executor being created during CLI login. Email delivery requires valid SMTP configuration.

## Authentication integration later

This service currently exposes inventory endpoints without authentication. The next phase is to connect it to your existing login/signup application through a configured Spring `RestClient`, then apply the agreed authentication/authorization contract. Do not expose this service publicly until that security integration is completed.
