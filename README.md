# FastReader

![FastReader](https://i.imgur.com/bzR0Oz1.gif)

FastReader is a rapid serial visual presentation [RSVP](http://en.wikipedia.org/wiki/Rapid_serial_visual_presentation) speed reader for Android 7.1+ (API 25). I forked this from [Glance-Android](https://github.com/OnlyInAmerica/GlanceReader) to learn about Android and ended up rewriting the whole app. This is my first mobile app and I actually use it on a daily basis.

Share URLs to FastReader from any application.

## Building

0. Make sure you've installed the following from the Android SDK Manager before building:
  	+ Android SDK Build-tools 26.0.1
	+ Android SDK tools 26.0
	+ SDK Platform 25
	+ Android Support Repository 47

1. Define the `ANDROID_HOME` environmental variable as your Android SDK location.

	If you need help check out [this guide](http://spring.io/guides/gs/android/).

3. Build!

  	To build an .apk from this directory, make sure `./gradlew` is executable and run:

    	$ ./gradlew assemble

	The resulting .apk will be availble in `./app/build/apk`.


## Thanks
Thanks to all the Glance contributors!

+ [Rich Jones](https://github.com/miserlou)
+ [andrewgiang](https://github.com/andrewgiang)
+ [defer](https://github.com/defer)
+ [psiegman](https://github.com/psiegman) (LGPL)
+ [rcarlsen](https://github.com/rcarlsen)
+ [mneimsky](https://github.com/mneimsky)

## License

GPLv3
