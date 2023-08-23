以下是RePlugin组内经常遇到的问题，在此进行解答。以方便新人，避免踩坑。

敬请留意，请先进入GitHub官网查看相关的文档：
https://github.com/Qihoo360/RePlugin
https://github.com/Qihoo360/RePlugin/wiki

在此非常感谢RePlugin开源大家庭群中， @osan @志鹏-深圳 @老王头 @Youloft-Coder @不知不觉 的热心贡献！

模板：

```markdown
#### Q：xxx

**A：xxx**
[Detail]

回答者：@xxx （GitHub上的帐号信息，或QQ昵称）
```

## RePlugin 主要疑惑解答

### Q：您们的项目支持“插件使用一套基础库（如network、logger等）”吗？
**A：如果是Jar包（不含资源）的话是完全支持的，虽然我们不鼓励这样做（2013年用到了很多坑，见后）**。请参见Sample代码中的Fragment示例。

此外，出于对“极致稳定”的追求，**我们和市面上大部分“共享ClassLoader”方案有所不同**，具体表现在：

* **共享的范围：“插件可以使用主程序的公共类”**，若结合 RePlugin.registerHookingClass()，还能做到“使用某个公共插件的公共类”。

从目前调研的情况来看，从公共代码库而言，可满足市面上大多数情况了。但我们不支持（也不打算支持）“所有插件和宿主都是一家亲”的完全耦合形态，因为这个有坑（见后）。

* **先后顺序**：RePlugin的做法是，优先使用插件内的类，若插件类找不到，才使用主程序（或经过registerHookingClass跳转后的）的类
* **引入的方式**：支持通过provided的方式来引入Jar包。但目前还不支持引入AAR（原因见后）
* **要求和限制**：不同于一些要求“插件和宿主必须不能重名”的情况，**RePlugin的方案是允许插件和宿主“重名”的，这样对开发者而言，束缚更小**。

有人可能要问了，**为什么我们“不鼓励”使用基础库方案呢**？

> 其实我们早在**2013年就研究出“共享代码”（当时是修改android.jar）方案了**，大家可以反编译当年手机卫士的APK即可了解。但后来经过一年的尝试后发现：
> 
> * **最容易出现的，就是“插件间版本”导致的问题。**
> 
> 试想，无论是反射，还是走Binder，其实都要求“必须做Try-catch”才行，这样就“天然的”将版本情况考虑了进来。但“直接调用”则不需要*（例如你调用x.aa()方法，通常情况下，下意识里是不会想到做Try-Catch的）*。**一旦调用的方法“被删改”，则会直接抛出NoSuchMethodError或者ClassNotFoundException等，对程序稳定性造成极为严重的影响**。
> 
> 我们曾在2013年底时，遇到过“主防”模块因重构而删除了一个类的方法，导致很多调用者“莫名其妙”的Crash掉。其原因就是没有做Try-Catch和版本判断。此情此景，历历在目。
> 
> * 其次，就是ClassLoader共享时，需**要求各插件不能有“重复的包名和类名”，否则会出现强制类型转换问题**。
> 
> 大家可以看到，2013年的卫士APK里面的插件，其“类名”都带上了插件前缀，就是为了防止这个情况的出现。
> 
> * 再次，就是**多出来Hook点了**，因为需要做DexPathList的反射和修改，涉及到Hook点了，不排除会有兼容性问题
> 
> 当然，和“共享资源”方案不同的是，代码共享方案不涉及到“ROM适配”情况，所以，在Hook点上稳定性还是可以的。现在Sample上已经提供了Fragment的方案（感谢 @Coder提供），欢迎大家参考。

回答者：@张炅轩

### Q：您们的项目支持“插件直接使用公共资源（或AAR）的方案”吗？

**A：我们支持插件间，插件和宿主间的资源交互**，但不支持（也不打算支持，2013年踩了无数坑，后述）“直接使用资源”的方式。即便支持这一特性会减少改动量。

> 这里所说的“直接使用资源”是指：插件A通过“R.drawable.common_xxx”来使用插件B或主程序的资源。

那么，我们非常推荐的做法是：

