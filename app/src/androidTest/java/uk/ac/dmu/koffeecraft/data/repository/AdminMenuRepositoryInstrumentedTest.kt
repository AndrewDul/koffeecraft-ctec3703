package uk.ac.dmu.koffeecraft.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.testsupport.BaseInstrumentedDatabaseTest

@RunWith(AndroidJUnit4::class)
class AdminMenuRepositoryInstrumentedTest : BaseInstrumentedDatabaseTest() {

    private lateinit var repository: AdminMenuRepository
    private lateinit var tempDir: File

    @Before
    fun setUpRepository() {
        repository = AdminMenuRepository(db)
        tempDir = File(context.filesDir, "admin_menu_repository_test")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
    }

    @After
    fun tearDownTempFiles() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun createProduct_persistsImageKey_andCustomImagePath() = runBlocking {
        val customPath = createFakeImageFile("created_custom_image.jpg").absolutePath

        val created = repository.createProduct(
            name = "Image Test Latte",
            productFamily = "COFFEE",
            description = "Coffee with stored image fields",
            price = 4.90,
            rewardEnabled = true,
            isNew = true,
            imageKey = "coffee_signature_house",
            customImagePath = customPath
        )

        assertNotNull(created)

        val stored = db.productDao().getById(created!!.productId)
        assertNotNull(stored)
        assertEquals("coffee_signature_house", stored!!.imageKey)
        assertEquals(customPath, stored.customImagePath)
    }

    @Test
    fun updateProduct_deletesPreviousCustomImage_whenSwitchingToAppLibrary() = runBlocking {
        val oldCustomFile = createFakeImageFile("old_custom_to_library.jpg")
        val existing = insertProduct(
            name = "Switch To Library",
            imageKey = null,
            customImagePath = oldCustomFile.absolutePath
        )

        val updated = repository.updateProduct(
            existing = existing,
            name = existing.name,
            productFamily = existing.productFamily,
            description = existing.description,
            price = existing.price,
            rewardEnabled = existing.rewardEnabled,
            isNew = existing.isNew,
            imageKey = "coffee_signature_house",
            customImagePath = null
        )

        assertNotNull(updated)
        assertFalse(oldCustomFile.exists())

        val stored = db.productDao().getById(existing.productId)
        assertNotNull(stored)
        assertEquals("coffee_signature_house", stored!!.imageKey)
        assertEquals(null, stored.customImagePath)
    }

    @Test
    fun updateProduct_deletesPreviousCustomImage_whenReplacingWithNewCustomImage() = runBlocking {
        val oldCustomFile = createFakeImageFile("old_custom_replace.jpg")
        val newCustomFile = createFakeImageFile("new_custom_replace.jpg")

        val existing = insertProduct(
            name = "Replace Custom",
            imageKey = null,
            customImagePath = oldCustomFile.absolutePath
        )

        val updated = repository.updateProduct(
            existing = existing,
            name = existing.name,
            productFamily = existing.productFamily,
            description = existing.description,
            price = existing.price,
            rewardEnabled = existing.rewardEnabled,
            isNew = existing.isNew,
            imageKey = null,
            customImagePath = newCustomFile.absolutePath
        )

        assertNotNull(updated)
        assertFalse(oldCustomFile.exists())
        assertTrue(newCustomFile.exists())

        val stored = db.productDao().getById(existing.productId)
        assertNotNull(stored)
        assertEquals(null, stored!!.imageKey)
        assertEquals(newCustomFile.absolutePath, stored.customImagePath)
    }

    @Test
    fun updateProduct_keepsExistingCustomImage_whenPathDidNotChange() = runBlocking {
        val existingCustomFile = createFakeImageFile("unchanged_custom.jpg")

        val existing = insertProduct(
            name = "Keep Same Custom",
            imageKey = null,
            customImagePath = existingCustomFile.absolutePath
        )

        val updated = repository.updateProduct(
            existing = existing,
            name = "Keep Same Custom Updated",
            productFamily = existing.productFamily,
            description = "Updated description",
            price = 5.10,
            rewardEnabled = existing.rewardEnabled,
            isNew = true,
            imageKey = null,
            customImagePath = existingCustomFile.absolutePath
        )

        assertNotNull(updated)
        assertTrue(existingCustomFile.exists())

        val stored = db.productDao().getById(existing.productId)
        assertNotNull(stored)
        assertEquals(existingCustomFile.absolutePath, stored!!.customImagePath)
        assertEquals("Keep Same Custom Updated", stored.name)
        assertEquals("Updated description", stored.description)
    }

    private suspend fun insertProduct(
        name: String,
        imageKey: String?,
        customImagePath: String?
    ): Product {
        val product = Product(
            name = name,
            productFamily = "COFFEE",
            description = "Repository image lifecycle test product",
            price = 4.50,
            isActive = true,
            isNew = false,
            imageKey = imageKey,
            customImagePath = customImagePath,
            rewardEnabled = true
        )

        val productId = db.productDao().insert(product)
        return product.copy(productId = productId)
    }

    private fun createFakeImageFile(fileName: String): File {
        val file = File(tempDir, fileName)
        file.writeText("fake-image-content-${System.nanoTime()}")
        return file
    }
}