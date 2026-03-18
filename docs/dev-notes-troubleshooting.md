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

## Registration, onboarding, and navigation fixes

During this stage, I fixed several issues related to the new premium registration flow, onboarding flow, and shell navigation.

### Main problems
1. **Registration still returned the user to Sign in**
   After successful registration, the app still followed the old flow and sent the user back to the login screen.

2. **The registration flow did not yet support the new profile fields**
   The original registration logic did not match the redesigned form or the updated customer data requirements.

3. **The onboarding flow was not connected to registration**
   A new customer needed to be logged in automatically and moved into intro screens instead of being redirected back to authentication.

4. **Bottom navigation behaved incorrectly after opening top-bar destinations**
   For both customer and admin, opening top-bar screens such as Notifications caused bottom navigation tabs like Home to behave incorrectly.

5. **Onboarding text and reward logic needed correction**
   Some intro messages used the wrong voice and the reward logic needed to match the final rule:
  - welcome Inbox message for every new customer
  - +5 beans only for promo consent

**How I fixed them**
- I redesigned the registration flow and aligned it with the new premium Register screen
- I updated the customer model, repository, ViewModel, and Room migration to support the new fields
- I changed registration success handling so the customer is logged in immediately
- I added a dedicated onboarding flow after registration
- I added post-onboarding welcome messaging and conditional starter beans logic
- I replaced the old automatic bottom navigation setup with explicit navigation handling for both customer and admin shells

**Result**
The app now:
- supports the new premium registration UI
- stores the new customer registration fields
- logs the user in directly after successful registration
- shows onboarding before entering the app
- sends a welcome Inbox message automatically
- awards 5 beans only when promotional Inbox consent is enabled
- handles customer and admin bottom navigation correctly after top-bar navigation

## Rewards bean icon resource fix

During the Rewards screen update, I hit a resource-linking issue while adding the coffee bean image.

### Problem
The bean image was initially added in a way that created launcher-style asset files inside `mipmap-anydpi-v26`.

Those generated files referenced:
- `coffee_bean_background`

but that drawable did not exist, which caused Android resource linking to fail.

### Root cause
The image was added as an asset in the wrong resource flow for this use case.

I needed a normal drawable for an `ImageView` inside the Rewards screen, but the generated files behaved like launcher icon resources instead.

### Fix
I removed the incorrect generated `mipmap` resources and added the coffee bean image as a normal drawable file inside:
- `res/drawable`

After that, the Rewards layout could correctly use:
- `@drawable/coffee_bean`

### Result
The Rewards screen now displays the bean icon correctly and builds without resource-linking errors.


## Customer and admin shell navigation fix

During this stage, I fixed bottom navigation behaviour for both customer and admin shells.

### Problem
The shell navigation still had inconsistent behaviour.

#### Customer issues
- tapping one bottom tab could open the wrong screen
- for example, `My Orders` could behave like `Menu`
- after opening top-bar destinations such as Notifications, Inbox, Settings, or Cart, bottom tab switching could become unreliable

#### Admin issues
- after opening top-bar destinations such as Notifications or Settings, tapping `Home` from the bottom bar did not always work immediately
- sometimes another bottom tab had to be opened first before `Home` worked again

### Root cause
The issue was caused by mixed shell navigation state handling.

Top-bar destinations and bottom-bar destinations were sharing one navigation flow, but the bottom bar state and tab selection logic were not being handled consistently.

The previous implementation also mixed old and new navigation helper logic, which caused unresolved references and unstable navigation behaviour.

### Fix
I separated shell navigation into two explicit flows:
- one helper for top-bar navigation
- one helper for bottom-bar navigation

I also updated bottom-bar handling so:
- bottom tabs navigate directly and consistently
- top-bar screens no longer block returning to bottom destinations
- bottom selection state is updated more safely after destination changes

### Result
The app now:
- switches correctly between bottom tabs for customer
- switches correctly between bottom tabs for admin
- allows returning from top-bar screens back to bottom destinations without getting stuck
- keeps shell navigation more stable and predictable

## Product customisation selector resource fix

During the premium customisation UI update, the project failed to compile because one of the new chip text selectors was missing.

### Problem
The build failed with an unresolved resource reference inside `ProductCustomizationBottomSheet.kt`.

### Symptom
Kotlin compilation failed with an error similar to:
- unresolved reference to `kc_chip_text_selector`

