(ns clojure-recommendations.config
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[clojure-recommendations started successfully]=-"))
   :middleware identity})
