(ns pennydreadful.client.projects.data
  (:require [pennydreadful.client.util :as util :refer [log DELETE]]
            [cljs.reader :as reader]
            [cljs.core.async :refer [chan <! >!]]
            [ajax.core :refer [GET POST PUT]]
            [tailrecursion.javelin])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [tailrecursion.javelin :refer [defc defc=]]
                   [pennydreadful.client.util-macros :refer [go-forever]]))

(def projects-to-create (chan 1))
(def created-projects (chan 1))
(def create-project-errors (chan 1))

(def project-eids-to-delete (chan 1))
(def deleted-project-eids (chan 1))
(def delete-project-errors (chan 1))

(def project-titles-to-update (chan 3))
(def updated-project-titles (chan 3))
(def update-project-title-errors (chan 3))

(def project-descriptions-to-update (chan 3))
(def updated-project-descriptions (chan 3))
(def update-project-description-errors (chan 3))

;; POST new projects in projects-to-create channel
(go-forever
 (let [project (<! projects-to-create)]
   (swap! util/outstanding-request-count inc)
   (POST "/project"
         {:format :raw
          :response-format :raw
          :params project
          :handler (fn [new-project-str]
                     (go (>! created-projects (reader/read-string new-project-str))))
          :error-handler (fn [resp]
                           (go (>! create-project-errors resp)))
          :finally #(swap! util/outstanding-request-count dec)})))

;; DELETE project-eids in projects-to-delete channel
(go-forever
 (let [project-eid (<! project-eids-to-delete)]
   (swap! util/outstanding-request-count inc)
   (DELETE (str "/project/" project-eid)
           {:format :raw
            :response-format :raw
            :handler (fn [_]
                       (go (>! deleted-project-eids project-eid)))
            :error-handler (fn [resp]
                             (go (>! delete-project-errors resp)))
            :finally #(swap! util/outstanding-request-count dec)})))

;; PUT project title updates in project-titles-to-update channel
(go-forever
 (let [project (<! project-titles-to-update)]
   (swap! util/outstanding-request-count inc)
   (PUT (str "/project/" (:id project))
        {:format :raw
         :response-format :raw
         :params project
         :handler (fn [_]
                    (go (>! updated-project-titles project)))
         :error-handler (fn [resp]
                          (go (>! update-project-title-errors resp)))
         :finally #(swap! util/outstanding-request-count dec)})))


;; PUT project description updates in project-descriptions-to-update channel
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
