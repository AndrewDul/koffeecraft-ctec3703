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

## Authentication entry flow redesign

I redesigned the authentication entry flow so the app now opens with a dedicated Welcome screen instead of showing the login form immediately.

### What I added
I added:
- a new `WelcomeFragment` as the first entry screen
- KoffeeCraft branding at the top
- a premium rounded card layout
- short reward-focused onboarding text
- separate actions for:
  - `Register now`
  - `Sign in`

### Navigation changes
I changed the navigation flow so:
- `welcomeFragment` is now the start destination
- global logout returns the user to the Welcome screen instead of opening Login directly

### Sign in redesign
I redesigned the Sign in screen to match the visual style of the Welcome screen.

I added:
- a premium coffee-themed rounded card layout
- styled email and password fields
- password visibility toggle
- cleaner spacing and hierarchy
- a direct link to registration

### Visual direction
I moved the authentication UI away from a plain academic form style and toward a warmer KoffeeCraft identity with:
- darker coffee-toned background
- rounded cards
- softer premium button styling
- more polished first-use presentation

### Fix-related updates
During this stage, I also fixed:
- navigation XML issues after changing the start flow
- login verification so it matches the existing password hashing implementation
- session handling so admin and customer login use the existing session model correctly


## Registration, onboarding, and rewards foundation update

I redesigned the customer registration flow so it now feels like a real KoffeeCraft product experience instead of a plain academic form.

### What I changed
I redesigned the Register screen into a premium coffee-themed card layout consistent with the Welcome and Sign in screens.

I added:
- a `Personal details` section
- a `Security` section
- country selection
- date of birth
- live password validation rules
- optional promotional Inbox consent
- required Terms of Use acceptance
- required Privacy Statement acceptance

### Customer profile model update
I extended the `Customer` entity and database so registration now stores:
- `country`
- `dateOfBirth`
- `marketingInboxConsent`
- `termsAccepted`
- `privacyAccepted`
- `beansBalance`

This gives me a proper foundation for:
- rewards
- promo messaging preferences
- future profile features
- onboarding-linked reward logic

### Database update
I added Room migration `6 -> 7` to support the new customer profile fields.

### Post-registration flow
I changed the registration flow so a newly created customer:
- is logged in immediately
- does not return to the Sign in screen
- is taken directly to onboarding

### Onboarding
I added a 3-step onboarding flow in the same premium visual style as the rest of the authentication experience.

The onboarding includes:
- centered rounded card layout
- branded visual consistency
- progress dots
- `Next`-only progression
- final confirmation step for promotional Inbox consent

### Welcome communication and starter reward logic
After onboarding:
- every new customer receives a welcome Inbox message
- a customer receives `+5 beans` only if promotional Inbox consent is enabled
- a customer notification is created when those first 5 beans are awarded

### Navigation update
I also fixed customer and admin shell navigation so bottom navigation works correctly even after opening top-bar destinations such as:
- Notifications
- Inbox
- Settings
- Cart


## Rewards screen beans balance card

I added a premium beans balance card at the top of the customer Rewards screen.

### What I added
I added:
- a full-width premium card at the top of `Rewards`
- a right-aligned bean balance group
- a coffee bean icon
- the current number of collected beans loaded from the customer profile

### Layout behaviour
I designed the bean icon and bean count as a single right-aligned group.

This means:
- the right edge stays visually fixed
- when the bean count grows, the content expands to the left
- the value does not push outside the screen
- the card remains visually stable for larger numbers

### Data source
The Rewards screen now reads the current customer's `beansBalance` from the database and displays it in the balance card.

### Visual direction
The beans balance card follows the same premium KoffeeCraft direction as the rest of the updated app:
- rounded card
- warm coffee-toned palette
- soft premium spacing
- cleaner product-style presentation

## Rewards system, beans earning, and reward redemption flow

I expanded the Rewards feature from a simple beans balance display into a working loyalty and redemption system.

### What I added
I added:
- beans earning based on purchased product quantity
- reward redemption through the `Rewards` screen
- support for reward items inside the cart
- support for £0.00 reward checkout items
- a milestone-based `5 Bean Booster` reward
- dedicated reward options for `Free Coffee` and `Free Cake`
- physical reward products for premium redemption flows

### Beans earning rules
I designed beans to be earned only from normal purchased products.

The current rule is:
- `1 purchased product = 1 bean`

This is based on product quantity, not product price.

Examples:
- 1 coffee = 1 bean
- 2 cakes = 2 beans
- 1 coffee + 2 cakes = 3 beans

Reward items do **not** generate beans.  
This prevents reward exploitation and keeps the loyalty system fair.

### Reward checkout rules
Reward items can now be added to the cart and processed through the same order flow as normal products.

Reward items:
- appear in the cart
- use a price of `£0.00`
- still go through normal order lifecycle states
- are included in the order pipeline like other items

This means redeemed rewards can move through statuses such as:
- placed
- preparing
- ready
- collected

### Beans spending rules
Beans are **not** deducted when a reward is added to the cart.

Instead, beans are deducted only at checkout.

This gives a safer and more realistic flow because:
- the customer may remove a reward before checkout
- the cart may change before payment
- beans are only committed when the order is actually placed

### Reward-aware cart model
I extended the cart model so it can now distinguish between:
- normal purchased products
- reward redemption items

Each cart line can now carry reward-related information such as:
- whether it is a reward
- reward type
- beans cost per unit

This makes it possible to:
- mix paid products and rewards in the same cart
- calculate `£` total separately from beans spending
- keep the checkout logic clear and extensible

### Rewards catalogue
I added the following reward catalogue structure:

- `Free Coffee` — 15 beans
- `Free Cake` — 18 beans
- `KoffeeCraft Mug` — 125 beans
- `KoffeeCraft Teddy Bear` — 250 beans
- `1kg Crafted Coffee Beans` — 370 beans
- `5 Bean Booster` — milestone reward

### Free Coffee and Free Cake flow
`Free Coffee` and `Free Cake` are handled as guided reward choices.

When the customer selects one of these rewards:
- the app opens a filtered selection flow
- only products from the relevant category are shown
- the customer chooses exactly one item
- the selected reward is then added to the cart as a reward item with `£0.00`

This keeps reward redemption separate from the normal product menu while still reusing real products and the normal order lifecycle.

### Physical rewards
I added support for physical rewards such as:
- KoffeeCraft Mug
- KoffeeCraft Teddy Bear
- 1kg Crafted Coffee Beans

These are reward-only products and are not intended to be part of the normal menu purchase flow.

They can be redeemed only from the `Rewards` screen.

### 5 Bean Booster milestone reward
I designed `5 Bean Booster` as a milestone-based manual claim reward.

It does **not** spend beans.

Instead:
- each customer has a stored next milestone threshold
- the first threshold starts at `10`
- when the customer reaches the threshold, the reward becomes claimable
- after claiming, the customer receives `+5 beans`
- the next threshold increases by `10`

Example flow:
- threshold = 10 → claim +5 beans
- next threshold = 20 → claim +5 beans
- next threshold = 30 → claim +5 beans

This prevents repeated claiming for the same milestone and creates a safer progression system.

### Database support
I extended the customer profile with:
- `nextBeansBonusThreshold`

I also added reward product seeding and migration support so the reward catalogue can be stored and reused consistently.

### Rewards screen UI
I expanded the `Rewards` screen beyond the original beans balance card.

The screen now includes:
- the premium beans balance card
- helper text explaining reward checkout behaviour
- a rewards list with action buttons
- reward eligibility based on available beans
- milestone reward claim support
- reward selection and add-to-cart actions

### Cart and checkout integration
I updated cart and checkout flow so they now:
- show reward items with `£0.00`
- calculate beans to spend separately from price total
- verify the customer has enough beans before checkout
- deduct beans only when checkout succeeds
- add earned beans based only on normal purchased products

### Visual direction
The rewards system follows the same premium KoffeeCraft direction as the rest of the updated app:
- rounded cards
- warm coffee-toned palette
- soft premium spacing
- clearer loyalty and redemption presentation
- product-style reward cards with room for future images

---

## Product structure redesign, premium extras flow, and customer Home dashboard update

