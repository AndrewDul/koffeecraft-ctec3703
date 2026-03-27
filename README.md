# KoffeeCraft

KoffeeCraft is my Android coffee shop application. I built it as a full mobile project with two role-based sides: **Customer** and **Admin**. The app includes menu browsing, product customisation, cart and checkout, rewards, saved payment methods, notifications, favourites, feedback, admin tools, account access control, and internal management features.

**A mini screenshot gallery is available at the end of this README, after the author section.**

---

## About the App

KoffeeCraft is a local-first Android app inspired by a modern coffee shop experience.  
I wanted to build a project that looks clean, works well on mobile, and demonstrates both customer features and admin management tools in one application.

Main areas I focused on:
- mobile UI design
- realistic user flows
- persistent local data
- customer and admin roles
- product configuration
- order handling
- rewards features
- testing
- project documentation
---

## What the App Can Do

### Customer side

A customer can:

- register a new account
- sign in
- browse the menu
- switch between coffee, cakes, and merchandise
- see recommended products, new arrivals, and rewards
- open product details
- customise drinks and products
- choose sizes
- add extras
- review allergen-related setup
- add items to the cart
- place an order
- choose a payment method at checkout
- save payment cards for future checkout
- view order history
- track order status
- receive inbox messages and notifications
- save favourites
- save reusable favourite presets
- collect beans
- redeem rewards
- leave ratings and written feedback after orders
- manage personal account settings

---

## Admin side

An admin can:

- sign in through the admin flow
- open the admin dashboard
- review key operational numbers
- manage the menu
- add products
- edit products
- enable or disable products
- delete products
- manage product sizes
- manage extras
- manage allergens
- manage customer and admin account access
- create new admin accounts
- use campaign tools
- review customer feedback
- review product performance signals
- review internal notifications
- review orders
- simulate and update order progress
- use internal settings and access tools

---

## Roles

### Customer
The customer role is focused on ordering, rewards, favourites, saved cards, notifications, feedback, and account settings.

### Admin
The admin role is focused on menu control, product setup, customer and admin account access, operational review, campaigns, feedback insights, notifications, and order management.

---

## Admin Features in More Detail

### 1. Admin Dashboard
The admin dashboard gives a quick overview of:
- orders today
- revenue today
- pending orders
- average rating
- promo opt-in rate
- active customers
- menu health and other operational summaries

### 2. Menu Control
In Menu Control, the admin can:
- add a new product
- edit product details
- set category / family
- manage sizes
- manage extras
- manage allergens
- enable or disable visibility
- delete products
- review reward availability

### 3. Product Images
When I add an image to a product, I can do it in **two ways**:

- choose a permanent image from the **built-in KoffeeCraft app image library**
- import an image from the **phone gallery**

This is already described in the product form flow and gives two practical ways to manage product visuals.

### 4. Feedback Insights
The admin can review:
- overall average rating
- coffee average
- cake average
- number of reviews
- percentage of reviews with comments
- hidden comments
- rating breakdown
- review moderation state

### 5. Campaign Studio
The admin can work with:
- promotional offers
- bonus beans
- audience selection
- campaign builder tools

### 6. Admin Settings and Access Control
The admin settings area includes access tools and account management controls.

The app includes:
- **Create Admin Account**
- **Account Access Center**
- internal communication and notification-related controls

### 7. Creating New Admin Accounts
To create a new admin account, I go to:

**Admin Settings → Access & Roles → Create Admin Account**

This screen is used to create a secure internal admin profile with status controls.

### 8. Order Status Simulation
The admin can simulate and update order progress through the main preparation stages.  
This helps me test realistic order flow behaviour inside the app.

The status flow includes stages such as:
- placed
- preparing
- ready
- collected

The admin order screens also include action-style controls like:
- move to preparing
- mark ready
- mark collected

---

## Rewards System

The rewards system is based on beans earned in the app.

Customers can:
- collect beans from activity and purchases
- track bean booster progress
- unlock or redeem rewards
- see reward requirements
- add reward items to the cart when eligible

