(ns pennydreadful.data.datomic
  (:require [clj-time.core :as time]
            [datomic.api :as d])
  (:use [flyingmachine.cartographer.core]))

(declare ent->user
         ent->project
         ent->collection)
(declare find-one
         find-all)

(def uri "datomic:free://localhost:4334/pennydreadful-db")

;; on a delay so we don't have to run the db just to load the ns
(def conn (delay (d/connect uri)))

(defn extant-map [m]
  (into {} (remove (fn [[k v]] (nil? v)) m)))

(defn entify [m rules]
  (let [with-nils (into {} (map (fn [[k v]]
                                  (assoc {} v (k m)))
                                (:attributes rules)))]
    (extant-map with-nils)))

(defmaprules ent->collection
  (attr :id :db/id)
  (attr :name :collection/name)
  (attr :description :collection/description)
  (attr :target :collection/target)
  (attr :deadline :collection/deadline))

(defmaprules ent->user
  (attr :id :db/id)
  (attr :username :user/username)
  (has-many :projects
            :rules ent->project
            :retriever (fn [user-ent]
                         (entity-seq
                          (find-all '[:find ?project-eid
                                      :in $ ?user-eid
                                      :where [?user-eid :user/projects ?project-eid]]
                                    (:db/id user-ent))))))

(defn find-all [query & inputs]
  (let [db (d/db @conn)
        results (apply (partial d/q query db) inputs)]
    (apply concat results)))

(defn find-one [query & inputs]
  (let [db (d/db @conn)
        results (apply (partial d/q query db) inputs)]
    (ffirst results)))

(defn entity-seq [eid-seq]
  (map (partial d/entity (d/db @conn)) eid-seq))

(defn tempid->id [result tempid]
  (d/resolve-tempid (:db-after result) (:tempids result) tempid))

(defn insert-map
  ([m mrules]
   (insert-map m mrules {}))
  ([m mrules relation]
   (let [tempid (d/tempid :db.part/user)
         ent (assoc (entify m mrules) :db/id tempid)
         facts (if (empty? relation) [ent] [ent {:db/id (:by-id relation) (:on relation) tempid}])
         result @(d/transact @conn facts)
         id (tempid->id result tempid)]
     (mapify (d/entity (:db-after result) id) mrules))))

(defn insert-user [user]
  (insert-map user ent->user))


(defn insert-collection [project collection]
  (insert-map collection ent->collection {:on :project/collections
                                          :by-id (:id project)}))


(defn collections-for-project-eid [project-eid]
  (map #(mapify % ent->collection)
       (entity-seq
        (find-all '[:find ?collection-eid
                    :in $ ?project-eid
                    :where [?project-eid :project/collections ?collection-eid]]
                  project-eid))))
