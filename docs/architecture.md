# KoffeeCraft – Architecture Overview

## Tech stack
- UI: XML Views
- Language: Kotlin
- Database: SQLite via Room
- Navigation: Navigation Component (Fragments)

## App layers
- data: Room entities, DAOs, database
- repository: business logic (auth, orders)
- ui: fragments + viewmodels

## Security
- Passwords are not stored in plain text.
- Password hashing: PBKDF2WithHmacSHA256 with per-user salt.

## Menu & Cart (MVP)
- Products are stored in Room and displayed in Menu via RecyclerView.
- Categories supported: COFFEE and CAKE.
- Cart is session-only (in-memory) and is cleared on logout.

## Notifications
- Uses Android NotificationChannel (API 26+) and NotificationCompat.
- On Android 13+ the app requests POST_NOTIFICATIONS runtime permission.
- Notifications are triggered for: Payment confirmed, Order preparing, Order ready for pickup.

## Orders & Status
- Orders are persisted in Room (`orders`, `order_items`, `payments`).
- Order status lifecycle: PLACED -> PREPARING -> READY -> COLLECTED (COLLECTED is simulated 10s after READY for newly placed orders).
- Local notifications are used for payment confirmation and status changes. Notifications are only triggered on status transitions (not on initial screen load).

## Feedback & Ratings
- Customers can leave a 1–5 star rating (RatingBar) and optional comment after an order reaches READY/COLLECTED.
- Feedback is stored in Room table `feedback` with a unique constraint on `orderId` (one feedback per order).
- Feedback can be edited later from "My Orders" (existing rating/comment is prefilled and updated via upsert/replace).

## Admin Area
- Admin UI is separated into a dedicated `AdminActivity` to isolate staff features from the customer flow.
- Admin uses BottomNavigationView with an admin-only Navigation Graph (`admin_nav_graph.xml`).
- Admin tabs: Home, Orders, Menu, Feedback, Settings (placeholders at this stage).


## Admin Orders & Simulation
- Admin can enable/disable automatic order status simulation via `SimulationSettings` (persisted in SharedPreferences).
- When simulation is OFF, staff updates order status manually from Admin Orders using a queue-style workflow.
- Admin Orders screen supports status filtering (PLACED/PREPARING/READY/COLLECTED) and shows Order ID, customer email, total and timestamp.
- Status transitions follow: PLACED -> PREPARING -> READY -> COLLECTED. Completed orders do not show the "Next" action.

## Product Management (Admin-ready)
- Products include admin-controlled flags:
    - `isActive` (DB column: `isAvailable`) – product remains visible in customer menu but should be greyed out when disabled.
    - `isNew` – drives the future customer "NEW" carousel and admin "NEW" badge.
    - `imageKey` – placeholder for future product images (maps to a drawable key or asset identifier).
- Admin Menu will manage products via card-style rows (image placeholder, name, price) with actions: Enable/Disable, Edit, Delete.


## Admin Product Management

I extended the admin menu so products can be managed directly from the admin flow without leaving the screen.

### Features added
- Product creation via dialog
- Product editing via dialog
- Product deletion with confirmation
- Product enable/disable toggle
- Visual admin badges for `NEW` and `DISABLED`

### Design decisions
- I kept `disable` as a soft availability change instead of removing the product from the database.
- I kept `delete` as a hard removal for full product deletion.
- I reused the existing `Product` entity fields:
  - `isActive` mapped to the database column `isAvailable`
  - `isNew` for admin marking and future customer highlighting
  - `imageKey` reserved for future drawable-based product image selection

### Why this approach
This keeps the admin workflow simple and fast while preserving the current database structure and avoiding unnecessary refactoring during core feature delivery.

---

## Customer Availability Rules

I aligned the customer menu with the admin availability state.

### Behaviour
- Disabled products remain visible in the customer menu
- Disabled products are visually greyed out
- Disabled products cannot be added to the cart
- The Add button changes to `Unavailable` when a product is disabled

### Reasoning
This matches the intended business rule:
- admins can temporarily make products unavailable
- customers can still see the menu item
- customers cannot order unavailable items

This also improves demo clarity because availability changes are visible immediately in the customer flow.

---

## Admin Category Filtering

I added a simple category filter to the Admin Menu to improve product management usability.

### Filter options
- `All`
- `Coffee`
- `Cake`

### Behaviour
- Only one filter can be active at a time
- `All` is selected by default
- One option is always selected
- Filtering is applied locally in the fragment after collecting `observeAll()` from Room

### Technical choice
I used `MaterialButtonToggleGroup` with:
- single selection
- required selection

This avoids custom toggle logic and keeps the UI predictable.

### Why filtering is local
I intentionally kept filtering in the UI layer because:
- the existing DAO already provides `observeAll()`
- no schema or query changes were required
- the feature remains simple and low-risk
- this is sufficient for the current product scale

---

## UI and Validation

I added basic validation to the admin product form.

### Validation rules
- Name must not be blank
- Description must not be blank
- Price must be a valid number greater than zero

### Result
This reduces invalid admin input and makes the app feel more like a real product rather than a prototype.

---

## Files involved

### Admin flow
- `AdminMenuFragment.kt`
- `AdminProductsAdapter.kt`
- `fragment_admin_menu.xml`
- `item_admin_product.xml`
- `dialog_product_form.xml`

### Customer flow
- `MenuFragment.kt`
- `ProductAdapter.kt`

### Existing reused layers
- `Product.kt`
- `ProductDao.kt`
- `KoffeeCraftDatabase.kt`
