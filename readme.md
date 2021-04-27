# Modern Treasury Client

This repository publishes a JVM client for the [Modern Treasury API](https://docs.moderntreasury.com/reference)
[Link to technical design doc](https://docs.google.com/document/d/1jRiC7TdkA88_Wto7dya_EP4Ok6S7McJbvGh6JYIRB6g/edit#)

## Technologies
**Languages:** Kotlin
**Runtime version:** Kotlin 1.4

### Usage
The client is (TODO will be) available on our internal maven repository. Add it as a dependency:
```
TODO
```
## Required AWS Parameters
Usage of the client depends on authentication parameters available in the AWS Secret store. They are:
- `$CP_CONFIG_MODERNTREASURY_API_ORG_ID`: The username for the ClassPass organization in Modern Treasury.
- `$CP_CONFIG_MODERNTREASURY_API_SECRET`: The API secret key for the ClassPass organization in Modern Treasury.

## Setup
The client comes with a guice module, which you can add to your application's own guice module like this:
```
    override fun configure() {
        val config = ModernTreasuryConfig(
            organizationId = ..., /* from aws secret store */
            apiKey = ..., /* from aws secret store */
        install(ModernTreasuryModule(config))
    }
```

From there you'll be able to access an instance of `ModernTreasuryClient` via injection.

### Testing
To run all tests, execute:
```
./gradlew test
```

## Ownership
**Squad:** Plans And Payments
**Slack:** [squad-plans-payments](https://classpass.slack.com/archives/CFW7SMMQF)
**Jira:** [FIN Jira project](https://classpass.atlassian.net/jira/software/c/projects/FIN/issues/)
