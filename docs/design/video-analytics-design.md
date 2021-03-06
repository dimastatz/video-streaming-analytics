# Video Streaming Analytics Design Document

## Introduction
Consider a live event like an NBA game. The video is captured by cameras installed on a Basketball Arena, and it makes its way to the viewer's [OTT](https://en.wikipedia.org/wiki/Over-the-top_media_service) devices: TVs, computers, smartphones through the Live Video Streaming System. Live Video Streaming Systems implements such services as ingest, transcoding, packaging, distribution, server-side ad insertion, etc. Each service in Video Streaming System delivers logs to the Video Analytics.

| ![Video Streaming Analytics](https://github.com/dimastatz/video-streaming-analytics/blob/main/docs/chart-video-streaming.png) |
|:--:| 
| *Diagram1: Video Streaming Flow* |

Video Streaming Analytics platforms help overcome streaming problems, understand the audience better and optimize the viewer’s satisfaction by ensuring higher video streaming quality. 
Video Streaming Platforms can differ from one to another. In the current project, we create the Analytics for the Video Streaming platforms that have the following components in common: Video Processing, Media Packaging, Media Storage, and Video Distribution.

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
The Data Flow of Video Streaming Analytics starts in Apache Kafka. Every microservice of the Live Video Streaming System delivers logs to Apache Kafka. The delivery methods can be different. One of the well-known patterns for the logs delivery is to use such open-source log shippers as [fluentd](https://www.fluentd.org/) or [logstash](https://www.elastic.co/logstash/). In such a scenario, microservices write their logs to the log files. Log shippers tail log files, read, filter, transform, and upload the needed data to the Apache Kafka server. 
Once data is available in Apache Kafka, Apache Spark Streaming takes the data and runs normalization, enrichment, and aggregation steps. 

| ![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/0ec45b4eb3200fd7edbb32c5d09a538f863dce3b/docs/chart-spark-app.png) |
| :--: |
| *Diagram2: Video Analytics Flow* |  


### Data Normalization
In Video Streaming Analytics we mainly focus on analyzing viewer experience. Since viewers consume the video data from CDN, the most important data sources for Video Streaming Analytics are CDN access log files. Video Streaming Systems can use different CDN Service providers simultaneously. Access log files format can differ from one CDN provider to another. For example, see access log formats for [EdgeCast](https://docs.edgecast.com/cdn/Content/RTLD/Log-Fields.htm), [Akamai](https://learn.akamai.com/en-us/webhelp/datastream/datastream-user-guide/GUID-56313AE3-C16F-4BCF-9D83-C26DE737F762.html) and [CloudFront](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/AccessLogs.html#access-logs-analyzing). All these formats are different, and that's why we have to normalize the incoming data first. The following [Scala Case Class](https://docs.scala-lang.org/tour/case-classes.html) defines the normalized CdnLog for the Video Streaming Analytics system:

```scala
case class CdnLog(
    cdnType: String,
    cdnPop: String,
    httpStatusCode: Int,
    httpLatency: Double,
    videoId: String,
    videoOwner: String,
    segmentId: String,
    profileId: String,
    timestamp: Timestamp
)
```

### Data Enrichment
Once CDN events are normalized, we can enrich them by using additional data sources. For example, to analyze the geographical popularity of the video, we should translate viewer IP addresses to country and city. Another example is parsing userAgent fields to discover the OS, browser, video players, etc.    

### Data Aggregation


