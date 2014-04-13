(ns pennydreadful.data.snippet
  (:require [datomic.api :as d]
            [clj-time.coerce :as tc]
            [pennydreadful.util :refer [denil]]
            [pennydreadful.data.datomic :as data]))

(defn- dehydrate [snippet]
  (denil
   {:db/id (:id snippet)
    :snippet/name (:name snippet)
    :snippet/description (:description snippet)
    :snippet/content (:content snippet)
    :snippet/create-date (:create-date snippet)
    :snippet/last-edit-date (:last-edit-date snippet)
    :snippet/target (:target snippet)
    :snippet/deadline (some-> snippet :deadline tc/to-date)
    :snippet/word-count-mode (case (:word-count-mode snippet)
                               :manual :snippet.word-count-mode/manual
                               nil)
    :snippet/deadline-mode (case (:deadline-mode snippet)
                             :manual :snippet.deadline-mode/manual
                             nil)}))

(defn- hydrate [snippet-entity]
  (denil
   {:id (:db/id snippet-entity)
    :entity :snippet
    :name (:snippet/name snippet-entity)
    :description (:snippet/description snippet-entity)
    :position (:snippet/position snippet-entity)
    :content (:snippet/content snippet-entity)
    :create-date (:snippet/create-date snippet-entity)
    :last-edit-date (:snippet/last-edit-date snippet-entity)
    :target (:snippet/target snippet-entity)
    :word-count-mode (case (:snippet/word-count-mode snippet-entity)
                       :snippet.word-count-mode/manual :manual
                       :off)
    :deadline-mode (case (:snippet/deadline-mode snippet-entity)
                     :snippet.deadline-mode/manual :manual
                     :off)
    :deadline (some-> snippet-entity :snippet/deadline tc/from-date)}))

(defn- hydrate-lite [snippet-entity]
  (denil
   {:id (:db/id snippet-entity)
    :name (:snippet/name snippet-entity)
    :description (:snippet/description snippet-entity)
    :position (:snippet/position snippet-entity)}))

(defn insert-snippet-into-collection! [collection-eid snippet]
  (let [tempid (d/tempid :db.part/user)
        snippet-entity (-> snippet (assoc :id tempid) dehydrate)
        facts [snippet-entity [:append-snippet-position-in-collection collection-eid tempid] {:db/id collection-eid :collection/children tempid}]
        result @(d/transact @data/conn facts)
        id (data/tempid->id result tempid)]
    (hydrate (d/entity (:db-after result) id))))

(defn update-snippet! [snippet]
  (let [snippet-entity (dehydrate snippet)
        result @(d/transact @data/conn [snippet-entity])]
    (hydrate (d/entity (:db-after result) (:id snippet)))))

(defn snippet-by-entity [ent depth]
  (if (= depth :snippet-meta)
    (hydrate-lite ent)
    (hydrate ent)))

(defn snippet-by-eid
  ([snippet-eid]
   (snippet-by-eid snippet-eid {:depth :snippet}))
  ([snippet-eid {:keys [depth] :as opts}]
   (case depth
     :snippet-meta (-> @data/conn d/db (d/entity snippet-eid) hydrate-lite)
     :snippet (-> @data/conn d/db (d/entity snippet-eid) hydrate))))

(defn move-up! [snippet-eid]
  @(d/transact @data/conn [[:move-up-snippet-position snippet-eid]]))

(defn move-down! [snippet-eid]
  @(d/transact @data/conn [[:move-down-snippet-position snippet-eid]]))
