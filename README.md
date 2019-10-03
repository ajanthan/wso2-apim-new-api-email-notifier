# wso2-apim-new-api-email-notifier
This is an extension for WSO2 API Manager to notify all the subscribers(all the users in subscriber role) whenever there is a new API published

## Deploying the Extension

1. Build the maven project and get the artifact from `target` folder
2. Copy the artifact to `WSO2_APIM_HOME/repository/components/dropins`
3. Restart is required to the extension be active

## Configuring the Extension
As mentioned in [WSO2 documentation](https://docs.wso2.com/display/AM260/Customizing+a+Workflow+Extension) update the workflow configuration as follows
```xml
<WorkFlowExtensions>
...
    <!-- Publisher related workflows -->
    <APIStateChange executor="org.wso2.carbon.apimgt.notification.extension.APIStateChangeNotificationExecutor">
        <Property name="stateList">Created:Publish</Property>
        <Property name="adminEmail">wso2alert.demo@gmail.com</Property>
        <Property name="emailPassword">password</Property>
    </APIStateChange>
...
</WorkFlowExtensions>
```
Refer the [complete configuration](src/main/resources/workflow-configs/workflow-extensions.xml) for further details.