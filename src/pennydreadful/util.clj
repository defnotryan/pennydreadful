(ns pennydreadful.util
  (:use clojure.pprint)
  (:require [markdown.core :as md]))

(def version (-> "project.clj" slurp read-string (nth 2)))

(defn pprint-str [x]
  (let [w (java.io.StringWriter.)]
    (pprint x w)
    (.toString w)))

(def not-nil? (complement nil?))

(defn denil [m]
  (into {} (remove (fn [[k v]] (nil? v)) m)))

(defn parse-long [s]
  (Long/parseLong s))
