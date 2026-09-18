package com.routina.bite.family

import android.content.Intent

/**
 * 家族契約裡 Bite 用得到的部分。
 *
 * 契約只是字串層級的約定（manifest meta-data + Intent extras），不是程式碼依賴，
 * 所以這裡放自己的一份就好，不必引用 Hub 的 library——Routina Flow 也是這樣做的。
 * 權威定義在 Hub 的 `core/contract/Family.kt`，靠 [CONTRACT_VERSION] 對版。
 *
 * Bite 只「被呼叫」、不掃描別人，所以只需要接收端會用到的這幾個鍵。
 */
object Family {

    /** 契約版本。鍵名或 Intent 形狀改了才會 +1 */
    const val CONTRACT_VERSION = 1

    /** 請這個 App 執行某個能力。收件人是 exported 的跳板 Activity */
    const val ACTION_RUN_CAPABILITY = "com.routina.family.action.RUN_CAPABILITY"

    /** 要執行哪個能力：值是 res/xml/family_capabilities.xml 裡的 id */
    const val EXTRA_CAPABILITY_ID = "com.routina.family.extra.CAPABILITY_ID"

    /** 參數的 extras 前綴：`com.routina.family.param.<參數名>`，值一律字串 */
    const val EXTRA_PARAM_PREFIX = "com.routina.family.param."
}

/** 這次要執行哪個能力。沒指定或空字串都回 null */
fun capabilityId(intent: Intent?): String? =
    intent?.getStringExtra(Family.EXTRA_CAPABILITY_ID)?.takeIf { it.isNotBlank() }
