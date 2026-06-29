package com.fuelmap.app.ui.navigation

object Routes {
    const val MAP = "map"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RESET = "reset"
    const val PROFILE = "profile"
    const val LEADERBOARD = "leaderboard"

    const val STATION = "station/{stationId}"
    fun station(id: Long) = "station/$id"

    const val MARK = "mark/{stationId}"
    fun mark(id: Long) = "mark/$id"

    const val ADMIN = "admin"
    const val ADMIN_REGIONS = "admin/regions"
    const val ADMIN_ADD_STATION = "admin/add_station"
    const val ADMIN_USER = "admin/user/{userId}"
    fun adminUser(id: Long) = "admin/user/$id"

    const val ARG_STATION_ID = "stationId"
    const val ARG_USER_ID = "userId"
}
