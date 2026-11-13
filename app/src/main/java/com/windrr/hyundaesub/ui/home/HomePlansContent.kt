package com.windrr.hyundaesub.ui.home

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.data.local.entity.PlanCategory
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/** 플로팅 탭바(62dp + 상하 12dp 여백)에 가리지 않도록 두는 하단 여백 */
private val BottomBarClearance = 100.dp

/** 계획이 있을 때 홈: 최근 등록된 계획(가로) + 새 계획 만들기 + 자주 찾는 내 계획(즐겨찾기, 가로) */
@Composable
fun HomePlansContent(
    state: HomeUiState,
    onNewPlan: () -> Unit,
    onHomeIntent: (HomeIntent) -> Unit,
) {
    LightStatusBarIcons()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .verticalScroll(rememberScrollState()),
    ) {
        HeroSection(recentPlans = state.recentPlans, onNewPlan = onNewPlan)

        Spacer(Modifier.height(36.dp))
        SectionTitle(text = "자주 찾는 내 계획", color = ChagokTextPrimary)
        Spacer(Modifier.height(20.dp))
        if (state.favoritePlans.isEmpty()) {
            Text(
                text = "계획에 별을 눌러 즐겨찾기하면 여기에 모여요.",
                color = ChagokTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.favoritePlans, key = { it.planId }) { plan ->
                    FavoritePlanCard(
                        plan = plan,
                        onStarClick = { onHomeIntent(HomeIntent.ToggleFavorite(plan.planId)) },
                    )
                }
            }
        }
        Spacer(Modifier.height(BottomBarClearance))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** 어두운 히어로 위라 상태바 아이콘을 흰색으로, 화면을 떠나면 원래대로 */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

@Composable
private fun HeroSection(recentPlans: List<PlanCardUi>, onNewPlan: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(HeroTop, HeroMiddle, HeroBottom))),
    ) {
        // 임시 배경 이미지 자리 (트렁크 열린 차 사진 받으면 Image로 교체)
        Icon(
            painter = painterResource(R.drawable.ic_car),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.07f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 60.dp)
                .size(320.dp),
        )
        Column(modifier = Modifier.statusBarsPadding()) {
            Spacer(Modifier.height(24.dp))
            LogoRow(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(28.dp))
            Text(
                text = "짐 싣기 준비완료!",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "이제 차에 실어볼까요?",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(44.dp))
            SectionTitle(text = "최근 등록된 계획", color = Color.White)
            Spacer(Modifier.height(20.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(recentPlans, key = { it.planId }) { plan -> RecentPlanCard(plan) }
            }
            Spacer(Modifier.height(24.dp))
            NewPlanButton(onClick = onNewPlan, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun LogoRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            // 임시 로고: 로고 에셋 받으면 교체
            Icon(
                painter = painterResource(R.drawable.ic_car),
                contentDescription = null,
                tint = ChagokNavy,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text = "차곡차곡", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

/** 반투명 유리 느낌 카드: 이름·D-day·짐 요약·인원 */
@Composable
private fun RecentPlanCard(plan: PlanCardUi) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .width(256.dp)
            .height(160.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = plan.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            plan.dDay?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = it,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(DDayBg)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = plan.itemsInline,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = plan.passengerLine,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NewPlanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "새 계획 만들기",
            color = NewPlanText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = ChagokTextSecondary,
        )
    }
}

/** 즐겨찾기 계획 카드: 분류·이미지·이름·짐 요약. 별을 누르면 즐겨찾기 해제 */
@Composable
private fun FavoritePlanCard(plan: PlanCardUi, onStarClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(172.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(StarBg)
                    .clickable(onClick = onStarClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = if (plan.isFavorite) "즐겨찾기 해제" else "즐겨찾기",
                    tint = StarColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = plan.category.label,
                color = ChagokTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color.White, CategoryImageTint))),
            contentAlignment = Alignment.Center,
        ) {
            // 임시 분류 이미지 (에셋 받으면 분류별 Image로 교체)
            Icon(
                painter = painterResource(plan.category.placeholderIcon()),
                contentDescription = null,
                tint = CategoryIcon,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = plan.name,
            color = ChagokTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = plan.itemsShort,
            color = ChagokTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
    }
}

private fun PlanCategory.placeholderIcon(): Int = when (this) {
    PlanCategory.TRAVEL -> R.drawable.ic_car
    PlanCategory.LEISURE -> R.drawable.ic_tab_luggage
    PlanCategory.MOVING -> R.drawable.ic_tab_plan
}

private val HeroTop = Color(0xFF1A3263)
private val HeroMiddle = Color(0xFF26467C)
private val HeroBottom = Color(0xFF2E4F86)
private val DDayBg = Color(0xFF05082B)
private val NewPlanText = Color(0xFF1F5FD1)
private val StarBg = Color(0xFFE3ECFC)
private val StarColor = Color(0xFF1F6FEB)
private val CategoryImageTint = Color(0xFFDCE6FB)
private val CategoryIcon = Color(0xFF5B7FBF)

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun HomePlansContentPreview() {
    val sample = { id: String, name: String, category: PlanCategory, dDay: String?, fav: Boolean ->
        PlanCardUi(
            planId = id,
            name = name,
            category = category,
            dDay = dDay,
            itemsInline = "유모차 1 · 캐리어 1 · 캠핑의자 2",
            itemsShort = "캐리어 1, 캠핑의자 2, 배낭 외 2",
            passengerLine = "운전자 포함 3명 · 더 뉴 아반떼 하이브리드",
            isFavorite = fav,
        )
    }
    HyundaeSubTheme {
        HomePlansContent(
            state = HomeUiState(
                isLoading = false,
                recentPlans = listOf(
                    sample("1", "주말 나들이", PlanCategory.TRAVEL, "D-3", false),
                    sample("2", "이케아", PlanCategory.MOVING, "D-6", false),
                ),
                favoritePlans = listOf(
                    sample("3", "가족 캠핑", PlanCategory.TRAVEL, null, true),
                    sample("4", "보드여행", PlanCategory.LEISURE, null, true),
                ),
            ),
            onNewPlan = {},
            onHomeIntent = {},
        )
    }
}
