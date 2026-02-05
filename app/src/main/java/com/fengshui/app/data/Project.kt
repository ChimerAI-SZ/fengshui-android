package com.fengshui.app.data

data class Project(
    val id: String,
    val name: String,
    val description: String? = null,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)
