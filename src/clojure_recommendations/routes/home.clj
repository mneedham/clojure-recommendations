(ns clojure-recommendations.routes.home
  (:require [clojure-recommendations.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [clojure.walk :as walk]
            [clojure-recommendations.timestamp :as t]
            [clojure-recommendations.cypher :as cypher]))

(def logged-in-user "Mark Needham")

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

(def suggested-events-query-1
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

(def suggested-events-query-2
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

(def suggested-events-query-3
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

(defn extract-date-time [timestamp]
  { :formatted-time (t/as-time timestamp)
    :formatted-date (t/as-date timestamp)
    :how-long (t/how-long timestamp) })

(defn suggested-events-1 [user]
  (let [rows (cypher/execute suggested-events-query-1 {:name logged-in-user})]
    (map #(merge % (extract-date-time (-> % :futureEventTime) )) rows)))

(defn suggested-events-2 [user]
  (let [rows (cypher/execute suggested-events-query-2 {:name logged-in-user})]
    (map #(merge % (extract-date-time (-> % :futureEventTime) )) rows)))

(defn suggested-events-3 [user]
  (let [rows (cypher/execute suggested-events-query-3 {:name logged-in-user})]
    (map #(merge % (extract-date-time (-> % :futureEventTime) )) rows)))

(def logged-in-user-query
  "MATCH (member:Member {name: {name}})
   RETURN member")

(defn person [name]
  (first (cypher/execute logged-in-user-query {:name name})))

(defn home-page []
  (layout/render
    "home.html" {:suggested-groups (cypher/execute suggested-groups {:name logged-in-user})
                 :suggested-events-1 (suggested-events-1 logged-in-user)
                 :suggested-events-2 (suggested-events-2 logged-in-user)
                 :suggested-events-3 (suggested-events-3 logged-in-user)
                 :person (person logged-in-user)}))

(defn about-page []
  (layout/render "about.html"))

(def event-query
  "MATCH (event:Event {id: {id}})<-[:HOSTED_EVENT]-(group)
   OPTIONAL MATCH (person)-[rsvp:RSVPD {response: 'yes'}]->(event)
   RETURN event, group, COUNT(rsvp) AS numberOfRSVPs, COLLECT(person) AS rsvps")

(defn event [event-id]
  (first (cypher/execute event-query {:id event-id})))

(defn event-page [event-id]
  (layout/render "event.html" {:event (event event-id) }))

(def group-query
  "MATCH (group:Group {id: {id}})<-[:MEMBER_OF]-()
   WITH group, COUNT(*) AS numberOfMembers
   OPTIONAL MATCH (group)-[:HOSTED_EVENT]->(event)
   WHERE event.time < timestamp()
   WITH group, numberOfMembers, event
   ORDER BY event.time DESC
   WITH group, numberOfMembers, COLLECT(event) AS events
   RETURN group, numberOfMembers, SIZE(events) AS numberOfEvents, events")

(defn group [group-id]
  (first (cypher/execute group-query {:id group-id})))

(defn group-page [group-id]
  (layout/render "group.html" {:group (group group-id)}))

(def user-query
  "MATCH (member:Member {id: {id}})
   OPTIONAL MATCH (member)-[:RSVPD {response: 'yes'}]->(event)
   WHERE event.time < timestamp()
   WITH member, SIZE((member)-[:MEMBER_OF]->()) AS numberOfGroups, event
   LIMIT 1
   MATCH (member)-[:MEMBER_OF]->(group)
   WITH member, numberOfGroups, event, group
   ORDER BY group.name
   RETURN member, numberOfGroups, event, COLLECT(group) AS groups")

(defn user [user-id]
  (first (cypher/execute user-query {:id user-id})))

(defn user-page [user-id]
  (layout/render "user.html" {:data (user user-id)}))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/events/:event-id" [event-id] (event-page event-id))
  (GET "/groups/:group-id" [group-id] (group-page group-id))
  (GET "/users/:user-id" [user-id] (user-page user-id))
  (GET "/about" [] (about-page)))
