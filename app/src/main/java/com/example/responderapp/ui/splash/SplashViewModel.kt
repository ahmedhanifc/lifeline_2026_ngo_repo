package com.example.responderapp.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.responderapp.data.repository.UserRepository
import com.example.responderapp.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    fun checkStartDestination(onResult: (String) -> Unit) {
        if (auth.currentUser == null) {
            onResult(Screen.Login.route)
        } else {
            viewModelScope.launch {
                try {
                    val isFirst = userRepository.isFirstTimeLogin()
                    if (isFirst) {
                        onResult(Screen.Onboarding.route)
                    } else {
                        onResult(Screen.Dashboard.route)
                    }
                } catch (e: Exception) {
                    // Fallback to login or dashboard?
                    // If error syncing, maybe Dashboard and retry later? 
                    // Or Login.
                    onResult(Screen.Login.route)
                }
            }
        }
    }
}
