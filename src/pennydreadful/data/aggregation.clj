(ns pennydreadful.data.aggregation
  (:require [clojure.walk :refer [walk]]
            [clojure.pprint :refer[pprint]]
            [clj-time.core :as t]
            [pennydreadful.util :refer [not-nil?]]))

(def word-regex #"\w+")

(defn sum-words [text]
  (count (re-seq word-regex text)))

(defn word-count [item]
  (if (= :snippet (:entity item))
    (sum-words (:content item))
    (walk word-count #(apply + %) (:children item))))

(defn word-count-target-aggregated [{:keys [word-count-mode target] :as item}]
  (case word-count-mode
    :manual target
    :automatic (->> item :children (map word-count-target-aggregated) (apply +))
    :off 0))

(defn nil-greatest [& instants]
  "Returns the greatest/latest argument that isn't nil"
  (->> instants
       (filter not-nil?)
       sort
       last))

(defn deadline-aggregated [{:keys [deadline-mode deadline] :as item}]
  (case deadline-mode
    :manual deadline
    :automatic (->> item :children (map deadline-aggregated) (apply nil-greatest))
    :off nil))
