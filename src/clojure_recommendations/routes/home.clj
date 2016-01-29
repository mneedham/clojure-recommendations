(ns clojure-recommendations.routes.home
  (:require [clojure-recommendations.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [clojure-recommendations.timestamp :as t]
            [clojure-recommendations.cypher :as cypher]
            [clojure-recommendations.queries :as queries]
            [clojure-recommendations.scoring :as scoring]))

(def logged-in-user "Mark Needham")

(defn extract-date-time [timestamp]
  { :formatted-time (t/as-time timestamp)
    :formatted-date (t/as-date timestamp)
    :how-long (t/how-long timestamp)})

(defn suggested-events-for-user [user query]
  (let [rows (cypher/execute query {:name user})]
    (map #(merge % (extract-date-time (-> % :futureEventTime) )) rows)))

(def suggested-events
  (partial suggested-events-for-user logged-in-user))

(defn person [name]
  (first (cypher/execute queries/logged-in-user {:name name})))

(defn suggested-groups [name]
  (let [result (cypher/execute queries/suggested-groups {:name name})]
    (take 10
          (->> result
               (map #(assoc % :score (scoring/score-row %)))
               (sort-by :score >)))))

(defn home-page []
  (layout/render
    "home.html" {:suggested-groups (suggested-groups logged-in-user)
                 :suggested-events-3 (suggested-events queries/suggested-events)
                 :person (person logged-in-user)}))

(defn about-page []
  (layout/render "about.html"))

(defn event [event-id]
  (let [result (first (cypher/execute queries/event {:id event-id :me logged-in-user}))]
    (merge result (extract-date-time (-> result :event :data :time)))))

(defn event-page [event-id]
  (layout/render "event.html" {:event (event event-id) }))

(defn group [group-id]
  (let [result (first (cypher/execute queries/group {:id group-id :me logged-in-user}))
        events-with-time (map #(merge % (extract-date-time (-> % :data :time))) (result :events))]
;;     (println "EVENTS:" (map #(merge % (extract-date-time (-> % :data :time))) (result :events)))
    (println "EVENTS: " events-with-time)
    (assoc-in result [:events] events-with-time)))

(defn group-page [group-id]
  (println "------" (group group-id))
  (layout/render "group.html" {:group (group group-id)}))

(defn user [user-id]
  (let [result (first (cypher/execute queries/user {:id user-id :me logged-in-user}))]
    (merge result (extract-date-time (-> result :event :data :time)))))

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
