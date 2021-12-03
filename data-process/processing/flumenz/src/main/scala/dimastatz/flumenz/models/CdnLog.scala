package dimastatz.flumenz.models

import java.sql.Timestamp

case class CdnLog(
    cdnType: String,
    cdnPop: String,
    httpStatusCode: Int,
    httpLatency: Double,
    sessionId: String,
    videoId: String,
    videoOwner: String,
    segmentId: String,
    profileId: String,
    timestamp: Timestamp
)
