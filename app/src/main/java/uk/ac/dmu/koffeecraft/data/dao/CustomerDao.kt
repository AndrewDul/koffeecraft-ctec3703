package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import uk.ac.dmu.koffeecraft.data.entities.Customer
import uk.ac.dmu.koffeecraft.data.querymodel.CustomerAccountTarget
import uk.ac.dmu.koffeecraft.data.querymodel.CustomerCampaignTarget
import uk.ac.dmu.koffeecraft.data.querymodel.CustomerInboxTarget
@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Query("SELECT * FROM customers WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): Customer?

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    suspend fun getById(customerId: Long): Customer?

    @Query(
        """
        SELECT
            customerId AS customerId,
            firstName AS firstName,
            lastName AS lastName,
            email AS email,
            dateOfBirth AS dateOfBirth,
            marketingInboxConsent AS marketingInboxConsent
        FROM customers
        ORDER BY customerId ASC
        """
    )
    suspend fun getAllInboxTargets(): List<CustomerInboxTarget>

    @Query(
        """
        SELECT
            customerId AS customerId,
            firstName AS firstName,
            lastName AS lastName,
            email AS email,
            dateOfBirth AS dateOfBirth,
            marketingInboxConsent AS marketingInboxConsent
        FROM customers
        WHERE dateOfBirth IS NOT NULL
          AND substr(dateOfBirth, 6, 5) = :monthDay
        ORDER BY customerId ASC
        """
    )
    suspend fun getBirthdayInboxTargets(monthDay: String): List<CustomerInboxTarget>

    @Query(
        """
        SELECT
            customerId AS customerId,
            firstName AS firstName,
            lastName AS lastName,
            email AS email,
            dateOfBirth AS dateOfBirth,
            marketingInboxConsent AS marketingInboxConsent
        FROM customers
        WHERE customerId = :customerId
        LIMIT 1
        """
    )
    suspend fun getInboxTargetByCustomerId(customerId: Long): CustomerInboxTarget?

    @Query(
        """
        SELECT
            c.customerId AS customerId,
            c.firstName AS firstName,
            c.lastName AS lastName,
            c.email AS email,
            c.dateOfBirth AS dateOfBirth,
            c.marketingInboxConsent AS marketingInboxConsent
        FROM customers c
        INNER JOIN orders o ON o.customerId = c.customerId
        WHERE o.orderId = :orderId
        LIMIT 1
        """
    )
    suspend fun getInboxTargetByOrderId(orderId: Long): CustomerInboxTarget?

    @Query(
        """
        SELECT
            customerId AS customerId,
            firstName AS firstName,
            lastName AS lastName,
            email AS email,
            isActive AS isActive,
            createdAt AS createdAt
        FROM customers
        WHERE customerId = :customerId
        LIMIT 1
        """
    )
    suspend fun getAccountTargetByCustomerId(customerId: Long): CustomerAccountTarget?

    @Query(
        """
        SELECT
            c.customerId AS customerId,
            c.firstName AS firstName,
            c.lastName AS lastName,
            c.email AS email,
            c.isActive AS isActive,
            c.createdAt AS createdAt
        FROM customers c
        INNER JOIN orders o ON o.customerId = c.customerId
        WHERE o.orderId = :orderId
        LIMIT 1
        """
    )
    suspend fun getAccountTargetByOrderId(orderId: Long): CustomerAccountTarget?

    @Query(
        """
        UPDATE customers
        SET isActive = :isActive
        WHERE customerId = :customerId
        """
    )
    suspend fun updateActiveStatus(customerId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM customers WHERE isActive = 1")
    suspend fun countActiveCustomers(): Int

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun countAllCustomers(): Int

    @Query("SELECT COUNT(*) FROM customers WHERE marketingInboxConsent = 1")
    suspend fun countPromoOptInCustomers(): Int

    @Query(
        """
        SELECT
            c.customerId AS customerId,
            c.firstName AS firstName,
            c.lastName AS lastName,
            c.email AS email,
            c.dateOfBirth AS dateOfBirth,
            c.marketingInboxConsent AS marketingInboxConsent,
            c.beansBalance AS beansBalance,
            COUNT(o.orderId) AS orderCount,
            MAX(o.createdAt) AS lastOrderAt,
            COALESCE(SUM(o.totalAmount), 0) AS lifetimeSpend
        FROM customers c
        LEFT JOIN orders o ON o.customerId = c.customerId
        GROUP BY
            c.customerId,
            c.firstName,
            c.lastName,
            c.email,
            c.dateOfBirth,
            c.marketingInboxConsent,
            c.beansBalance
        ORDER BY c.customerId ASC
        """
    )
    suspend fun getAllCampaignTargets(): List<CustomerCampaignTarget>

    @Query(
        """
        SELECT
            c.customerId AS customerId,
            c.firstName AS firstName,
            c.lastName AS lastName,
            c.email AS email,
            c.dateOfBirth AS dateOfBirth,
            c.marketingInboxConsent AS marketingInboxConsent,
            c.beansBalance AS beansBalance,
            COUNT(o.orderId) AS orderCount,
            MAX(o.createdAt) AS lastOrderAt,
            COALESCE(SUM(o.totalAmount), 0) AS lifetimeSpend
        FROM customers c
        LEFT JOIN orders o ON o.customerId = c.customerId
        WHERE c.customerId = :customerId
        GROUP BY
            c.customerId,
            c.firstName,
            c.lastName,
            c.email,
            c.dateOfBirth,
            c.marketingInboxConsent,
            c.beansBalance
        LIMIT 1
        """
    )
    suspend fun getCampaignTargetByCustomerId(customerId: Long): CustomerCampaignTarget?

    @Query(
        """
        SELECT
            c.customerId AS customerId,
            c.firstName AS firstName,
            c.lastName AS lastName,
            c.email AS email,
            c.dateOfBirth AS dateOfBirth,
            c.marketingInboxConsent AS marketingInboxConsent,
            c.beansBalance AS beansBalance,
            COUNT(oAll.orderId) AS orderCount,
            MAX(oAll.createdAt) AS lastOrderAt,
            COALESCE(SUM(oAll.totalAmount), 0) AS lifetimeSpend
        FROM customers c
        LEFT JOIN orders oAll ON oAll.customerId = c.customerId
        WHERE c.customerId = (
            SELECT customerId
            FROM orders
            WHERE orderId = :orderId
            LIMIT 1
        )
        GROUP BY
            c.customerId,
            c.firstName,
            c.lastName,
            c.email,
            c.dateOfBirth,
            c.marketingInboxConsent,
            c.beansBalance
        LIMIT 1
        """
    )
    suspend fun getCampaignTargetByOrderId(orderId: Long): CustomerCampaignTarget?

    @Query(
        """
        UPDATE customers
        SET beansBalance = beansBalance + :beansAmount
        WHERE customerId = :customerId
        """
    )
    suspend fun addBeansToCustomer(customerId: Long, beansAmount: Int)
}
