(ns pennydreadful.views.tree
  (:use net.cgrand.enlive-html))

(def template-path "pennydreadful/views/templates/base.html")

;; Nav tree snippets
(defsnippet snippet-node template-path [:#project-tree :.pd-snippet]
  [{snippet-name :name snippet-eid :id} current-eid]
  [:.snippet-name] (add-class (if (= current-eid snippet-eid)
                                "current"
                                ""))
  [:.snippet-name :a] (do-> (content snippet-name)
                            (set-attr :href (str "/snippet/" snippet-eid)))
  [:pd-snippet] (set-attr :id (str "snippet-node-" snippet-eid)))

(defsnippet folder-node template-path [:#project-tree :.pd-folder]
  [{folder-name :name folder-children :children folder-eid :id} current-eid]
  [:.folder-name] (add-class (if (= current-eid folder-eid)
                               "current"
                               ""))
  [:.folder-name :a] (do-> (content folder-name)
                           (set-attr :href (str "/folder/" folder-eid)))
  [:.pd-folder] (set-attr :id (str "folder-node-" folder-eid))
  [:ul.fa-ul] (content
               (map
                (fn [{entity-type :entity :as child}]
                  (if (= :folder entity-type)
                    (folder-node child current-eid)
                    (snippet-node child current-eid)))
                folder-children)))

(defsnippet collection-node template-path [:#project-tree :.pd-collection]
  [current-eid {collection-name :name collection-children :children collection-eid :id}]
  [:.collection-name] (add-class (if (= current-eid collection-eid)
                                   "current"
                                   ""))
  [:.collection-name :a] (do-> (content collection-name)
                               (set-attr :href (str "/collection/" collection-eid)))
  [:.pd-collection] (set-attr :id (str "collection-node-" collection-eid))
  [:ul.fa-ul] (content
               (map
                (fn [{entity-type :entity :as child}]
                  (if (= :folder entity-type)
                    (folder-node child current-eid)
                    (snippet-node child current-eid)))
                collection-children)))

(defsnippet project-nav-tree template-path [:#project-nav]
  [{:keys [id name collections]} current-eid]
  [:#project-title :a] (do-> (content name)
                             (set-attr :href (str "/project/" id)))
  [:#project-tree :> :ul.fa-ul] (->> collections
                                     (sort-by :position)
                                     (map (partial collection-node current-eid))
                                     content))