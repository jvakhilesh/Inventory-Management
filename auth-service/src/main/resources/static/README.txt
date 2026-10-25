SecurePortal - minimal email OTP frontend integration

This package preserves the supplied frontend pages and existing styling/features.
Only signup.html, js/auth.js, and css/style.css were modified to add email OTP verification.
index.html, login.html, and home.html are copied unchanged from the supplied files.

Copy into: src/main/resources/static/
  index.html
  login.html
  signup.html
  home.html
  js/auth.js
  css/style.css

Manual signup sends otpChannel=EMAIL and whatsappNumber=null. After signup, the page
shows the OTP panel. POST /auth/verify-otp activates the account and sets auth cookies;
then the browser redirects to /home.html. Resend calls POST /auth/resend-otp.

Use Ctrl+F5 after copying to avoid cached JavaScript/CSS. The frontend has been syntax-checked,
but it has not been run against your local Spring Boot app in this environment.
