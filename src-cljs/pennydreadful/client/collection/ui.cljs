(ns pennydreadful.client.collection.ui
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [>! <!]]
            [tailrecursion.javelin]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.collection.data :as data]
            [pennydreadful.client.util :refer [log debounce default-string extract-id move-node-up move-node-down format]])
  (:require-macros [enfocus.macros :refer [defaction defsnippet]]
                   [pennydreadful.client.util-macros :refer [go-forever]]
                   [tailrecursion.javelin :refer [defc defc= cell=]]
                   [cljs.core.async.macros :refer [go]]))

;; Javelin cells

(defc progress-mode-selected-radio nil)

(defc progress-mode :off)

(defc= new-progress-mode
  (if-let [radio-id (some-> progress-mode-selected-radio .-id)]
    (case radio-id
      "target-off" :off
      "target-auto" :automatic
      "target-manual" :manual)))

(defc new-word-count-target-raw "0")

(defc= new-word-count-target-valid?
  (or
   (#{:automatic :off} new-progress-mode)
   (not (nil? (re-matches #"\d*" new-word-count-target-raw)))))

(defc= new-word-count-target
  (when new-word-count-target-valid?
    (let [as-int (js/parseInt new-word-count-target-raw)]
      (if (js/isNaN as-int)
        0
        as-int))))

(defc auto-target-word-count 1)

(defc manual-target-word-count 1)

(defc= target-word-count
  (case progress-mode
    :automatic auto-target-word-count
    :manual manual-target-word-count
    nil))

(defc current-word-count 0)

(defc= word-count-progress-bar-style
  (->> (/ current-word-count target-word-count)
       (* 100)
       (format "width:%2.0f%%")))

(defc deadline-mode-selected-radio nil)

(defc= new-deadline-mode
  (if-let [radio-id (some-> deadline-mode-selected-radio .-id)]
    (case radio-id
      "deadline-off" :off
      "deadline-auto" :automatic
      "deadline-manual" :manual)))

(defc deadline-mode :off)

(defc manual-deadline nil)

(defc auto-deadline nil)

(defc= deadline
  (case deadline-mode
    :automatic auto-deadline
    :manual manual-deadline
    nil))

(defc new-deadline-raw "")

(defc= new-deadline-valid?
  (or
   (#{:off :automatic} new-deadline-mode)
   (not (empty? new-deadline-raw))))

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

(defsnippet folder-panel :compiled "src/pennydreadful/views/templates/collection.html"
  "#children-list > *:first-child"
  [{folder-title :name folder-description :description folder-eid :id}]
  ".folder" (ef/set-attr :id (str "folder-panel-" folder-eid))
  ".folder-title" (ef/do-> (ef/content folder-title) (ef/set-attr :data-id folder-eid))
  ".folder-description" (ef/do-> (ef/content folder-description) (ef/set-attr :data-id folder-eid))
  ".folder-move-up" (ef/set-attr :data-id folder-eid)
  ".folder-move-down" (ef/set-attr :data-id folder-eid))

(defsnippet new-folder-node :compiled "src/pennydreadful/views/templates/project.html"
  "#project-tree .pd-folder"
  [{folder-name :name folder-eid :id}]
  ".folder-name" (ef/html-content (ef/html [:a {:href (str "/folder/" folder-eid)} folder-name]))
  ".pd-folder" (ef/set-attr :id (str "folder-node-" folder-eid))
  "ul.fa-ul" (ef/content nil))

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

(defaction disable-word-count-target-input []
  "#target-manual-value" (ef/set-attr :disabled "disabled"))

(defaction enable-word-count-target-input []
  "#target-manual-value" (ef/remove-attr :disabled))

(defaction show-target-manual-value-error []
  "#target-manual-value-error" (ef/remove-style :display))

(defaction hide-target-manual-value-error []
  "#target-manual-value-error" (ef/set-style :display "none"))

(defaction disable-progress-submit-button []
  ".submit-progress-dialog" (ef/add-class "disabled"))

(defaction enable-progress-submit-button []
  ".submit-progress-dialog" (ef/remove-class "disabled"))

(defaction progress-on []
  ".progress-on" (ef/remove-class "hide")
  ".progress-off" (ef/add-class "hide"))

(defaction progress-off []
  ".progress-on" (ef/add-class "hide")
  ".progress-off" (ef/remove-class "hide"))

(defaction disable-deadline-input []
  "#deadline-manual-value" (ef/set-attr :disabled "disabled"))

(defaction enable-deadline-input []
  "#deadline-manual-value" (ef/remove-attr :disabled))

(defaction show-deadline-manual-value-error []
  "#deadline-manual-value-error" (ef/remove-style :display))

(defaction hide-deadline-manual-value-error []
  "#deadline-manual-value-error" (ef/set-style :display "none"))

(defaction disable-deadline-submit-button []
  ".submit-deadline-dialog" (ef/add-class "disabled"))

(defaction enable-deadline-submit-button []
  ".submit-deadline-dialog" (ef/remove-class "disabled"))

(defaction deadline-on []
  ".deadline-on" (ef/remove-class "hide")
  ".deadline-off" (ef/add-class "hide"))

(defaction deadline-off []
  ".deadline-on" (ef/add-class "hide")
  ".deadline-off" (ef/remove-class "hide"))

;; Events

(defn prepare-content-string [s default]
  (let [default-trim (fnil string/trim "")]
    (-> s default-trim (default-string default))))

(defn make-folder-title-input-handler [folder-eid]
  (comp
   (debounce
    (fn [event]
      (go
       (>! data/folder-meta-to-update
           {:id folder-eid :name (prepare-content-string (-> event .-target .-textContent) "Untitled Folder")})))
    3000)
   (fn [event]
     (swap! unsaved-changes conj [:folder-title folder-eid])
     event)))

(def input>folder-title
  (let [handlers (atom {})]
    (fn [event]
      (let [folder-eid (extract-id (.-target event))]
        (when-not (@handlers folder-eid)
          (swap! handlers assoc folder-eid (make-folder-title-input-handler folder-eid)))
        ((@handlers folder-eid) event)))))

(defn make-folder-description-input-handler [folder-eid]
  (comp
   (debounce
    (fn [event]
      (go
       (>! data/folder-meta-to-update
           {:id folder-eid :description (prepare-content-string (-> event .-target .-innerHTML) "Type here to add a description")})))
    3000)
   (fn [event]
     (swap! unsaved-changes conj [:folder-description folder-eid])
     event)))

(def input>folder-description
  (let [handlers (atom {})]
    (fn [event]
      (let [folder-eid (extract-id (.-target event))]
        (when-not (@handlers folder-eid)
          (swap! handlers assoc folder-eid (make-folder-description-input-handler folder-eid)))
        ((@handlers folder-eid) event)))))

(defn close-word-count-mode-dialog []
  (.foundation (js/$ "#edit-progress-dialog") "reveal" "close"))

(defn close-deadline-mode-dialog []
  (.foundation (js/$ "#edit-deadline-dialog") "reveal" "close"))

(defn click>submit-progress-dialog [event]
  (when @new-word-count-target-valid?
   (go
    (>! data/collection-meta-to-update
        (if (#{:manual} @new-progress-mode)
          {:id @collection-eid :word-count-mode @new-progress-mode :target @new-word-count-target}
          {:id @collection-eid :word-count-mode @new-progress-mode})))
   (close-word-count-mode-dialog)))

(defn click>cancel-progress-dialog [event]
  (close-word-count-mode-dialog))

(defn click>submit-deadline-dialog [event]
  (when @new-deadline-valid?
   (go
    (>! data/collection-meta-to-update
        (if (#{:manual} @new-deadline-mode)
          {:id @collection-eid :deadline-mode @new-deadline-mode :deadline (.format (js/moment @new-deadline-raw) "YYYY-MM-DDTHH:mm:ss.SSSZ")}
          {:id @collection-eid :deadline-mode @new-deadline-mode})))
   (close-deadline-mode-dialog)))

(defn click>cancel-deadline-dialog [event]
  (close-deadline-mode-dialog))

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

(defn click>create-folder [event]
  (when @new-folder-name-valid?
    (go (>! data/folders-to-create
            [@collection-eid
             {:name @new-folder-name
              :description "Type here to add a description."}]))))

(defn submit>new-folder-form [event]
  (.preventDefault event)
  (when @new-folder-name-valid?
    (go (>! data/folders-to-create
            [@collection-eid
             {:name @new-folder-name
              :description "Type here to add a description"}]))))

(defn change>new-snippet-name [event]
  (reset! new-snippet-name (-> event .-target .-value)))

(defn keyup>new-snippet-name [event]
  (reset! new-snippet-name (-> event .-target .-value)))

(defn change>input-target-mode [event]
  (reset! progress-mode-selected-radio (-> event .-target)))

(defn change>target-manual-value [event]
  (reset! new-word-count-target-raw (-> event .-target .-value)))

(defn keyup>target-manual-value [event]
  (reset! new-word-count-target-raw (-> event .-target .-value)))

(defn change>input-deadline-mode [event]
  (reset! deadline-mode-selected-radio (-> event .-target)))

(defn change>deadline-manual-value [event]
  (reset! new-deadline-raw (-> event .-target .-value)))

(defaction setup-events []
  "body" (ee/listen-live :input ".folder-title" input>folder-title)
  "body" (ee/listen-live :input ".folder-description" input>folder-description)
  ".cancel-progress-dialog" (ee/listen :click click>cancel-progress-dialog)
  ".submit-progress-dialog" (ee/listen :click click>submit-progress-dialog)
  ".cancel-deadline-dialog" (ee/listen :click click>cancel-deadline-dialog)
  ".submit-deadline-dialog" (ee/listen :click click>submit-deadline-dialog)
  "#collection-description" (ee/listen :input input>collection-description)
  "#new-folder-name-input" (ee/listen :change change>new-folder-name)
  "#new-folder-name-input" (ee/listen :keyup keyup>new-folder-name)
  "#create-folder-button" (ee/listen :click click>create-folder)
  "#new-folder-form" (ee/listen :submit submit>new-folder-form)
  "#new-snippet-name-input" (ee/listen :change change>new-snippet-name)
  "#new-snippet-name-input" (ee/listen :keyup keyup>new-snippet-name)
  "input[name=target-mode]" (ee/listen :change change>input-target-mode)
  "#target-manual-value" (ee/listen :change change>target-manual-value)
  "#target-manual-value" (ee/listen :keyup keyup>target-manual-value)
  "input[name=deadline-mode]" (ee/listen :change change>input-deadline-mode)
  "#deadline-manual-value" (ee/listen :change change>deadline-manual-value))

(defn ready []
  (disable-new-folder-button)
  (disable-new-snippet-button)
  (reset! collection-eid (ef/from "#collection-eid" (ef/get-prop :value)))
  (reset! progress-mode-selected-radio (first (ef/from "input[name=target-mode][checked]" identity)))
  (reset! progress-mode (keyword (ef/from "#word-count-mode" (ef/get-prop :value))))
  (reset! new-word-count-target-raw (ef/from "#target-manual-value" (ef/get-prop :value)))
  (reset! current-word-count (js/parseInt (ef/from "#current-wc" (ef/get-prop :value))))
  (reset! manual-target-word-count (js/parseInt (ef/from "#manual-wc" (ef/get-prop :value))))
  (reset! auto-target-word-count (js/parseInt (ef/from "#auto-wc" (ef/get-prop :value))))
  (reset! deadline-mode-selected-radio (first (ef/from "input[name=deadline-mode][checked]" identity)))
  (reset! deadline-mode (keyword (ef/from "#deadline-mode" (ef/get-prop :value))))
  (reset! new-deadline-raw (ef/from "#target-manual-value" (ef/get-prop :value)))
  (reset! manual-deadline (js/moment (ef/from "#manual-deadline" (ef/get-prop :value))))
  (reset! auto-deadline (js/moment (ef/from "#auto-deadline" (ef/get-prop :value))))
  (cell= (if unsaved-changes?
           (show-changes-flag)
           (hide-changes-flag)))
  (cell= (if new-folder-name-valid?
           (enable-new-folder-button)
           (disable-new-folder-button)))
  (cell= (if new-snippet-name-valid?
           (enable-new-snippet-button)
           (disable-new-snippet-button)))
  (cell= (if (#{:manual} new-progress-mode)
           (enable-word-count-target-input)
           (disable-word-count-target-input)))
  (cell= (if new-word-count-target-valid?
           (do (hide-target-manual-value-error) (enable-progress-submit-button))
           (do (show-target-manual-value-error) (disable-progress-submit-button))))
  (cell= (if (#{:automatic :manual} progress-mode)
           (progress-on)
           (progress-off)))
  (cell= (if (#{:manual} new-deadline-mode)
           (enable-deadline-input)
           (disable-deadline-input)))
  (cell= (if new-deadline-valid?
           (do (hide-deadline-manual-value-error) (enable-deadline-submit-button))
           (do (show-deadline-manual-value-error) (disable-deadline-submit-button))))
  (cell= (if (#{:automatic :manual} deadline-mode)
           (deadline-on)
           (deadline-off)))
  (cell= (ef/at "#target-wc" (ef/content (str target-word-count))))
  (cell= (ef/at "#progress-meter" (ef/set-attr :style word-count-progress-bar-style)))
  (cell= (when deadline
           (ef/at "#deadline-reminder" (ef/set-attr :title (.format deadline "D MMMM YYYY")))))
  (cell= (ef/at "#deadline-reminder" (some-> deadline .fromNow ef/content)))
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
         (ef/at "#collection-description" (ef/html-content collection-description)))))
   (when-let [collection-target (:target collection)]
     (reset! manual-target-word-count collection-target))
   (when-let [collection-word-count-mode (:word-count-mode collection)]
     (reset! progress-mode collection-word-count-mode))
   (when-let [collection-deadline-mode (:deadline-mode collection)]
     (reset! deadline-mode collection-deadline-mode))
   (when-let [collection-deadline (:deadline collection)]
     (reset! manual-deadline (js/moment collection-deadline "YYYY-MM-DDTHH:mm:ss.SSSZ")))))

;; Handle collection meta update errors
(go-forever
 (let [response (<! data/update-collection-meta-errors)]
   (log response)))

;; Handle folders created
(go-forever
 (let [{folder-eid :id :as folder} (<! data/created-folders)]
   (ef/at
    (str "#collection-node-" @collection-eid " > ul.fa-ul") (ef/append (new-folder-node folder))
    "#children-list" (ef/append (folder-panel folder))
    "#new-folder-name-input" (ef/set-prop :value ""))
   (reset! new-folder-name "")))

;; Handle folder created errors
(go-forever
 (let [response (<! data/create-folder-errors)]
   (log response)))

;; Handle folder metadata updates
(go-forever
 (let [folder (<! data/updated-folder-meta)
       folder-eid (-> folder :id str)]
   (when-let [folder-name (:name folder)]
     (swap! unsaved-changes disj [:folder-title folder-eid])
     (let [selector (str "#folder-panel-" folder-eid ". folder-title")
           current-name (ef/from selector (ef/get-text))]
       (when-not (= folder-name current-name)
         (ef/at selector (ef/content folder-name)))
       (ef/at
        (str "#folder-node-" folder-eid " .folder-name") (ef/content folder-name))))
   (when-let [folder-description (:description folder)]
     (swap! unsaved-changes disj [:folder-description folder-eid])
     (let [selector (str "#folder-panel-" folder-eid " .folder-description")
           current-description (ef/from selector (ef/get-text))]
       (when-not (= folder-description current-description)
         (ef/at selector (ef/html-content folder-description)))))))

;; Handle folder metadata update errors
(go-forever
 (let [response (<! data/update-folder-meta-errors)]
   (log response)))
