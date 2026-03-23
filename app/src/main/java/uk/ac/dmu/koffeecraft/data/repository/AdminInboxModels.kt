package uk.ac.dmu.koffeecraft.data.repository

enum class AdminInboxTargetMode {
    ORDER_NUMBER,
    CUSTOMER_ID
}

enum class AdminInboxMessageType {
    CUSTOM,
    IMPORTANT,
    SERVICE
}