# FrostWire for Android.

[![tip for next commit](https://tip4commit.com/projects/200.svg)](https://tip4commit.com/github/frostwire/frostwire-android)

## ABOUT

A file sharing client, media player and simple file manager for your Android devices.

FrostWire for Android will let you search and download files from the BitTorrent network and several popular cloud services and it will let you share files with other FrostWire for Android devices on the same local network (via WiFi)

The local sharing happens through a simple JSON/HTTP protocol (still to be documented, you can look at how it works on the code for now, very very simple)

We'll put the specs of this protocol out for you to implement clients in your favorite platform, this way you will be able to browse, stream media or download files that FrostWire for Android users have decided to share on the local network.

FrostWire for Android also features a simple media player that we'll be evolving with future releases.

**Downloading FrostWire does not constitute permission or a license for obtaining or distributing unauthorized files. It is illegal for you to distribute copyrighted files without permission.**

If you want to know about legal content you can download and distribute legally please visit FrostClick.com, Vodo.net, ClearBits.net and CreativeCommons.org

## HACKING GUIDELINES

- **Keep it simple**.

- **Do not repeat yourself**. Re-use your own code and our code. It'll be faster to code, and easier to maintain.

- Looking at the Issue tracker (https://github.com/frostwire/frostwire-android/issues) is a good place to start if you want to help.

- Try to mimic our coding style and formatting before you submit a patch.

- When you submit a patch, try to explain what issue you're fixing in detail and how you're fixing in detail it so it's easier for us to read your patches.
  If it's too hard to explain what you're doing, you're probably making things more complex than they already are.
  Look and test your code well before submitting patches.

- We prefer well named methods and code re-usability than a lot of comments. Code should be self-explanatory.

Becoming a collaborator will only happen after we have received lots of great patches and we get to know you better.

## BUILD INSTRUCTIONS

### On Eclipse (Temporary)

As of 1.3.8 (build 149), this might be the last release we build the project using Eclipse/Android Development Toolkit Bundle.

1. Make sure you have enough heap available for eclipse, before you launch eclipse modify eclipse.ini to something along these lines:

```
-XX:MaxPermSize=256m
-Xms40m
-Xmx2048m
```
1. Open the project in eclipse, and build it using the Wizard. Should work out of the box.
![building with eclipse android tools](https://cloud.githubusercontent.com/assets/163977/4533475/57d32b66-4d9c-11e4-9dee-96e1f233ebfc.png)

*We are currently doing major changes to our project structure and we'll be using [***gradle***](http://www.gradle.org/) and [***Android Studio***](https://developer.android.com/sdk/installing/studio.html) so these build instructions won't be valid for long*

Happy Hacking.
[![tip for next commit](https://tip4commit.com/projects/200.svg)](https://tip4commit.com/github/frostwire/frostwire-android)
