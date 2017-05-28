# Android Things voice assistant

Simple voice assistant based on Android Things and Raspberry Pi 3. It uses [Pocketsphinx](https://github.com/cmusphinx/pocketsphinx-android) for wakeword recognition, [Yandex SpeechKit Mobile SDK](https://tech.yandex.ru/speechkit/mobilesdk/) for speech recognition and [Api.ai Android SDK](https://github.com/api-ai/apiai-android-client) for natural language understanding. It understands **only Russian language.**

### Abilities:
  - play music from Spotify;
  - manage tasks from Todoist;
  - show images from Bing image search;
  - tell the weather from Open Weather;
  - recognize and remember RFID tags;
  - make Wikipedia queries;
  - translate from Russian to English or Chinese;
  - GPIO, turn on/off colors of the LED.

### Video demonstration (English subs available):
[![Android Things voice assistant](https://i.ytimg.com/vi_webp/0Jb0Qh_W2WQ/sddefault.webp)](http://www.youtube.com/watch?v=0Jb0Qh_W2WQ)

### Usage
To use it, you need Android Things(>= v.0.2) to be installed on your Rpi. Then connect microphone (you can use USB mic), speakers, monitor and if you want to use LED or RFID scanner, connect it like on the image below. **When you first start, you will need a mouse to give a few permissions to the app.** Then you have to add your API keys in [**Config.java**](https://github.com/Mkryglikov/smart/blob/master/app/src/main/java/ru/mkryglikov/smart/Config.java)

<img width="460" height="894" src="https://preview.ibb.co/iSnOFv/Smart_bb.png">