The rewards area also shows progress-based behaviour such as bean booster progress and locked / unlocked reward states.

---

## Payments and Checkout

The checkout flow supports:
- payment method selection
- saved cards
- new card entry
- card persistence for future checkout
- selected / default saved card logic

The customer can also manage payment methods from settings.

---

## Feedback and Ratings

The feedback system allows customers to:
- leave a rating
- leave written feedback
- review product-related experience after orders

The admin can:
- review feedback
- inspect rating breakdown
- review comments
- hide or moderate comments where needed

---

## Notifications and Inbox

The app includes both:
- notification-related screens
- inbox / message-related screens

This supports order updates, internal messages, and customer-facing communication.

---

## Technology Stack

I built KoffeeCraft with:

- **Language:** Kotlin
- **Platform:** Android
- **UI:** XML layouts + Material Components
- **Navigation:** Navigation Component
- **Database:** Room with SQLite
- **Build system:** Gradle
- **Architecture style:** layered Android structure with UI, persistence, repositories, and feature logic

---

## Project Structure

Main parts of the project:

- `app/src/main/`  
  Main app code, resources, layouts, drawables, navigation, values, and Android manifest

- `app/src/test/`  
  Local unit tests

- `app/src/androidTest/`  
  Instrumented tests, DAO tests, repository tests, and UI smoke tests

- `app/schemas/`  
  Exported Room database schemas

- `docs/`  
  Project documentation, screenshots, and testing evidence

---

## Database

I use Room / SQLite for persistent local data.

Main data areas include:
- customers
- admins
- products
- product sizes
- extras
- allergens
- orders
- order items
- saved cards
- notifications
- inbox data
- favourites
- rewards
- feedback

---

## Testing

I added multiple types of testing to this project.

### Test levels used
- local unit tests
- instrumented repository tests
- instrumented DAO tests
- UI smoke tests

### Test areas covered
The testing work covers important behaviour such as:
- saved payment card persistence
- default card selection
- order item query logic
- reorder-related behaviour
- feedback aggregation
- feedback moderation state
- notification queries
- unread count logic
- read state updates
- welcome screen navigation
- registration validation
- successful onboarding entry
- customer login flow
- customer shell navigation
- admin login flow
- admin orders access

### Examples of tests used
Examples from my testing work include:
- `CustomerPaymentCardDaoInstrumentedTest`
- `OrderItemDaoInstrumentedTest`
- `FeedbackDaoInstrumentedTest`
- `NotificationDaoInstrumentedTest`
- `CustomerFlowEspressoTest`
- `AdminFlowEspressoTest`

---

## AI-Generated Images

A large part of the current product and reward visuals were prepared with AI support and then integrated into the app.

This includes image assets such as:
- coffee product images
- cake product images
- reward images

These images are used in the current UI and product library flows to improve the visual quality of the app.

---

## Documentation

I keep project documentation in the repository so the structure and development process are easier to understand.

### Main documentation files

- `architecture.md`  
  My architecture notes, design decisions, feature changes, and evolution of the project

- `dev-notes-troubleshooting.md`  
  My development notes, problems I hit during the build, and how I fixed them

- `testing-evidence.md`  
  Summary of testing work and supporting evidence

### Supporting documentation folders

- `docs/screenshots/`  
  Screenshots used in the README and project presentation

- `docs/evidence/testing/`  
  Testing evidence images and proof material for test batches

- `app/schemas/`  
  Exported Room schemas used to document database structure changes

### Useful app resource locations

- `app/src/main/res/layout/`  
  Main UI layouts, fragments, item cards, dialogs, and sheets

- `app/src/main/res/navigation/`  
  Navigation graphs for customer and admin flows

- `app/src/main/res/drawable-nodpi/`  
  Product and reward image assets

- `app/src/main/res/values/` and `app/src/main/res/values-night/`  
  Colors, themes, strings, and theme resources for light and dark mode

