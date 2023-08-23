RePlugin提供了非常丰富的“自定义行为”特性。帮助您更好的在“无需修改源代码”的情况下，来使用RePlugin。

本文将分为两大部分：
 * 如何自定义
 * 最常见的自定义行为

## 如何自定义？

RePlugin的自定义行为，全部集中在RePluginConfig类中，并写了非常详细的JavaDoc供您们参考。

### 设置Config

推荐在RePluginApplication中覆写createConfig方法，如下：

```java
public class SampleApplication extends RePluginApplication {
    @Override
    protected RePluginConfig createConfig() {
        RePluginConfig c = new RePluginConfig();
        ...
        return c;
    }
```

如果您是“非继承式”，则需要在调用 RePlugin.App.attachBaseContext() 的地方，传递RePluginConfig即可。具体做法如下：

```java
RePluginConfig c = new RePluginConfig();
...
RePlugin.App.attachBaseContext(context, c);
```

> 注意：RePluginConfig中的东西是“只允许写一次的”。一旦在上述两个地方设置了RePluginConfig之后，则其内变量**不允许再修改**，其表现为调用所有set方法均失效，提示“rpc.cam: do not modify”错误。

### 设置Callbacks

Callbacks（含EventCallbacks）都存在RePluginConfig内，设置方法：
* RePluginConfig.setCallbacks()
* RePluginConfig.setEventCallbacks()

除此之外，针对RePluginCallbacks，我们还提供了一套更便捷的设置方式，同样，可以继承RePluginApplication中的createCallbacks方法，并让其继承**RePluginCallbacks**，如下：

```java
public class SampleApplication extends RePluginApplication {
    @Override
    protected RePluginCallbacks createCallbacks() {
        return new HostCallbacks(this);
    }
    
    private class HostCallbacks extends RePluginCallbacks {
        private HostCallbacks(Context context) {
            super(context);
        }
        ...
    }
}
```

只需createCallbacks即可，无需配置RePluginConfig，其RePluginApplication类内部会做相应的处理。

## 常见的自定义行为

以下是您们经常用到的常见自定义行为，在Sample的Host中已经都体现了，大家欢迎参考。

除了这些常见自定义行为外，RePlugin还提供了大量的方法帮助您设置。具体可参见以下类的JavaDoc帮助：
* RePluginConfig
* RePluginCallbacks
* RePluginEventCalbacks
* RePlugin

### 在插件不存在时，提示下载

通过覆写 **RePluginCallbacks.onPluginNotExistsForActivity()** 方法，当插件"没有安装"时触发此逻辑，可打开您的"下载对话框"并开始下载。其中"intent"需传递到"对话框"内，这样可在下载完成后，打开这个插件的Activity。

```java
@Override
public boolean onPluginNotExistsForActivity(Context context, String plugin, Intent intent, int process) {
    ...
    return super.onPluginNotExistsForActivity(context, plugin, intent, process);
}
```

### 开启“插件类不存在时读取宿主”功能

RePlugin默认不鼓励大家使用此功能（具体原因参见FAQ的说明）。但由于此类需求相对较多（包括360手机助手在内）。我们研究出一套“稳定的”插件与宿主共享类方案，插件可“无缝”使用宿主类，且没有Hook，无需改变父子类加载器关系。

具体做法很简单，调用RePluginConfig.setUseHostClassIfNotFound(true)即可。如不调用，则默认不支持此功能，这样更稳定。

### 开启/关闭“签名校验”功能

有关“签名校验”的具体信息，请[[点击此处阅读《插件的管理》中“安全与签名校验”一节|插件的管理#安全与签名校验]]。其开关在RePluginConfig.setVerifySign()中。

### 针对打点统计数据，或特殊事件的处理

RePluginEventCallbacks就是为此需求而设计。请详见JavaDoc以及Sample中所述，这里只展示最常用的“插件安装失败”的示例。

```java
@Override
public void onInstallPluginFailed(String path, InstallResult code) {
    QDasStats.reportFail(xxx);
    super.onInstallPluginFailed(path, code);
}
```