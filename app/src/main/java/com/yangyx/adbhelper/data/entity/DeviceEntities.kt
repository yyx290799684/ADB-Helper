package com.yangyx.adbhelper.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    val ipAddress: String,
    val port: Int = 5555,
    val name: String = "",
    val model: String = "",
    val aliasName: String = "",
    val lastConnectedTime: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val lastUsedBitrate: Int = 4000000,
    val lastUsedResolution: Int = 1080
)

@Entity(tableName = "command_history")
data class CommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ipAddress: String,
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)