In this stage, I improved both the internal product structure and the customer-facing Home experience.

### Product structure redesign

I changed the way products are represented so the project now separates:

- `productFamily`
  - `COFFEE`
  - `CAKE`
  - `MERCH`
- `rewardEnabled`

### Why I changed this

Previously, the product category concept was carrying too much responsibility.  
It was being used at the same time to describe what the product is and how it should behave in the rewards logic.

I changed this because I wanted the structure to be cleaner and more scalable.

Now:
- `productFamily` describes what the product actually is
- `rewardEnabled` describes whether the product can be used in rewards

This improves the architecture because:
- coffee and cake products can still behave like normal menu products
- coffee and cake can also become reward-eligible without needing fake duplicate categories
- merch reward items can exist as their own proper product family
- the logic is easier to understand and easier to maintain later

### Database and query update

To support this change, I updated the Room layer.

I added:
- support for `rewardEnabled` in `Product`
- migration logic to move older reward-style products into the clearer `MERCH` structure
- updated product queries for:
  - normal menu loading
  - reward loading
  - active `NEW` products
  - family-based ordering

This means the database now supports a more professional separation between product identity and reward behaviour.

---

## Admin product management improvement

I redesigned the admin product form so the admin can now clearly choose:

- the product family
- whether the product should also be reward-enabled

This makes the admin workflow more explicit and reduces confusion during product creation.

### What improved

This change improved the admin experience because:
- product creation is now more intentional
- reward behaviour is easier to configure
- the structure matches the real business logic better
- future filtering and reporting will be easier to maintain

---

## Premium extras management redesign

I also redesigned the extras management flow.

Previously, extras management was closer to a technical checkbox-style interaction.  
At this stage, I changed it into a more structured admin flow.

The new `Manage extras` approach separates:

- extras assigned to the current product
- extras available in the shared library for that product family

### Admin actions now supported more clearly

The admin can now:
- assign an extra to the current product
- remove an extra from the current product without deleting it globally
- edit extra details
- enable / disable extras in the shared library
- create new extras directly from the management flow
- permanently delete an extra only when needed

### Why this was important

I changed this because extras are now part of several important systems in the app:

- product customisation
- final price calculation
- estimated calories
- favourite preset combinations
- premium product presentation

Because extras affect both data and user experience, I wanted their admin flow to feel more like a proper management panel and less like a temporary utility dialog.

### What improved

This redesign improved the project because:
- assignment logic is clearer
- the difference between remove / disable / delete is now easier to understand
- extras are easier to maintain across multiple products
- the admin experience feels more premium and more professional

---

## Customer Home dashboard redesign

I redesigned the customer Home screen into a richer dashboard experience.

Instead of leaving Home as a basic placeholder, I added multiple premium sections that help the customer discover rewards and products more naturally.

### Final Home structure in this stage

The Home screen now contains these sections in this order:

1. Beans balance card
2. Rewards preview carousel
3. New arrivals carousel
4. Recommended coffees carousel
5. Recommended cakes carousel

### Beans balance card

I added a compact beans card at the top of the Home screen.

It shows:
- the customer bean balance
- a short milestone / reward progress message

I also removed the extra button from this card and made the whole card clickable.

### Why I changed that interaction

I removed the button because the card looked too tall and visually heavy.  
Making the whole card clickable made the interaction cleaner and more premium.

This improved the screen because:
- the layout became more compact
- the card became easier to scan
- the user still has direct access to Rewards with fewer visual distractions

---

## Rewards preview section

I added a rewards preview carousel directly on Home.

This section helps the user discover:
- milestone-style reward logic
- free coffee reward
- free cake reward
- merch reward items

The full rewards screen still remains the main destination, but Home now acts as a discovery entry point.

### What improved

This makes the loyalty system more visible and more engaging because the customer can immediately see that rewards exist without first navigating manually.

---

## New arrivals section

I added a new arrivals carousel based on products marked with the admin-controlled `NEW` flag.

### Why this matters

This gives the Home screen a more dynamic feel and allows admin product highlighting to become visible immediately in the customer experience.

It also helps prevent the Home screen from feeling static.

---

## Recommended coffee and cake sections

I added two separate recommendation carousels:

- recommended coffees
- recommended cakes

These recommendations are based on product feedback.

### Recommendation rules

To make the recommendations more reliable, I applied these rules:

- only `COFFEE` products appear in the coffee section
- only `CAKE` products appear in the cake section
- only active products can appear
- products must have at least `3` ratings
- the top `3` products are selected
- ordering is based on:
  1. average rating
  2. rating count
  3. product name

### Why I used a minimum rating threshold

I used a minimum of `3` ratings so that a single very high score would not dominate the recommendations unfairly.

This makes the recommendation logic more believable and gives the customer a more trustworthy discovery experience.

---

## Home layout and visual refinement

I also refined the Home screen visually so it better matches the KoffeeCraft premium direction.

I changed the Home screen to use:
- a milk-coffee background tone instead of plain white
- softer rounded cards
- more compact section spacing
- wider horizontal carousel cards
- lower card height
- partial next-card visibility to suggest sideways scrolling

### Why this visual change matters

The previous version used too much vertical space and the carousel cards were too narrow and tall.

I changed this because I wanted:
- a more premium visual density
- more content visible on screen at once
- stronger carousel affordance
- a warmer and more polished first impression

### What improved

This improved the customer Home experience because:
- the screen feels more intentional and less empty
- the dashboard looks more like a real coffee app landing page
- the user can scan sections faster
- horizontal carousels now communicate their purpose better

---

## Summary of this stage

At this stage, I improved both architecture and user experience.

### Architecture improvements
- clearer product structure
- better reward configuration
- more maintainable extras management
- stronger separation of concerns

### Customer experience improvements
- richer and more useful Home screen
- better visibility of rewards
- clearer product discovery
- more premium layout and proportions

This stage moved the project closer to a polished assessment-ready product instead of a simple feature collection.


## My Orders premium refinement and crafted order history

I extended the earlier My Orders redesign into a more premium and more interactive customer history screen.

### What I changed
I added:
- a premium top section with clearer hierarchy
- a `Browse menu` action
- date filter chips for:
  - `All`
  - `Today`
  - `Yesterday`
  - `2 days ago`
  - `Last 7 days`
  - `Last 14 days`
  - `Earlier`
- softer premium full-width order cards
- inline expand / collapse behaviour inside the same card
- text-style `Buy again` action instead of a heavy button
- customer-side soft hide using:
  - swipe left
  - swipe right
  - `X` action

### Why I changed this
I changed this because the previous My Orders version was still functionally correct but visually too plain and too prototype-like for the current KoffeeCraft direction.

I wanted the screen to feel closer to a real premium mobile product by improving:
- information hierarchy
- spacing
- interaction clarity
- visual consistency with the rest of the customer app

### Soft hide design decision
I deliberately implemented removal from My Orders as a customer-side soft hide rather than deleting the real order record.

This means:
- the order disappears from the customer history view
- the real order data remains stored
- admin flow and order history integrity are not affected
- no unnecessary database deletion logic was introduced for this UI action

This made the feature safer and lower risk.

### Crafted order indicator
I also added a `Crafted` badge for customised orders.

The badge appears when an order includes at least one product that was customised using:
- size or portion selection
- add-ons
- stored customisation metadata

I positioned the badge below the order number and above the order date so it becomes part of the visual identity of the card instead of competing with the status chip.

### Order data persistence improvement
To support the crafted history behaviour correctly, I updated order placement so customised cart metadata is stored in `order_items`.

This now preserves:
- selected option label
- selected option size value
- selected option size unit
- selected add-on summary
- estimated calories

### Result
The My Orders screen now:
- looks more premium
- supports faster scanning
- supports date-based filtering
- supports safer customer-side history cleanup
- shows richer inline order details
- highlights customised orders more clearly

### Files involved
- `OrdersFragment.kt`
- `OrdersAdapter.kt`
- `OrderItemDao.kt`
- `OrderRepository.kt`
- `CheckoutFragment.kt`
- `HiddenOrdersStore.kt`
- `fragment_orders.xml`
- `item_order.xml`
- `item_order_detail.xml`
- `bg_orders_filter_chip.xml`
- `bg_orders_filter_chip_selected.xml`
- `bg_order_status_chip.xml`
- `bg_order_crafted_chip.xml`
- `bg_order_delete_circle.xml`

