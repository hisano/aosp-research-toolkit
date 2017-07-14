# AOSP (Android Open Source Project) Research Toolkit

AOSP Research Tookitは、AOSPのソースコード調査を支援するツールです。  
AOSPのファイル群から、調査用のEclipseプロジェクトを生成するツールを中心に、下記のような作業(一例)をサポートします。

* アプリからフレームワークまで、一気通貫でJavaソースコードを辿る
* Androidバージョンごとの違いを把握する
* 実機を用いてシステムサービスをデバッグする

AOSP Research Tookitの利用方法ですが、下記の二通りの方法があります。

||AOSPから生成されたEclipse・IntelliJプロジェクトを利用|[AOSP](http://source.android.com/source/requirements.html)をビルドした後、AOSP Research Tookitのツールを用いて、Eclipseプロジェクトを生成して利用|
|---|---|---|
|メリット|すぐに調査が可能|自由にバージョンを選んで調査が可能|
|デメリット|用意されたAndroidバージョンでしか調査できない|ソースコードのダウンロード・ビルド等々、事前準備に相当な時間がかかる|

## AOSPから事前生成されたIntelliJプロジェクトを利用

### :black_small_square:準備

Android 2.3から7.1のソースコードを含む下記プロジェクトをダウンロード後、解凍してIntelliJで開いてください。

http://files.hisano.jp/aosp/aosp.zip

### :black_small_square:Eclipseと比べたIntelliJを使うメリット

* Javaのコンパイル時に展開されてしまう定数も正しく検索結果に表示
* Javaのリフレクション経由のコードも検索結果に表示
* 複数のAndroidバージョンをまたがって、クラス・メソッド等の利用場所を検索可能

## AOSPから事前生成されたEclipseプロジェクトを利用

### :black_small_square:準備

下記から、調査したいバージョンのファイル名をクリックしてダウンロードしてください。  

> :warning: Nexus端末等での実機デバッグを行うためには、書き込むファームウェアとAndroidバージョンを一致させる必要があります。  
> https://source.android.com/source/build-numbers.html  
> https://developers.google.com/android/nexus/images  

> :warning: 複数のAndroidバージョンを比較したい場合には、複数ファイルをダウンロードしてください。  

|Androidバージョン|ファイル|Nexus端末向けファームウェアでのバージョン|
|---|---|---|
|6.0.0 r1|[android-6.0.0_r1.zip](http://files.hisano.jp/aosp/android-6.0.0_r1.zip)|MRA58K|
|5.1.0 r1|[android-5.1.0_r1.zip](http://files.hisano.jp/aosp/android-5.1.0_r1.zip)|LMY47D|
|5.0.1 r1|[android-5.0.1_r1.zip](http://files.hisano.jp/aosp/android-5.0.1_r1.zip)|LRX22C|
|4.4 r1|[android-4.4_r1.zip](http://files.hisano.jp/aosp/android-4.4_r1.zip)|KRT16M|
|4.3 r1.1|[android-4.3_r1.1.zip](http://files.hisano.jp/aosp/android-4.3_r1.1.zip)|JWR66Y|
|4.2.1 r1|[android-4.2.1_r1.zip](http://files.hisano.jp/aosp/android-4.2.1_r1.zip)|JOP40D|
|4.1.2 r1|[android-4.1.2_r1.zip](http://files.hisano.jp/aosp/android-4.1.2_r1.zip)|JZO54K|
|4.0.3 r1|[android-4.0.3_r1.zip](http://files.hisano.jp/aosp/android-4.0.3_r1.zip)|IML74K|
|2.3.4 r1|[android-2.3.4_r1.zip](http://files.hisano.jp/aosp/android-2.3.4_r1.zip)|GRJ22|

ファイルのダウンロード後、各ファイルごとに下記の手順でEclipseにインポートしてください。

1. Eclipseの[File - Import...]メニューから、"Existing Projects into Workspace"を選択
2. "Select archive file"フィールドに上記でタウンロードしたファイルを指定して"Finish"を押下

### :black_small_square:現在表示しているアクティビティから処理を調査(ソースコード調査の一例)

1. コマンドプロンプト等で"adb shell dumpsys activy top"を実行
2. 上記の出力結果のACTIVITY行から、現在表示しているアクティビティのクラス名を取得
    + 例) Nexus 5 + Android 6で電卓アプリを表示している時には、"ACTIVITY com.google.android.calculator/com.android.calculator2.Calculator f98e77c pid=5041"となるため、"/"以降の"Calculator"がクラス名 
3. [Navigate - Open Type...]メニューから"Calculator"クラスを開く
4. Eclipseのソースコードを辿る機能を利用して調査
    + [フィールドやメソッド一覧を表示(Quick Outline機能)](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2FgettingStarted%2Fqs-Quickviews.htm)
    + [クラス階層を表示(Quick Type Hierarchy機能)](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2FgettingStarted%2Fqs-6.htm)
    + [パンくずリストで同一パッケージのクラスを表示(Show in Breadcrumb機能)](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Freference%2Fref-java-editor-breadcrumb.htm)
    + [メソッド等の処理を表示(Open Declaration機能)](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2FgettingStarted%2Fqs-Navigate.htm)
    + [上記以外の検索機能](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftips%2Fjdt_tips.html&anchor=searching_section)

### :black_small_square:特定アプリの起点となるアクティビティやサービスから調査

1. コマンドプロンプト等で"adb shell pm list packages"で、インストールされているアプリのアプリ名を調査
2. コマンドプロンプト等で"adb shell dumpsys package [アプリ名]"を実行して、アクティビティ・サービス・インテントフィルタ等のクラス名を調査
    + AOSPの各アプリのAndroidManifest.xml等や、[apktool](http://ibotpeaches.github.io/Apktool/)での調査も可能
3. 前項の手順でソースコードを調査

### :black_small_square:Nexus端末を用いた実機デバッグ

1. [Nexus端末向けのファームウェア](https://developers.google.com/android/nexus/images)をダウンロード
2. 上記を解凍した後、さらに"image-<端末コード名>-<バージョン>.zip"を解凍してboot.imgを取得
3. boot.imgを[bootimg](http://dark-cyanide-devs.blogspot.jp/2015/01/port-bootimg.html)等でアンパックして、"/default.prop"ファイルの"ro.debuggable=0"を"ro.debuggable=1"に変更した後にリパック
    + アンパック例) bootimg.exe --unpack-bootimg
    + リパック例) bootimg.exe --repack-bootimg
4. 上記でリパックしたboot.imgをfastbootコマンドで書き込み
    + ブートローダへの移行例) adb reboot bootloader
    + 書き込み例) fastboot flash boot boot-new.img
5. 端末を再起動
    + 再起動例) fastboot reboot
6. ADT Plugin for Eclipse又はDDMSの"Devices"ビューでデバッグ対象のプロセスを選択(デバッガ接続用の8700ポートをオープン)
7. [Run - Debug Configurations...]メニューから"Remote Java Application - Debug Selected Process in 'Devices' View with 'android-2.3.4_r1' Project"を選択して"Debug"ボタンを押下

### :black_small_square:リアルタイムにシステムサービスの呼び出されているメソッドを調査

1. ADT Plugin for Eclipse又はDDMSの"Devices"ビューで"system_process"プロセスを選択(デバッガ接続用の8700ポートをオープン)
2. ShowCalledMethodsOfSystemServicesクラスを実行

## [AOSP](http://source.android.com/source/requirements.html)をビルドした後、AOSP Research Tookitのツールを用いて、Eclipseプロジェクトを生成して利用

TBD
