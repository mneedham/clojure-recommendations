(ns clojure-recommendations.scoring)


(defn log2 [n]
  (/ (Math/log n) (Math/log 2)))

(defn score [minimum maximum eighty raw]
  (if (< raw minimum)
    0
    (let [alpha (/ (log2 5) eighty)
          exp (Math/exp (* (- alpha) raw))]
      (* maximum (- 1 exp)))))

(defn score-item [{minimum :minimum maximum :maximum eighty :eighty n :n}]
  (score minimum maximum eighty n))

(defn score-row [row]
  (let [topics  {:n (-> row :topics) :minimum 1 :maximum 100 :eighty 5}
        members {:n (-> row :numberOfMembers) :minimum 50 :maximum 100 :eighty 1000}
        events  {:n (-> row :recentEvents) :minimum 1 :maximum 100 :eighty 3}]
    (reduce #(+ %1 (score-item %2)) 0 [topics members events])))
