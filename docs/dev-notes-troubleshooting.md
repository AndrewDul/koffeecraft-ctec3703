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

## Notifications not showing (Android 13+)
**Cause:** Missing POST_NOTIFICATIONS permission.  
**Fix:** Add manifest permission and request it at runtime in MainActivity.

## Issue: Order status notifications triggered when viewing past orders
**Symptom:** Opening an existing order from "My Orders" triggered status simulation again and produced repeated notifications.

**Cause:** `OrderStatusFragment` always ran the status simulation and notification logic on screen entry. The first Flow emission was treated as a status change.

**Fix:**
- Added a navigation argument `simulate` (default false). It is set to true only when navigating from Checkout after placing a new order.
- Status simulation runs only when `simulate=true` and the initial status is `PLACED`.
- Notifications are skipped on the first Flow emission to prevent alerts when simply viewing an existing order.

**Result:** Past orders can be viewed without re-simulating status changes or re-sending notifications.