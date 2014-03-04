(ns pennydreadful.data.folder
  (:require [datomic.api :as d]
            [clj-time.coerce :as tc]
            [pennydreadful.util :refer [denil not-nil?]]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.snippet :as data-snippet]))

(defn folder-entity? [ent]
  (contains? ent :folder/name))

(defn- dehydrate [folder]
  (denil
   {:db/id (:id folder)
    :folder/name (:name folder)
    :folder/description (:description folder)
    :folder/target (:target folder)
    :folder/word-count-mode (case (:word-count-mode folder)
                              :manual :folder.word-count-mode/manual
                              :automatic :folder.word-count-mode/automatic
                              :folder.word-count-mode/off)
    :folder/deadline-mode (case (:deadline-mode folder)
                            :manual :folder.deadline-mode/manual
                            :automatic :folder.deadline-mode/automatic
                            :folder.deadline-mode/off)
    :folder/deadline (some-> folder :deadline tc/to-date)}))

(defn- hydrate [folder-entity]
  (denil
   {:id (:db/id folder-entity)
    :entity :folder
    :name (:folder/name folder-entity)
    :description (:folder/description folder-entity)
    :target (:folder/target folder-entity)
    :word-count-mode (case (:folder/word-count-mode folder-entity)
                       :folder.word-count-mode/manual :manual
                       :folder.word-count-mode/automatic :automatic
                       :off)
    :deadline-mode (case (:folder/deadline-mode folder-entity)
                     :folder.deadline-mode/manual :manual
                     :folder.deadline-mode/automatic :automatic
                     :off)
    :deadline (some-> folder-entity :folder/deadline tc/from-date)}))

(defn insert-folder-into-collection! [collection-eid folder]
  (let [tempid (d/tempid :db.part/user)
        folder-entity (-> folder (assoc :id tempid) dehydrate)
        facts [folder-entity {:db/id collection-eid :collection/children tempid}]
        result @(d/transact @data/conn facts)
        id (data/tempid->id result tempid)]
    (hydrate (d/entity (:db-after result) id))))

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

(defn owned-eids [folder-entity]
  (concat
   (map :db/id (:folder/children folder-entity))
   (mapcat
    (fn [child-entity]
      (when (:folder/name child-entity)
        (owned-eids child-entity)))
    (:folder/children folder-entity))))
