package dimastatz.flumenz.models

case class CdnLog(
    cdnType: String,
    cdnPop: String,
    httpStatusCode: Int,
    httpLatency: Double,
    sessionId: String,
    videoId: String,
    videoOwner: String,
    segmentId: String,
    profileId: String
)
