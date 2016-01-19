(ns clojure-recommendations.cypher
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [clojure.walk :as walk]))

(def conn (nr/connect "http://neo4j:neo4j@localhost:7474/db/data/"))

(defn execute
  ([query] (execute-cypher query {}))
  ([query params] (-> (cy/tquery conn query params) walk/keywordize-keys)))
