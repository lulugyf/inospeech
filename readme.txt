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


{"subscriptions":[
{"id":"feed\/http:\/\/feeds.feedburner.com\/letscorp\/aDmw","title":"墙外楼","categories":[],"sortid":"00F99451","firstitemmsec":1489676764000000,"url":"http:\/\/feeds.feedburner.com\/letscorp\/aDmw","htmlUrl":"http:\/\/www.letscorp.net\/","iconUrl":"https:\/\/d1ys61kka3jf1i.cloudfront.net\/www.letscorp.net?w=16\u0026cs=2208400478"},
{"id":"feed\/http:\/\/feeds2.feedburner.com\/programthink","title":"编程随想的博客","categories":[],"sortid":"0364803F","firstitemmsec":1518443003026939,"url":"http:\/\/feeds2.feedburner.com\/programthink","htmlUrl":"http:\/\/program-think.blogspot.com\/","iconUrl":"https:\/\/d1ys61kka3jf1i.cloudfront.net\/program-think.blogspot.com?w=16\u0026cs=1509107791"},
{"id":"feed\/http:\/\/feeds.kenengba.com\/kenengbarss","title":"可能吧","categories":[],"sortid":"0236892B","firstitemmsec":1489676764000000,"url":"http:\/\/feeds.kenengba.com\/kenengbarss","htmlUrl":"https:\/\/kenengba.com\/","iconUrl":"https:\/\/d1ys61kka3jf1i.cloudfront.net\/kenengba.com?w=16\u0026cs=405325250"},
{"id":"feed\/http:\/\/cnpolitics.org\/feed\/","title":"政见 CNPolitics.org","categories":[],"sortid":"02368885","firstitemmsec":1489676764000000,"url":"http:\/\/cnpolitics.org\/feed\/","htmlUrl":"http:\/\/cnpolitics.org\/","iconUrl":"https:\/\/d1ys61kka3jf1i.cloudfront.net\/cnpolitics.org?w=16\u0026cs=2442232022"},
{"id":"feed\/http:\/\/feeds.feedburner.com\/chinadigitaltimes\/IyPt","title":"中国数字时代","categories":[],"sortid":"00F99453","firstitemmsec":1489676764000000,"url":"http:\/\/feeds.feedburner.com\/chinadigitaltimes\/IyPt","htmlUrl":"http:\/\/chinadigitaltimes.net\/chinese","iconUrl":"https:\/\/d1ys61kka3jf1i.cloudfront.net\/chinadigitaltimes.net?w=16\u0026cs=3519324717"},
{"id":"feed\/http:\/\/botanwang.com\/rss.xml","title":"博谈网","categories":[],"sortid":"0157F6A6","firstitemmsec":1489676764000000,"url":"http:\/\/botanwang.com\/rss.xml","htmlUrl":"https:\/\/botanwang.com\/","iconUrl":"https:\/\/d1ys61kka3jf1i.cloudfront.net\/botanwang.com?w=16\u0026cs=125542481"}]}