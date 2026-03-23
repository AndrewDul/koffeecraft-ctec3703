package uk.ac.dmu.koffeecraft.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.testsupport.BaseInstrumentedDatabaseTest
import uk.ac.dmu.koffeecraft.testsupport.TestSeedData

@RunWith(AndroidJUnit4::class)
class CustomerPaymentCardDaoInstrumentedTest : BaseInstrumentedDatabaseTest() {

    @Test
    fun getDefaultForCustomer_andOrdering_returnExpectedCards() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "cards@example.com"
        )

        val firstCardId = db.customerPaymentCardDao().insert(
            CustomerPaymentCard(
                customerId = customerId,
                nickname = "Personal Visa",
                cardholderName = "Andrew Dul",
                brand = "VISA",
                maskedCardNumber = "•••• •••• •••• 1111",
                last4 = "1111",
                expiryMonth = 12,
                expiryYear = 2029,
                isDefault = true,
                createdAt = 1000L
            )
        )

        val secondCardId = db.customerPaymentCardDao().insert(
            CustomerPaymentCard(
                customerId = customerId,
                nickname = "Backup Mastercard",
                cardholderName = "Andrew Dul",
                brand = "MASTERCARD",
                maskedCardNumber = "•••• •••• •••• 2222",
                last4 = "2222",
                expiryMonth = 11,
                expiryYear = 2030,
                isDefault = false,
                createdAt = 2000L
            )
        )

        val defaultBefore = db.customerPaymentCardDao().getDefaultForCustomer(customerId)
        assertEquals(firstCardId, defaultBefore?.cardId)

        db.customerPaymentCardDao().clearDefaultForCustomer(customerId)
        db.customerPaymentCardDao().setDefault(secondCardId, customerId)

        val defaultAfter = db.customerPaymentCardDao().getDefaultForCustomer(customerId)
        val allCards = db.customerPaymentCardDao().getAllForCustomer(customerId)
        val mostRecent = db.customerPaymentCardDao().getMostRecentForCustomer(customerId)

        assertEquals(secondCardId, defaultAfter?.cardId)
        assertEquals(2, allCards.size)
        assertEquals(secondCardId, allCards.first().cardId)
        assertTrue(allCards.first().isDefault)
        assertEquals(secondCardId, mostRecent?.cardId)
    }

    @Test
    fun updateNickname_andDeleteByIdAndCustomer_modifyStoredCards() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "cards2@example.com"
        )

        val cardId = db.customerPaymentCardDao().insert(
            CustomerPaymentCard(
                customerId = customerId,
                nickname = "Old name",
                cardholderName = "Andrew Dul",
                brand = "VISA",
                maskedCardNumber = "•••• •••• •••• 3333",
                last4 = "3333",
                expiryMonth = 8,
                expiryYear = 2031,
                isDefault = true
            )
        )

        db.customerPaymentCardDao().updateNickname(
            cardId = cardId,
            customerId = customerId,
            nickname = "Main Visa"
        )

        val renamed = db.customerPaymentCardDao().getAllForCustomer(customerId).single()
        assertEquals("Main Visa", renamed.nickname)

        db.customerPaymentCardDao().deleteByIdAndCustomer(
            cardId = cardId,
            customerId = customerId
        )

        val afterDelete = db.customerPaymentCardDao().getAllForCustomer(customerId)
        assertTrue(afterDelete.isEmpty())
    }
}