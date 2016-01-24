(ns clojure-recommendations.cypher
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [clojure.walk :as walk]))

(def conn (nr/connect "http://neo4j:medium@localhost:7474/db/data/"))

(defn execute
  ([query] (execute query {}))
  ([query params] (-> (cy/tquery conn query params) walk/keywordize-keys)))


;; Find Clojure meetups
(def query
  "MATCH (group:Group)-[:HAS_TOPIC]->(:Topic {name: 'Clojure'})
   RETURN group
   LIMIT 5")

;; Show raw response
(cy/tquery conn query)

;; Extract the interesting parts
(->> (cy/tquery conn query)
     walk/keywordize-keys
     (map #(-> % :group :data)))
