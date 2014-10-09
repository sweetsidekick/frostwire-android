FrostWire for Android
============
**A file sharing client, media player and simple file manager for your Android devices.**

FrostWire for Android will let you search and download files from the BitTorrent network and several popular cloud services and it will let you share files with other FrostWire for Android devices on the same local network (via WiFi)

The local sharing happens through a simple JSON/HTTP protocol (still to be documented, you can look at how it works on the code for now, very very simple)

![](http://i.imgur.com/U20h8cL.png)
- - -
Coding guidelines
============

- **Keep it simple**.

- **Do not repeat yourself**. Re-use your own code and our code. It'll be faster to code, and easier to maintain.

- If you want to help, [Issue tracker](https://github.com/frostwire/frostwire-android/issues) is a good place to take a look at.

- Try to follow our coding style and formatting before submitting a patch.
 
- All pull requests should come from a feature branch created on your git fork. We'll review your code and will only merge it to the `master` branch if it doesn't break the build. If you can include tests for your pull request you get extra bonus points ;)

- When you submit a pull request try to explain what issue you're fixing in detail and how you're fixing in detail it so it's easier for us to read your patches.
  If it's too hard to explain what you're doing, you're probably making things more complex than they already are.
  Look and test your code well before submitting patches.

- We prefer well named methods and code re-usability than a lot of comments. Code should be self-explanatory.

Becoming a core collaborator with direct commit access to the upstream repository will only happen after we have received lots of great patches and we get to know you better.


Tip for commit
---------------
You can donate for development, thereby encouraging some developers or you can participate and get a tip for commits approved us.

[![tip for next commit](https://tip4commit.com/projects/200.svg)](https://tip4commit.com/github/frostwire/frostwire-android)

Build instructions
============

Eclipse (temporary)
------------------
As of 1.3.8 (build 149), this might be the last release we build the project using Eclipse/Android Development Toolkit Bundle.

1. Make sure you have enough heap available for Eclipse.
2. Before you launch Eclipse, modify eclipse.ini to something along these lines:

        -XX:MaxPermSize=256m
        -Xms40m
        -Xmx2048m


3. Open the project in eclipse, and build it using the Wizard. Should work out of the box.
![building with eclipse android tools](https://cloud.githubusercontent.com/assets/163977/4533475/57d32b66-4d9c-11e4-9dee-96e1f233ebfc.png)

*We are currently doing major changes to our project structure and we'll be using [***gradle***](http://www.gradle.org/) and [***Android Studio***](https://developer.android.com/sdk/installing/studio.html) so these build instructions won't be valid for long*

Happy coding.

Download:
============

[Latest binaries](http://www.frostwire.com/android) | [Previous versions (SourceForge)](https://sourceforge.net/projects/frostwire-android/files/)

**Downloading FrostWire does not constitute permission or a license for obtaining or distributing unauthorized files. It is illegal for you to distribute copyrighted files without permission.**

If you want to know about legal content you can download and distribute legally please visit [FrostClick](http://frostclick.com), [VODO](http://vodo.net), ClearBits.net and [Creative Commons](http://creativecommons.org)

