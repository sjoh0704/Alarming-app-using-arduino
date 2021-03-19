# Collision alarming app using arduino
## Collision alarming service
- SW-420 진동센서에서 충돌 트리거를 발생시킨후 HC-06 bluetooth sensor를 이용하여 안드로이드 앱으로 전송
- 앱에서는 센서의 트리거의 위험수준을 판별하여 위험 수준이 일정 수준 이상일때 미리 저장된 연락망으로 SMS전송
- 부가적으로 앱에서 블랙박스 기능을 탑재하여 녹화된 영상을 구글 스토리지(Firebase)에 자동 저장 
</br>


> ## Block Diagram

![image](https://user-images.githubusercontent.com/66519046/110231008-5a135c80-7f58-11eb-813c-28f8c5f55635.png)

</br>


> ##  Screen

### 기본 스크린
![image](https://user-images.githubusercontent.com/66519046/111751664-1b768e00-88d8-11eb-9374-b07fc63b88a4.png)

</br>

### 연락처 저장 
![image](https://user-images.githubusercontent.com/66519046/111751838-4c56c300-88d8-11eb-98c0-c6e992e8621d.png)


</br>

### 블랙박스 이용
![image](https://user-images.githubusercontent.com/66519046/111752773-7492f180-88d9-11eb-87b3-6e5a05e80edd.png)


</br>

### 메시지 전송
![image](https://user-images.githubusercontent.com/66519046/111751008-4ad8cb00-88d7-11eb-9213-7297330fcb9e.png)


> ## 회로 설계도  
![image](https://user-images.githubusercontent.com/66519046/111749308-ecaae880-88d4-11eb-83a7-670c4f7a69f4.png)  
< Connection Arduino Uno R3 with SW-420 Vibration Sensor >
</br>

![image](https://user-images.githubusercontent.com/66519046/111750079-1b758e80-88d6-11eb-918e-63352e7973f4.png)

</br>

![image](https://user-images.githubusercontent.com/66519046/111749970-e36e4b80-88d5-11eb-96c7-31eab0213647.png)   
< Connection Arduino Uno R3 with HC-06 Bluetooth Module >
</br>

![image](https://user-images.githubusercontent.com/66519046/111750230-5081e100-88d6-11eb-804f-25bba5d42a94.png)
</br>





