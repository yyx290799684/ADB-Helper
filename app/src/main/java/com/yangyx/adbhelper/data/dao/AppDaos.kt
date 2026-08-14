package com.yangyx.adbhelper.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yangyx.adbhelper.data.entity.CommandEntity
import com.yangyx.adbhelper.data.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY isFavorite DESC, lastConnectedTime DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE ipAddress = :ip LIMIT 1")
    suspend fun getDeviceByIp(ip: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE ipAddress = :ip")
    suspend fun deleteDeviceByIp(ip: String)

    @Update
    suspend fun updateDevice(device: DeviceEntity)
}

@Dao
interface CommandDao {
    @Query("SELECT * FROM command_history WHERE ipAddress = :ip ORDER BY timestamp DESC LIMIT 50")
    fun getCommandHistory(ip: String): Flow<List<CommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CommandEntity)

    @Query("DELETE FROM command_history WHERE ipAddress = :ip")
    suspend fun clearHistory(ip: String)
}
