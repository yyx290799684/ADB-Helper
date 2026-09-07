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
    @Query("SELECT * FROM devices ORDER BY isFavorite DESC, sortOrder ASC, lastConnectedTime DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices")
    suspend fun getAllDevicesDirect(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE ipAddress = :ip AND port = :port LIMIT 1")
    suspend fun getDeviceByIpAndPort(ip: String, port: Int): DeviceEntity?

    @Query("SELECT * FROM devices WHERE ipAddress = :ip LIMIT 1")
    suspend fun getDeviceByIp(ip: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE serialNo = :serialNo")
    suspend fun getDevicesBySerialNo(serialNo: String): List<DeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity): Long

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)

    @Query("DELETE FROM devices WHERE ipAddress = :ip AND port = :port")
    suspend fun deleteDeviceByIpAndPort(ip: String, port: Int)

    @Query("DELETE FROM devices WHERE ipAddress = :ip")
    suspend fun deleteDeviceByIp(ip: String)

    @Query("DELETE FROM devices WHERE serialNo = :serialNo AND serialNo != ''")
    suspend fun deleteDevicesBySerialNo(serialNo: String)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Update
    suspend fun updateDevices(devices: List<DeviceEntity>)

    @Query("UPDATE devices SET aliasName = :aliasName, iconType = :iconType WHERE serialNo = :serialNo AND serialNo != ''")
    suspend fun updateAliasAndIconBySerial(serialNo: String, aliasName: String, iconType: String)

    @Query("UPDATE devices SET aliasName = :aliasName, iconType = :iconType WHERE id = :id")
    suspend fun updateAliasAndIconById(id: Long, aliasName: String, iconType: String)
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
