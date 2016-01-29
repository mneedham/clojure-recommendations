(ns clojure-recommendations.timestamp
  (:require [clj-time.core :as t])
  (:require [clj-time.coerce :as c])
  (:require [clj-time.format :as f]))

(defn as-time [timestamp]
  (let [time (c/from-long timestamp)]
    (f/unparse (f/formatter "HH:mm") time)))

(defn how-long [timestamp]
  (let [time (c/from-long timestamp)]
    (if (t/after? time (t/now))
      (t/in-days (t/interval (t/now) time))
      (t/in-days (t/interval  time (t/now))))))

(defn day-suffix [day]
  (let [stripped-day (if (< day 20) day (mod day 10))]
    (cond (= stripped-day 1) "st"
          (= stripped-day 2) "nd"
          (= stripped-day 3) "rd"
          :else "th")))

(defn as-date [timestamp]
  (let [time (c/from-long timestamp)
        day (read-string (f/unparse (f/formatter "d") time))]
    (str day
         (day-suffix day)
         " "
         (f/unparse (f/formatter "MMMM yyyy") time)) ))

(defn as-date-time [timestamp]
  (let [time (c/from-long timestamp)
        day (read-string (f/unparse (f/formatter "d") time))]
    (str day
         (day-suffix day)
         " "
         (f/unparse (f/formatter "MMMM yyyy, HH:mm") time))))
