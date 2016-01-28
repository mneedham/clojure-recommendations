(ns clojure-recommendations.queries)

(def suggested-groups
  "MATCH (member:Member {name: {name}})-[:INTERESTED_IN]->(topic)<-[:HAS_TOPIC]-(otherGroup)
   WHERE NOT (member)-[:MEMBER_OF]->(otherGroup)
   WITH otherGroup, COUNT(*) AS topics, SIZE((otherGroup)<-[:MEMBER_OF]-()) AS numberOfMembers
   WHERE topics > 0
   OPTIONAL MATCH (otherGroup)-[:HOSTED_EVENT]->(event) WHERE (timestamp() - 90*24*60*60*1000 ) < event.time < timestamp()
   RETURN otherGroup, topics , numberOfMembers, COUNT(event) AS recentEvents")

(def suggested-events
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
   WITH group, numberOfMembers, SIZE(events) as numberOfEvents, events
   OPTIONAL MATCH (me:Member {name: {me}})-[:FRIENDS]-(friend)-[:MEMBER_OF]->(group)
   RETURN group, numberOfMembers, numberOfEvents, events, COLLECT(friend) AS friends")

(def user
 "MATCH (member:Member {id: {id}})
  OPTIONAL MATCH (member)-[:RSVPD {response: 'yes'}]->(event)
  WHERE event.time < timestamp()

  WITH member, SIZE((member)-[:MEMBER_OF]->()) AS numberOfGroups, event
  LIMIT 1
  MATCH (member)-[:MEMBER_OF]->(group)

  WITH member, numberOfGroups, event, group
  ORDER BY group.name
  WITH member, numberOfGroups, event, COLLECT(group) AS groups

  MATCH (me:Member {name: {me}})
  OPTIONAL MATCH (me)-[:MEMBER_OF]->(commonGroup)<-[:MEMBER_OF]-(member)

  WITH me, member, numberOfGroups, event, groups, COLLECT(commonGroup) AS commonGroups
  OPTIONAL MATCH (me)-[:RSVPD {response: 'yes'}]->(commonEvent:Event)<-[:RSVPD {response: 'yes'}]-(member:Member)
  WHERE commonEvent.time < timestamp()
  WITH me, member, numberOfGroups, event, groups, commonGroups, commonEvent ORDER BY commonEvent.time DESC

  RETURN member, numberOfGroups, event, groups, commonGroups, COLLECT(commonEvent) AS commonEvents
  ")

(def logged-in-user
  "MATCH (member:Member {name: {name}})
   RETURN member")

(def topic
  "MATCH (topic:Topic {id: {id}})<-[:HAS_TOPIC]-(group)
   WITH topic, group
   ORDER BY group.name
   RETURN topic, SIZE((topic)<-[:INTERESTED_IN]-()) AS numberOfMembers, COLLECT(group) AS groups")