### Root cause
The premium chip styling logic referenced a new color selector resource for chip text states, but the corresponding XML file had not yet been added to `res/color`.

### Fix
I created the missing color state list resource:
- `res/color/kc_chip_text_selector.xml`

This restored the missing reference used by the premium chip styling code.

### Result
The project compiled correctly again and the product customisation chips could use the intended selected / unselected premium text styling.

---

## Notes for this stage

Most of the remaining work in this stage was not bug-fixing but structured feature development and UI refinement.

The main changes were:
- product model redesign using `productFamily` and `rewardEnabled`
- premium admin extras management redesign
- premium customer Home dashboard with rewards and recommendation carousels


## 31) Crafted label did not appear for customised orders in My Orders

**Problem**
The new `Crafted` badge in My Orders did not appear even when the customer had changed product size or selected add-ons.

**Cause**
The My Orders UI and DAO were already able to read customisation metadata from `order_items`, but the order placement flow was still saving only the basic order item fields:
- `productId`
- `quantity`
- `unitPrice`

This meant the database did not preserve the customisation metadata needed to detect crafted orders later in history.

**Fix**
I updated the checkout / order placement flow so customised cart metadata is stored in `order_items`.

The saved fields now include:
- `selectedOptionLabel`
- `selectedOptionSizeValue`
- `selectedOptionSizeUnit`
- `selectedAddOnsSummary`
- `estimatedCalories`

I also kept the My Orders card logic based on real persisted data instead of showing the badge through UI-only assumptions.

**Result**
The `Crafted` badge now appears correctly for newly placed customised orders and the order history can display more accurate custom product information.


## 32) Customer Favourites needed a dedicated premium interaction model

**Problem**
The favourites screen had inconsistent behaviour between its two sections.

- saved custom favourites still relied on a dialog for details
- standard favourites still reused the generic menu product card style
- standard favourites did not expand inline
- long add-on summaries in saved presets could break the layout
- some actions became visually duplicated when standard favourites were expanded

**Cause**
The favourites feature had grown beyond the original simple card/dialog flow.

Saved presets and standard favourites were being treated too similarly in structure, even though they represent different user actions:
- one stores a full customised configuration
- the other stores a normal product bookmark

The standard favourites section also still depended on a generic adapter intended for menu browsing rather than a favourites-specific interaction.

**Fix**
I redesigned the Customer Favourites screen into two clearer premium sections:

- `Saved custom favourites` now use expandable inline cards instead of a popup dialog
- long add-on text was changed to wrap correctly inside the card
- the unnecessary `Close` action was removed because tapping the card again already collapses it
- `Standard favourites` now use a dedicated premium adapter and layout
- `Customize` remains available as a bold text action in the collapsed card
- `Buy again` remains in the collapsed card
- the expanded standard favourite card now only adds `Remove` to avoid action duplication

I also changed the standard expanded info from:
- `Family`
- `Availability`

to:
- `Standard size`
- `Calories`

To support this, I added a dedicated Room projection that reads the default product option for each standard favourite.

**Result**
The Customer Favourites screen is now:
- more premium
- more consistent with the rest of the app
- easier to browse inline
- clearer in how standard favourites and saved custom presets differ
- more stable when showing long saved add-on content

## 33) Room / KSP failed after adding standard favourite projection

**Problem**
The build failed during KSP processing after I introduced the new standard favourite projection for the favourites screen.

**Symptom**
Gradle failed with a Room/KSP processing error during:
- `:app:kspDebugKotlin`

**Cause**
The DAO query used:
- `p.productFamily`

but the actual database column in `products` is:
- `category`

The Kotlin property name and the Room database column name were not the same in this case.

**Fix**
I corrected the Room query alias so it maps:
- `p.category AS productFamily`

instead of referencing a non-existent database column.

**Result**
KSP processed the DAO correctly again and the favourites feature could compile with the new standard favourite card projection.

## 34) Customer notification card layout broke after adding order total

**Problem**
After adding the order total to the collapsed customer notification card, the layout became incorrect.

**Cause**
The new `Order total` row was initially inserted as a separate sibling inside the top horizontal layout, alongside:
- the main notification content column
- the remove `X` action

Because of this, the row tried to behave like another horizontal column instead of part of the main left-side content.

**Fix**
I moved the `Order total` row inside the main left content column, underneath:
- the status chip
- the order number
- the status sentence

This restored the correct structure of the collapsed notification card.

