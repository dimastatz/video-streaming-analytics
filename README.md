# Video Streaming Analytics with Apache Spark


## Introduction

Apache Spark Streaming is a leading platform that provides scalable and fast data stream processing and is widely used to solve such problems as real-time predictions, pattern recognition, streaming analytics, and many more.  
In this project I will use Apache Spark to build 7 video KPIs and data point which can help understanding the Video Streaming Platform's audience better, optimize viewer’s satisfaction and experience by ensuring higher video streaming quality.  
Video Streaming Platforms can differ one from another in terms of implementation, but probably we can state that most of them have the following components in common: Video Procssing(encoding), Media Packaging, Media Storage (AWS S3 as an example), Video Delivery (CDN, CloudFront as an example)

![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/main/docs/chart-video-streaming.png)
  

```scala
import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession

val spark = SparkSession
  .builder
  .appName("StructuredNetworkWordCount")
  .getOrCreate()
  
import spark.implicits._
```


## Objectives

## 


## Analysis