package com.tolstoy.zurichat.ui.organizations.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolstoy.zurichat.R
import com.tolstoy.zurichat.data.functional.Result
import com.tolstoy.zurichat.data.remoteSource.TokenInterceptor
import com.tolstoy.zurichat.models.organization_model.UserOrganizationModel
import com.tolstoy.zurichat.ui.organizations.states.UserOrganizationViewState
import com.tolstoy.zurichat.ui.organizations.usecase.GetUserOrganization
import com.tolstoy.zurichat.ui.profile.data.UserOrganizationResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserOrganizationViewModel @Inject constructor(
    private val getUserOrganization: GetUserOrganization,
    private val interceptor: TokenInterceptor,
) : ViewModel() {

    private val _userOrganizationFlow =
        MutableStateFlow<UserOrganizationViewState>(UserOrganizationViewState.Empty)
    val userOrganizationFlow: StateFlow<UserOrganizationViewState> = _userOrganizationFlow

    fun getUserOrganizations(emailAddress: String) {
        viewModelScope.launch {
            _userOrganizationFlow.value = UserOrganizationViewState.Loading(R.string.retrieving_user_org)
            getUserOrganization(emailAddress).collect {
                when (it) {
                    is Result.Success -> {
                        if (it.data is UserOrganizationModel) {
                            _userOrganizationFlow.value =
                                UserOrganizationViewState.Success(R.string.successful_get_user_org,
                                    it.data)
                        }
                    }
                    is Result.Failed -> {
                        if (it.errorMessage is String) {
                            _userOrganizationFlow.value =
                                UserOrganizationViewState.Failure(it.errorMessage)
                        }
                    }
                }
            }
        }
    }

    fun setToken(token: String) {
        interceptor.setToken(token)

    }
}
