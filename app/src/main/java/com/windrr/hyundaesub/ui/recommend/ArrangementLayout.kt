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
    val markRects: List<PxRect> get() = seats.map(VehicleTopView::seatMarkRect)
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
 * 서버 배치안 → 투싼 탑뷰 이미지 위 사각형.
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
                seat in passengers -> SeatBox(seat, SeatMark.BOARDING, VehicleTopView.seatMarkRect(seat))
                // 접는 좌석은 "좌석" 칸을 지우고 그 자리에 짐만 그린다
                seat in foldedSeats -> null
                else -> SeatBox(seat, SeatMark.EMPTY_SEAT, VehicleTopView.seatMarkRect(seat))
            }
        }

        val items = arrangement.placements.orEmpty()
            .sortedWith(compareBy({ it.loadOrder }, { it.placementId }))
            .mapNotNull { placement ->
                val space = spaces[placement.spaceId] ?: return@mapNotNull null
                val item = itemsById[placement.itemId] ?: return@mapNotNull null
                val (extentX, extentY) = footprint(item, placement.orientation)
                val rect = cargoRect(space, placement.xCm, placement.yCm, extentX, extentY)
                    ?: toImageRect(spaceRect(space) ?: return@mapNotNull null, space, placement.xCm, placement.yCm, extentX, extentY)
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
            KIND_TRUNK -> VehicleTopView.TRUNK
            KIND_SEAT -> seatCodes.map(VehicleTopView::seatCushion).reduceOrNull(PxRect::union)
            KIND_FOLDED_SEAT -> seatCodes.map(VehicleTopView::foldedSeat).reduceOrNull(PxRect::union)
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

    /**
     * 트렁크·접은 2열에 놓인 짐 → 이미지 px. 적재 탑뷰(img_vehicle_tucson_cargo_view)는 트렁크·2열을 실제 비율로 늘린 그림이라
     * 공간의 차량 좌표(origin_cm)에 짐 위치를 더해 **한 축척**([VehicleTopView.PX_PER_CM])으로 그대로 옮긴다.
     * 짐이 트렁크에서 접은 2열까지 걸쳐 있으면 그림에서도 그만큼 걸쳐 보인다.
     * 차량 좌표가 없거나 이 그림 밖 공간(펴진 좌석, 1열 등)이면 null → [toImageRect]
     */
    fun cargoRect(space: PackingSpaceResponse, xCm: Double, yCm: Double, extentX: Double, extentY: Double): PxRect? {
        val origin = space.originCm ?: return null
        val inCargo = when (space.kind) {
            KIND_TRUNK -> true
            KIND_FOLDED_SEAT -> space.seatCodes.orEmpty().isNotEmpty() && space.seatCodes.orEmpty().all { it.startsWith("ROW_2_") }
            else -> false
        }
        if (!inCargo) return null
        val s = VehicleTopView.PX_PER_CM
        val x0 = (origin.xCm + xCm).coerceIn(0.0, VehicleTopView.CARGO_WIDTH_CM)
        val x1 = (origin.xCm + xCm + extentX).coerceIn(0.0, VehicleTopView.CARGO_WIDTH_CM)
        val y0 = (origin.yCm + yCm).coerceIn(0.0, VehicleTopView.CARGO_DEPTH_CM)
        val y1 = (origin.yCm + yCm + extentY).coerceIn(0.0, VehicleTopView.CARGO_DEPTH_CM)
        val left = VehicleTopView.TRUNK.left
        val bottom = VehicleTopView.TRUNK.bottom
        return PxRect(
            left = (left + x0 * s).toFloat(),
            right = (left + x1 * s).toFloat(),
            top = (bottom - y1 * s).toFloat(),
            bottom = (bottom - y0 * s).toFloat(),
        )
    }

    /**
     * 공간 안 위치(cm) → 이미지 px (차량 좌표로 못 옮기는 공간). 가로·세로 같은 축척으로 칸 가운데에 그린다.
     * 공간 밖으로 나가는 값은 공간 경계로 자른다. y=0(뒤)이 칸 아래.
     */
    fun toImageRect(area: PxRect, space: PackingSpaceResponse, xCm: Double, yCm: Double, extentX: Double, extentY: Double): PxRect {
        val w = space.widthCm.takeIf { it > 0 } ?: 1.0
        val d = space.depthCm.takeIf { it > 0 } ?: 1.0
        val scale = minOf(area.width / w, area.height / d)
        val drawLeft = area.left + (area.width - w * scale) / 2
        val x0 = xCm.coerceIn(0.0, w)
        val x1 = (xCm + extentX).coerceIn(0.0, w)
        val y0 = yCm.coerceIn(0.0, d)
        val y1 = (yCm + extentY).coerceIn(0.0, d)
        return PxRect(
            left = (drawLeft + x0 * scale).toFloat(),
            right = (drawLeft + x1 * scale).toFloat(),
            top = (area.bottom - y1 * scale).toFloat(),
            bottom = (area.bottom - y0 * scale).toFloat(),
        )
    }

    private fun seatOf(code: String): SeatCode? = SeatCode.entries.firstOrNull { it.name == code }

    const val KIND_TRUNK = "TRUNK"
    const val KIND_SEAT = "SEAT"
    const val KIND_FOLDED_SEAT = "FOLDED_SEAT"
}

