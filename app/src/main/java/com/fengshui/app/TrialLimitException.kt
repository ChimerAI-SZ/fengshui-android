package com.fengshui.app

class TrialLimitException(message: String, val limitType: LimitType) : Exception(message) {
    enum class LimitType {
        GROUP, ORIGIN, DESTINATION
    }
}
