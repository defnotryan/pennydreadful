(ns pennydreadful.client.util
  (:require [clojure.string :as string]
            [ajax.core :refer [GET POST PUT DELETE]]
            [cljs.core.async :as async :refer [chan put! close!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))

(defn log [anything]
  (.log js/console anything))

(defn parse-params [path]
  (into {}
    (let [param-str (second (string/split path #"\?"))]
      (for [pair (string/split param-str #"&")]
        (let [[k v] (string/split pair #"=")]
          (when k [(keyword k) v]))))))

(def query-params (parse-params (.-location js/window)))

(defn- ajax-async-wrap [METHOD]
  (fn [uri options]
    (let [ch (chan 1)]
      (METHOD uri (merge options
                    {:format :raw ;;TODO would love to use edn
                     :response-format :raw  ;;TODO would love to use edn
                     :handler (fn [resp]
                                (put! ch resp)
                                (close! ch))
                     :error-handler (fn [resp]
                                      (put! ch resp)
                                      (close! ch))}))
      ch)))

(def GET+ (ajax-async-wrap GET))
(def POST+ (ajax-async-wrap POST))
(def PUT+ (ajax-async-wrap PUT))
(def DELETE+ (ajax-async-wrap DELETE))
