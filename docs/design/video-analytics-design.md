# Video Streaming Analytics Design Document

## Introduction
Video Streaming Analytics platforms help overcome streaming problems, understand the audience better and optimize the viewer’s satisfaction by ensuring higher video streaming quality. 
Video Streaming Platforms can differ from one to another. In the current project, we create the Analytics for the Video Streaming platforms that have the following components in common: Video Processing, Media Packaging, Media Storage, and Video Distribution.  
<br/>
![Video Streaming Analytics](https://github.com/dimastatz/video-streaming-analytics/blob/main/docs/chart-video-streaming.png)
*image_caption*
<br/> 


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

## Data Flow
The solution is built on top of Apache Spark Streaming. Apache Kafka serves as a data source for Apache Spark Streaming. The processed data persisted to the File Sink in append-mode.  
<br/>
![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/0ec45b4eb3200fd7edbb32c5d09a538f863dce3b/docs/chart-spark-app.png)
<br/>  
On The  



### Data Normalization

### Data Enrichment

### Data Aggregation


