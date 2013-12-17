(ns pennydreadful.client.util
  (:require [clojure.string :as string]))

(defn parse-params [path]
  (into {}
    (let [param-str (second (string/split path #"\?"))]
      (for [pair (string/split param-str #"&")]
        (let [[k v] (string/split pair #"=")]
          (when k [(keyword k) v]))))))

(def query-params (parse-params (.-location js/window)))
