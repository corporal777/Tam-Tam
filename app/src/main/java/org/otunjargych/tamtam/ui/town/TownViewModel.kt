package org.otunjargych.tamtam.ui.town

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.otunjargych.tamtam.di.data.AppData
import org.otunjargych.tamtam.di.repo.location.LocationRepository
import org.otunjargych.tamtam.model.request.LocationRequestModel
import org.otunjargych.tamtam.model.request.LocationResponseModel
import org.otunjargych.tamtam.ui.base.BaseViewModel
import org.otunjargych.tamtam.util.extensions.performOnBackgroundOutOnMain
import org.otunjargych.tamtam.util.extensions.withDelay
import org.otunjargych.tamtam.util.extensions.withLoadingDialog
import java.util.concurrent.TimeUnit

class TownViewModel(
    private val appData: AppData,
    private val locationRepository: LocationRepository,
    private val locationProviderClient: FusedLocationProviderClient
) : BaseViewModel() {

    private val _userLocation = MutableLiveData<LocationResponseModel?>()
    val userLocation: LiveData<LocationResponseModel?> get() = _userLocation

    private val _townSavedSuccess = MutableLiveData<Boolean>()
    val townSavedSuccess: LiveData<Boolean> get() = _townSavedSuccess

    var userTown = ""

    fun initLocation(rxPermissions: RxPermissions) {
        compositeDisposable += getLocation(rxPermissions)
            .flatMap {
                locationRepository.checkUserLocation(
                    LocationRequestModel(
                        it.latitude,
                        it.longitude
                    )
                )
            }
            .timeout(10000, TimeUnit.MILLISECONDS)
            .performOnBackgroundOutOnMain()
            .withLoadingDialog(loadingData)
            .subscribeBy(
                onError = {
                    it.printStackTrace()
                    _userLocation.postValue(null)
                },
                onSuccess = {
                    if (!it.city.isNullOrEmpty()) userTown = it.city
                    _userLocation.postValue(it)
                })

    }

    fun saveUserCity() {
        compositeDisposable += Completable.fromAction {
            appData.initUserTown(userTown)
        }
            .withDelay(1000)
            .performOnBackgroundOutOnMain()
            .withLoadingDialog(progressData)
            .subscribeBy {
                _townSavedSuccess.postValue(true)
            }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(rxPermissions: RxPermissions): Maybe<Location> {
        return rxPermissions.request(Manifest.permission.ACCESS_COARSE_LOCATION)
            .firstElement()
            .flatMap { isGranted ->
                if (isGranted) {
                    Maybe.create<Location> { emitter ->
                        val locationRequest = LocationRequest.create()
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setNumUpdates(1)

                        val callback = object : LocationCallback() {
                            override fun onLocationResult(location: LocationResult) {
                                location.lastLocation?.let { emitter.onSuccess(it) }
                            }
                        }
                        locationProviderClient.requestLocationUpdates(
                            locationRequest,
                            callback,
                            Looper.getMainLooper()
                        )
                            .addOnFailureListener {
                                emitter.onError(it)
                                it.printStackTrace()
                            }

                        emitter.setCancellable {
                            locationProviderClient.removeLocationUpdates(callback)
                        }
                    }
                } else Maybe.empty()
            }
    }
}