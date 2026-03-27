package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePreset
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePresetAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.querymodel.CustomerFavouritePresetCard
@Dao
interface CustomerFavouritePresetDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPreset(preset: CustomerFavouritePreset): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddOnRefs(refs: List<CustomerFavouritePresetAddOnCrossRef>)

    @Query("""
        DELETE FROM customer_favourite_preset_add_on_cross_ref
        WHERE presetId = :presetId
    """)
    suspend fun deleteAddOnRefsForPreset(presetId: Long)

    @Query("""
        DELETE FROM customer_favourite_presets
        WHERE presetId = :presetId
    """)
    suspend fun deletePresetById(presetId: Long)

    @Query("""
        SELECT * FROM customer_favourite_presets
        WHERE presetId = :presetId
        LIMIT 1
    """)
    suspend fun getPresetById(presetId: Long): CustomerFavouritePreset?

    @Query(
        """
    SELECT
        cp.presetId AS presetId,
        cp.productId AS productId,
        p.name AS productName,
        p.description AS productDescription,
        p.category AS productFamily,
        p.imageKey AS imageKey,
        p.customImagePath AS customImagePath,
        cp.optionId AS optionId,
        po.displayLabel AS optionLabel,
        po.sizeValue AS optionSizeValue,
        po.sizeUnit AS optionSizeUnit,
        cp.totalPriceSnapshot AS totalPrice,
        cp.totalCaloriesSnapshot AS totalCalories,
        GROUP_CONCAT(a.name, ', ') AS addOnSummary,
        cp.createdAt AS createdAt
    FROM customer_favourite_presets cp
    INNER JOIN products p ON p.productId = cp.productId
    INNER JOIN product_options po ON po.optionId = cp.optionId
    LEFT JOIN customer_favourite_preset_add_on_cross_ref cpa ON cpa.presetId = cp.presetId
    LEFT JOIN add_ons a ON a.addOnId = cpa.addOnId
    WHERE cp.customerId = :customerId
      AND p.isAvailable = 1
    GROUP BY
        cp.presetId,
        cp.productId,
        p.name,
        p.description,
        p.category,
        p.imageKey,
        p.customImagePath,
        cp.optionId,
        po.displayLabel,
        po.sizeValue,
        po.sizeUnit,
        cp.totalPriceSnapshot,
        cp.totalCaloriesSnapshot,
        cp.createdAt
    ORDER BY cp.createdAt DESC
    """
    )
    fun observePresetCardsForCustomer(customerId: Long): Flow<List<CustomerFavouritePresetCard>>

    @Query("""
        SELECT a.*
        FROM add_ons a
        INNER JOIN customer_favourite_preset_add_on_cross_ref cpa ON cpa.addOnId = a.addOnId
        WHERE cpa.presetId = :presetId
        ORDER BY a.name ASC
    """)
    suspend fun getAddOnsForPreset(presetId: Long): List<AddOn>
}

