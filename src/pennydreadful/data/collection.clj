(ns pennydreadful.data.collection
  (:require [datomic.api :as d]
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
    :collection/deadline (:deadline collection)}))

(defn- hydrate [collection-entity]
  (denil
   {:id (:db/id collection-entity)
    :name (:collection/name collection-entity)
    :description (:collection/description collection-entity)
    :target (:collection/target collection-entity)
    :deadline (:collection/deadline collection-entity)}))

(def collection-eid-owned-by-user-eid-query
  '[:find ?collection-eid
    :in $ ?user-eid ?collection-eid
    :where [?user-eid :user/projects ?project-eid]
           [?project-eid :project/collections ?collection-eid]])

(defn collection-eid-owned-by-user-eid? [collection-eid user-eid]
  (let [db (d/db @data/conn)
        results (d/q collection-eid-owned-by-user-eid-query db user-eid collection-eid)]
    (not-nil? (ffirst results))))

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
     :snippet-names (get-with-children collection-eid :snippet-names)
     (get-shallow collection-eid))))

(defn insert-collection! [project-eid collection]
  (let [tempid (d/tempid :db.part/user)
        collection-entity (-> collection (assoc :id tempid) (dehydrate))
        facts [collection-entity {:db/id project-eid :project/collections tempid}]
        result @(d/transact @data/conn facts)
        id (data/tempid->id result tempid)]
    (hydrate (d/entity (:db-after result) id))))

(defn delete-collection! [collection-eid]
  @(d/transact @data/conn [[:db.fn/retractEntity collection-eid]]))
