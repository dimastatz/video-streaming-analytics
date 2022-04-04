# Video Ingest

## Background
Briefly describe the System. What business or technical problem does it solve. Describe it by its similarity to other well-known systems or subjects. 

## Requirements

### Functional 
- Handle Video signal (recorder, RTP, UDP, VoD)
- Create video slices (UHD, HD, SD 4 sec)
- Store data in the central repository
- Encode slices to different resolutions (rays)
- Save artifacts to S3 or PutToCache
- Slicer API (black screen and ads)


### Non-Functional
Address the following aspects of System Design
- Security
- Efficiency
    - Latency for ingest: 
        - Target 6s
        - LL HLS
        - Frame by frame
        - Put to cache
        - Dasher (Slicer and Dasher on client side)
        - Out of scope (WebRTC)
    - Multi Region
        - US covered
        = EU (?)

- Capacity
    - Slicers
        - Total number of slicers 
    - Encoders
        - Total number of encoders (min/max spike)
- Scalability
    = Auto scalers
- Reliability
    - Slicer failover 
    = Broker retry to encoder
    - Slicer retry to broker
    

## System APIs
Reduce Complexity

## High-Level Design


## Detailed Design 
Low-Level design of each component (OOD can help)

## Analysis
- Cost
- Security
- Reliability
- Performance Efficiency
- Operation Excellence