## Customer Favourites redesign

I redesigned the customer Favourites screen so it now behaves more like a premium mobile product instead of a collection of temporary utility actions.

### Screen structure
I kept the favourites area split into two separate sections:
- `Saved custom favourites`
- `Standard favourites`

This preserves the difference between:
- fully saved customised configurations
- normal favourited products

### Saved custom favourites
I changed saved custom favourites from a dialog-based interaction into expandable inline cards.

Each saved preset card now:
- shows the product name
- shows saved size information
- shows saved add-ons
- shows calories
- shows price
- supports inline expand / collapse in the same list item

I removed the old popup-style details flow because it felt less premium and interrupted the browsing experience.

I also removed the extra `Close` action because tapping the same card again already collapses it, which keeps the interaction cleaner.

### Long add-on handling
I improved the saved preset card layout so long add-on summaries wrap correctly instead of:
- overlapping the label area
- overflowing outside the right edge of the card

This was important because custom favourites can contain many extras and the layout needed to stay stable for larger content.

### Standard favourites
I separated standard favourites from the shared menu adapter and gave them their own favourites-specific presentation.

Each standard favourite now uses:
- a premium expandable card
- a text-style `Customize` action
- a text-style `Buy again` action
- a `Remove` action inside the expanded area

I intentionally kept `Customize` and `Buy again` visible in the collapsed card and limited the expanded card to `Remove` only, so actions are not duplicated.

### Expanded standard favourite information
I changed the expanded information for standard favourites so it now shows:
- `Standard size`
- `Calories`
- `From`

instead of the earlier:
- `Family`
- `Availability`

To support this, I extended the favourites data query to read the default product option from `product_options`.

### Technical direction
For standard favourites, I introduced a dedicated projection model and query rather than reusing the generic product menu adapter.

This keeps the favourites screen more specialised and avoids breaking menu behaviour in other parts of the app.

### Files involved
- `CustomerFavouritesFragment.kt`
- `CustomerFavouritePresetAdapter.kt`
- `StandardFavouriteAdapter.kt`
- `FavouriteDao.kt`
- `fragment_customer_favourites.xml`
- `item_customer_favourite_preset.xml`
- `item_customer_standard_favourite.xml`


## Customer Notifications redesign

I redesigned the customer notifications screen so it now matches the premium visual direction of the rest of KoffeeCraft.

### What I changed
I replaced the basic notification list presentation with a more premium card-based layout.

Each customer notification card now supports:
- a premium rounded card style
- a styled status chip
- order number
- a short order status sentence
- total order price in the collapsed state
- inline expand / collapse behaviour
- swipe left and swipe right delete
- a premium `X` remove action

### Expanded notification details
When expanded, each notification now shows the ordered products in a structured layout instead of a plain text block.

Each ordered item now displays:
- product name
- `Crafted` badge when the item was customised
- quantity
- price per item
- total line price

This makes the notification screen much easier to scan and much closer to a real premium mobile ordering app.

### Why I changed this
The earlier notification screen was functionally correct, but the presentation was too basic and too close to a prototype.

It relied on:
- simple title/message text
- a raw details block
- minimal card styling

This did not match the stronger premium direction already used in:
- My Orders
- Favourites
- other customer-facing screens

### Design decisions
I kept delete behaviour lightweight and consistent by supporting:
- swipe left
- swipe right
- direct remove using the `X` action

I also kept notifications expandable inline rather than opening a new screen, because this makes order updates faster to check and keeps the flow lightweight.

### Result
The Customer Notifications screen now:
- looks more polished
- feels more premium
- gives better visibility of order progress
- shows meaningful order details without leaving the screen
- visually matches the rest of the customer app much better

### Files involved
- `CustomerNotificationsFragment.kt`
- `CustomerNotificationsAdapter.kt`
- `fragment_customer_notifications.xml`
- `item_customer_notification.xml`
- `item_customer_notification_detail.xml`

## Cart, Checkout, Order Status, and Feedback premium customer flow update

I redesigned the late-stage customer order flow so the transition from cart to payment, order tracking, and feedback now feels much closer to a real premium mobile app.

### Cart redesign
I upgraded the Cart screen from a simple functional list into a more polished order review flow.

Each cart item now presents:
- product name
- `Crafted` badge for customised items
- size information
- add-ons
- calories
- price per item
- line total
- a cleaner quantity stepper with subtle `-` and `+` controls

I intentionally did not include allergen data in cart cards, because the screen should stay focused on ordering clarity rather than becoming overloaded.

I also redesigned the summary area so it now includes:
- `Order total`
- optional beans spending information
- `Proceed to checkout`
- `Browse menu`

### Top bar badge redesign
I upgraded the customer top bar counters for:
- cart
- inbox
- notifications

The badges now use a warmer premium visual style with:
- improved shape
- stronger readability
- removal of the old plain grey appearance

### Checkout redesign
I redesigned Checkout into a more premium payment confirmation screen.

I replaced the basic payment choice UI with two pill-style selectable options:
- `Card`
- `Cash`

This creates a cleaner and more modern interaction than traditional radio buttons.

I also added:
- a premium header card
- clearer payment method grouping
- a dedicated total summary card
- a stronger primary action using `Proceed to pay`

### Order Status redesign
I redesigned the order status screen so it now uses the same premium visual language as:
- My Orders
- Favourites
- Notifications
- Cart
- Checkout

The screen now includes:
- premium header card
- status chip
- order number
- current status display
- `Back to menu`
- `Leave feedback`

### Feedback availability behaviour
I changed the feedback action behaviour so the customer can always see the `Leave feedback` button, but it only becomes enabled when the order reaches `COLLECTED`.

This gives the user better visibility of the feature while still preserving the business rule around review timing.

### Feedback list redesign
I redesigned `Feedback for Order` into a premium product list.

Each purchased product card now shows:
- product name
- quantity
- price
- current rating state
- `Crafted` badge for customised purchased items
- `Leave feedback` or `Edit feedback`

### Product feedback redesign
I redesigned the individual product feedback screen into structured premium sections.

The screen now separates:
- product information
- crafted state
- order number and quantity
- rating input
- comment input
- action buttons

This gives the feedback flow a clearer hierarchy and a more finished product feel.

### Data support for crafted feedback
To support the premium feedback flow correctly, I extended the purchased-product feedback projection so the feedback UI can detect whether a purchased item was customised.

This makes it possible to show the `Crafted` badge consistently in:
- feedback product list
- individual product feedback screen

### Files involved
- `activity_main.xml`
- `fragment_cart.xml`
- `item_cart.xml`
- `CartAdapter.kt`
- `CartFragment.kt`
- `fragment_checkout.xml`
- `CheckoutFragment.kt`
- `fragment_order_status.xml`
- `OrderStatusFragment.kt`
- `OrderItemDao.kt`
- `fragment_feedback.xml`
- `FeedbackFragment.kt`
- `item_feedback_product.xml`
- `FeedbackProductsAdapter.kt`
- `fragment_product_feedback.xml`
- `ProductFeedbackFragment.kt`
- `bg_topbar_badge.xml`
- `bg_cart_quantity_button.xml`

## Customer Settings redesign

I redesigned the customer Settings area into a more complete premium account hub so it now feels consistent with the rest of the KoffeeCraft customer experience.

### Main settings structure
The main Settings screen now presents:
- customer full name
- customer email
- premium grouped sections for:
  - Account
  - Security
  - Inbox Preferences
  - Help & Policies
  - Danger Zone

This creates a clearer structure than the earlier minimal settings version and makes the screen feel closer to a real mobile product.

### Personal Info
I added a dedicated `Personal Info` screen where the customer can update:
- first name
- last name
- email

The fields are pre-filled with the current customer values instead of relying on placeholder text only.

I kept `Date of Birth` visible but read-only. This was an intentional decision so the stored birthday reward logic cannot be abused by repeatedly changing the birthday.

### Change Password
I added a dedicated `Change Password` screen with:
- current password field
- new password field
- live password validation chips

