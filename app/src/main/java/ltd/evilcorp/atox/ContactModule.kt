package ltd.evilcorp.atox

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ContactModule(application: Application) {
    private val db = Room.databaseBuilder(application, ContactDatabase::class.java, "contact_db")
        .allowMainThreadQueries()
        .build()

    @Singleton
    @Provides
    fun provideDatabase(): ContactDatabase {
        return db
    }

    @Singleton
    @Provides
    fun provideDao(): ContactDao {
        return db.contactDao()
    }
}