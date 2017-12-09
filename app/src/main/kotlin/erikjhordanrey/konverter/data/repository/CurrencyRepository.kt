/**
 * Copyright 2017 Erik Jhordan Rey.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package erikjhordanrey.konverter.data.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import erikjhordanrey.konverter.data.remote.CurrencyResponse
import erikjhordanrey.konverter.data.remote.RemoteCurrencyDataSource
import erikjhordanrey.konverter.data.room.CurrencyEntity
import erikjhordanrey.konverter.data.room.RoomCurrencyDataSource
import erikjhordanrey.konverter.domain.AvailableExchange
import erikjhordanrey.konverter.domain.Currency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepository @Inject constructor(
    private val roomCurrencyDataSource: RoomCurrencyDataSource,
    private val remoteCurrencyDataSource: RemoteCurrencyDataSource
) : Repository {

  override fun getAvailableExchangeFromFirebase(fromCurrency: String,
      toCurrency: String): LiveData<AvailableExchange> {
    val mutableLiveData = MutableLiveData<AvailableExchange>()
    //Get Data from Firebase and select quotes child
    FirebaseDatabase.getInstance()
        .getReference().child("quotes")
        .addListenerForSingleValueEvent(object : ValueEventListener {
          //if the operation is failed
          override fun onCancelled(error: DatabaseError?) {
            mutableLiveData.value = null
          }

          //if the operation succeeded, extract data and return exchange rate to the VM
          override fun onDataChange(data: DataSnapshot?) {
            if (data?.exists()!!) {
              var dataToResponse = CurrencyResponse(true, data.value as Map<String, Double>)

              dataToResponse = CurrencyResponse(true, processCurrencies(fromCurrency, toCurrency,
                  dataToResponse))

              mutableLiveData.value = transform(dataToResponse)
            }
          }
        })

    return mutableLiveData
  }


  val allCompositeDisposable: MutableList<Disposable> = arrayListOf()

  override fun getCurrencyList(): LiveData<List<Currency>> {
    val roomCurrencyDao = roomCurrencyDataSource.currencyDao()
    val mutableLiveData = MutableLiveData<List<Currency>>()
    val disposable = roomCurrencyDao.getAllCurrencies()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ currencyList: List<CurrencyEntity> ->
          mutableLiveData.value = transform(currencyList)
        }, { t: Throwable? -> t!!.printStackTrace() })
    allCompositeDisposable.add(disposable)
    return mutableLiveData
  }


  override fun getTotalCurrencies() = roomCurrencyDataSource.currencyDao().getCurrenciesTotal()

  override fun addCurrencies() {
    val currencyEntityList = RoomCurrencyDataSource.getAllCurrencies()
    roomCurrencyDataSource.currencyDao().insertAll(currencyEntityList)
  }

  private fun transform(currencies: List<CurrencyEntity>): List<Currency> {
    val currencyList = ArrayList<Currency>()
    currencies.forEach {
      currencyList.add(Currency(it.countryCode, it.countryName))
    }
    return currencyList
  }

  override fun getAvailableExchange(currencies: String): LiveData<AvailableExchange> {
    val mutableLiveData = MutableLiveData<AvailableExchange>()
    val disposable = remoteCurrencyDataSource.requestAvailableExchange(currencies)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ currencyResponse ->
          if (currencyResponse.isSuccess) {
            mutableLiveData.value = transform(currencyResponse)
          } else {
            throw Throwable("CurrencyRepository -> on Error occurred")
          }
        }, { t: Throwable? -> t!!.printStackTrace() })
    allCompositeDisposable.add(disposable)
    return mutableLiveData
  }

  private fun transform(exchangeMap: CurrencyResponse): AvailableExchange {
    return AvailableExchange(exchangeMap.currencyQuotes)
  }

  private fun processCurrencies(currency1: String, currency2: String,
      dataToResponse: CurrencyResponse): HashMap<String, Double> {
    val exchangeValues: HashMap<String, Double> = HashMap()
    dataToResponse.currencyQuotes.forEach({
      if (it.key.equals("USD" + currency1) || it.key.equals("USD" + currency2)) {
        exchangeValues.put(it.key, it.value)
      }
    })
    return exchangeValues
  }

}
