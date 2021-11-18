# Overview

This is a JVM client for the [Modern Treasury API](https://docs.moderntreasury.com/reference), written in Kotlin. Backed by AsyncHTTPClient,
the requests made by moderntreasury-client are non-blocking. So far, this implementation covers most of the endpoints for
working with ModernTreasury's ledger API. Pull requests to implement other endpoints are welcome!

# Contributing
We welcome contributions from everyone! See [CONTRIBUTING.md](CONTRIBUTING.md) for information on making a contribution.

# Usage

## Setup
Instantiate an `AsyncModernTreasuryClient` by calling the static `asyncModernTreasuryClient()` function with a config object. You should use
a single instance of `ModernTreasuryClient` for your entire application's lifecycle.

## Authenticating
To authenticate with Modern Treasury, provide the client with your api key's Organization Id and Secret.

# Development

## Testing
To run all tests, execute:
```
./gradlew test
```

## Live Tests
This project also contains a suite of tests that make requests against the real Modern Treasury api. These tests are
skipped by default to decouple our CI builds from Modern Treasury itself but you can (and should) run them yourself like
this:
1. `cp live-test/src/test/resources/live-tests.properties.example live-test/src/test/resources/live-tests.properties`
2. fill in your organization id and api key values in `live-test/src/test/resources/live-tests.properties`
3. run the tests with `./gradlew test -PliveTests`

## ModernTreasuryFake


## Formatting
`./gradlew check` will check code formatting, and `./gradlew formatKotlin` will autoformat the code.

## License headers
The `./gradlew check` task will ensure that license headers are properly applied, and `./gradlew licenseFormat` will apply headers for you.

# License
See [LICENSE](LICENSE) for the project license.
