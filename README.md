# Video Streaming Analytics with Apache Spark

## Introduction
Video Streaming Analytics platforms help overcome streaming problems, understand the audience better and optimize the viewer’s satisfaction by ensuring higher video streaming quality. 
Video Streaming Platforms can differ from one to another. In the current project, we create the Analytics for the Video Streaming platforms that have the following components in common: Video Processing, Media Packaging, Media Storage, and Video Distribution.  
<br/>
![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/main/docs/chart-video-streaming.png)
<br/>  

## Objectives
The main objective of this project is to create a reusable component that collects and analyzes video streaming data in real-time with the latency of 2 minutes. This project will produce the following KPIs:

The main objective of this project is to collect, analyze and report the following video KPIs:
  - Views - indicates how many times your video has been consumed
  - Unique views - represent the actual number of people who watched the video or live stream
  - Session duration - the average time a user watched the video
  - Start-up time - the time it takes to start playing the video
  - Video buffering - describes the time it takes to (pre-)load the data that is needed to play a video
  - Geolocation - the geographical popularity of the video
  - Device data - discover the OS, browsers, video players that are used by the audience 
  - Cdn QoS - the quality of service of the CDN.


## Tech Stack
The solution is developed on top of Apache Spark Streaming and can be used as a domain-specific add-on to Apache Spark Streaming.
Another important aspect of this project is Cloud Nativeness. This project avoids Cloud Specific lockdowns, like dependency on managed services, proprietary software, etc. To achieve the `run anywere' ability, containerization is used widely. All needed tools, libs, code, and runtime engines are packaged into the docker containers. Docker-Compose is used to simulate the production environment.
The main languages of this project are Scala and Python.

## Design
The project runs on top of Apache Spark Streaming. Apache Kafka serves as a data source for Apache Spark Streaming. The processed data is written to the File Sink by using Append Mode.  
<br/>
![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/0ec45b4eb3200fd7edbb32c5d09a538f863dce3b/docs/chart-spark-app.png)
<br/>  
For more information, view the following design [document](https://github.com/dimastatz/video-streaming-analytics/blob/702cebabc397bd66794f864005f2faac85a49958/docs/design/video-analytics-design.md)


## How to use
As a prerequisites you should install [docker desktop](https://docs.docker.com/desktop/) and [docker compose](https://docs.docker.com/compose/). After installing the docker software, run 
[docker-compose-process.yml](https://github.com/dimastatz/video-streaming-analytics/blob/946d90a124a4e8b8078dca347abb109ded971eef/docker-compose-process.yml). This docker-compose file starts Apache Kafka and Apache Spark Streaming containers. You can feed data the Apache Kafka to see how it works.
In order to use consume this project from another Apache Spark application, you can build by using [sbt](https://www.scala-sbt.org/)

```bash
mkdir video-analytics
cd video-analytics
git clone git@github.com:dimastatz/video-streaming-analytics.git
cd video-streaming-analytics/data-process/processing/flumenz
sbt assmembly
```




