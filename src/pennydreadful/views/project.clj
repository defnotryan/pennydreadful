(ns pennydreadful.views.project
  (:use net.cgrand.enlive-html)
  (:require [pennydreadful.views.base :as views.base]
            [pennydreadful.views.tree :as views.tree]
            [pennydreadful.util :as util]))

(def template-path "pennydreadful/views/templates/project.html")

(def cljs-launch-ns "pennydreadful.client.project.ui")

(defsnippet collection-panel template-path [:#collections-list :> first-child]
  [{collection-title :name collection-description :description collection-eid :id}]
  [:.collection] (set-attr :id (str "collection-panel-" collection-eid))
  [:.collection-title] (content collection-title)
  [:.collection-description] (html-content collection-description)
  [:.collection-delete-link] (set-attr :data-reveal-id (str "delete-confirmation-" collection-eid))
  [:.reveal-modal] (set-attr :id (str "delete-confirmation-" collection-eid))
  [:.reveal-modal :em] (content collection-title)
  [(attr? :data-id)] (set-attr :data-id collection-eid))

(deftemplate project-page template-path [{:keys [project] :as context}]
  [:head] (substitute (views.base/base-head cljs-launch-ns))
  [:nav] (substitute (views.base/base-nav (:username context)))
  [:section.deps] (substitute (views.base/base-deps))
  [:#project-nav] (substitute (views.tree/project-nav-tree project nil))
  [:#project-title] (content (:name project))
  [:#project-description] (content (:description project))
  [:#project-eid] (set-attr :value (:id project))
  [:#collections-list] (content (map collection-panel (sort-by :position (:collections project)))))

(defn render [context]
  (apply str (project-page context)))