The live validation mirrors the registration-style rule feedback so the customer can immediately see whether the new password satisfies each requirement.

The password update flow verifies the current password first, then saves a newly generated password hash and salt.

### Inbox Preferences
I added a separate `Inbox Preferences` screen so the customer can change promotional message consent after registration.

This keeps the original registration choice intact while allowing the user to update that preference later from Settings.

### Help and policy content
I added separate Settings-linked screens for:
- Help
- Terms of Use
- Privacy Statement

To keep the project structure simpler, I used a shared info page pattern with different content based on the selected section.

### Delete Account
I added a dedicated `Delete Account` screen with:
- current password confirmation
- explicit warning
- delete and back actions

The deletion flow permanently removes the customer account and related customer-owned data from the local database.

### Design direction
Across all settings-related screens I used:
- premium warm coffee-tone background
- rounded cards
- softer premium buttons
- clear section grouping
- back navigation using a top-left `‹` pattern

This keeps Settings visually aligned with:
- Cart
- Checkout
- Order Status
- Feedback
- Notifications
- Favourites

### Files involved
- `CustomerSettingsFragment.kt`
- `fragment_customer_settings.xml`
- `CustomerPersonalInfoFragment.kt`
- `fragment_customer_personal_info.xml`
- `CustomerChangePasswordFragment.kt`
- `fragment_customer_change_password.xml`
- `CustomerInboxPreferencesFragment.kt`
- `fragment_customer_inbox_preferences.xml`
- `SettingsInfoPageFragment.kt`
- `fragment_settings_info_page.xml`
- `CustomerDeleteAccountFragment.kt`
- `fragment_customer_delete_account.xml`
- `nav_graph.xml`

## Customer Inbox redesign

I redesigned the customer inbox so it now matches the premium customer UI used across the rest of KoffeeCraft.

### Main inbox layout
The inbox screen now includes:
- a premium header card
- warm coffee-tone background styling
- grouped top filters for:
  - `All`
  - `Read`
  - `Unread`
  - `Promo`
  - `Important`
  - `Service`

This makes the inbox easier to scan and more consistent with other customer-facing screens such as:
- Notifications
- Cart
- Checkout
- Settings

### Inbox cards
Each inbox message now uses a more premium rounded card style and shows:
- title
- date and time
- read state chip
- optional category chip
- message body
- `Read more / Read less`
- remove `X`

### Category chip behaviour
I added category chip support based on the stored delivery type of the inbox message.

The inbox now shows:
- `Promo` for promotional messages
- `Important` for important admin messages
- `Service` for service-related messages

I intentionally kept the category chip hidden for:
- custom general messages
- welcome messages

This keeps the UI cleaner and avoids unnecessary badge clutter.

### Read and unread behaviour
I changed the inbox logic so messages are no longer marked as read immediately when the customer enters the screen.

Instead, a message becomes read when the customer actually opens the card.

This makes the `Read` and `Unread` filters meaningful and better reflects real customer behaviour.

### Admin compatibility
I did not change the admin inbox sending flow.

Customer inbox titles and delivery categories continue to come from the existing admin-side message generation logic, which keeps the inbox redesign compatible with the current admin system.

### Files involved
- `CustomerInboxFragment.kt`
- `CustomerInboxAdapter.kt`
- `fragment_customer_inbox.xml`
- `item_customer_inbox.xml`

## Menu and rewards system refinement

I refined both the customer Menu experience and the customer Rewards/Beans experience so they now feel more consistent with the premium KoffeeCraft UI and behave more realistically.

### Menu redesign
I redesigned the customer Menu so product interaction now happens directly inside the product card instead of relying on the earlier visible customise-button-first flow.

The new menu approach uses:
- premium warm background
- premium filter chips for category switching
- premium product cards with favourite heart support
- inline expand/collapse behaviour on card tap

Each product card now shows a cleaner collapsed state with:
- product name
- description
- standard size
- from price
- favourite heart

When expanded, the same card now displays:
- size choices
- extra add-ons
- live calories
- live allergens
- extras total
- final total
- save favourite combo
- add to cart

Only one product card stays expanded at a time, which keeps the menu cleaner and closer to a real customer app experience.

Unavailable products are also visually softened and marked as unavailable rather than being removed completely.

### Rewards screen polish
I kept the rewards logic structure but refined the presentation so the screen now better matches the rest of the app.

The rewards screen now includes:
- premium background
- dedicated `Rewards` title
- improved helper text
- subtle card strokes
- premium action buttons

This keeps the screen visually aligned with:
- Cart
- Checkout
- Inbox
- Settings
- Customer Home

### Bean booster redesign
I replaced the earlier user-facing threshold style with a progress-based booster model.

Instead of showing a large historic milestone such as a very high next threshold value, the app now presents:
- progress toward the next booster
- optional pending booster readiness

This is clearer and feels more natural in a customer rewards system.

The logic is now based on:
- earned beans contributing to progress
- progress rolling over every 10 earned beans
- pending boosters being stored until claimed
- spending beans not resetting booster progress

### Rewards and cart consistency
I fixed the reward flow so reward items are now treated differently from normal paid products inside the cart.

Reward items:
- should not inflate in quantity
- should reserve the correct number of beans
- should not distort cart badge totals
- should not distort the `Available now` / `Reserved in cart` bean summary

### Free coffee and free cake customisation direction
I extended the rewards flow toward a more premium free-product experience.

The intended reward behaviour now supports the idea that:
- the base reward item is free
- size upgrades may add extra cost
- add-ons may add extra cost
- total price is calculated from upgrades only, not from the full base product price

This creates a more realistic free reward customisation flow and matches the app’s broader customisation system.

### Home page bean card refinement
I also aligned the home page bean card more closely with the rewards experience by adding visual booster progress support.

This keeps the reward journey more visible without requiring the customer to open the rewards screen every time.

### Files involved
- `ProductDao.kt`
- `fragment_menu.xml`
- `item_product.xml`
- `MenuFragment.kt`
- `ProductAdapter.kt`
- `fragment_customer_rewards.xml`
- `CustomerRewardsFragment.kt`
- `RewardProductPickerAdapter.kt`
- `dialog_reward_product_picker.xml`
- `item_reward_product_picker.xml`
- `ProductCustomizationBottomSheet.kt`
- `CartManager.kt`
- `CheckoutFragment.kt`
- `CustomerHomeFragment.kt`
- `Customer.kt`
- `KoffeeCraftDatabase.kt`
- `BeansBoosterManager.kt`

---

## Rewards flow, crafted-state rules, and favourites UI refinement

I refined several connected parts of the customer experience to improve pricing correctness, crafted-state accuracy, and visual consistency.

### 1. Standard favourites card refinement

I updated the standard favourites card so the collapsed state now also shows calories.

#### What changed
- I added collapsed calories to standard favourites
- I kept the existing price/action layout
- I aligned the collapsed information density more closely with saved custom favourites

#### Why I changed it
I changed this because the standard favourites card looked visually lighter and less informative than the saved custom favourites card.  
Adding calories improves consistency and makes the favourites screen feel more complete.

---

### 2. Crafted-state rules in cart and orders

I refined the crafted-state logic so products are only treated as crafted when the user actually changes the default configuration.

#### Previous behaviour
Previously, a product could appear as crafted even when the user selected the default option and no add-ons.

#### New behaviour
A product is now treated as crafted only when at least one of the following is true:
- a non-default size/option is selected
- at least one add-on is selected

#### Technical decision
I kept this logic inside the existing cart/order flow rather than introducing a new Room field such as `isCustomised`.

#### Why I changed it
I changed it this way because:
- it fixes the visible crafted badge problem without a schema migration
- it keeps the behaviour aligned with the real user action
- it avoids marking standard products as customised when they are actually unchanged

#### Result
- standard menu products no longer show crafted badges incorrectly
- cart presentation is more truthful
- order history presentation is more accurate

---

### 3. Reward customisation pricing

I corrected reward pricing so free drink and free cake rewards now start from a zero-cost base item.

#### Previous behaviour
The reward customisation screen displayed the normal product base price even when the reward should have made the base item free.

#### New behaviour
The reward customisation flow now works like this:
- base reward product price = `£0.00`
- size upgrades add only the option surcharge
- add-ons add only their own surcharge

