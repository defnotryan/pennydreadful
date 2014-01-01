(ns pennydreadful.util
  (:use clojure.pprint)
  (:require [markdown.core :as md]))

(def version (-> "project.clj" slurp read-string (nth 2)))

(defn pprint-str [x]
  "Creates a String containing the pretty-print of x."
  (let [w (java.io.StringWriter.)]
    (pprint x w)
    (.toString w)))

(def not-nil? (complement nil?))

(defn denil [m]
  "Derives a map from m where keys mapped to nil have been dissoc'ed."
  (into {} (remove (fn [[k v]] (nil? v)) m)))

(defn mapped? [m k v]
  "Returns m if m contains key k mapped to value v, false otherwise."
  (if (and (contains? m k) (= v (get m k)))
    m
    false))

(defn parse-long [s]
  (Long/parseLong s))
