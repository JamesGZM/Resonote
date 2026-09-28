package com.resonote.core.database.vip

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vip_check_in", primaryKeys = ["userId", "date"])
data class VipCheckInEntity(val userId: String, val date: String, val signed: Boolean, val upgraded: Boolean)

@Dao
abstract class VipCheckInDao {
    @Query("SELECT * FROM vip_check_in WHERE userId = :userId ORDER BY date DESC")
    abstract fun observe(userId: String): Flow<List<VipCheckInEntity>>

    @Query("SELECT * FROM vip_check_in WHERE userId = :userId AND date = :date")
    abstract suspend fun find(userId: String, date: String): VipCheckInEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(record: VipCheckInEntity)

    @Transaction
    open suspend fun merge(record: VipCheckInEntity) {
        val old = find(record.userId, record.date)
        insert(
            record.copy(
                signed = record.signed || old?.signed == true,
                upgraded =
                record.upgraded || old?.upgraded == true,
            ),
        )
    }
}
