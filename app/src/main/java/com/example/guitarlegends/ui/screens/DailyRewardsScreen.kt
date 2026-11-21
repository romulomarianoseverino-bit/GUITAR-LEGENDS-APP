package com.example.guitarlegends.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitarlegends.model.DailyRewardCatalog
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun DailyRewardsScreen(viewModel: GameViewModel) {
    BackHandler { viewModel.navigateBack() }

    val stats by viewModel.playerStats.collectAsState()
    val rewardHistory by viewModel.rewardHistory.collectAsState()
    val actionMessage by viewModel.rewardActionMessage.collectAsState()
    val isBusy by viewModel.isAuthBusy.collectAsState()

    val todayUtc = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }
    val alreadyClaimedToday = stats.lastRewardDayKey == todayUtc ||
        rewardHistory.any { it.dayKey == todayUtc }

    val nextDayNumber = if (alreadyClaimedToday) {
        ((stats.rewardStreak - 1).coerceAtLeast(0) % 30) + 1
    } else {
        (stats.rewardStreak % 30) + 1
    }
    val featuredReward = DailyRewardCatalog.rewardForDay(nextDayNumber)
    val currentWeekKey = DailyRewardWeeklyLimit.weekKey(todayUtc)
    val claimedDailyCoinsThisWeek = rewardHistory
        .filter { DailyRewardWeeklyLimit.weekKey(it.dayKey) == currentWeekKey }
        .sumOf { it.coinsGranted }
    val remainingDailyCoinsThisWeek = (DailyRewardWeeklyLimit.MAX_COINS_PER_WEEK - claimedDailyCoinsThisWeek)
        .coerceAtLeast(0)
    val previewDailyCoins = DailyRewardWeeklyLimit.grant(
        featuredReward.coinsGranted,
        claimedDailyCoinsThisWeek
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            onBackClick = { viewModel.navigateBack() },
            title = "Recompensas Diárias & Turnê Fiel"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Painel Esquerdo: Resgate de Hoje & Sequência
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = RockAccentGold.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "SEQUÊNCIA ATUAL: ${stats.rewardStreak} DIA(S)",
                                color = RockAccentGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = if (alreadyClaimedToday) "RECOMPENSA DE HOJE RESGATADA" else "PRÊMIO DO DIA $nextDayNumber",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )

                        Surface(
                            color = Color(0x2200E5FF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (alreadyClaimedToday) Icons.Default.CheckCircle else Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = if (alreadyClaimedToday) RockAccentGreen else RockAccentGold,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (previewDailyCoins < featuredReward.coinsGranted) {
                                            "${featuredReward.title} (limite semanal aplicado)"
                                        } else featuredReward.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Text(featuredReward.description, color = Color.White.copy(alpha = 0.78f), fontSize = 10.sp)
                                    Text(
                                        text = "+${previewDailyCoins} Moedas" +
                                            if (featuredReward.cosmeticId.isNotBlank()) " + Item Desbloqueado" else "",
                                        color = RockAccentCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        if (!actionMessage.isNullOrBlank()) {
                            Surface(color = Color(0x2200E676), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = actionMessage.orEmpty(),
                                    color = RockAccentGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Text(
                            text = "Limite de recompensas diárias: ${DailyRewardWeeklyLimit.MAX_COINS_PER_WEEK} moedas por semana. Usadas: ${claimedDailyCoinsThisWeek.coerceAtMost(DailyRewardWeeklyLimit.MAX_COINS_PER_WEEK)}/${DailyRewardWeeklyLimit.MAX_COINS_PER_WEEK}; restam ${remainingDailyCoinsThisWeek}. Reinicia segunda-feira às 00:00 UTC. O limite não afeta moedas conquistadas nas músicas.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 9.sp,
                            lineHeight = 13.sp
                        )
                    }

                    RockButton(
                        text = if (alreadyClaimedToday) "VOLTE AMANHÃ ✔" else if (isBusy) "RESGATANDO..." else "RESGATAR RECOMPENSA",
                        onClick = { viewModel.claimDailyReward() },
                        enabled = !alreadyClaimedToday && !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "claim_daily_reward_button",
                        containerColor = if (alreadyClaimedToday) Color.DarkGray else RockAccentGold,
                        contentColor = Color.Black
                    )
                }
            }

            // Painel Direito: Calendário Completo do Ciclo
            Card(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentCyan, RockCardBorder))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "CALENDÁRIO DE PRÊMIOS (CICLO DE 15 DIAS)",
                        color = RockAccentCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(DailyRewardCatalog.cycleRewards) { item ->
                            val isClaimed = item.dayNumber <= stats.rewardStreak &&
                                (alreadyClaimedToday || item.dayNumber < nextDayNumber)
                            val isCurrent = item.dayNumber == nextDayNumber && !alreadyClaimedToday

                            Surface(
                                color = when {
                                    isCurrent -> Color(0x33FFD700)
                                    isClaimed -> Color(0x2200E676)
                                    item.isMilestone -> Color(0x28FF2A55)
                                    else -> Color(0x18FFFFFF)
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = if (isCurrent) ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(RockAccentGold, RockAccentCyan))
                                ) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "DIA ${item.dayNumber}",
                                        color = if (isCurrent) RockAccentGold else RockAccentCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        lineHeight = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = when {
                                            isClaimed -> "✔ RESGATADO"
                                            isCurrent -> "DISPONÍVEL!"
                                            else -> "+${item.coinsGranted} LC"
                                        },
                                        color = when {
                                            isClaimed -> RockAccentGreen
                                            isCurrent -> RockAccentGold
                                            else -> Color.White.copy(alpha = 0.65f)
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
