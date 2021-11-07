# Problem Scope
Consider a sessionization problem in Data Streaming Systems. Each session consists of sequence of events following by a closing event or by a period of inactivity. The following case classes define events and session structure in the system.

```scala
case class Event(sessionId: String, eventType: String, ts: Timestamp)

case class Session(sessionId: String, start: Timestamp, close: Timestamp, events: Int, close_dt: String)
```

Each event can be either SessionOpen, SessionProgress or SessionClose event.
Design and implement the solution that calculates sessions duration and sessions message number. The max session length is 1 hour. Due to delivery errors in the system, some sessions have no SessionClose events. Such sessions should be closed due to timeout. Some sessions will miss SessionOpen event due to data loss. Such sessions will have negative duration.  

- Example:

The following sequence of events: 

```scala
val m1 = Event("1", "SessionOpen", new Timestamp(System.currentTimeMillis()))
val m1 = Event("2", "SessionOpen", new Timestamp(System.currentTimeMillis()))
val m1 = Event("1", "SessionClose", new Timestamp(System.currentTimeMillis()))
val m1 = Event("3", "SessionClose", new Timestamp(System.currentTimeMillis()))
```

Should produce the following sequence of Sessions
```scala
val session1 = Session("1", 1)
val session3 = Session("3", 0)
val session2 = Session("2", 60)
```  
<br/>

# Solution





