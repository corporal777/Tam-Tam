package org.otunjargych.tamtam.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.otunjargych.tamtam.di.data.AppData
import org.otunjargych.tamtam.di.repo.auth.AuthRepository
import org.otunjargych.tamtam.di.repo.user.UserRepository
import org.otunjargych.tamtam.model.UserNew
import org.otunjargych.tamtam.ui.base.BaseViewModel
import org.otunjargych.tamtam.util.extensions.performOnBackgroundOutOnMain
import org.otunjargych.tamtam.util.extensions.withDelay
import org.otunjargych.tamtam.util.extensions.withLoadingDialog
import java.util.concurrent.TimeUnit

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val appData: AppData
) : BaseViewModel() {

    private var isFirstLaunch = true
    private val _userData = MutableLiveData<UserNew?>()
    val userData: LiveData<UserNew?> get() = _userData

    private val _successLoggedOut = MutableLiveData<Boolean>()
    val successLoggedOut: LiveData<Boolean> get() = _successLoggedOut

    init {
        getCurrentUser()
    }

    private fun getCurrentUser() {
        compositeDisposable += userRepository.getCurrentUser().ignoreElement()
            .timeout(2000, TimeUnit.MILLISECONDS)
            .let {
                if (isFirstLaunch) {
                    isFirstLaunch = false
                    it.withLoadingDialog(loadingData)
                } else it
            }
            .subscribeSimple(
                onError = { it.printStackTrace() },
                onComplete = {}
            )
    }

    fun loadUser() {
        compositeDisposable += appData.userChangeSubject
            .performOnBackgroundOutOnMain()
            .subscribeSimple(
                onError = {
                    it.printStackTrace()
                    _userData.postValue(null)
                },
                onNext = {
                    _userData.postValue(it.value)
                }
            )
    }

    fun logout() {
        compositeDisposable += authRepository.logout(appData.getUserId())
            .withDelay(1000)
            .performOnBackgroundOutOnMain()
            .withLoadingDialog(progressData)
            .subscribeBy(
                onError = { it.printStackTrace() },
                onComplete = {
                    appData.logOut()
                    _successLoggedOut.postValue(true)
                }
            )
    }


    fun clearSuccessLoggedOut(logged: Boolean) {
        _successLoggedOut.value = logged
    }

    fun getCurrentTown() = appData.getUserTown()
    fun isUserLoggedOut() = appData.isLoggedOut()
}