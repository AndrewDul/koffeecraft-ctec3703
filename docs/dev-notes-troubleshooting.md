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
