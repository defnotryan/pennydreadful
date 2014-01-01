(ns pennydreadful.views.project
  (:use net.cgrand.enlive-html)
  (:require [pennydreadful.views.base :as views.base]
            [pennydreadful.util :as util]))

(def template-path "pennydreadful/views/templates/project.html")

(def cljs-launch-ns "pennydreadful.client.project.ui")

;; Nav tree snippets
(defsnippet snippet-node template-path [:#project-tree :.pd-snippet]
  [{snippet-name :name}]
  [:.snippet-name] (content snippet-name))

(defsnippet folder-node template-path [:#project-tree :.pd-folder]
  [{folder-name :name folder-children :children}]
  [:.folder-name] (content folder-name)
  [:ul.fa-ul] (content
               (map
                (fn [{entity-type :entity :as child}]
                  (if (= :folder entity-type)
                    (folder-node child)
                    (snippet-node child)))
                folder-children)))

(defsnippet collection-node template-path [:#project-tree :.pd-collection]
  [{collection-name :name collection-children :children}]
  [:.collection-name] (content collection-name)
  [:ul.fa-ul] (content
               (map
                (fn [{entity-type :entity :as child}]
                  (if (= :folder entity-type)
                    (folder-node child)
                    (snippet-node child)))
                collection-children)))

(defsnippet collection-panel template-path [:#collections-list :> first-child]
  [{collection-title :name collection-description :description collection-eid :id}]
  [:.collection] (set-attr :id (str "collection-panel-" collection-eid))
  [:.collection-title] (content collection-title)
  [:.collection-description] (html-content collection-description))

(deftemplate project-page template-path [{:keys [project] :as context}]
  [:head] (substitute (views.base/base-head cljs-launch-ns))
  [:nav] (substitute (views.base/base-nav (:username context)))
  [:section.deps] (substitute (views.base/base-deps))
  [:#project-tree :ul.fa-ul] (content (map collection-node (:collections project)))
  [:#project-title] (content (:name project))
  [:#project-description] (content (:description project))
  [:#collections-list] (content (map collection-panel (:collections project))))

(defn render [context]
  (apply str (project-page context)))
