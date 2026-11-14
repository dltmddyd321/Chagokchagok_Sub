package com.windrr.hyundaesub.ui.recommend

import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.data.remote.model.ArrangementResponse
import com.windrr.hyundaesub.data.remote.model.PackingSpaceResponse
import com.windrr.hyundaesub.data.remote.model.SnapshotItem
import com.windrr.hyundaesub.ui.seat.AvanteSeats

/** 탑뷰 이미지 위 사각형. 좌표는 원본 이미지 px */
data class PxRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun union(other: PxRect) = PxRect(
        minOf(left, other.left),
        minOf(top, other.top),
        maxOf(right, other.right),
        maxOf(bottom, other.bottom),
    )
}

enum class SeatMark { BOARDING, EMPTY_SEAT }

/** 좌석 표시칸 ("탑승" / "좌석") */
data class SeatBox(val seat: SeatCode, val mark: SeatMark, val rect: PxRect)

/** 짐 한 개 사각형. [order]는 0부터, 싣는 순서대로 */
data class ItemBox(
    val placementId: String,
    val name: String,
    val order: Int,
    val rect: PxRect,
    /** 놓인 공간 종류 (TRUNK / SEAT / FOLDED_SEAT) — 기본 안내 문구용 */
    val spaceKind: String = "",
)

/** 이 배치에서 함께 접는 좌석 묶음 (예: 뒷좌석 왼쪽+가운데) */
data class FoldedSeatGroup(val seats: List<SeatCode>) {
    val markRects: List<PxRect> get() = seats.map(AvanteTopView::seatMarkRect)
}

data class ArrangementLayoutUi(
    val arrangementId: String,
    val seats: List<SeatBox>,
    /** 싣는 순서(load_order)대로 */
    val items: List<ItemBox>,
    /** 접어야 하는 좌석 묶음 (가이드 화면에서 접을 자리 표시) */
    val foldedGroups: List<FoldedSeatGroup> = emptyList(),
)

/**
 * 서버 배치안 → 아반떼 탑뷰 이미지 위 사각형.
 *
 * 서버 공간 치수(데모 차량)는 이미지 비율과 맞지 않아 차량 전체를 한 배율로 옮기면 짐이 좌석·트렁크 그림을 벗어난다.
 * 그래서 공간(트렁크·좌석·접은 좌석)마다 이미지 위 영역을 정해 두고, 짐 위치를 그 공간 안 비율로 옮긴다.
 * - 공간 안 좌표: x = 왼쪽(운전석 쪽)→오른쪽, y = 뒤(트렁크 쪽)→앞. 이미지는 위가 차 앞이라 y는 아래→위로 옮긴다.
 * - 짐 바닥 크기: orientation 앞 두 글자가 x·y 축 치수.
 */
internal object ArrangementLayoutMapper {

    fun map(arrangement: ArrangementResponse, snapshotItems: List<SnapshotItem>): ArrangementLayoutUi {
        val spaces = arrangement.packingSpaces.orEmpty().associateBy { it.spaceId }
        val itemsById = snapshotItems.associateBy { it.itemId }

        val passengers = arrangement.passengerSeats.orEmpty().mapNotNull(::seatOf).toSet()
        val foldedSeats = arrangement.packingSpaces.orEmpty()
            .filter { it.kind == KIND_FOLDED_SEAT }
            .flatMap { it.seatCodes.orEmpty() }
            .mapNotNull(::seatOf)
            .toSet()
        val seats = AvanteSeats.SELECTABLE.mapNotNull { seat ->
            when {
                seat in passengers -> SeatBox(seat, SeatMark.BOARDING, AvanteTopView.seatMarkRect(seat))
                // 접는 좌석은 "좌석" 칸을 지우고 그 자리에 짐만 그린다
                seat in foldedSeats -> null
                else -> SeatBox(seat, SeatMark.EMPTY_SEAT, AvanteTopView.seatMarkRect(seat))
            }
        }

        val items = arrangement.placements.orEmpty()
            .sortedWith(compareBy({ it.loadOrder }, { it.placementId }))
            .mapNotNull { placement ->
                val space = spaces[placement.spaceId] ?: return@mapNotNull null
                val item = itemsById[placement.itemId] ?: return@mapNotNull null
                val area = spaceRect(space) ?: return@mapNotNull null
                val (extentX, extentY) = footprint(item, placement.orientation)
                val rect = toImageRect(area, space, placement.xCm, placement.yCm, extentX, extentY)
                ItemBox(placement.placementId, item.name?.takeIf { it.isNotBlank() } ?: "짐", 0, rect, space.kind)
            }
            .mapIndexed { index, box -> box.copy(order = index) }

        val foldedGroups = arrangement.packingSpaces.orEmpty()
            .filter { it.kind == KIND_FOLDED_SEAT }
            .map { space -> FoldedSeatGroup(space.seatCodes.orEmpty().mapNotNull(::seatOf).filter { it in AvanteSeats.SELECTABLE }) }
            .filter { it.seats.isNotEmpty() }

        return ArrangementLayoutUi(arrangement.arrangementId, seats, items, foldedGroups)
    }