#### Why I changed it
I changed this because the previous behaviour was inconsistent with the intended business rule for rewards.  
The customer should only pay for upgrades or extras, not for the rewarded base item itself.

#### Result
This makes the reward system easier to understand and more consistent with the beans logic.

---

### 4. Reward picker UI consistency

I replaced the reward selection dialog with a dedicated bottom sheet.

#### Previous behaviour
The initial reward product choice used a regular dialog, while the next customisation step used a bottom sheet.  
This made the reward flow feel visually inconsistent.

#### New behaviour
Both reward steps now follow the same bottom-sheet interaction style:
- reward item selection opens as a bottom sheet
- reward customisation opens as a bottom sheet
- both use the same warm premium visual direction

#### Why I changed it
I changed this to improve UI consistency and make the reward journey feel like one connected flow instead of two different interaction patterns.

#### Files involved
- `CustomerRewardsFragment.kt`
- `RewardProductPickerBottomSheet.kt`
- `sheet_reward_product_picker.xml`
- `ProductCustomizationBottomSheet.kt`

---

### 5. Flow-level impact

These refinements affect several connected customer features:
- favourites
- cart
- reward selection
- reward customisation
- order history

---

## Feedback flow expansion from Order Status and My Orders

I extended the existing feedback system so customers can access it from more natural places in the order journey without introducing a second review flow.

### 1. Reusing the existing feedback architecture

I kept the existing feedback structure as the single source of truth:
- `FeedbackFragment` still acts as the order-level feedback entry point
- `ProductFeedbackFragment` still handles creating and editing product-level ratings and comments
- existing feedback persistence and edit behaviour remain unchanged

### Why I chose this approach
I chose this because it avoids duplicating review logic and reduces the risk of inconsistent behaviour between multiple feedback entry paths.

---

### 2. Feedback availability in Order Status

I improved the `Order Status` screen so it now communicates when feedback is available.

#### New behaviour
- feedback stays disabled before collection
- feedback becomes available when the order reaches `COLLECTED` or `COMPLETED`
- the screen now displays helper text explaining whether feedback is locked, available, partially completed, or already finished

### Why I changed it
I changed this because the previous flow expected the user to know that feedback only became available after collection, but the UI did not explain that clearly.

---

### 3. Feedback entry from My Orders

I added feedback access to the expanded card in `My Orders`.

#### New behaviour
Depending on the feedback state of the order, the customer now sees:
- `Leave feedback`
- `Continue feedback`
- `Edit feedback`

This action appears only when feedback is actually available for the order.

### Why I changed it
I changed this because `My Orders` is one of the most natural places for a user to revisit past purchases and decide to review them.  
This makes the feedback feature easier to discover and more aligned with normal customer behaviour.

---

### 4. Feedback progress awareness

I added lightweight order-level feedback summary handling in the orders UI.

#### Summary logic
For each order, I now evaluate:
- how many eligible paid products are available for feedback
- how many of those products already have feedback
- whether the order still has pending feedback items
- whether the order is fully reviewed

### Result
This allows the UI to present more accurate feedback states instead of showing one static action for every order.

---

### 5. Navigation refinement after saving feedback

I updated the post-save navigation in the product feedback flow.

#### Previous behaviour
After saving feedback, the user could be pushed back to the menu.

#### New behaviour
After saving feedback, the app now returns the user to the feedback list for the same order.

### Why I changed it
I changed this because it keeps the user inside the same review journey and makes multi-item feedback much smoother.

---

### 6. Design impact

These changes improve:
- discoverability of feedback
- consistency of the customer order journey
- reuse of existing review logic
- product review completion flow

---

## Home recommendation logic refinement and new Most Loved carousel

I refined the recommendation structure on the customer home page so each carousel now represents a clearer and more consistent signal.

### 1. Recommended Coffees

I kept `Recommended Coffees` based only on customer star ratings.

#### New rule
- only products from the `COFFEE` family are included
- only active products are included
- products need at least `1` rating
- results are ordered by:
  - average rating descending
  - rating count descending
  - product name ascending
- the carousel shows up to `3` products

### Why I changed it
I changed this because the previous threshold of `3` minimum ratings was too restrictive for the current dataset and could leave the carousel nearly empty even when rated products already existed.

---

### 2. Recommended Cakes

I kept `Recommended Cakes` based only on customer star ratings.

#### New rule
- only products from the `CAKE` family are included
- only active products are included
- products need at least `1` rating
- results are ordered by:
  - average rating descending
  - rating count descending
  - product name ascending
- the carousel shows up to `3` products

### Why I changed it
I changed this for the same reason as coffees.  
I wanted the cakes recommendation carousel to reflect real customer ratings without being blocked by an unnecessarily high threshold.

---

### 3. Most Loved Products

I added a new carousel called `Most Loved Products`.

#### Purpose
This carousel is designed to represent customer affection through favourites rather than ratings.

#### Rule
- it includes both `COFFEE` and `CAKE` products in the same carousel
- only active products are included
- results are ordered by:
  - favourite count descending
  - product name ascending
- the carousel shows up to `5` products

### Why I changed it
I added this because favourites and ratings represent two different signals:
- ratings show product quality feedback
- favourites show ongoing customer preference and attachment

Keeping them separate makes the home page more understandable and gives the recommendations a more professional structure.

---

### 4. Home page information architecture

The home page recommendation area now has three distinct recommendation types:

1. `Recommended Coffees` → rating-based coffee recommendations
2. `Recommended Cakes` → rating-based cake recommendations
3. `Most Loved Products` → favourites-based mixed coffee and cake recommendations

---

### 5. UI impact

I extended the home screen layout to support the new carousel and its empty state.

#### Added/updated home sections
- recommended coffees
- recommended cakes
- most loved products


## Admin settings and internal admin access management update

I redesigned the admin settings area into a more structured premium control hub so it matches the quality and visual direction of the customer-facing settings screens.

Instead of using a simple list of utility actions, I grouped the screen into clearer functional sections:

- **Access & Roles**
- **Communications**
- **System Controls**
- **Session**

This improves navigation clarity and makes the admin side feel more like a real production back-office rather than a basic coursework utility screen.

### Create Admin Account flow

I added a new **Create Admin Account** flow as an internal-only admin management feature.

The new screen allows an existing admin to create another admin profile with:

- full name
- email
- phone
- username
- password
- confirm password
- active/inactive status on creation

I kept the admin sign-in flow based on email to avoid breaking the existing authentication logic, while still extending the admin profile model to support richer internal account management.

### Admin data model and Room update

To support this properly, I extended the `Admin` Room entity with additional profile and access-control fields:

- `fullName`
- `phone`
- `username`
- `isActive`

I also updated the Room migration path and seed logic so existing databases can move forward safely without losing the original seeded admin account.

This change improves the long-term maintainability of admin access features and prepares the app for more realistic internal account administration.

### Account Access Center

I expanded the previous customer-only account management area into a more complete **Account Access Center**.

This screen now supports both:

- **Customers**
- **Admins**

through a segmented access model.

Customer account actions remain focused on account status and deletion, while admin account actions now include:

- **activation**
- **deactivation**
- **password reset**



### Admin access protection rules

I added important safety rules for admin account management:

- the currently signed-in admin cannot deactivate their own account from the management screen
- the system prevents the last active admin account from being disabled

These checks reduce the risk of locking the app out of administrative access and make the internal account logic more robust.


### Session handling improvement

I improved session handling by extending `SessionManager` to track the current admin ID in addition to the existing admin/customer role state.

This supports the new account-protection rules and creates a cleaner foundation for future admin profile features.


### Admin settings UX refinement

I refined the interaction style of the Admin Settings screen by removing inline action buttons such as **Open** and replacing them with fully tappable settings rows.

This gives the interface a cleaner premium feel, reduces visual clutter, and keeps the admin settings UX consistent with the polished KoffeeCraft design language.



## Admin feedback redesign into Feedback Insights dashboard

I redesigned the admin feedback area into a full **Feedback Insights** dashboard so the screen feels more like a real analytics and moderation panel rather than a simple review list.

