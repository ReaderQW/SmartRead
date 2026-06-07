package com.example.utils

/**
 * 后台任务管理配置
 */
object BackgroundTaskConfig {
    // 同步间隔
    const val SYNC_INTERVAL_MINUTES = 30L
    const val FORCE_SYNC_INTERVAL_MINUTES = 60L

    // 重试配置
    const val MAX_RETRIES = 3
    const val INITIAL_BACKOFF_SECONDS = 15L
    const val MAX_BACKOFF_SECONDS = 3600L
    const val BACKOFF_MULTIPLIER = 2f

    // 电池和网络约束
    const val REQUIRES_BATTERY_LEVEL_PERCENT = 20
    const val REQUIRES_WIFI = false
}

/**
 * 数据同步状态
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    FAILED
}

/**
 * 任务优先级
 */
enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    IMMEDIATE
}