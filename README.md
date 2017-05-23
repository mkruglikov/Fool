# Android Things voice assistant

Voice assistant based on Android Things and Raspberry Pi 3. It uses [Pocketsphinx](https://github.com/cmusphinx/pocketsphinx-android) for wakeword recognition, [Yandex SpeechKit Mobile SDK](https://tech.yandex.ru/speechkit/mobilesdk/) for speech recognition and [Api.ai Android SDK](https://github.com/api-ai/apiai-android-client) for natural language understanding. It understands **only Russian language.**

### Abilities:
  - play music from Spotify;
  - manage tasks from Todoist;
  - show images from Bing image search;
  - tell the weather from Open Weather;
  - recognize and remember RFID tags;
  - make Wikipedia queries;
  - GPIO, turn on/off colors of the LED.

### Video demonstration:
[![Android Things voice assistant](https://i.ytimg.com/vi_webp/0Jb0Qh_W2WQ/sddefault.webp)](http://www.youtube.com/watch?v=0Jb0Qh_W2WQ)

### Usage
To use it, you need Android Things(>= v.0.2) to be installed on your Rpi. Then connect microphone (you can use USB mic), speakers, monitor and if you want to use LED or RFID scanner, connect wires like on the image below. **When you first start, you will need a mouse to give some of the a permissions to the app.** Then you have to add your API keys in **Config.java**

<img width="460" height="894" src="https://psv4.userapi.com/c816525/u139483659/docs/beed467d1234/Smart_bb.png?extra=xmClxK6tXvbkqYCeZN9ykUNQr52w62Pp7tZpedGYdMNeFapgs4KFPQSNB8fp1kb_ivx6gU8WXJB81yjEqVK1YybJuEI2F_1lZPHwPdsOgRqMj6THamy0c5DJvA">
