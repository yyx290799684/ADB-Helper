package com.yangyx.adbhelper.data.repository

import android.content.Context
import com.yangyx.adbhelper.data.dao.CommandDao
import com.yangyx.adbhelper.data.dao.DeviceDao
import com.yangyx.adbhelper.data.entity.CommandEntity
import com.yangyx.adbhelper.data.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class AdbRepository(
    private val deviceDao: DeviceDao,
    private val commandDao: CommandDao,
    private val context: Context? = null
) {
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()

    private val prefs by lazy {
        context?.getSharedPreferences("adb_devices_backup_prefs", Context.MODE_PRIVATE)
    }

    suspend fun checkAndRestoreBackupIfNeeded() {
        if (prefs == null) return
        try {
            val current = deviceDao.getAllDevicesDirect()
            if (current.isEmpty()) {
                val backupJson = prefs?.getString("saved_devices_json", "") ?: ""
                if (backupJson.isNotBlank()) {
                    val restored = parseDevicesFromJson(backupJson)
                    if (restored.isNotEmpty()) {
                        for (dev in restored) {
                            deviceDao.insertOrUpdateDevice(dev.copy(id = 0))
                        }
                    }
                }
            } else {
                syncBackupFromDb()
            }
        } catch (_: Exception) {}
    }

    private suspend fun syncBackupFromDb() {
        if (prefs == null) return
        try {
            val devices = deviceDao.getAllDevicesDirect()
            if (devices.isNotEmpty()) {
                val json = serializeDevicesToJson(devices)
                prefs?.edit()?.putString("saved_devices_json", json)?.apply()
            }
        } catch (_: Exception) {}
    }

    private fun serializeDevicesToJson(devices: List<DeviceEntity>): String {
        val array = JSONArray()
        for (d in devices) {
            val obj = JSONObject()
            obj.put("serialNo", d.serialNo)
            obj.put("ipAddress", d.ipAddress)
            obj.put("port", d.port)
            obj.put("name", d.name)
            obj.put("model", d.model)
            obj.put("aliasName", d.aliasName)
            obj.put("lastConnectedTime", d.lastConnectedTime)
            obj.put("sortOrder", d.sortOrder)
            obj.put("isFavorite", d.isFavorite)
            obj.put("lastUsedBitrate", d.lastUsedBitrate)
            obj.put("lastUsedResolution", d.lastUsedResolution)
            obj.put("iconType", d.iconType)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseDevicesFromJson(jsonStr: String): List<DeviceEntity> {
        val list = mutableListOf<DeviceEntity>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DeviceEntity(
                        serialNo = obj.optString("serialNo", ""),
                        ipAddress = obj.optString("ipAddress", ""),
                        port = obj.optInt("port", 5555),
                        name = obj.optString("name", ""),
                        model = obj.optString("model", ""),
                        aliasName = obj.optString("aliasName", ""),
                        lastConnectedTime = obj.optLong("lastConnectedTime", System.currentTimeMillis()),
                        sortOrder = obj.optInt("sortOrder", 0),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        lastUsedBitrate = obj.optInt("lastUsedBitrate", 4000000),
                        lastUsedResolution = obj.optInt("lastUsedResolution", 1080),
                        iconType = obj.optString("iconType", "phone")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    suspend fun getAllDevicesDirect(): List<DeviceEntity> {
        return deviceDao.getAllDevicesDirect()
    }

    suspend fun getDeviceByIpAndPort(ip: String, port: Int): DeviceEntity? {
        return deviceDao.getDeviceByIpAndPort(ip, port)
    }

    suspend fun saveDevice(device: DeviceEntity) {
        deviceDao.insertOrUpdateDevice(device)
        syncBackupFromDb()
    }

    suspend fun updateDevices(devices: List<DeviceEntity>) {
        deviceDao.updateDevices(devices)
        syncBackupFromDb()
    }

    suspend fun deleteDevice(ip: String) {
        deviceDao.deleteDeviceByIp(ip)
        syncBackupFromDb()
    }

    suspend fun deleteDeviceById(id: Long) {
        deviceDao.deleteDeviceById(id)
        syncBackupFromDb()
    }

    suspend fun deleteDeviceByIpAndPort(ip: String, port: Int) {
        deviceDao.deleteDeviceByIpAndPort(ip, port)
        syncBackupFromDb()
    }

    suspend fun deleteDevicesBySerialNo(serialNo: String) {
        deviceDao.deleteDevicesBySerialNo(serialNo)
        syncBackupFromDb()
    }

    suspend fun updateDeviceAliasAndIcon(serialNo: String, id: Long, aliasName: String, iconType: String) {
        if (serialNo.isNotBlank()) {
            deviceDao.updateAliasAndIconBySerial(serialNo, aliasName, iconType)
        } else if (id > 0) {
            deviceDao.updateAliasAndIconById(id, aliasName, iconType)
        }
        syncBackupFromDb()
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

