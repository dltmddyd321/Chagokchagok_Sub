package com.windrr.hyundaesub.ui.item.confirm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.windrr.hyundaesub.data.local.AppDatabase
import com.windrr.hyundaesub.data.local.entity.DimensionSource
import com.windrr.hyundaesub.data.local.entity.SavedItemEntity
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.measure.BoxSize
import com.windrr.hyundaesub.ui.item.PickedItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** 측정값 확인 / 직접 입력. 저장하면 내 짐(saved_item)에 등록하고 이번 계획 짐으로도 담는다 */
class ItemConfirmViewModel(application: Application) : AndroidViewModel(application) {

    private val savedItemDao = AppDatabase.getInstance(application).savedItemDao()

    private val _state = MutableStateFlow(ItemConfirmUiState())
    val state: StateFlow<ItemConfirmUiState> = _state.asStateFlow()

    private val _effect = Channel<ItemConfirmEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var started = false

    /** 저장하기 연타로 같은 짐이 두 번 등록되지 않게 */
    private var saving = false

    /** 화면 진입 시 한 번만: 측정값(없으면 직접 입력)으로 채운다 */
    fun start(measured: BoxSize?) {
        if (started) return
        started = true
        _state.value = ItemConfirmUiState.fromMeasure(measured)
    }

    fun onIntent(intent: ItemConfirmIntent) {
        when (intent) {
            is ItemConfirmIntent.NameChanged -> _state.update { it.copy(name = intent.name.take(MAX_NAME_LENGTH)) }
            is ItemConfirmIntent.DimensionChanged -> {
                // 숫자만, 최대 3자리
                val digits = intent.value.filter(Char::isDigit).take(3)
                _state.update {
                    when (intent.dimension) {
                        Dimension.WIDTH -> it.copy(width = digits)
                        Dimension.DEPTH -> it.copy(depth = digits)
                        Dimension.HEIGHT -> it.copy(height = digits)
                    }
                }
            }
            ItemConfirmIntent.Increase -> _state.update {
                it.copy(quantity = (it.quantity + 1).coerceAtMost(ItemConfirmUiState.MAX_QUANTITY))
            }
            ItemConfirmIntent.Decrease -> _state.update { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) }
            is ItemConfirmIntent.AddPhotos -> _state.update {
                it.copy(photos = (it.photos + intent.uris).distinct().take(ItemConfirmUiState.MAX_PHOTOS))
            }
            is ItemConfirmIntent.RemovePhoto -> _state.update { it.copy(photos = it.photos - intent.uri) }
            ItemConfirmIntent.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.canSave || saving) return
        saving = true
        val saved = SavedItemEntity(
            savedItemId = UUID.randomUUID().toString(),
            name = s.name.trim(),
            widthCm = s.widthCm!!,
            depthCm = s.depthCm!!,
            heightCm = s.heightCm!!,
            defaultQuantity = s.quantity,
            dimensionSource = if (s.fromMeasure) DimensionSource.AR else DimensionSource.MANUAL,
        )
        val item = PickedItem(
            key = "saved-${saved.savedItemId}",
            name = saved.name,
            widthCm = saved.widthCm,
            depthCm = saved.depthCm,
            heightCm = saved.heightCm,
            quantity = s.quantity,
            savedItemId = saved.savedItemId,
        )
        viewModelScope.launch {
            savedItemDao.upsert(saved)
            _effect.send(ItemConfirmEffect.Saved(item))
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 20
    }
}
