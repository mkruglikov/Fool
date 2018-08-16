# Fool. Russian Android Things voice assistant

Simple voice assistant based on Android Things and Raspberry Pi 3. It uses [Pocketsphinx](https://github.com/cmusphinx/pocketsphinx-android) for wakeword recognition, [Yandex SpeechKit Mobile SDK](https://tech.yandex.ru/speechkit/mobilesdk/) for speech recognition and [Api.ai Android SDK](https://github.com/api-ai/apiai-android-client) for natural language understanding. 
It understands **only Russian language.**

## Abilities:
  - play music from Spotify;
  - manage tasks from Todoist;
  - show images from Bing image search;
  - tell the weather from Open Weather;
  - recognize and remember RFID tags;
  - make Wikipedia queries;
  - translate from Russian to English or Chinese;
  - GPIO, turn on/off colors of the LED.

## Video demonstration (English subs available):
[![Android Things voice assistant](https://i.ytimg.com/vi_webp/0Jb0Qh_W2WQ/sddefault.webp)](http://www.youtube.com/watch?v=0Jb0Qh_W2WQ)

## Usage
To use it, you need Android Things(>= v.0.2) to be installed on your RPi 3. Then connect a microphone (you can use USB mic), speakers, monitor and if you want to use LED or RFID scanner, connect it like shown on the image below.
**When you first start, you will need a mouse to give a few permissions to the app.**
Then you have to add your API keys in [**Config.java**](https://github.com/Mkryglikov/smart/blob/master/app/src/main/java/ru/mkryglikov/smart/Config.java)

<img width="320" height="622" src="https://preview.ibb.co/iSnOFv/Smart_bb.png">

## License
MIT License

Copyright (c) 2018 Maksim Kruglikov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
