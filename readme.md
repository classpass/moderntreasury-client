# Modern Treasury Client

This is a JVM client for the [Modern Treasury API](https://docs.moderntreasury.com/reference)
[Link to technical design doc](https://docs.google.com/document/d/1jRiC7TdkA88_Wto7dya_EP4Ok6S7McJbvGh6JYIRB6g/edit#)

## Technologies
**Languages:** Kotlin
**Runtime version:** Kotlin 1.4

### Usage
## Authenticating
To authenticate with Modern Treasury, provide the client with your api key's Organization Id and Secret.

## Guice Setup
The client comes with a guice module, which you can add to your application's own guice module like this:
```
    override fun configure() {
        val config = ModernTreasuryConfig(
            organizationId = ..., /* Your api key's organization id */
            apiKey = ..., /* Your api key's secret */
        install(ModernTreasuryModule(config))
    }
```

From there you'll be able to access an instance of `ModernTreasuryClient` via injection.

## Manual Setup
Instantiate your own `AsyncModernTreasuryClient` by passing in an `AsyncHttpClient` configured to use your API key in 
basic auth for all requests, and a base URL, typically `https://app.moderntreasury.com/api`. You should use a single
instance of `AsyncModernTreasuryClient` for your entire application's lifecycle.

### Testing
To run all tests, execute:
```
./gradlew test
```

## Ownership
**Squad:** Plans And Payments
**Slack:** [squad-plans-payments](https://classpass.slack.com/archives/CFW7SMMQF)
**Jira:** [FIN Jira project](https://classpass.atlassian.net/jira/software/c/projects/FIN/issues/)
