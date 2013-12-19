(ns pennydreadful.auth
  (:require [cemerick.friend :as friend]
            [pennydreadful.data.user :as data-user]))

(defn credentials [username]
  (data-user/user-for-username username))
