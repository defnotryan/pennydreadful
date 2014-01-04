(ns pennydreadful.client.project.data
  (:require [pennydreadful.client.util :as util :refer [log DELETE]]
            [cljs.reader :as reader]
            [cljs.core.async :refer [chan <! >!]]
            [ajax.core :refer [GET POST PUT]]
            [tailrecursion.javelin])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [tailrecursion.javelin :refer [defc defc=]]
                   [pennydreadful.client.util-macros :refer [go-forever]]))

(def collection-eids-to-delete (chan 1))
(def deleted-collection-eids (chan 1))
(def delete-collection-errors (chan 1))

(def collections-to-create (chan 1))
(def created-collections (chan 1))
(def create-collection-errors (chan 1))

;; DELETE collection-eids in collection-eids-to-delete channel
(go-forever
 (let [collection-eid (<! collection-eids-to-delete)]
   (swap! util/outstanding-request-count inc)
   (DELETE (str "/collection/" collection-eid)
         {:format :raw
          :response-format :raw
          :handler (fn [_]
                     (go (>! deleted-collection-eids collection-eid)))
          :error-handler (fn [resp]
                           (go (>! delete-collection-errors resp)))
          :finally #(swap! util/outstanding-request-count dec)})))


;; POST new collections in collections-to-create channel
(go-forever
 (let [[project-eid collection] (<! collections-to-create)]
   (swap! util/outstanding-request-count inc)
   (POST (str "/project/" project-eid "/collection")
         {:format :raw
          :response-format :raw
          :params collection
          :handler (fn [new-collection-str]
                     (go (>! created-collections (reader/read-string new-collection-str))))
          :error-handler (fn [resp]
                           (go (>! create-collection-errors resp)))
          :finally #(swap! util/outstanding-request-count dec)})))
