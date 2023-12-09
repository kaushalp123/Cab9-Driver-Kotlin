# Cab9 Driver App V3 Android
### POWERFUL, SIMPLE AND INNOVATIVE WAY TO MANAGE YOUR TAXI BUSINESS.
### Code Language
This project is developed in native Android using Kotlin language.
### Design Support
This app uses the AndroidX design support libraries.
### Architecture Design Pattern
We have used **Model-View-ViewModel(MVVM)** pattern for building this app.  
The reason we chose MVVP over other patterns is it provides an easy way to structure the project codes. 
MVVM is widely accepted design pattern since it provides modularity, testability, and a more clean and maintainable codebase.  

It is composed of the following three components:  
* **Model:** This layer is responsible for the abstraction of the data sources. Model and ViewModel work together to get and save the data.
* **View:** The purpose of this layer is to inform the ViewModel about the user’s action. This layer observes the ViewModel and does not contain any kind of application logic.
* **ViewModel:** It exposes those data streams which are relevant to the View. Moreover, it serves as a link between the Model and the View.

**Key Points of MVVM Architecture**:   
* ViewModel does not hold any kind of reference to the View.
* Many to-1 relationships exist between View and ViewModel.
* No triggering methods to update the View.

### App Performance and energy consumption
The location tracking(in Foreground and Background) is the core feature of the app, which has the most impact on device battery consumption and app performance.  
To make sure the app doesn't consume much battery and memory, we use the Google's location service API.  
Google service API has several options which helps the user to fetch the accurate location while still maintaining the battery efficiency.

**Battery Optmization**:  
For making sure that our app doesn't consume much battery, we have set different location request parameters depending on the situation for fetching the location using google services API.
* While driver is **Online**, we need the location frequently which is accurate and also battery efficient, so we use **PRIORITY_HIGH_ACCURACY** value as an argument to setPriority() method of locationRequest object and we fetch the accurate location every 5 seconds by passing the value **5** to setInterval() method of the locationRequest object. This method very rarely uses GPS, but typically uses a combination of Wi-Fi and cell information to compute device location.
* While driver is **on a ride**, we need the more accurate location frequently, so here we use **PRIORITY_HIGH_ACCURACY** value as an argument to setPriority() method of locationRequest object and we fetch the accurate location every 5 seconds by passing the value **5** to setInterval() method of the locationRequest object. This method uses GPS, Wi-Fi, cell and a variety of Sensors to fetch the location.
* While driver is **OnBreak and Offline**, we don't need much accurate location and also not much frequently, so we use **PRIORITY_BALANCED_POWER_ACCURACY** value as an argument to setPriority() method of locationRequest object and we fetch the location every 10 seconds by passing the value **10** to setInterval() method of the locationRequest object.


#### Build Flavours
We have number of clients who use our Cab9 app, which have color difference and their own logos which makes the app appear as their own. We brand the app according to the client's colors and logos while still maintaining the single code base. This is possible by adding product flavours(Brands) in the build.gradle file and adding the separate **res** folder for each of them.

If a client wants a unique functionality , then we create a different **SourceSets** of that class in that particluar flavour's folder and we mention this path in the build.gradle file's SourceSets section of that particular flavour.
