# ISAC: Impaired Sight Audio Companion

ISAC is a project that uses an ESP32-CAM module and an ultrasonic sensor to capture images and detect objects present in the images using TensorFlow Lite object detection library. The detected objects' names are converted to speech using text-to-speech and can be heard by visually challenged individuals through an Android application.

## Components

- ESP32-CAM module
- Ultrasonic sensor
- Firebase server
- Android application
- TensorFlow Lite object detection library

## Functionality

The system operates by continuously measuring the distance using the ultrasonic sensor. If the distance measured is less than 100, the ESP32-CAM module captures an image, which is then converted to base64 format and sent to the Firebase server as a JSON file. The Android application continuously monitors the Firebase Realtime Database and fetches the data whenever a new JSON file is added to the database. The fetched data is displayed on the texture view and then analyzed using the TensorFlow Lite object detection library to detect objects present in the image. The detected object's names are then converted to speech using text-to-speech and can be heard by visually challenged individuals through the Android application.

## Installation and Configuration

To use this project, follow these steps:

1. Connect the ESP32-CAM module and ultrasonic sensor to your development board.
2. Clone the project repository and upload the code to your development board.
3. Set up a Firebase account and create a new Realtime Database.
4. Configure the ESP32-CAM module to connect to the internet and the Firebase Realtime Database.
5. Install the Android application on your device and configure it to connect to the Firebase Realtime Database.
6. Install the TensorFlow Lite object detection library and ensure that it is properly configured in your Android application.

## Screenshots

Include circuit diagram and Android app screenshots here.

## Contributors

This project was created by [AbhishekPSingh07](https://github.com/AbhishekPSingh07). Contributions to the project are welcome and appreciated.


