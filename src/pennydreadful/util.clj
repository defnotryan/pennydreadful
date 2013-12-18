(ns pennydreadful.util
  (:use clojure.pprint)
  (:require [markdown.core :as md]))

(def version (-> "project.clj" slurp read-string (nth 2)))

(defn pprint-str [x]
  (let [w (java.io.StringWriter.)]
    (pprint x w)
    (.toString w)))

(def not-nil? (complement nil?))
