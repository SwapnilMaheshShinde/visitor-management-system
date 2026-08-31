package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.VmsDao
import com.example.data.local.entity.AppointmentEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.VisitEntity
import com.example.data.local.entity.VisitRequestEntity

@Database(
    entities = [
        VisitEntity::class,
        VisitRequestEntity::class,
        AppointmentEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VmsDatabase : RoomDatabase() {
    abstract fun vmsDao(): VmsDao

    companion object {
        @Volatile
        private var INSTANCE: VmsDatabase? = null

        fun getInstance(context: Context): VmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VmsDatabase::class.java,
                    "vms_enterprise_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
