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
