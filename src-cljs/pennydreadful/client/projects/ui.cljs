(ns pennydreadful.client.projects.ui
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [clojure.string :refer [blank? trim]]
            [tailrecursion.javelin]
            [cljs.core.async :as async :refer [>! <! close!]]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.projects.data :as data]
            [pennydreadful.client.util :refer [log extract-id debounce]])
  (:require-macros [enfocus.macros :refer [defaction defsnippet]]
                   [tailrecursion.javelin :refer [defc defc= cell=]]
                   [cljs.core.async.macros :refer [go]]))

;; Javelin cells

(defc new-project-name "")

(defc= new-project-name-valid?
  (not (blank? new-project-name)))

(defc unsaved-changes #{})

(defc= unsaved-changes? (not (empty? unsaved-changes)))


;; DOM manipulation

(defsnippet project-panel :compiled "src/pennydreadful/views/templates/projects.html"
  "#project-list > *:first-child"
  [{project-title :name project-description :description project-eid :id}]
  ".project" (ef/set-attr :id (str "project-panel-" project-eid))
  ".project-title" (ef/do-> (ef/content project-title) (ef/set-attr :data-id project-eid))
  ".project-description" (ef/do-> (ef/html-content project-description) (ef/set-attr :data-id project-eid))
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

(defaction show-changes-flag []
  "#changes-flag" (ef/remove-class "hide"))

(defaction hide-changes-flag []
  "#changes-flag" (ef/add-class "hide"))

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

(defn prepare-content-string [s default-string]
  (log s)
  (let [trimmed-string (trim s)]
    (if (blank? trimmed-string)
      default-string
      trimmed-string)))

(defn make-project-title-input-handler [project-eid]
  (comp
   (debounce
    (fn [event]
      (go
       (>! data/project-titles-to-update
           {:id project-eid :name (prepare-content-string (-> event .-target .-textContent) "Untitled Project")})))
    3000)
   (fn [event]
     (swap! unsaved-changes conj [:project-title project-eid])
     event)))

(def input>project-title
  (let [handlers (atom {})]
    (fn [event]
      (let [project-eid (extract-id (.-target event))]
        (ef/at
         (str "#delete-confirmation-" project-eid " em") (ef/content (-> event .-target .-textContent)))
        (when-not (@handlers project-eid)
          (swap! handlers assoc project-eid (make-project-title-input-handler project-eid)))
        ((@handlers project-eid) event)))))

(defn make-project-description-input-handler [project-eid]
  (comp
   (debounce
    (fn [event]
      (go
       (>! data/project-descriptions-to-update
           {:id project-eid :description (prepare-content-string (-> event .-target .-innerHTML) "Type here to add a description")})))
    3000)
   (fn [event]
     (swap! unsaved-changes conj [:project-description project-eid])
     event)))

(def input>project-description
  (let [handlers (atom {})]
    (fn [event]
      (let [project-eid (extract-id (.-target event))]
        (when-not (@handlers project-eid)
          (swap! handlers assoc project-eid (make-project-description-input-handler project-eid)))
        ((@handlers project-eid) event)))))

(defn keydown>project-title [event]
  (when (= 13 (.-keyCode event))
    (.preventDefault event)
    (.blur (.-target event))))

(defn keydown>project-description [event]
  (when (= 13 (.-keyCode event))
    (.preventDefault event)
    (.blur (.-target event))))

(defn click>create-project [event]
  (when @new-project-name-valid?
    (go (>! data/projects-to-create {:name @new-project-name :description "Type here to add a description."}))))

(defn keyup>new-project-name [event]
  (reset! new-project-name (-> event .-target .-value)))

(defn change>new-project-name [event]
  (reset! new-project-name (-> event .-target .-value)))

;; TODO handle blur>new-project-name?

(defn submit>new-project-form [event]
  (.preventDefault event)
  (when @new-project-name-valid?
    (go (>! data/projects-to-create {:name @new-project-name :description "Type here to add a description."}))))

(defaction setup-events []
  "body" (ee/listen-live :click ".delete-confirm" click>delete-confirm)
  "body" (ee/listen-live :click ".delete-nevermind" click>delete-nevermind)
  "body" (ee/listen-live :input ".project-title" input>project-title)
  "body" (ee/listen-live :input ".project-description" input>project-description)
  ".project-title" (ee/listen :keydown keydown>project-title) ;; listen-live doesn't give us .-keyCode :(
  ;".project-description" (ee/listen :keydown keydown>project-description)
  "#create-project-button" (ee/listen :click click>create-project)
  "#new-project-name-input" (ee/listen :keyup keyup>new-project-name)
  "#new-project-name-input" (ee/listen :change change>new-project-name)
  "#new-project-form" (ee/listen :submit submit>new-project-form))


;; Initialize

(defn ready []
  (disable-new-project-button)
  (cell= (if new-project-name-valid?
           (enable-new-project-button)
           (disable-new-project-button)))
  (cell= (if unsaved-changes?
           (show-changes-flag)
           (hide-changes-flag)))
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
(go
 (while true
   (let [{project-eid :id :as project} (<! data/created-projects)]
     (ef/at
      "#project-list" (ef/prepend (project-panel project))
      "#new-project-name-input" (ef/set-prop :value "")
      (str "#project-panel-" project-eid " .project-title") (ee/listen :keydown keydown>project-title)
      (str "#project-panel-" project-eid " .project-title") (ee/listen :input input>project-title)
      (str "#project-panel-" project-eid " .project-delete-link") (ee/listen :click #(open-confirm-modal project-eid)))
     (reset! new-project-name "")
     (.scroll js/window 0 0))))

;; Handle project create errors
(go
 (while true
   (let [response (<! data/create-project-errors)]
     (log response))))

;; Handle project title updates
(go
 (while true
   (let [project (<! data/updated-project-titles)
         project-eid (-> project :id str)
         project-name (:name project)]
     (swap! unsaved-changes disj [:project-title project-eid])
     (let [selector (str "#project-panel-" project-eid " .project-title")
           current-name (ef/from selector (ef/get-text))]
       (when-not (= project-name current-name)
         (ef/at selector (ef/content project-name)))
       (ef/at
        (str "#delete-confirmation-" project-eid " em") (ef/content project-name))))))

;; Handle project title update errors
(go
 (while true
   (let [response (<! data/update-project-title-errors)]
     (log response))))


;; Handle project description updates
(go
 (while true
   (let [project (<! data/updated-project-descriptions)
         project-eid (-> project :id str)
         project-description (:description project)]
     (swap! unsaved-changes disj [:project-description project-eid])
     (let [selector (str "#project-panel-" project-eid " .project-description")
           current-description (ef/from selector (ef/get-text))]
       (when-not (= project-description current-description)
         (ef/at selector (ef/html-content project-description)))))))

;; Handle project description update errors
(go
 (while true
   (let [response (<! data/update-project-description-errors)]
     (log response))))