**Result**
The collapsed notification card now displays:
- status
- order number
- status sentence
- order total
- remove action

in a clean and visually stable premium layout.

## 35) Checkout primary action text became invisible

**Problem**
The checkout primary action was clickable, but the button text was not visible.

**Cause**
The `MaterialButton` used a custom `android:background` together with:
- `app:backgroundTint="@android:color/transparent"`

This created a styling conflict, so the button still occupied layout space and responded to clicks, but the visual rendering of the text/background became unreliable.

**Fix**
I removed the manual drawable background from the `MaterialButton` and styled the button using normal Material properties instead:
- `app:backgroundTint`
- `app:cornerRadius`
- `app:strokeColor`
- `app:strokeWidth`

**Result**
The checkout action text is now clearly visible and the button keeps a stable premium appearance.

## 36) Feedback flow needed crafted state for purchased products

**Problem**
The redesigned feedback screens needed to display whether a purchased product had been customised, so the UI could show the `Crafted` badge consistently.

**Cause**
The existing purchased-product feedback projection focused only on:
- product identity
- quantity
- unit price
- rating/comment state

It did not yet expose the customisation metadata required to determine whether the purchased product should be treated as crafted.

**Fix**
I extended the Room projection used by the feedback flow so it now also includes:
- selected option label
- selected option size value
- selected option size unit
- selected add-ons summary
- estimated calories

I also added an `isCrafted` derived property to the projection model.

**Result**
The feedback product list and the individual product feedback screen can now show the `Crafted` badge correctly for customised purchased items.

## 37) Settings area needed a full customer account structure

**Problem**
The original customer settings area was too minimal and did not match the premium visual direction of the rest of the application.

It only exposed a basic settings entry point and did not provide a complete account management flow.

**Cause**
Settings had not yet been expanded into a proper customer-facing account area. Important tasks such as:
- editing personal details
- changing password
- managing inbox preferences
- viewing policy/help content
- deleting the account

were not yet separated into structured screens.

**Fix**
I redesigned Settings into a premium multi-screen account flow with grouped sections:
- Account
- Security
- Inbox Preferences
- Help & Policies
- Danger Zone

I added dedicated screens for:
- Personal Info
- Change Password
- Inbox Preferences
- Help / Terms / Privacy
- Delete Account

**Result**
The customer settings experience now feels more complete, more realistic, and more consistent with the rest of the premium KoffeeCraft UI.


## 38) Customer inbox unread state was being cleared too early

**Problem**
The inbox redesign introduced `Read` and `Unread` filters, but the unread state would not remain meaningful because messages were being marked as read too early.

**Cause**
The customer inbox screen marked all messages as read immediately when the screen opened.

This meant:
- unread messages lost their unread state before the customer actually opened them
- the `Unread` filter would become much less useful
- the inbox behaviour did not reflect real reading behaviour

**Fix**
I removed the automatic `markAllAsRead` behaviour from inbox screen entry and changed the logic so a message is marked as read only when the customer opens the card.

**Result**
The inbox now supports meaningful:
- `Read`
- `Unread`

filtering, and the read state better reflects actual customer interaction.

## 39) Reward items could be inflated in cart and break bean availability totals

**Problem**
The rewards screen showed incorrect bean values such as:
- extremely high `Reserved in cart`
- incorrect `Available now`
- oversized cart badge totals

This also blocked normal reward selection even when the customer still had a valid bean balance.

**Cause**
Reward items could still be increased through cart quantity behaviour, so a reward entry could reserve beans repeatedly even though reward items should behave like one-off redemptions.

**Fix**
I changed the cart reward behaviour so reward quantities are normalised and reward items are no longer allowed to inflate through the normal quantity increase flow.

**Result**
Bean reservation values now stay realistic and the rewards screen can calculate availability correctly again.


## 40) Free coffee and free cake could fail despite available beans

**Problem**
Free coffee and free cake could appear unavailable or behave incorrectly even when the customer had enough beans.

**Cause**
Two separate issues contributed to this:
- reserved bean values could become corrupted by inflated reward quantities in cart
- older local data could leave reward eligibility in an inconsistent state for coffee and cake products

**Fix**
I corrected reward quantity behaviour in cart and added migration support to stabilise reward eligibility handling for existing local data.

**Result**
Free coffee and free cake reward flow can now rely on a more accurate bean state and more consistent reward product eligibility.

## 41) ProductAdapter refactor caused Kotlin visibility and scope compile errors

