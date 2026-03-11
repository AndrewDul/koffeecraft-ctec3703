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


## Feedback module redesign

I redesigned the feedback module so feedback is now stored per purchased product instead of per order.

### Previous design
Previously, feedback was linked to `orderId`, which meant one rating/comment represented the whole order.

### New design
I changed feedback so one feedback record is linked to one purchased product using `orderItemId`.

### Why I changed it
I changed this because admin analytics and product-based rankings require feedback to belong to a specific purchased product.  
This allows the system to calculate:
- product average rating
- best rated products
- lowest rated products
- most commented products
- least commented products

### Data model
The `feedback` entity now stores:
- `orderItemId`
- `customerId`
- `rating`
- `comment`
- `isHidden`
- `isModerated`
- `createdAt`
- `updatedAt`

### Database changes
I introduced database migrations to:
- rebuild feedback storage around `orderItemId`
- add moderation fields for hidden/moderated comments

---

## Customer feedback flow

I redesigned the customer feedback journey to make it easier to review purchased items one by one.

### Current behaviour
- feedback becomes available after the order reaches `COLLECTED`
- the customer opens feedback for a specific order
- the customer sees purchased products from that order
- each purchased product can be reviewed individually
- after submitting feedback, the next unreviewed purchased product opens automatically
- after all purchased products are reviewed, the app returns to the main menu

### Why I changed it
This creates a more realistic product-level feedback flow and avoids mixing several products inside one order-level review.

---

## Admin feedback management

I implemented an admin feedback management screen with full-width feedback cards.

### Admin feedback screen includes
- product name
- category
- order ID
- customer ID
- rating
- comment or no-comment state
- updated timestamp

### Filtering and sorting
I added:
- rating filter (`All`, `1★`, `2★`, `3★`, `4★`, `5★`)
- comment filter (`All comments`, `With comment`, `Without comment`)
- category filter (`All categories`, `Coffee`, `Cake`)
- sort mode (`Newest first`, `Oldest first`)

### Admin actions
I added:
- delete feedback
- hide comment
- unhide comment

### Moderation logic
When a comment is hidden:
- the feedback record stays in the database
- the rating remains available for statistics
- the comment becomes hidden in admin presentation
- moderation state is stored using `isHidden` and `isModerated`

### Why I used hide instead of delete for moderation
I used hide/unhide so inappropriate comments can be moderated without losing rating data used by dashboard statistics.

---

## Admin Home feedback dashboard

I expanded Admin Home into a dashboard with full-width cards.

### Dashboard cards
1. Order simulation
2. Best rated products
3. Lowest rated products
4. Most commented products
5. Least commented products

### Carousel behaviour
Each ranking card uses a carousel that shows up to three ranked positions:
- `#1`
- `#2`
- `#3`

Each carousel also includes dot indicators to show the current position.

### Ranking rules
- **Best rated products**: highest average rating
- **Lowest rated products**: lowest average rating
- **Most commented products**: highest number of non-empty comments
- **Least commented products**: lowest number of non-empty comments, excluding products with zero comments

### Why I added this dashboard
This gives the admin a quick overview of product sentiment and comment activity without opening the full feedback management screen.

---

## Key files involved

### Data layer
- `Feedback.kt`
- `FeedbackDao.kt`
- `OrderItemDao.kt`
- `KoffeeCraftDatabase.kt`

### Customer flow
- `FeedbackFragment.kt`
- `ProductFeedbackFragment.kt`
- `FeedbackProductsAdapter.kt`
- `fragment_feedback.xml`
- `fragment_product_feedback.xml`
- `item_feedback_product.xml`
- `OrderStatusFragment.kt`
- `nav_graph.xml`

### Admin flow
- `AdminFeedbackFragment.kt`
- `AdminFeedbackAdapter.kt`
- `fragment_admin_feedback.xml`
- `item_admin_feedback.xml`
- `AdminHomeFragment.kt`
- `AdminHomeCarouselAdapter.kt`
- `fragment_admin_home.xml`
- `item_admin_home_carousel_page.xml`
