package org.otunjargych.tamtam.ui.auth.register

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.otunjargych.tamtam.R
import org.otunjargych.tamtam.di.data.AppData
import org.otunjargych.tamtam.di.repo.auth.AuthRepository
import org.otunjargych.tamtam.di.repo.user.UserRepository
import org.otunjargych.tamtam.model.ContactInformation
import org.otunjargych.tamtam.model.UserNew
import org.otunjargych.tamtam.model.request.ErrorResponse
import org.otunjargych.tamtam.model.request.RegisterLoginModel
import org.otunjargych.tamtam.model.request.RegisterRequestBody
import org.otunjargych.tamtam.ui.base.BaseViewModel
import org.otunjargych.tamtam.ui.views.PasswordData
import org.otunjargych.tamtam.util.*
import org.otunjargych.tamtam.util.extensions.performOnBackgroundOutOnMain
import org.otunjargych.tamtam.util.extensions.withLoadingDialog
import retrofit2.HttpException
import kotlin.math.log

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val appData: AppData
) : BaseViewModel() {

    private val _enableBtnRegister = MutableLiveData(false)
    val enableBtnRegister: LiveData<Boolean> get() = _enableBtnRegister

    private val _firstNameError = MutableLiveData(false)
    val firstNameError: LiveData<Boolean> get() = _firstNameError

    private val _lastNameError = MutableLiveData(false)
    val lastNameError: LiveData<Boolean> get() = _lastNameError

    private val _emailError = MutableLiveData<Int>()
    val emailError: LiveData<Int> get() = _emailError

    private val _passwordError = MutableLiveData(false)
    val passwordError: LiveData<Boolean> get() = _passwordError

    private val _loginIsUnique = MutableLiveData<Boolean>()
    val loginIsUnique: LiveData<Boolean> get() = _loginIsUnique

    private val _successResponse = MutableLiveData<UserNew?>()
    val successResponse: LiveData<UserNew?> get() = _successResponse

    private var firstName = ""
    private var lastName = ""
    private var login = ""
    private var passwordData = PasswordData(false, "")
    var loginType = ""

    fun checkLoginIsUnique() {
        if (isDataValid()) {
            loginType = if (isPhone(login) && !isContainLetters(login)) "phone"
            else "email"

            compositeDisposable += authRepository.checkIfLoginExists(getValidatedLogin())
                .andThen(authRepository.sendVerifyCode(RegisterLoginModel(getValidatedLogin(), loginType)))
                .performOnBackgroundOutOnMain()
                .withLoadingDialog(progressData)
                .subscribeSimple(
                    onError = {
                        it.printStackTrace()
                        if (it is HttpException) {
                            try {
                                val error = it.getErrorResponse()
                                if (error?.message == "Login is not free!") _emailError.postValue(1)
                            } catch (e: Exception) {

                            }
                        }
                        _loginIsUnique.postValue(false)
                    },
                    onComplete = {
                        _loginIsUnique.postValue(true)
                    })

        } else showErrors()

    }

    fun register() {
        compositeDisposable += authRepository.register(getDataToSave())
            .performOnBackgroundOutOnMain()
            .withLoadingDialog(progressData)
            .subscribeBy(
                onError = {
                    it.printStackTrace()
                    _successResponse.postValue(null)
                },
                onSuccess = { _successResponse.postValue(it) }
            )
    }

    fun performNameChanged(value: String) {
        this.firstName = value
        _firstNameError.postValue(false)
        _enableBtnRegister.postValue(isDataValid())
    }

    fun performLastNameChanged(value: String) {
        this.lastName = value
        _lastNameError.postValue(false)
        _enableBtnRegister.postValue(isDataValid())
    }

    fun performEmailChanged(value: String) {
        this.login = value
        _emailError.postValue(0)
        _enableBtnRegister.postValue(isDataValid())
        loginType = if (isPhone(login) && !isContainLetters(login)) "phone" else "email"
    }

    fun performPasswordChanged(value: PasswordData) {
        this.passwordData = value
        _passwordError.postValue(false)
        _enableBtnRegister.postValue(isDataValid())
    }

    private fun isDataValid(): Boolean {
        val firstNameValid = !firstName.isNullOrBlank()
        val lastNameValid = !lastName.isNullOrBlank()
        val emailValid = if (isPhone(login)) isPhoneIsValid(login)
        else AuthValidateUtil.isValidEmail(login)

        val passwordValid =
            if (passwordData.isValid) {
                AuthValidateUtil.isValidPassword(passwordData.password)
            } else false
        return firstNameValid && lastNameValid && passwordValid && emailValid
    }

    private fun showErrors() {
        _firstNameError.postValue(firstName.isNullOrBlank())
        _lastNameError.postValue(lastName.isNullOrBlank())

        if (isPhone(login)) {
            if (!isPhoneIsValid(login)) _emailError.postValue(2)
        } else {
            if (!AuthValidateUtil.isValidEmail(login)) _emailError.postValue(2)
        }

        _passwordError.postValue(passwordData.isValid)
    }


    private fun getDataToSave(): MutableMap<String, String> {
        return mutableMapOf<String, String>().apply {
            put("firstName", firstName)
            put("lastName", lastName)
            put("login", getValidatedLogin())
            put("password", passwordData.password)
        }
    }


    fun getValidatedLogin(): String {
        return if (isPhone(login) && !isContainLetters(login)) validatePhoneBeforeSend(login)
        else login
    }

    companion object {
        const val ERROR_LOGIN_EXISTS = 1
        const val ERROR_LOGIN_INCORRECT = 2
        const val ERROR_LOGIN_CORRECT = 0
    }
}