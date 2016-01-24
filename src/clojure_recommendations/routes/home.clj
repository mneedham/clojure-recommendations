(ns clojure-recommendations.routes.home
  (:require [clojure-recommendations.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [clojure-recommendations.timestamp :as t]
            [clojure-recommendations.cypher :as cypher]
            [clojure-recommendations.queries :as queries]))

(def logged-in-user "Pieter Cailliau")

(defn extract-date-time [timestamp]
  { :formatted-time (t/as-time timestamp)
    :formatted-date (t/as-date timestamp)
    :how-long (t/how-long timestamp) })

(defn suggested-events-for-user [user query]
  (let [rows (cypher/execute query {:name user})]
    (map #(merge % (extract-date-time (-> % :futureEventTime) )) rows)))

(def suggested-events
  (partial suggested-events-for-user logged-in-user))

(defn person [name]
  (first (cypher/execute queries/logged-in-user {:name name})))

(defn home-page []
  (layout/render
    "home.html" {:suggested-groups (cypher/execute queries/suggested-groups {:name logged-in-user})
                 :suggested-events-1 (suggested-events queries/suggested-events-1)
                 :suggested-events-2 (suggested-events queries/suggested-events-2)
                 :suggested-events-3 (suggested-events queries/suggested-events-3)
                 :person (person logged-in-user)}))

(defn about-page []
  (layout/render "about.html"))

(defn event [event-id]
  (first (cypher/execute queries/event {:id event-id})))

(defn event-page [event-id]
  (layout/render "event.html" {:event (event event-id) }))

(defn group [group-id]
  (first (cypher/execute queries/group {:id group-id})))

(defn group-page [group-id]
  (layout/render "group.html" {:group (group group-id)}))

(defn user [user-id]
  (first (cypher/execute queries/user {:id user-id})))

(defn user-page [user-id]
  (layout/render "user.html" {:data (user user-id)}))

(defn topic [topic-id]
  (first (cypher/execute queries/topic {:id topic-id})))

(defn topic-page [topic-id]
  (layout/render "topic.html" {:data (topic topic-id)}))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/events/:event-id" [event-id] (event-page event-id))
  (GET "/groups/:group-id" [group-id] (group-page group-id))
  (GET "/users/:user-id" [user-id] (user-page user-id))
  (GET "/topics/:topic-id" [topic-id] (topic-page topic-id))
  (GET "/about" [] (about-page)))