* **通过反射来引入View（例如卫士的WebView、信息流等插件都是这么做的）。这也是我们最为推荐的做法**。一方面，View本身是“一个整体”，内部可以通过代码来做版本判断、甚至动画等高难度的处理，兼容性很不错；另一方面，若View被删除了，则只要开发者在外面做判空处理即可，不用担心使用某个资源时出现的严重问题
* **通过使用公共类库（如XML引入的方式）来引入View（目前手机助手的换肤UI就是这么做的）**。您可以参见Sample中的Fragment了解大致内容。
* 针对其它需要（例如不方便提供View的），则**使用 RePlugin.fetchResources() 来获取Resources对象**，并通过getIdentifier来反射获取资源。这样即便资源删除了，也可以通过判空来“天然的”（无需刻意判断版本的）解决版本问题

以上两种做法均有Sample，分别是Fragment和获取Layout的，欢迎大家参考。

> 补充：RePlugin会同时把Host和Plugin的Context传递给插件，供开发者选择。想用插件的就用插件，想用宿主的就用宿主，甚至用其它插件的也都可以。

有人可能要问了，**为什么不能做到“像直接使用公共库”那样，顺带也支持“直接使用公共资源”呢？是你们做不到吗？**

当然不是！这个和我们当年遇到的非常多的坑，有很大的关系：

> 其实我们早在**2013年就研究出“共享资源”（修改Aapt）方案**，当时是联合台湾团队一起搞的，没有任何的参考。大家可以反编译当年手机卫士的APK即可了解。但后来经过一年的尝试后发现：
> 
> * 和代码一样，**资源同样有“插件间版本”导致的问题，而且还更加严重**
> 
> 正常情况下，直接调用类中的某个方法，至少可能还会做个Try-Catch（有些是习惯使然）。但资源呢？**有谁能想到“直接通过R.string.xxx”的东西，它一定是拿不到的呢？**但是，现实是残酷的。若直接使用跨插件、跨主程序的资源，**则任何对资源的删改，都有可能导致“资源错乱”、崩溃等情况**。
> 
>  我们曾在2013年底时，遇到过二维码插件，因“直接使用主程序资源”，而恰逢主程序布局和资源调整，导致插件公共资源出现“显示错乱”的情况，且“必须升级主程序才能修改”（当然，后来也想过把资源都放公共插件里，最后还是放弃整套方案了）。
>  
>  这还没完，等我们准备换用现在的方案前，惊讶的发现，为了兼容当时为数不多（也就10多个插件）的公共资源，避免这些插件出现显示异常，**我们必须要在宿主中留下多种资源，即便这个资源已不再使用（但老插件能保证不用吗）**。和代码一样，此情此景，历历在目。
>  
> * **机型适配，机型适配，机型适配**！
> 
> 我们曾在内部称之为“共享资源”方案——也即需要修改Aapt、做addAssetPath等。虽然其好处是插件和主程序可以“直接使用”各自的资源，交互更容易。**但代价是需要做“资源分区”，以及针对不同机型（如ZTEResources、MiuiResources）等做适配，稳定性上值得考量**。
> 
> 也就是说，共享资源方案和RePlugin核心诉求——稳定是第一要义，是背离的。我们肯定不能做这个，即便通过巨大的努力，使其适配了市面上近乎所有的手机，但确实不能保证未来不会因某个手机，或ROM的修改，而出现新的问题。
> 
> * 当我们插件数量达到20多个时，会发现**随着插件的增多，“资源分区ID”会越来越难管控**，这对我们来说，同样也是一种挑战。

不同于“共享代码”方案，**RePlugin是不会计划支持直接使用资源的方案，我们不想修改RePlugin的主打说明和宗旨，我们要做的，就是极致稳定，与众不同**（1 Hook点，0 Binder Hook，无需厂商适配）。但请放心，我们会尽可能提供方便的办法，让开发者们可以“调用一些API”即可获取插件的资源，相对来讲，稳定又比较方便。

回答者：@张炅轩

### Q：您们和360之前发的DroidPlugin的主要区别是什么？
A：这个问题问得很好。很多人都有这个疑惑——“*为什么你们360要开发两套不同的插件化框架呢*”？

其实归根结底，最根本的区别是——**目标的不同**：

* DroidPlugin主要解决的是各个独立功能拼装在一起，能够快速发布，其间不需要有任何的交互。**目前市面上的一些双开应用，和DroidPlugin的思路有共同之处**。当然了，要做到完整的双开，则仍需要大量的修改，如Native Hook等。

