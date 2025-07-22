package com.weather.alarm.batch.infrastructure.scheduler

// 매일 2,5,8,11,14,17,20,23시 15분
const val COLLECT_WEATHER_DATA_DAILY = "0 15 2,5,8,11,14,17,20,23 * * *"

// 오전 8시~오후 10시, 30분마다
const val COLLECT_WEATHER_IN_ACTIVE_TIME_PER_30M = "0 */30 8-22 * * *"

// 매 시간
const val CHECK_NOTIFICATION_INFO_HOURLY = "0 0 * * * *"
