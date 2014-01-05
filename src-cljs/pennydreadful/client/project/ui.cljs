(ns pennydreadful.client.project.ui
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [clojure.string :refer [blank? trim]]
            [cljs.core.async :as async :refer [>! <!]]
            [tailrecursion.javelin]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.project.data :as data]
            [pennydreadful.client.util :refer [log extract-id debounce]])
  (:require-macros [enfocus.macros :refer [defaction defsnippet]]
                   [pennydreadful.client.util-macros :refer [go-forever]]
                   [tailrecursion.javelin :refer [defc defc= cell=]]
                   [cljs.core.async.macros :refer [go]]))

;; Javelin cells

(defc new-collection-name "")

(defc= new-collection-name-valid?
  (not (blank? new-collection-name)))

(defc project-eid "")

(defc unsaved-changes #{})

(defc= unsaved-changes? (not (empty? unsaved-changes)))

;; DOM manipulation

(defsnippet collection-panel :compiled "src/pennydreadful/views/templates/project.html"
  "#collections-list > *:first-child"
  [{collection-title :name collection-description :description collection-eid :id}]
  ".collection" (ef/set-attr :id (str "collection-panel-" collection-eid))
  ".collection-title" (ef/do-> (ef/content collection-title) (ef/set-attr :data-id collection-eid))
  ".collection-description" (ef/do-> (ef/content collection-description) (ef/set-attr :data-id collection-eid))
  ".collection-delete-link" (ef/set-attr :data-reveal-id (str "delete-confirmation-" collection-eid))
  ".reveal-modal" (ef/set-attr :id (str "delete-confirmation-" collection-eid))
  ".reveal-modal em" (ef/content collection-title)
  ".delete-confirm" (ef/set-attr :data-id collection-eid)
  ".delete-nevermind" (ef/set-attr :data-id collection-eid))

(defsnippet new-collection-node :compiled "src/pennydreadful/views/templates/project.html"
  "#project-tree .pd-collection"
  [{collection-name :name collection-eid :id}]
  ".collection-name" (ef/content collection-name)
  ".pd-collection" (ef/set-attr :id (str "collection-node-" collection-eid))
  "ul.fa-ul" (ef/content nil))

(defaction disable-new-collection-button []
  "#create-collection-button" (ef/add-class "disabled"))

(defaction enable-new-collection-button []
  "#create-collection-button" (ef/remove-class "disabled"))

(defaction show-changes-flag []
  "#changes-flag" (ef/remove-class "hide"))

(defaction hide-changes-flag []
  "#changes-flag" (ef/add-class "hide"))

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

(defn click>create-collection [event]
  (when @new-collection-name-valid?
    (go (>! data/collections-to-create
            [@project-eid
             {:name @new-collection-name
              :description "Type here to add a description."}]))))

(defn change>new-collection-name [event]
  (reset! new-collection-name (-> event .-target .-value)))

(defn keyup>new-collection-name [event]
  (reset! new-collection-name (-> event .-target .-value)))

(defn submit>new-collection-form [event]
  (.preventDefault event)
  (when @new-collection-name-valid?
    (go (>! data/collections-to-create
            [@project-eid
             {:name @new-collection-name
              :description "Type here to add a description."}]))))

(defn prepare-content-string [s default-string]
  (let [trimmed-string (trim s)]
    (if (blank? trimmed-string)
      default-string
      trimmed-string)))

(def input>project-description
  (comp
   (debounce
    (fn [event]
      (go
       (>! data/project-descriptions-to-update
           {:id @project-eid :description (prepare-content-string (-> event .-target .-innerHTML) "Type here to add a description")})))
    3000)
   (fn [event]
     (swap! unsaved-changes conj [:project-description @project-eid])
     event)))

(defaction setup-events []
  "body" (ee/listen-live :click ".delete-confirm" click>delete-confirm)
  "body" (ee/listen-live :click ".delete-nevermind" click>delete-nevermind)
  "#create-collection-button" (ee/listen :click click>create-collection)
  "#new-collection-name-input" (ee/listen :change change>new-collection-name)
  "#new-collection-name-input" (ee/listen :keyup keyup>new-collection-name)
  "#new-collection-form" (ee/listen :submit submit>new-collection-form)
  "#project-description" (ee/listen :input input>project-description))

;; Initialize
(defn ready []
  (disable-new-collection-button)
  (cell= (if new-collection-name-valid?
           (enable-new-collection-button)
           (disable-new-collection-button)))
  (cell= (if unsaved-changes?
           (show-changes-flag)
           (hide-changes-flag)))
  (reset! project-eid (ef/from "#project-eid" (ef/get-prop :value)))
  (setup-events))

(defn init []
  (main/ready ready)
  (log "pennydreadful.client.project.ui initialized"))

;; Handle stuff

;; Handle collections deleted
(go-forever
 (let [collection-eid (<! data/deleted-collection-eids)]
   (ef/at (str "#collection-panel-" collection-eid) (ef/remove-node))
   (ef/at (str "#collection-node-" collection-eid) (ef/remove-node))
   (close-confirm-modal collection-eid)))

;; Handle collection delete errors
(go-forever
 (let [response (<! data/delete-collection-errors)]
   (log response)))

;; Handle collections created
(go-forever
 (let [{collection-eid :id :as collection} (<! data/created-collections)]
   (ef/at
    "#project-tree > ul.fa-ul" (ef/append (new-collection-node collection))
    "#collections-list" (ef/prepend (collection-panel collection))
    "#new-collection-name-input" (ef/set-prop :value "")
    (str "#collection-panel-" collection-eid " .collection-delete-link") (ee/listen :click #(open-confirm-modal collection-eid)))
   (reset! new-collection-name "")
   (.scroll js/window 0 0)))

;; Handle collection create errors
(go-forever
 (let [response (<! data/create-collection-errors)]
   (log response)))


;; Handle project description updates
(go-forever
 (let [project (<! data/updated-project-descriptions)
       project-eid (-> project :id str)
       project-description (:description project)]
   (swap! unsaved-changes disj [:project-description project-eid])
   (let [current-description (ef/from "#project-description" (ef/get-text))]
     (when-not (= project-description current-description)
       (ef/at "#project-description" (ef/html-content project-description))))))
