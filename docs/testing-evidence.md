## Batch 1 – Unit Tests
Date: 2026-03-23

Tests included:
- PasswordRulesValidatorTest
- DateOfBirthValidatorTest
- PaymentCardValidatorTest
- CheckoutCardFormValidatorTest
- BeansBoosterManagerTest
- AdminMenuProductValidatorTest

Result:
- 43 tests passed

Evidence:
- docs/evidence/testing/unit-batch-1-overview1.png
- docs/evidence/testing/unit-batch-1-overview2.png

Notes:
- This batch focused on validation logic, payment input handling, rewards logic, and admin product form validation.

Validation hardening update:
- Password validation was improved so whitespace no longer counts as a special character.
- Date of birth validation was improved to reject impossible dates and future dates.
- Batch 1 unit tests were re-run after these validation changes.


## Batch 2 – Instrumented Repository Tests
Date: 2026-03-23

Tests included:
- AuthRepositoryInstrumentedTest
- CheckoutRepositoryInstrumentedTest
- FeedbackRepositoryInstrumentedTest
- AdminOrdersRepositoryInstrumentedTest

Focus:
- authentication and registration flow
- order submission and payment persistence
- feedback saving and review progression
- admin order management and notification creation

Evidence:
- docs/evidence/testing/instrumented-batch-2-overview.png



## Batch 3 – Instrumented DAO Tests
Date: 2026-03-23

Tests included:
- CustomerPaymentCardDaoInstrumentedTest
- OrderItemDaoInstrumentedTest
- FeedbackDaoInstrumentedTest
- NotificationDaoInstrumentedTest

Focus:
- saved payment card persistence and default card selection
- order item query behaviour for reorder, feedback, and display
- feedback aggregation and moderation state changes
- notification queries, unread counts, and read state updates

Evidence:
- docs/evidence/testing/dao-batch-3-overview.png


## Batch 4 – UI Smoke Tests
Date: 2026-03-23

Tests included:
- CustomerFlowEspressoTest
- AdminFlowEspressoTest

Focus:
- welcome screen navigation
- registration validation and successful onboarding entry
- customer login and customer shell navigation
- admin login and admin orders screen access

Evidence:
- docs/evidence/testing/ui-batch-4-overview.png