The goal of this change was to improve both the visual quality and the usefulness of the admin-side feedback workflow. Instead of forcing the admin to scan raw review entries first, the screen now opens with a summary of key feedback metrics and a clearer view of customer sentiment.

### New feedback overview section

At the top of the screen, I added a premium summary area with six overview cards:

- **Overall Average**
- **Coffee Average**
- **Cake Average**
- **Total Reviews**
- **Reviews With Comments**
- **Hidden Comments**

This gives the admin immediate visibility into the most important feedback statistics without needing to search or filter manually.

### Rating breakdown section

I added a dedicated **Rating Breakdown** card that shows the distribution of:

- **5★**
- **4★**
- **3★**
- **2★**
- **1★**

Each rating row uses a progress-style visual layout so the admin can quickly understand overall sentiment trends. This makes the screen feel more like a true management dashboard and also supports the assessment requirement that the admin should be able to review feedback and see rating information in a meaningful way.

### Filter redesign

I removed the previous spinner-based filter controls and replaced them with a more premium chip-style filtering system.

The new filter areas cover:

- **Rating**
- **Comment**
- **Category**
- **Sort**

This change improves usability, reduces the technical “school project” feeling of the screen, and keeps the admin interface visually aligned with the rest of the more polished KoffeeCraft UI.

### Review feed redesign

I upgraded the old feedback list into a more premium review feed using expandable cards.

Each feedback card now shows:

- product name
- product category
- rating
- visibility state
- customer ID
- order ID
- review date
- comment preview

When expanded, the card reveals:

- full review content
- moderation state
- updated date
- moderation actions

This gives the admin a cleaner moderation workflow while keeping the list visually elegant and easier to scan.

### Moderation action refinement

I replaced the older utility-style feedback actions with softer premium moderation controls.

Instead of relying on plain technical buttons, the expanded review cards now present more polished actions for:

- **Hide / Unhide comment**
- **Delete review**

This improves the visual consistency of the admin side and keeps the moderation flow aligned with the premium design language used across the rest of the app.

### DAO and data support for insights

To support the new screen properly, I extended `FeedbackDao` with additional queries for:

- feedback overview values
- rating breakdown values

This means the admin feedback screen is no longer only a review list. It now acts as a lightweight analytics surface that combines moderation and insight in one place.

### Stability fix

During implementation, I also fixed a crash caused by incorrect empty-state view binding in the admin feedback screen.

The layout used a `MaterialCardView` as the empty-state container, but the fragment was binding it as a `TextView`. I corrected the binding to match the actual view type, which resolved the crash when opening the screen.

## Persistent session and customer cart restore update

I added a persistent session layer so the app can restore the correct signed-in state after restart instead of relying only on in-memory session values.

### Persistent login session

I introduced a dedicated remembered session store using `SharedPreferences`.

The remembered session now saves:

- `userId`
- `role`
- `isLoggedIn`
- `onboardingPending`

This allows the app to reopen the correct flow after restart:

- **CUSTOMER** → customer flow
- **CUSTOMER with unfinished onboarding** → onboarding flow
- **ADMIN** → admin flow

### Session validation on startup

I updated the startup logic so the app does not trust remembered session data blindly.

When the app starts, it now:

1. reads the remembered session
2. restores the role and user ID into memory
3. validates the remembered account against the current database state
4. clears the remembered session if the account no longer exists or is inactive

This makes the session restore flow more robust and prevents invalid or outdated session data from reopening protected app areas.

### Onboarding resume support

I extended the remembered customer session with an `onboardingPending` flag.

This means that if a newly registered customer closes the app before finishing onboarding, the app can reopen the onboarding flow correctly instead of sending the user directly to the main customer area.

Once onboarding is completed, the remembered session is updated so the normal customer flow is restored on future launches.

### Persistent customer cart

I added a dedicated persistent cart store so the customer cart is no longer memory-only.

The cart is now saved per customer account using snapshot-based cart persistence.  
Each stored cart line keeps the technical data needed for safe reconstruction, including:

- product ID
- quantity
- unit price
- reward state
- reward type
- beans cost
- selected option ID
- selected option display data
- selected add-on IDs
- add-on summary
- estimated calories

This allows the app to restore both standard and customised cart lines more reliably.

### Cart restore validation

I designed the cart restore flow to validate stored cart lines against the current database state before restoring them.

During restore, the app checks whether:

- the product still exists
- the product is still active
- the selected option still exists and belongs to the product
- the selected add-ons are still valid and assigned to the product
- reward items are still reward-eligible

If a stored line is no longer valid, it is skipped instead of being restored blindly.

This keeps the cart consistent with the live app data and reduces the risk of broken cart state after menu changes.

### Customer-only cart restore

The persistent cart restore logic only applies to the **customer** role.

The admin side does not have its own cart and does not restore any cart data.  
When entering admin flow, I only clear any residual in-memory customer cart state to keep role separation clean and prevent stale customer cart state from remaining in memory.

### Logout and account removal consistency

I updated logout and account removal behaviour so session state and in-memory cart state are cleared consistently.

This helps keep the app lifecycle cleaner and ensures that remembered login and cart behaviour stay predictable across:

- logout
- restart
- onboarding completion
- account deletion
- role switching


## Premium order confirmation, smart notification routing, and admin back-office dashboard update

At this stage, I extended three important areas of the application so the product feels more polished, more realistic, and more portfolio-ready:

- customer order completion flow
- notification behaviour
- admin home dashboard

These changes improved both the premium visual quality of the app and the strength of the operational flows behind it.

### Order confirmation and tracking improvement

I upgraded the customer order completion experience by redesigning `OrderStatusFragment` into a combined **confirmation + tracking** screen instead of keeping it as a more technical status-only screen.

The updated order status flow now gives the customer a clearer success moment after checkout by showing:

- a stronger confirmation hero state
- order number
- items ordered
- total paid
- payment type
- estimated ready time

I kept the screen useful after checkout as well, so it still works as the live order tracking screen and not only as a one-time confirmation view.

This improved the overall UX quality of the ordering journey and made the post-payment flow feel more like a real premium mobile app.

### Notification system improvement

I upgraded the local notification system so it behaves more like a real production mobile app instead of acting only as a simple alert mechanism.

I introduced two clearer notification categories:

- **Order Updates**
- **Offers & Promotions**

This creates a better separation between operational notifications and marketing communication.

I also added notification deep-link routing so tapping a notification now opens the correct destination inside the app:

- order notifications open the relevant `OrderStatusFragment`
- promo notifications open `CustomerInboxFragment`

This made the notification flow much more useful and realistic because notifications now act as meaningful entry points into the app rather than just passive messages.

### Read-state and badge behaviour improvement

I refined the notification and inbox behaviour so unread state is better aligned with actual user actions.

Instead of depending only on broad mark-all-read behaviour, the app now reacts more intelligently to notification taps and targeted entry points. This improves consistency between:

- what the user opens
- what becomes read
- what remains visible in unread badges

The result is a cleaner and more believable messaging experience across customer notifications and inbox flows.

### Admin dashboard redesign

I redesigned the admin home screen into a more complete premium **back-office dashboard**.

The older admin home structure had useful analytics content, but it felt more like a technical statistics screen than a real management dashboard. I replaced that structure with a stronger hierarchy focused on operational clarity and commercial insight.

The updated dashboard now includes a KPI section for:

- **Orders Today**
- **Revenue Today**
- **Pending Orders**
- **Average Rating**
- **Promo Opt-In Rate**
- **Active Customers**

This gives the admin immediate visibility into the most important live metrics without needing to enter separate screens.

### Menu Health section

I added a dedicated **Menu Health** section so the admin can quickly assess the current state of the live menu.

This section now shows:

- available products
- disabled products
- reward-enabled live products
- new arrivals currently live

Because the current data model does not include true stock quantity, I intentionally used menu-health metrics instead of pretending to support low-stock tracking. This keeps the dashboard aligned with the real capabilities of the current implementation.

### Quick Actions redesign

I added a more useful **Quick Actions** area using premium tappable cards rather than small technical controls.

The admin can now jump quickly to the most important operational destinations:

- Manage Menu
- View Orders
- Send Promo
- Review Feedback
- Manage Customers
- Admin Settings

This makes the home screen feel much more like a real back-office control center.

