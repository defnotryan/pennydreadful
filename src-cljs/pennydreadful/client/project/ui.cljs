(ns pennydreadful.client.project.ui
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [clojure.string :refer [blank? trim]]
            [cljs.core.async :as async :refer [>! <!]]
            [tailrecursion.javelin]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.project.data :as data]
            [pennydreadful.client.util :refer [log extract-id]])
  (:require-macros [enfocus.macros :refer [defaction defsnippet]]
                   [pennydreadful.client.util-macros :refer [go-forever]]
                   [tailrecursion.javelin :refer [defc defc= cell=]]
                   [cljs.core.async.macros :refer [go]]))

;; Javelin cells

(defc new-collection-name "")

(defc= new-collection-name-valid?
  (not (blank? new-collection-name)))

(defc project-eid "")

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

(defaction setup-events []
  "body" (ee/listen-live :click ".delete-confirm" click>delete-confirm)
  "body" (ee/listen-live :click ".delete-nevermind" click>delete-nevermind)
  "#create-collection-button" (ee/listen :click click>create-collection)
  "#new-collection-name-input" (ee/listen :change change>new-collection-name)
  "#new-collection-name-input" (ee/listen :keyup keyup>new-collection-name)
  "#new-collection-form" (ee/listen :submit submit>new-collection-form))

;; Initialize
(defn ready []
  (disable-new-collection-button)
  (cell= (if new-collection-name-valid?
           (enable-new-collection-button)
           (disable-new-collection-button)))
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
   (close-confirm-modal collection-eid)))

;; Handle collection delete errors
(go-forever
 (let [response (<! data/delete-collection-errors)]
   (log response)))

;; Handle collections created
(go-forever
 (let [{collection-eid :id :as collection} (<! data/created-collections)]
   (ef/at
    "#collections-list" (ef/prepend (collection-panel collection))
    "#new-collection-name-input" (ef/set-prop :value "")
    (str "#collection-panel-" collection-eid " .collection-delete-link") (ee/listen :click #(open-confirm-modal collection-eid)))
   (reset! new-collection-name "")
   (.scroll js/window 0 0)))

;; Handle collection create errors
(go-forever
 (let [response (<! data/create-collection-errors)]
   (log response)))
