(ns pennydreadful.client.projects
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [clojure.string :refer [blank?]]
            [cljs.core.async :as async :refer [<!]]
            [cljs.reader :as reader]
            [tailrecursion.javelin :as jav]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.util :refer [DELETE POST+ log extract-id]])
  (:require-macros [enfocus.macros :refer [defaction defsnippet]]
                   [tailrecursion.javelin :refer [defc defc= cell=]]
                   [cljs.core.async.macros :refer [go]]))

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

(defaction enable-new-project-button []
  "#create-project-button" (ef/remove-class "disabled"))

(defaction disable-new-project-button []
  "#create-project-button" (ef/add-class "disabled"))

(defc new-project-name "")

(defc= new-project-name-valid?
  (not (blank? new-project-name)))

(cell= (if new-project-name-valid?
         (enable-new-project-button)
         (disable-new-project-button)))

(defn post-new-project! [m]
  (go (<! (POST+ "/project" {:params m}))))

(defaction add-project-panel [project]
  "#project-list" (ef/prepend (project-panel project)))

#_(defaction setup-new-project-events [project]
  (str "#project-panel-" (:id project) " .delete-confirm") (ee/listen :click #()))

(defn reset-new-project []
  (ef/at "#new-project-name-input" (ef/set-prop :value ""))
  (reset! new-project-name "")
  (.scroll js/window 0 0))

(defn close-confirm-modal [project-eid]
  (.foundation (js/$ (str "#delete-confirmation-" project-eid)) "reveal" "close"))

(defn open-confirm-modal [project-eid]
  (.foundation (js/$ (str "#delete-confirmation-" project-eid)) "reveal" "open"))

(defn create-project! []
  (go
    (let [new-project-edn (<! (post-new-project! {:name @new-project-name :description "Type here to add a description."}))
          new-project (reader/read-string new-project-edn)]
      (add-project-panel new-project)
      (ef/at
       (str "#project-panel-" (:id new-project) " .project-delete-link") (ee/listen :click #(open-confirm-modal (:id new-project))))
      (reset-new-project))))

(defn new-project-name-change [event]
  (reset! new-project-name (-> event .-target .-value)))

(defn remove-project [project-eid]
  (ef/at (str "#project-panel-" project-eid) (ef/remove-node)))

(defn delete-project! [project-eid]
  (DELETE (str "/project/" project-eid)
               {:format :raw
                :response-format :raw
                :handler #(remove-project project-eid)
                :error-handler #(log "not ok")
                :finally #(close-confirm-modal project-eid)}))

(defaction setup-events []
  "body" (ee/listen-live :click ".delete-confirm" #(delete-project! (extract-id (.-target %))))
  "body" (ee/listen-live :click ".delete-nevermind" #(close-confirm-modal (extract-id (.-target %))))
  "#create-project-button" (ee/listen :click #(when @new-project-name-valid? (create-project!)))
  "#new-project-name-input" (ee/listen :keyup new-project-name-change))

(defn ready []
  (disable-new-project-button)
  (setup-events))

(defn init []
  (main/ready ready)
  (.log js/console "pennydreadful.client.projects initialized"))