### Insight carousel refinement

I kept the carousel idea from the previous admin home version, but I refined it into a more focused and premium structure.

Instead of keeping many weaker or more fragmented carousels, I reduced the insight area to three stronger sections:

- **Top Rated Products**
- **Most Loved Products**
- **Most Commented Products**

This keeps the dashboard more readable and gives more weight to the insights that are most valuable from a business and customer-experience perspective.

### Needs Attention section

I added a **Needs Attention** section so the admin can see short operational highlights immediately.

This area now surfaces actionable summaries such as:

- pending active orders
- disabled products
- hidden feedback comments
- promo consent coverage

This helps the dashboard feel more alive and more decision-oriented instead of being only a passive reporting screen.

### DAO support for dashboard metrics

To support the new admin dashboard properly, I extended the DAO layer with additional summary queries for:

- order activity
- daily revenue
- pending orders
- active customers
- promo opt-in counts
- menu health counts

This means the dashboard is driven by real live data from the current database model rather than by placeholder assumptions.


## Premium admin menu refinement and Campaign Studio navigation upgrade

At this stage, I improved two important admin-side areas:

- the Admin Menu product management experience
- the admin communication / campaign navigation structure

These changes made the admin side feel less like a basic coursework utility screen and more like a polished premium back-office experience.

### Admin Menu redesign

I refined the Admin Menu screen so it is now visually more consistent with the rest of the KoffeeCraft application.

The screen now uses the same warm premium coffee palette, softer spacing, rounded cards, and a more elegant structure for browsing and managing products.

I also adjusted the action hierarchy so product cards are now cleaner in their collapsed state.

Instead of showing action buttons immediately on every product tile, the card now stays visually lightweight until the admin intentionally opens it.

### Expandable product management cards

I changed the admin product cards so the main product tile now works as an expandable management surface.

The updated behaviour is:

- tapping a product expands it inline
- tapping another product collapses the previously opened one
- only one product stays expanded at a time

This makes the menu management flow more readable and more premium, while also reducing visual noise in long admin product lists.

### Action placement improvement

I moved product actions into the expanded state of the product card.

This means the collapsed card is now focused on fast scanning and identification, while the expanded state becomes the operational workspace for that product.

The admin actions remain available, but they now appear in a more controlled and intentional layout.

This improves both usability and visual quality.

### Configuration tools polish

I improved the visual presentation of the product configuration area that opens from the admin menu.

The Product Details dialog was updated to use:

- warmer premium background tones
- more consistent card sections
- cleaner typography hierarchy
- softer and more polished action buttons

I then extended that same visual treatment to the supporting configuration dialogs for:

- size / portion setup
- extras management
- allergen setup

This brings the configuration flow closer to the same design language used across the premium customer-facing parts of the app.

### Admin navigation restructure

I also improved the admin navigation structure to better separate communication history from campaign creation.

Previously, Inbox occupied a main bottom-navigation position.

I changed that structure so:

- Inbox is now part of the top-bar utility actions
- Studio becomes a main bottom-navigation destination

This creates a clearer distinction between:

- quick communication tools in the top bar
- strategic admin workspaces in the bottom navigation

### Campaign Studio shell

I introduced the first navigation shell for a new admin feature called **Campaign Studio**.

This screen is now positioned as a main admin destination and is intended to support future functionality such as:

- smart promo targeting
- loyalty campaigns
- targeted beans rewards
- audience preview before send

At this stage, the Studio screen is structural and visual, prepared for the next development phase where real targeting logic and campaign execution will be added.

### Why this improves the architecture

These changes improve the architecture in three ways:

1. Admin product management now follows a clearer expand-to-operate pattern instead of exposing too many actions at once.
2. Product configuration tools are now visually and structurally aligned with the premium app identity.
3. Admin communication is better separated into:
  - Inbox as a utility action
  - Studio as a dedicated campaign workspace

This makes the admin side more scalable and more suitable for additional premium management features in later stages.


## Separation of campaign workflows and direct admin messaging

At this stage, I refined the communication architecture on the admin side so the app no longer repeats similar behaviour across multiple screens.

### Campaign Studio responsibility

I kept **Campaign Studio** as the dedicated workspace for campaign-based communication.

This screen is now responsible only for:

- promotional offers
- bonus beans rewards
- combined offer + bonus beans campaigns
- targeted audience selection
- preview-based campaign sending

This makes Studio behave like a premium loyalty and campaign tool rather than a general-purpose inbox screen.

### Admin Inbox responsibility

I redefined **Admin Inbox** as a direct operational messaging tool instead of a second campaign screen.

The Inbox now focuses on one-to-one communication only.

The admin can now find a recipient by:

- order number
- customer ID

and send a direct message using operational templates such as:

- Important
- Service
- Custom

This creates a much cleaner separation between direct support-style communication and campaign management.

### Removal of duplicated messaging behaviour

Before this change, the communication layer had overlapping behaviour between:

- campaign-style sending
- inbox-style sending

This made the admin side feel less focused and less premium.

I removed that overlap by making sure:

- promotions and loyalty rewards live only in Studio
- important and service communication live only in Inbox

This improves role clarity across the admin tools and avoids duplicated UX patterns inside the app.

### Inbox UI refinement

I also redesigned the Inbox screen so it visually matches the rest of the premium KoffeeCraft admin experience.

The new Inbox structure includes:

- a premium header card
- direct target lookup section
- recipient summary card
- message type section
- compose message section
- large premium send button

This creates a more realistic production-style admin communication flow and makes the feature feel more aligned with a professional back-office app.

### Architectural result

This change improves the communication architecture in the following way:

- **Campaign Studio** = campaign targeting, loyalty, beans, promotions
- **Inbox / Direct Message Centre** = direct operational communication

That separation makes the admin side more scalable, easier to reason about, and much more polished from both a UX and portfolio perspective.


## Premium redesign of the admin notifications flow

At this stage, I refined the **Admin Notifications** screen so it now feels visually and structurally aligned with the rest of the premium KoffeeCraft admin experience.

### What I changed

I redesigned the screen from a plain operational list into a more polished **action queue** view.

The updated notifications area now includes:

- a premium header card
- a queue summary section
- warmer coffee-tone cards
- clearer information hierarchy
- status chips for order progress
- more readable metadata
- cleaner action buttons for operational next steps

### Notification item improvements

I rebuilt each admin notification card so it now presents information in a more production-style layout.

Each card now gives the admin a clearer visual understanding of:

- the current order status
- what action is needed
- when the notification was created
- whether the item is still actionable or ready to be cleared

I also improved the action wording to make the screen feel more realistic and task-oriented.

The main action labels now reflect the actual order progression:

- Move to Preparing
- Move to Ready
- Mark as Collected

### What I kept unchanged

I kept the existing logic intact so this was a UX and presentation improvement rather than a behavioural rewrite.

That means:

- only collected notifications can be removed
- swipe delete still works only for collected notifications
- placed / preparing / ready notifications still use the same next-step order progression logic
- admin notifications are still marked as read when the screen is opened

### Why this improves the architecture and UX

This change improves the admin-side experience in two ways.

First, it makes the notifications screen feel like a real **operational queue**, which is more suitable for a premium back-office coffee app.

Second, it keeps the behaviour stable while improving the visual structure, so the feature now fits much better alongside the already refined admin areas such as:

- dashboard
- orders
- menu management
- inbox / direct messaging
- campaign studio

As a result, the notifications feature now supports the same premium product identity instead of feeling like a temporary utility screen.

## Admin flow cleanup and login architecture refactor

I cleaned up the remaining legacy admin navigation layer so the project now reflects the real runtime structure more accurately.

Previously, the app still contained an older admin navigation path inside the main customer/auth navigation graph, even though the actual admin experience had already been moved to `AdminActivity` with its own `admin_nav_graph`. I removed that old path to avoid having two architectural approaches to the same admin flow. This made the separation between customer navigation and admin navigation much clearer and reduced confusion in the project structure.

I also aligned the Admin Home quick action flow with the intended admin communication model. The promo-related quick action previously opened Admin Inbox, which no longer matched the current separation of responsibilities. I changed that entry so it now opens Campaign Studio instead. This keeps the admin messaging structure more consistent:
- Admin Inbox is used for direct 1:1 communication
- Campaign Studio is used for targeted campaigns, offers, loyalty actions, and bonus beans

