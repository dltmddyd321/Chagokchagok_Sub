package com.windrr.hyundaesub.data.local.entity

/** 짐 치수를 어떻게 얻었는지 (서버 dimension_source와 같은 값) */
enum class DimensionSource {
    /** AR 측정 */
    AR,

    /** 직접 입력 */
    MANUAL,
}
