# Video Streaming Analytics Design Document

## Introddction 

## Objectives
The main objective of this project is to collect, analyze and report the following video KPIs
  - Views - indicates how many times your video has been consumed
  - Unique views - represent the actual number of people who watched the video or live stream
  - Session duration - the average time a user watched the video
  - Start-up time - the time it takes to start playing the video
  - Video buffering - describes the time it takes to (pre-)load the data that is needed to play a video
  - Geolocation - the geographical popularity of the video
  - Device data - discover the OS, browsers, video players that are used by the audience 
  - Cdn QoS - the quality of service of the CDN.

## Data Flow Diagram
The project runs on top of Apache Spark Streaming. Apache Kafka serves as a data source for Apache Spark Streaming. The processed data persisted to the File Sink in append-mode.  
<br/>
![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/0ec45b4eb3200fd7edbb32c5d09a538f863dce3b/docs/chart-spark-app.png)
<br/>  

## 