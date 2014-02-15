(ns pennydreadful.client.collection.ui
  (:require [pennydreadful.client.main :as main]
            [pennydreadful.client.util :refer [log]]))

(defn ready []
  (log "pennydreadful.client.collection.ui ready"))

(defn init []
  (main/ready ready))


