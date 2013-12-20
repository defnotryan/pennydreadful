(ns pennydreadful.data.project
  (:require [datomic.api :as d]
            [pennydreadful.data.datomic :as data]))

(defn- dehydrate [project]
  {:db/id (:id project)
   :project/name (:name project)
   :project/description (:description project)})

(defn- hydrate [project-entity]
  {:id (:db/id project-entity)
   :name (:project/name project-entity)
   :description (:project/description project-entity)})

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

(defn insert-project [user-eid project]
  (let [tempid (d/tempid :db.part/user)
        project-entity (-> project (assoc :id tempid) (dehydrate))
        facts [project-entity {:db/id user-eid :user/projects tempid}]
        result @(d/transact @data/conn facts)
        id (data/tempid->id result tempid)]
    (hydrate (d/entity (:db-after result) id))))

(defn project-by-eid [project-eid]
  (-> @data/conn
      (d/db)
      (d/entity project-eid)
      (hydrate)))

(defn project-eid-owned-by-user-eid? [project-eid user-eid]
  (some #{project-eid} (project-eids-for-user-eid user-eid)))
