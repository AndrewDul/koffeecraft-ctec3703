# KoffeeCraft – Dev Notes (Troubleshooting)

## 1) AAR metadata / compileSdk mismatch
**Issue:** Build failed with AAR metadata errors (some AndroidX dependencies required API 36+).  
**Fix:** Updated `compileSdk` (and `targetSdk`) to 36 to satisfy dependency requirements.


## 2) Adaptive icon requires API 26+
**Issue:** Android resource linking failed for `<adaptive-icon>` in `mipmap-anydpi` because adaptive icons require API 26+.  
**Fix:** Set `minSdk` to 26 (or alternatively, icons could be moved to `mipmap-anydpi-v26`).


## 3) Database Inspector showed "Nothing to show"
**Issue:** App Inspection → Database Inspector initially displayed no databases.  
**Fix:** Triggered database creation with a simple DAO query (e.g., count admins/products) during development to ensure the database file is created and visible in App Inspection.


## 4) Notifications not showing (Android 13+)
**Cause:** Missing POST_NOTIFICATIONS permission.  
**Fix:** Add manifest permission and request it at runtime in MainActivity.


## 5) Issue: Order status notifications triggered when viewing past orders
**Symptom:** Opening an existing order from "My Orders" triggered status simulation again and produced repeated notifications.

**Cause:** `OrderStatusFragment` always ran the status simulation and notification logic on screen entry. The first Flow emission was treated as a status change.

**Fix:**
- Added a navigation argument `simulate` (default false). It is set to true only when navigating from Checkout after placing a new order.
- Status simulation runs only when `simulate=true` and the initial status is `PLACED`.
- Notifications are skipped on the first Flow emission to prevent alerts when simply viewing an existing order.

**Result:** Past orders can be viewed without re-simulating status changes or re-sending notifications.


## 6) Issue: AdminActivity crash on launch
**Symptom:** App crashed immediately after opening AdminActivity.

**Cause:** Admin navigation destinations referenced fragments that did not exist yet (missing fragment classes/layouts). Bottom navigation also requires destination IDs to match menu item IDs.

**Fix:** Added minimal admin fragments and layouts for all admin destinations, and ensured `admin_bottom_nav.xml` item IDs match `admin_nav_graph.xml` destination IDs.


## 7) Admin Orders: Next button shown for COLLECTED
**Issue:** The "Next" action was visible even for orders with status COLLECTED.

**Fix:** I hid the Next button in the adapter when the order status is COLLECTED to prevent invalid status transitions.

### 8) Room migration mismatch: product active flag (isActive vs isAvailable)

**Symptom**
- Build failed in KSP/Room and app crashed after log in to admin account.
- Products disappeared in UI after schema updates.

**Root cause**
- Column naming inconsistency between:
    - `Product` entity mapping
    - Room migration SQL
    - DAO queries
- Emulator kept an old database, so seeding didn’t re-run.

**Fix**
- Standardized queries to use the existing DB column (`isAvailable`) and mapped Kotlin property via `@ColumnInfo`.
- Implemented safe migration (`addColumnIfMissing`) to avoid duplicate-column crashes.
- Uninstalled / cleared app data on emulator to recreate DB and re-seed products.


### 9) Admin Menu filter not appearing correctly

**Symptom**
-The category filter in Admin Menu did not appear in the correct place or the layout behaved unexpectedly.

**Cause**
-The `MaterialButtonToggleGroup` was constrained to itself:

**Fix**
`xml`
-`app:layout_constraintTop_toBottomOf="@id/toggleCategoryFilter`

### 10) Feedback build errors after moving from order-level to product-level feedback

### Problem
After changing feedback from `orderId` to `orderItemId`, the project stopped compiling.

### Symptoms
Errors included:
- unresolved reference to `getByOrderId`
- unresolved reference to old feedback fields
- constructor mismatch for `Feedback(orderId = ...)`

