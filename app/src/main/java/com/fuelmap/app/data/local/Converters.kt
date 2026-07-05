package com.fuelmap.app.data.local

import androidx.room.TypeConverter
import com.fuelmap.app.domain.model.ConfirmationType
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.Queue
import com.fuelmap.app.domain.model.Role
import com.fuelmap.app.domain.model.StationStatus

class Converters {
    @TypeConverter fun roleToString(v: Role): String = v.name
    @TypeConverter fun stringToRole(v: String): Role = Role.valueOf(v)

    @TypeConverter fun fuelToString(v: FuelType): String = v.name
    @TypeConverter fun stringToFuel(v: String): FuelType = FuelType.valueOf(v)

    @TypeConverter fun queueToString(v: Queue): String = v.name
    @TypeConverter fun stringToQueue(v: String): Queue = Queue.valueOf(v)

    @TypeConverter fun confToString(v: ConfirmationType): String = v.name
    @TypeConverter fun stringToConf(v: String): ConfirmationType = ConfirmationType.valueOf(v)

    @TypeConverter fun statusToString(v: StationStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): StationStatus = StationStatus.valueOf(v)
}
