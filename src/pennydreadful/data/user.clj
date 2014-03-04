(ns pennydreadful.data.user
  (:require [datomic.api :as d]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.datomic :as data]))

(defn- dehydrate [user]
  {:db/id (:id user)
   :user/username (:username user)
   :user/password (:password user)})

(defn- hydrate [user-entity]
  {:id (:db/id user-entity)
   :username (:user/username user-entity)
   :password (:user/password user-entity)})

(def user-eid-for-username-query
  '[:find ?user-eid
    :in $ ?username
    :where [?user-eid :user/username ?username]])

(defn user-for-username [username]
  (let [db (d/db @data/conn)
        results (d/q user-eid-for-username-query db username)
        user-eid (ffirst results)
        user-entity (d/entity db user-eid)]
    (hydrate user-entity)))

(defn owned-eids [user-eid]
  (let [db (d/db @data/conn)
        user-entity (d/entity db user-eid)
        project-entities (:user/projects user-entity)]
    (concat
     (map :db/id project-entities))
     (mapcat data-project/owned-eids project-entities)))
