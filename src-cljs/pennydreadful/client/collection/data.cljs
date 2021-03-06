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

(def folder-eids-to-move-up (chan 1))
(def moved-up-folder-eids (chan 1))
(def move-up-folder-eid-errors (chan 1))

(def folder-eids-to-move-down (chan 1))
(def moved-down-folder-eids (chan 1))
(def move-down-folder-eid-errors (chan 1))

(def snippets-to-create (chan 1))
(def created-snippets (chan 1))
(def create-snippet-errors (chan 1))

(def snippet-meta-to-update (chan 3))
(def updated-snippet-meta (chan 3))
(def update-snippet-meta-errors (chan 3))

(def snippet-eids-to-move-up (chan 1))
(def moved-up-snippet-eids (chan 1))
(def move-up-snippet-eid-errors (chan 1))

(def snippet-eids-to-move-down (chan 1))
(def moved-down-snippet-eids (chan 1))
(def move-down-snippet-eid-errors (chan 1))

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
         :error-handler (fn [resp] (go (>! update-folder-meta-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; PUT folder-eids for moving up
(go-forever
 (let [folder-eid (<! folder-eids-to-move-up)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/folder/" folder-eid "/move-up")
        {:format :raw
         :response-format :raw
         :handler (fn [_]
                    (go (>! moved-up-folder-eids folder-eid)))
         :error-handler (fn [resp]
                          (go (>! move-up-folder-eid-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; PUT folder-eids for moving down
(go-forever
 (let [folder-eid (<! folder-eids-to-move-down)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/folder/" folder-eid "/move-down")
        {:format :raw
         :response-format :raw
         :handler (fn [_]
                    (go (>! moved-down-folder-eids folder-eid)))
         :error-handler (fn [resp]
                          (go (>! move-down-folder-eid-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; PUT snippet-eids for moving up
(go-forever
 (let [snippet-eid (<! snippet-eids-to-move-up)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/snippet/" snippet-eid "/move-up")
        {:format :raw
         :response-format :raw
         :handler (fn [_]
                    (go (>! moved-up-snippet-eids snippet-eid)))
         :error-handler (fn [resp]
                          (go (>! move-up-snippet-eid-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; PUT snippet-eids for moving down
(go-forever
 (let [snippet-eid (<! snippet-eids-to-move-down)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/snippet/" snippet-eid "/move-down")
        {:format :raw
         :response-format :raw
         :handler (fn [_]
                    (go (>! moved-down-snippet-eids snippet-eid)))
         :error-handler (fn [resp]
                          (go (>! move-down-snippet-eid-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; POST new snippets in snippets-to-create channel
(go-forever
 (let [[collection-eid snippet] (<! snippets-to-create)]
   (swap! util/outstanding-request-count inc)
   (POST (str "/collection/" collection-eid "/snippet")
         {:format :raw
          :response-format :raw
          :params snippet
          :handler (fn [new-snippet-str]
                     (go (>! created-snippets (reader/read-string new-snippet-str))))
          :error-handler (fn [resp]
                           (go (>! create-snippet-errors resp)))
          :finally #(swap! util/outstanding-request-count dec)})))

;; PUT snippet metadata updates in snippet-meta-to-update
(go-forever
 (let [{snippet-eid :id :as snippet} (<! snippet-meta-to-update)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/snippet/" snippet-eid)
        {:format :raw
         :response-format :raw
         :params snippet
         :handler (fn [_] (go (>! updated-snippet-meta snippet)))
         :error-handler (fn [resp] (go (>! update-snippet-meta-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))
