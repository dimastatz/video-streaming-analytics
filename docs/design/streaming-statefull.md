# Problem Scope
Consider a sessionization problem. Each session consists of two events of a starting event and a closing event. The following case classes define events and session structure in the system.

```scala
case class Event(sessionId: String, eventType: String, ts: Timestamp)

case class Session(sessionId: String, durationMinutes: Int)
```

Each event can be either SessionOpen or SessionClose event.
Design and implement the solution that calculates sessions length. The max session length is 1 hour. Due to delivery errors in the system, some sessions have the OpenSession event only. Such sessions should be closed due to timeout, and session duration should be the max session duration - 1 hour. Some sessions have the CloseSession events only and should be closed immediately, and the session duration will be 0.   


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





