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


## 9) Admin Menu filter not appearing correctly

**Symptom**
The category filter in Admin Menu did not appear in the correct place or the layout behaved unexpectedly.

**Cause**
The `MaterialButtonToggleGroup` was constrained to itself:

**Fix**
```xml
app:layout_constraintTop_toBottomOf="@id/toggleCategoryFilter"


## 10) Feedback build errors after moving from order-level to product-level feedback

### Problem
After changing feedback from `orderId` to `orderItemId`, the project stopped compiling.

### Symptoms
Errors included:
- unresolved reference to `getByOrderId`
- unresolved reference to old feedback fields
- constructor mismatch for `Feedback(orderId = ...)`

### Cause
The old customer feedback screen still used the previous order-level feedback API and data model.

### Fix
I replaced the old logic with the new purchased-product feedback flow:
- feedback now loads purchased products for a specific order
- a single purchased product is reviewed using `orderItemId`
- saving feedback uses the new `Feedback(orderItemId, ...)` model

---

## 11) Admin Home dashboard query functions failed to compile

### Problem
The project failed to compile with errors such as:
- `Function 'getTopRatedProducts' must have a body`
- unresolved references from `AdminHomeFragment`

### Cause
The new DAO query methods were pasted outside `interface FeedbackDao`.

### Fix
I moved all dashboard query methods inside `interface FeedbackDao` and kept the projection data classes below the interface.

---

## 12) Feedback analytics could not be calculated per product with the old model

### Problem
The original feedback design stored one review per order.  
This made it impossible to calculate accurate product-based analytics for Admin Home.

### Cause
One order can contain multiple purchased products, so one order-level rating cannot be assigned reliably to a single product.

### Fix
I rebuilt feedback storage so each feedback record belongs to one purchased product using `orderItemId`.

---

## 13)Customer feedback needed to support reviewing multiple purchased products

### Problem
A single order could contain multiple products, but the original feedback flow allowed only one shared review.

### Fix
I changed the customer flow so:
- the customer first sees purchased products from the selected order
- each product can be reviewed separately
- after submit, the next unreviewed product opens automatically
- when all items are reviewed, the app returns to Menu

---

## 14) Feedback button visibility in Order Status

### Problem
The feedback button was available too early in the order lifecycle.

### Fix
I restricted feedback access so the button only appears when the order status is `COLLECTED`.

---

## 15) Admin feedback moderation requirements

### Problem
Admin needed a way to moderate inappropriate comments without losing rating statistics.

### Fix
I added:
- `isHidden`
- `isModerated`

I also implemented:
- hide comment
- unhide comment

This keeps the rating in the database while hiding the comment text.

---

## 16) Comment filter behaviour

### Problem
`Without comment` only returns feedback records that still exist and have no visible comment.

### Current behaviour
- if a feedback record is deleted, it will not appear in `Without comment`
- if a feedback has an empty comment, it appears in `Without comment`
- if a feedback has a hidden comment, it can be treated separately depending on current filter logic

### Note
This is correct because the admin feedback screen only shows existing feedback records, not products with no feedback record at all.

---

## 17) Admin Home ranking rules

### Problem
Least-commented products should not be dominated by products with zero comments.

### Fix
I excluded zero-comment products from the least-commented ranking so the card only shows products that actually have at least one comment.

