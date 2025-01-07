/**
 * Copyright 2025 ClassPass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.classpass.moderntreasury.model.request

/**
 * In requests to ModernTreasury, metadata can be included as a map of strings. If you would like to remove metadata
 * that is already set, you can unset it by passing in the key-value pair with an empty string or null as the value.
 */
typealias RequestMetadata = Map<String, String?>

fun Map<String, String>.toQueryParams(): Map<String, List<String>> {
    return this.mapKeys { (key, _) ->
        "metadata[$key]"
    }.mapValues { (_, value) ->
        listOf((value))
    }
}