I then refactored the login architecture because authentication responsibilities were split across multiple layers. Although `AuthRepository` and `LoginViewModel` already existed, `LoginFragment` was still performing a large part of the authentication work directly. I moved the core login logic into `AuthRepository` and made `LoginViewModel` responsible for exposing login state, success results, and errors. After that, I reduced `LoginFragment` to UI-level responsibilities only, such as:
- validating input fields
- rendering loading and error state
- performing Android-specific actions after successful login, such as session persistence, cart restoration, navigation, and launching `AdminActivity`

This refactor improved architectural consistency and reduced duplicated logic. It also made the login flow easier to maintain and easier to extend in the future without having authentication rules scattered across the fragment and repository layers.

Overall impact:
- cleaner separation between customer/auth flow and admin flow
- cleaner separation between Admin Inbox and Campaign Studio
- removal of obsolete legacy navigation remnants
- a more maintainable and production-like login architecture
- stronger architectural consistency for both assessment quality and portfolio quality


## Customer payment methods and premium checkout payment flow

I added a new customer-side payment methods module to make the checkout experience feel more realistic and more aligned with a premium mobile app.

Previously, the app only supported a simple checkout payment choice without any saved customer payment methods. I introduced a dedicated local wallet flow for demo cards by creating a new `CustomerPaymentCard` entity with its own DAO and database migration. This separated reusable customer payment methods from the existing `Payment` order record model, which keeps the architecture cleaner and avoids mixing saved cards with per-order payment history.

I extended the customer settings area with a new `Payment Methods` entry and added a dedicated payment methods screen. This screen allows the customer to:
- view saved cards
- add a new demo card
- set a default card
- remove saved cards

I designed the saved card flow to fit the existing premium coffee theme by using warm neutral colours, rounded cards, and a live card preview rather than a plain utility form. I also added a top back action so the payment methods screen now behaves consistently with the other customer settings sub-screens.

For the card form itself, I introduced a reusable validation and formatting utility. I kept the overall form structure close to a real mobile payment form, including:
- nickname
- cardholder name
- card number
- expiry
- CVV

However, because this project uses a simulated payment experience rather than a real banking integration, I simplified the card number validation so that any 16-digit value is accepted. This keeps the feature practical for the coursework demo while still preserving a realistic user interface. I also improved the validation feedback so the user gets clearer guidance when a field is incomplete or incorrectly formatted.

I then upgraded checkout so the payment experience is more complete and better integrated with the settings wallet flow. Checkout now supports three payment types:
- Card
- Apple Pay
- Cash

When Card is selected, checkout now shows:
- the customer’s saved cards
- automatic selection of the default card
- the same premium new-card form used in payment methods

This means the customer can either use a saved card or enter a new one during checkout. I also added an option to save a newly entered checkout card for future orders, which improves continuity between checkout and account settings.

I treated Apple Pay differently from a standard card form because real applications usually present Apple Pay as a distinct payment experience rather than as a manual card-entry flow. In this project it is still a simulated local flow, but it now has its own dedicated UI block and payment type value, which makes the checkout architecture more realistic and easier to extend later.

Overall impact:
- introduced a dedicated saved-cards wallet structure for customers
- improved separation between saved payment methods and per-order payment records
- made checkout more realistic and more portfolio-ready
- increased consistency between settings and checkout
- strengthened the premium feel of the customer experience without adding a real payment gateway

## Orders Control search refinement and scrolling adjustments

I refined the Orders Control search flow so it is more consistent with the way the admin inbox already works.

Previously, the orders screen used a single search field that tried to match multiple identifier types at once. This made the behaviour less explicit and less aligned with the rest of the admin experience. I changed that so the screen now separates search intent more clearly by introducing dedicated search mode chips for:
- Order ID
- Customer ID

I also added a dedicated Find action so the screen behaves more like an intentional admin lookup tool rather than filtering too broadly from one shared search path. This makes the search flow easier to understand and closer to the inbox interaction pattern.

At the layout level, I also adjusted the Orders Control screen structure to improve overall scroll behaviour and make the screen easier to use. The goal was to keep the full screen naturally scrollable while preserving the premium filter section and improving general usability.

This area still needs another optimisation pass because the screen can still feel heavy in some situations. The search and scrolling behaviour is now more usable, but the remaining stutter should still be investigated further.

Overall impact:
- improved clarity of the Orders Control search flow
- better consistency between Orders Control and Admin Inbox
- clearer search intent for Order ID and Customer ID lookups
- improved screen usability and scroll behaviour
- identified Orders Control performance as a remaining area for further refinement

## Theme and stability cleanup

I cleaned the theme foundation of the app and prepared it for full light/dark mode support.

### What I added
- I added `KoffeeCraftApp` so the saved theme can be applied from app start.
- I rebuilt the shared color system in `colors.xml` and `values-night/colors.xml`.
- I kept the original app look as the light mode base.
- I prepared the dark mode as a darker version of the same design language.

### What I changed
- I simplified `themes.xml` and `values-night/themes.xml` to a safer base theme.
- I removed broken backup and data extraction manifest references.
- I set `android:allowBackup="false"` as part of the security and cleanup pass.
- I restored damaged color resources after a bad global replace changed literal color values into self-references.
- I added missing shared colors such as `kc_divider` and `kc_danger_soft`.

### Stability fixes
- I removed all broken `Color.parseColor("@color/...")` calls and replaced them with resource-based color access.
- I updated multiple screens and adapters so they now use `ContextCompat.getColor(...)` instead of invalid runtime color parsing.
- I removed unsafe `!!` usage when parsing payment card expiry values in checkout-related flows.
- I added RecyclerView `NO_POSITION` safety checks in swipe handlers.

### Files and flows I cleaned
- Admin:
  - home
  - campaign studio
  - inbox
  - notifications
  - account management
  - orders adapter
- Customer:
  - inbox
  - notifications
  - orders
  - menu
  - checkout
  - payment methods

### Result
- I removed the main runtime crash pattern caused by invalid color parsing.
- I made the theme foundation stable again.
- I reduced the risk of crashes during checkout and swipe actions.
- I prepared the app for the next stage of full UI theme migration.


### Database integrity and notification flow hardening

I strengthened two important areas of the application architecture: manual order status notifications and database-level relational integrity.

#### 1. Manual admin order status updates now notify customers
Previously, when an admin manually changed an order status inside `AdminOrdersFragment`, the application updated the order record in the database but did not create a customer-facing notification. This created a functional gap between simulated status updates and manual status updates.

I updated the manual status change flow so that after the status is changed:
- the updated order is reloaded,
- a customer notification is created through `CustomerNotificationManager`,
- the admin notification state is synchronised through `AdminNotificationManager`.

This makes the manual order workflow consistent with the rest of the notification system and ensures that customer-visible status communication is reliable.

#### 2. Added Room foreign key relations for core transactional tables
The original schema relied mostly on logical relationships rather than database-enforced referential integrity. That meant related records could become orphaned and some cleanup had to be performed manually.

I introduced foreign key constraints for the main relational flows:
- `Order -> Customer`
- `OrderItem -> Order`
- `Payment -> Order`
- `Feedback -> OrderItem / Customer`
- `CustomerPaymentCard -> Customer`
- `Favourite -> Customer / Product`

I intentionally did not yet add `OrderItem -> Product` because product deletion behaviour still needs to be refactored to soft delete in order to preserve historical order data safely.

#### 3. Added a Room migration for the new FK-protected schema
Because SQLite requires table recreation when foreign key constraints are introduced, I added a migration that:
- cleans orphaned legacy rows before migration,
- recreates the affected tables with foreign keys,
- restores data into the new schema,
- recreates the required indices.

During implementation, Room schema validation failures highlighted mismatches between entity definitions and migration SQL. I corrected these by aligning entity annotations and defaults with the migrated schema, especially for saved payment cards and other FK-enabled tables.

#### Result
These changes improve:
- database consistency,
- maintainability,
- correctness of delete cascades,
- reliability of order communication,
- overall architectural quality of the application.


