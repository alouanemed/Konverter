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

package erikjhordanrey.konverter.data.remote

import javax.inject.Inject

class RemoteCurrencyDataSource @Inject constructor(private val remoteCurrencyService: RemoteCurrencyService) {

  fun requestAvailableExchange(currencies: String) =
      remoteCurrencyService.requestAvailableExchange(
          RemoteContract.ACCESS_KEY_API_LAYER, currencies, RemoteContract.FORMAT_TYPE)
}
