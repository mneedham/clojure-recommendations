
(require '[clojure.core.reducers :as r])
(r/fold + (r/filter even? (r/map inc [1 1 1 2])))

(into [] (r/filter even? (r/map inc [1 1 1 2])))


(defn log2 [n]
  (/ (Math/log n) (Math/log 2)))

(defn score-item [n]
  (if (= n 0) 0 (log2 n)))

(+ (score-item 12) (score-item 13) (score-item 5))
(+ (score-item 12) (score-item 13) (score-item 5) (score-item 6))


(reduce #(+ %1 (score-item %2)) 0 [12 13 5])
(reduce #(+ %1 (score-item %2)) 0 [12 13 5 6])

(r/reduce #(+ %1 (score-item %2)) 0 [12 13 5 6])

(defn sum-scores
  ([] 0)
  ([acc item] (+ acc (score-item item))))

(r/reduce sum-scores [12 13 5 6])
(r/fold sum-scores [12 13 5 6])

(->>[12 13 5 6]
    (map score-item)
    (filter #(> % 3)))

(->> [12 13 5 6]
     (r/map score-item)
     (r/filter #(> % 3))
     (r/take 1)
     (into []))

(->> [12 13 5 6]
     (r/map score-item)
     (r/filter #(> % 3))
     (r/fold +))