**Problem**
After the menu refactor, `ProductAdapter` failed to compile with errors related to:
- exposing a private parameter type
- unresolved references from inside the ViewHolder

**Cause**
`ProductCardState` was still marked as private while being exposed through the ViewHolder bind signature, and the ViewHolder could not access adapter-level helper methods because it was not structured correctly for that access pattern.

**Fix**
I adjusted the adapter structure so the state type was no longer exposed incorrectly and the ViewHolder could correctly access the helper logic used for inline menu customisation.

**Result**
The menu adapter compiles correctly and the inline expandable product customisation flow can build successfully.

## 42) Rewards refactor caused compile errors due to import duplication and missing cart method

**Problem**
The rewards flow failed to compile after the refactor.

**Cause**
There were two direct issues:
- a duplicate `LinearLayoutManager` import inside `CustomerRewardsFragment`
- `CustomerRewardsFragment` referenced `CartManager.addReward(...)` while the current `CartManager` version did not yet include that method

**Fix**
I removed the duplicate import and restored a complete reward-aware `CartManager` implementation including the missing reward add method.

**Result**
The rewards module and reward cart flow can compile against the same cart API again.

---

## 43) Standard products were incorrectly shown as crafted

**Problem**  
I noticed that standard products from the menu were shown with the `Crafted` label in Cart and My Orders even when I had not customised them.

**What was happening**  
Default option data and calculated values were enough to make some items look customised, even when the user had not changed anything.

**Fix**  
I tightened the crafted-state rule so an item is only treated as crafted when:
- a non-default option is selected, or
- at least one add-on is selected

I also adjusted the cart flow so unchanged default selections are treated as standard products instead of customised ones.

**Result**  
Standard products no longer show the crafted label incorrectly in Cart and My Orders.

---

## 44) Reward customisation showed the normal product price instead of a free base item

**Problem**  
I noticed that when I opened `Free Coffee` or `Free Cake`, the reward customisation screen showed the normal base product price instead of `£0.00`.

**Cause**  
The reward customisation summary still calculated the total using the normal product base price.

**Fix**  
I changed reward pricing so:
- the base reward item starts from `£0.00`
- only option surcharges and add-ons increase the total

**Result**  
The reward screen now matches the intended reward rule: the base item is free and the customer only pays for upgrades or extras.

---

## 45) Reward flow used two different UI patterns

**Problem**  
I noticed that reward selection used a regular dialog first, but reward customisation used a bottom sheet after that.

**Why it was a problem**  
This made the reward journey feel inconsistent and visually disconnected.

**Fix**  
I replaced the reward picker dialog with a dedicated bottom sheet so both steps now use the same bottom-sheet interaction style.

**Result**  
The reward flow now feels more consistent and matches the premium bottom-sheet style already used in the app.

---

## 46) CustomerRewardsFragment failed to compile after reward flow refactor

**Problem**  
After integrating the new reward picker flow, the project failed to compile with a Kotlin syntax error in `CustomerRewardsFragment.kt`.

**Symptom**  
The compiler reported:
- `Syntax error: Expecting member declaration`

**Cause**  
Old dialog-based reward logic and the new bottom-sheet logic became mixed in the same fragment during integration, which left invalid structure in the file.

**Fix**  
I replaced the fragment with a clean version that keeps only the current reward flow and removes the mixed old/new dialog code.

**Result**  
The fragment compiled correctly again and the reward flow became structurally cleaner.

---

## 47) Standard favourites looked less informative than saved custom favourites

**Problem**  
I noticed that standard favourites did not show calories in the collapsed card, which made them look less complete than saved custom favourites.

**Fix**  
I added collapsed calories to the standard favourites card layout and binding logic.

**Result**  
The favourites screen now feels more visually balanced and consistent.


## 48) Admin Feedback crash after opening Feedback Insights

### Problem

After opening the new **Feedback Insights** screen from the admin bottom navigation, the application crashed immediately instead of loading the screen.

### Cause

The crash was caused by an incorrect view binding type inside `AdminFeedbackFragment`.

In the layout, the empty state container used the ID `tvEmpty`, but this view was actually a `MaterialCardView`.

In the fragment code, I was binding that same ID as a `TextView`, which caused a runtime type cast failure when the screen opened.

### Fix

I corrected the binding in `AdminFeedbackFragment` so the empty state is treated as a generic `View` container instead of a `TextView`.

