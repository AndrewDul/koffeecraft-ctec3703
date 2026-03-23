package uk.ac.dmu.koffeecraft.testsupport

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

abstract class BaseInstrumentedDatabaseTest {

    protected lateinit var context: Context
    protected lateinit var db: KoffeeCraftDatabase

    @Before
    fun setUpDatabase() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(
            context,
            KoffeeCraftDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDownDatabase() {
        db.close()
    }
}