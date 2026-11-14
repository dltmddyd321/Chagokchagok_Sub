package com.windrr.hyundaesub.ui.home

import com.windrr.hyundaesub.data.local.entity.PlanCategory
import com.windrr.hyundaesub.data.local.relation.PlanWithDetails
import com.windrr.hyundaesub.data.remote.model.PlanSummaryResponse
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 홈 계획 카드 한 장 */
data class PlanCardUi(
    val planId: String,
    val name: String,
    val category: PlanCategory,
    /** "D-3", "D-DAY". 날짜가 없거나 지났으면 null */
    val dDay: String?,
    /** 최근 계획 카드용: "유모차 1 · 캐리어 1 · 캠핑의자 2" */
    val itemsInline: String,
    /** 즐겨찾기 카드용: "캐리어 1, 캠핑의자 2, 배낭 외 2" */
    val itemsShort: String,
    /** "운전자 포함 3명 · 더 뉴 아반떼 하이브리드" */
    val passengerLine: String,
    val isFavorite: Boolean,
)

/** 홈 계획 데이터 출처 */
enum class PlanSource { SERVER, LOCAL }

data class HomeUiState(
    val isLoading: Boolean = true,
    val recentPlans: List<PlanCardUi> = emptyList(),
    val favoritePlans: List<PlanCardUi> = emptyList(),
    /** 서버 조회에 성공했으면 SERVER, 실패해서 기기 DB를 보여주는 중이면 LOCAL */
    val source: PlanSource = PlanSource.LOCAL,
) {
    /** 계획이 하나도 없으면 첫 화면(짐 싣기 전 안내)을 보여준다 */
    val hasPlans: Boolean get() = recentPlans.isNotEmpty()
}

sealed interface HomeIntent {
    data class ToggleFavorite(val planId: String) : HomeIntent
}

internal object PlanCardMapper {

    /** 서버 계획 목록 한 줄 → 카드. 서버엔 출발일이 없어 D-day는 표시하지 않는다 */
    fun fromServer(summary: PlanSummaryResponse): PlanCardUi {
        val items = summary.itemSummaries.orEmpty().map { (it.name ?: "짐") to (it.quantity ?: 1) }
        return PlanCardUi(
            planId = summary.planId,
            name = summary.name?.takeIf { it.isNotBlank() } ?: "이름 없는 계획",
            category = PlanCategory.fromCode(summary.category),
            dDay = null,
            itemsInline = items.joinToString(" · ") { (name, qty) -> "$name $qty" },
            itemsShort = itemsShort(items),
            passengerLine = buildString {
                append("운전자 포함 ${summary.passengerCount ?: 1}명")
                summary.vehicleName?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
            },
            isFavorite = summary.isFavorite == true,
        )
    }

    /** 즐겨찾기 카드에서 이름까지 보여줄 짐 종류 수 */
    private const val SHORT_ITEM_COUNT = 3

    fun map(details: PlanWithDetails, today: LocalDate): PlanCardUi {
        val plan = details.plan
        val items = details.items
        return PlanCardUi(
            planId = plan.planId,
            name = plan.name.ifBlank { "이름 없는 계획" },
            category = plan.category,
            dDay = dDay(plan.tripDate, today),
            itemsInline = items.joinToString(" · ") { "${it.name} ${it.quantity}" },
            itemsShort = itemsShort(items.map { it.name to it.quantity }),
            passengerLine = buildString {
                append("운전자 포함 ${plan.passengerCount}명")
                details.vehicle?.name?.let { append(" · ").append(it) }
            },
            isFavorite = plan.isFavorite,
        )
    }

    fun dDay(tripDate: String?, today: LocalDate): String? {
        val date = tripDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days < 0 -> null
            days == 0L -> "D-DAY"
            else -> "D-$days"
        }
    }

    /** 앞의 두 종류는 "이름 개수", 세 번째는 이름만 + 나머지 "외 N" */
    fun itemsShort(items: List<Pair<String, Int>>): String {
        if (items.size <= SHORT_ITEM_COUNT) return items.joinToString(", ") { (name, qty) -> "$name $qty" }
        val head = items.take(SHORT_ITEM_COUNT - 1).joinToString(", ") { (name, qty) -> "$name $qty" }
        val third = items[SHORT_ITEM_COUNT - 1].first
        return "$head, $third 외 ${items.size - SHORT_ITEM_COUNT}"
    }
}
