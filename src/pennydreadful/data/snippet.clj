(ns pennydreadful.data.snippet
  (:require [datomic.api :as d]
            [pennydreadful.util :refer [denil]]
            [pennydreadful.data.datomic :as data]))

(defn- dehydrate [snippet]
  (denil
   {:db/id (:id snippet)
    :snippet/name (:name snippet)
    :snippet/description (:description snippet)
    :snippet/content (:content snippet)
    :snippet/create-date (:create-date snippet)
    :snippet/last-edit-date (:last-edit-date snippet)}))

(defn- hydrate [snippet-entity]
  (denil
   {:id (:db/id snippet-entity)
    :entity :snippet
    :name (:snippet/name snippet-entity)
    :description (:snippet/description snippet-entity)
    :content (:snippet/content snippet-entity)
    :create-date (:snippet/create-date snippet-entity)
    :last-edit-date (:snippet/last-edit-date snippet-entity)}))

(defn- hydrate-lite [snippet-entity]
  (denil
   {:id (:db/id snippet-entity)
    :name (:snippet/name snippet-entity)}))

(defn snippet-by-entity [ent depth]
  (if (= depth :snippet-names)
    (hydrate-lite ent)
    (hydrate ent)))
