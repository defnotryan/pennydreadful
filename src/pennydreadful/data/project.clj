(ns pennydreadful.data.project
  (:require [datomic.api :as d]
            [pennydreadful.util :refer [denil]]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.collection :as data-collection]))

(defn- dehydrate [project]
  (denil
    {:db/id (:id project)
     :project/name (:name project)
     :project/description (:description project)}))

(defn- hydrate [project-entity]
  (denil
    {:id (:db/id project-entity)
     :name (:project/name project-entity)
     :description (:project/description project-entity)}))

(def project-eids-for-user-eid-query
  '[:find ?project-eid
    :in $ ?user-eid
    :where [?user-eid :user/projects ?project-eid]])

(defn projects-for-user-eid [user-eid]
  (let [db (d/db @data/conn)
        results (d/q project-eids-for-user-eid-query db user-eid)]
    (map #(hydrate (d/entity db (first %))) results)))

(defn project-eids-for-user-eid [user-eid]
  (let [db (d/db @data/conn)
        results (d/q project-eids-for-user-eid-query db user-eid)]
    (map first results)))

(defn insert-project! [user-eid project]
  (let [tempid (d/tempid :db.part/user)
        project-entity (-> project (assoc :id tempid) (dehydrate))
        facts [project-entity {:db/id user-eid :user/projects tempid}]
        result @(d/transact @data/conn facts)
        id (data/tempid->id result tempid)]
    (hydrate (d/entity (:db-after result) id))))

(defn update-project! [project]
  (let [project-entity (dehydrate project)
        result @(d/transact @data/conn [project-entity])]
    (hydrate (d/entity (:db-after result) (:id project)))))

(defn- get-shallow [project-eid]
  (-> @data/conn
      d/db
      (d/entity project-eid)
      hydrate))

(defn- get-with-collections [project-eid depth]
  (let [project-entity (-> @data/conn (d/db) (d/entity project-eid))
        collection-eids (map :db/id (:project/collections project-entity))
        project (hydrate project-entity)]
    (assoc project :collections (map #(data-collection/collection-by-eid % {:depth depth}) collection-eids))))

(defn project-by-eid
  ([project-eid]
   (project-by-eid project-eid {:depth :project}))
  ([project-eid {:keys [depth] :as opts}]
   (case depth
     :project (get-shallow project-eid)
     :collection (get-with-collections project-eid :collection)
     :snippet-meta (get-with-collections project-eid :snippet-meta)
     :snippet (get-with-collections project-eid :snippet)
     (get-shallow project-eid))))

(defn delete-project! [project-eid]
  @(d/transact @data/conn [[:db.fn/retractEntity project-eid]]))

(defn owned-eids [project-entity]
  (concat
   (map :db/id (:project/collections project-entity))
   (mapcat data-collection/owned-eids (:project/collections project-entity))))
