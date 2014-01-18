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

(def project-descriptions-to-update (chan 1))
(def updated-project-descriptions (chan 1))
(def update-project-description-errors (chan 1))

(def collection-titles-to-update (chan 3))
(def updated-collection-titles (chan 3))
(def update-collection-title-errors (chan 3))

(def collection-descriptions-to-update (chan 3))
(def updated-collection-descriptions (chan 3))
(def update-collection-description-errors (chan 3))

(def collection-eids-to-move-up (chan 1))
(def moved-up-collection-eids (chan 1))
(def move-up-collection-eid-errors (chan 1))

(def collection-eids-to-move-down (chan 1))
(def moved-down-collection-eids (chan 1))
(def move-down-collection-eid-errors (chan 1))

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

;; PUT project description updates in project-descriptions-to-update
(go-forever
 (let [project (<! project-descriptions-to-update)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/project/" (:id project))
        {:format :raw
         :response-format :raw
         :params project
         :handler (fn [_]
                    (go (>! updated-project-descriptions project)))
         :error-handler (fn [resp]
                          (go (>! update-project-description-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; PUT collection title updates in collection-titles-to-update
(go-forever
 (let [collection (<! collection-titles-to-update)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/collection/" (:id collection))
        {:format :raw
         :response-format :raw
         :params collection
         :handler (fn [_]
                    (go (>! updated-collection-titles collection)))
         :error-handler (fn [resp]
                          (go (>! update-collection-title-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; PUT collection description updates in collection-descriptions-to-update
(go-forever
 (let [collection (<! collection-descriptions-to-update)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/collection/" (:id collection))
        {:format :raw
         :response-format :raw
         :params collection
         :handler (fn [_]
                    (go (>! updated-collection-descriptions collection)))
         :error-handler (fn [resp]
                          (go (>! update-collection-description-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; PUT collection-eids for moving up
(go-forever
 (let [collection-eid (<! collection-eids-to-move-up)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/collection/" collection-eid "/move-up")
        {:format :raw
         :response-format :raw
         :handler (fn [_]
                    (go (>! moved-up-collection-eids collection-eid)))
         :error-handler (fn [resp]
                          (go (>! move-up-collection-eid-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))

;; PUT collection-eids for moving down
(go-forever
 (let [collection-eid (<! collection-eids-to-move-down)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/collection/" collection-eid "/move-down")
        {:format :raw
         :response-format :raw
         :handler (fn [_]
                    (go (>! moved-down-collection-eids collection-eid)))
         :error-handler (fn [resp]
                          (go (>! move-down-collection-eid-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))