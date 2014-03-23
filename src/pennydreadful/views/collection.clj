(ns pennydreadful.views.collection
  (:use net.cgrand.enlive-html)
  (:require [taoensso.timbre :as timbre]
            [clj-time.format :as tf]
            [pennydreadful.views.base :as views.base]
            [pennydreadful.views.tree :as views.tree]
            [pennydreadful.views.utils :refer [attr-cond]]
            [pennydreadful.util :as util]))

(def template-path "pennydreadful/views/templates/collection.html")

(def cljs-launch-ns "pennydreadful.client.collection.ui")

(def deadline-formatter (tf/formatters :date))

(def deadline-long-formatter (tf/formatter "d MMMM yyyy"))

(defsnippet folder-panel template-path [:#children-list :> :.folder]
  [{folder-title :name folder-description :description folder-eid :id}]
  [:.folder] (set-attr :id (str "folder-panel-" folder-eid))
  [:.folder-title] (content folder-title)
  [:.folder-description] (content folder-description)
  [(attr? :data-id)] (set-attr :data-id folder-eid))

(defsnippet snippet-panel template-path [:#children-list :> :.snippet]
  [{snippet-title :name snippet-description :description snippet-eid :id}]
  [:.snippet] (set-attr :id (str "snippet-panel-" snippet-eid))
  [:.snippet-title] (content snippet-title)
  [:.snippet-description] (content snippet-description)
  [(attr? :data-id)] (set-attr :data-id snippet-eid))

(deftemplate collection-page template-path [{:keys [collection project username] :as context}]

  ;; Boilerplate
  [:head] (substitute (views.base/base-head cljs-launch-ns))
  [:nav] (substitute (views.base/base-nav username))
  [:section.deps] (substitute (views.base/base-deps))
  [:#project-nav] (substitute (views.tree/project-nav-tree project (:id collection)))

  ;; Collection metadata
  [:#collection-title] (content (:name collection))
  [:#collection-eid] (set-attr :value (:id collection))
  [:#collection-description] (html-content (:description collection))

  ;; Word count panel
  [:.current-wc] (-> collection :word-count str content)
  [:#current-wc] (->> collection :word-count str (set-attr :value))
  [:#auto-wc] (->> collection :auto-target str (set-attr :value))
  [:#manual-wc] (->> collection :target str (set-attr :value))
  [:#word-count-mode] (->> collection :word-count-mode name (set-attr :value))
  [:#progress-meter] (if-let [percentage (some->> (:target collection)
                                                  (util/protect 0)
                                                  (/ (:word-count collection))
                                                  float
                                                  (* 100)
                                                  (format "width:%2.0f%%"))]
                       (set-attr :style percentage)
                       (set-attr :style "width:0%"))

  ;; Word count target config dialog
  [:#target-off] (attr-cond (= :off (:word-count-mode collection)) :checked "checked")
  [:#target-auto] (attr-cond (= :automatic (:word-count-mode collection)) :checked "checked")
  [:#target-manual] (attr-cond (= :manual (:word-count-mode collection)) :checked "checked")
  [:#target-manual-value] (set-attr :value (:target collection))

  ;; Deadline panel
  [:#manual-deadline] (->> collection :deadline (tf/unparse deadline-formatter) (set-attr :value))
  [:#auto-deadline] (->> collection :auto-deadline (tf/unparse deadline-formatter) (set-attr :value))
  [:#deadline-mode] (->> collection :deadline-mode name (set-attr :value))
  [:#deadline-reminder] (->> collection :deadline (tf/unparse deadline-long-formatter) (set-attr :title))

  ;; Deadline target config dialog
  [:#deadline-off] (attr-cond (= :off (:deadline-mode collection)) :checked "checked")
  [:#deadline-auto] (attr-cond (= :automatic (:deadline-mode collection)) :checked "checked")
  [:#deadline-manual] (attr-cond (= :manual (:deadline-mode collection)) :checked "checked")
  [:#deadline-manual-value] (->> collection :deadline (tf/unparse deadline-formatter) (set-attr :value))

  ;; Child panels
  [:#children-list] (content
                     (map
                      (fn [{entity-type :entity :as child}]
                        (if (= :folder entity-type)
                          (folder-panel child)
                          (snippet-panel child)))
                      (:children collection))))

(defn render [context]
  (apply str (collection-page context)))
