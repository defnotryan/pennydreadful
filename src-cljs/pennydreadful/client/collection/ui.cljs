(ns pennydreadful.client.collection.ui
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [>! <!]]
            [tailrecursion.javelin]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.collection.data :as data]
            [pennydreadful.client.util :refer [log debounce default-string extract-id debouce move-node-up move-node-down]])
  (:require-macros [enfocus.macros :refer [defaction defsnippet]]
                   [pennydreadful.client.util-macros :refer [go-forever]]
                   [tailrecursion.javelin :refer [defc defc= cell=]]
                   [cljs.core.async.macros :refer [go]]))

;; Javelin cells

(defc new-folder-name "")

(defc= new-folder-name-valid?
  (not (string/blank? new-folder-name)))

(defc new-snippet-name "")

(defc= new-snippet-name-valid?
  (not (string/blank? new-snippet-name)))

(defc collection-eid "")

(defc unsaved-changes #{})

(defc= unsaved-changes? (not (empty? unsaved-changes)))

;; DOM Manipulation

(defaction show-changes-flag []
  "#changes-flag" (ef/remove-class "hide"))

(defaction hide-changes-flag []
  "#changes-flag" (ef/add-class "hide"))

(defaction disable-new-folder-button []
  "#create-folder-button" (ef/add-class "disabled"))

(defaction enable-new-folder-button []
  "#create-folder-button" (ef/remove-class "disabled"))

(defaction disable-new-snippet-button []
  "#create-snippet-button" (ef/add-class "disabled"))

(defaction enable-new-snippet-button []
  "#create-snippet-button" (ef/remove-class "disabled"))

;; Events

(defn click>cancel-progress-dialog [event]
  (.foundation (js/$ "#edit-progress-dialog") "reveal" "close"))

(defn click>cancel-deadline-dialog [event]
  (.foundation (js/$ "#edit-deadline-dialog") "reveal" "close"))

(defn prepare-content-string [s default]
  (let [default-trim (fnil string/trim "")]
    (-> s default-trim (default-string default))))

(def input>collection-description
  (comp
   (debounce
    (fn [event]
      (go
       (>! data/collection-meta-to-update
           {:id @collection-eid :description (prepare-content-string (-> event .-target .-innerHTML) "Type here to add a description")})))
    3000)
   (fn [event]
     (swap! unsaved-changes conj [:collection-description @collection-eid])
     event)))

(defn change>new-folder-name [event]
  (reset! new-folder-name (-> event .-target .-value)))

(defn keyup>new-folder-name [event]
  (reset! new-folder-name (-> event .-target .-value)))

(defn change>new-snippet-name [event]
  (reset! new-snippet-name (-> event .-target .-value)))

(defn keyup>new-snippet-name [event]
  (reset! new-snippet-name (-> event .-target .-value)))

(defaction setup-events []
  ".cancel-progress-dialog" (ee/listen :click click>cancel-progress-dialog)
  ".cancel-deadline-dialog" (ee/listen :click click>cancel-deadline-dialog)
  "#collection-description" (ee/listen :input input>collection-description)
  "#new-folder-name-input" (ee/listen :change change>new-folder-name)
  "#new-folder-name-input" (ee/listen :keyup keyup>new-folder-name)
  "#new-snippet-name-input" (ee/listen :change change>new-snippet-name)
  "#new-snippet-name-input" (ee/listen :keyup keyup>new-snippet-name))

(defn ready []
  (disable-new-folder-button)
  (disable-new-snippet-button)
  (cell= (if unsaved-changes?
           (show-changes-flag)
           (hide-changes-flag)))
  (cell= (if new-folder-name-valid?
           (enable-new-folder-button)
           (disable-new-folder-button)))
  (cell= (if new-snippet-name-valid?
           (enable-new-snippet-button)
           (disable-new-snippet-button)))
  (reset! collection-eid (ef/from "#collection-eid" (ef/get-prop :value)))
  (setup-events)
  (log "pennydreadful.client.collection.ui ready"))

(defn init []
  (main/ready ready))

;; Handle collection metadata updates
(go-forever
 (let [collection (<! data/updated-collection-meta)
       collection-eid (-> collection :id str)]
   (when-let [collection-description (:description collection)]
     (swap! unsaved-changes disj [:collection-description collection-eid])
     (let [current-description (ef/from "#collection-description" (ef/get-text))]
       (when-not (= collection-description current-description)
         (ef/at "#collection-description" (ef/html-content collection-description)))))))

;; Handle collection meta update errors
(go-forever
 (let [response (<! data/update-collection-meta-errors)]
   (log response)))

