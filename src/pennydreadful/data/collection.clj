(ns pennydreadful.data.collection
  (:require [datomic.api :as d]
            [clj-time.coerce :as tc]
            [pennydreadful.util :refer [denil not-nil?]]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.folder :as data-folder]
            [pennydreadful.data.snippet :as data-snippet]))

(defn- dehydrate [collection]
  (denil
   {:db/id (:id collection)
    :collection/name (:name collection)
    :collection/description (:description collection)
    :collection/target (:target collection)
    :collection/word-count-mode (case (:word-count-mode collection)
                                  :manual :collection.word-count-mode/manual
                                  :automatic :collection.word-count-mode/automatic
                                  :collection.word-count-mode/off)
    :collection/deadline-mode (case (:deadline-mode collection)
                                :manual :collection.deadline-mode/manual
                                :automatic :collection.deadline-mode/automatic
                                :collection.deadline-mode/off)
    :collection/deadline (some-> collection :deadline tc/to-date)}))

(defn- hydrate [collection-entity]
  (denil
   {:id (:db/id collection-entity)
    :entity :collection
    :name (:collection/name collection-entity)
    :description (:collection/description collection-entity)
    :target (:collection/target collection-entity)
    :word-count-mode (case (:collection/word-count-mode collection-entity)
                       :collection.word-count-mode/manual :manual
                       :collection.word-count-mode/automatic :automatic
                       :off)
    :deadline-mode (case (:collection/deadline-mode collection-entity)
                     :collection.deadline-mode/manual :manual
                     :collection.deadline-mode/automatic :automatic
                     :off)
    :deadline (some-> collection-entity :collection/deadline tc/from-date)
    :position (:collection/position collection-entity)}))

(def collection-eid-owned-by-user-eid-query
  '[:find ?collection-eid
    :in $ ?user-eid ?collection-eid
    :where [?user-eid :user/projects ?project-eid]
           [?project-eid :project/collections ?collection-eid]])

(defn collection-eid-owned-by-user-eid? [collection-eid user-eid]
  (let [db (d/db @data/conn)
        results (d/q collection-eid-owned-by-user-eid-query db user-eid collection-eid)]
    (not-nil? (ffirst results))))

(def project-eid-for-collection-eid-query
  '[:find ?project-eid
    :in $ ?collection-eid
    :where [?project-eid :project/collections ?collection-eid]])

(defn project-eid-for-collection-eid [collection-eid]
  (let [db (d/db @data/conn)
        results (d/q project-eid-for-collection-eid-query db collection-eid)]
    (ffirst results)))

(defn- get-shallow [collection-eid]
  (-> @data/conn
      d/db
      (d/entity collection-eid)
      hydrate))

(defn- get-child [child-eid depth]
  (let [child-entity (-> @data/conn (d/db) (d/entity child-eid))]
    (if (data-folder/folder-entity? child-entity)
      (data-folder/folder-by-entity child-entity depth)
      (data-snippet/snippet-by-entity child-entity depth))))

(defn- get-with-children [collection-eid depth]
  (let [collection-entity (-> @data/conn (d/db) (d/entity collection-eid))
        child-eids (map :db/id (:collection/children collection-entity))
        collection (hydrate collection-entity)]
    (assoc collection :children (map #(get-child % depth) child-eids))))

(defn collection-by-eid
  ([collection-eid]
   (collection-by-eid collection-eid {:depth :collection}))
  ([collection-eid {:keys [depth] :as opts}]
   (case depth
     :collection (get-shallow collection-eid)
     :snippet-meta (get-with-children collection-eid :snippet-meta)
     :snippet (get-with-children collection-eid :snippet)
     (get-shallow collection-eid))))

(defn insert-collection! [project-eid collection]
  (let [tempid (d/tempid :db.part/user)
        collection-entity (-> collection (assoc :id tempid) (dehydrate))
        facts [collection-entity [:append-collection-position project-eid tempid] {:db/id project-eid :project/collections tempid}]
        result @(d/transact @data/conn facts)
        id (data/tempid->id result tempid)]
    (hydrate (d/entity (:db-after result) id))))

(defn delete-collection! [collection-eid]
  @(d/transact @data/conn [[:elide-collection-position collection-eid][:db.fn/retractEntity collection-eid]]))

(defn update-collection! [collection]
  (let [collection-entity (dehydrate collection)
        result @(d/transact @data/conn [collection-entity])]
    (hydrate (d/entity (:db-after result) (:id collection)))))

(defn move-up! [collection-eid]
  @(d/transact @data/conn [[:move-up-collection-position collection-eid]]))

(defn move-down! [collection-eid]
  @(d/transact @data/conn [[:move-down-collection-position collection-eid]]))