* RePlugin解决的是各个功能模块能独立升级，又能需要和宿主、插件之间有一定交互和耦合。

此外，从技术层面上，其最核心的区别就一个：**Hook点的多少**。

* DroidPlugin可以做到**让APK“直接运行在主程序”中**，无需任何额外修改。但需要Hook大量的API（包括AMS、PackageManager等），在适配上需要做大量的工作。

* RePlugin只Hook了ClassLoader，所以**极为稳定**，且同样**支持绝大多数单品的特性**，但需要插件做“少许修改”。好在作为插件开发者而言无需过于关心，因为通过“动态编译方案”，开发者可做到“无需开发者修改Java Code，即可运行在主程序中”的效果。

可以肯定的是，**DroidPlugin也是一款业界公认的，优秀的免安装插件方案**。我相信，随着时间的推移，RePlugin和DroidPlugin会分别在各自领域（全面插件化 & 应用免安装）打造出属于自己的一番天地。

回答者：@张炅轩

## RePlugin 接入和类库开发解惑

### Q：java.lang.IllegalStateException: You need to use a Theme.AppCompat theme (or descendant) with this activity.
**A：请检查以下配置是否正确：**
* 在插件 Mainifest 中，将主题声明为 AppCompat 类的主题（如 @style/Theme.AppCompat，也可以是自定义 AppCompat 主题）；
* 在宿主的 build.gradle 中修改 replugin-host-gradle 配置如下：
```groovy
  apply plugin: 'replugin-host-gradle'
  repluginHostConfig {
    useAppCompat = true
  }
```
* 宿主和插件使用的 appcompat-v7 库版本要保持一致（因为内部用到了反射的方式来取 Theme 的 int 值，不同版本的 v7 库 int 值可能不一致）。

回答者：@Coder，@胡俊杰

### Q: java.lang.ClassNotFoundException: Didn't find class "xxx.loader.p.ProviderN1"

**A：通常遇到这个问题是因为没有在主程序的AndroidManifest.xml中声明Application**，或在Application中没有调用RePlugin.App.attachBaseContext等方法导致。

