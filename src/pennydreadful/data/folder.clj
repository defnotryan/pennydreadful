(ns pennydreadful.data.folder
  (:require [datomic.api :as d]
            [pennydreadful.util :refer [denil]]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.snippet :as data-snippet]))

(defn folder-entity? [ent]
  (contains? ent :folder/name))

(defn- dehydrate [folder]
  (denil
   {:db/id (:id folder)
    :folder/name (:name folder)
    :folder/description (:description folder)
    :folder/target (:target folder)}))

(defn- hydrate [folder-entity]
  (denil
   {:id (:db/id folder-entity)
    :entity :folder
    :name (:folder/name folder-entity)
    :description (:folder/description folder-entity)
    :target (:folder/target folder-entity)}))

(declare folder-by-entity)

(defn- get-child [child-eid depth]
  (let [child-entity (-> @data/conn d/db (d/entity child-eid))]
    (if (folder-entity? child-entity)
      (folder-by-entity child-entity depth)
      (data-snippet/snippet-by-entity child-entity depth))))

(defn folder-by-entity [ent depth]
  (let [child-eids (map :db/id (:folder/children ent))
        folder (hydrate ent)]
    (assoc folder :children (map #(get-child % depth) child-eids))))
