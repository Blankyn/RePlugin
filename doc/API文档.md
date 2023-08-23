针对一些核心的，对外公开的类，我们有非常详细的JavaDoc说明，方便您在开发中参考。

请您直接在Android Studio中点击某个您想了解的类即可看到。

目前已知的一些对外公开的核心类，都放在com.qihoo360.replugin包下，具体如下：

* RePlugin
* RePlugin.App
* RePluginApplication
* PluginServiceClient
* PluginProviderClient
* IHostBinderFetcher

以及一些Utils类，如下：
* IPC
* LocalBroadcastHelper
* ThreadUtils

以及一些可允许自定义的类：
* RePluginClassLoader
* RePluginConfig
* RePluginCallbacks
* RePluginEventCallbacks

由于类可能会发生一些变化，这里仅为举例，不再赘述。欢迎随时在Android Studio中查看JavaDoc文档，了解更多的玩法。