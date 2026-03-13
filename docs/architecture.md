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


## Communication module and shell redesign

I added a dedicated communication layer that separates system notifications from admin messages.

### Core communication design
I introduced two separate data models:
- `AppNotification` for system events
- `InboxMessage` for direct or broadcast admin messages

I intentionally kept these separate:
- **Notifications** store system-driven events such as order progress updates and admin order-action alerts
- **Inbox** stores admin-authored messages such as promotions, reminders, or important notices

This means that a message sent from Admin Inbox does not appear in customer Notifications. It only appears in customer Inbox.

### Customer profile extension
I extended the `Customer` model with `dateOfBirth` so I can support:
- birthday-targeted inbox messages
- future rewards logic
- future birthday-based customer features

---

## Admin notifications

I implemented a stored admin notification center for manual order handling.

### Purpose
Admin notifications are created when an order requires staff action, especially when simulation is disabled and the order needs a manual status change.

### Admin notification content
Each notification stores:
- order ID
- created date/time
- current order status
- unread/read state

### Admin quick action
I added a `Next` action directly on the notification card so I can move the order through the workflow:
- `PLACED -> PREPARING`
- `PREPARING -> READY`
- `READY -> COLLECTED`

### Admin notification lifecycle
I changed the logic so admin notifications are not automatically removed after `COLLECTED`.  
The admin now removes them manually, which gives better control over completed order history.

### Admin deletion rules
I restricted deletion so only `COLLECTED` notifications can be removed.  
This prevents accidental deletion of active order notifications.

---

## Admin inbox

I implemented an Admin Inbox module for writing and sending customer messages.

### Supported targeting modes
I added support for:
- all users
- birthday today
- customer found by order number
- customer found by customer ID

### Privacy-oriented targeting
I deliberately avoided general searching by personal details such as full name or email.  
I used system-linked targeting instead:
- `orderId`
- `customerId`

This keeps the feature closer to a real system and reduces unnecessary exposure of customer data.

### Message delivery
Admin Inbox sends messages into the customer Inbox only.  
These messages do not create system notifications.

---

## Customer application shell

I rebuilt the customer-side app shell to match a more complete mobile application structure.

### Fixed top bar
I added a fixed top bar containing:
- temporary KoffeeCraft branding
- Cart icon with badge
- Inbox icon with badge
- Notifications icon with badge
- Settings icon

### Fixed bottom navigation
I added a fixed bottom navigation with:
- Home
- Menu
- My Orders
- Favourite
- Rewards

### Customer settings
I added a customer settings screen with logout functionality.

---

## Customer inbox

I added a dedicated customer Inbox for admin-authored messages.

### Behaviour
Customer Inbox:
- displays full-width cards
- supports swipe delete
- supports delete by `X`
- supports unread badge count
- supports long-message expansion using `Read more / Read less`

### Expand/collapse implementation
I changed the expansion logic so expanded state is tracked safely in the adapter rather than inside recycled ViewHolders.

---

## Customer notifications

I added a dedicated customer Notifications screen for system order updates.

### Purpose
Customer Notifications store order status events such as:
- preparing
- ready
- collected

### Behaviour
Customer Notifications:
- display full-width cards
- support swipe delete
- support delete by `X`
- use a separate unread badge from Inbox

### Expanded order details
I improved customer notification cards so the user can expand them and see:
- ordered products
- quantities
- price per unit
- line totals

This makes notification cards more useful than simple short alerts.

---

## Cart access redesign

I moved Cart access from the old Menu screen into the fixed customer top bar.

### Changes
I replaced the old cart button in Menu with:
- a top-bar cart icon
- a live badge counter

This better matches the new customer shell and makes Cart accessible from anywhere inside the main customer experience.

---

## Order flow improvements

I improved several parts of the order flow and order presentation.

### Order simulation
I extracted simulation logic into `OrderSimulationManager`.

### Why I changed it
Previously, status simulation depended on fragment lifecycle, which meant it could stop when the user left the screen.

### Result
Simulation now continues more reliably outside the fragment lifecycle.

### Back navigation
I changed Order Status so `Back to Menu` is always available.  
Only feedback availability remains tied to order status rules.

---

## My Orders redesign

I redesigned My Orders into a more app-like card system.

### New behaviour
I changed My Orders so:
- each order is displayed as a full-width card
- clicking expands or collapses the same card
- ordered products, quantities, and prices are shown inline
- `Order again` remains available inside the card

This removes unnecessary navigation and gives the user faster access to order information.

---

## Files involved

### New or expanded communication/data layer
- `AppNotification.kt`
- `InboxMessage.kt`
- `NotificationDao.kt`
- `InboxMessageDao.kt`
- `CustomerDao.kt`
- `Customer.kt`
- `KoffeeCraftDatabase.kt`

### Admin communication
- `AdminNotificationManager.kt`
- `AdminNotificationsFragment.kt`
- `AdminNotificationsAdapter.kt`
- `AdminInboxFragment.kt`
- `activity_admin.xml`
- `admin_bottom_nav.xml`
- `fragment_admin_notifications.xml`
- `item_admin_notification.xml`
- `fragment_admin_inbox.xml`

### Customer communication and shell
- `MainActivity.kt`
- `activity_main.xml`
- `customer_bottom_nav.xml`
- `CustomerInboxFragment.kt`
- `CustomerInboxAdapter.kt`
- `CustomerNotificationsFragment.kt`
- `CustomerNotificationsAdapter.kt`
- `CustomerSettingsFragment.kt`
- `fragment_customer_inbox.xml`
- `fragment_customer_notifications.xml`
- `fragment_customer_settings.xml`
- `item_customer_inbox.xml`
- `item_customer_notification.xml`

### Order and UX improvements
- `OrderStatusFragment.kt`
- `OrderSimulationManager.kt`
- `OrderItemDao.kt`
- `OrdersFragment.kt`
- `OrdersAdapter.kt`
- `item_order.xml`
- `CartManager.kt`
- `fragment_menu.xml`
- `MenuFragment.kt`