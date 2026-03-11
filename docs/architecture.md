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
- Orders are stored in Room (`orders`, `order_items`, `payments`).
- Order status uses a simple 3-step flow: PLACED -> PREPARING -> READY.
- Local notifications are used for payment confirmation and status updates.

## Feedback & Ratings
- Customers can leave a 1–5 star rating (RatingBar) and optional comment after an order reaches READY.
- Feedback is stored in Room table `feedback` with a unique constraint per `orderId` (one feedback per order).
- Feedback can be edited later from "My Orders" (prefill existing rating/comment and save changes).
- Data integrity is enforced via a unique index on `feedback.orderId` and upsert (REPLACE) in `FeedbackDao`.