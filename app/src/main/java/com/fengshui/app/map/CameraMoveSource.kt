package com.fengshui.app.map

enum class CameraMoveSource(val priority: Int) {
    GPS_AUTO_LOCATE(1),
    MAP_INIT(2),
    USER_POINT_SELECT(3),
    SEARCH_RESULT(4),
    USER_MANUAL(5)
}