**Cause**
The old customer feedback screen still used the previous order-level feedback API and data model.

**Fix**
I replaced the old logic with the new purchased-product feedback flow:
- feedback now loads purchased products for a specific order
- a single purchased product is reviewed using `orderItemId`
- saving feedback uses the new `Feedback(orderItemId, ...)` model

---

## 11) Admin Home dashboard query functions failed to compile

**Problem**
The project failed to compile with errors such as:
- `Function 'getTopRatedProducts' must have a body`
- unresolved references from `AdminHomeFragment`

**Cause**
The new DAO query methods were pasted outside `interface FeedbackDao`.

**Fix**
I moved all dashboard query methods inside `interface FeedbackDao` and kept the projection data classes below the interface.

---

## 12) Feedback analytics could not be calculated per product with the old model

**Problem**
The original feedback design stored one review per order.  
This made it impossible to calculate accurate product-based analytics for Admin Home.

**Cause**
One order can contain multiple purchased products, so one order-level rating cannot be assigned reliably to a single product.

**Fix**
I rebuilt feedback storage so each feedback record belongs to one purchased product using `orderItemId`.

---

## 13)Customer feedback needed to support reviewing multiple purchased products

**Problem**
A single order could contain multiple products, but the original feedback flow allowed only one shared review.

**Fix**
I changed the customer flow so:
- the customer first sees purchased products from the selected order
- each product can be reviewed separately
- after submit, the next unreviewed product opens automatically
- when all items are reviewed, the app returns to Menu

---

## 14) Feedback button visibility in Order Status

**Problem**
The feedback button was available too early in the order lifecycle.

**Fix**
I restricted feedback access so the button only appears when the order status is `COLLECTED`.

---

## 15) Admin feedback moderation requirements

**Problem**
Admin needed a way to moderate inappropriate comments without losing rating statistics.

**Fix**
I added:
- `isHidden`
- `isModerated`

I also implemented:
- hide comment
- unhide comment

This keeps the rating in the database while hiding the comment text.

---

## 16) Comment filter behaviour

**Problem**
`Without comment` only returns feedback records that still exist and have no visible comment.

**Current behaviour**
- if a feedback record is deleted, it will not appear in `Without comment`
- if a feedback has an empty comment, it appears in `Without comment`
- if a feedback has a hidden comment, it can be treated separately depending on current filter logic

**Note**
This is correct because the admin feedback screen only shows existing feedback records, not products with no feedback record at all.

---

## 17) Admin Home ranking rules

**Problem**
Least-commented products should not be dominated by products with zero comments.

**Fix**
I excluded zero-comment products from the least-commented ranking so the card only shows products that actually have at least one comment.


## 18)Send Message moved outside the visible screen

**Problem**
The Admin Inbox screen used a simple `LinearLayout`, so when the content became longer or the keyboard opened, the `Send Message` action could move outside the visible area.

**Fix**
I changed the layout structure to use:
- `ConstraintLayout`
- `NestedScrollView` for the content
- a bottom-aligned send action

**Result**
The content scrolls correctly and the send action remains visible.

---

## 19)Read more / Read less looked too heavy

**Problem**
The long-message toggle in customer Inbox was implemented as a regular `Button`, which looked visually heavy and inconsistent.

**Fix**
I changed it into a lighter text-link style component:
- text-based appearance
- stronger visual contrast
- simpler interaction style

**Result**
The message toggle now feels lighter and closer to a real mobile inbox UI.

---

## 20)Inbox expand/collapse was unstable after scrolling

**Problem**
Expanded state was originally held inside `ViewHolder`, which is unsafe because `RecyclerView` recycles rows.

**Fix**
I moved expand/collapse state into the adapter and tracked it using stable item identifiers.

**Result**
Inbox cards expand and collapse reliably without random state jumps after scrolling.

---

## 21)User could not leave Order Status screen

**Problem**
`Back to Menu` was blocked until the order reached a later status, which trapped the user inside the screen.

