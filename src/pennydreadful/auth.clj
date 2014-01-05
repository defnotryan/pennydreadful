(ns pennydreadful.auth
  (:require [cemerick.friend :as friend]
            [pennydreadful.util :refer [not-nil?]]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.collection :as data-coll]))

(defn credentials [username]
  (data-user/user-for-username username))

(defn user-eid-can-mutate-project-eid? [user-eid project-eid]
  (not-nil? (data-project/project-eid-owned-by-user-eid? project-eid user-eid)))

(defn user-eid-can-mutate-collection-eid? [user-eid collection-eid]
  (data-coll/collection-eid-owned-by-user-eid? collection-eid user-eid))
