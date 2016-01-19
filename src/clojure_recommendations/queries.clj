(ns clojure-recommendations.queries)

(def suggested-groups
  "MATCH (member:Member {name: {name}})-[:INTERESTED_IN]->(topic),
        (member)-[:MEMBER_OF]->(group)-[:HAS_TOPIC]->(topic)
   WITH member, topic, COUNT(*) AS score
   MATCH (topic)<-[:HAS_TOPIC]-(otherGroup)
   WHERE NOT (member)-[:MEMBER_OF]->(otherGroup)
   RETURN otherGroup,
          COLLECT(topic) AS topicsInCommon,
          SUM(score) as score
   ORDER BY score DESC
   LIMIT 10")

(def suggested-events-1
 "WITH 24.0*60*60*1000 AS oneDay
  MATCH (member:Member {name: {name}})
  MATCH (futureEvent:Event) WHERE futureEvent.time >= timestamp()
  MATCH (futureEvent)<-[:HOSTED_EVENT]-(group)

  WITH oneDay, group, futureEvent, member, EXISTS((group)<-[:MEMBER_OF]-(member)) AS isMember
  OPTIONAL MATCH (member)-[rsvp:RSVPD {response: 'yes'}]->(pastEvent)<-[:HOSTED_EVENT]-(group)
  WHERE pastEvent.time < timestamp()

  RETURN group,
         futureEvent,
         isMember,
         COUNT(rsvp) AS previousEvents,
         round((futureEvent.time - timestamp()) / oneDay) AS days,
         futureEvent.time + futureEvent.utcOffset AS futureEventTime
  ORDER BY days, previousEvents DESC
  LIMIT 10")

(def suggested-events-2
  "WITH 24.0*60*60*1000 AS oneDay
   MATCH (member:Member {name: {name}})
   MATCH (futureEvent:Event) WHERE futureEvent.time >= timestamp()
   MATCH (futureEvent)<-[:HOSTED_EVENT]-(group)

   WITH oneDay, group, futureEvent, member, EXISTS((group)<-[:MEMBER_OF]-(member)) AS isMember
   OPTIONAL MATCH (member)-[rsvp:RSVPD {response: 'yes'}]->(pastEvent)<-[:HOSTED_EVENT]-(group)
   WHERE pastEvent.time < timestamp()

   WITH oneDay, group, futureEvent, member, isMember, COUNT(rsvp) AS previousEvents
   OPTIONAL MATCH (futureEvent)<-[:HOSTED_EVENT]-()-[:HAS_TOPIC]->(topic)<-[:INTERESTED_IN]-(member)

   RETURN group, futureEvent, isMember, previousEvents,
          COUNT(topic) AS topics, round((futureEvent.time - timestamp()) / oneDay) AS days,
          futureEvent.time + futureEvent.utcOffset AS futureEventTime
   ORDER BY days,previousEvents DESC, topics DESC
   LIMIT 10")


(def suggested-events-3
"WITH 24.0*60*60*1000 AS oneDay
 MATCH (member:Member {name: {name}})
 MATCH (futureEvent:Event) WHERE futureEvent.time >= timestamp()
 MATCH (futureEvent)<-[:HOSTED_EVENT]-(group)

 WITH oneDay, group, futureEvent, member, EXISTS((group)<-[:MEMBER_OF]-(member)) AS isMember
 OPTIONAL MATCH (member)-[rsvp:RSVPD {response: 'yes'}]->(pastEvent)<-[:HOSTED_EVENT]-(group)
 WHERE pastEvent.time < timestamp()

 WITH oneDay, group, futureEvent, member, isMember, COUNT(rsvp) AS previousEvents
 OPTIONAL MATCH (futureEvent)<-[:HOSTED_EVENT]-()-[:HAS_TOPIC]->(topic)<-[:INTERESTED_IN]-(member)

 WITH oneDay, group, futureEvent, member, isMember, previousEvents, COUNT(topic) AS topics
 OPTIONAL MATCH (member)-[:FRIENDS]-(friend:Member)-[rsvpYes:RSVP_YES]->(futureEvent)

 RETURN group, futureEvent, isMember, round((futureEvent.time - timestamp()) / oneDay) AS days,
        previousEvents, topics, COUNT(rsvpYes) AS friendsGoing, COLLECT(friend)[..5] AS friends,
        futureEvent.time + futureEvent.utcOffset AS futureEventTime
 ORDER BY days, friendsGoing DESC, previousEvents DESC
 LIMIT 10")

(def event
 "MATCH (event:Event {id: {id}})<-[:HOSTED_EVENT]-(group)
  OPTIONAL MATCH (person)-[rsvp:RSVPD {response: 'yes'}]->(event)
  RETURN event, group, COUNT(rsvp) AS numberOfRSVPs, COLLECT(person) AS rsvps")

(def group
  "MATCH (group:Group {id: {id}})<-[:MEMBER_OF]-()
   WITH group, COUNT(*) AS numberOfMembers
   OPTIONAL MATCH (group)-[:HOSTED_EVENT]->(event)
   WHERE event.time < timestamp()
   WITH group, numberOfMembers, event
   ORDER BY event.time DESC
   WITH group, numberOfMembers, COLLECT(event) AS events
   RETURN group, numberOfMembers, SIZE(events) AS numberOfEvents, events")

(def user
 "MATCH (member:Member {id: {id}})
  OPTIONAL MATCH (member)-[:RSVPD {response: 'yes'}]->(event)
  WHERE event.time < timestamp()
  WITH member, SIZE((member)-[:MEMBER_OF]->()) AS numberOfGroups, event
  LIMIT 1
  MATCH (member)-[:MEMBER_OF]->(group)
  WITH member, numberOfGroups, event, group
  ORDER BY group.name
  RETURN member, numberOfGroups, event, COLLECT(group) AS groups")

(def logged-in-user
  "MATCH (member:Member {name: {name}})
   RETURN member")
