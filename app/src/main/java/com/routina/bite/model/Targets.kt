package com.routina.bite.model

import kotlinx.serialization.Serializable

/**
 * 每日目標。熱量、蛋白質與喝水一定有值，脂肪與碳水可以不設（不設就只顯示吃了多少）。
 */
@Serializable
data class Targets(
    val kcal: Int = 2000,
    val protein: Int = 120,
    val fat: Int? = null,
    val carbs: Int? = null,
    /** 每日喝水目標，毫升 */
    val water: Int = 2000
)
