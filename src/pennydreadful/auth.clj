(ns pennydreadful.auth
  (:require [cemerick.friend :as friend]
            [pennydreadful.util :refer [not-nil?]]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.collection :as data-coll]))

(defn credentials [username]
  (data-user/user-for-username username))

(defn user-eid-can-mutate-eid? [user-eid eid]
  (some #{eid} (data-user/owned-eids user-eid)))
