# Video Streaming Analytics with Apache Spark

## Introduction
Video Streaming Analytics platforms help overcome streaming problems, understand the audience better and optimize the viewer’s satisfaction by ensuring higher video streaming quality. 
Video Streaming Platforms can differ from one to another. In the current project, we create the Analytics for the Video Streaming platforms that have the following components in common: Video Processing, Media Packaging, Media Storage, and Video Distribution.  
<br/>
![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/main/docs/chart-video-streaming.png)
<br/>  

## Objectives
The main objective of this project is to create a reusable component that collects and analyzes video streaming data in real-time with the latency of 10 minutes. This project will produce the following KPIs:

The main objective of this project is to collect, analyze and report the following video KPIs
  - Views - indicates how many times your video has been consumed
  - Unique views - represent the actual number of people who watched the video or live stream
  - Session duration - the average time a user watched the video
  - Start-up time - the time it takes to start playing the video
  - Video buffering - describes the time it takes to (pre-)load the data that is needed to play a video
  - Geolocation - the geographical popularity of the video
  - Device data - discover the OS, browsers, video players that are used by the audience 
  - Cdn QoS - the quality of service of the CDN.


## Tech Stack
The solution is developed on top of Apache Spark Streaming and can be seen as a domain-specific add-on to Apache Spark Streaming.
Another important aspect of this project is Cloud Nativeness. This project avoids Cloud Specific lockdowns, like dependency on managed services, proprietary software, etc. In order to achieve the `run anywere' ability, containerization is used widely. All needed tools, libs, code and runtime engines are packaged into docker containers. Docker-Compose is used to simulate the full production environment.
As for programming languages, Python and Scala are main languages of this project.

## Design
The project is built on top of Apache Spark Streaming. Apache Kafka is used as a data source for the Apache Spark Streaming. The processed data is writtend to the File Sink by using Append Mode.  
<br/>
![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/0ec45b4eb3200fd7edbb32c5d09a538f863dce3b/docs/chart-spark-app.png)
<br/>  

## How to use
As a prerequistes you should install [docker desktop](https://docs.docker.com/desktop/) and [docker compose](https://docs.docker.com/compose/). Once docker software is installed, you can run 
[docker-compose-process.yml](https://github.com/dimastatz/video-streaming-analytics/blob/946d90a124a4e8b8078dca347abb109ded971eef/docker-compose-process.yml). This docker-compose file starts Apache Kafka and Apache Spark Streaming containers. You can feed data into the Apache Kafka in order to see how it works.


