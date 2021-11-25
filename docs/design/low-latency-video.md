## Ofer 2021-11-22

1. Dasher versus Frane by Frane
    - Slicer but making all bitrates, needs a lot of resources on client-side
    - Demuxer (?)
    - Gstreamer, DirectShow versus Uplynk Graph
    - Frame by Frame

2. Problems
    = Slicer adds latency
    - Encoders not in RT (Nvidia generic, Xlink AWS VT1)
    - Jitter warm uptime
    - 

3. WebRTC


## Ofer 2021-11-25

1. Server-side ad insertion
    - Problem: ad length is not always equals to the break's length
    - Slate = number of slices when the last has variable size.
    - Slicer is not distributed, and this is a source of truth 
    - Ingest slices as small as possible
    - It takes in avg 7 sec to fecth the add from adproxy
    - When slice is ready,  
    - Prefetch = get_ad(id) + show_ad(id)
    - Drift 
    = That's all for ad insertion 

2; Low latency HLS
    - 