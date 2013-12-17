(ns pennydreadful.util
  (:use clojure.pprint)
  (:require [noir.io :as io]
            [markdown.core :as md]))

(def version (-> "project.clj" slurp read-string (nth 2)))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->>
    (io/slurp-resource filename)
    (md/md-to-html-string)))

(defn pprint-str [x]
  (let [w (java.io.StringWriter.)]
    (pprint x w)
    (.toString w)))

(def not-nil? (complement nil?))
