package com.fuelmap.app.data.local

import androidx.room.Embedded
import androidx.room.Relation

/** Отметка вместе с её типами топлива. */
data class MarkWithItems(
    @Embedded val mark: FuelMarkEntity,
    @Relation(parentColumn = "id", entityColumn = "markId")
    val items: List<FuelMarkItemEntity>
) {
    val hasAvailableFuel: Boolean get() = items.any { it.available }
}

/** АЗС вместе с её текущим (актуальным) статусом, если он есть. */
data class StationWithCurrentMark(
    @Embedded val station: GasStationEntity,
    @Relation(
        entity = FuelMarkEntity::class,
        parentColumn = "id",
        entityColumn = "stationId"
    )
    val marks: List<MarkWithItems>
) {
    val currentMark: MarkWithItems? get() = marks.firstOrNull { it.mark.isCurrent }
}
