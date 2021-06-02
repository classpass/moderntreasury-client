# Modern Treasury Client

This is a JVM client for the [Modern Treasury API](https://docs.moderntreasury.com/reference)  
[Link to technical design doc](https://docs.google.com/document/d/1jRiC7TdkA88_Wto7dya_EP4Ok6S7McJbvGh6JYIRB6g/edit#)

## Technologies
**Languages:** Kotlin  
**Runtime version:** Kotlin 1.4

### Usage
## Authenticating
To authenticate with Modern Treasury, provide the client with your api key's Organization Id and Secret.

## Setup
Instantiate an `AsyncModernTreasuryClient` by calling the static `asyncModernTreasuryClient()` function with a config object. You should use 
a single instance of `ModernTreasuryClient` for your entire application's lifecycle.

### Testing
To run all tests, execute:
```
./gradlew test
```

#### Live Tests
This project also contains a suite of tests that make requests against the real Modern Treasury api. These tests are
skipped by default to decouple our CI builds from Modern Treasury itself but you can (and should) run them yourself like
this:
1. `cp live-test/src/test/resources/live-tests.properties.example live-test/src/test/resources/live-tests.properties`
2. fill in your organization id and api key values in `live-test/src/test/resources/live-tests.properties`
3. run the tests with `./gradlew test -PliveTests`

## Creating a release
Merge your changes to main. Checkout the latest main locally.
Make a tag: `git tag -am "1.0.3" 1.0.3`
Push the tag: `git push --tags`

## Ownership
**Squad:** Plans And Payments  
**Slack:** [squad-plans-payments](https://classpass.slack.com/archives/CFW7SMMQF)  
**Jira:** [FIN Jira project](https://classpass.atlassian.net/jira/software/c/projects/FIN/issues/)  
