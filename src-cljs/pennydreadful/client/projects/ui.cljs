(ns pennydreadful.client.projects.ui
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [clojure.string :refer [blank?]]
            [tailrecursion.javelin]
            [cljs.core.async :as async :refer [>! <! close!]]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.projects.data :as data]
            [pennydreadful.client.util :refer [log extract-id]])
  (:require-macros [enfocus.macros :refer [defaction defsnippet]]
                   [tailrecursion.javelin :refer [defc defc= cell=]]
                   [cljs.core.async.macros :refer [go]]))

;; Javelin cells

(defc new-project-name "")

(defc= new-project-name-valid?
  (not (blank? new-project-name)))


;; DOM manipulation

(defsnippet project-panel :compiled "src/pennydreadful/views/templates/projects.html"
  "#project-list > *:first-child"
  [{project-title :name project-description :description project-eid :id}]
  ".project" (ef/set-attr :id (str "project-panel-" project-eid))
  ".project-title" (ef/content project-title)
  ".project-description" (ef/content project-description)
  ".project-write-link" (ef/set-attr :href (str "/project/" project-eid))
  ".project-delete-link" (ef/set-attr :data-reveal-id (str "delete-confirmation-" project-eid))
  ".reveal-modal" (ef/set-attr :id (str "delete-confirmation-" project-eid))
  ".reveal-modal em" (ef/content project-title)
  ".delete-confirm" (ef/set-attr :data-id project-eid)
  ".delete-nevermind" (ef/set-attr :data-id project-eid))

(defaction disable-new-project-button []
  "#create-project-button" (ef/add-class "disabled"))

(defaction enable-new-project-button []
  "#create-project-button" (ef/remove-class "disabled"))

(defn command-confirm-modal [command project-eid]
  (.foundation (js/$ (str "#delete-confirmation-" project-eid)) "reveal" command))

(def open-confirm-modal (partial command-confirm-modal "open"))

(def close-confirm-modal (partial command-confirm-modal "close"))


;; Events

(defn click>delete-confirm [event]
  (go
   (let [project-eid (extract-id (.-target event))]
     (>! data/project-eids-to-delete project-eid))))

(defn click>delete-nevermind [event]
  (let [project-eid (extract-id (.-target event))]
    (close-confirm-modal project-eid)))

(defn click>create-project [event]
  (when @new-project-name-valid?
    (go (>! data/projects-to-create {:name @new-project-name :description "Type here to add a description."}))))

(defn keyup>new-project-name [event]
  (reset! new-project-name (-> event .-target .-value)))

(defaction setup-events []
  "body" (ee/listen-live :click ".delete-confirm" click>delete-confirm)
  "body" (ee/listen-live :click ".delete-nevermind" click>delete-nevermind)
  "#create-project-button" (ee/listen :click click>create-project)
  "#new-project-name-input" (ee/listen :keyup keyup>new-project-name))


;; Initialize

(defn ready []
  (disable-new-project-button)
  (cell= (if new-project-name-valid?
           (enable-new-project-button)
           (disable-new-project-button)))
  (setup-events))

(defn init []
  (main/ready ready)
  (log "pennydreadful.client.projects.ui initialized"))


;; Handle stuff

;; Handle projects deleted
(go
 (while true
   (let [project-eid (<! data/deleted-project-eids)]
     (ef/at (str "#project-panel-" project-eid) (ef/remove-node))
     (close-confirm-modal project-eid))))

;; Handle project delete errors
(go
 (while true
   (let [response (<! data/delete-project-errors)]
     (log response))))

;; Handle projects created
(declare project-panel)
(go
 (while true
   (let [{project-eid :id :as project} (<! data/created-projects)]
     (ef/at
      "#project-list" (ef/prepend (project-panel project))
      "#new-project-name-input" (ef/set-prop :value "")
      (str "#project-panel-" project-eid " .project-delete-link") (ee/listen :click #(open-confirm-modal project-eid)))
     (reset! new-project-name "")
     (.scroll js/window 0 0))))

;; Handle project create errors
(go
 (while true
   (let [response (<! data/create-project-errors)]
     (log response))))