I also updated the visibility handling logic to show and hide that card correctly through the new view reference.

### Result

The `Feedback Insights` screen now opens correctly without crashing.

The admin feedback dashboard loads as expected, including:

- overview cards
- rating breakdown
- premium filter chips
- feedback list
- empty state handling


## 49) Startup crash caused by invalid Navigation argument default type

### Problem

After the recent notification deep-link update, the app started crashing immediately on launch before the main customer flow could open.

### Cause

The issue was caused by an invalid default value type in `nav_graph.xml`.

I added a new navigation argument called `launchInboxMessageId` with type `long`, but the default value was set as `-1` instead of `-1L`.

Because of that, Navigation failed while inflating the graph and the app crashed during `MainActivity` startup.

### Fix

I corrected the default argument value in `nav_graph.xml` from:

- `-1`

to:

- `-1L`

This matched the declared `long` type correctly and allowed the navigation graph to inflate without errors.

### Result

The app now starts correctly again and the notification deep-link flow can load the navigation graph safely.


## 50)Build failed after admin navigation changes because a previously used destination was missing from the nav graph

### Problem

After introducing the new Studio destination and updating the admin navigation structure, the project failed to compile.

### Cause

`AdminSettingsFragment` was still navigating to `adminCreateAccountFragment`, but that destination had been accidentally omitted from the updated `admin_nav_graph.xml`.

Because of that, Kotlin could not resolve `R.id.adminCreateAccountFragment` during compilation.

### Fix

I restored the missing `adminCreateAccountFragment` entry inside `admin_nav_graph.xml` while keeping the new Studio destination and the updated top-bar / bottom-nav structure.

### Result

The build error was resolved and the admin navigation graph became complete again.

Existing admin account creation navigation now works alongside the new Campaign Studio navigation structure.


## 51)Theme and resource build problems

### Problem: `attr/colorBackground not found`
I got a resource linking error after changing the theme files.

Error:
- `style attribute 'attr/colorBackground' not found`

Cause:
- I used `colorBackground` in the theme in a way that Android could not resolve correctly.

Fix:
- I removed the unsafe theme setup.
- I simplified the theme files and kept only a safe base configuration with `android:windowBackground`.

---

## 52)Manifest backup resource errors

### Problem: missing `xml/data_extraction_rules` and `xml/backup_rules`
The build failed because the manifest pointed to XML files that did not exist.

Error:
- `resource xml/data_extraction_rules not found`
- `resource xml/backup_rules not found`

Cause:
- The manifest still contained old backup-related resource references.

Fix:
- I removed these XML references from the manifest.
- I set `android:allowBackup="false"`.

---

## 53)Missing color resources

### Problem: missing `kc_divider` and `kc_danger_soft`
The build failed because some layouts referenced colors that were not defined.

Error:
- `resource color/kc_divider not found`
- `resource color/kc_danger_soft not found`

Cause:
- The layouts were updated before those shared colors were added.

Fix:
- I added the missing color resources in both `values/colors.xml` and `values-night/colors.xml`.

---

## 54)App crash after global replace in colors

### Problem: app crashed on startup after color cleanup
The app stopped opening after I used global replace during theme work.

Cause:
- `colors.xml` was damaged by replace actions.
- Some colors became self-references, for example:
    - `@color/kc_shell_background` inside the definition of `kc_shell_background`

Fix:
- I restored the real hex values in `colors.xml`.
- I checked the theme files again after that.

---

## 55)Runtime crash: `Unknown color`

### Problem: app crashed with `IllegalArgumentException: Unknown color`
The app started, but some screens crashed when opened.

Cause:
- Some Kotlin files still used:
    - `Color.parseColor("@color/...")`
- `Color.parseColor()` only accepts literal values like `#FFFFFF`, not Android resource references.

Fix:
- I replaced these calls with:
    - `ContextCompat.getColor(requireContext(), R.color...)`
    - or `ContextCompat.getColor(itemView.context, R.color...)`
- I cleaned this problem from the affected fragments and adapters.

Affected areas:
- admin home
- admin campaign studio
- admin inbox
- admin notifications
- admin orders
- customer inbox
- customer notifications
- orders
- menu
- checkout

---

## 56)Checkout safety fix

### Problem: unsafe expiry parsing with `!!`
There were places where card expiry parsing used `!!`.

Cause:
- If expiry parsing returned `null`, the app could crash.

