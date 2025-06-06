# 🚨 Archived Project
This project is no longer maintained. No further updates or support will be provided.
Use the Kotlin SDK now provided by Modern Treasury https://github.com/Modern-Treasury/modern-treasury-kotlin

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

## Releasing a version

### Checking artifacts locally

To see the artifacts that would be released, build the relevant artifacts locally using a fake version `foo`:

```
./gradlew publishSonatypePublicationToLocalDebugRepository -Pversion=foo
tree */build/repos/localDebug
```

### Maven Central requirements

To publish to Maven Central, you'll need the `sonatypeUsername` and `sonatypePassword` Gradle properties set (`~/.gradle/gradle.properties` is typically where people put these). You'll also need GPG set up for the [signing plugin](https://docs.gradle.org/current/userguide/signing_plugin.html), as per Sonatype's [requirements](https://central.sonatype.org/publish/requirements/gpg/). All told, your properties should have:


```
sonatypeUsername = ...
sonatypePassword = ...

signing.keyId = ...
signing.password = ...
signing.secretKeyRingFile = ...
```

### Uploading a test version to Sonatype

Once you have that set up, you can try publishing with a test version:

```
./gradlew -Pversion=0.0 publishToSonatype
```

This will create and populate a staging repo in [Sonatype's s01 nexus instance](https://s01.oss.sonatype.org/#stagingRepositories). You can inspect the contents, and "Close" it, which runs validation for GPG signatures, etc. If closing reports validation errors, those must be addressed before releasing can work. Either way, "Drop" the staging repo once you're done playing with it.

### Releasing a real version

The [release plugin](https://github.com/researchgate/gradle-release) automates making appropriate commits, tags, etc. Run `./gradlew release` and follow the prompts to pick the released version and the next snapshot version.

Once that's done, go to [nexus](https://s01.oss.sonatype.org/#stagingRepositories) and "Close", then "Release" the staging repo you just uploaded. After 15-20 mins, the artifacts should be available in Maven Central.

# License
See [LICENSE](LICENSE) for the project license.
