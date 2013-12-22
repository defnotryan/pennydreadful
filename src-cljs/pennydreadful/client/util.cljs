(ns pennydreadful.client.util
  (:require [clojure.string :as string]
            [enfocus.core :as ef]
            [ajax.core]
            [tailrecursion.javelin])
  (:require-macros [tailrecursion.javelin :refer [defc defc=]]))

(defn log [anything]
  (.log js/console anything))

(defn parse-params [path]
  (into {}
    (let [param-str (second (string/split path #"\?"))]
      (for [pair (string/split param-str #"&")]
        (let [[k v] (string/split pair #"=")]
          (when k [(keyword k) v]))))))

(def query-params (parse-params (.-location js/window)))

(defn extract-id [node]
  (ef/from node (ef/get-attr :data-id)))

(defn show-spinner []
  (ef/at "#spinner" (ef/remove-class "hide")))

(defn hide-spinner []
  (ef/at "#spinner" (ef/add-class "hide")))

;; Ajax stuff

(defn DELETE [uri & [opts]]
  (ajax.core/ajax-request uri "DELETE" (ajax.core/transform-opts opts)))

(defc outstanding-request-count 0)

(defc= requests-outstanding? (pos? outstanding-request-count))
