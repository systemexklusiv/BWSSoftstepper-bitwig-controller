Bitwig Studio Softstepper 

Written as Java extension

Motivation

I wanted to have a looper in Bitwig where I can control a bunch of clips, arm tracks, delete fails, mute/unmute in a way one can easy record different versions with all the crucial tasks controlled with the feet. I couldn’t find a script which fit to my needs therefore I came up with this one. I hope you will enjoy it :-)

To me as Java-Programmer it was great news that one can code controller scripts in Java in Bitwig! Also Bitwig opposed to Ableton Live made a console in their Host Software and opened a DEBUG_PORT to make coding feel as it should be. With the help of master Moßgraber it was like a breeze to get up and running funky java code in the Bitwig ecosystem yay :-D.  I always wanted to have a good responsive tailored script for the nice Softstep controllers therefore I came up with this script.

Check out this video
https://youtu.be/jUpwnU9GnyM

Installation
1. Get the latest release
1. Copy that ﬁle in the following location depending on your OS:
Windows: %USERPROFILE%\Documents\Bitwig Studio\Extensions\
Mac: ~/Documents/Bitwig Studio/Extensions/
Linux: ~/Bitwig Studio/Extensions/
2. Since Bitwig Studio 4.3 you can simply drag and drop the ﬁle on it’s main window and the ﬁle will be automatically copied in the correct folder!
2. Get the softstep- preset which is needed to for the control script and load it up to the controller in standalone-mode vie SoftStep Editors
1. Go here and install SoftStep Editors https://www.keithmcmillen.com/downloads/
2. import the preset BWSSoftstepper, put it in the setlist of the editor and click on “Send To Softstep”. You should see its Name “BWS” on the display-
3. Open Bitwig Studio and open the Dashboard. Select Settings and Controllers.
click on Add controller, select the manufacturer, Keith McMillen and the controller Softstep and click on Add.
IMPORTANT: note that the extension does not start until you configured the necessary in- and outputs!


Manual and everyting is in the release Zip

Big Thanks to Jürgen Moßgraber
( https://mossgrabers.de/ ) for making amazing software and tutorials
Thanks to Nikolaus Gradwohl who wrote a nice JS script for the Softstep which helped me a lot.
Here he wrote about it
https://www.local-guru.net/blog/2018/11/30/Bitwig-Studio-Controllerscript-for-SoftStep2-
-----------
Checkout my music and channel here

https://www.youtube.com/user/systemexklusiv/videos
https://www.facebook.com/david.rival.10297/
https://www.instagram.com/systemexklusiv/

Feel free to contact me! Have fun :-)