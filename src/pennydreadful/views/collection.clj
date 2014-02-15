(ns pennydreadful.views.collection
  (:use net.cgrand.enlive-html)
  (:require [pennydreadful.views.base :as views.base]
            [pennydreadful.views.tree :as views.tree]
            [pennydreadful.util :as util]))

(def template-path "pennydreadful/views/templates/collection.html")

(def cljs-launch-ns "pennydreadful.client.collection.ui")

(defsnippet folder-panel template-path [:#children-list :> :.folder]
  [{folder-title :name folder-description :description folder-eid :id}]
  [:.folder] (set-attr :id (str "folder-panel-" folder-eid))
  [:.folder-title] (content folder-title)
  [:.folder-description] (content folder-description))

(defsnippet snippet-panel template-path [:#children-list :> :.snippet]
  [{snippet-title :name snippet-description :description snippet-eid :id}]
  [:.snippet] (set-attr :id (str "snippet-panel-" snippet-eid))
  [:.snippet-title] (content snippet-title)
  [:.snippet-description] (content snippet-description))

(deftemplate collection-page template-path [{:keys [collection project username] :as context}]
  [:head] (substitute (views.base/base-head cljs-launch-ns))
  [:nav] (substitute (views.base/base-nav username))
  [:section.deps] (substitute (views.base/base-deps))
  [:#project-nav] (substitute (views.tree/project-nav-tree project (:id collection)))
  [:#collection-title] (content (:name collection))
  [:#collection-description] (content (:description collection))
  [:#children-list] (content
                     (map
                      (fn [{entity-type :entity :as child}]
                        (if (= :folder entity-type)
                          (folder-panel child)
                          (snippet-panel child)))
                      (:children collection))))

(defn render [context]
  (apply str (collection-page context)))