// index.js
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json"); // make sure this file exists in functions/

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://pbcms-c955a-default-rtdb.firebaseio.com"
});

// Triggered when any sensor changes
exports.sendSensorAlerts = functions.database
  .ref("/sensors/{sensorType}")
  .onWrite(async (change, context) => {
    const sensorType = context.params.sensorType;
    const newValue = change.after.val();

    let title = "";
    let body = "";

    // 🚪 Door Status
    if (sensorType === "door_status" && newValue === "OPEN") {
      title = "Door Opened";
      body = "The chiller door has been opened.";
    }

    // 🌡 Temperature
    if (sensorType === "temperature") {
      if (newValue < 1 || newValue > 6) {
        title = "Temperature Warning";
        body = `Temperature is ${newValue}°C (outside safe range 1–6°C).`;
      }
      if (newValue < 0 || newValue > 8) {
        title = "Temperature Critical";
        body = `Critical temperature detected: ${newValue}°C! Immediate action required.`;
      }
    }

    // 💧 Humidity
    if (sensorType === "humidity") {
      if (newValue < 30 || newValue > 70) {
        title = "Humidity Warning";
        body = `Humidity is ${newValue}% (outside safe range 30–70%).`;
      }
      if (newValue < 25 || newValue > 75) {
        title = "Humidity Critical";
        body = `Critical humidity detected: ${newValue}%! Immediate action required.`;
      }
    }

    // 🚨 Send FCM + save to database
    if (title && body) {
      const message = {
        data: { title, body },
        topic: "alerts" // all devices subscribed to "alerts"
      };

      try {
        // Send notification
        await admin.messaging().send(message);
        console.log("✅ Notification sent:", message);

        // Save notification in Realtime Database
        const alertData = {
          title,
          message: body,
          timestamp: Date.now(),
          senderId: "71197206218",
          type: sensorType
        };

        await admin.database().ref("/notifications").push(alertData);
        console.log("✅ Notification saved to database");
      } catch (error) {
        console.error("❌ Error sending notification:", error);
      }
    }
  });

