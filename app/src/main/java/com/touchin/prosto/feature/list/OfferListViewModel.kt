package com.touchin.prosto.feature.list

import android.content.Context
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.anadolstudio.core.viewmodel.lce.lceFlow
import com.anadolstudio.core.viewmodel.lce.mapLceContent
import com.anadolstudio.core.viewmodel.lce.onEachContent
import com.anadolstudio.core.viewmodel.lce.onEachError
import com.touchin.data.repository.common.PreferencesStorage
import com.touchin.domain.repository.offers.OffersRepository
import com.touchin.prosto.R
import com.touchin.prosto.base.viewmodel.BaseContentViewModel
import com.touchin.prosto.base.viewmodel.navigateTo
import com.touchin.prosto.base.viewmodel.navigateUp
import com.touchin.prosto.feature.model.OfferUi
import com.touchin.prosto.feature.model.toUi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class OfferListViewModel @Inject constructor(
    private val context: Context,
    private val offersRepository: OffersRepository,
    private val preferenceStorage: PreferencesStorage
) : BaseContentViewModel<OfferListState>(
    OfferListState(
        hastFavoriteOffers = preferenceStorage.favorySet.isNotEmpty()
    )
), OfferListController {

    init {
        loadOffers()
    }

    private fun loadOffers() {
        lceFlow { emit(offersRepository.getOfferList()) }
            .mapLceContent { offers -> offers.map { it.toUi(isFavorite = preferenceStorage.favorySet.contains(it.id)) } }
            .onEach { updateState { copy(loadingState = it) } }
            .onEachContent { offers -> updateState { copy(offersList = offers) } }
            .onEachError { showError(it) }
            .launchIn(viewModelScope)
    }

    override fun onBackClicked() = _navigationEvent.navigateUp()

    override fun onOfferClicked(offerUi: OfferUi) = _navigationEvent.navigateTo(
        id = R.id.action_offerListFragment_to_offerBottom,
        args = bundleOf(context.getString(R.string.navigation_offer) to offerUi)
    )

    override fun onFavoriteChecked(offerUi: OfferUi) {
        val favorySet = preferenceStorage.favorySet.toMutableSet()
        val newOffer = offerUi.copy(isFavorite = !offerUi.isFavorite)
        if (offerUi.isFavorite) {
            favorySet.remove(offerUi.id)
        } else {
            favorySet.add(offerUi.id)
        }
        preferenceStorage.favorySet = favorySet
        val newList = state.offersList.map {
            if (it.id == newOffer.id) {
                newOffer
            } else {
                it
            }
        }
        updateState { copy(offersList = newList, hastFavoriteOffers = favorySet.isNotEmpty()) }
    }

    override fun onFavoriteFilterClicked() = showTodo()
}
