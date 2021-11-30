# Video Streaming Analytics with Apache Spark

## Introduction
Consider a live event like an NBA game. The video is captured by cameras installed on a Basketball Arena, and it makes its way to the viewer's [OTT](https://en.wikipedia.org/wiki/Over-the-top_media_service) devices: TVs, computers, smartphones through the Live Video Streaming System. Live Video Streaming Systems implements such services as ingest, transcoding, packaging, distribution, server-side ad insertion, etc. Each service in Video Streaming System delivers logs to the Video Analytics.

| ![Video Streaming Analytics](https://github.com/dimastatz/video-streaming-analytics/blob/main/docs/chart-video-streaming.png) |
|:--:| 
| *Diagram1: Video Streaming Flow* |

Video Streaming Analytics platforms help overcome streaming problems, understand the audience better and optimize the viewer’s satisfaction by ensuring higher video streaming quality. 
Video Streaming Platforms can differ from one to another. In the current project, we create the Analytics for the Video Streaming platforms that have the following components in common: Video Processing, Media Packaging, Media Storage, and Video Distribution.

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


## Design
The project runs on top of Apache Spark Streaming. Apache Kafka serves as a data source for Apache Spark Streaming. The processed data is written to the File Sink by using Append Mode.  

| ![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/0ec45b4eb3200fd7edbb32c5d09a538f863dce3b/docs/chart-spark-app.png) |
| :--: |
| *Diagram2: Video Analytics Flow* |

For more information, view the following design [document](https://github.com/dimastatz/video-streaming-analytics/blob/main/docs/design/video-analytics-design.md)


## How-To

### Scala Project
To consume this project from another Apache Spark application, you can build it by using [sbt](https://www.scala-sbt.org/):
```bash
mkdir video-analytics
cd video-analytics
git clone git@github.com:dimastatz/video-streaming-analytics.git
cd video-streaming-analytics/data-process/processing/flumenz
sbt assembly
```
The command [sbt assembly](https://github.com/sbt/sbt-assembly) builds an [uber jar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar/11947093). Uber jar can be used from another Spark Application. 
You can also try [sbt testCoverage](https://github.com/scoverage/sbt-scoverage) to run all unit tests at once. The code coverage of this project is above [90%](https://github.com/dimastatz/video-streaming-analytics/blob/main/data-process/processing/flumenz/build.sbt#L37), and you can examine almost everything from the unit tests.


### Docker Compose
As a prerequisites you should install [docker desktop](https://docs.docker.com/desktop/) and [docker compose](https://docs.docker.com/compose/). After installing the docker software, run 
[docker-compose-process.yml](https://github.com/dimastatz/video-streaming-analytics/docker-compose-process.yml). This docker-compose file starts Apache Kafka and Apache Spark Streaming containers. You can feed data the Apache Kafka to see how it works.








