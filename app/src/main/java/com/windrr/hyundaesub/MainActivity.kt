package com.windrr.hyundaesub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windrr.hyundaesub.measure.ArMeasureScreen
import com.windrr.hyundaesub.ui.main.MainIntent
import com.windrr.hyundaesub.ui.main.MainRoute
import com.windrr.hyundaesub.ui.main.MainScreen
import com.windrr.hyundaesub.ui.main.MainViewModel
import com.windrr.hyundaesub.ui.seat.SeatSelectRoute
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme
import com.windrr.hyundaesub.ui.vehicle.VehicleSelectRoute

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HyundaeSubTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                when (state.route) {
                    MainRoute.TABS -> MainScreen(state = state, onIntent = viewModel::onIntent)
                    MainRoute.VEHICLE_SELECT -> VehicleSelectRoute(
                        onBack = { viewModel.onIntent(MainIntent.BackToTabs) },
                        onNext = { viewModel.onIntent(MainIntent.VehicleSelected(it)) },
                        viewModel = viewModel(key = "vehicle-${state.loadFlowId}"),
                    )
                    MainRoute.SEAT_SELECT -> SeatSelectRoute(
                        onBack = { viewModel.onIntent(MainIntent.BackToVehicleSelect) },
                        onNext = { viewModel.onIntent(MainIntent.SeatsSelected(it)) },
                        viewModel = viewModel(key = "seat-${state.loadFlowId}"),
                    )
                    MainRoute.MEASURE -> ArMeasureScreen(onBack = { viewModel.onIntent(MainIntent.BackToTabs) })
                }
            }
        }
    }
}
