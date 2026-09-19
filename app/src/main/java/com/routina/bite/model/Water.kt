package com.routina.bite.model

import kotlinx.serialization.Serializable

/**
 * 一天喝的水，單位毫升。
 *
 * 只存一天的總量，不存每一次喝了多少——記錄的當下沒人想看「早上 10:03 喝了 250」，
 * 想知道的是「今天到底喝夠了沒」。按錯就按減回去。
 * 清單裡沒有那一天，就是那天還沒喝。
 */
@Serializable
data class WaterDay(val date: String, val ml: Int)
