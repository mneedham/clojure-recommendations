(ns clojure-recommendations.config
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [clojure-recommendations.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[clojure-recommendations started successfully using the development profile]=-"))
   :middleware wrap-dev})