**Fix**
I changed the logic so:
- `Back to Menu` is always available
- feedback availability still follows order status rules

**Result**
The user can leave the screen at any time.

---

## 22)Order simulation could stop after leaving the fragment

**Problem**
Order simulation was tied to fragment lifecycle, so leaving the screen could cancel the coroutine and stop status progression.

**Fix**
I extracted the logic into `OrderSimulationManager`, outside the fragment lifecycle.

**Result**
Status simulation continues reliably even if the user leaves the screen.

---

## 23)Admin notifications were deleted automatically after COLLECTED

**Problem**
Admin order-action notifications were being removed automatically after the order reached `COLLECTED`.

**Fix**
I changed the notification lifecycle so completed notifications remain stored until the admin removes them manually.

**Result**
The admin keeps control over completed notifications and they no longer disappear unexpectedly.

---

## 24)Admin could delete active order notifications too early

**Problem**
Swipe delete and manual delete could remove notifications for active orders, not just completed ones.

**Fix**
I restricted deletion so only `COLLECTED` notifications can be removed.

**Result**
Active order notifications are protected from accidental deletion.

---

## 25)Customer notifications showed only short text

**Problem**
Customer notifications originally showed only a title and message, without useful order details.

**Fix**
I extended the notification flow so customer notification cards can display detailed order contents after expand:
- product name
- quantity
- price per unit
- line total

**Result**
Customer notifications are now much more informative.

---

## 26)My Orders forced navigation instead of showing inline details

**Problem**
The old My Orders flow only showed basic data and pushed the user into another screen for more information.

**Fix**
I redesigned My Orders using expandable full-width cards with inline details and retained `Order again`.

**Result**
The user can inspect order contents directly inside the list.

---

## 27)Cart was not always accessible from the main customer experience

**Problem**
Cart access was tied to the old Menu screen.

**Fix**
I moved Cart access into the fixed customer top bar and added a live badge counter.

**Result**
Cart is available across the customer shell and behaves more like a real app feature.

---

## 29)Communication channels needed clear separation

**Problem**
System events and admin-written messages could easily become mixed conceptually.

**Fix**
I separated communication into two systems:
- `AppNotification` for system events
- `InboxMessage` for admin-authored messages

**Result**
Notifications and Inbox now behave independently and support separate unread badges.

---

## 30)Admin targeting needed a safer model

**Problem**
Searching customers directly by personal details would expose more information than necessary.

**Fix**
I limited admin targeting to:
- all users
- birthday today
- order number
- customer ID

**Result**
The messaging feature is more privacy-aware and easier to justify in a real application context.

---

## Key issues I fixed in this stage

The most important issues I fixed were:
1. Order simulation was incorrectly tied to fragment lifecycle
2. Admin notifications were auto-deleting after `COLLECTED`
3. Inbox expand/collapse state was unstable because it lived in `ViewHolder`
4. Admin Inbox layout was unsafe for long content and the keyboard
5. Customer notifications were too shallow and lacked order detail
6. My Orders did not match the expected card-based expandable behaviour

## Welcome and sign-in flow fixes

During this stage, I fixed several issues related to the new authentication entry flow.

### Main problems
1. **Navigation XML errors**
   While changing the start destination and adding the Welcome screen, the navigation XML became invalid and caused build failures.

2. **Login verification mismatch**
   The new Sign in flow initially referenced a password verification method that did not exist in the current security utility.

3. **Session assignment mismatch**
   The first login version tried to write customer session data in a way that did not match the existing `SessionManager` API.

### How I fixed them
- I corrected the navigation graph structure and repaired the malformed XML
- I changed login verification to use the existing password hashing approach already used by the app
- I updated login session handling to use the existing `SessionManager` methods for:
  - customer login
  - admin login

### Result
The app now:
- opens on the Welcome screen
- navigates correctly to Register and Sign in
- logs in customers and admins correctly
- returns to Welcome on logout

