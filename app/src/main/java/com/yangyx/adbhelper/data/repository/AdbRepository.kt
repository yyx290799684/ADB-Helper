package com.yangyx.adbhelper.data.repository

import com.yangyx.adbhelper.data.dao.CommandDao
import com.yangyx.adbhelper.data.dao.DeviceDao
import com.yangyx.adbhelper.data.entity.CommandEntity
import com.yangyx.adbhelper.data.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

class AdbRepository(
    private val deviceDao: DeviceDao,
    private val commandDao: CommandDao
) {
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()

    suspend fun saveDevice(device: DeviceEntity) {
        deviceDao.insertOrUpdateDevice(device)
    }

    suspend fun deleteDevice(ip: String) {
        deviceDao.deleteDeviceByIp(ip)
    }

    fun getCommandHistory(ip: String): Flow<List<CommandEntity>> {
        return commandDao.getCommandHistory(ip)
    }

    suspend fun saveCommand(ip: String, command: String, isSuccess: Boolean) {
        commandDao.insertCommand(
            CommandEntity(
                ipAddress = ip,
                command = command,
                isSuccess = isSuccess
            )
        )
    }

    suspend fun clearCommandHistory(ip: String) {
        commandDao.clearHistory(ip)
    }
}
