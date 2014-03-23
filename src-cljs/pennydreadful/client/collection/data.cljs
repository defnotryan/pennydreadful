(ns pennydreadful.client.collection.data
  (:require [pennydreadful.client.util :as util :refer [log]]
            [cljs.reader :as reader]
            [cljs.core.async :refer [chan <! >!]]
            [ajax.core :refer [GET PUT POST]]
            [tailrecursion.javelin])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [tailrecursion.javelin :refer [defc defc=]]
                   [pennydreadful.client.util-macros :refer [go-forever]]))

(def collection-meta-to-update (chan 3))
(def updated-collection-meta (chan 3))
(def update-collection-meta-errors (chan 3))

(def folders-to-create (chan 1))
(def created-folders (chan 1))
(def create-folder-errors (chan 1))

(def folder-meta-to-update (chan 3))
(def updated-folder-meta (chan 3))
(def update-folder-meta-errors (chan 3))

;; PUT collection metadata updates in collection-meta-to-update
(go-forever
 (let [collection (<! collection-meta-to-update)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/collection/" (:id collection))
        {:format :raw
         :response-format :raw
         :params collection
         :handler (fn [_]
                    (go (>! updated-collection-meta collection)))
         :error-handler (fn [resp]
                          (go (>! update-collection-meta-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; POST new folders in folders-to-create channel
(go-forever
 (let [[collection-eid folder] (<! folders-to-create)]
   (swap! util/outstanding-request-count inc)
   (POST (str "/collection/" collection-eid "/folder")
         {:format :raw
          :response-format :raw
          :params folder
          :handler (fn [new-folder-str]
                     (go (>! created-folders (reader/read-string new-folder-str))))
          :error-handler (fn [resp]
                           (go (>! create-folder-errors resp)))
          :finally #(swap! util/outstanding-request-count dec)})))

;; PUT folder metadata updates in folder-meta-to-update
(go-forever
 (let [{folder-eid :id :as folder} (<! folder-meta-to-update)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/folder/" folder-eid)
        {:format :raw
         :response-format :raw
         :params folder
         :handler (fn [_] (go (>! updated-folder-meta folder)))
         :error-handler (fn [resp] (go (>! update-folder-meta-errors)))
         :finally #(swap! util/outstanding-request-count dec)})))
