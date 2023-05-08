#include "esp_camera.h"
#include <WiFi.h>
#include<Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#include<base64.h>
#define CAMERA_MODEL_AI_THINKER // Has PSRAM

#include "camera_pins.h"

// ===========================
// Enter your WiFi credentials
// ===========================
const char* ssid = "***********";
const char* password = "*********";
// ================================
// Enter your Firebase credentials
// ================================
const char* api_key =  "******* Enter your Api Key here*****";
const char* database_url = "******* Enter your Api Key here*****";

int trigPin = 4;
int echoPin = 2;
int duration;
int distance;
FirebaseData fdbo;
FirebaseAuth auth;
FirebaseConfig config2;
unsigned long sendDataPrevMillis = 0;
bool signupOk = false;
void startCameraServer();
void setupLedFlash(int pin);

void setup() {
  Serial.begin(115200);
  Serial.setDebugOutput(true);
  Serial.println();

  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.frame_size = FRAMESIZE_UXGA;
  config.pixel_format = PIXFORMAT_JPEG; // for streaming
  //config.pixel_format = PIXFORMAT_RGB565; // for face detection/recognition
  config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.jpeg_quality = 12;
  config.fb_count = 1;
  
  // if PSRAM IC present, init with UXGA resolution and higher JPEG quality
  //                      for larger pre-allocated frame buffer.
  if(config.pixel_format == PIXFORMAT_JPEG){
    if(psramFound()){
      config.jpeg_quality = 10;
      config.fb_count = 2;
      config.grab_mode = CAMERA_GRAB_LATEST;
    } else {
      // Limit the frame size when PSRAM is not available
      config.frame_size = FRAMESIZE_SVGA;
      config.fb_location = CAMERA_FB_IN_DRAM;
    }
  } else {
    // Best option for face detection/recognition
    config.frame_size = FRAMESIZE_240X240;
#if CONFIG_IDF_TARGET_ESP32S3
    config.fb_count = 2;
#endif
  }

#if defined(CAMERA_MODEL_ESP_EYE)
  pinMode(13, INPUT_PULLUP);
  pinMode(14, INPUT_PULLUP);
#endif

  // camera init
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x", err);
    return;
  }

  sensor_t * s = esp_camera_sensor_get();
  // initial sensors are flipped vertically and colors are a bit saturated
  if (s->id.PID == OV3660_PID) {
    s->set_vflip(s, 1); // flip it back
    s->set_brightness(s, 1); // up the brightness just a bit
    s->set_saturation(s, -2); // lower the saturation
  }
  // drop down frame size for higher initial frame rate
  if(config.pixel_format == PIXFORMAT_JPEG){
    s->set_framesize(s, FRAMESIZE_QVGA);
  }

#if defined(CAMERA_MODEL_M5STACK_WIDE) || defined(CAMERA_MODEL_M5STACK_ESP32CAM)
  s->set_vflip(s, 1);
  s->set_hmirror(s, 1);
#endif

#if defined(CAMERA_MODEL_ESP32S3_EYE)
  s->set_vflip(s, 1);
#endif

// Setup LED FLash if LED pin is defined in camera_pins.h
#if defined(LED_GPIO_NUM)
  setupLedFlash(LED_GPIO_NUM);
#endif

  WiFi.begin(ssid, password);
  WiFi.setSleep(false);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected");

  startCameraServer();

  Serial.print("Camera Ready! Use 'http://");
  Serial.print(WiFi.localIP());
  Serial.println("' to connect");
  Serial.println();
  config2.api_key  =api_key;
  config2.database_url = database_url;
  if(Firebase.signUp(&config2,&auth,"","")){
    Serial.println("SignUp ok");
    signupOk = true;
  }else{
    Serial.println(config2.signer.signupError.message.c_str());
  }
  config2.token_status_callback = tokenStatusCallback;
  Firebase.begin(&config2,&auth);
  Firebase.reconnectWiFi(true);
  pinMode(trigPin,OUTPUT);
  pinMode(echoPin,INPUT);
}

void loop() {
  // Do nothing. Everything is done in another task by the web server
  digitalWrite(trigPin,LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin,HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin,LOW);
  duration = pulseIn(echoPin,HIGH);
  distance = duration*0.034/2;
  Serial.println(distance);
  if(distance<100){
    if(Firebase.ready()&& signupOk && (millis() - sendDataPrevMillis > 5000  || sendDataPrevMillis == 0)){
      sendDataPrevMillis = millis();
      // Data = distance;
    }
    FirebaseJson json2;
    String jsonData = "{\"photo\":\"" + Photo2Base64() + "\"}";
    json2.set("Photo",Photo2Base64());
    String photoPath = "/esp32-cam";
    if (Firebase.RTDB.setJSON(&fdbo,"/esp32-cam",&json2)) {
      Serial.println(fdbo.dataPath());
      Serial.println("Successfully saved to: " + fdbo.dataPath());
    } else {
      Serial.println(fdbo.errorReason());
    }
  }
  delay(1000);
}
String Photo2Base64() {
    camera_fb_t * fb = NULL;
    sensor_t * sensor = esp_camera_sensor_get();
    sensor->set_framesize(sensor, FRAMESIZE_SXGA); // set frame size to 1280x960
    fb = esp_camera_fb_get();   
    if(!fb) {
      Serial.println("Camera capture failed");
      return "";
    }
  
    String imageFile = "data:image/jpeg;base64";
    String encrypt = base64::encode(fb->buf,fb->len);

    esp_camera_fb_return(fb);
    
    return encrypt;
}

