(ns pennydreadful.auth
  (:require [cemerick.friend :as friend]
            [pennydreadful.util :refer [not-nil?]]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]))

(defn credentials [username]
  (data-user/user-for-username username))

(defn user-eid-can-mutate-project-eid? [user-eid project-eid]
  (not-nil? (data-project/project-eid-owned-by-user-eid? user-eid project-eid)))
