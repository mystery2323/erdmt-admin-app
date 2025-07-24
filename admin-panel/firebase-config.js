
const firebaseConfig = {
  apiKey: "AIzaSyAF3DcJieHh9Ik9pxbWGK1rRjLgDSkY82U",
  authDomain: "radiant-works-461812-q5.firebaseapp.com",
  databaseURL: "https://radiant-works-461812-q5-default-rtdb.firebaseio.com",
  projectId: "radiant-works-461812-q5",
  storageBucket: "radiant-works-461812-q5.firebasestorage.app",
  messagingSenderId: "690941744376",
  appId: "1:690941744376:web:59c11a11300573ed4c170d",
  measurementId: "G-CSEVE7TQVN"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Firebase services
const auth = firebase.auth();
const database = firebase.database();
const storage = firebase.storage();

// Global references
let currentUser = null;
let devicesRef = database.ref('devices');
let logsRef = database.ref('logs');