请严格按照“[主程序接入指南](https://github.com/Qihoo360/RePlugin/wiki/%E4%B8%BB%E7%A8%8B%E5%BA%8F%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97)”所述来完成接入，一共只有三个步骤，非常简单。同时，请关闭Instant Run防止出现未知问题（正在兼容）。

当然，如果在严格按照接入文档后，仍出现这个问题（这种情况非常罕见），请向我们提交Issue。Issue中应包括：完整的Logcat信息（含崩溃前后上下文）、手机型号、ROM版本、Android版本等。感谢您的理解。

回答者：@张炅轩

### Q : 插件中使用透明主题的注意点？
**A : 如下：**

* **透明样式目前不能结合 AppCompact 使用；如果要使用透明样式，插件 manifest 中必须声明为以下四种主题之一：**

```java
/**
     * 手动判断主题是否是透明主题
     */
    public static boolean isTranslucentTheme(int theme) {
        return theme == android.R.style.Theme_Translucent
                || theme == android.R.style.Theme_Dialog
                || theme == android.R.style.Theme_Translucent_NoTitleBar
                || theme == android.R.style.Theme_Translucent_NoTitleBar_Fullscreen;
    }
```
> 原因： 由于 AppCompat 没有默认的透明主题，所以 HOST 中没有 AppCompat 的透明坑位。
> 
> 计划：支持起来太过于复杂，暂不提供。

回答者 : @胡俊杰

### Q : 如何解决插件中只希望通过provided方式引入的库，却在插件中其他第三方库存在依赖的问题？
**A : 场景如下：**

插件中只希望通过provided方式引入rxjava库，然而在插件依赖的某第三方库中依赖了rxjava，而该第三方库又无法修改，这时可以通过如下方式解决：

* 在dependencies统一指定transitive为false 或单独指定某依赖项的transitive为false
* 手动将未加入的依赖添加至dependencies
> transitive用于自动处理子依赖项。默认为true，gradle自动添加子依赖项，形成一个多层树形结构；设置为false，则需要手动添加每个依赖项。

回答者 : @小志

### Q：Clone了项目后如何使用本地的Gradle插件而不是使用jcenter托管的呢
**A：需要遵循这两个步骤：**
1. 首先你需要将host-gradle/plugin-gradle打开然后执行publishToMavenLocal来进行将你需要使用的插件发布到Maven本地仓库中
2. 在你需要使用本地插件的项目根目录的build.gradle加入如下配置

![MavenLocal](https://qqadapt.qpic.cn/txdocpic/0/a3d234f63e5ba16f6334c7f9f656055f/0)

如果这样操作之后还是没有能使用到本地的插件则需要检查在你相应gradle工程中的版本配置是否与你使用的一致，参考文章 ：http://note.youdao.com/share/?id=61c0f27494c5a466a40642829da2938c&type=note#/

回答者：@Coder

### Q : 插件中使用高德地图的经验分享？
**A：**
* 关于高德地图的key值，请用宿主的包名创建一个key值在插件的AndroidManifest.xml中，否则会显示key值不正确
>解释：高德地图的key值，高德地图是用宿主的包名校验的，插件的包名没有校验

回答者 : @书生

### Q : 插件中依赖jar或aar后导致的几种编译错误解决？
**A : **
* 编译错误：Error converting bytecode to dex:Cause: java.lang.RuntimeException: Exception parsing classes
Caused by: com.android.dx.cf.iface.ParseException: class name (com/xxx/android/xxx/A) does not match path (com/xxx/android/xxx/a.class)
>解决：这是因为jar混淆后类文件与类名大小写不一致，导致编译失败。与百度地图等大小写编译问题类似。解决方法是在混淆jar时，配置下混淆规则，来解决混淆后大小写不一致问题：
>
>##混淆时不使用大小写混合，混淆后的类名为小写(尤其windows用户，因为windows大小写不敏感)
>
>-dontusemixedcaseclassnames

回答者 : @osan

### Q：如何解决webview加载asserts中的html等资源文件失败的问题？具体如：WebView.loadUrl("file:///android_asset/xxx.html")时提示：RR_FILE_NOT_FOUND
A：
1. 前提：设置webview的允许使用File协议，使能访问asset加载本地的HTML等资源文件。即：WebView.getSettings().setAllowFileAccess(true)，不过会存在WebView跨源攻击问题，你可以做一些相关的安全策略，规避风险规避；

2. 采用插件化框架后后，WebView并不能保证一定能读取主程序和插件内的assets中html等资源文件，其中，Android API < 19 只能读取插件assets中的html文件，而在 Android API >= 19 只能读取主程序assets中的html文件。

其具体原因是：在API 19开始，webview内核由WebKit过渡到了Chromium，其内部的getAssets方法的Context由mContext改ApplicationContext。

具体源码见：http://androidxref.com/5.0.0_r2/xref/external/chromium_org/content/public/android/java/src/org/chromium/content/browser/BrowserStartupController.java

可以看到： Context appContext = mContext.getApplicationContext();

问题解决方案：**将assets中的html等文件提取拷贝到File System中（如插件的fils或者cache中）**，具体时机可以在webview插件初始化的时候或者在具体的loadurl之前完成即可。

回答者：@SkyEric

## RePlugin 插件管理解惑

### Q：内置插件可否按需选择是否默认加载呢？
**A：只要放入了assets/plugins中的*.jar是不支持按需加载的**，如果你真的需要按需加载的话也是可以如下操作的：
1. 将你需要按需加载的插件放入assets的其它目录中这样gradle就不会生成相关的内置插件信息
2. 释放你的插件到一个磁盘目录
3. 调用Replugin.install(path)方法来进行安装调用即可实现

回答者：@Coder

### Q：您们是否支持DataBinding？

A：支持。我们有几个插件在用。除此之外，我们的Sample工程，其Demo2就是用DataBinding做的。您们可以体验一下。

回答者：@张炅轩

### Q：插件中共享宿主的DataBinding库时的注意点？

**A：**
可能会有这种场景，例如当使用DataBindingUtil.inflate(LayoutInflater.from(pluginContext), R.layout.dialog_test, null, false)是 会报 view tag isn't correct on view:layout这个异常。

原因：android.databinding.DataBindingUtil是jar包的类，但它里面的静态变量 android.databinding.DataBinderMapper 是编译器生成，也就是说每个apk，会有一个DataBinderMapper。这个类保存的是生成类 XXXBinding 与 layout的关系。由于宿主是最开始加载，所以android.databinding.DataBindingUtil里面的是宿主的DataBinderMapper。

解决方案：在插件工程建立一个android.databinding.PluginDataBindingUtil (即拷贝一份DataBindingUtil代码改个类名),之后再插件中就使用PluginDataBindingUtil即可。

回答者：@MinF

### Q : 如何监听安装事件
**A : 如图。也可参考最新的Sample工程。**

![EventCallbacks](https://qqadapt.qpic.cn/txdocpic/0/0c6025a6ada89d4af6bdfa20a9910399/0)

回答者 : @志鹏-深圳

### Q : 如何判断插件的各种 Activity 被成功替换成 PluginXXXActivity
**A : 如图~**

![EventCallbacks](https://qqadapt.qpic.cn/txdocpic/0/0e977730fa1eefd11edcd108e0dfef92/0)

在继承AppCompatActivity的类中打印父类名，如果替换成功，上图会显示 PluginAppCompatActivity。这算是一个小技巧，更多的可以查看 Gradle Console的输出日志。

回答者 : @志鹏-深圳

### Q : 插件中使用mutidex分包时的注意点？
**A : 可能遇到情况如下：**

* **无法启动插件activity**：log提示java.lang.NoClassDefFoundError: library.d 或 java.lang.NoClassDefFoundError: com.qihoo360.replugin.Entry$1
> 原因： 插件过大，导致使用了mutidex的处理，而RePlugin的包恰好被分在class2.dex，然后就抛出找不到replugin相关类的异常，导致插件加载失败
> 
> 解决方案：1.插件的代码尽量小，尽量保证在一个dex中；2.可以在gradle中，指定replugin插件库的类分配在主dex中。

还有一种可能，应该是插件被“分包”导致的问题，打包后虽然有主classes有找到com.qihoo360.replugin.Entry但还是报错，自己手动添加maindexlist.txt在里面添加com.qihoo360.replugin打包进去就好了，具体操作请参考http://blog.csdn.net/gaozhan_csdn/article/details/52024497

回答者 : @小志 @T-BayMax

### Q : 多个插件希望使用相同的网络请求框架或其他有公共库时，最佳实践是？
**A ：RePlugin团队提供了360手机卫士团队的实践思路作为参考**

* 卫士的做法一：提供一些基础的插件（如WebView、分享等），各插件对它是反射调用，接口封装好的前提下，这种做法也是很清晰的。
* 卫士的做法二：如果是公共库的话，每个插件放一份，但混淆时会自动去掉无用类和方法，这样的好处是，公共库的任何版本更新不会影响到所有插件

回答者 : @张炅轩 360

### Q：插件没有启动时，为什么无法收到静态广播？
**A：这个问题我们当初在内部讨论了很久。如果安装完成后就开始监听静态广播，则我们担心每次收到一个广播，就会“拉起”一堆的插件，对内存产生影响。为了减少其占用，我们采用的是“按需注册”，也就是加载插件后才去注册的方式。
当然，我们已经把“支持加载前就注册广播”作为未来支持的计划，但我们会有开关控制，方便大家选择。

回答者：@张炅轩

### Q：插件怎么与宿主之间代码共享
**A：**
1. 宿主中有相应的实现代码将其制作为jar
2. 在插件中以provided的方式依赖这个jar（目的是为了骗过编译器）
3. 在宿中打开UseHostClassIfNotFound
> 注：插件和宿主应该可以有相同的类。只不过遵循“先插件后宿主”。

回答者：@Coder

### Q：与百度加固冲突怎么解决？
**A：**
1. 百度加固后的应用程序，attachBaseContext()接口的调用，是通过Native代码反射Java代码来实现的；
2. RePlugin，默认打开双进程架构时，attachBaseContext()中会通过同步Binder来拉起一个新的管理进程，这一步和以上步骤冲突了；
3. 解决方式也很简单，关掉RePlugin的双进程架构即可；

回答者：@Cundong

### Q：使用Fargemnt常见问题汇总？

错误1.“Fragment cannot be cast to android.support.v4.app.Fragment”问题（[#378](https://github.com/Qihoo360/RePlugin/issues/378)、[#467](https://github.com/Qihoo360/RePlugin/issues/467)）？

A：该类问题主要是由于Fragment继承自android.support.v4.app.Fragment，若直接继承android.app.Fragment不会有该类问题，两者的使用以及区别不再赘述。
插件内部出现该类问题，主要是由于在插件的dependencies中直接或间接引入v4包，包括引入第三方库时，其依赖关系导致最终引入与该第三方库版本相对应的v4包（尤其需要注意），以致插件与宿主host的都包含有v4包，最终导致宿主调用插件遇到类转化问题。

具体解决：

方式一：可以参照demo1。通过修改插件的build.gradle文件，以provided files('libs/fragment.jar')方式，骗过编译期，并借助Gradle 的exclude语法来解决版本冲突，移除support-v4，即：

configurations {
all*.exclude group: 'com.android.support', module: 'support-v4'
}

方式二：也可以借助打包工具来处理，具体可参照：[replugin-resolve-deps-conflict](https://github.com/lijunjieone/replugin-resolve-deps-conflict)，同样可解决此类问题。@lijunjie

另外，[如果存在通过provided方式引入的库，而在插件中第三方库存在依赖的问题，可参照解决](https://github.com/Qihoo360/RePlugin/wiki/FAQ#q--%E5%A6%82%E4%BD%95%E8%A7%A3%E5%86%B3%E6%8F%92%E4%BB%B6%E4%B8%AD%E5%8F%AA%E5%B8%8C%E6%9C%9B%E9%80%9A%E8%BF%87provided%E6%96%B9%E5%BC%8F%E5%BC%95%E5%85%A5%E7%9A%84%E5%BA%93%E5%8D%B4%E5%9C%A8%E6%8F%92%E4%BB%B6%E4%B8%AD%E5%85%B6%E4%BB%96%E7%AC%AC%E4%B8%89%E6%96%B9%E5%BA%93%E5%AD%98%E5%9C%A8%E4%BE%9D%E8%B5%96%E7%9A%84%E9%97%AE%E9%A2%98)



错误2."Unable to instantiate fragment com.xx.TestFragment: make sure class name exists, is public, and has an empty constructor that is public"问题([#292](https://github.com/Qihoo360/RePlugin/issues/292))？

A：该问题最可能的原因是由于自定义Fragment类缺少无参的public构造函数，其次看看是否由于混淆导致类名找不到。
另外，不建议通过静态的方式添加fragment，即使用<fragment>标签的方式。提倡通过动态方式添加fragment，即使用add的方式动态添加。

回答者：@SkyEric

### Q : [PluginContext ](https://github.com/Qihoo360/RePlugin/blob/53a39d4396c38cde0cd9f60f04c2ca08db3aaffb/replugin-host-library/replugin-host-lib/src/main/java/com/qihoo360/loader2/PluginContext.java)中，为何删除了对getDatabasePath()方法的重写？
**A : 这个问题比较复杂，需要详细介绍一下。**

主要原因是为了适配 Android 8.1 及后续版本，我们从系统源码的角度来看一下这个问题：

SQLiteOpenHelper#getWritetableDatabase()方法中，会调用
SQLiteOpenHelper#getDatabaseLocked()方法：
 ![](https://s33.postimg.cc/nt2qyq3pr/faq1.png)

SQLiteOpenHelper#getDatabaseLocked()中有打开数据库文件的逻辑，这个逻辑，
在Android 8.1中发生了变化，具体变化，可以通过源码来看：

android-8.0.0_r36：
https://android.googlesource.com/platform/frameworks/base/+/android-8.0.0_r36/core/java/android/database/sqlite/SQLiteOpenHelper.java
![](https://s33.postimg.cc/p84bngcin/faq2.png)

android-8.1.0_r9：
https://android.googlesource.com/platform/frameworks/base/+/android-8.1.0_r9/core/java/android/database/sqlite/SQLiteOpenHelper.java
![](https://s33.postimg.cc/6312dmkz3/faq3.png)

可以看到，Android 8.1中，增加了一次对mContext.getDatabasePath()的使用。
而，如果当前传入的Context是插件上下文，真正实现逻辑位于RePlugin源码中PluginContext类，其内部对getDatabasePath()方法做了重写，重写后的逻辑就是：不再返回原数据库目录，而是返回一个自定义的拼接后的路径（如：plugins_v3_data），导致上述系统源码找不到数据库，因此也就不再创建文件了，插件创建数据库时，直接抛异常了。

为了适配 Android 8.1 这一改动，该方法不再重写，因此，需要各插件之间约定数据库名字，防止出现重名数据库的情况。

回答者：@Cundong
