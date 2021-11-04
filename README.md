# Video Streaming Analytics with Apache Spark


## Introduction

Video Streaming Analytics platforms help overcome streaming problems, understand the audience better and optimize the viewer’s satisfaction by ensuring higher video streaming quality. 
Be aware that Video Streaming Platforms can differ from one to another. In the current project, we create the Analytics for the Video Streaming platforms that have the following components in common: Video Processing, Media Packaging, Media Storage, and Video Distribution.  
<br/>
![alt text](https://github.com/dimastatz/video-streaming-analytics/blob/main/docs/chart-video-streaming.png)
<br/>  

## Objectives
The main objective of this project is to collect, analyze and report the following video KPIs
  - Views - indicates how many times your video has been consumed
  - Unique views = represent the actual number of people who watched the video or live stream
  - Session duration = the average time a user watched the video
  - Start-up time - the time it takes to start playing the video
  - Video buffering - describes the time it takes to (pre-)load the data that is needed to play a video
  - Geolocation - the geographical popularity of the video
  - Device data - discovers the OS, browsers, video players across the audience 
  - Cdn QoS - quality of service of the CDN.

Another objective is to process and analyze data in near real-time, with a max latency of 2 minutes.

And the last objective, but not least, is to create the code as generic as possible. 


## Project Scope
