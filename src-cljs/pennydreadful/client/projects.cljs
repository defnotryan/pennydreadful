(ns pennydreadful.client.projects
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [clojure.string :refer [blank?]]
            [cljs.core.async :as async :refer [<!]]
            [cljs.reader :as reader]
            [tailrecursion.javelin :as jav]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.util :refer [POST+ log]])
  (:require-macros [enfocus.macros :refer [defaction defsnippet]]
                   [tailrecursion.javelin :refer [defc defc= cell=]]
                   [cljs.core.async.macros :refer [go]]))

(defsnippet project-panel :compiled "src/pennydreadful/views/templates/projects.html"
  "#project-list > *:first-child"
  [{project-title :name project-description :description}]
  ".project-title" (ef/content project-title)
  ".project-description" (ef/content project-description))

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

(defn create-project! []
  (go
    (let [new-project-edn (<! (post-new-project! {:name @new-project-name :description "Type here to add a description."}))
          new-project (reader/read-string new-project-edn)]
      (add-project-panel new-project)
      (reset! new-project-name "")
      (ef/at "#new-project-name-input" (ef/set-prop :value ""))
      (.scroll js/window 0 0))))

(defn new-project-name-change [event]
  (reset! new-project-name (-> event .-target .-value)))

(defaction setup-events []
  "#create-project-button" (ee/listen :click #(when @new-project-name-valid? (create-project!)))
  "#new-project-name-input" (ee/listen :keyup new-project-name-change))

(defn ready []
  (disable-new-project-button)
  (setup-events))

(defn init []
  (main/ready ready)
  (.log js/console "pennydreadful.client.projects initialized"))
