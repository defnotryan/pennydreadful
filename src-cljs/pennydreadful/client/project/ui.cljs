(ns pennydreadful.client.project.ui
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [cljs.core.async :as async :refer [>! <!]]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.project.data :as data]
            [pennydreadful.client.util :refer [log extract-id]])
  (:require-macros [enfocus.macros :refer [defaction defsnippet]]
                   [pennydreadful.client.util-macros :refer [go-forever]]
                   [cljs.core.async.macros :refer [go]]))

;; DOM manipulation

(defaction disable-new-collection-button []
  "#create-collection-button" (ef/add-class "disabled"))

(defaction enable-new-collection-button []
  "#create-collection-button" (ef/remove-class "disabled"))

(defn command-confirm-modal [command collection-eid]
  (.foundation (js/$ (str "#delete-confirmation-" collection-eid)) "reveal" command))

(def open-confirm-modal (partial command-confirm-modal "open"))

(def close-confirm-modal (partial command-confirm-modal "close"))

;; Events

(defn click>delete-confirm [event]
  (go
   (let [collection-eid (extract-id (.-target event))]
     (>! data/collection-eids-to-delete collection-eid))))

(defn click>delete-nevermind [event]
  (let [collection-eid (extract-id (.-target event))]
    (close-confirm-modal collection-eid)))

(defaction setup-events []
  "body" (ee/listen-live :click ".delete-confirm" click>delete-confirm)
  "body" (ee/listen-live :click ".delete-nevermind" click>delete-nevermind))

;; Initialize
(defn ready []
  (disable-new-collection-button)
  (setup-events))

(defn init []
  (main/ready ready)
  (log "pennydreadful.client.project.ui initialized"))

;; Handle stuff

;; Handle collections deleted
(go-forever
 (let [collection-eid (<! data/deleted-collection-eids)]
   (ef/at (str "#collection-panel-" collection-eid) (ef/remove-node))
   (close-confirm-modal collection-eid)))

;; Handle collection delete errors
(go-forever
 (let [response (<! data/delete-collection-errors)]
   (log response)))
