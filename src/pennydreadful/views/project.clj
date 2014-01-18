(ns pennydreadful.views.project
  (:use net.cgrand.enlive-html)
  (:require [pennydreadful.views.base :as views.base]
            [pennydreadful.util :as util]))

(def template-path "pennydreadful/views/templates/project.html")

(def cljs-launch-ns "pennydreadful.client.project.ui")

;; Nav tree snippets
(defsnippet snippet-node template-path [:#project-tree :.pd-snippet]
  [{snippet-name :name snippet-eid :id}]
  [:.snippet-name :a] (do-> (content snippet-name)
                            (set-attr :href (str "/snippet/" snippet-eid))))

(defsnippet folder-node template-path [:#project-tree :.pd-folder]
  [{folder-name :name folder-children :children folder-eid :id}]
  [:.folder-name :a] (do-> (content folder-name)
                           (set-attr :href (str "/folder/" folder-eid)))
  [:ul.fa-ul] (content
               (map
                (fn [{entity-type :entity :as child}]
                  (if (= :folder entity-type)
                    (folder-node child)
                    (snippet-node child)))
                folder-children)))

(defsnippet collection-node template-path [:#project-tree :.pd-collection]
  [{collection-name :name collection-children :children collection-eid :id}]
  [:.collection-name :a] (do-> (content collection-name)
                               (set-attr :href (str "/collection/" collection-eid)))
  [:.pd-collection] (set-attr :id (str "collection-node-" collection-eid))
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
  [:.collection-description] (html-content collection-description)
  [:.collection-delete-link] (set-attr :data-reveal-id (str "delete-confirmation-" collection-eid))
  [:.reveal-modal] (set-attr :id (str "delete-confirmation-" collection-eid))
  [:.reveal-modal :em] (content collection-title)
  [(attr? :data-id)] (set-attr :data-id collection-eid))

(deftemplate project-page template-path [{:keys [project] :as context}]
  [:head] (substitute (views.base/base-head cljs-launch-ns))
  [:nav] (substitute (views.base/base-nav (:username context)))
  [:section.deps] (substitute (views.base/base-deps))
  [:#project-tree :ul.fa-ul] (content (map collection-node (sort-by :position (:collections project))))
  [:#project-title] (content (:name project))
  [:#project-description] (content (:description project))
  [:#project-eid] (set-attr :value (:id project))
  [:#collections-list] (content (map collection-panel (sort-by :position (:collections project)))))

(defn render [context]
  (apply str (project-page context)))
