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

## Creating a release
Merge your changes to main. Checkout the latest main locally.
Make a tag: `git tag -am "1.0.3" 1.0.3`
Push the tag: `git push --tags`

## Ownership
**Squad:** Plans And Payments  
**Slack:** [squad-plans-payments](https://classpass.slack.com/archives/CFW7SMMQF)  
**Jira:** [FIN Jira project](https://classpass.atlassian.net/jira/software/c/projects/FIN/issues/)  
