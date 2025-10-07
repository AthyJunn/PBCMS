#include <WiFi.h>
#include <FirebaseESP32.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include "DHT.h"
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <time.h>

const char* WIFI_SSID_1 = "OPPO Reno8 T 5G";
const char* WIFI_PASSWORD_1 = "m8375iq5";
const char* WIFI_SSID_2 = "58F2A3-Maxis Fibre Internet";
const char* WIFI_PASSWORD_2 = "18A3358392";

#define API_KEY "AIzaSyAuJjF7dME30MFvqMLxfgXZeG19rGnMFak"
#define DATABASE_URL "https://pbcms-c955a-default-rtdb.firebaseio.com/"
#define USER_EMAIL "device@iot.com"
#define USER_PASSWORD "device1234"

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// Pins
#define DS18B20_PIN 5
#define DHT_PIN 4
#define DHT_TYPE DHT22
#define REED_PIN 14

// Temperature Icon (thermometer)
const unsigned char temp_icon_8x8 [] PROGMEM = {
  0b00110000,
  0b00110000,
  0b00110000,
  0b00110000,
  0b00110000,
  0b01111000,
  0b01111000,
  0b00110000
};

// Humidity Icon (water drop)
const unsigned char humidity_icon_8x8 [] PROGMEM = {
  0b00010000,
  0b00111000,
  0b01111100,
  0b01111100,
  0b01111100,
  0b00111000,
  0b00010000,
  0b00000000
};

// Lock Icon (closed padlock)
const unsigned char lock_icon_8x8 [] PROGMEM = {
  0b01111000,
  0b10000100,
  0b10110100,
  0b10110100,
  0b10110100,
  0b10000100,
  0b01111000,
  0b00000000
};

// Unlock Icon (open padlock)
const unsigned char unlock_icon_8x8 [] PROGMEM = {
  0b01111000,
  0b10000100,
  0b10010100,
  0b10100100,
  0b10100100,
  0b10000100,
  0b01111000,
  0b00000000
};

// OLED display
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

OneWire oneWire(DS18B20_PIN);
DallasTemperature sensors(&oneWire);
DHT dht(DHT_PIN, DHT_TYPE);

// NTP for timestamp
const char* ntpServer = "pool.ntp.org";
const long gmtOffset_sec = 8 * 3600;  // GMT+8 (Malaysia)
const int daylightOffset_sec = 0;

void connectWiFi() {
  Serial.println("Connecting to Wi-Fi...");
  WiFi.disconnect(true);
  delay(100);
  WiFi.begin(WIFI_SSID_1, WIFI_PASSWORD_1);
  unsigned long startAttemptTime = millis();

  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < 10000) {
    Serial.print(".");
    delay(500);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("\nConnected to Wi-Fi: %s\n", WIFI_SSID_1);
    return;
  }

  WiFi.disconnect(true);
  delay(100);
  WiFi.begin(WIFI_SSID_2, WIFI_PASSWORD_2);
  startAttemptTime = millis();

  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < 10000) {
    Serial.print(".");
    delay(500);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("\nConnected to Wi-Fi: %s\n", WIFI_SSID_2);
  } else {
    Serial.println("\n❌ Failed to connect to any WiFi");
  }
}

void initTime() {
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
}

void waitForTime() {
  struct tm timeinfo;
  while (!getLocalTime(&timeinfo)) {
    Serial.println("Waiting for NTP time...");
    delay(1000);
  }
  Serial.println("✅ NTP time obtained");
}

String getTimestampKey() {
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) return "unknown";
  char buffer[20];
  strftime(buffer, sizeof(buffer), "%Y-%m-%d_%H-%M", &timeinfo);
  return String(buffer);
}

void setup() {
  Serial.begin(115200);
  connectWiFi();

  // OLED display init
  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println(F("SSD1306 allocation failed"));
    while (true); // halt
  }

  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.println("Initializing...");
  display.display();
  delay(1000);

  // Firebase
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Sensors
  sensors.begin();
  dht.begin();
  pinMode(REED_PIN, INPUT_PULLUP);

  // Time
  initTime();
  waitForTime();
}

void updateDisplay(float tempInside, float humidity, bool doorClosed) {
  display.clearDisplay();

  // Text
  display.setCursor(0, 0);
  display.print("Temp: ");
  display.print(tempInside, 1);
  display.println(" C");

  display.setCursor(0, 20);
  display.print("Humidity: ");
  display.print(humidity, 1);
  display.println(" %");

  display.setCursor(0, 40);
  display.print("Door: ");
  display.println(doorClosed ? "CLOSE" : "OPEN");

  // Icons
  // Draw temperature icon at (110, 0)
display.drawBitmap(110, 0, temp_icon_8x8, 8, 8, SSD1306_WHITE);

// Draw humidity icon at (110, 20)
display.drawBitmap(110, 20, humidity_icon_8x8, 8, 8, SSD1306_WHITE);

// Draw door status icon at (110, 40)
if (doorClosed) {
  display.drawBitmap(110, 40, lock_icon_8x8, 8, 8, SSD1306_WHITE);
} else {
  display.drawBitmap(110, 40, unlock_icon_8x8, 8, 8, SSD1306_WHITE);
}

  display.display();
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("Wi-Fi lost. Reconnecting...");
    connectWiFi();
  }

  sensors.requestTemperatures();
  float tempDS18B20 = sensors.getTempCByIndex(0);
  float tempDHT22 = dht.readTemperature();
  float humidity = dht.readHumidity();

  if (isnan(tempDS18B20)) tempDS18B20 = 0.0;
  if (isnan(tempDHT22)) tempDHT22 = 0.0;
  if (isnan(humidity)) humidity = 0.0;

  String doorStatus = (digitalRead(REED_PIN) == LOW) ? "CLOSE" : "OPEN";
  bool doorClosed = (doorStatus == "CLOSE");
  String wifiStatus = (WiFi.status() == WL_CONNECTED) ? "connected" : "disconnected";

  String timestampKey = getTimestampKey();

  Serial.printf("🌡 Inside Temp: %.2f °C | Humidity: %.2f %% | Door: %s | Key: %s\n",
                tempDS18B20, humidity, doorStatus.c_str(), timestampKey.c_str());

  if (Firebase.ready()) {
    // Update current values
    Firebase.setFloat(fbdo, "/sensors/temperature", tempDS18B20);
    Firebase.setFloat(fbdo, "/sensors/humidity", humidity);
    Firebase.setString(fbdo, "/sensors/door_status", doorStatus);
    Firebase.setString(fbdo, "/wifiStatus", wifiStatus);

    // Update history
    if(timestampKey != "unknown") {
      FirebaseJson json;
      json.set("temperature", tempDS18B20);
      json.set("humidity", humidity);
      json.set("door_status", doorStatus);
      Firebase.setJSON(fbdo, "/sensors/history/" + timestampKey, json);
    }
  }

  updateDisplay(tempDS18B20, humidity, doorClosed);

  delay(5000);  // 5 seconds
}