- `app/src/main/AndroidManifest.xml`  
  Android app manifest and launch configuration

---

## How to Run the App

1. Open the project in **Android Studio**.
2. Let **Gradle sync** finish.
3. Make sure the Android SDK and emulator are ready.
4. Run the app on:
    - an Android emulator, or
    - a physical Android device
5. Test both main flows:
    - customer flow
    - admin flow

---

## Current Status and Next Steps

This project is still being developed.

I will continue working on it to move it closer to a version that can be downloaded and used as a more complete application.

My next development focus is:
- more visual polish
- stronger UI consistency
- security improvements
- better hardening of account and admin-related flows
- more production-ready refinement

---

## Author

**Andrzej Dul**  
Software Engineering Student  
De Montfort University

---

## Gallery



<table>
  <tr>
    <td align="center">
      <img src="docs/screenshots/launcher.png" width="180" alt="Launcher"><br>
      <sub>Launcher</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/welcome.png" width="180" alt="Welcome Screen"><br>
      <sub>Welcome Screen</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/login.png" width="180" alt="Login"><br>
      <sub>Login</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/register1.png" width="180" alt="Register Step 1"><br>
      <sub>Register Step 1</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/register2.png" width="180" alt="Register Step 2"><br>
      <sub>Register Step 2</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/customer_dasboard.png" width="180" alt="Customer Dashboard"><br>
      <sub>Customer Dashboard</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/customer_dashboard2.png" width="180" alt="Customer Dashboard 2"><br>
      <sub>Customer Dashboard 2</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/customer_menu.png" width="180" alt="Customer Menu"><br>
      <sub>Customer Menu</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/customer_menu2.png" width="180" alt="Customer Menu 2"><br>
      <sub>Customer Menu 2</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/cart.png" width="180" alt="Cart"><br>
      <sub>Cart</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/checkout.png" width="180" alt="Checkout"><br>
      <sub>Checkout</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/customer_rewards.png" width="180" alt="Customer Rewards"><br>
      <sub>Customer Rewards</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/customer_rewards2.png" width="180" alt="Customer Rewards 2"><br>
      <sub>Customer Rewards 2</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/myorders.png" width="180" alt="My Orders"><br>
      <sub>My Orders</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/orderstatus.png" width="180" alt="Order Status"><br>
      <sub>Order Status</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/customernotifications.png" width="180" alt="Customer Notifications"><br>
      <sub>Customer Notifications</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/customerinbox.png" width="180" alt="Customer Inbox"><br>
      <sub>Customer Inbox</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/favourites.png" width="180" alt="Favourites"><br>
      <sub>Favourites</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/feedback.png" width="180" alt="Leave Feedback"><br>
      <sub>Leave Feedback</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/customer_settings.png" width="180" alt="Customer Settings"><br>
      <sub>Customer Settings</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/admin_dashboard.png" width="180" alt="Admin Dashboard"><br>
      <sub>Admin Dashboard</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/admin_menu_control.png" width="180" alt="Admin Menu Control"><br>
      <sub>Admin Menu Control</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/product_manage.png" width="180" alt="Product Details"><br>
      <sub>Product Details</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/admin_image_library.png" width="180" alt="Product Image Library"><br>
      <sub>Product Image Library</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/campaing_studio.png" width="180" alt="Campaign Studio"><br>
      <sub>Campaign Studio</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/adminordercontrol.png" width="180" alt="Orders Control"><br>
      <sub>Orders Control</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/admindirectmessage.png" width="180" alt="Direct Message Centre"><br>
      <sub>Direct Message Centre</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/adminnotifications.png" width="180" alt="Admin Notifications"><br>
      <sub>Admin Notifications</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/admin_feedback.png" width="180" alt="Feedback Insights"><br>
      <sub>Feedback Insights</sub>
    </td>
    <td align="center">
      <img src="docs/screenshots/admin_settings.png" width="180" alt="Admin Settings"><br>
      <sub>Admin Settings</sub>
    </td>
  </tr>
</table>