    /** 공간이 이미지에서 차지하는 영역. 모르는 공간이면 null (그리지 않음) */
    fun spaceRect(space: PackingSpaceResponse): PxRect? {
        val seatCodes = space.seatCodes.orEmpty().mapNotNull(::seatOf)
        return when (space.kind) {
            KIND_TRUNK -> AvanteTopView.TRUNK
            KIND_SEAT -> seatCodes.map(AvanteTopView::seatCushion).reduceOrNull(PxRect::union)
            KIND_FOLDED_SEAT -> seatCodes.map(AvanteTopView::foldedSeat).reduceOrNull(PxRect::union)
            else -> null
        }
    }

    /** orientation 세 글자 중 앞 두 글자(x·y 축) 치수 = 위에서 본 바닥 크기 */
    fun footprint(item: SnapshotItem, orientation: String): Pair<Double, Double> {
        fun dim(axis: Char) = when (axis) {
            'W' -> item.widthCm
            'D' -> item.depthCm
            'H' -> item.heightCm
            else -> item.widthCm
        }
        val axes = orientation.takeIf { it.length == 3 } ?: "WDH"
        return dim(axes[0]) to dim(axes[1])
    }

    /** 공간 안 위치(cm) → 이미지 px. 공간 밖으로 나가는 값은 공간 경계로 자른다 */
    fun toImageRect(area: PxRect, space: PackingSpaceResponse, xCm: Double, yCm: Double, extentX: Double, extentY: Double): PxRect {
        val w = space.widthCm.takeIf { it > 0 } ?: 1.0
        val d = space.depthCm.takeIf { it > 0 } ?: 1.0
        val x0 = (xCm / w).coerceIn(0.0, 1.0)
        val x1 = ((xCm + extentX) / w).coerceIn(0.0, 1.0)
        val y0 = (yCm / d).coerceIn(0.0, 1.0)
        val y1 = ((yCm + extentY) / d).coerceIn(0.0, 1.0)
        return PxRect(
            left = area.left + (area.width * x0).toFloat(),
            right = area.left + (area.width * x1).toFloat(),
            // y=0(뒤)이 이미지 아래
            top = area.bottom - (area.height * y1).toFloat(),
            bottom = area.bottom - (area.height * y0).toFloat(),
        )
    }

    private fun seatOf(code: String): SeatCode? = SeatCode.entries.firstOrNull { it.name == code }

    const val KIND_TRUNK = "TRUNK"
    const val KIND_SEAT = "SEAT"
    const val KIND_FOLDED_SEAT = "FOLDED_SEAT"
}

/**
 * img_vehicle_avante_top_view_applied (1255×1673px, 위가 차 앞) 위 영역. 단위 원본 px.
 * 좌석 중심은 좌석 선택 화면과 같은 값.
 */
internal object AvanteTopView {
    const val IMAGE_W = 1255
    const val IMAGE_H = 1673

    /** 추천 화면에 보여줄 영역: 앞좌석 위 ~ 뒤 범퍼(AVANTE 글자) */
    const val CROP_LEFT = 60
    const val CROP_TOP = 420
    const val CROP_W = 1135
    const val CROP_H = 1160

    /** "탑승"/"좌석" 표시칸 크기 */
    private const val MARK_PX = 170f

    private val SEAT_CENTERS = mapOf(
        SeatCode.ROW_1_LEFT to (445f to 705f),
        SeatCode.ROW_1_RIGHT to (813f to 705f),
        SeatCode.ROW_2_LEFT to (440f to 1070f),
        SeatCode.ROW_2_CENTER to (630f to 1070f),
        SeatCode.ROW_2_RIGHT to (817f to 1070f),
    )

    /** 좌석 방석 (펴진 좌석 위에 짐을 놓을 때) */
    private val CUSHIONS = mapOf(
        SeatCode.ROW_1_LEFT to PxRect(325f, 615f, 565f, 790f),
        SeatCode.ROW_1_RIGHT to PxRect(695f, 615f, 935f, 790f),
        SeatCode.ROW_2_LEFT to PxRect(320f, 990f, 565f, 1150f),
        SeatCode.ROW_2_CENTER to PxRect(575f, 990f, 685f, 1150f),
        SeatCode.ROW_2_RIGHT to PxRect(695f, 990f, 945f, 1150f),
    )

    /** 접은 좌석: 방석 + 접혀 눕는 등받이까지 */
    private val FOLDED = mapOf(
        SeatCode.ROW_1_LEFT to PxRect(325f, 615f, 565f, 870f),
        SeatCode.ROW_1_RIGHT to PxRect(695f, 615f, 935f, 870f),
        SeatCode.ROW_2_LEFT to PxRect(320f, 990f, 570f, 1250f),
        SeatCode.ROW_2_CENTER to PxRect(570f, 990f, 690f, 1250f),
        SeatCode.ROW_2_RIGHT to PxRect(690f, 990f, 945f, 1250f),
    )

    /** 트렁크 바닥 */
    val TRUNK = PxRect(330f, 1262f, 960f, 1460f)

    fun seatMarkRect(seat: SeatCode): PxRect {
        val (cx, cy) = SEAT_CENTERS.getValue(seat)
        val half = MARK_PX / 2
        return PxRect(cx - half, cy - half, cx + half, cy + half)
    }

    fun seatCushion(seat: SeatCode): PxRect = CUSHIONS.getValue(seat)

    fun foldedSeat(seat: SeatCode): PxRect = FOLDED.getValue(seat)
}
