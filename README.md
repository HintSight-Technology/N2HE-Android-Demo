# N2HE Android Demo

This demo is built to demonstrate the use of N2HE on Logistic Regression and Facial Verification, in Android. The various security parameters can be modified using setter methods in `Parameters` class.


## Prerequisites

- Compile SDK >= 33.0
- Java >= 11 


## Installation
1. In your terminal, run  
```
git clone https://github.com/HintSight-Technology/N2HE-Android-Demo.git
```

2. Download [**icrsv1_xnnpack_fp32.pte**](https://hintsightfhe-my.sharepoint.com/:u:/g/personal/kaiwen_hintsight_com/EUbp22n-fz9LjOjbEBClxnQBEF5D4UjIGGIg9R5ygdmcPg?e=3Uoyye) model file into N2HE-Android-Demo/app/src/main/assets directory. \
Download [**libexecutorch.so**](https://hintsightfhe-my.sharepoint.com/:u:/g/personal/kaiwen_hintsight_com/EaA-ZADfijBMof305iGoBSkBvVdar9XOMOSGIxLSVYUCzg?e=wzl3sB) file into N2HE-Android-Demo/app/src/main/jniLibs/arm64-v8a directory.

3. Replace <SERVER_GET_URL> and <SERVER_POST_URL> with respectively server urls in both **LogisticRegressions.java** and **FacialVerification.java**.

4. Select **pixel 6a API 35 arm64 v8a** as the emulator and run in Android Studio. 

