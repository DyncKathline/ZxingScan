# ZxingScan
QRCode.java文件中提供了6种生成二维码的样式，可直接按照如下方式使用。
```
qrcode1.setImageBitmap(QRCode.createQRCode("http://www.tmtpost.com/2536837.html"));
qrcode2.setImageBitmap(QRCode.createQRCodeWithLogo2("http://www.jianshu.com/users/4a4eb4feee62/latest_articles", 500, QRCode.drawableToBitmap(getResources().getDrawable(R.drawable.head))));
qrcode3.setImageBitmap(QRCode.createQRCodeWithLogo3("http://www.jianshu.com/users/4a4eb4feee62/latest_articles", 500, QRCode.drawableToBitmap(getResources().getDrawable(R.drawable.head))));
qrcode4.setImageBitmap(QRCode.createQRCodeWithLogo4("http://www.jianshu.com/users/4a4eb4feee62/latest_articles", 500, QRCode.drawableToBitmap(getResources().getDrawable(R.drawable.head))));
qrcode5.setImageBitmap(QRCode.createQRCodeWithLogo5("http://www.jianshu.com/users/4a4eb4feee62/latest_articles", 500, QRCode.drawableToBitmap(getResources().getDrawable(R.drawable.head))));
qrcode6.setImageBitmap(QRCode.createQRCodeWithLogo6("http://www.jianshu.com/users/4a4eb4feee62/latest_articles", 500, QRCode.drawableToBitmap(getResources().getDrawable(R.drawable.head))));
```
## 效果图
![image](https://raw.githubusercontent.com/DyncKathline/IJKPlayer-android/master/screenshot/GIF1.gif)
![image](https://raw.githubusercontent.com/DyncKathline/IJKPlayer-android/master/screenshot/GIF2.gif)
## barcode
1. 扫描速度比zxinglibrary大大提高速倍，可以和微信媲美，而且可以以任意比例进行预览。
2. 支持识别多个二维码和条形码。