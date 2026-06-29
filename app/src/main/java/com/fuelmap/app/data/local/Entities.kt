package com.fuelmap.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fuelmap.app.domain.model.ConfirmationType
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.Queue
import com.fuelmap.app.domain.model.Role

@Entity(
    tableName = "users",
    indices = [Index(value = ["login"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val login: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val carPlate: String,
    val region: String,
    val role: Role = Role.USER,
    val karma: Int = 0,
    val isBanned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "regions")
data class RegionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val minLat: Double,
    val minLng: Double,
    val maxLat: Double,
    val maxLng: Double
) {
    val centerLat: Double get() = (minLat + maxLat) / 2
    val centerLng: Double get() = (minLng + maxLng) / 2

    fun contains(lat: Double, lng: Double): Boolean =
        lat in minLat..maxLat && lng in minLng..maxLng
}

@Entity(
    tableName = "gas_stations",
    foreignKeys = [
        ForeignKey(
            entity = RegionEntity::class,
            parentColumns = ["id"],
            childColumns = ["regionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("regionId"), Index(value = ["lat", "lng"], unique = true)]
)
data class GasStationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val regionId: Long
)

@Entity(
    tableName = "fuel_marks",
    foreignKeys = [
        ForeignKey(
            entity = GasStationEntity::class,
            parentColumns = ["id"],
            childColumns = ["stationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stationId"), Index("userId"), Index("isCurrent")]
)
data class FuelMarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stationId: Long,
    val userId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isCurrent: Boolean = true,
    val queue: Queue = Queue.NONE,
    val confirmCount: Int = 0,
    val emptyCount: Int = 0
)

@Entity(
    tableName = "fuel_mark_items",
    foreignKeys = [
        ForeignKey(
            entity = FuelMarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["markId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("markId")]
)
data class FuelMarkItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val markId: Long,
    val type: FuelType,
    val available: Boolean,
    val price: Double
)

@Entity(
    tableName = "confirmations",
    indices = [Index("markId"), Index(value = ["markId", "userId"], unique = true)]
)
data class ConfirmationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val markId: Long,
    val userId: Long,
    val type: ConfirmationType,
    val createdAt: Long = System.currentTimeMillis()
)
