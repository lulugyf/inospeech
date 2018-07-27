When change develop computer, following file need to be modified:
1. local.properties
      specify the android-sdk location
2. app/build.gradle
      change the value of "compileSdkVersion" and "buildToolsVersion"


** Adding udev rules for USB debugging Android devices:
  >lsusb
Bus 001 Device 008: ID 18d1:d002 Google Inc.
 >sudo vi /etc/udev/rules.d/51-android.rules
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", ATTR{idProduct}=="d002", MODE="0660", GROUP="plugdev", SYMLINK+="android%n"
 >sudo usermod -aG plugdev


gradle global config proxy in ~/.gradle/gradle.properties
         systemProp.http.proxyHost=127.0.0.1
         systemProp.http.proxyPort=8080
         systemProp.https.proxyHost=127.0.0.1
         systemProp.https.proxyPort=8080
   it seems that gradle only support http-proxy, no socks support

urllib.parse.urlencode

appid: 1000000383
appkey: qjkqDw3PXoacHrpahEL2OSDKgeiExZfu

?AppId=1000000383&AppKey=qjkqDw3PXoacHrpahEL2OSDKgeiExZfu

https://www.inoreader.com/reader/api/0/user-info

https://www.inoreader.com/reader/api/0/tag/list

https://www.inoreader.com/reader/api/0/unread-count
https://www.inoreader.com/reader/api/0/subscription/list



"id":"feed\/http:\/\/feeds.feedburner.com\/letscorp\/aDmw"
"id":"feed\/http:\/\/feeds2.feedburner.com\/programthink"
"id":"feed\/http:\/\/feeds.kenengba.com\/kenengbarss"
"id":"feed\/http:\/\/cnpolitics.org\/feed\/"
"id":"feed\/http:\/\/feeds.feedburner.com\/chinadigitaltimes\/IyPt"
"id":"feed\/http:\/\/botanwang.com\/rss.xml"

https://www.inoreader.com/reader/api/0/stream/contents/feed%2Fhttp:%2F%2Fbotanwang.com%2Frss.xml

https://www.inoreader.com/reader/api/0/stream/contents/user%2F-%2Fstate%2Fcom.google%2Freading-list
https://www.inoreader.com/reader/api/0/stream/contents/user%2F-%2Fstate%2Fcom.google%2Fread
