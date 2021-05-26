package com.classpass.moderntreasury.exception

import org.asynchttpclient.Response

class MissingPaginationHeadersException(response: Response) :
    ModernTreasuryClientException(
        "expected pagination headers on ModernTreasury response from URI ${response.uri}," +
            " but some or all of them were missing"
    )
