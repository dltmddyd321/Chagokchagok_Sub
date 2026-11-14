package com.windrr.hyundaesub.ui.recommend

import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.data.remote.model.LoadingGuideResponse

/** 가이드 한 단계 (화면용). [number]는 1부터, 이미지 배지와 같은 번호 */
data class GuideStepUi(
    val number: Int,
    val title: String,
    val body: String?,
    /** 배지를 붙일 짐 (좌석 접기 단계면 null) */
    val placementId: String? = null,
    /** 배지를 붙일 접는 좌석 묶음 (짐 단계면 null) */
    val foldedGroup: FoldedSeatGroup? = null,
)

/**
 * 서버 적재 안내 → 화면 단계.
 * 서버 단계는 짐(placement)마다 하나라 "좌석 접기"가 따로 없다. 시안처럼 접기 단계를 앱이 앞에 붙이고, 이어서 짐 단계.
 * 서버가 단계를 비워 주면(FALLBACK·실패) 배치의 싣는 순서로 기본 문구를 만든다.
 */
internal object LoadingGuideBuilder {

    fun build(layout: ArrangementLayoutUi, guide: LoadingGuideResponse?): List<GuideStepUi> {
        val foldSteps = layout.foldedGroups.map { group ->
            Step(title = "${seatGroupName(group.seats)}을 접어요", body = "접은 자리에 짐을 실을 수 있어요.", foldedGroup = group)
        }
        val itemsById = layout.items.associateBy { it.placementId }
        val serverSteps = guide?.steps.orEmpty()
            .filter { it.text.isNotBlank() && it.placementId in itemsById }
            .sortedBy { it.order }
            .map { step ->
                val (title, body) = splitText(step.text)
                Step(title = title, body = body, placementId = step.placementId)
            }
        val itemSteps = serverSteps.ifEmpty { layout.items.map(::fallbackStep) }
        return (foldSteps + itemSteps).mapIndexed { index, step ->
            GuideStepUi(index + 1, step.title, step.body, step.placementId, step.foldedGroup)
        }
    }

    /** 첫 줄 = 제목, 나머지 = 설명 */
    fun splitText(text: String): Pair<String, String?> {
        val lines = text.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }
        return lines.first() to lines.drop(1).joinToString(" ").takeIf { it.isNotBlank() }
    }

    private fun fallbackStep(item: ItemBox) = Step(
        title = "${item.name} 넣기",
        body = when (item.spaceKind) {
            ArrangementLayoutMapper.KIND_TRUNK -> "트렁크의 표시된 자리에 넣어 주세요."
            ArrangementLayoutMapper.KIND_FOLDED_SEAT -> "접은 좌석 위 표시된 자리에 놓아 주세요."
            ArrangementLayoutMapper.KIND_SEAT -> "좌석 위 표시된 자리에 놓아 주세요."
            else -> null
        },
        placementId = item.placementId,
    )

    fun seatGroupName(seats: List<SeatCode>): String {
        val set = seats.toSet()
        return when {
            set == setOf(SeatCode.ROW_2_LEFT, SeatCode.ROW_2_CENTER) -> "왼쪽 뒷좌석"
            set == setOf(SeatCode.ROW_2_CENTER, SeatCode.ROW_2_RIGHT) -> "오른쪽 뒷좌석"
            set.size == 1 -> seatName(seats.first())
            else -> seats.joinToString("·") { seatName(it) }
        }
    }

    private fun seatName(seat: SeatCode) = when (seat) {
        SeatCode.ROW_1_LEFT -> "운전석"
        SeatCode.ROW_1_RIGHT -> "조수석"
        SeatCode.ROW_2_LEFT -> "왼쪽 뒷좌석"
        SeatCode.ROW_2_CENTER -> "가운데 뒷좌석"
        SeatCode.ROW_2_RIGHT -> "오른쪽 뒷좌석"
        SeatCode.ROW_1_CENTER -> "앞좌석 가운데"
        SeatCode.ROW_3_LEFT, SeatCode.ROW_3_CENTER, SeatCode.ROW_3_RIGHT -> "3열 좌석"
    }

    private data class Step(
        val title: String,
        val body: String?,
        val placementId: String? = null,
        val foldedGroup: FoldedSeatGroup? = null,
    )
}