/**
 * img_vehicle_tucson_cargo_view (976×1957px, drawable-nodpi, 위가 차 앞) 위 영역. 단위 원본 px.
 * 원본 탑뷰(img_vehicle_tucson_top_view, 976×1612)에서 2열(905~1162px)과 트렁크(1162~1405px)만 세로로 늘려
 * 트렁크 123×100cm, 접은 2열 60cm를 가로와 같은 축척(650px/123cm ≈ 5.28px/cm)으로 맞춘 그림이다.
 */
internal object VehicleTopView {
    const val IMAGE_W = 976
    const val IMAGE_H = 1957

    /** 추천 화면에 보여줄 영역: 앞좌석 위 ~ 뒤 범퍼(TUCSON 글자) */
    const val CROP_LEFT = 0
    const val CROP_TOP = 520
    const val CROP_W = 976
    const val CROP_H = 1343

    /** 트렁크·2열 적재 공간 축척 (가로·세로 같음) */
    const val PX_PER_CM = 650.0 / 123.0

    /** 차량 좌표로 그리는 범위: 트렁크 폭 123cm, 깊이 트렁크 100 + 접은 2열 60cm */
    const val CARGO_WIDTH_CM = 123.0
    const val CARGO_DEPTH_CM = 160.0

    /** "탑승"/"좌석" 표시칸 크기 */
    private const val MARK_PX = 170f

    private val SEAT_CENTERS = mapOf(
        SeatCode.ROW_1_LEFT to (322f to 625f),
        SeatCode.ROW_1_RIGHT to (668f to 625f),
        SeatCode.ROW_2_LEFT to (305f to 1010f),
        SeatCode.ROW_2_CENTER to (492f to 1010f),
        SeatCode.ROW_2_RIGHT to (680f to 1010f),
    )

    /** 좌석 방석 (펴진 좌석 위에 짐을 놓을 때) */
    private val CUSHIONS = mapOf(
        SeatCode.ROW_1_LEFT to PxRect(210f, 550f, 440f, 700f),
        SeatCode.ROW_1_RIGHT to PxRect(560f, 550f, 790f, 700f),
        SeatCode.ROW_2_LEFT to PxRect(185f, 905f, 425f, 1115f),
        SeatCode.ROW_2_CENTER to PxRect(432f, 905f, 552f, 1115f),
        SeatCode.ROW_2_RIGHT to PxRect(558f, 905f, 805f, 1115f),
    )

    /** 접은 좌석: 방석 + 접혀 눕는 등받이까지 (2열은 트렁크 바닥 바로 위까지) */
    private val FOLDED = mapOf(
        SeatCode.ROW_1_LEFT to PxRect(210f, 550f, 440f, 830f),
        SeatCode.ROW_1_RIGHT to PxRect(560f, 550f, 790f, 830f),
        SeatCode.ROW_2_LEFT to PxRect(185f, 905f, 428f, 1222f),
        SeatCode.ROW_2_CENTER to PxRect(428f, 905f, 555f, 1222f),
        SeatCode.ROW_2_RIGHT to PxRect(555f, 905f, 805f, 1222f),
    )

    /** 트렁크 바닥 123×100cm (아래가 트렁크 입구 = 차량 좌표 y 0) */
    val TRUNK = PxRect(175f, 1222f, 825f, 1750f)

    fun seatMarkRect(seat: SeatCode): PxRect {
        val (cx, cy) = SEAT_CENTERS.getValue(seat)
        val half = MARK_PX / 2
        return PxRect(cx - half, cy - half, cx + half, cy + half)
    }

    fun seatCushion(seat: SeatCode): PxRect = CUSHIONS.getValue(seat)

    fun foldedSeat(seat: SeatCode): PxRect = FOLDED.getValue(seat)
}
