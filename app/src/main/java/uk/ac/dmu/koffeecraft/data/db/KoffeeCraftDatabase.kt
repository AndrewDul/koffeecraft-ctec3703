package uk.ac.dmu.koffeecraft.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import uk.ac.dmu.koffeecraft.data.dao.AddOnDao
import uk.ac.dmu.koffeecraft.data.dao.AdminDao
import uk.ac.dmu.koffeecraft.data.dao.AllergenDao
import uk.ac.dmu.koffeecraft.data.dao.CustomerDao
import uk.ac.dmu.koffeecraft.data.dao.CustomerFavouritePresetDao
import uk.ac.dmu.koffeecraft.data.dao.CustomerPaymentCardDao
import uk.ac.dmu.koffeecraft.data.dao.FavouriteDao
import uk.ac.dmu.koffeecraft.data.dao.FeedbackDao
import uk.ac.dmu.koffeecraft.data.dao.InboxMessageDao
import uk.ac.dmu.koffeecraft.data.dao.NotificationDao
import uk.ac.dmu.koffeecraft.data.dao.OrderDao
import uk.ac.dmu.koffeecraft.data.dao.OrderItemDao
import uk.ac.dmu.koffeecraft.data.dao.PaymentDao
import uk.ac.dmu.koffeecraft.data.dao.ProductDao
import uk.ac.dmu.koffeecraft.data.dao.ProductOptionDao
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.AddOnAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Admin
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import uk.ac.dmu.koffeecraft.data.entities.Customer
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePreset
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePresetAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.data.entities.Favourite
import uk.ac.dmu.koffeecraft.data.entities.Feedback
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.entities.OrderItem
import uk.ac.dmu.koffeecraft.data.entities.Payment
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

@Database(
    entities = [
        CustomerPaymentCard::class,
        Customer::class,
        Admin::class,
        Product::class,
        Order::class,
        OrderItem::class,
        Payment::class,
        Feedback::class,
        AppNotification::class,
        InboxMessage::class,
        AddOn::class,
        ProductOption::class,
        Allergen::class,
        ProductAddOnCrossRef::class,
        ProductAllergenCrossRef::class,
        AddOnAllergenCrossRef::class,
        Favourite::class,
        CustomerFavouritePreset::class,
        CustomerFavouritePresetAddOnCrossRef::class
    ],
    version = 18,
    exportSchema = true
)
abstract class KoffeeCraftDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun adminDao(): AdminDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun notificationDao(): NotificationDao
    abstract fun inboxMessageDao(): InboxMessageDao
    abstract fun addOnDao(): AddOnDao
    abstract fun productOptionDao(): ProductOptionDao
    abstract fun allergenDao(): AllergenDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun customerFavouritePresetDao(): CustomerFavouritePresetDao
    abstract fun customerPaymentCardDao(): CustomerPaymentCardDao

    companion object {
        @Volatile
        private var INSTANCE: KoffeeCraftDatabase? = null

        fun getInstance(context: Context): KoffeeCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KoffeeCraftDatabase::class.java,
                    "koffeecraft.db"
                )
                    .addMigrations(*DatabaseMigrations.ALL)
                    .addCallback(DatabaseSeeder.create())
                    .build()

                INSTANCE = instance
                instance
            }
        }

        internal fun getInstanceHolder(): KoffeeCraftDatabase? = INSTANCE
    }
}