Fix:
- I replaced the unsafe `!!` pattern with safe checks.
- I now stop the action and show a message when the expiry cannot be parsed.

Affected areas:
- `CheckoutFragment`
- `CustomerPaymentMethodsFragment`

---

## 57)RecyclerView swipe safety

### Problem: swipe handlers could use an invalid adapter position
Swipe-to-delete logic used `bindingAdapterPosition` directly.

Cause:
- In some RecyclerView edge cases, the position can be `NO_POSITION`.

Fix:
- I added a check before reading the item:
    - if the position is `RecyclerView.NO_POSITION`, I return immediately.

Affected areas:
- `CustomerInboxFragment`
- `CustomerNotificationsFragment`

## 58)Theme cleanup and build/runtime issues after dark mode refactor

### Problem 1: Android resource linking failed after theme changes
While I was moving the app from fixed colors to theme colors, the build failed with missing resource errors.

Examples:
- `attr/colorBackground not found`
- `xml/data_extraction_rules not found`
- `xml/backup_rules not found`
- `color/kc_divider not found`
- `color/kc_danger_soft not found`

### Cause
I referenced theme or XML resources that were not yet created or were named differently than expected.

### Fix
I added or restored the missing resources and aligned the XML files with the actual resource names used in the project.

---

### Problem 2: App crashed at runtime because of `Color.parseColor(...)`
After part of the theme refactor, some screens crashed on launch or when opening specific fragments.

Examples:
- `AdminHomeFragment`
- `AdminCampaignStudioFragment`
- `CheckoutFragment`

The crash reason was:
- `IllegalArgumentException: Unknown color`

### Cause
Some Kotlin code still used `Color.parseColor(...)` with values that were no longer raw hex strings. After the refactor, some values came from theme/resource-based colors, so parsing them as plain strings caused a crash.

### Fix
I removed those unsafe color parsing cases and switched them to proper resource-based color usage.

---

### Problem 3: Kotlin compile error because XML IDs no longer matched the code
After replacing some layout files, the build failed again.

Examples:
- `Unresolved reference 'tilExtraPrice'`
- `Unresolved reference 'etExtraPrice'`
- `Unresolved reference 'cbIsDefault'`
- `Unresolved reference 'tvGoToRegister'`

### Cause
During layout cleanup, I renamed or removed IDs that were still used by Kotlin files such as:
- `AdminMenuFragment.kt`
- `LoginFragment.kt`

### Fix
I restored the missing IDs in the XML files instead of changing the Kotlin logic, so the existing code could continue working safely.

Restored IDs:
- `tilExtraPrice`
- `etExtraPrice`
- `cbIsDefault`
- `tvGoToRegister`

---

### Problem 4: Theme looked inconsistent because some screens still used hardcoded colors
Even after the main theme system was added, some layouts still used old fixed colors.

This caused:
- weak contrast in dark mode
- bright cards inside dark screens
- text that was hard to read
- inconsistent premium look between screens

### Cause
Some XML files still contained old hardcoded values like:
- `#2E2018`
- `#6E5A4D`
- `#7A5730`
- `#B00020`
- old light beige panel colors

### Fix
I replaced the remaining hardcoded values in the affected files with the KoffeeCraft theme color system, for example:
- `@color/kc_text_primary`
- `@color/kc_text_secondary`
- `@color/kc_text_muted`
- `@color/kc_success`
- `@color/kc_success_text`
- `@color/kc_danger`
- `@color/kc_danger_text`
- `@color/kc_surface_panel`
- `@color/kc_surface_primary`
- `@color/kc_surface_secondary`
- `@color/kc_border_soft`

---

### Problem 5: Validation tiles used old Android system colors
Password rule tiles in some fragments still used old Android colors:
- `android.R.color.holo_green_dark`
- `android.R.color.holo_red_dark`

### Cause
Those colors did not match the current KoffeeCraft theme and made the validation UI look inconsistent.

### Fix
I replaced them with app theme colors:
- `R.color.kc_success_text`
- `R.color.kc_danger_text`

Affected Kotlin files:
- `RegisterFragment.kt`
- `CustomerChangePasswordFragment.kt`
- `AdminCreateAccountFragment.kt`

---

### Result
After these fixes:
- the app builds again
- the affected theme crashes are removed
- the XML and Kotlin files use more consistent theme colors
- dark mode is much more stable and visually aligned with the KoffeeCraft design
- the login and admin menu flows work again after restoring the missing layout IDs