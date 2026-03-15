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
